package de.ggbot.core.nametag;

import net.labymod.api.client.entity.player.badge.renderer.BadgeRenderer;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.network.NetworkPlayerInfo;

/**
 * Badge renderer that displays the GGBot team in-game icon next to a player's
 * entry in the tab list.
 *
 * <p>Both render() and {@link #isVisible} run per tab-list entry per
 * frame, so they only hit the TTL-cached lookup in
 * {@link GGBotTeamPlayer#getBadgeIcon} instead of walking the team list.
 */
public class TeamNameTagIconBadge extends BadgeRenderer {

  /**
   * Renders the 9×9 team icon at the specified tab-list position.
   *
   * @param ctx    the screen rendering context
   * @param x      horizontal position
   * @param y      vertical position
   * @param player the network player info
   */
  @Override
  public void render(ScreenContext ctx, float x, float y, NetworkPlayerInfo player) {
    Icon icon = GGBotTeamPlayer.getBadgeIcon(player.profile().getUniqueId());
    if (icon == null) return;
    ctx.canvas().submitIcon(icon, x, y - 1, 9, 9);
  }

  /**
   * Returns whether this badge should be shown for the given player.
   *
   * @param player the player to check
   * @return {@code true} if the player is a GGBot team member with an in-game icon
   */
  @Override
  protected boolean isVisible(NetworkPlayerInfo player) {
    return GGBotTeamPlayer.getBadgeIcon(player.profile().getUniqueId()) != null;
  }
}
