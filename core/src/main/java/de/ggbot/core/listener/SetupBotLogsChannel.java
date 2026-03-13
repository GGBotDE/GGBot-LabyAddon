package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.advanced.IngameChatTab;
import net.labymod.api.client.chat.filter.ChatFilter;
import net.labymod.api.configuration.labymod.chat.ChatWindow;
import net.labymod.api.configuration.labymod.chat.config.RootChatTabConfig;
import net.labymod.api.configuration.labymod.chat.config.RootChatTabConfig.Type;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import java.util.List;

import static de.ggbot.core.api.BotRequests.sentLogIds;

public class SetupBotLogsChannel {
  public static IngameChatTab customTab;
  private GGBot addon;
  public SetupBotLogsChannel(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
  }
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.logs.tab")) return;
    if(!GGBot.getInstance().configuration().botlogSub.botLog.get()){
      return;
    }
    sentLogIds.clear();
    ChatWindow mainWindow = getChatWindow();
    if (mainWindow == null) return;
    getChatWindow().getTabs().removeIf(tab -> tab.getName().equalsIgnoreCase("Bot-Log"));
    RootChatTabConfig config = new RootChatTabConfig(
        Type.CUSTOM,
        "Bot-Log"
    );
    customTab = new IngameChatTab(mainWindow, config);
    mainWindow.initializeTab(config, customTab, true);
    customTab.config().filters().set(List.of(createFilter("filter")));
  }
  private static ChatFilter createFilter(String name) {
    ChatFilter filter = new ChatFilter();
    filter.name().set(name);
    filter.getIncludedTags().add("\u200B");

    return filter;
  }

  public static ChatWindow getChatWindow() {
    for (ChatWindow window : Laby.references().advancedChatController().getWindows()) {
      if (window.isMainWindow()) {
        return window;
      }
    }
    return null;
  }

}
