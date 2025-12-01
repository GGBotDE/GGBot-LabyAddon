package de.ggbot.core.nametag;

import de.ggbot.core.GGBot;
import de.ggbot.core.utils.ttlcache.TTLCache;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.laby3d.renderer.snapshot.AbstractLabySnapshot;
import net.labymod.api.laby3d.renderer.snapshot.ExtraKey;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GGBotTeamTagUserSnapshot extends AbstractLabySnapshot {

  public static final ExtraKey<GGBotTeamTagUserSnapshot> GGBOT_TEAMMEMBER = ExtraKey.of(
      "ggbot_teammember",
      GGBotTeamTagUserSnapshot.class
  );

  private GGBotTeamPlayer player;

  public GGBotTeamTagUserSnapshot(Player player, Extras extras, GGBot addon) {
    super(extras);
    this.player = new GGBotTeamPlayer(player);
  }

  public GGBotTeamPlayer getPlayer() {
    return player;
  }
}
