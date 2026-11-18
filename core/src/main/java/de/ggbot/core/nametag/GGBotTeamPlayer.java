package de.ggbot.core.nametag;

import de.ggbot.core.overlay.ShopHintRenderer;
import de.ggbot.core.utils.ttlcache.TTLCache;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class GGBotTeamPlayer {

  /**
   * Displayed as a fallback when the API does not provide an icon.
   */
  private static final Icon LOGO = Icon.texture(ResourceLocation.create("ggbot", "textures/icon.png"));

/**
 * Shared lookup cache used by both the tab-list badge and player name tag.
 * Since both renderers are invoked for every entry on each frame, caching
 * avoids performing the same lookup repeatedly.
 * Empty {@link Optional} values are also cached to prevent unnecessary
 * lookups for players without a tag.
 */
  private static final TTLCache<UUID, Optional<Icon>> tagCache = new TTLCache<>();

  /**
   * Stores icons loaded by URL, shared across all players using the same icon.
   */
  private static final Map<String, Icon> urlIcons = new ConcurrentHashMap<>();

  /**
   * Defines the duration for which a tag lookup result remains valid, in milliseconds.
   */
  private static final long TAG_CACHE_TTL_MS = 60_000L;

  private final UUID uuid;
  private final String name;

  public GGBotTeamPlayer(Player player) {
    this.uuid = player.getUniqueId();
    this.name = player.getName();
  }

  /**
   * Retrieves the in-game icon for a player, or null if no tag is available.
   *
   * @param uuid the player's UUID
   * @param name the player's name, used to recognize GGBots
   * @return the icon, or {@code null}
   */
  public static Icon getTeamIcon(UUID uuid, String name) {
    Optional<Icon> cached = tagCache.get(uuid);
    if (cached != null) {
      return cached.orElse(null);
    }
    TeamFetcher fetcher = TeamFetcher.teamInstance;
    // Prevents caching of untagged players before data is available.
    if (fetcher == null || !fetcher.isFetched()) {
      return null;
    }
    GGBotTag tag = fetcher.getTag(uuid);
    if (tag == null && ShopHintRenderer.isOnlineBot(name)) {
      tag = fetcher.getBotTag();
    }
    Icon icon = tag != null ? icon(tag.getIconUrl()) : null;
    tagCache.put(uuid, Optional.ofNullable(icon), TAG_CACHE_TTL_MS);
    return icon;
  }

  private static Icon icon(String url) {
    if (url == null || url.isEmpty()) {
      return LOGO;
    }
    return urlIcons.computeIfAbsent(url, Icon::url);
  }

  /**
   * Returns the UUID of the player.
   *
   * @return the UUID of the player
   */
  public UUID getUuid() {
    return uuid;
  }

  /**
   * Returns the name of the player.
   *
   * @return the name of the player
   */
  public String getName() {
    return name;
  }
}
