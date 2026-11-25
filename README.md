# GE Wiki Price

A RuneLite plugin that pre-fills the Grand Exchange price prompt with the
[OSRS Wiki real-time price](https://prices.runescape.wiki/), adjusted by a percentage: +1% when buying and -1% when selling by default.

## How it works

1. Open a buy or sell offer on the Grand Exchange and pick an item.
   The plugin fetches that item's latest price from the wiki API in the background.
2. Click the **"..."** button to enter a price. The prompt opens already filled with:
   - **Buy offers:** wiki *buy price* (instant-buy / "high") plus 1%, rounded down
   - **Sell offers:** wiki *sell price* (instant-sell / "low") minus 1%, rounded down
3. Press **Enter** to accept it, or Backspace to change it.

The prompt title shows the wiki price it used, e.g. `Set a price for each item: (wiki 644,258 +1%)` on a buy offer.

The plugin never confirms the price for you. That keeps it within RuneLite's Plugin Hub rules
(no automated game input).

## Settings

| Setting | Default |
| --- | --- |
| Buy offers → Base price | Wiki buy price (instant buy / high) |
| Buy offers → % above wiki price | 1.0 |
| Sell offers → Base price | Wiki sell price (instant sell / low) |
| Sell offers → % below wiki price | 1.0 |
| Show wiki price in prompt | On |

If the wiki has no recent trade on the chosen side, the other side's price is used.
If the wiki has no price for the item at all, the prompt is left empty as usual.

## Running it locally

Requires JDK 11 or newer.

```
./gradlew run        # launches RuneLite in developer mode with the plugin loaded
./gradlew test       # runs the price-rounding tests
```

Or open the folder in IntelliJ IDEA, then run `GeWikiPricePluginTest` from `src/test/java`.

If Gradle fails with `Unable to establish loopback connection` on Windows, point Java's
Unix-socket temp folder at a short path and retry:

```powershell
New-Item -ItemType Directory -Force C:\Users\Public\jtmp
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\Users\Public\jtmp"
```

If Gradle says `JAVA_HOME is not set` right after installing a JDK, open a new terminal
(the old one doesn't see the new install), or set it for the current one:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot"
```

## Files

- `GeWikiPricePlugin.java` – detects the GE price prompt and fills it in
- `WikiPriceClient.java` – calls `https://prices.runescape.wiki/api/v1/osrs/latest?id=<item>` and caches results for 30s
- `GeWikiPriceConfig.java` – plugin settings
