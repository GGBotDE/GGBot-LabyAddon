package de.ggbot.core.listener;

import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextDecoration;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import de.ggbot.core.GGBot;
import de.ggbot.core.auth.OAuthServer;

import java.io.IOException;

import static de.ggbot.core.api.BotRequests.bots;

public class AuthEvent {

  private static GGBot addon;
  private static OAuthServer authServer;

  public AuthEvent(GGBot addon) {
    AuthEvent.addon = addon;
  }

  @Subscribe
  public void onServerJoin(ServerJoinEvent e) throws IOException {
    authServer = new OAuthServer(addon);
    if(!GGBot.isAuth){
      Component message = Component.text()
          .append(Component.translatable("ggbot.messages.join.prefix1").color(NamedTextColor.BLUE))
          .append(Component.translatable("ggbot.messages.join.prefixname").color(NamedTextColor.AQUA))
          .append(Component.translatable("ggbot.messages.join.prefix2").color(NamedTextColor.BLUE))
          .build();
      Component message2;
      Component message3;
      if(GGBot.isExpired){
        message2 = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.expired.text"))
            .color(NamedTextColor.RED)
            .build();
        message3 = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.expired.text2").decoration(TextDecoration.BOLD, true)).append(Component.translatable("ggbot.messages.join.error.expired.text3").decoration(TextDecoration.BOLD, false))
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.openUrl(authServer.getUrl()))
            .build();
      } else {
        message2 = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.authenticate.text"))
            .color(NamedTextColor.RED)
            .build();
        message3 = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.authenticate.text2").decoration(TextDecoration.BOLD, true)).append(Component.translatable("ggbot.messages.join.error.authenticate.text3").decoration(TextDecoration.BOLD, false))
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.openUrl(authServer.getUrl()))
            .build();
      }

      Component message4 = Component.text()
          .append(Component.translatable("ggbot.messages.join.prefix3").color(NamedTextColor.BLUE))
          .build();

      addon.displayMessage(message);
      addon.displayMessage(message2);
      addon.displayMessage(message3);
      addon.displayMessage(message4);
      auth();
    }else{
      botLoad();
    }

  }

    @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent e) {
    if(authServer != null)
      authServer.close();
  }
  public static void auth(){
    try {
       authServer.listenForCodeAsync((Code) -> authServer.getTokenAsync(Code, (Token) -> {
         System.out.println("Token: " + Token);
        addon.configuration().token.set(Token.get("access_token").getAsString());
         addon.configuration().expiresAt.set(String.valueOf(
             System.currentTimeMillis() + (Token.get("expires_in").getAsInt() * 1000L)));
        GGBot.code = Token.get("access_token").getAsString();
        GGBot.isAuth = true;
        GGBot.isExpired = false;
      }));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void botLoad() {
    if(!addon.configuration().botlist.get().isEmpty()){
      // Bot ausgewählt
      String bot = addon.configuration().botlist.get();
      if (!bot.contains("(") || !bot.contains(")")) return; // Ungültiges Format
      String id = bot.substring(bot.lastIndexOf('(') + 1, bot.lastIndexOf(')')); // ID aus String holen
      for (Bot bots : bots) {
        if (bots.getId().toString().equals(id)) { // passenden Bot gefunden
          break;
        }
      }
      // kein passender Bot gefunden
    }
  }
}
