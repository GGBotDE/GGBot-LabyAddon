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
 * Player middle-click interaction (combat module): make the selected bot attack
 * the clicked player via the {@code !pvp <player>} command.
 */
public class AttackPlayerInteraction implements BulletPoint {

  private final GGBot addon;

  public AttackPlayerInteraction(GGBot addon) {
    this.addon = addon;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("ggbot.messages.interaction.attackplayer.name");
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  /** Only shown when enabled in settings and the combat module is active on the bot. */
  @Override
  public boolean isVisible(Player player) {
    return addon.configuration().generalSub.attackPlayerEnabled.get()
        && addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.interaction.attackplayer")
        && BotMenuState.get().isModuleActive("combat");
  }

  @Override
  public void execute(Player player) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.interaction.attackplayer")) {
      return;
    }
    Bot bot = BotRequests.getBotFromCache(addon);
    if (bot == null || player == null) return;
    BotRequests.sendCommandToBot(addon, bot, "!pvp " + player.getName());
  }
}
