package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
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
  public void onChat(ChatMessageSendEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.console")) return;
    if (!addon.configuration().prefixSub.botCommands.get()) return;

    String message = e.getMessage();
    if (!message.startsWith(addon.configuration().prefixSub.prefix.get())) return;

    // Cancel the event immediately on the game thread — no blocking work here.
    e.setCancelled(true);
    String command = "!" + message.substring(addon.configuration().prefixSub.prefix.get().length());

    Thread t = new Thread(() -> {
      if (BotRequests.isOnlineCached(addon)) {
        try {
          BotRequests.sendCommand(addon, command);
        } catch (Exception ex) {
          addon.logger().error("Failed to send bot command: " + ex.getMessage());
          addon.getVersioningHandler().reportError(ex);
        }
      } else {
        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          Notification.Builder builder = Notification.builder()
              .title(Component.text("ERROR", NamedTextColor.RED))
              .text(Component.translatable("ggbot.messages.command.send.error.offline"))
              .type(Type.SYSTEM);
          Laby.labyAPI().notificationController().push(builder.build());
        });
      }
    }, "ggbot-command-send");
    t.setDaemon(true);
    t.start();
  }
}
