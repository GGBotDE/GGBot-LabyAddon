package de.ggbot.core.nametag;

import de.ggbot.core.GGBot;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.ExtrasWriter;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;

@AutoService(LabySnapshotFactory.class)
public class GGBotTeamTagUserSnapshotFactory extends
    LabySnapshotFactory<Player, GGBotTeamTagUserSnapshot> {

  private final GGBot addon;

  public GGBotTeamTagUserSnapshotFactory(GGBot addon) {
    super(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER);
    this.addon = addon;
  }

  @Override
  public GGBotTeamTagUserSnapshot create(Player player, Extras extras) {
    return new GGBotTeamTagUserSnapshot(player, extras, this.addon);
  }
}
