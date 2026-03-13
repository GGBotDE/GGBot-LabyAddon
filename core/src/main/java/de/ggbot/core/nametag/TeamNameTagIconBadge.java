package de.ggbot.core.nametag;

import net.labymod.api.client.entity.player.badge.renderer.BadgeRenderer;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.network.NetworkPlayerInfo;

/**
 * Badge renderer that displays the GGBot team in-game icon next to a player's
 * entry in the tab list.
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
    GGBotTeamPlayer teamPlayer = new GGBotTeamPlayer(player.profile().getUniqueId());
    if (!teamPlayer.isTeamMember() || teamPlayer.getIngameIcon() == null) return;
    ctx.canvas().submitIcon(teamPlayer.getIngameIcon(), x, y, 9, 9);
  }

  /**
   * Returns whether this badge should be shown for the given player.
   *
   * @param player the player to check
   * @return {@code true} if the player is a GGBot team member with an in-game icon
   */
  @Override
  protected boolean isVisible(NetworkPlayerInfo player) {
    GGBotTeamPlayer teamPlayer = new GGBotTeamPlayer(player.profile().getUniqueId());
    return teamPlayer.isTeamMember() && teamPlayer.getIngameIcon() != null;
  }
}
