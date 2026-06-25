package de.ggbot.core.nametag;

import de.ggbot.core.GGBot;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.AbstractLabySnapshot;
import net.labymod.api.laby3d.renderer.snapshot.ExtraKey;
import net.labymod.api.laby3d.renderer.snapshot.Extras;

/**
 * Snapshot that stores the {@link GGBotTeamPlayer} for a player entity so that
 * name-tag and badge renderers can access team data without re-querying per frame.
 */
public class GGBotTeamTagUserSnapshot extends AbstractLabySnapshot {

  /** Key used to store and retrieve this snapshot from an entity's extras. */
  public static final ExtraKey<GGBotTeamTagUserSnapshot> GGBOT_TEAMMEMBER = ExtraKey.of(
      "ggbot_teammember",
      GGBotTeamTagUserSnapshot.class
  );

  private final GGBotTeamPlayer player;

  /**
   * @param player the player entity this snapshot belongs to
   * @param extras the extras map provided by the snapshot system
   * @param addon  the addon instance (unused, reserved for future use)
   */
  public GGBotTeamTagUserSnapshot(Player player, Extras extras, GGBot addon) {
    super(extras);
    this.player = new GGBotTeamPlayer(player);
  }

  /**
   * Returns the {@link GGBotTeamPlayer} associated with this snapshot.
   *
   * @return the team player wrapper
   */
  public GGBotTeamPlayer getPlayer() {
    return player;
  }
}
