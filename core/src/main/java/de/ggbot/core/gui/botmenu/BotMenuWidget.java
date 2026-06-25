package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.KickArea;
import de.ggbot.sdk.model.Ticket;
import de.ggbot.sdk.model.TicketMessage;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextColor;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Bot management menu: sidebar (bot card + tab navigation) and a content area that
 * swaps between Overview, Modules, Logs and Tools tabs. All user-facing strings
 * are translated via i18n keys under {@code ggbot.botmenu.*}.
 */
@AutoWidget
@Link("botmenu.lss")
public class BotMenuWidget extends DivWidget {

  private final Bot bot;
  private final GGBot addon = GGBot.getInstance();
  private Runnable closeAction;

  private VerticalListWidget<Widget> content;
  private ComponentWidget statusLabel;
  private TextFieldWidget searchField;
  private VerticalListWidget<Widget> searchResults;

  private final List<String> logLines = new ArrayList<>();
  private String logFilter = "";

  /** Tracks the active tab and the logs-tab widgets so logs can refresh in place. */
  private String currentTab = "overview";
  private VerticalListWidget<Widget> logListRef;
  private ButtonWidget logRefreshRef;

  /** Currently opened ticket in the Tickets tab; {@code null} shows the list. */
  private de.ggbot.sdk.model.Ticket selectedTicket;
  private boolean ticketsShowClosed = false;

  /** Polls the bot status while the menu is open; stopped in {@link #dispose()}. */
  private ScheduledExecutorService statusPoller;

  public BotMenuWidget(Bot bot) {
    this.bot = bot;
  }

  public void setCloseAction(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("bot-menu-widget");

    buildSidebar();

    DivWidget mainContainer = new DivWidget();
    mainContainer.addId("bot-menu-main");
    content = new VerticalListWidget<>();
    content.addId("bot-menu-content");
    ScrollWidget scroll = new ScrollWidget(content);
    scroll.addId("bot-menu-main-scroll");
    mainContainer.addChild(scroll);
    this.addChild(mainContainer);

    if (bot == null) {
      ComponentWidget none = ComponentWidget.i18n("ggbot.botmenu.noBot");
      none.addId("bot-menu-empty");
      content.addChild(none);
      return;
    }
    showTab("overview");
    refreshStatus();
    startStatusPoller();
  }

  /** Starts a periodic status refresh (every 5 seconds) while the menu is open. */
  private void startStatusPoller() {
    if (statusPoller != null) return;
    statusPoller = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "ggbot-menu-status");
      t.setDaemon(true);
      return t;
    });
    statusPoller.scheduleAtFixedRate(this::refreshStatus, 5, 5, TimeUnit.SECONDS);
  }

  /** {@inheritDoc} Stops the status poller when the menu screen closes. */
  @Override
  public void dispose() {
    if (statusPoller != null) {
      statusPoller.shutdownNow();
      statusPoller = null;
    }
    super.dispose();
  }

  private Component statusComponent(boolean online) {
    TextColor color = online ? NamedTextColor.GREEN : NamedTextColor.RED;
    // The bullet glyph is identical across languages, so only the label is translated.
    return Component.text("● ", color).append(Component.translatable(
        online ? "ggbot.botmenu.status.online" : "ggbot.botmenu.status.offline", color));
  }

  /** Re-fetches the bot list and updates the status label. */
  private void refreshStatus() {
    if (bot == null) return;
    BotRequests.updateBotListAsync(addon, () -> {
      Bot fresh = BotRequests.getBotFromCache(addon);
      boolean online = fresh != null ? Boolean.TRUE.equals(fresh.getOnline())
          : Boolean.TRUE.equals(bot.getOnline());
      if (statusLabel != null) statusLabel.setComponent(statusComponent(online));
    });
  }

  /** Refreshes the status shortly after a start/stop so it reflects the new state. */
  private void scheduleStatusRefresh() {
    Laby.labyAPI().minecraft().executeOnRenderThread(this::refreshStatus);
  }

  // ---- sidebar ------------------------------------------------------------

  private void buildSidebar() {
    DivWidget sidebar = new DivWidget();
    sidebar.addId("bot-menu-sidebar");

    VerticalListWidget<Widget> col = new VerticalListWidget<>();
    col.addId("bot-menu-sidebar-col");

    if (bot != null) {
      String linkName = bot.getLinkName();
      if (linkName != null && !linkName.isEmpty() && !"unknown".equals(linkName)) {
        IconWidget head = new IconWidget(Icon.head(linkName));
        head.addId("bot-menu-head");
        col.addChild(head);
      }
      ComponentWidget name = ComponentWidget.text(displayName(bot));
      name.addId("bot-menu-title");
      col.addChild(name);

      statusLabel = ComponentWidget.component(statusComponent(Boolean.TRUE.equals(bot.getOnline())));
      statusLabel.addId("bot-menu-status");
      col.addChild(statusLabel);

      HorizontalListWidget controls = new HorizontalListWidget();
      controls.addId("bot-menu-controls");
      ButtonWidget start = ButtonWidget.i18n("ggbot.botmenu.start");
      start.addId("bot-menu-start");
      withCooldown(start, () -> safe(() -> {
        BotRequests.startBot(addon, bot);
        scheduleStatusRefresh();
      }, "start"));
      controls.addEntry(start);
      ButtonWidget stop = ButtonWidget.i18n("ggbot.botmenu.stop");
      stop.addId("bot-menu-stop");
      withCooldown(stop, () -> safe(() -> {
        BotRequests.stopBot(addon, bot);
        scheduleStatusRefresh();
      }, "stop"));
      controls.addEntry(stop);
      col.addChild(controls);

      ButtonWidget changeBot = ButtonWidget.i18n("ggbot.botmenu.changeBot");
      changeBot.addId("bot-menu-changebot");
      changeBot.setPressable(() -> {
        de.ggbot.core.utils.GuiSounds.click();
        closeMenu();
        Laby.labyAPI().minecraft().executeOnRenderThread(() ->
            Laby.labyAPI().minecraft().minecraftWindow().displayScreen(
                new de.ggbot.core.gui.botselector.BotSelectorActivity()));
      });
      col.addChild(changeBot);

      ButtonWidget controlBtn = ButtonWidget.i18n("ggbot.botmenu.control.button");
      controlBtn.addId("bot-menu-controlmode");
      controlBtn.setPressable(() -> {
        de.ggbot.core.utils.GuiSounds.click();
        ControlModeManager.get().toggle(bot);
        closeMenu();
      });
      col.addChild(controlBtn);
    }

    col.addChild(tabButton("ggbot.botmenu.tab.overview", "overview"));
    col.addChild(tabButton("ggbot.botmenu.tab.modules", "modules"));
    col.addChild(tabButton("ggbot.botmenu.tab.logs", "logs"));
    col.addChild(tabButton("ggbot.botmenu.tab.tools", "tools"));
    col.addChild(tabButton("ggbot.botmenu.tab.tickets", "tickets"));

    ButtonWidget close = ButtonWidget.i18n("ggbot.botmenu.close");
    close.addId("bot-menu-close");
    close.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      closeMenu();
    });
    col.addChild(close);

    ScrollWidget sidebarScroll = new ScrollWidget(col);
    sidebarScroll.addId("bot-menu-sidebar-scroll");
    sidebar.addChild(sidebarScroll);
    this.addChild(sidebar);
  }

  private ButtonWidget tabButton(String labelKey, String tab) {
    ButtonWidget b = ButtonWidget.i18n(labelKey);
    b.addId("bot-menu-tab");
    b.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      showTab(tab);
    });
    return b;
  }

  private interface Action {
    void run() throws Exception;
  }

  private void safe(Action a, String what) {
    try {
      a.run();
    } catch (Exception e) {
      addon.logger().error("Failed to {} bot: {}", what, String.valueOf(e.getMessage()));
    }
  }

  /** Cooldown applied to action buttons to stop them being spammed. */
  private static final long BUTTON_COOLDOWN_MS = 1200L;

  /**
   * Wires a button so each press disables it, runs the action, and re-enables it
   * after a short cooldown. This prevents rapid repeated clicks (e.g. start/stop).
   */
  private void withCooldown(ButtonWidget button, Runnable action) {
    button.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      button.setEnabled(false);
      try {
        action.run();
      } finally {
        Thread t = new Thread(() -> {
          try {
            Thread.sleep(BUTTON_COOLDOWN_MS);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          }
          Laby.labyAPI().minecraft().executeOnRenderThread(() -> button.setEnabled(true));
        }, "ggbot-btn-cooldown");
        t.setDaemon(true);
        t.start();
      }
    });
  }

  // ---- tab dispatch -------------------------------------------------------

  private void showTab(String tab) {
    if (content == null) return;
    this.currentTab = tab;
    this.logListRef = null;
    this.logRefreshRef = null;
    content.removeChildIf(w -> true);
    switch (tab) {
      case "modules" -> buildModulesTab();
      case "logs" -> buildLogsTab();
      case "tools" -> buildToolsTab();
      case "tickets" -> buildTicketsTab();
      default -> buildOverviewTab();
    }
  }

  private void header(String key) {
    ComponentWidget h = ComponentWidget.i18n(key);
    h.addId("bot-menu-section-header");
    content.addChildInitialized(h);
  }

  // ---- Overview -----------------------------------------------------------

  private void buildOverviewTab() {
    header("ggbot.botmenu.section.command");
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("bot-menu-command-row");
    TextFieldWidget input = new TextFieldWidget();
    input.addId("bot-menu-command-input");
    input.maximalLength(256);
    input.placeholder(Component.translatable("ggbot.botmenu.command.placeholder"));
    ButtonWidget send = ButtonWidget.i18n("ggbot.botmenu.command.send");
    send.addId("bot-menu-command-send");
    send.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      String cmd = input.getText();
      if (cmd != null && !cmd.isBlank()) {
        BotRequests.sendCommandToBot(addon, bot, cmd.trim());
        input.setText("");
        refreshLogsAfterCommand();
      }
    });
    row.addEntry(input);
    row.addEntry(send);
    content.addChildInitialized(row);

    header("ggbot.botmenu.section.overlays");
    content.addChildInitialized(toggle("ggbot.botmenu.toggle.kickAreas",
        BotMenuState.get().isHighlightKickAreas(), v -> {
          BotMenuState.get().setHighlightKickAreas(v);
          addon.configuration().botMenuSub.highlightKickAreas.set(v);
          if (v) {
            BotRequests.getKickAreasAsync(addon, bot, KickAreaHighlighter::update);
          } else {
            KickAreaHighlighter.refresh();
          }
        }));
    content.addChildInitialized(toggle("ggbot.botmenu.toggle.chestContents",
        BotMenuState.get().isShowChestContents(), v -> {
          BotMenuState.get().setShowChestContents(v);
          addon.configuration().botMenuSub.showChestContents.set(v);
        }));
  }

  /** A toggle button (added directly so it always renders). */
  private ButtonWidget toggle(String labelKey, boolean initial, Consumer<Boolean> onChange) {
    ButtonWidget b = new ButtonWidget();
    b.addId("bot-menu-toggle");
    boolean[] state = {initial};
    b.updateComponent(toggleText(labelKey, state[0]));
    b.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      state[0] = !state[0];
      b.updateComponent(toggleText(labelKey, state[0]));
      onChange.accept(state[0]);
    });
    return b;
  }

  private Component toggleText(String labelKey, boolean on) {
    return Component.translatable(labelKey)
        .append(Component.text(": "))
        .append(Component.translatable(on ? "ggbot.botmenu.on" : "ggbot.botmenu.off",
            on ? NamedTextColor.GREEN : NamedTextColor.RED));
  }

  // ---- Modules ------------------------------------------------------------

  private void buildModulesTab() {
    // All module sections are always shown so the editors are reliably visible.
    // (Backend module-detection via getBotSettings proved unreliable and would
    // otherwise hide everything, so gating the editors was removed on purpose.)
    moduleSection("ggbot.botmenu.module.sell", "sell", new String[][]{
        {"sellAreaStart", "ggbot.botmenu.editor.sellAreaStart"},
        {"sellAreaEnd", "ggbot.botmenu.editor.sellAreaEnd"},
        {"sellDropLocation", "ggbot.botmenu.editor.sellDropLocation"}
    });
    // Show the whole sell area (start corner to end corner) as one spanning box.
    PositionEditorWidget.buildAreaToggle(content, "sell", "sellAreaStart", "sellAreaEnd",
        "ggbot.botmenu.editor.sellArea");
    content.addChildInitialized(actionButton("ggbot.botmenu.action.sellScan",
        () -> BotRequests.startSellScanAsync(addon, bot)));

    moduleSection("ggbot.botmenu.module.buy", "buy", new String[][]{
        {"chestOverrideLocation", "ggbot.botmenu.editor.chestOverride"}
    });
    content.addChildInitialized(actionButton("ggbot.botmenu.action.buyScan",
        () -> BotRequests.startBuyScanAsync(addon, bot)));

    moduleSection("ggbot.botmenu.module.ggfeatures", "ggfeatures",
        new String[][]{{"trashItemsIntoChest", "ggbot.botmenu.editor.trashChest"}});
    buildKickAreaSection();

    ComponentWidget utilsHeader = ComponentWidget.i18n("ggbot.botmenu.module.utils");
    utilsHeader.addId("bot-menu-section-header");
    content.addChildInitialized(utilsHeader);
    content.addChildInitialized(actionButton("ggbot.botmenu.action.followMe",
        () -> {
          var p = Laby.labyAPI().minecraft().getClientPlayer();
          if (p != null) BotRequests.followPlayerAsync(addon, bot, p.getName());
        }));
    content.addChildInitialized(actionButton("ggbot.botmenu.action.stopFollowMe",
        () -> BotRequests.stopFollowingAsync(addon, bot)));

    buildItemSearch();

    // Keep the cached module set fresh (used only for the player interactions).
    BotRequests.getActiveModulesAsync(addon, bot,
        modules -> BotMenuState.get().setActiveModules(modules));
  }

  private void buildKickAreaSection() {
    header("ggbot.botmenu.section.kickAreas");
    VerticalListWidget<Widget> kickList = new VerticalListWidget<>();
    kickList.addId("bot-menu-kickarea-list");
    content.addChildInitialized(kickList);
    BotRequests.getKickAreasAsync(addon, bot, areas -> {
      KickAreaHighlighter.update(areas);
      kickList.removeChildIf(w -> true);
      if (areas.isEmpty()) {
        kickList.addChildInitialized(dim("ggbot.botmenu.kickAreas.none"));
        return;
      }
      for (KickArea area : areas) {
        ComponentWidget line = ComponentWidget.text(formatKickArea(area));
        line.addId("bot-menu-kickarea-line");
        kickList.addChildInitialized(line);
      }
    });
  }

  // ---- Tickets ------------------------------------------------------------

  private void buildTicketsTab() {
    if (selectedTicket == null) {
      buildTicketList();
    } else {
      buildTicketDetail(selectedTicket);
    }
  }

  private void buildTicketList() {
    header("ggbot.botmenu.tickets.title");
    content.addChildInitialized(toggle("ggbot.botmenu.tickets.showClosed",
        ticketsShowClosed, v -> {
          ticketsShowClosed = v;
          showTab("tickets");
        }));

    VerticalListWidget<Widget> listW = new VerticalListWidget<>();
    listW.addId("bot-menu-ticket-list");
    content.addChildInitialized(listW);
    listW.addChildInitialized(dim("ggbot.botmenu.tickets.loading"));

    String status = ticketsShowClosed ? "closed" : "open";
    BotRequests.getTicketsForMenuAsync(addon, status, tickets -> {
      listW.removeChildIf(w -> true);
      if (tickets.isEmpty()) {
        listW.addChildInitialized(dim("ggbot.botmenu.tickets.empty"));
        return;
      }
      for (Ticket ticket : tickets) {
        listW.addChildInitialized(buildTicketRow(ticket));
      }
    });
  }

  private HorizontalListWidget buildTicketRow(Ticket ticket) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("bot-menu-ticket-row");

    String user = ticket.getUsername() == null ? "?" : ticket.getUsername();
    ComponentWidget name = ComponentWidget.component(
        Component.text(user + " (#" + ticket.getId() + ")"));
    name.addId("bot-menu-ticket-name");
    row.addEntry(name);

    String status = ticket.getStatus() == null ? "" : ticket.getStatus().getValue();
    ComponentWidget statusW = ComponentWidget.component(Component.text(status));
    statusW.addId("open".equalsIgnoreCase(status)
        ? "bot-menu-ticket-open" : "bot-menu-ticket-closed");
    row.addEntry(statusW);

    ButtonWidget view = ButtonWidget.i18n("ggbot.botmenu.tickets.view", () -> {
      de.ggbot.core.utils.GuiSounds.click();
      selectedTicket = ticket;
      showTab("tickets");
    });
    view.addId("bot-menu-ticket-btn");
    row.addEntry(view);

    return row;
  }

  private void buildTicketDetail(Ticket ticket) {
    String user = ticket.getUsername() == null ? "" : ticket.getUsername();

    content.addChildInitialized(actionButton("ggbot.botmenu.tickets.back", () -> {
      selectedTicket = null;
      showTab("tickets");
    }));

    ComponentWidget head = ComponentWidget.component(
        Component.text(user + " (#" + ticket.getId() + ")"));
    head.addId("bot-menu-section-header");
    content.addChildInitialized(head);

    VerticalListWidget<Widget> messages = new VerticalListWidget<>();
    messages.addId("bot-menu-ticket-messages");
    content.addChildInitialized(messages);
    messages.addChildInitialized(dim("ggbot.botmenu.tickets.loading"));

    int ticketId = ticket.getId() == null ? 0 : ticket.getId();
    BotRequests.getTicketMessagesAsync(addon, ticketId, list -> {
      messages.removeChildIf(w -> true);
      if (list.isEmpty()) {
        messages.addChildInitialized(dim("ggbot.botmenu.tickets.noMessages"));
        return;
      }
      for (TicketMessage message : list) {
        String sender = message.getSender() == null ? "?" : message.getSender();
        String type = message.getSenderType() == null ? "" : message.getSenderType().getValue();
        ComponentWidget line = ComponentWidget.component(
            Component.text(sender + ": " + (message.getMessage() == null ? "" : message.getMessage())));
        line.addId("staff".equalsIgnoreCase(type)
            ? "bot-menu-ticket-staff" : "bot-menu-ticket-user");
        messages.addChildInitialized(line);
      }
    });

    HorizontalListWidget respondRow = new HorizontalListWidget();
    respondRow.addId("bot-menu-command-row");
    TextFieldWidget input = new TextFieldWidget();
    input.addId("bot-menu-command-input");
    input.maximalLength(256);
    input.placeholder(Component.translatable("ggbot.botmenu.tickets.respondPlaceholder"));
    ButtonWidget send = ButtonWidget.i18n("ggbot.botmenu.tickets.respond");
    send.addId("bot-menu-command-send");
    withCooldown(send, () -> {
      String msg = input.getText();
      if (msg != null && !msg.isBlank()) {
        BotRequests.sendCommandToBot(addon, bot, "!respond " + user + " " + msg.trim());
        input.setText("");
        rebuildTicketsSoon();
      }
    });
    respondRow.addEntry(input);
    respondRow.addEntry(send);
    content.addChildInitialized(respondRow);

    content.addChildInitialized(actionButton("ggbot.botmenu.tickets.close", () -> {
      BotRequests.sendCommandToBot(addon, bot, "!close " + user);
      selectedTicket = null;
      rebuildTicketsSoon();
    }));
  }

  /** Rebuilds the Tickets tab shortly after a respond/close so changes show up. */
  private void rebuildTicketsSoon() {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(800L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        if ("tickets".equals(currentTab)) {
          showTab("tickets");
        }
      });
    }, "ggbot-tickets-refresh");
    thread.setDaemon(true);
    thread.start();
  }

  private ButtonWidget actionButton(String labelKey, Runnable onClick) {
    ButtonWidget b = ButtonWidget.i18n(labelKey);
    b.addId("bot-menu-action-btn");
    withCooldown(b, onClick);
    return b;
  }

  private void moduleSection(String titleKey, String module, String[][] editors) {
    ComponentWidget h = ComponentWidget.i18n(titleKey);
    h.addId("bot-menu-section-header");
    content.addChildInitialized(h);
    // Editors are appended as plain widgets (label + row) so they render reliably.
    for (String[] e : editors) {
      PositionEditorWidget.build(content, bot, module, e[0], e[1]);
    }
  }

  private void buildItemSearch() {
    header("ggbot.botmenu.section.itemSearch");
    HorizontalListWidget searchRow = new HorizontalListWidget();
    searchRow.addId("bot-menu-search-row");
    searchField = new TextFieldWidget();
    searchField.addId("bot-menu-search-input");
    searchField.placeholder(Component.translatable("ggbot.botmenu.search.placeholder"));
    searchField.setText(BotMenuState.get().getItemSearch());
    searchRow.addEntry(searchField);
    content.addChildInitialized(searchRow);

    searchResults = new VerticalListWidget<>();
    searchResults.addId("bot-menu-search-results");
    content.addChildInitialized(searchResults);

    searchField.updateListener(term -> {
      String clean = TextUtil.stripColors(term);
      BotMenuState.get().setItemSearch(clean);
      rebuildSearchResults(clean);
    });
    rebuildSearchResults(BotMenuState.get().getItemSearch());
  }

  /** Applies a search term to the field, state, and results immediately. */
  private void applySearch(String term) {
    String clean = TextUtil.stripColors(term);
    BotMenuState.get().setItemSearch(clean);
    if (searchField != null) {
      searchField.setText(clean);
    }
    rebuildSearchResults(clean);
  }

  private void rebuildSearchResults(String term) {
    if (searchResults == null) return;
    searchResults.removeChildIf(w -> true);
    if (term == null || term.isBlank()) return;
    List<ChestItemCache.Entry> matches = ChestItemCache.matching(term);
    if (matches.isEmpty()) {
      searchResults.addChildInitialized(dim("ggbot.botmenu.search.none"));
      return;
    }
    int shown = 0;
    for (ChestItemCache.Entry e : matches) {
      searchResults.addChildInitialized(buildItemRow(e));
      if (++shown >= 25) break;
    }
  }

  private HorizontalListWidget buildItemRow(ChestItemCache.Entry e) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("bot-menu-item-row");

    IconWidget icon = new IconWidget(ItemIcons.forItemType(e.itemType()));
    icon.addId("bot-menu-item-icon");
    row.addEntry(icon);

    Component label = Component.text(e.name(), NamedTextColor.YELLOW)
        .append(Component.text(" [" + e.shop() + "] @ " + e.x() + " " + e.y() + " " + e.z(),
            NamedTextColor.GRAY));
    ComponentWidget text = ComponentWidget.component(label);
    text.addId("bot-menu-item-label");
    row.addEntry(text);

    ButtonWidget pick = ButtonWidget.i18n("ggbot.botmenu.search.mark");
    pick.addId("bot-menu-item-pick");
    pick.setPressable(() -> applySearch(e.name()));
    row.addEntry(pick);
    return row;
  }

  // ---- Logs ---------------------------------------------------------------

  private void buildLogsTab() {
    header("ggbot.botmenu.section.logs");

    HorizontalListWidget bar = new HorizontalListWidget();
    bar.addId("bot-menu-log-bar");
    TextFieldWidget filter = new TextFieldWidget();
    filter.addId("bot-menu-log-filter");
    filter.placeholder(Component.translatable("ggbot.botmenu.logs.filter"));
    filter.setText(logFilter);
    bar.addEntry(filter);
    ButtonWidget refresh = ButtonWidget.i18n("ggbot.botmenu.logs.refresh");
    refresh.addId("bot-menu-log-refresh");
    bar.addEntry(refresh);
    content.addChildInitialized(bar);

    VerticalListWidget<Widget> logList = new VerticalListWidget<>();
    logList.addId("bot-menu-log");
    content.addChildInitialized(logList);
    this.logListRef = logList;
    this.logRefreshRef = refresh;

    filter.updateListener(term -> {
      logFilter = term == null ? "" : term;
      renderLogs(logList);
    });
    refresh.setPressable(() -> {
      refresh.setEnabled(false);
      loadLogs(logList, refresh);
    });
    loadLogs(logList, refresh);
  }

  /**
   * Refreshes the logs shortly after a command is sent (the bot needs a moment to
   * produce the new log lines): updates the chat log tab and, if the menu logs tab
   * is open, reloads it in place.
   */
  private void refreshLogsAfterCommand() {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(800L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      try {
        BotRequests.logsAsync();
      } catch (Exception ignored) {
        // Best-effort chat-tab refresh.
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        if ("logs".equals(currentTab) && logListRef != null) {
          loadLogs(logListRef, logRefreshRef);
        }
      });
    }, "ggbot-log-refresh");
    thread.setDaemon(true);
    thread.start();
  }

  private void loadLogs(VerticalListWidget<Widget> logList, ButtonWidget refresh) {
    logList.removeChildIf(w -> true);
    logList.addChildInitialized(dim("ggbot.botmenu.logs.loading"));
    BotRequests.getLogLinesAsync(addon, bot, lines -> {
      logLines.clear();
      logLines.addAll(lines);
      renderLogs(logList);
      if (refresh != null) refresh.setEnabled(true);
    });
  }

  private void renderLogs(VerticalListWidget<Widget> logList) {
    logList.removeChildIf(w -> true);
    String f = logFilter.toLowerCase();
    int shown = 0;
    for (String line : logLines) {
      if (!f.isEmpty() && !line.toLowerCase().contains(f)) continue;
      ComponentWidget w = ComponentWidget.text(line, logColor(line));
      w.addId("bot-menu-log-line");
      logList.addChildInitialized(w);
      if (++shown >= 200) break;
    }
    if (shown == 0) {
      logList.addChildInitialized(dim(logLines.isEmpty()
          ? "ggbot.botmenu.logs.none" : "ggbot.botmenu.search.none"));
    }
  }

  private TextColor logColor(String line) {
    String u = line.toUpperCase();
    if (u.contains("ERROR") || u.contains("SEVERE") || u.contains("FATAL")) return NamedTextColor.RED;
    if (u.contains("WARN")) return NamedTextColor.YELLOW;
    if (u.contains("INFO")) return NamedTextColor.GREEN;
    if (u.contains("DEBUG") || u.contains("TRACE")) return NamedTextColor.GRAY;
    return NamedTextColor.WHITE;
  }

  // ---- Tools (WorldEdit) --------------------------------------------------

  private void buildToolsTab() {
    header("ggbot.botmenu.section.tools");
    ComponentWidget loading = dim("ggbot.botmenu.logs.loading");
    content.addChildInitialized(loading);

    // WorldEdit controls are shown when the worldedit module is enabled. If module
    // detection is unavailable (empty), we show them anyway so the tab is usable.
    BotRequests.getActiveModulesAsync(addon, bot, modules -> {
      content.removeChildIf(w -> w == loading);
      if (!modules.isEmpty() && !modules.contains("worldedit")) {
        content.addChildInitialized(dim("ggbot.botmenu.tools.disabled"));
        return;
      }

      content.addChildInitialized(dim("ggbot.botmenu.tools.hint"));

      HorizontalListWidget row = new HorizontalListWidget();
      row.addId("bot-menu-worldedit-row");
      // Setting a position closes the menu so you can look around to pick blocks.
      row.addEntry(weButton("ggbot.botmenu.tools.pos1", () -> {
        BotMenuPositions.setFromLook(true);
        closeMenu();
      }));
      row.addEntry(weButton("ggbot.botmenu.tools.pos2", () -> {
        BotMenuPositions.setFromLook(false);
        closeMenu();
      }));
      content.addChildInitialized(row);

      HorizontalListWidget row2 = new HorizontalListWidget();
      row2.addId("bot-menu-worldedit-row");
      row2.addEntry(weButton("ggbot.botmenu.tools.send", () -> BotMenuPositions.sendToBot(bot)));
      // Clears the local pos1/pos2 selection and its overlay.
      row2.addEntry(weButton("ggbot.botmenu.tools.clear", BotMenuPositions::clearSelection));
      content.addChildInitialized(row2);
    });
  }

  private ButtonWidget weButton(String key, Runnable r) {
    ButtonWidget b = ButtonWidget.i18n(key);
    b.addId("bot-menu-we-btn");
    b.setPressable(r::run);
    return b;
  }

  // ---- helpers ------------------------------------------------------------

  private void closeMenu() {
    if (closeAction != null) closeAction.run();
  }

  private ComponentWidget dim(String key) {
    ComponentWidget w = ComponentWidget.i18n(key);
    w.addId("bot-menu-dim");
    return w;
  }

  private static String formatKickArea(KickArea area) {
    StringBuilder sb = new StringBuilder();
    sb.append(area.getName() == null ? "?" : TextUtil.stripColors(area.getName()));
    if (area.getType() != null) sb.append(" [").append(area.getType()).append("]");
    if (area.getMaxPlayerCount() != null) sb.append(" max=").append(area.getMaxPlayerCount());
    if (area.getAfkTime() != null) sb.append(" afk=").append(area.getAfkTime());
    if (area.getExceptions() != null && !area.getExceptions().isEmpty()) {
      sb.append(" except=").append(String.join(",", area.getExceptions()));
    }
    return sb.toString();
  }

  private static String displayName(Bot bot) {
    if (bot.getDescription() != null && !bot.getDescription().isEmpty()) {
      return TextUtil.stripColors(bot.getDescription());
    }
    String n = bot.getLinkName();
    if (n != null && !n.isEmpty() && !"unknown".equals(n)) return n;
    return "Bot #" + bot.getId();
  }
}
