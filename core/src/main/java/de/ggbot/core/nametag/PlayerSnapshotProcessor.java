package de.ggbot.core.nametag;

import net.labymod.api.client.entity.Entity;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.render.state.entity.EntitySnapshotProcessor;
import net.labymod.api.client.render.state.entity.EntitySnapshotRegistry;
import net.labymod.api.laby3d.renderer.snapshot.ExtrasWriter;
import net.labymod.api.service.annotation.AutoService;

/**
 * Snapshot processor that captures {@link GGBotTeamTagUserSnapshot} data for
 * every {@link Player} entity each render frame.
 */
@AutoService(EntitySnapshotProcessor.class)
public class PlayerSnapshotProcessor extends EntitySnapshotProcessor<Player> {

  /**
   * @param registry the entity snapshot registry
   */
  public PlayerSnapshotProcessor(EntitySnapshotRegistry registry) {
    super(registry);
  }

  /**
   * Returns {@code true} for all {@link Player} entities.
   *
   * @param entity the entity to check
   * @return {@code true} if the entity is a player
   */
  @Override
  public boolean supports(Entity entity) {
    return entity instanceof Player;
  }

  /**
   * Captures the GGBot team snapshot for the given player.
   *
   * @param player       the player being processed
   * @param partialTicks render partial tick delta
   * @param entityWriter writer for appending snapshot extras
   */
  @Override
  public void process(Player player, float partialTicks, ExtrasWriter entityWriter) {
    this.registry().captureSnapshot(entityWriter, GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER, player);
  }
}