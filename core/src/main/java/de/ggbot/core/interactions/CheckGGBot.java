package de.ggbot.core.interactions;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiClient;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.core.Configuration;
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
    if(!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.checkbot")) return;
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath(this.addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.checkbot"));

    PublicApi api = new PublicApi();
    String serverIP = GGBot.getInstance().labyAPI().serverController().getCurrentServerData().address().getHost().toLowerCase();
    String[] splittedServer = serverIP.split("\\.");
    try {
      if(splittedServer.length > 1) {
        String baseDomain = splittedServer[splittedServer.length - 2] + "." + splittedServer[splittedServer.length - 1];
        for (Server server : api.getPublicServers()) {
          if (server.getName().equals(baseDomain)) {
            serverIP = baseDomain;
          }
        }
      }
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    try {
      PublicBot bot = api.getPublicBotByLink(player.getName(), serverIP);
      if(bot.getOnline()){
        GGBot.getInstance().displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1",BLUE)
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.isbot", GRAY)));
      }else{
        GGBot.getInstance().displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1",BLUE)
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
            .append(Component.translatable("ggbot.messages.interaction.checkggbot.isnotbot", GRAY)));
      }
    } catch (ApiException e) {
      GGBot.getInstance().displayMessage(Component.translatable("ggbot.messages.interaction.checkggbot.prefix1",BLUE)
          .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix2", AQUA))
          .append(Component.translatable("ggbot.messages.interaction.checkggbot.prefix3", BLUE))
          .append(Component.translatable("ggbot.messages.interaction.checkggbot.isnotbot", GRAY)));
    }
    System.out.println();
  }
}
