package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import net.labymod.api.Laby;
import net.labymod.api.client.Minecraft;
import net.labymod.api.client.chat.advanced.ChatMessagesWidget;
import net.labymod.api.client.chat.advanced.IngameChatTab;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.configuration.labymod.chat.AdvancedChatMessage;
import net.labymod.api.configuration.labymod.chat.ChatWindow;
import net.labymod.api.configuration.labymod.chat.config.RootChatTabConfig;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ChatMessageSendEvent;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Type;
import org.openapitools.client.ApiException;

public class ChatListener {
  private static GGBot addon;
  public ChatListener(GGBot addon) {
    this.addon = addon;
  }
  @Subscribe
  public void onChat(ChatMessageSendEvent e) throws ApiException {
    if(GGBot.getInstance().configuration().prefixSub.botCommands.get()){
      String message = e.getMessage();
      if(message.startsWith(GGBot.getInstance().configuration().prefixSub.prefix.get())){
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
