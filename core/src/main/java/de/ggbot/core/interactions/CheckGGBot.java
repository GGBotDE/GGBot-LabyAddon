package de.ggbot.core.interactions;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.GetBotsOnServer200Response;
import de.ggbot.sdk.model.GetBotsOnServer200ResponseBotsInner;
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
      // Look the player up in the full list of bots on this server (the same reliable
      // source the shop uses) instead of a per-link lookup, which could fail/cache and
      // wrongly report a real bot as "not a bot".
      boolean isBot = false;
      try {
        GetBotsOnServer200Response response = api.getBotsOnServer(finalServerIP);
        if (response != null && response.getBots() != null) {
          for (GetBotsOnServer200ResponseBotsInner serverBot : response.getBots()) {
            // Require the bot to be online: a matching name alone does not prove the
            // account is actually running GGBot.
            if (serverBot.getLinkName() != null
                && serverBot.getLinkName().equalsIgnoreCase(player.getName())
                && Boolean.TRUE.equals(serverBot.getOnline())) {
              isBot = true;
              break;
            }
          }
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to fetch bots on server: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      }

      String resultKey = isBot ? "ggbot.messages.interaction.checkggbot.isbot"
          : "ggbot.messages.interaction.checkggbot.isnotbot";
      addon.displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1", BLUE)
          .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
          .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
          .append(Component.translatable(resultKey, GRAY)));
    }, "ggbot-check-bot");
    checkThread.setDaemon(true);
    checkThread.start();
  }
}
