package de.ggbot.core.nametag;

import net.labymod.api.client.entity.player.tag.tags.IconTag;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.state.entity.EntitySnapshot;

/**
 * Displays the GGBot team or bot icon alongside a player's name tag.
 */
public class TeamNameTagIcon extends IconTag {

  private static final float ICON_SIZE = 9F;

  public TeamNameTagIcon() {
    super(ICON_SIZE);
  }

  /**
   * Fetches the cached icon for the player's snapshot.
   *
   * @param snapshot the current entity snapshot
   * @return the icon, or {@code null} to hide the tag
   */
  @Override
  public Icon getIcon(EntitySnapshot snapshot) {
    if (snapshot.isDiscrete() || snapshot.isInvisible()
        || !snapshot.has(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER)) {
      return null;
    }
    GGBotTeamTagUserSnapshot teamSnapshot = snapshot.get(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER);
    if (teamSnapshot == null || teamSnapshot.getPlayer() == null) {
      return null;
    }
    GGBotTeamPlayer player = teamSnapshot.getPlayer();
    return GGBotTeamPlayer.getTeamIcon(player.getUuid(), player.getName());
  }

  /**
   * Indicates this method adheres to the parent class's contract.
   *
   * @return true indicating visibility is restricted to the main name tag
   */
  @Override
  public boolean isOnlyVisibleOnMainTag() {
    return true;
  }
}
