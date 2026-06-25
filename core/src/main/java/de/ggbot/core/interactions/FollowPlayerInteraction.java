package de.ggbot.core.interactions;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.entity.player.interaction.BulletPoint;
import net.labymod.api.client.gui.icon.Icon;

/**
 * Player middle-click interaction (utils module): make the selected bot follow
 * the clicked player via the {@code !follow <player>} command.
 */
public class FollowPlayerInteraction implements BulletPoint {

  private final GGBot addon;

  public FollowPlayerInteraction(GGBot addon) {
    this.addon = addon;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("ggbot.messages.interaction.followplayer.name");
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  /** Only shown when enabled in settings and the utils module is active on the bot. */
  @Override
  public boolean isVisible(Player player) {
    return addon.configuration().generalSub.followPlayerEnabled.get()
        && addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.interaction.followplayer")
        && BotMenuState.get().isModuleActive("utils");
  }

  @Override
  public void execute(Player player) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.interaction.followplayer")) {
      return;
    }
    Bot bot = BotRequests.getBotFromCache(addon);
    if (bot == null || player == null) return;
    BotRequests.sendCommandToBot(addon, bot, "!follow " + player.getName());
  }
}
