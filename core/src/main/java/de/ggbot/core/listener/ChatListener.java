package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.core.ApiException;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatMessageSendEvent;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Type;

public class ChatListener {
  private final GGBot addon;

  public ChatListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onChat(ChatMessageSendEvent e) throws ApiException {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.console")) return;
    if(addon.configuration().prefixSub.botCommands.get()){
      String message = e.getMessage();
      if(message.startsWith(addon.configuration().prefixSub.prefix.get())){
        e.setCancelled(true);
        String command = "!" + message.substring(1);
        if(BotRequests.isOnline(addon)) {
          BotRequests.sendCommand(addon, command);
        }else{
          Notification.Builder builder = Notification.builder()
              .title(Component.text("ERROR", NamedTextColor.RED))
              .text(Component.translatable("ggbot.messages.command.send.error.offline"))
              .type(Type.SYSTEM);
          Laby.labyAPI().notificationController().push(builder.build());
        }
      }
    }
  }
}
