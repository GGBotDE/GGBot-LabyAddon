package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.widget.BotNameWidget;
import de.ggbot.core.widget.StatusWidget;
import de.ggbot.sdk.core.ApiException;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import java.util.Timer;
import java.util.TimerTask;

public class StartTimerOnJoin {
  private static GGBot addon;
  public static Timer timer;
  public static Timer timerStatus;

  public StartTimerOnJoin(GGBot addon) {
    this.addon = addon;

  }
  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
    stopTimer();
    stopTimerStatusCheck();
  }
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if(GGBot.getInstance().configuration().botlogSub.botLog.get()){
      startTimer();
    }
    startTimerStatusCheck();
  }

  public static void startTimer() {
    timer = new Timer(true);

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotRequests.logsAsync();
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, GGBot.getInstance().configuration().botlogSub.minutes.get() * 60 * 1000);
  }
  public static void startTimer(long time) {
    timer = new Timer(true);

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotRequests.logsAsync();
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, time * 60 * 1000);
  }
  public static void stopTimer(){
    if(timer != null){
      timer.cancel();
    }
  }


  public static void startTimerStatusCheck() {
    timerStatus = new Timer(true);

    timerStatus.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotNameWidget.BotName.updateAndFlush(BotRequests.getName(GGBot.getInstance()));
          StatusWidget.Status.updateAndFlush(BotRequests.getStatus(GGBot.getInstance()));
          //BotMoneyWidget.BotMoney.updateAndFlush(BotRequests.getMoney(GGBot.getInstance()));
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, GGBot.getInstance().configuration().botlogSub.minutes.get() * 60 * 1000);
  }
  public static void startTimerStatusCheck(long time) {
    timerStatus = new Timer(true);

    timerStatus.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        try {
          BotNameWidget.BotName.updateAndFlush(BotRequests.getName(GGBot.getInstance()));
          StatusWidget.Status.updateAndFlush(BotRequests.getStatus(GGBot.getInstance()));
          //BotMoneyWidget.BotMoney.updateAndFlush(BotRequests.getMoney(GGBot.getInstance()));
        } catch (ApiException e) {
          e.printStackTrace();
        }
      }
    }, 0, time * 60 * 1000);
  }
  public static void stopTimerStatusCheck(){
    if(timerStatus != null){
      timerStatus.cancel();
    }
  }
}
