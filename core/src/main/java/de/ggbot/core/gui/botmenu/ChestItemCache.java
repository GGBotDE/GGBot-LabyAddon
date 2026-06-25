package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.BlockPosition;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.BuyItem;
import de.ggbot.sdk.model.SellItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Caches the selected bot's buy/sell items together with their chest positions,
 * so the chest-contents overlay and item search can resolve item to chest without
 * hitting the API every frame. Populated directly from the SDK item models.
 */
public final class ChestItemCache {

  /** A single item entry tied to a chest block position. */
  public record Entry(String name, String shop, String itemType, int x, int y, int z) {}

  private static final List<Entry> ENTRIES = new ArrayList<>();

  private ChestItemCache() {}

  /** Refreshes the cache for the given bot (buy + sell items). */
  public static void refresh(Bot bot) {
    if (bot == null) {
      return;
    }
    GGBot addon = GGBot.getInstance();
    BotRequests.getSellItemsAsync(addon, bot, items -> {
      synchronized (ENTRIES) {
        ENTRIES.removeIf(e -> e.shop().equals("sell"));
        for (SellItem item : items) {
          Entry entry = toEntry(item.getName(), "sell", item.getItemType(),
              item.getChestPosition());
          if (entry != null) {
            ENTRIES.add(entry);
          }
        }
      }
    });
    BotRequests.getBuyItemsAsync(addon, bot, items -> {
      synchronized (ENTRIES) {
        ENTRIES.removeIf(e -> e.shop().equals("buy"));
        for (BuyItem item : items) {
          Entry entry = toEntry(item.getName(), "buy", item.getItemType(),
              item.getChestPosition());
          if (entry != null) {
            ENTRIES.add(entry);
          }
        }
      }
    });
  }

  private static Entry toEntry(String name, String shop, String itemType, BlockPosition pos) {
    if (name == null || pos == null
        || pos.getX() == null || pos.getY() == null || pos.getZ() == null) {
      return null;
    }
    // Store the colour-stripped name so legacy codes never leak into search/marking.
    return new Entry(TextUtil.stripColors(name), shop, itemType,
        pos.getX().intValue(), pos.getY().intValue(), pos.getZ().intValue());
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
   * Merges entries that refer to the same item at the same chest (same name +
   * position) but in different shops, combining their shop label into
   * {@code "buy/sell"} so each item appears once.
   */
  private static List<Entry> merge(List<Entry> entries) {
    java.util.LinkedHashMap<String, Entry> byKey = new java.util.LinkedHashMap<>();
    for (Entry e : entries) {
      String key = (e.name() == null ? "" : e.name().toLowerCase(Locale.ROOT))
          + "@" + e.x() + "," + e.y() + "," + e.z();
      Entry existing = byKey.get(key);
      if (existing == null) {
        byKey.put(key, e);
      } else if (!existing.shop().contains(e.shop())) {
        byKey.put(key, new Entry(existing.name(), "buy/sell", existing.itemType(),
            existing.x(), existing.y(), existing.z()));
      }
    }
    return new ArrayList<>(byKey.values());
  }
}
