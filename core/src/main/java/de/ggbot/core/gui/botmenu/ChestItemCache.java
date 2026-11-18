package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.ApiParsers;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.BlockPosition;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.BuyItem;
import de.ggbot.sdk.model.SellItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Stores the buy and sell items of the displayed bots along with their
 * corresponding chest positions. This allows the chest overlay and item
 * search to locate items without making repeated API requests every frame.
 * Cache entries are populated directly from the SDK item models.
 */
public final class ChestItemCache {

  /**
   * Describes a bot's item entry tied to a chest block's location.
   */
  public record Entry(Long botId, String bot, String name, String shop, String itemType, String nbt,
      int x, int y, int z) {}

  private static final List<Entry> ENTRIES = new ArrayList<>();

  /**
   * Tracks cache version changes for per-frame readers.
   */
  private static volatile int version;

  private ChestItemCache() {}

  /**
   * Refreshes the cache with the latest item data for specified bots.
   */
  public static void refresh(List<Bot> bots) {
    GGBot addon = GGBot.getInstance();
    synchronized (ENTRIES) {
      ENTRIES.clear();
      version++;
    }
    for (Bot bot : bots) {
      Long id = bot.getId();
      String label = BotMenuWidget.displayName(bot);
      BotRequests.getSellItemsAsync(addon, bot, items -> {
        List<Entry> entries = new ArrayList<>();
        for (SellItem item : items) {
          add(entries, id, label, item.getName(), "sell",
              ApiParsers.itemType(item.getItemKey(), item.getFallbackKey(), item.getItemType()),
              item.getNbt(),
              item.getChestPosition());
        }
        replace(id, "sell", entries);
      });
      BotRequests.getBuyItemsAsync(addon, bot, items -> {
        List<Entry> entries = new ArrayList<>();
        for (BuyItem item : items) {
          add(entries, id, label, item.getName(), "buy",
              ApiParsers.itemType(item.getItemKey(), item.getFallbackKey(), item.getItemType()),
              item.getNbt(),
              item.getChestPosition());
        }
        replace(id, "buy", entries);
      });
    }
  }

  /**
   * Replaces specific shop entries for a bot while keeping other entries intact.
   *
   * @param botId The bot ID associated with the entries
   * @param shop The shop identifier for the entries
   * @param entries The new list of entries to replace the existing ones
   */
  private static void replace(Long botId, String shop, List<Entry> entries) {
    synchronized (ENTRIES) {
      ENTRIES.removeIf(e -> e.shop().equals(shop) && Objects.equals(e.botId(), botId));
      ENTRIES.addAll(entries);
      version++;
    }
  }

  /**
   * Returns the current cache version for tracking changes.
   *
   * @return The current version number of the cache
   */
  public static int version() {
    return version;
  }

  private static void add(List<Entry> target, Long botId, String bot, String name, String shop,
      String itemType, String nbt, BlockPosition pos) {
    if (name == null || pos == null
        || pos.getX() == null || pos.getY() == null || pos.getZ() == null) {
      return;
    }
    // Store the colour-stripped name so legacy codes never leak into search/marking.
    target.add(new Entry(botId, bot, TextUtil.stripColors(name), shop, itemType, nbt,
        pos.getX().intValue(), pos.getY().intValue(), pos.getZ().intValue()));
  }

  /** Returns the merged item entries stored in the chest at the given block. */
  public static List<Entry> itemsAtChest(int x, int y, int z) {
    List<Entry> result = new ArrayList<>();
    synchronized (ENTRIES) {
      for (Entry e : ENTRIES) {
        if (e.x() == x && e.y() == y && e.z() == z) {
          result.add(e);
        }
      }
    }
    return merge(result);
  }

  /** Returns merged entries whose item name contains the search term (case-insensitive). */
  public static List<Entry> matching(String term) {
    List<Entry> result = new ArrayList<>();
    if (term == null || term.isBlank()) {
      return result;
    }
    String needle = TextUtil.stripColors(term).toLowerCase(Locale.ROOT);
    synchronized (ENTRIES) {
      for (Entry e : ENTRIES) {
        if (e.name() != null && e.name().toLowerCase(Locale.ROOT).contains(needle)) {
          result.add(e);
        }
      }
    }
    return merge(result);
  }

  /**
   * Merges duplicate entries for the same bot and item at the same position.
   *
   * @param entries List of entries to merge
   * @return A list of merged entries with unique bot/item/position combinations
   */
  private static List<Entry> merge(List<Entry> entries) {
    java.util.LinkedHashMap<String, Entry> byKey = new java.util.LinkedHashMap<>();
    for (Entry e : entries) {
      String key = e.botId() + "/" + (e.name() == null ? "" : e.name().toLowerCase(Locale.ROOT))
          + "@" + e.x() + "," + e.y() + "," + e.z();
      Entry existing = byKey.get(key);
      if (existing == null) {
        byKey.put(key, e);
      } else if (!existing.shop().contains(e.shop())) {
        byKey.put(key, new Entry(existing.botId(), existing.bot(), existing.name(), "buy/sell",
            existing.itemType(), existing.nbt(), existing.x(), existing.y(), existing.z()));
      }
    }
    return new ArrayList<>(byKey.values());
  }
}
