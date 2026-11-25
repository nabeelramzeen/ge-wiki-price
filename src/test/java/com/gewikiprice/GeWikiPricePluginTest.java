package com.gewikiprice;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches RuneLite with this plugin loaded, for local testing: ./gradlew run
 */
public class GeWikiPricePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GeWikiPricePlugin.class);
		RuneLite.main(args);
	}
}
