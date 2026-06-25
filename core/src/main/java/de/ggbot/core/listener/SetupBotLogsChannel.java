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
import net.labymod.api.util.I18n;
import java.util.List;

import static de.ggbot.core.api.BotRequests.sentLogIds;

public class SetupBotLogsChannel {
  public static IngameChatTab customTab;
  private final GGBot addon;

  public SetupBotLogsChannel(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
  }
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if(!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.log.tab")) return;
    if(!addon.configuration().botlogSub.botLog.get()){
      return;
    }
    try {
      sentLogIds.clear();
      ChatWindow mainWindow = getChatWindow();
      if (mainWindow == null) return;

      String tabName = I18n.getTranslation("ggbot.messages.log.tab");

      // Idempotent: if a log tab already exists, reuse the first one and drop any
      // duplicates. This guards against ServerJoinEvent firing repeatedly (e.g.
      // GrieferGames sub-server hops) and accumulating hundreds of tabs.
      IngameChatTab existing = null;
      var tabs = mainWindow.getTabs();
      for (int i = tabs.size() - 1; i >= 0; i--) {
        var tab = tabs.get(i);
        if (tab.getName() != null && tab.getName().equalsIgnoreCase(tabName)) {
          if (existing == null && tab instanceof IngameChatTab ingameTab) {
            existing = ingameTab;
          } else {
            tabs.remove(i);
          }
        }
      }
      if (existing != null) {
        customTab = existing;
        return;
      }

      RootChatTabConfig config = new RootChatTabConfig(Type.CUSTOM, tabName);
      customTab = new IngameChatTab(mainWindow, config);
      mainWindow.initializeTab(config, customTab, true);
      customTab.config().filters().set(List.of(createFilter("filter")));
    } catch (Exception ex) {
      addon.logger().error("Failed to set up bot logs chat tab: " + ex.getMessage());
    }
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
