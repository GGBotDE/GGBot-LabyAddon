package de.ggbot.core.gui.shop.utils;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.VersioningHandler;
import de.ggbot.sdk.api.ModulesApi;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.PublicBot;
import de.ggbot.sdk.model.SellItem;
import de.ggbot.sdk.model.SellItemPrice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shared cache for a bot's public data and sell items, keyed by bot + server.
 *
 * <p>The sell items are fetched early (as soon as the shop hint spots a nearby
 * bot) so that:
 * <ul>
 *   <li>the hint can stay hidden for bots without any sell items,</li>
 *   <li>the shop GUI opens instantly instead of re-doing the request, and</li>
 *   <li>a hint fetch followed shortly by a GUI open results in one request,
 *       not two (in-flight fetches are deduplicated per key).</li>
 * </ul>
 *
 * <p>Cached data is valid for {@link #GUI_MAX_AGE_MS} when opening the GUI
 * (prices shown for purchase should be fresh) and for the longer
 * {@link #HINT_MAX_AGE_MS} when only deciding whether to show the hint.
 */
public final class ShopDataCache {

  /** Maximum age of a cache entry the shop GUI may reuse without refetching. */
  public static final long GUI_MAX_AGE_MS = 60_000L;

  /** Maximum age of a cache entry the shop hint may reuse. */
  public static final long HINT_MAX_AGE_MS = 5 * 60_000L;

  /** Feature key whose flag and compat routing gate the fetch. */
  private static final String FEATURE = "de.ggbot.addon.shop.fetchitems";

  /** One cached fetch result for a bot on a server. */
  public static final class Entry {
    private final PublicBot publicBot;
    private final List<CustomSellItem> sellItems;
    private final long fetchedAtMs;

    private Entry(PublicBot publicBot, List<CustomSellItem> sellItems) {
      this.publicBot = publicBot;
      this.sellItems = Collections.unmodifiableList(sellItems);
      this.fetchedAtMs = System.currentTimeMillis();
    }

    /** Returns the bot's public data. */
    public PublicBot getPublicBot() { return publicBot; }

    /** Returns the bot's sell items; empty when the bot sells nothing. */
    public List<CustomSellItem> getSellItems() { return sellItems; }

    /** Returns the age of this entry in milliseconds. */
    public long ageMs() { return System.currentTimeMillis() - fetchedAtMs; }
  }

  /**
   * How long a failed fetch blocks further attempts for the same key. The shop
   * hint re-asks on every frame, so without this a permanently failing bot
   * would result in one request after the other.
   */
  private static final long FAILURE_COOLDOWN_MS = 30_000L;

  private static final Map<String, Entry> entries = new HashMap<>();
  private static final Map<String, List<Consumer<Entry>>> pendingCallbacks = new HashMap<>();
  private static final Map<String, Long> lastFailureMs = new HashMap<>();

  private ShopDataCache() {
  }

  private static String key(String botName, String serverIp) {
    return botName.toLowerCase(Locale.ROOT) + "@" + serverIp.toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the cached entry for a bot if it is younger than {@code maxAgeMs}.
   *
   * @param botName  the bot's player name
   * @param serverIp the normalized server host
   * @param maxAgeMs maximum acceptable entry age in milliseconds
   * @return the entry, or {@code null} if absent or too old
   */
  public static synchronized Entry get(String botName, String serverIp, long maxAgeMs) {
    Entry entry = entries.get(key(botName, serverIp));
    if (entry == null || entry.ageMs() > maxAgeMs) return null;
    return entry;
  }

  /**
   * Fetches a bot's public data and sell items in the background and caches
   * the result. Concurrent fetches for the same bot/server are deduplicated:
   * additional callers just queue their callback on the running request.
   *
   * @param versioningHandler used for the feature flag and base URL routing
   * @param botName           the bot's player name
   * @param serverIp          the normalized server host
   * @param callback          invoked with the fetched entry, or {@code null}
   *                          on failure; may itself be {@code null} for
   *                          fire-and-forget prefetches. Runs on the fetch
   *                          thread.
   */
  public static void fetchAsync(VersioningHandler versioningHandler, String botName,
      String serverIp, Consumer<Entry> callback) {
    if (!versioningHandler.isFeatureEnabled(FEATURE)) {
      if (callback != null) callback.accept(null);
      return;
    }

    String key = key(botName, serverIp);
    boolean inCooldown = false;
    synchronized (ShopDataCache.class) {
      List<Consumer<Entry>> callbacks = pendingCallbacks.get(key);
      if (callbacks != null) {
        // A fetch is already running; just queue the callback.
        if (callback != null) callbacks.add(callback);
        return;
      }
      Long failedAt = lastFailureMs.get(key);
      if (failedAt != null && System.currentTimeMillis() - failedAt < FAILURE_COOLDOWN_MS) {
        inCooldown = true;
      } else {
        lastFailureMs.remove(key);
        callbacks = new ArrayList<>();
        if (callback != null) callbacks.add(callback);
        pendingCallbacks.put(key, callbacks);
      }
    }
    if (inCooldown) {
      if (callback != null) callback.accept(null);
      return;
    }

    Thread thread = new Thread(() -> {
      Entry entry = null;
      try {
        entry = doFetch(versioningHandler, botName, serverIp);
      } catch (RuntimeException e) {
        GGBot.getInstance().logger().error(
            "Failed to fetch shop data for bot: " + botName + " on server: " + serverIp, e);
        GGBot.getInstance().getVersioningHandler().reportError(e);
      } finally {
        // Always hand the queued callers a result: a fetch that leaves its
        // entry in the pending map would block every later fetch for this key.
        List<Consumer<Entry>> callbacks;
        synchronized (ShopDataCache.class) {
          if (entry != null) {
            entries.put(key, entry);
            lastFailureMs.remove(key);
          } else {
            lastFailureMs.put(key, System.currentTimeMillis());
          }
          callbacks = pendingCallbacks.remove(key);
        }
        if (callbacks != null) {
          for (Consumer<Entry> queued : callbacks) {
            queued.accept(entry);
          }
        }
      }
    }, "ggbot-shop-data-fetch");
    thread.setDaemon(true);
    thread.start();
  }

  /** Performs the actual API calls; returns {@code null} on failure. */
  private static Entry doFetch(VersioningHandler versioningHandler, String botName,
      String serverIp) {
    try {
      PublicApi publicApi = new PublicApi();
      ModulesApi modulesApi = new ModulesApi();
      String baseUrl = versioningHandler.getBaseUrlForFeature(FEATURE);
      publicApi.setCustomBaseUrl(baseUrl);
      modulesApi.setCustomBaseUrl(baseUrl);

      PublicBot publicBot = publicApi.getPublicBotByLink(botName, serverIp);
      if (publicBot == null || publicBot.getToken() == null) return null;

      List<CustomSellItem> sellItems = new ArrayList<>();
      for (SellItem item : modulesApi.getPublicSellItems(publicBot.getToken())) {
        for (SellItemPrice price : item.getPrices()) {
          sellItems.add(new CustomSellItem(
              item.getId(),
              item.getName(),
              item.getItemType(),
              item.getNbt(),
              price.getPrice(),
              price.getAmount(),
              item.getChestPosition()
          ));
        }
      }
      return new Entry(publicBot, sellItems);
    } catch (ApiException e) {
      GGBot.getInstance().logger().error(
          "Failed to fetch shop data for bot: " + botName + " on server: " + serverIp, e);
      GGBot.getInstance().getVersioningHandler().reportError(e);
      return null;
    }
  }
}
