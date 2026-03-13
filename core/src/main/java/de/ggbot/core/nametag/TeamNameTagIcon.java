package de.ggbot.core.nametag;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission.DisplayMode;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Name tag renderer that replaces the player's name tag component with their
 * GGBot team in-game icon when applicable.
 */
public class TeamNameTagIcon extends ComponentNameTag {

  private final Supplier<Float> scaleSupplier;

  /**
   * @param scaleSupplier provides the render scale for this tag
   */
  public TeamNameTagIcon(Supplier<Float> scaleSupplier) {
    this.scaleSupplier = scaleSupplier;
  }

  /** {@inheritDoc} */
  @Override
  public float getScale() {
    return scaleSupplier.get();
  }

  /**
   * Replaces the default name tag with the player's team icon component,
   * falling back to the default rendering when no icon is available.
   *
   * @param snapshot the current entity snapshot
   * @return list of components to display
   */
  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    if (snapshot.isDiscrete()
        || snapshot.isInvisible()
        || !snapshot.has(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER)) {
      return super.buildComponents(snapshot);
    }
    GGBotTeamTagUserSnapshot teamSnapshot = snapshot.get(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER);
    if (teamSnapshot == null || teamSnapshot.getPlayer() == null
        || !teamSnapshot.getPlayer().isTeamMember()
        || teamSnapshot.getPlayer().getIngameIcon() == null) {
      return super.buildComponents(snapshot);
    }
    return Collections.singletonList(Component.icon(teamSnapshot.getPlayer().getIngameIcon()));
  }

  /**
   * Renders the team icon as a 9×9 sprite above the player's head.
   *
   * @param stack                the matrix stack
   * @param submissionCollector  the render submission collector
   * @param snapshot             the current entity snapshot
   */
  @Override
  public void render(Stack stack, SubmissionCollector submissionCollector, EntitySnapshot snapshot) {
    super.render(stack, submissionCollector, snapshot);
    GGBotTeamTagUserSnapshot teamSnapshot = snapshot.get(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER);
    if (teamSnapshot == null || teamSnapshot.getPlayer() == null
        || !teamSnapshot.getPlayer().isTeamMember()
        || teamSnapshot.getPlayer().getIngameIcon() == null) {
      return;
    }
    submissionCollector.submitIcon(stack, teamSnapshot.getPlayer().getIngameIcon(), DisplayMode.NORMAL, -8, 0, 9, 9, -1);
  }
}

