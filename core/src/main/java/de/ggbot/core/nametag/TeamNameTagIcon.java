package de.ggbot.core.nametag;

import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.badge.renderer.BadgeRenderer;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.ScreenInstance;
import net.labymod.api.client.gui.screen.widget.widgets.navigation.tab.ComponentTab;
import net.labymod.api.client.network.NetworkPlayerInfo;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.client.scoreboard.TabList;
import net.labymod.api.configuration.labymod.main.laby.multiplayer.TabListConfig;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission.DisplayMode;
import net.labymod.serverapi.core.model.display.ServerBadge;
import net.labymod.serverapi.core.model.display.TabListFlag;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class TeamNameTagIcon extends ComponentNameTag {

  private final Supplier<Float> scaleSupplier;

  public TeamNameTagIcon(Supplier<Float> scaleSupplier) {
    this.scaleSupplier = scaleSupplier;
  }

  @Override
  public float getScale() {
    return scaleSupplier.get();
  }

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    if (snapshot.isDiscrete()
        || snapshot.isInvisible()
        || !snapshot.has(GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER)) {
      return super.buildComponents(snapshot);
    }

    GGBotTeamTagUserSnapshot teamSnapshot = snapshot.get(
        GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER
    );
    if(teamSnapshot == null || teamSnapshot.getPlayer() == null || !teamSnapshot.getPlayer().isTeamMember() || teamSnapshot.getPlayer().getIngameIcon() == null) {
      return super.buildComponents(snapshot);
    }

    return Collections.singletonList(Component.icon(teamSnapshot.getPlayer().getIngameIcon()));
  }

  @Override
  public void render(Stack stack, SubmissionCollector submissionCollector,
      EntitySnapshot snapshot) {
    super.render(stack, submissionCollector, snapshot);
    GGBotTeamTagUserSnapshot teamSnapshot = snapshot.get(
        GGBotTeamTagUserSnapshot.GGBOT_TEAMMEMBER
    );

    if(teamSnapshot == null || teamSnapshot.getPlayer() == null || !teamSnapshot.getPlayer().isTeamMember() || teamSnapshot.getPlayer().getIngameIcon() == null)
      return;

    submissionCollector.submitIcon(stack, teamSnapshot.getPlayer().getIngameIcon(), DisplayMode.NORMAL, -8, 0, 9, 9, -1);
  }
}

