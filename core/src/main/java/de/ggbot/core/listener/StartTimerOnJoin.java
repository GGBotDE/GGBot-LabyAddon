package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.core.widget.afk.AfkBotsWidget;
import de.ggbot.core.widget.afk.AfkFleetsWidget;
import de.ggbot.core.widget.ggfeatures.BotMoneyWidget;
import de.ggbot.core.widget.ggfeatures.CitybuildWidget;
import de.ggbot.core.widget.ggfeatures.PlotWidget;
import de.ggbot.core.widget.ingame.FoodWidget;
import de.ggbot.core.widget.ingame.HealthWidget;
import de.ggbot.core.widget.ingame.LevelWidget;
import de.ggbot.core.widget.ingame.PositionWidget;
import de.ggbot.core.widget.info.BotNameWidget;
import de.ggbot.core.widget.info.StatusWidget;
import de.ggbot.core.widget.ticket.TicketAmountWidget;
import de.ggbot.core.widget.ticket.TicketClosedAmountWidget;
import de.ggbot.core.widget.ticket.TicketOpenAmountWidget;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.Position;
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

/**
 * Starts and manages periodic background timers that fetch bot data
 * (logs, status, statistics) while the player is connected to a server.
 *
 * <p>All timers are cancelled on server disconnect and restarted on reconnect.
 */
public class StartTimerOnJoin {

  private final GGBot addon;

  /**
   * Delays log start to allow chat tabs to settle after joining.
   */
  private static final long LOG_START_DELAY_MS = 5_000L;

  private Timer logTimer;
  private Timer statusTimer;
  private Timer statsTimer;

  /**
   * Creates a new timer listener.
   *
   * @param addon the addon instance used to access configuration and feature flags
   */
  public StartTimerOnJoin(GGBot addon) {
    this.addon = addon;
  }

  /**
   * Cancels all running timers when the player disconnects.
   *
   * @param e the server disconnect event
   */
  @Subscribe
  public void onServerQuit(ServerDisconnectEvent e) {
    cancelLogTimer();
    cancelStatusTimer();
    cancelStatsTimer();
  }

  /**
   * Starts all configured timers when the player connects to a server.
   * Intervals are read from the addon configuration.
   *
   * @param e the server join event
   */
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.timers")) return;
    startLogTimer(addon.configuration().botlogSub.minutes.get() * 60_000L);
    startStatusTimer(addon.configuration().statusSub.statusMinutes.get() * 60_000L);
    startStatsTimer(addon.configuration().statsSub.statsMinutes.get() * 60_000L);
  }

  /**
   * Starts the log-fetching timer with the given interval.
   * Does nothing if the log feature flag or the bot-log setting is disabled.
   *
   * @param intervalMs interval between fetches in milliseconds
   */
  public void startLogTimer(long intervalMs) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.timers.logs")) return;
    if (!addon.configuration().botlogSub.botLog.get()) return;
    logTimer = new Timer(true);
    logTimer.scheduleAtFixedRate(wrapTask(() -> {
      try { BotRequests.logsAsync(); }
      catch (ApiException ignored) {}
    }), LOG_START_DELAY_MS,
        addon.getVersioningHandler().clampIntervalMs("de.ggbot.addon.timers.logs", intervalMs));
  }

  /**
   * Starts the status-fetching timer with the given interval.
   * Updates the bot-name and status widgets when they are enabled.
   *
   * @param intervalMs interval between fetches in milliseconds
   */
  public void startStatusTimer(long intervalMs) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.timers.status")) return;
    if (!addon.configuration().statusSub.statusEnabled.get()) return;
    statusTimer = new Timer(true);
    statusTimer.scheduleAtFixedRate(wrapTask(this::updateStatusWidgets), 0,
        addon.getVersioningHandler().clampIntervalMs("de.ggbot.addon.timers.status", intervalMs));
  }

  /** Refreshes the bot cache and the name/status widgets from it. */
  private void updateStatusWidgets() {
    if (!GGBot.isAuthenticated() || !addon.labyAPI().serverController().isConnected()) return;
    // Refresh the cache first so getName/getStatus read up-to-date data.
    try { BotRequests.updateBotList(addon); } catch (ApiException ignored) {}
    var botNameWidget = addon.labyAPI().hudWidgetRegistry().getById(BotNameWidget.WIDGET_ID);
    if (botNameWidget != null && botNameWidget.isEnabled())
      BotNameWidget.update(BotRequests.getName(addon));

    var statusWidget = addon.labyAPI().hudWidgetRegistry().getById(StatusWidget.WIDGET_ID);
    if (statusWidget != null && statusWidget.isEnabled())
      StatusWidget.update(BotRequests.getStatus(addon));
  }

  /**
   * Starts the statistics-fetching timer with the given interval.
   * Updates money, health, CityBuild, plot, and ticket widgets when they are enabled.
   *
   * @param intervalMs interval between fetches in milliseconds
   */
  public void startStatsTimer(long intervalMs) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.timers.stats")) return;
    if (!addon.configuration().statsSub.statsEnabled.get()) return;
    statsTimer = new Timer(true);
    statsTimer.scheduleAtFixedRate(wrapTask(this::updateStatsWidgets), 0,
        addon.getVersioningHandler().clampIntervalMs("de.ggbot.addon.timers.stats", intervalMs));
  }

  /** Fetches and updates all enabled statistics widgets (money, health, tickets, ...). */
  private void updateStatsWidgets() {
    if (!GGBot.isAuthenticated() || !addon.labyAPI().serverController().isConnected()) return;
    try {
        boolean ggfeatures = BotMenuState.get().isModuleAvailable("ggfeatures");
        boolean ticketsActive = BotMenuState.get().isModuleAvailable("tickets");

        var moneyWidget = addon.labyAPI().hudWidgetRegistry().getById(BotMoneyWidget.WIDGET_ID);
        if (ggfeatures && moneyWidget != null && moneyWidget.isEnabled())
          BotRequests.getMoney(addon, (Consumer<Double>) money ->
              BotMoneyWidget.update(Component.text(money)
                  .append(Component.translatable("ggbot.widget.money.unit"))));

        var healthWidget = addon.labyAPI().hudWidgetRegistry().getById(HealthWidget.WIDGET_ID);
        if (healthWidget != null && healthWidget.isEnabled())
          BotRequests.getHealth(addon, (Consumer<Double>) health ->
              HealthWidget.update(Component.text(health + " ")
                  .append(Component.translatable("ggbot.widget.health.unit"))));

        var cbWidget = addon.labyAPI().hudWidgetRegistry().getById(CitybuildWidget.WIDGET_ID);
        if (ggfeatures && cbWidget != null && cbWidget.isEnabled())
          BotRequests.getCitybuild(addon, CitybuildWidget::update);

        var plotWidget = addon.labyAPI().hudWidgetRegistry().getById(PlotWidget.WIDGET_ID);
        if (ggfeatures && plotWidget != null && plotWidget.isEnabled())
          BotRequests.getPlot(addon, PlotWidget::update);

        var ticketWidget = addon.labyAPI().hudWidgetRegistry().getById(TicketAmountWidget.WIDGET_ID);
        if (ticketsActive && ticketWidget != null && ticketWidget.isEnabled())
          BotRequests.getTickets(addon, "", (Consumer<List<Ticket>>) tickets ->
              TicketAmountWidget.update(tickets.size()));

        var openWidget = addon.labyAPI().hudWidgetRegistry().getById(TicketOpenAmountWidget.WIDGET_ID);
        if (ticketsActive && openWidget != null && openWidget.isEnabled())
          BotRequests.getTickets(addon, StatusEnum.OPEN.getValue(), (Consumer<List<Ticket>>) tickets ->
              TicketOpenAmountWidget.update(tickets.size()));

        var closedWidget = addon.labyAPI().hudWidgetRegistry().getById(TicketClosedAmountWidget.WIDGET_ID);
        if (ticketsActive && closedWidget != null && closedWidget.isEnabled())
          BotRequests.getTickets(addon, StatusEnum.CLOSED.getValue(), (Consumer<List<Ticket>>) tickets ->
              TicketClosedAmountWidget.update(tickets.size()));

        updateIngameWidgets();

        if (isWidgetEnabled(AfkBotsWidget.WIDGET_ID) || isWidgetEnabled(AfkFleetsWidget.WIDGET_ID))
          BotRequests.refreshAfkWidgets(addon);

      } catch (ApiException e) {
        addon.logger().error("Failed to fetch bot statistics: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      }
  }

  /**
   * Updates in-game widgets with live data to avoid unnecessary API calls.
   */
  private void updateIngameWidgets() {
    Bot bot = BotRequests.getBotFromCache(addon);
    if (bot == null) return;

    if (isWidgetEnabled(FoodWidget.WIDGET_ID)) {
      Double food = BotRequests.getFood(addon, bot);
      if (food != null) FoodWidget.update(FoodWidget.format(food));
    }

    if (isWidgetEnabled(LevelWidget.WIDGET_ID)) {
      Integer level = BotRequests.getLevel(addon, bot);
      if (level != null) LevelWidget.update(level);
    }

    if (isWidgetEnabled(PositionWidget.WIDGET_ID)) {
      Position position = BotRequests.getBotLocation(addon, bot);
      if (position != null) PositionWidget.update(PositionWidget.format(position));
    }
  }

  private boolean isWidgetEnabled(String id) {
    var widget = addon.labyAPI().hudWidgetRegistry().getById(id);
    return widget != null && widget.isEnabled();
  }

  /**
   * Immediately refreshes all enabled bot widgets on a background thread. Used to
   * update the HUD instantly when the selected bot changes, instead of waiting for
   * the next periodic timer tick.
   */
  public void refreshNow() {
    BotRequests.onSelectedBotChanged(addon);
    Thread thread = new Thread(() -> {
      updateStatusWidgets();
      updateStatsWidgets();
    }, "ggbot-widget-refresh");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Cancels and clears the log-fetching timer.
   */
  public void cancelLogTimer() {
    if (logTimer != null) { logTimer.cancel(); logTimer = null; }
  }

  /**
   * Cancels and clears the status-fetching timer.
   */
  public void cancelStatusTimer() {
    if (statusTimer != null) { statusTimer.cancel(); statusTimer = null; }
  }

  /**
   * Cancels and clears the statistics-fetching timer.
   */
  public void cancelStatsTimer() {
    if (statsTimer != null) { statsTimer.cancel(); statsTimer = null; }
  }

  /**
   * Cancels every running timer at once. Used when the addon is disabled
   * mid-session.
   */
  public void cancelAllTimers() {
    cancelLogTimer();
    cancelStatusTimer();
    cancelStatsTimer();
  }

  /**
   * Wraps a {@link Runnable} in a {@link TimerTask} for use with
   * {@link Timer#scheduleAtFixedRate}.
   *
   * <p>The event bus stops delivering events to disabled addons, so the
   * {@link ServerDisconnectEvent} that normally cancels these timers never
   * arrives once the user disables the addon mid-session. Each tick therefore
   * re-checks the enabled switch itself and shuts every timer down instead of
   * polling the API until the game closes.
   *
   * @param runnable the task body
   * @return a {@link TimerTask} that delegates to the runnable
   */
  private TimerTask wrapTask(Runnable runnable) {
    return new TimerTask() {
      @Override public void run() {
        if (!Boolean.TRUE.equals(addon.configuration().enabled().get())) {
          cancelAllTimers();
          return;
        }
        runnable.run();
      }
    };
  }
}
