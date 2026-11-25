package com.gewikiprice;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Units;

@ConfigGroup(GeWikiPriceConfig.GROUP)
public interface GeWikiPriceConfig extends Config
{
	String GROUP = "gewikiprice";

	@ConfigSection(
		name = "Buy offers",
		description = "Price used when you click \"...\" to enter a price on a buy offer",
		position = 0
	)
	String buySection = "buy";

	@ConfigSection(
		name = "Sell offers",
		description = "Price used when you click \"...\" to enter a price on a sell offer",
		position = 1
	)
	String sellSection = "sell";

	@ConfigItem(
		keyName = "buyPriceType",
		name = "Base price",
		description = "Which wiki real-time price buy offers are based on",
		section = buySection,
		position = 0
	)
	default WikiPriceType buyPriceType()
	{
		return WikiPriceType.BUY_PRICE;
	}

	@ConfigItem(
		keyName = "buyPercentAbove",
		name = "% above wiki price",
		description = "Buy offers are pre-filled this many percent above the wiki price (rounded down)",
		section = buySection,
		position = 1
	)
	@Units(Units.PERCENT)
	default double buyPercentAbove()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "sellPriceType",
		name = "Base price",
		description = "Which wiki real-time price sell offers are based on",
		section = sellSection,
		position = 0
	)
	default WikiPriceType sellPriceType()
	{
		return WikiPriceType.SELL_PRICE;
	}

	@ConfigItem(
		keyName = "sellPercentBelow",
		name = "% below wiki price",
		description = "Sell offers are pre-filled this many percent below the wiki price (rounded down)",
		section = sellSection,
		position = 1
	)
	@Units(Units.PERCENT)
	default double sellPercentBelow()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "showWikiPrice",
		name = "Show wiki price in prompt",
		description = "Adds the wiki price the pre-filled value came from to the price prompt's title",
		position = 2
	)
	default boolean showWikiPrice()
	{
		return true;
	}
}
