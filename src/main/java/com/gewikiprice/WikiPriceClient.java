package com.gewikiprice;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches real-time prices from the OSRS Wiki prices API
 * (https://prices.runescape.wiki/api/v1/osrs/latest) and caches them per item.
 */
@Slf4j
@Singleton
public class WikiPriceClient
{
	private static final HttpUrl LATEST_URL = HttpUrl.get("https://prices.runescape.wiki/api/v1/osrs/latest");

	// The wiki asks API users to send a descriptive User-Agent.
	private static final String USER_AGENT = "RuneLite GE Wiki Price plugin";

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final Map<Integer, WikiPrice> cache = new ConcurrentHashMap<>();

	@Inject
	WikiPriceClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	/**
	 * Returns the cached price for an item if it was fetched within {@code maxAge}, else null.
	 */
	@Nullable
	public WikiPrice getCached(int itemId, Duration maxAge)
	{
		WikiPrice price = cache.get(itemId);
		if (price == null || price.getFetchedAt().plus(maxAge).isBefore(Instant.now()))
		{
			return null;
		}
		return price;
	}

	public void clearCache()
	{
		cache.clear();
	}

	/**
	 * Fetches the latest price for one item in the background. The callback runs on an
	 * OkHttp thread, and only when a price was received.
	 */
	public void fetch(int itemId, Consumer<WikiPrice> callback)
	{
		HttpUrl url = LATEST_URL.newBuilder()
			.addQueryParameter("id", Integer.toString(itemId))
			.build();

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to fetch wiki price for item {}", itemId, e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						log.debug("Wiki price request for item {} returned HTTP {}", itemId, response.code());
						return;
					}

					LatestResponse latest = gson.fromJson(body.charStream(), LatestResponse.class);
					LatestEntry entry = latest == null || latest.data == null
						? null
						: latest.data.get(Integer.toString(itemId));
					if (entry == null || (entry.high == null && entry.low == null))
					{
						log.debug("Wiki has no price for item {}", itemId);
						return;
					}

					WikiPrice price = new WikiPrice(entry.high, entry.low, Instant.now());
					cache.put(itemId, price);
					callback.accept(price);
				}
				catch (JsonParseException e)
				{
					log.debug("Unable to parse wiki price for item {}", itemId, e);
				}
			}
		});
	}

	/** Shape of the /latest response: {"data": {"4151": {"high": .., "highTime": .., "low": .., "lowTime": ..}}} */
	private static class LatestResponse
	{
		Map<String, LatestEntry> data;
	}

	private static class LatestEntry
	{
		Integer high;
		Long highTime;
		Integer low;
		Long lowTime;
	}
}
