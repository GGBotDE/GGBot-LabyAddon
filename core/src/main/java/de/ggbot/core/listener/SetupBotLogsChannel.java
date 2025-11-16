package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.auth.OAuthServer;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.advanced.IngameChatTab;
import net.labymod.api.client.chat.filter.ChatFilter;
import net.labymod.api.configuration.labymod.chat.AdvancedChatMessage;
import net.labymod.api.configuration.labymod.chat.ChatTab;
import net.labymod.api.configuration.labymod.chat.ChatWindow;
import net.labymod.api.configuration.labymod.chat.config.RootChatTabConfig;
import net.labymod.api.configuration.labymod.chat.config.RootChatTabConfig.Type;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import org.openapitools.client.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static de.ggbot.core.api.BotRequests.sentLogIds;

public class SetupBotLogsChannel {
  private static GGBot addon;
  private static OAuthServer authServer;
  public static IngameChatTab customTab;
  public static Timer timer;

  public SetupBotLogsChannel(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
    stopTimer();
  }
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if(!GGBot.getInstance().configuration().botlogSub.botLog.get()){
      return;
    }
    stopTimer();
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
    customTab.handleInput(AdvancedChatMessage.text("Hi"));
    customTab.config().filters().set(List.of(createFilter("filter")));
    startTimer();
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

  public static void startTimer() {
    timer = new Timer(true); // true = daemon, beendet sich automatisch beim App-Ende

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotRequests.logsAsync(GGBot.getInstance()); // dein statischer Log-Call
          System.out.println("Checked Log Basic: " + GGBot.getInstance().configuration().botlogSub.minutes.get());
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, GGBot.getInstance().configuration().botlogSub.minutes.get() * 60 * 1000); // 0 = sofort starten, 60*1000 = alle 60 Sekunden
  }
  public static void startTimer(long time) {
    timer = new Timer(true); // true = daemon, beendet sich automatisch beim App-Ende

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotRequests.logsAsync(GGBot.getInstance()); // dein statischer Log-Call
          System.out.println("Checked Log: " + GGBot.getInstance().configuration().botlogSub.minutes.get());
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, time * 60 * 1000); // 0 = sofort starten, 60*1000 = alle 60 Sekunden
  }
  public static void stopTimer(){
    if(timer != null){
      timer.cancel();
    }
  }

}
