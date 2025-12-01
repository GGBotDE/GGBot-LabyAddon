package de.ggbot.core.nametag;

import net.labymod.api.client.entity.player.badge.renderer.BadgeRenderer;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.network.NetworkPlayerInfo;

public class TeamNameTagIconBadge extends BadgeRenderer {

  @Override
  public void render(ScreenContext ctx, float x, float y, NetworkPlayerInfo player) {
    GGBotTeamPlayer teamPlayer = new GGBotTeamPlayer(player.profile().getUniqueId());
    if(!teamPlayer.isTeamMember() || teamPlayer.getIngameIcon() == null) return;
    ctx.canvas().submitIcon(teamPlayer.getIngameIcon(), x, y, 9, 9);
  }

  @Override
  protected boolean isVisible(NetworkPlayerInfo player) {
    GGBotTeamPlayer teamPlayer = new GGBotTeamPlayer(player.profile().getUniqueId());
    return teamPlayer.isTeamMember() && teamPlayer.getIngameIcon() != null;
  }
}
