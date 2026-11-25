package com.gewikiprice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which of the two OSRS Wiki real-time prices to base an offer on.
 * The wiki labels "high" as the Buy price and "low" as the Sell price.
 */
@Getter
@RequiredArgsConstructor
public enum WikiPriceType
{
	BUY_PRICE("Wiki buy price (instant buy / high)"),
	SELL_PRICE("Wiki sell price (instant sell / low)");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}
