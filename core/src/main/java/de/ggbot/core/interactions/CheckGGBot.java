package de.ggbot.core.interactions;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.PublicBot;
import de.ggbot.sdk.model.Server;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.entity.player.interaction.BulletPoint;
import net.labymod.api.client.gui.icon.Icon;

import static net.labymod.api.client.component.format.NamedTextColor.AQUA;
import static net.labymod.api.client.component.format.NamedTextColor.BLUE;
import static net.labymod.api.client.component.format.NamedTextColor.GRAY;

public class CheckGGBot implements BulletPoint {
  private final GGBot addon;

  public CheckGGBot(GGBot addon) {
    this.addon = addon;
  }

  @Override
  public Component getTitle() {
    return Component.translatable("ggbot.messages.interaction.checkggbot.name");
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public void execute(Player player) {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if (!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.checkbot")) return;

    String rawServerIP = GGBot.getInstance().labyAPI().serverController()
        .getCurrentServerData().address().getHost().toLowerCase();
    String[] parts = rawServerIP.split("\\.");

    Thread checkThread = new Thread(() -> {
      PublicApi api = new PublicApi();
      api.setCustomBaseUrl(this.addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.checkbot"));

      String serverIP = rawServerIP;
      try {
        if (parts.length > 1) {
          String baseDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
          for (Server server : api.getPublicServers()) {
            if (server.getName().equals(baseDomain)) {
              serverIP = baseDomain;
              break;
            }
          }
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to fetch public servers: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      }

      final String finalServerIP = serverIP;
      try {
        PublicBot bot = api.getPublicBotByLink(player.getName(), finalServerIP);
        if (bot.getOnline()) {
          addon.displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1", BLUE)
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.isbot", GRAY)));
        } else {
          addon.displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1", BLUE)
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
              .append(Component.translatable("ggbot.messages.interaction.checkggbot.isnotbot", GRAY)));
        }
      } catch (ApiException e) {
        addon.displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1", BLUE)
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.isnotbot", GRAY)));
      }
    }, "ggbot-check-bot");
    checkThread.setDaemon(true);
    checkThread.start();
  }
}
