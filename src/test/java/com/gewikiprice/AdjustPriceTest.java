package com.gewikiprice;

import static org.junit.Assert.assertEquals;
import java.time.Instant;
import org.junit.Test;

public class AdjustPriceTest
{
	@Test
	public void sellOnePercentBelow()
	{
		assertEquals(990, GeWikiPricePlugin.adjustPrice(1000, -1.0));
		assertEquals(1_782_000, GeWikiPricePlugin.adjustPrice(1_800_000, -1.0));
	}

	@Test
	public void buyOnePercentAbove()
	{
		assertEquals(1010, GeWikiPricePlugin.adjustPrice(1000, 1.0));
		assertEquals(1_818_000, GeWikiPricePlugin.adjustPrice(1_800_000, 1.0));
	}

	@Test
	public void roundsDown()
	{
		// 1234 * 0.99 = 1221.66
		assertEquals(1221, GeWikiPricePlugin.adjustPrice(1234, -1.0));
		// 1234 * 1.01 = 1246.34
		assertEquals(1246, GeWikiPricePlugin.adjustPrice(1234, 1.0));
		// 1000 * 0.997 = 997 exactly, no floating point drift
		assertEquals(997, GeWikiPricePlugin.adjustPrice(1000, -0.3));
	}

	@Test
	public void zeroPercentKeepsPrice()
	{
		assertEquals(1234, GeWikiPricePlugin.adjustPrice(1234, 0));
	}

	@Test
	public void neverBelowOneCoin()
	{
		assertEquals(1, GeWikiPricePlugin.adjustPrice(1, -1.0));
		assertEquals(1, GeWikiPricePlugin.adjustPrice(50, -100));
	}

	@Test
	public void maxCashStack()
	{
		assertEquals(2_126_008_810, GeWikiPricePlugin.adjustPrice(Integer.MAX_VALUE, -1.0));
		assertEquals(Integer.MAX_VALUE, GeWikiPricePlugin.adjustPrice(Integer.MAX_VALUE, 1.0));
	}

	@Test
	public void fallsBackToOtherSideWhenMissing()
	{
		WikiPrice onlyLow = new WikiPrice(null, 500, Instant.now());
		assertEquals(Integer.valueOf(500), onlyLow.get(WikiPriceType.BUY_PRICE));
		assertEquals(Integer.valueOf(500), onlyLow.get(WikiPriceType.SELL_PRICE));
	}
}
