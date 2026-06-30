// SPDX-License-Identifier: GPL-2.0
/*
 * DRM panel driver for Sitronix ST77922 based MIPI-DSI panels.
 *
 * Ported from the Espressif esp-iot-solution component
 * `esp_lcd_st77922` (esp_lcd_st77922_mipi.c). The vendor-specific
 * initialisation sequence below is taken verbatim from that component's
 * `vendor_specific_init_default[]` table (Apache-2.0, Espressif 2024) and
 * re-expressed as DSI DCS writes.
 *
 * The display mode (st77922_mode) is the Elegoo Centauri Carbon 2's own
 * 532x300 timing, taken from the vendor BSP disp2 lcd0 node. The init
 * sequence, however, is from Espressif's 480x480 reference module, so the
 * two are not guaranteed to be a matched pair: if the panel does not come
 * up cleanly, replace st77922_init_sequence() with the values for the
 * actual CC2 glass (from the vendor BSP disp2 st77922 module).
 *
 * Copyright (C) 2026 James Turton.
 */

#include <linux/delay.h>
#include <linux/gpio/consumer.h>
#include <linux/media-bus-format.h>
#include <linux/mod_devicetable.h>
#include <linux/module.h>
#include <linux/of.h>
#include <linux/regulator/consumer.h>
#include <linux/types.h>

#include <video/mipi_display.h>

#include <drm/drm_mipi_dsi.h>
#include <drm/drm_modes.h>
#include <drm/drm_panel.h>

#define DRV_NAME "panel-sitronix-st77922"

/* Page / command-set selection (ST77922 manufacturer command sets) */
#define ST77922_PAGE_CMD1	0xF0	/* user / DCS standard commands */
#define ST77922_PAGE_CMD2	0xF1
#define ST77922_PAGE_CMD3	0xF2

struct st77922 {
	struct device *dev;
	struct drm_panel panel;
	struct gpio_desc *reset_gpio;
	struct regulator *vdd;
	struct regulator *iovcc;
	const struct st77922_panel_desc *desc;
	enum drm_panel_orientation orientation;
};

struct st77922_panel_desc {
	const struct drm_display_mode *mode;
	unsigned int lanes;
	unsigned long mode_flags;
	enum mipi_dsi_pixel_format format;
	void (*init_sequence)(struct mipi_dsi_multi_context *dsi_ctx);
};

static inline struct st77922 *panel_to_st77922(struct drm_panel *panel)
{
	return container_of(panel, struct st77922, panel);
}

/*
 * Vendor-specific initialisation. Translated 1:1 from esp_lcd_st77922_mipi.c.
 * Colour format is set to RGB888 (COLMOD 0x77) to match MIPI_DSI_FMT_RGB888;
 * use 0x55 for RGB565 or 0x66 for RGB666 if you change dsi->format.
 */
static void st77922_init_sequence(struct mipi_dsi_multi_context *dsi_ctx)
{
	/* Select user/standard command page, then MADCTL + COLMOD */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, ST77922_PAGE_CMD1, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, MIPI_DCS_SET_ADDRESS_MODE, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, MIPI_DCS_SET_PIXEL_FORMAT, 0x77);

	mipi_dsi_dcs_write_seq_multi(dsi_ctx, MIPI_DCS_SET_DISPLAY_OFF);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, MIPI_DCS_ENTER_SLEEP_MODE);
	mipi_dsi_msleep(dsi_ctx, 120);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xD0, 0x02);

	/* ===================== CMD2 ===================== */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, ST77922_PAGE_CMD2, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x60, 0x00, 0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x65, 0x80);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x66, 0x02, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xBE, 0x24, 0x00, 0xED);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x70, 0x11, 0x9D, 0x11, 0xE0, 0xE0,
				     0x00, 0x08, 0x75, 0x00, 0x00, 0x00, 0x1A);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x71, 0xD0); /* MIPI command mode */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x71, 0xD3);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x7B, 0x00, 0x08, 0x08);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x80, 0x55, 0x62, 0x2F, 0x17, 0xF0,
				     0x52, 0x70, 0xD2, 0x52, 0x62, 0xEA);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x81, 0x26, 0x52, 0x72, 0x27);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x84, 0x92, 0x25);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x86, 0xC6, 0x04, 0xB1, 0x02, 0x58,
				     0x12, 0x58, 0x10, 0x13, 0x01, 0xA5, 0x00,
				     0xA5, 0xA5);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x87, 0x10, 0x10, 0x58, 0x00, 0x02,
				     0x3A);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x88, 0x00, 0x00, 0x2C, 0x10, 0x04,
				     0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01,
				     0x01, 0x00, 0x06);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x89, 0x00, 0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x8A, 0x13, 0x00, 0x2C, 0x00, 0x00,
				     0x2C, 0x10, 0x10, 0x00, 0x3E, 0x19);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x8B, 0x15, 0xB1, 0xB1, 0x44, 0x96,
				     0x2C, 0x10, 0x97, 0x8E);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x8C, 0x1D, 0xB1, 0xB1, 0x44, 0x96,
				     0x2C, 0x10, 0x50, 0x0F, 0x01, 0xC5, 0x12,
				     0x09);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x8D, 0x0C);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x8E, 0x33, 0x01, 0x0C, 0x13, 0x01,
				     0x01);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x90, 0x00, 0x44, 0x55, 0x7A, 0x00,
				     0x40, 0x40, 0x3F, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x91, 0x00, 0x44, 0x55, 0x7B, 0x00,
				     0x40, 0x7F, 0x3F, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x92, 0x00, 0x44, 0x55, 0x2F, 0x00,
				     0x30, 0x00, 0x05, 0x3F, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x93, 0x00, 0x43, 0x11, 0x3F, 0x00,
				     0x3F, 0x00, 0x05, 0x3F, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x94, 0x00, 0x00, 0x00, 0x00, 0x00,
				     0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x95, 0x9D, 0x1D, 0x00, 0x00, 0xFF);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x96, 0x44, 0x44, 0x07, 0x16, 0x3A,
				     0x3B, 0x01, 0x00, 0x3F, 0x3F, 0x00, 0x40);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x97, 0x44, 0x44, 0x25, 0x34, 0x3C,
				     0x3D, 0x1F, 0x1E, 0x3F, 0x3F, 0x00, 0x40);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xBA, 0x55, 0x3F, 0x3F, 0x3F, 0x3F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9A, 0x40, 0x00, 0x06, 0x00, 0x00,
				     0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9B, 0x00, 0x00, 0x06, 0x00, 0x00,
				     0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9C, 0x40, 0x12, 0x00, 0x00, 0x00,
				     0x12, 0x00, 0x00, 0x00, 0x12, 0x00, 0x00,
				     0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9D, 0x80, 0x53, 0x00, 0x00, 0x00,
				     0x80, 0x64, 0x01);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9E, 0x53, 0x00, 0x00, 0x00, 0x80,
				     0x64, 0x01);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x9F, 0xA0, 0x09, 0x00, 0x57);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB3, 0x00, 0x30, 0x0F, 0x00, 0x00,
				     0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB4, 0x10, 0x09, 0x0B, 0x02, 0x00,
				     0x19, 0x18, 0x13, 0x1E, 0x1D, 0x1C, 0x1E);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB5, 0x08, 0x12, 0x03, 0x0A, 0x19,
				     0x01, 0x11, 0x18, 0x1D, 0x1E, 0x1E, 0x1C);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB6, 0xFF, 0xFF, 0x00, 0x07, 0xFF,
				     0x0B, 0xFF);
	/*
	 * The ESP source declared a 17-byte length for 0xB7 but supplied only
	 * 16 data bytes (the 0xB8 sibling register is 16). 16 bytes is sent.
	 */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB7, 0x00, 0x0B, 0x12, 0x0A, 0x0B,
				     0x06, 0x37, 0x00, 0x02, 0x4D, 0x08, 0x14,
				     0x14, 0x30, 0x36, 0x0F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB8, 0x00, 0x0B, 0x11, 0x09, 0x09,
				     0x06, 0x37, 0x06, 0x05, 0x4D, 0x08, 0x13,
				     0x13, 0x2F, 0x36, 0x0F);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xB9, 0x23, 0x23);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xBB, 0x00, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xBF, 0x0F, 0x13, 0x13, 0x09, 0x09,
				     0x09);

	/* ===================== CMD3 ===================== */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, ST77922_PAGE_CMD3, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x73, 0x04, 0xBA, 0x12, 0x5E, 0x55);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x77, 0x6B, 0x5B, 0xFD, 0xC3, 0xC5);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x7A, 0x15, 0x27);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x7B, 0x04, 0x57);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0x7E, 0x01, 0x0E);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xBF, 0x36);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, 0xE3, 0x40, 0x40);

	/* ===================== CMD1 ===================== */
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, ST77922_PAGE_CMD1, 0x00);
	mipi_dsi_dcs_write_seq_multi(dsi_ctx, MIPI_DCS_ENTER_INVERT_MODE);
}

static int st77922_prepare(struct drm_panel *panel)
{
	struct st77922 *ctx = panel_to_st77922(panel);
	int ret;

	gpiod_set_value_cansleep(ctx->reset_gpio, 1);

	ret = regulator_enable(ctx->iovcc);
	if (ret < 0)
		return dev_err_probe(ctx->dev, ret, "Failed to enable iovcc\n");

	ret = regulator_enable(ctx->vdd);
	if (ret < 0) {
		regulator_disable(ctx->iovcc);
		return dev_err_probe(ctx->dev, ret, "Failed to enable vdd\n");
	}

	/* Power-stabilise, then release reset (active-low). */
	usleep_range(10000, 20000);
	gpiod_set_value_cansleep(ctx->reset_gpio, 0);
	msleep(120);

	return 0;
}

static int st77922_enable(struct drm_panel *panel)
{
	struct st77922 *ctx = panel_to_st77922(panel);
	struct mipi_dsi_device *dsi = to_mipi_dsi_device(ctx->dev);
	struct mipi_dsi_multi_context dsi_ctx = { .dsi = dsi };

	/*
	 * Sent from enable() (not prepare()) so the DSI link is already up,
	 * matching the proven sun6i-dsi + Sitronix panel ordering.
	 */
	ctx->desc->init_sequence(&dsi_ctx);

	mipi_dsi_dcs_exit_sleep_mode_multi(&dsi_ctx);
	mipi_dsi_msleep(&dsi_ctx, 120);

	mipi_dsi_dcs_set_display_on_multi(&dsi_ctx);
	/* Tearing-effect line on, V-blank only (ESP sent 0x35 0x00). */
	mipi_dsi_dcs_set_tear_on_multi(&dsi_ctx, MIPI_DSI_DCS_TEAR_MODE_VBLANK);

	return dsi_ctx.accum_err;
}

static int st77922_disable(struct drm_panel *panel)
{
	struct st77922 *ctx = panel_to_st77922(panel);
	struct mipi_dsi_device *dsi = to_mipi_dsi_device(ctx->dev);
	struct mipi_dsi_multi_context dsi_ctx = { .dsi = dsi };

	mipi_dsi_dcs_set_display_off_multi(&dsi_ctx);
	mipi_dsi_dcs_enter_sleep_mode_multi(&dsi_ctx);
	mipi_dsi_msleep(&dsi_ctx, 120);

	return dsi_ctx.accum_err;
}

static int st77922_unprepare(struct drm_panel *panel)
{
	struct st77922 *ctx = panel_to_st77922(panel);

	gpiod_set_value_cansleep(ctx->reset_gpio, 1);
	regulator_disable(ctx->vdd);
	regulator_disable(ctx->iovcc);

	return 0;
}

/*
 * Centauri Carbon 2 panel timing, derived from the vendor BSP disp2 lcd0 node:
 *   lcd_x 532, lcd_y 300, lcd_dclk_freq 20 MHz
 *   lcd_ht 714, lcd_hbp 84, lcd_hspw 4  -> hfp 98, hsync 4, hbp 80
 *   lcd_vt 462, lcd_vbp 24, lcd_vspw 4  -> vfp 138, vsync 4, vbp 20
 * 714 x 462 @ 20 MHz pixel clock -> ~60 Hz. At 24bpp / 1 lane this is
 * ~480 Mbps, close to the ESP reference PHY rate (500 Mbps).
 *
 * NOTE: the init_sequence above is from Espressif's 480x480 ST77922 module,
 * so init and mode are not guaranteed to be a matched pair. If the panel
 * does not sync, the authoritative init for this glass is the vendor BSP
 * disp2 st77922 module. Espressif's reference mode, for comparison, was:
 *   clock 20400, 480x480, hfp 40/hsync 2/hbp 40, vfp 117/vsync 2/vbp 6.
 */
static const struct drm_display_mode st77922_mode = {
	.clock		= 20000,
	.hdisplay	= 532,
	.hsync_start	= 532 + 98,
	.hsync_end	= 532 + 98 + 4,
	.htotal		= 532 + 98 + 4 + 80,
	.vdisplay	= 300,
	.vsync_start	= 300 + 138,
	.vsync_end	= 300 + 138 + 4,
	.vtotal		= 300 + 138 + 4 + 20,
	.width_mm	= 110,	/* vendor lcd_width  = 0x6e */
	.height_mm	= 62,	/* vendor lcd_height = 0x3e */
};

static const struct st77922_panel_desc st77922_desc = {
	.mode = &st77922_mode,
	.lanes = 1,
	/* video mode (vendor lcd_dsi_if=0), no EoT packet (lcd_dsi_eotp=0) */
	.mode_flags = MIPI_DSI_MODE_VIDEO | MIPI_DSI_MODE_VIDEO_SYNC_PULSE |
		      MIPI_DSI_MODE_LPM | MIPI_DSI_MODE_NO_EOT_PACKET,
	.format = MIPI_DSI_FMT_RGB888,
	.init_sequence = st77922_init_sequence,
};

static const u32 st77922_bus_formats[] = {
	MEDIA_BUS_FMT_RGB888_1X24,
};

static int st77922_get_modes(struct drm_panel *panel,
			     struct drm_connector *connector)
{
	struct st77922 *ctx = panel_to_st77922(panel);
	struct drm_display_mode *mode;

	mode = drm_mode_duplicate(connector->dev, ctx->desc->mode);
	if (!mode)
		return -ENOMEM;

	drm_mode_set_name(mode);
	mode->type = DRM_MODE_TYPE_DRIVER | DRM_MODE_TYPE_PREFERRED;
	connector->display_info.width_mm = mode->width_mm;
	connector->display_info.height_mm = mode->height_mm;
	drm_mode_probed_add(connector, mode);

	drm_display_info_set_bus_formats(&connector->display_info,
					 st77922_bus_formats,
					 ARRAY_SIZE(st77922_bus_formats));

	return 1;
}

static enum drm_panel_orientation st77922_get_orientation(struct drm_panel *panel)
{
	struct st77922 *ctx = panel_to_st77922(panel);

	return ctx->orientation;
}

static const struct drm_panel_funcs st77922_drm_funcs = {
	.prepare	= st77922_prepare,
	.enable		= st77922_enable,
	.disable	= st77922_disable,
	.unprepare	= st77922_unprepare,
	.get_modes	= st77922_get_modes,
	.get_orientation = st77922_get_orientation,
};

static int st77922_probe(struct mipi_dsi_device *dsi)
{
	struct device *dev = &dsi->dev;
	struct st77922 *ctx;
	int ret;

	ctx = devm_kzalloc(dev, sizeof(*ctx), GFP_KERNEL);
	if (!ctx)
		return -ENOMEM;

	ctx->dev = dev;
	ctx->desc = of_device_get_match_data(dev);
	if (!ctx->desc)
		return -ENODEV;

	ctx->reset_gpio = devm_gpiod_get(dev, "reset", GPIOD_OUT_HIGH);
	if (IS_ERR(ctx->reset_gpio))
		return dev_err_probe(dev, PTR_ERR(ctx->reset_gpio),
				     "Failed to get reset gpio\n");

	/* Both supplies are optional; absent ones resolve to a dummy. */
	ctx->vdd = devm_regulator_get(dev, "vdd");
	if (IS_ERR(ctx->vdd))
		return dev_err_probe(dev, PTR_ERR(ctx->vdd),
				     "Failed to get vdd regulator\n");

	ctx->iovcc = devm_regulator_get(dev, "iovcc");
	if (IS_ERR(ctx->iovcc))
		return dev_err_probe(dev, PTR_ERR(ctx->iovcc),
				     "Failed to get iovcc regulator\n");

	ret = of_drm_get_panel_orientation(dev->of_node, &ctx->orientation);
	if (ret < 0)
		return dev_err_probe(dev, ret, "Failed to get orientation\n");

	mipi_dsi_set_drvdata(dsi, ctx);

	dsi->lanes = ctx->desc->lanes;
	dsi->format = ctx->desc->format;
	dsi->mode_flags = ctx->desc->mode_flags;

	drm_panel_init(&ctx->panel, dev, &st77922_drm_funcs,
		       DRM_MODE_CONNECTOR_DSI);

	ret = drm_panel_of_backlight(&ctx->panel);
	if (ret)
		return ret;

	drm_panel_add(&ctx->panel);

	ret = mipi_dsi_attach(dsi);
	if (ret < 0) {
		dev_err_probe(dev, ret, "mipi_dsi_attach failed\n");
		drm_panel_remove(&ctx->panel);
		return ret;
	}

	return 0;
}

static void st77922_remove(struct mipi_dsi_device *dsi)
{
	struct st77922 *ctx = mipi_dsi_get_drvdata(dsi);
	int ret;

	ret = mipi_dsi_detach(dsi);
	if (ret < 0)
		dev_err(&dsi->dev, "Failed to detach from DSI host: %d\n", ret);

	drm_panel_remove(&ctx->panel);
}

static const struct of_device_id st77922_of_match[] = {
	{ .compatible = "sitronix,st77922", .data = &st77922_desc },
	{ /* sentinel */ }
};
MODULE_DEVICE_TABLE(of, st77922_of_match);

static struct mipi_dsi_driver st77922_driver = {
	.probe	= st77922_probe,
	.remove = st77922_remove,
	.driver = {
		.name = DRV_NAME,
		.of_match_table = st77922_of_match,
	},
};
module_mipi_dsi_driver(st77922_driver);

MODULE_AUTHOR("James Turton <james.turton@gmx.com>");
MODULE_DESCRIPTION("DRM driver for Sitronix ST77922 based MIPI DSI panels");
MODULE_LICENSE("GPL");
