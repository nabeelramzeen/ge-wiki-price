package com.gewikiprice;

import com.google.inject.Provides;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.QuantityFormatter;

/**
 * When you click "..." to enter a price on a Grand Exchange offer, this plugin pre-fills the
 * price prompt with the OSRS Wiki real-time price: +1% for buy offers, -1% for sell offers (configurable).
 * You still confirm the price yourself by pressing Enter (or edit it first).
 */
@Slf4j
@PluginDescriptor(
	name = "GE Wiki Price",
	description = "Pre-fills the Grand Exchange price input with the OSRS Wiki real-time price +1% when buying and -1% when selling",
	tags = {"grand", "exchange", "ge", "wiki", "price", "offer", "flip"}
)
public class GeWikiPricePlugin extends Plugin
{
	/** VarClientID.MESLAYERMODE values for numeric chatbox prompts. */
	private static final int INPUT_MODE_INT = 7;
	private static final int INPUT_MODE_LONG = 30;

	/** Title the game shows on the GE price prompt. */
	private static final String PRICE_PROMPT_TITLE = "Set a price for each item:";

	/** GE_NEWOFFER_TYPE varbit value for a sell offer (0 is a buy offer). */
	private static final int OFFER_TYPE_SELL = 1;

	/** Cached prices older than this are re-fetched when the prompt opens. */
	private static final Duration MAX_PRICE_AGE = Duration.ofSeconds(30);

	/** How many client frames to wait for the prompt title to appear. */
	private static final int MAX_PROMPT_WAIT_FRAMES = 10;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private GeWikiPriceConfig config;

	@Inject
	private WikiPriceClient wikiPriceClient;

	@Provides
	GeWikiPriceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GeWikiPriceConfig.class);
	}

	@Override
	protected void shutDown()
	{
		wikiPriceClient.clearCache();
	}

	/**
	 * Prefetch the wiki price as soon as an item is chosen in the GE offer screen,
	 * so it is usually ready by the time the price prompt is opened.
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarpId() != VarPlayerID.TRADINGPOST_SEARCH)
		{
			return;
		}

		int itemId = event.getValue();
		if (itemId > 0 && wikiPriceClient.getCached(itemId, MAX_PRICE_AGE) == null)
		{
			wikiPriceClient.fetch(itemId, price -> { });
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() != VarClientID.MESLAYERMODE)
		{
			return;
		}

		int mode = client.getVarcIntValue(VarClientID.MESLAYERMODE);
		if (mode != INPUT_MODE_INT && mode != INPUT_MODE_LONG)
		{
			return;
		}

		// The prompt's title is set by a script after the input mode changes,
		// so check over the next few frames.
		int[] frames = {0};
		clientThread.invokeLater(() ->
		{
			if (isPricePromptOpen())
			{
				onPricePromptOpened();
				return true;
			}
			return ++frames[0] >= MAX_PROMPT_WAIT_FRAMES;
		});
	}

	private void onPricePromptOpened()
	{
		int itemId = client.getVarpValue(VarPlayerID.TRADINGPOST_SEARCH);
		if (itemId <= 0)
		{
			return;
		}

		boolean selling = isSellOffer();
		WikiPrice cached = wikiPriceClient.getCached(itemId, MAX_PRICE_AGE);
		if (cached != null)
		{
			applyPrice(itemId, selling, cached);
			return;
		}

		wikiPriceClient.fetch(itemId, price -> clientThread.invokeLater(() -> applyPrice(itemId, selling, price)));
	}

	/**
	 * Writes the adjusted price into the open price prompt. Must run on the client thread.
	 * Does nothing if the prompt was closed, the offer changed, or the player already typed.
	 */
	private void applyPrice(int itemId, boolean selling, WikiPrice price)
	{
		if (!isPricePromptOpen()
			|| client.getVarpValue(VarPlayerID.TRADINGPOST_SEARCH) != itemId
			|| isSellOffer() != selling)
		{
			return;
		}

		String typed = client.getVarcStrValue(VarClientID.MESLAYERINPUT);
		if (typed != null && !typed.isEmpty())
		{
			return;
		}

		WikiPriceType type = selling ? config.sellPriceType() : config.buyPriceType();
		// Sell offers go below the wiki price, buy offers go above it.
		double percentChange = selling ? -config.sellPercentBelow() : config.buyPercentAbove();
		Integer wikiPrice = price.get(type);
		if (wikiPrice == null || wikiPrice <= 0)
		{
			return;
		}

		int offerPrice = adjustPrice(wikiPrice, percentChange);
		String text = Integer.toString(offerPrice);

		client.setVarcStrValue(VarClientID.MESLAYERINPUT, text);
		Widget input = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		if (input != null)
		{
			input.setText(text + "*");
		}

		if (config.showWikiPrice())
		{
			Widget title = client.getWidget(InterfaceID.Chatbox.MES_TEXT);
			if (title != null)
			{
				title.setText(PRICE_PROMPT_TITLE + " (wiki " + QuantityFormatter.formatNumber(wikiPrice)
					+ " " + (selling ? "-" : "+") + formatPercent(Math.abs(percentChange)) + "%)");
			}
		}

		log.debug("Pre-filled {} price for item {}: wiki {} -> {}", selling ? "sell" : "buy", itemId, wikiPrice, offerPrice);
	}

	private boolean isPricePromptOpen()
	{
		Widget title = client.getWidget(InterfaceID.Chatbox.MES_TEXT);
		Widget offerSetup = client.getWidget(InterfaceID.GeOffers.SETUP);
		return title != null
			&& !title.isHidden()
			&& title.getText() != null
			&& title.getText().startsWith(PRICE_PROMPT_TITLE)
			&& offerSetup != null
			&& !offerSetup.isHidden();
	}

	private boolean isSellOffer()
	{
		return client.getVarbitValue(VarbitID.GE_NEWOFFER_TYPE) == OFFER_TYPE_SELL;
	}

	/**
	 * Returns {@code price} changed by {@code percentChange} percent (negative = lower,
	 * positive = higher), rounded down, and kept within 1..Integer.MAX_VALUE.
	 * Uses BigDecimal so e.g. 1000 at -1% is exactly 990 and at +1% exactly 1010.
	 */
	static int adjustPrice(int price, double percentChange)
	{
		BigDecimal factor = BigDecimal.valueOf(100).add(BigDecimal.valueOf(percentChange));
		BigDecimal adjusted = BigDecimal.valueOf(price)
			.multiply(factor)
			.divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);

		if (adjusted.compareTo(BigDecimal.ONE) < 0)
		{
			return 1;
		}
		if (adjusted.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)
		{
			return Integer.MAX_VALUE;
		}
		return adjusted.intValue();
	}

	private static String formatPercent(double percent)
	{
		return BigDecimal.valueOf(percent).stripTrailingZeros().toPlainString();
	}
}
