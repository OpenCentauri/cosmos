// SPDX-License-Identifier: GPL-2.0-only
/*
 * Sitronix ST77922 I2C Touchscreen Controller Driver
 *
 * Derived from Sitronix vendor driver
 *   (C) 2011 Sitronix Technology Co., Ltd. <rudy_huang@sitronix.com.tw>
 *
 * Register map, protocol types, and packet layouts are taken verbatim
 * from the Sitronix vendor reference driver (GPL-2.0).
 */
#include <linux/version.h>
#include <linux/of.h>
#include <linux/of_device.h>
#include <linux/pm_runtime.h>
#include <linux/kernel.h>
#include <linux/i2c.h>
#include <linux/input.h>
#include <linux/input/mt.h>
#include <linux/interrupt.h>
#include <linux/gpio.h>
#include <linux/of_gpio.h>
#include <linux/delay.h>
#include <linux/slab.h>
#include <linux/mutex.h>
#include <linux/module.h>

#define ST77922_TS_NAME	"st77922-ts"

#define SITRONIX_MAX_TOUCHES	10

enum sitronix_reg {
	FIRMWARE_VERSION      = 0x0000,
	STATUS_REG            = 0x0001,
	DEVICE_CONTROL_REG    = 0x0002,
	X_RESOLUTION_LOW      = 0x0005,
	Y_RESOLUTION_LOW      = 0x0007,
	MAX_NUM_TOUCHES       = 0x0009,
	FIRMWARE_REVISION_3   = 0x000c,
	FIRMWARE_REVISION_2   = 0x000d,
	FIRMWARE_REVISION_1   = 0x000e,
	FIRMWARE_REVISION_0   = 0x000f,
	TOUCH_INFO            = 0x0010,
	TOUCH_POINT0          = 0x0014,
	MISC_CONTROL          = 0x00f0,
	CUSTOMER_INFO         = 0x00f1,
	SMART_WAKE_UP_REG     = 0x00f2,
	CHIP_ID               = 0x00f4,
	PAGE_REG              = 0x00ff,
};

#define STX_STATUS_READY_MASK   0x0f
#define STX_TOUCH_UPDATE_BIT    0x08
#define STX_TOUCH_VALID_BIT     0x80
#define STX_COORD_H_MASK        0x3f
#define STX_TOUCH_POINT_BYTES   7

struct sitronix_ts {
	struct i2c_client  *client;
	struct input_dev   *input;
	struct mutex        lock;

	/* DT-provided GPIOs */
	int           irq_gpio;
	int           rst_gpio;

	/* IC capabilities (read from registers at probe) */
	uint8_t  fw_revision[4];
	int      resolution_x;
	int      resolution_y;
	uint8_t  max_touches;
	uint8_t  chip_id;
	uint8_t  pixel_length;    /* bytes per touch in packet */
	u8       Num_X;
	u8       Num_Y;

	bool     suspended;
};

/* ======================================================================== */
/* Low-level I2C helpers                                                     */
/* ======================================================================== */

static int stx_i2c_read(struct i2c_client *client, u16 reg, u8 *buf, int len)
{
	u8 addr[2] = { reg >> 8, reg & 0xff };
	struct i2c_msg msgs[] = {
		{
			.addr = client->addr,
			.flags = 0,
			.len = sizeof(addr),
			.buf = addr,
		},
		{
			.addr = client->addr,
			.flags = I2C_M_RD,
			.len = len,
			.buf = buf,
		},
	};
	int ret;

	ret = i2c_transfer(client->adapter, msgs, ARRAY_SIZE(msgs));
	if (ret != ARRAY_SIZE(msgs)) {
		if (ret >= 0)
			ret = -EIO;
		dev_err(&client->dev, "i2c read reg 0x%04x failed: %d\n", reg, ret);
		return ret;
	}

	return 0;
}

/* ======================================================================== */
/* IC initialisation: read chip capabilities                                 */
/* ======================================================================== */

static int stx_wait_ready(struct sitronix_ts *ts)
{
	u8 status;
	int ret;
	int retries = 100;

	do {
		ret = stx_i2c_read(ts->client, STATUS_REG, &status, 1);
		if (ret < 0)
			return ret;
		if (!(status & STX_STATUS_READY_MASK))
			return 0;
		msleep(1);
	} while (--retries > 0);

	dev_warn(&ts->client->dev, "controller stayed busy (status=0x%02x)\n", status);
	return -ETIMEDOUT;
}

static int stx_get_resolution(struct sitronix_ts *ts)
{
	u8 buf[4];
	int ret;

	ret = stx_i2c_read(ts->client, X_RESOLUTION_LOW, buf, sizeof(buf));
	if (ret < 0)
		return ret;

	ts->resolution_x = ((buf[0] & STX_COORD_H_MASK) << 8) | buf[1];
	ts->resolution_y = ((buf[2] & STX_COORD_H_MASK) << 8) | buf[3];

	dev_info(&ts->client->dev, "resolution: %d x %d\n",
		 ts->resolution_x, ts->resolution_y);
	return 0;
}

static int stx_get_chip_id(struct sitronix_ts *ts)
{
	u8 chip_id;
	int ret;

	ret = stx_i2c_read(ts->client, CHIP_ID, &chip_id, 1);
	if (ret < 0)
		return ret;

	ts->chip_id = chip_id;
	ts->Num_X = 0;
	ts->Num_Y = 0;

	dev_info(&ts->client->dev, "chip_id=%d\n", ts->chip_id);
	return 0;
}

static int stx_get_max_touches(struct sitronix_ts *ts)
{
	u8 max_touches;
	int ret;

	ret = stx_i2c_read(ts->client, MAX_NUM_TOUCHES, &max_touches, 1);
	if (ret < 0)
		return ret;

	ts->max_touches = min_t(u8, max_touches, SITRONIX_MAX_TOUCHES);

	dev_info(&ts->client->dev, "max_touches=%d\n", ts->max_touches);
	return 0;
}

static int stx_init_ic(struct sitronix_ts *ts)
{
	int ret;

	ret = stx_wait_ready(ts);
	if (ret < 0)
		return ret;

	ret = stx_get_chip_id(ts);
	if (ret < 0)
		return ret;

	ret = stx_get_resolution(ts);
	if (ret < 0)
		return ret;

	ret = stx_get_max_touches(ts);
	if (ret < 0)
		return ret;

	ts->pixel_length = STX_TOUCH_POINT_BYTES;
	return 0;
}

/* ======================================================================== */
/* Hardware reset                                                             */
/* ======================================================================== */

static void stx_hw_reset(struct sitronix_ts *ts)
{
	if (!gpio_is_valid(ts->rst_gpio))
		return;

	gpio_direction_output(ts->rst_gpio, 0);
	msleep(1);
	gpio_set_value(ts->rst_gpio, 1);
	msleep(100);
	dev_dbg(&ts->client->dev, "hw reset done\n");
}

/* ======================================================================== */
/* Touch reporting                                                            */
/* ======================================================================== */

/*
 * Stock CC2 touch path:
 *   - read update/status byte from 0x0010
 *   - when bit 3 is set, read 7-byte contact records from 0x0014
 */
static void stx_report_touches(struct sitronix_ts *ts)
{
	struct i2c_client *client = ts->client;
	struct input_dev  *input  = ts->input;
	u8 buf[SITRONIX_MAX_TOUCHES * STX_TOUCH_POINT_BYTES] = {};
	u8 update;
	int buf_len = ts->max_touches * ts->pixel_length;
	int slot;
	int ret;

	ret = stx_i2c_read(client, TOUCH_INFO, &update, 1);
	if (ret < 0)
		return;

	if (update & STX_TOUCH_UPDATE_BIT) {
		ret = stx_i2c_read(client, TOUCH_POINT0, buf, buf_len);
		if (ret < 0)
			return;
	}

	for (slot = 0; slot < ts->max_touches; slot++) {
		const u8 *p = &buf[slot * ts->pixel_length];
		bool valid = (update & STX_TOUCH_UPDATE_BIT) && (p[0] & STX_TOUCH_VALID_BIT);
		u16 x = ((p[0] & STX_COORD_H_MASK) << 8) | p[1];
		u16 y = ((p[2] & STX_COORD_H_MASK) << 8) | p[3];

		input_mt_slot(input, slot);
		if (valid) {
			input_mt_report_slot_state(input, MT_TOOL_FINGER, true);
			input_report_abs(input, ABS_MT_POSITION_X, x);
			input_report_abs(input, ABS_MT_POSITION_Y, y);
			input_report_abs(input, ABS_MT_TOUCH_MAJOR, 1);
			input_report_abs(input, ABS_MT_PRESSURE, 255);
		} else {
			input_mt_report_slot_state(input, MT_TOOL_FINGER, false);
		}
	}

	input_mt_report_pointer_emulation(input, false);
	input_sync(input);
}

/* ======================================================================== */
/* Threaded IRQ handler                                                       */
/* ======================================================================== */

static irqreturn_t sitronix_ts_irq_thread(int irq, void *dev_id)
{
	struct sitronix_ts *ts = dev_id;

	mutex_lock(&ts->lock);
	if (!ts->suspended)
		stx_report_touches(ts);
	mutex_unlock(&ts->lock);

	return IRQ_HANDLED;
}

/* ======================================================================== */
/* Power management                                                           */
/* ======================================================================== */

static int sitronix_ts_suspend(struct device *dev)
{
	struct i2c_client *client = to_i2c_client(dev);
	struct sitronix_ts *ts    = i2c_get_clientdata(client);

	mutex_lock(&ts->lock);
	ts->suspended = true;
	disable_irq(client->irq);
	mutex_unlock(&ts->lock);
	return 0;
}

static int sitronix_ts_resume(struct device *dev)
{
	struct i2c_client *client = to_i2c_client(dev);
	struct sitronix_ts *ts    = i2c_get_clientdata(client);

	mutex_lock(&ts->lock);
	stx_hw_reset(ts);
	ts->suspended = false;
	enable_irq(client->irq);
	mutex_unlock(&ts->lock);
	return 0;
}

static SIMPLE_DEV_PM_OPS(sitronix_ts_pm_ops,
			 sitronix_ts_suspend,
			 sitronix_ts_resume);

/* ======================================================================== */
/* Probe / remove                                                             */
/* ======================================================================== */

static int sitronix_ts_probe(struct i2c_client *client)
{
	struct device *dev = &client->dev;
	struct sitronix_ts *ts;
	struct input_dev   *input;
	int ret;

	if (!i2c_check_functionality(client->adapter, I2C_FUNC_I2C)) {
		dev_err(dev, "I2C_FUNC_I2C not supported\n");
		return -ENODEV;
	}

	ts = devm_kzalloc(dev, sizeof(*ts), GFP_KERNEL);
	if (!ts)
		return -ENOMEM;

	ts->client = client;
	mutex_init(&ts->lock);
	i2c_set_clientdata(client, ts);

	/* ---- GPIO setup ---------------------------------------------------- */
	ts->irq_gpio = of_get_named_gpio(dev->of_node, "irq-gpio", 0);
	ts->rst_gpio = of_get_named_gpio(dev->of_node, "reset-gpio", 0);
	if (!gpio_is_valid(ts->rst_gpio))
		ts->rst_gpio = of_get_named_gpio(dev->of_node, "rst-gpio", 0);

	if (gpio_is_valid(ts->rst_gpio)) {
		ret = devm_gpio_request_one(dev, ts->rst_gpio,
					    GPIOF_OUT_INIT_HIGH, "sitronix_rst");
		if (ret) {
			dev_err(dev, "failed to request rst-gpio: %d\n", ret);
			return ret;
		}
	}

	if (gpio_is_valid(ts->irq_gpio)) {
		ret = devm_gpio_request_one(dev, ts->irq_gpio,
					    GPIOF_IN, "sitronix_irq");
		if (ret) {
			dev_err(dev, "failed to request irq-gpio: %d\n", ret);
			return ret;
		}
		if (!client->irq)
			client->irq = gpio_to_irq(ts->irq_gpio);
	}

	if (!client->irq) {
		dev_err(dev, "no IRQ assigned\n");
		return -EINVAL;
	}

	/* ---- Reset IC ------------------------------------------------------ */
	stx_hw_reset(ts);

	/* ---- Read IC capabilities ------------------------------------------ */
	ret = stx_init_ic(ts);
	if (ret < 0) {
		dev_err(dev, "IC init failed: %d\n", ret);
		return ret;
	}

	/* Validate resolution; fall back to safe defaults if IC reports 0 */
	if (ts->resolution_x == 0 || ts->resolution_y == 0) {
		dev_warn(dev, "IC reported 0 resolution; using 800x480 defaults\n");
		ts->resolution_x = 800;
		ts->resolution_y = 480;
	}
	if (ts->max_touches == 0) {
		dev_warn(dev, "IC reported 0 max_touches; using 5\n");
		ts->max_touches = 5;
	}

	/* ---- Input device -------------------------------------------------- */
	input = devm_input_allocate_device(dev);
	if (!input)
		return -ENOMEM;

	ts->input        = input;
	input->name      = "Sitronix I2C Touchscreen";
	input->phys      = "sitronix_ts/input0";
	input->id.bustype = BUS_I2C;
	input->dev.parent = dev;

	input_set_capability(input, EV_ABS, ABS_X);
	input_set_capability(input, EV_ABS, ABS_Y);
	input_set_capability(input, EV_KEY, BTN_TOUCH);
	__set_bit(INPUT_PROP_DIRECT, input->propbit);

	input_set_abs_params(input, ABS_X,
			     0, ts->resolution_x - 1, 0, 0);
	input_set_abs_params(input, ABS_Y,
			     0, ts->resolution_y - 1, 0, 0);
	input_set_abs_params(input, ABS_MT_POSITION_X,
			     0, ts->resolution_x - 1, 0, 0);
	input_set_abs_params(input, ABS_MT_POSITION_Y,
			     0, ts->resolution_y - 1, 0, 0);
	input_set_abs_params(input, ABS_MT_PRESSURE,     0, 255, 0, 0);
	input_set_abs_params(input, ABS_MT_TOUCH_MAJOR,  0, 255, 0, 0);
	input_set_abs_params(input, ABS_MT_TRACKING_ID,  -1, ts->max_touches - 1, 0, 0);

	ret = input_mt_init_slots(input, ts->max_touches, INPUT_MT_DIRECT);
	if (ret) {
		dev_err(dev, "input_mt_init_slots failed: %d\n", ret);
		return ret;
	}

	input_set_drvdata(input, ts);

	ret = input_register_device(input);
	if (ret) {
		dev_err(dev, "input_register_device failed: %d\n", ret);
		return ret;
	}

	/* ---- Threaded IRQ -------------------------------------------------- */
	ret = devm_request_threaded_irq(dev, client->irq,
					NULL, sitronix_ts_irq_thread,
					IRQF_ONESHOT | IRQF_TRIGGER_LOW,
					ST77922_TS_NAME, ts);
	if (ret) {
		dev_err(dev, "request_threaded_irq failed: %d\n", ret);
		return ret;
	}

	dev_info(dev, "Sitronix touchscreen probed (chip_id=%d res=%dx%d mt=%d)\n",
		 ts->chip_id, ts->resolution_x, ts->resolution_y, ts->max_touches);
	return 0;
}

static void sitronix_ts_remove(struct i2c_client *client)
{
	struct sitronix_ts *ts = i2c_get_clientdata(client);

	// disable+sync IRQ so the thread can't touch ts->input; pull rst low
	// so the IC releases the I2C bus (else next probe gets -6/ENXIO).
	// devm handles input_unregister+free, gpio_free, and kfree for us.
	if (client->irq) {
		disable_irq(client->irq);
		if (ts) {
			mutex_lock(&ts->lock);
			ts->suspended = true;
			mutex_unlock(&ts->lock);
		}
		synchronize_irq(client->irq);
	}

	if (ts && gpio_is_valid(ts->rst_gpio)) {
		gpio_set_value(ts->rst_gpio, 0);
		msleep(1);  // let any in-flight I2C byte settle before line idles
		mutex_destroy(&ts->lock);
	}

	i2c_set_clientdata(client, NULL);
	dev_info(&client->dev, "Sitronix touchscreen removed\n");
}

/* ======================================================================== */
/* DT / I2C tables and driver registration                                   */
/* ======================================================================== */

static const struct of_device_id sitronix_ts_of_match[] = {
	{ .compatible = "sitronix,st77922-ts" },
	{ },
};
MODULE_DEVICE_TABLE(of, sitronix_ts_of_match);

static const struct i2c_device_id sitronix_ts_id[] = {
	{ ST77922_TS_NAME, 0 },
	{ },
};
MODULE_DEVICE_TABLE(i2c, sitronix_ts_id);

static struct i2c_driver sitronix_ts_i2c_driver = {
	.driver = {
		.name           = ST77922_TS_NAME,
		.of_match_table = sitronix_ts_of_match,
		.pm             = &sitronix_ts_pm_ops,
	},
	.probe    = sitronix_ts_probe,
	.remove   = sitronix_ts_remove,
	.id_table = sitronix_ts_id,
};

static int __init sitronix_ts_i2c_init(void)
{
	return i2c_add_driver(&sitronix_ts_i2c_driver);
}
module_init(sitronix_ts_i2c_init);

static void __exit sitronix_ts_i2c_exit(void)
{
	i2c_del_driver(&sitronix_ts_i2c_driver);
}
module_exit(sitronix_ts_i2c_exit);

MODULE_AUTHOR("Paul Swenson <pdscomp@gmail.com>");
MODULE_DESCRIPTION("Sitronix ST77922 Touchscreen Controller Driver");
MODULE_LICENSE("GPL v2");
