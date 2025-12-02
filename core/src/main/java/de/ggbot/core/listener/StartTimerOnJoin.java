package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.widget.ggfeatures.BotMoneyWidget;
import de.ggbot.core.widget.info.BotNameWidget;
import de.ggbot.core.widget.ggfeatures.CitybuildWidget;
import de.ggbot.core.widget.ingame.HealthWidget;
import de.ggbot.core.widget.ggfeatures.PlotWidget;
import de.ggbot.core.widget.info.StatusWidget;
import de.ggbot.core.widget.ticket.TicketAmountWidget;
import de.ggbot.core.widget.ticket.TicketClosedAmountWidget;
import de.ggbot.core.widget.ticket.TicketOpenAmountWidget;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.Ticket;
import de.ggbot.sdk.model.Ticket.StatusEnum;
import net.labymod.api.client.component.Component;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class StartTimerOnJoin {

  private static Timer logTimer;
  private static Timer statusTimer;
  private static Timer statsTimer;


  public StartTimerOnJoin(GGBot addon) {}

  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
    cancel(logTimer);
    cancel(statusTimer);
    cancel(statsTimer);
  }

  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    long intervalLog = GGBot.getInstance().configuration().botlogSub.minutes.get() * 60 * 1000L;
    long intervalStatus = GGBot.getInstance().configuration().statusMinutes.get() * 60 * 1000L;
    long intervalStats = GGBot.getInstance().configuration().statsMinute.get() * 60 * 1000L;

    startLogTimer(intervalLog);
    startStatusTimer(intervalStatus);
    startStatsTimer(intervalStats);
  }

  private static void cancel(Timer t) {
    if (t != null) t.cancel();
  }

  public static void startLogTimer(long interval) {
    if (!GGBot.getInstance().configuration().botlogSub.botLog.get()){
      return;
    }
      logTimer = new Timer(true);
    logTimer.scheduleAtFixedRate(timerTask(() -> {
      try { BotRequests.logsAsync(); }
      catch (ApiException ignored) {}
    }), 0, interval);
  }

  public static void startStatusTimer(long interval) {
    statusTimer = new Timer(true);
    statusTimer.scheduleAtFixedRate(timerTask(() -> {
      if (GGBot.isAuth) {
        if (GGBot.getInstance().labyAPI().serverController().isConnected()) {
          try {
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new BotNameWidget().getId())
                .isEnabled()) {
              BotNameWidget.BotName.updateAndFlush(BotRequests.getName(GGBot.getInstance()));
            }
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new StatusWidget().getId())
                .isEnabled()) {
              StatusWidget.Status.updateAndFlush(BotRequests.getStatus(GGBot.getInstance()));
            }
          } catch (ApiException ignored) {
          }
        }
      }
    }), 0, interval);
  }

  public static void startStatsTimer(long interval) {
    statsTimer = new Timer(true);
    statsTimer.scheduleAtFixedRate(timerTask(() -> {
      if (GGBot.isAuth) {
        if (GGBot.getInstance().labyAPI().serverController().isConnected()) {
          try {
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new BotMoneyWidget().getId()).isEnabled())
              BotRequests.getMoney(GGBot.getInstance(), (Consumer<Double>) money ->
                  BotMoneyWidget.BotMoney.updateAndFlush(
                      Component.text(money).append(Component.translatable("ggbot.widget.money.unit")))
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new HealthWidget().getId()).isEnabled())
              BotRequests.getHealth(GGBot.getInstance(), (Consumer<Double>) health ->
                  HealthWidget.Health.updateAndFlush(
                      Component.text(health + " ").append(Component.translatable("ggbot.widget.health.unit")))
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new CitybuildWidget().getId()).isEnabled())
              BotRequests.getCitybuild(GGBot.getInstance(), (Consumer<String>) cb ->
                  CitybuildWidget.Citybuild.updateAndFlush(cb)
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new PlotWidget().getId()).isEnabled())
              BotRequests.getPlot(GGBot.getInstance(), (Consumer<String>) plot ->
                  PlotWidget.Plot.updateAndFlush(plot)
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new TicketAmountWidget().getId()).isEnabled())
              BotRequests.getTickets(GGBot.getInstance(), "", (Consumer<List<Ticket>>) tickets ->
                  TicketAmountWidget.TicketAmount.updateAndFlush(tickets.size())
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new TicketOpenAmountWidget().getId()).isEnabled())
              BotRequests.getTickets(GGBot.getInstance(), StatusEnum.OPEN.getValue(), (Consumer<List<Ticket>>) tickets ->
                  TicketOpenAmountWidget.TicketAmount.updateAndFlush(tickets.size())
              );
            if (GGBot.getInstance().labyAPI().hudWidgetRegistry()
                .getById(new TicketClosedAmountWidget().getId()).isEnabled())
              BotRequests.getTickets(GGBot.getInstance(), StatusEnum.CLOSED.getValue(), (Consumer<List<Ticket>>) tickets ->
                  TicketClosedAmountWidget.TicketAmount.updateAndFlush(tickets.size())
              );
          } catch (ApiException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }), 0, interval);
  }

  public static void cancelLogTimer() {
    cancel(logTimer);
  }

  public static void cancelStatusTimer() {
    cancel(statusTimer);
  }

  public static void cancelStatsTimer() {
    cancel(statsTimer);
  }

  private static TimerTask timerTask(Runnable r) {
    return new TimerTask() {
      @Override public void run() { r.run(); }
    };
  }
}
