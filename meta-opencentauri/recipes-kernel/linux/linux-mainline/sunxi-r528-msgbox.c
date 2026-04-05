// SPDX-License-Identifier: GPL-2.0
//
// Copyright (c) 2026 James Turton <james.turton@gmx.com>
//
// Allwinner R528/D1/T113 Message Box driver
//
// The R528 MSGBOX uses a different register layout from the older sun6i MSGBOX.
// Key architectural differences:
//
//   - Two separate MSGBOX instances exist: one where ARM is the local (receiving)
//     side, and one where DSP is the local (receiving) side.
//   - Each instance has 4 unidirectional channels with 8-deep 32-bit FIFOs.
//   - Separate RX_IRQ and TX_IRQ register banks (instead of combined LOCAL/REMOTE).
//   - Direction is architecturally fixed (not configurable via CTRL register).
//
// From the ARM Linux perspective:
//   - "local" regs  (ARM CPUX_MSGBOX @ 0x03003000): DSP writes, ARM reads (RX)
//   - "remote" regs (DSP_MSGBOX      @ 0x01701000): ARM writes, DSP reads (TX)
//
// The driver exposes 8 channels to the mailbox framework:
//   - Channels 0-3: RX (receive from DSP), backed by local MSGBOX
//   - Channels 4-7: TX (transmit to DSP),  backed by remote MSGBOX
//

#include <linux/bitops.h>
#include <linux/clk.h>
#include <linux/device.h>
#include <linux/err.h>
#include <linux/interrupt.h>
#include <linux/io.h>
#include <linux/kernel.h>
#include <linux/mailbox_controller.h>
#include <linux/module.h>
#include <linux/of.h>
#include <linux/of_irq.h>
#include <linux/platform_device.h>
#include <linux/reset.h>
#include <linux/spinlock.h>

#define NUM_CHANS_PER_DIR	4
#define NUM_CHANS			(NUM_CHANS_PER_DIR * 2) /* 4 RX + 4 TX */

/*
 * Register offsets within each MSGBOX instance.
 * N = remote CPU index (always 0 for R528, only one remote CPU).
 * P = channel index (0-3).
 */
#define RX_IRQ_EN_REG		0x0020	/* N=0: 0x0020 */
#define RX_IRQ_STATUS_REG	0x0024
#define TX_IRQ_EN_REG		0x0030
#define TX_IRQ_STATUS_REG	0x0034
#define FIFO_STATUS_REG(p)	(0x0050 + (p) * 0x4)
#define MSG_STATUS_REG(p)	(0x0060 + (p) * 0x4)
#define MSG_DATA_REG(p)		(0x0070 + (p) * 0x4)
#define TX_INT_THRES_REG(p)	(0x0080 + (p) * 0x4)

/*
 * RX_IRQ_EN / RX_IRQ_STATUS bit layout:
 *   Bit 0: Channel 0 reception IRQ
 *   Bit 2: Channel 1 reception IRQ
 *   Bit 4: Channel 2 reception IRQ
 *   Bit 6: Channel 3 reception IRQ
 */
#define RX_IRQ(p)		BIT((p) * 2)
#define RX_IRQ_MASK		(RX_IRQ(0) | RX_IRQ(1) | RX_IRQ(2) | RX_IRQ(3))

/*
 * TX_IRQ_EN / TX_IRQ_STATUS bit layout:
 *   Bit 1: Channel 0 transmit IRQ
 *   Bit 3: Channel 1 transmit IRQ
 *   Bit 5: Channel 2 transmit IRQ
 *   Bit 7: Channel 3 transmit IRQ
 */
#define TX_IRQ(p)		BIT((p) * 2 + 1)
#define TX_IRQ_MASK		(TX_IRQ(0) | TX_IRQ(1) | TX_IRQ(2) | TX_IRQ(3))

/* MSG_STATUS_REG: bits [3:0] = number of unread messages (0-8) */
#define MSG_STATUS_MASK		GENMASK(3, 0)

/* FIFO_STATUS_REG: bit 0 = FIFO not available (full) */
#define FIFO_NOT_AVA_FLAG	BIT(0)

#define mbox_dbg(mbox, ...)	dev_dbg((mbox)->controller.dev, __VA_ARGS__)

struct sunxi_r528_msgbox {
	struct mbox_controller	controller;
	struct clk		*clk;
	spinlock_t		lock;
	void __iomem	*local_regs;	/* ARM CPUX_MSGBOX (RX from DSP) */
	void __iomem	*remote_regs;	/* DSP_MSGBOX (TX to DSP) */
};

static bool sunxi_r528_msgbox_peek_data(struct mbox_chan *chan);

static inline int channel_number(struct mbox_chan *chan)
{
	return chan - chan->mbox->chans;
}

static inline bool is_rx_channel(int n)
{
	return n < NUM_CHANS_PER_DIR;
}

static inline int channel_index(int n)
{
	/* Map unified channel number to per-direction index (0-3) */
	return n % NUM_CHANS_PER_DIR;
}

static inline struct sunxi_r528_msgbox *to_sunxi_r528_msgbox(struct mbox_chan *chan)
{
	return chan->con_priv;
}

/*
 * Get the appropriate register base for a channel.
 * RX channels (0-3) use local_regs, TX channels (4-7) use remote_regs.
 */
static inline void __iomem *chan_regs(struct sunxi_r528_msgbox *mbox, int n)
{
	return is_rx_channel(n) ? mbox->local_regs : mbox->remote_regs;
}

static irqreturn_t sunxi_r528_msgbox_irq(int irq, void *dev_id)
{
	struct sunxi_r528_msgbox *mbox = dev_id;
	uint32_t status;
	int p;

	/* Only examine channels that are currently enabled. */
	status = readl(mbox->local_regs + RX_IRQ_EN_REG) &
		 readl(mbox->local_regs + RX_IRQ_STATUS_REG);

	if (!(status & RX_IRQ_MASK))
		return IRQ_NONE;

	for (p = 0; p < NUM_CHANS_PER_DIR; ++p) {
		struct mbox_chan *chan = &mbox->controller.chans[p]; /* RX chan */

		if (!(status & RX_IRQ(p)))
			continue;

		while (sunxi_r528_msgbox_peek_data(chan)) {
			uint32_t msg = readl(mbox->local_regs + MSG_DATA_REG(p));

			mbox_dbg(mbox, "RX channel %d received 0x%08x\n", p, msg);
			mbox_chan_received_data(chan, &msg);
		}

		/* The RX IRQ pending can be cleared only once the FIFO is empty. */
		writel(RX_IRQ(p), mbox->local_regs + RX_IRQ_STATUS_REG);
	}

	return IRQ_HANDLED;
}

static int sunxi_r528_msgbox_send_data(struct mbox_chan *chan, void *data)
{
	struct sunxi_r528_msgbox *mbox = to_sunxi_r528_msgbox(chan);
	int n = channel_number(chan);
	int p = channel_index(n);
	uint32_t msg = *(uint32_t *)data;

	/* Sanity check: this must be a TX channel. */
	if (WARN_ON_ONCE(is_rx_channel(n)))
		return -EINVAL;

	/* Check that the FIFO is not full before writing. */
	if (readl(mbox->remote_regs + FIFO_STATUS_REG(p)) & FIFO_NOT_AVA_FLAG) {
		mbox_dbg(mbox, "Channel %d FIFO full\n", p);
		return -EBUSY;
	}

	writel(msg, mbox->remote_regs + MSG_DATA_REG(p));
	mbox_dbg(mbox, "Channel %d sent 0x%08x\n", p, msg);

	return 0;
}

static int sunxi_r528_msgbox_startup(struct mbox_chan *chan)
{
	struct sunxi_r528_msgbox *mbox = to_sunxi_r528_msgbox(chan);
	int n = channel_number(chan);
	int p = channel_index(n);

	if (is_rx_channel(n)) {
		/* Flush the receive FIFO. */
		while (sunxi_r528_msgbox_peek_data(chan))
			readl(mbox->local_regs + MSG_DATA_REG(p));

		/* Clear any stale pending IRQ. */
		writel(RX_IRQ(p), mbox->local_regs + RX_IRQ_STATUS_REG);

		/* Enable the receive IRQ. */
		spin_lock(&mbox->lock);
		writel(readl(mbox->local_regs + RX_IRQ_EN_REG) | RX_IRQ(p),
		       mbox->local_regs + RX_IRQ_EN_REG);
		spin_unlock(&mbox->lock);
	}

	mbox_dbg(mbox, "Channel %d startup complete\n", n);

	return 0;
}

static void sunxi_r528_msgbox_shutdown(struct mbox_chan *chan)
{
	struct sunxi_r528_msgbox *mbox = to_sunxi_r528_msgbox(chan);
	int n = channel_number(chan);
	int p = channel_index(n);

	if (is_rx_channel(n)) {
		/* Disable the receive IRQ. */
		spin_lock(&mbox->lock);
		writel(readl(mbox->local_regs + RX_IRQ_EN_REG) & ~RX_IRQ(p),
		       mbox->local_regs + RX_IRQ_EN_REG);
		spin_unlock(&mbox->lock);

		/* Attempt to flush the FIFO until the IRQ is cleared. */
		do {
			while (sunxi_r528_msgbox_peek_data(chan))
				readl(mbox->local_regs + MSG_DATA_REG(p));
			writel(RX_IRQ(p), mbox->local_regs + RX_IRQ_STATUS_REG);
		} while (readl(mbox->local_regs + RX_IRQ_STATUS_REG) & RX_IRQ(p));
	}

	mbox_dbg(mbox, "Channel %d shutdown complete\n", n);
}

static bool sunxi_r528_msgbox_last_tx_done(struct mbox_chan *chan)
{
	struct sunxi_r528_msgbox *mbox = to_sunxi_r528_msgbox(chan);
	int n = channel_number(chan);
	int p = channel_index(n);

	if (is_rx_channel(n))
		return true;

	/*
	 * A TX is considered done when the FIFO in the remote MSGBOX is empty,
	 * meaning the DSP has read all messages. This is checked via the
	 * MSG_STATUS register which reports the number of unread messages.
	 *
	 * An alternative approach (as in sun6i) would be to snoop the remote's
	 * RX_IRQ_STATUS, but that requires the remote side to use interrupts
	 * and clear the pending bit. Checking MSG_STATUS is more universally
	 * correct regardless of how the remote firmware handles reception.
	 */
	return !(readl(mbox->remote_regs + MSG_STATUS_REG(p)) & MSG_STATUS_MASK);
}

static bool sunxi_r528_msgbox_peek_data(struct mbox_chan *chan)
{
	struct sunxi_r528_msgbox *mbox = to_sunxi_r528_msgbox(chan);
	int n = channel_number(chan);
	int p = channel_index(n);

	if (!is_rx_channel(n))
		return false;

	return readl(mbox->local_regs + MSG_STATUS_REG(p)) & MSG_STATUS_MASK;
}

static const struct mbox_chan_ops sunxi_r528_msgbox_chan_ops = {
	.send_data    = sunxi_r528_msgbox_send_data,
	.startup      = sunxi_r528_msgbox_startup,
	.shutdown     = sunxi_r528_msgbox_shutdown,
	.last_tx_done = sunxi_r528_msgbox_last_tx_done,
	.peek_data    = sunxi_r528_msgbox_peek_data,
};

static int sunxi_r528_msgbox_probe(struct platform_device *pdev)
{
	struct device *dev = &pdev->dev;
	struct mbox_chan *chans;
	struct reset_control *reset;
	struct sunxi_r528_msgbox *mbox;
	int i, ret;

	mbox = devm_kzalloc(dev, sizeof(*mbox), GFP_KERNEL);
	if (!mbox)
		return -ENOMEM;

	chans = devm_kcalloc(dev, NUM_CHANS, sizeof(*chans), GFP_KERNEL);
	if (!chans)
		return -ENOMEM;

	for (i = 0; i < NUM_CHANS; ++i)
		chans[i].con_priv = mbox;

	mbox->clk = devm_clk_get(dev, NULL);
	if (IS_ERR(mbox->clk)) {
		ret = PTR_ERR(mbox->clk);
		dev_err(dev, "Failed to get clock: %d\n", ret);
		return ret;
	}

	ret = clk_prepare_enable(mbox->clk);
	if (ret) {
		dev_err(dev, "Failed to enable clock: %d\n", ret);
		return ret;
	}

	reset = devm_reset_control_get_exclusive(dev, NULL);
	if (IS_ERR(reset)) {
		ret = PTR_ERR(reset);
		dev_err(dev, "Failed to get reset control: %d\n", ret);
		goto err_disable_unprepare;
	}

	/*
	 * We share this hardware with the DSP firmware, so we only deassert
	 * the reset and do not assert it on removal or probe failure.
	 */
	ret = reset_control_deassert(reset);
	if (ret) {
		dev_err(dev, "Failed to deassert reset: %d\n", ret);
		goto err_disable_unprepare;
	}

	/*
	 * Map the two MSGBOX register regions:
	 *   - index 0 ("local"):  ARM CPUX_MSGBOX for receiving from DSP
	 *   - index 1 ("remote"): DSP_MSGBOX for transmitting to DSP
	 */
	mbox->local_regs = devm_platform_ioremap_resource(pdev, 0);
	if (IS_ERR(mbox->local_regs)) {
		ret = PTR_ERR(mbox->local_regs);
		dev_err(dev, "Failed to map local MMIO resource: %d\n", ret);
		return ret;
	}

	mbox->remote_regs = devm_platform_ioremap_resource(pdev, 1);
	if (IS_ERR(mbox->remote_regs)) {
		ret = PTR_ERR(mbox->remote_regs);
		dev_err(dev, "Failed to map remote MMIO resource: %d\n", ret);
		return ret;
	}

	/* Disable all IRQs for this end of the msgbox. */
	writel(0, mbox->local_regs + RX_IRQ_EN_REG);

	ret = devm_request_irq(dev, irq_of_parse_and_map(dev->of_node, 0),
			       sunxi_r528_msgbox_irq, 0, dev_name(dev), mbox);
	if (ret) {
		dev_err(dev, "Failed to register IRQ handler: %d\n", ret);
		goto err_disable_unprepare;
	}

	mbox->controller.dev           = dev;
	mbox->controller.ops           = &sunxi_r528_msgbox_chan_ops;
	mbox->controller.chans         = chans;
	mbox->controller.num_chans     = NUM_CHANS;
	mbox->controller.txdone_irq    = false;
	mbox->controller.txdone_poll   = true;
	mbox->controller.txpoll_period = 5;

	spin_lock_init(&mbox->lock);
	platform_set_drvdata(pdev, mbox);

	ret = mbox_controller_register(&mbox->controller);
	if (ret) {
		dev_err(dev, "Failed to register controller: %d\n", ret);
		goto err_disable_unprepare;
	}

	dev_info(dev, "Registered %d channels (4 RX + 4 TX)\n", NUM_CHANS);

	return 0;

err_disable_unprepare:
	clk_disable_unprepare(mbox->clk);

	return ret;
}

static int sunxi_r528_msgbox_remove(struct platform_device *pdev)
{
	struct sunxi_r528_msgbox *mbox = platform_get_drvdata(pdev);

	mbox_controller_unregister(&mbox->controller);
	/* See the comment in sunxi_r528_msgbox_probe about the reset line. */
	clk_disable_unprepare(mbox->clk);

	return 0;
}

static const struct of_device_id sunxi_r528_msgbox_of_match[] = {
	{ .compatible = "allwinner,sunxi-r528-msgbox", },
	{},
};
MODULE_DEVICE_TABLE(of, sunxi_r528_msgbox_of_match);

static struct platform_driver sunxi_r528_msgbox_driver = {
	.driver = {
		.name = "sunxi-r528-msgbox",
		.of_match_table = sunxi_r528_msgbox_of_match,
	},
	.probe  = sunxi_r528_msgbox_probe,
	.remove = sunxi_r528_msgbox_remove,
};
module_platform_driver(sunxi_r528_msgbox_driver);

MODULE_AUTHOR("James Turton <james.turton@gmx.com>");
MODULE_DESCRIPTION("Allwinner R528/D1/T113 Message Box");
MODULE_LICENSE("GPL v2");
