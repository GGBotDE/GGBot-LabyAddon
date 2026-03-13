package de.ggbot.core.nametag;

import de.ggbot.core.GGBot;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.ExtrasWriter;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;

/**
 * Factory that produces {@link GGBotTeamTagUserSnapshot} instances for the
 * LabyMod snapshot system.
 */
@AutoService(LabySnapshotFactory.class)
public class GGBotTeamTagUserSnapshotFactory extends
    LabySnapshotFactory<Player, GGBotTeamTagUserSnapshot> {

  private final GGBot addon;

  /**
   * @param addon the addon instance forwarded to each created snapshot
   */
  public GGBotTeamTagUserSnapshotFactory(GGBot addon) {
    super(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER);
    this.addon = addon;
  }

  /**
   * Creates a new {@link GGBotTeamTagUserSnapshot} for the given player.
   *
   * @param player the player entity
   * @param extras the extras map provided by the snapshot system
   * @return a freshly created snapshot
   */
  @Override
  public GGBotTeamTagUserSnapshot create(Player player, Extras extras) {
    return new GGBotTeamTagUserSnapshot(player, extras, this.addon);
  }
}
