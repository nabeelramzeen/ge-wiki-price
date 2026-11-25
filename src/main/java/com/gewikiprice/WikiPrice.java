package com.gewikiprice;

import java.time.Instant;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One item's latest real-time prices from prices.runescape.wiki.
 */
@Value
public class WikiPrice
{
	/** Most recent instant-buy price (the wiki's "Buy price"). */
	@Nullable
	Integer high;

	/** Most recent instant-sell price (the wiki's "Sell price"). */
	@Nullable
	Integer low;

	/** When this price was fetched from the wiki. */
	Instant fetchedAt;

	/**
	 * Returns the requested price, falling back to the other one if the wiki
	 * has no recent trade on that side. Returns null if neither is known.
	 */
	@Nullable
	public Integer get(WikiPriceType type)
	{
		Integer primary = type == WikiPriceType.BUY_PRICE ? high : low;
		Integer fallback = type == WikiPriceType.BUY_PRICE ? low : high;
		return primary != null ? primary : fallback;
	}
}
