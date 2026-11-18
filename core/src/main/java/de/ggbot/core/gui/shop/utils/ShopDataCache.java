package de.ggbot.core.gui.shop.utils;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.ApiParsers;
import de.ggbot.core.api.VersioningHandler;
import de.ggbot.sdk.api.ModulesApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.SellArea;
import de.ggbot.sdk.model.SellItem;
import de.ggbot.sdk.model.SellItemPrice;
import net.labymod.api.Laby;
import net.labymod.api.client.entity.player.ClientPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Maintains a shared cache of each bot's sell items and sell area, identified
 * by the combination of bot and server.
 * 
 * <p>Callers pass the bot token from the public server bot list. Resolving it
 * again by link name is not reliable: a link name can point at more than one bot
 * record, and the lookup may answer with an offline one that has no items.
 *
 * <p>The sell items are fetched early (as soon as the shop hint spots a nearby
 * bot) so that:
 * <ul>
 *   <li>Hints can be suppressed for bots that have no sell items.</li>
 *   <li>The shop GUI can display its contents immediately without making
 *       another API request.</li>
 *   <li>Requests triggered by the hint and a subsequent GUI opening are
 *       shared, avoiding duplicate calls through per-key in-flight
 *       request deduplication.</li>
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
    private final List<CustomSellItem> sellItems;
    private final SellArea sellArea;
    private final long fetchedAtMs;

    private Entry(List<CustomSellItem> sellItems, SellArea sellArea) {
      this.sellItems = Collections.unmodifiableList(sellItems);
      this.sellArea = sellArea;
      this.fetchedAtMs = System.currentTimeMillis();
    }

    /** Returns the bot's sell items; empty when the bot sells nothing. */
    public List<CustomSellItem> getSellItems() { return sellItems; }

    /**
     * Provides the sell area region where buyers must stand, or null if unrestricted.
     *
     * @return the sell area region or null if unrestricted
     */
    public SellArea getSellArea() { return sellArea; }

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
   * Retrieves or fetches a cached entry if it's within the acceptable age.
   *
   * @param versioningHandler used for feature flags and base URL routing
   * @param botName the bot's player name
   * @param serverIp the normalized server host
   * @param botToken the bot token from the public server bot list
   * @param maxAgeMs maximum acceptable age of a reused entry
   * @param callback invoked with the entry, or null on failure
   */
  public static void getOrFetch(VersioningHandler versioningHandler, String botName,
      String serverIp, String botToken, long maxAgeMs, Consumer<Entry> callback) {
    Entry cached = get(botName, serverIp, maxAgeMs);
    if (cached != null) {
      callback.accept(cached);
      return;
    }
    fetchAsync(versioningHandler, botName, serverIp, botToken, callback);
  }

  /**
   * Checks if the player is within the bot's sell area.
   *
   * @param entry the cached entry, may be null
   * @return true when buying from this bot is possible from here
   */
  public static boolean playerInSellArea(Entry entry) {
    SellArea area = entry == null ? null : entry.getSellArea();
    if (area == null || area.getStart() == null || area.getEnd() == null) {
      return true;
    }
    ClientPlayer player = Laby.labyAPI().minecraft().getClientPlayer();
    if (player == null) {
      return false;
    }
    return within(player.position().getX(), area.getStart().getX(), area.getEnd().getX())
        && within(player.position().getY(), area.getStart().getY(), area.getEnd().getY())
        && within(player.position().getZ(), area.getStart().getZ(), area.getEnd().getZ());
  }

  /**
   * Ensures the player's position is within the specified block coordinates range.
   *
   * @param value the player's coordinate value
   * @param from the lower bound of the coordinate range
   * @param to the upper bound of the coordinate range
   * @return true if the value is within the coordinate range
   */
  private static boolean within(double value, Long from, Long to) {
    if (from == null || to == null) {
      return true;
    }
    return value >= Math.min(from, to) && value < Math.max(from, to) + 1;
  }

  /**
   * Retrieves a bot's public information and sell items asynchronously, then
   * stores the results in the cache. Simultaneous requests for the same bot
   * and server share a single running fetch, with subsequent callers registering
   * their callbacks to receive the result when the request completes.
   *
   * @param versioningHandler used for feature flags and base URL routing
   * @param botName the bot's player name
   * @param serverIp the normalized server host
   * @param botToken the bot token from the public server bot list
   * @param callback invoked with the fetched entry, or null on failure
   */
  public static void fetchAsync(VersioningHandler versioningHandler, String botName,
      String serverIp, String botToken, Consumer<Entry> callback) {
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
        entry = doFetch(versioningHandler, botName, serverIp, botToken);
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
      String serverIp, String botToken) {
    if (botToken == null) return null;
    try {
      ModulesApi modulesApi = new ModulesApi();
      modulesApi.setCustomBaseUrl(versioningHandler.getBaseUrlForFeature(FEATURE));

      List<CustomSellItem> sellItems = new ArrayList<>();
      for (SellItem item : modulesApi.getPublicSellItems(botToken)) {
        for (SellItemPrice price : item.getPrices()) {
          sellItems.add(new CustomSellItem(
              item.getId(),
              item.getName(),
              ApiParsers.itemType(item.getItemKey(), item.getFallbackKey(), item.getItemType()),
              item.getNbt(),
              price.getPrice(),
              price.getAmount(),
              item.getChestPosition()
          ));
        }
      }
      SellArea sellArea = null;
      try {
        sellArea = modulesApi.getSellArea(botToken);
      } catch (Exception ignored) {
        // Handles the case where a bot does not have a sell area, resulting in a 404 response.
      }
      return new Entry(sellItems, sellArea);
    } catch (ApiException e) {
      GGBot.getInstance().logger().error(
          "Failed to fetch shop data for bot: " + botName + " on server: " + serverIp, e);
      GGBot.getInstance().getVersioningHandler().reportError(e);
      return null;
    }
  }
}
