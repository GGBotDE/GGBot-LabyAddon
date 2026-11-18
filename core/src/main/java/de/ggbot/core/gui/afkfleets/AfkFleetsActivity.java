package de.ggbot.core.gui.afkfleets;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.AfkOverview;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.cbx.AfkCbxManager;
import de.ggbot.core.utils.TextUtil;
import de.ggbot.sdk.model.AfkBot;
import de.ggbot.sdk.model.AfkFleet;
import de.ggbot.sdk.model.AfkInspect;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

/**
 * Provides the GUI for managing the linked account's AFK fleets.
 * The main screen displays the available fleets, while the second screen
 * shows the bots assigned to the selected fleet.
 * Actions are applied to the selected bots, or to the entire fleet if
 * no bots have been selected.
 * Data is fetched when the GUI is opened and whenever a manual refresh
 * is triggered. No automatic polling occurs while the GUI is active.
 * Messaging, movement and inspection are handled through the AFK CBX
 * session when available, with HTTP endpoints used as a fallback.
 */
@AutoActivity
@Link("afkfleets.lss")
public class AfkFleetsActivity extends SimpleActivity {

  /**
   * Defines the delay before reloading a bot's state after start or stop.
   */
  private static final long RELOAD_DELAY_MS = 3_000L;

  private static final double WALK_BLOCKS = 1.0;
  private static final double TURN_DEGREES = 45.0;

  private final GGBot addon;
  private final Set<String> picked = new HashSet<>();
  private final List<ButtonWidget> onlineButtons = new ArrayList<>();

  private AfkOverview overview = new AfkOverview(new ArrayList<>(), new ArrayList<>());
  private VerticalListWidget<Widget> list;
  private ComponentWidget targetLabel;
  private ButtonWidget startButton;
  private ButtonWidget stopButton;
  private ButtonWidget fleetButton;
  private AfkFleet selected;
  private AfkBot selectedBot;
  private String search = "";
  private boolean loaded = false;

  public AfkFleetsActivity(GGBot addon) {
    this.addon = addon;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();

    DivWidget panel = new DivWidget();
    panel.addId("afkfleets-panel");

    ComponentWidget header = buildHeader();
    header.addId("afkfleets-header");
    panel.addChild(header);

    panel.addChild(selected == null ? buildSearchControls() : buildMessageControls());

    list = new VerticalListWidget<>();
    list.addId("afkfleets-list");
    ScrollWidget scroll = new ScrollWidget(list);
    scroll.addId("afkfleets-scroll");
    panel.addChild(scroll);

    HorizontalListWidget buttons = new HorizontalListWidget();
    buttons.addId("afkfleets-buttons");
    if (selected != null) {
      ButtonWidget back = ButtonWidget.i18n("ggbot.afkfleets.back", () -> {
        de.ggbot.core.utils.GuiSounds.click();
        if (selectedBot != null) {
          selectedBot = null;
        } else {
          selected = null;
          picked.clear();
        }
        reload();
      });
      back.addId("afkfleets-back");
      buttons.addEntry(back);
    }
    ButtonWidget refresh = ButtonWidget.i18n("ggbot.afkfleets.refresh");
    refresh.addId("afkfleets-refresh");
    refresh.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      fetch();
    });
    buttons.addEntry(refresh);
    ButtonWidget close = ButtonWidget.i18n("ggbot.afkfleets.close", this::closeScreen);
    close.addId("afkfleets-close");
    buttons.addEntry(close);
    panel.addChild(buttons);

    this.document().addChild(panel);

    rebuildRows();
    if (!GGBot.isAuthenticated()) return;
    AfkCbxManager.get().open();
    if (!loaded) {
      fetch();
    }
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    AfkCbxManager.get().close();
  }

  private DivWidget buildSearchControls() {
    DivWidget controls = new DivWidget();
    controls.addId("afkfleets-controls");
    TextFieldWidget searchField = new TextFieldWidget();
    searchField.addId("afkfleets-search");
    searchField.placeholder(Component.translatable("ggbot.afkfleets.searchPlaceholder"));
    searchField.setText(search);
    searchField.updateListener(text -> {
      search = text == null ? "" : text.toLowerCase(Locale.ROOT);
      rebuildRows();
    });
    controls.addChild(searchField);
    return controls;
  }

  private DivWidget buildMessageControls() {
    DivWidget controls = new DivWidget();
    controls.addId("afkfleets-controls");
    TextFieldWidget input = new TextFieldWidget();
    input.addId("afkfleets-input");
    input.maximalLength(256);
    input.placeholder(Component.translatable(selectedBot == null
        ? "ggbot.afkfleets.chatPlaceholder" : "ggbot.afkfleets.botChatPlaceholder"));
    input.submitHandler(text -> sendMessage(input));
    controls.addChild(input);
    ButtonWidget send = ButtonWidget.i18n("ggbot.afkfleets.send", () -> sendMessage(input));
    send.addId("afkfleets-send");
    controls.addChild(send);
    return controls;
  }

  private ComponentWidget buildHeader() {
    if (selectedBot != null) return ComponentWidget.text(botName(selectedBot));
    if (selected != null) return ComponentWidget.text(TextUtil.stripColors(selected.getName()));
    return ComponentWidget.i18n("ggbot.afkfleets.title");
  }

  private void sendMessage(TextFieldWidget input) {
    String text = input.getText();
    if (text == null || text.isBlank() || selected == null) return;
    List<String> tokens = AfkOverview.tokens(targets(), AfkOverview::isOnline);
    if (tokens.isEmpty()) return;
    de.ggbot.core.utils.GuiSounds.click();
    if (controlsWholeFleet()) {
      BotRequests.chatAfkFleetAsync(addon, selected, tokens, text.trim());
    } else {
      BotRequests.chatAfkBotsAsync(addon, tokens, text.trim());
    }
    input.setText("");
  }

  private boolean controlsWholeFleet() {
    return selectedBot == null && picked.isEmpty();
  }

  private List<AfkBot> targets() {
    if (selectedBot != null) return List.of(selectedBot);
    return AfkOverview.pick(overview.botsOf(selected), picked);
  }

  private void fetch() {
    BotRequests.getAfkOverviewAsync(addon, result -> {
      overview = result;
      loaded = true;
      picked.retainAll(AfkOverview.tokens(overview.botsOf(selected), bot -> true));
      boolean inFleet = selected != null;
      if (AfkOverview.onlineCount(overview.getBots()) > 0) {
        AfkCbxManager.get().ensureConnected();
      }
      if (selectedBot != null) {
        selectedBot = findBot(selectedBot.getToken());
        if (selectedBot == null) {
          reload();
          return;
        }
        selected = overview.findFleet(selectedBot.getFleet());
      } else if (selected != null) {
        selected = overview.findFleet(selected.getId());
      }
      if (selected == null && inFleet) {
        selectedBot = null;
        reload();
        return;
      }
      rebuildRows();
    });
  }

  private AfkBot findBot(String token) {
    for (AfkBot bot : overview.getBots()) {
      if (Objects.equals(bot.getToken(), token)) return bot;
    }
    return null;
  }

  /**
   * Schedules a delayed reload to ensure state changes are effective before refreshing data.
   */
  private void fetchDelayed() {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(RELOAD_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(this::fetch);
    }, "ggbot-afk-reload");
    thread.setDaemon(true);
    thread.start();
  }

  private void rebuildRows() {
    if (list == null) return;
    list.removeChildIf(w -> true);
    onlineButtons.clear();
    targetLabel = null;

    if (!GGBot.isAuthenticated()) {
      addStatus("ggbot.afkfleets.login");
      return;
    }
    if (!loaded) {
      addStatus("ggbot.afkfleets.loading");
      return;
    }
    if (selectedBot != null) {
      list.addChildInitialized(buildBotRow(selectedBot));
      addControls();
      updateTargets();
      return;
    }
    if (selected != null) {
      List<AfkBot> bots = overview.botsOf(selected);
      if (bots.isEmpty()) {
        addStatus("ggbot.afkfleets.noBots");
        return;
      }
      addTargetRow();
      addControls();
      for (AfkBot bot : bots) {
        list.addChildInitialized(buildBotRow(bot));
      }
      updateTargets();
      return;
    }

    int shown = 0;
    for (AfkFleet fleet : overview.getFleets()) {
      if (!search.isEmpty() && !matches(fleet)) continue;
      list.addChildInitialized(buildFleetRow(fleet));
      shown++;
    }
    if (shown == 0) {
      addStatus(overview.getFleets().isEmpty()
          ? "ggbot.afkfleets.empty" : "ggbot.afkfleets.noMatch");
    }
  }

  private boolean matches(AfkFleet fleet) {
    String name = fleet.getName() == null ? "" : fleet.getName();
    String server = fleet.getServer() == null ? "" : fleet.getServer();
    return (name + " " + server).toLowerCase(Locale.ROOT).contains(search);
  }

  private void addStatus(String key) {
    ComponentWidget status = ComponentWidget.i18n(key);
    status.addId("afkfleets-status");
    list.addChildInitialized(status);
  }

  private HorizontalListWidget buildFleetRow(AfkFleet fleet) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("afkfleets-row");

    ComponentWidget name = ComponentWidget.text(TextUtil.stripColors(fleet.getName()));
    name.addId("afkfleets-name");
    row.addEntry(name);

    if (fleet.getServer() != null) {
      ComponentWidget server = ComponentWidget.text(fleet.getServer());
      server.addId("afkfleets-server");
      row.addEntry(server);
    }

    List<AfkBot> bots = overview.botsOf(fleet);
    int online = AfkOverview.onlineCount(bots);
    ComponentWidget count = ComponentWidget.text(online + "/" + bots.size());
    count.addId(online > 0 ? "afkfleets-online" : "afkfleets-offline");
    row.addEntry(count);

    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("afkfleets-actions");
    actions.addEntry(actionButton("ggbot.afkfleets.start", "afkfleets-start", online < bots.size(),
        () -> BotRequests.startAfkFleetAsync(addon, fleet, this::onStateRequested)));
    actions.addEntry(actionButton("ggbot.afkfleets.stop", "afkfleets-stop", online > 0,
        () -> BotRequests.stopAfkFleetAsync(addon, fleet, this::onStateRequested)));
    ButtonWidget open = ButtonWidget.i18n("ggbot.afkfleets.open", () -> {
      de.ggbot.core.utils.GuiSounds.click();
      selected = fleet;
      reload();
    });
    open.addId("afkfleets-open");
    actions.addEntry(open);
    row.addEntry(actions);
    return row;
  }

  private VerticalListWidget<Widget> buildBotRow(AfkBot bot) {
    VerticalListWidget<Widget> entry = new VerticalListWidget<>();
    entry.addId("afkfleets-bot-entry");

    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("afkfleets-row");

    if (selectedBot == null) row.addEntry(buildPick(bot));

    String linkName = bot.getLinkName();
    boolean hasName = linkName != null && !linkName.isEmpty() && !"unknown".equals(linkName);
    if (hasName) {
      IconWidget head = new IconWidget(Icon.head(linkName));
      head.addId("afkfleets-head");
      row.addEntry(head);
    }
    ComponentWidget name = ComponentWidget.text(botName(bot));
    name.addId("afkfleets-name");
    row.addEntry(name);

    boolean online = AfkOverview.isOnline(bot);
    ComponentWidget status = ComponentWidget.i18n(statusKey(bot));
    status.addId(online ? "afkfleets-online" : "afkfleets-offline");
    row.addEntry(status);

    ComponentWidget details = ComponentWidget.empty();
    details.addId("afkfleets-details");

    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("afkfleets-actions");
    actions.addEntry(actionButton("ggbot.afkfleets.start", "afkfleets-start", !online,
        () -> BotRequests.startAfkBotsAsync(addon, List.of(bot.getToken()),
            this::onStateRequested)));
    actions.addEntry(actionButton("ggbot.afkfleets.stop", "afkfleets-stop", online,
        () -> BotRequests.stopAfkBotsAsync(addon, List.of(bot.getToken()),
            this::onStateRequested)));
    if (selectedBot == null) {
      ButtonWidget manage = ButtonWidget.i18n("ggbot.afkfleets.manage", () -> {
        de.ggbot.core.utils.GuiSounds.click();
        selectedBot = bot;
        reload();
      });
      manage.addId("afkfleets-open");
      actions.addEntry(manage);
    } else {
      ButtonWidget inspect = ButtonWidget.i18n("ggbot.afkfleets.inspect", () -> {
        de.ggbot.core.utils.GuiSounds.click();
        BotRequests.inspectAfkBotAsync(addon, bot, result -> showInspect(details, result));
      });
      inspect.addId("afkfleets-open");
      inspect.setEnabled(online);
      actions.addEntry(inspect);
    }
    row.addEntry(actions);

    entry.addChildInitialized(row);
    entry.addChildInitialized(details);
    return entry;
  }

  private CheckBoxWidget buildPick(AfkBot bot) {
    CheckBoxWidget box = new CheckBoxWidget();
    box.addId("afkfleets-pick");
    box.setState(picked.contains(bot.getToken())
        ? CheckBoxWidget.State.CHECKED : CheckBoxWidget.State.UNCHECKED);
    box.setPressable(() -> {
      if (box.state() == CheckBoxWidget.State.CHECKED) {
        picked.add(bot.getToken());
      } else {
        picked.remove(bot.getToken());
      }
      updateTargets();
    });
    return box;
  }

  private void addTargetRow() {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("afkfleets-row");

    targetLabel = ComponentWidget.empty();
    targetLabel.addId("afkfleets-name");
    row.addEntry(targetLabel);

    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("afkfleets-actions");
    startButton = actionButton("ggbot.afkfleets.start", "afkfleets-start", true, this::startTargets);
    actions.addEntry(startButton);
    stopButton = actionButton("ggbot.afkfleets.stop", "afkfleets-stop", true, this::stopTargets);
    actions.addEntry(stopButton);
    fleetButton = ButtonWidget.i18n("ggbot.afkfleets.wholeFleet", () -> {
      de.ggbot.core.utils.GuiSounds.click();
      picked.clear();
      rebuildRows();
    });
    fleetButton.addId("afkfleets-open");
    actions.addEntry(fleetButton);
    row.addEntry(actions);
    list.addChildInitialized(row);
  }

  private void startTargets() {
    if (controlsWholeFleet()) {
      BotRequests.startAfkFleetAsync(addon, selected, this::onStateRequested);
    } else {
      BotRequests.startAfkBotsAsync(addon,
          AfkOverview.tokens(targets(), bot -> !AfkOverview.isOnline(bot)), this::onStateRequested);
    }
  }

  private void stopTargets() {
    if (controlsWholeFleet()) {
      BotRequests.stopAfkFleetAsync(addon, selected, this::onStateRequested);
    } else {
      BotRequests.stopAfkBotsAsync(addon,
          AfkOverview.tokens(targets(), AfkOverview::isOnline), this::onStateRequested);
    }
  }

  private void updateTargets() {
    List<AfkBot> targets = targets();
    int online = AfkOverview.tokens(targets, AfkOverview::isOnline).size();
    onlineButtons.forEach(button -> button.setEnabled(online > 0));
    if (targetLabel == null) return;
    targetLabel.setComponent(Component.translatable(picked.isEmpty()
        ? "ggbot.afkfleets.target.fleet" : "ggbot.afkfleets.target.selected",
        Component.text(online + "/" + targets.size())));
    startButton.setEnabled(online < targets.size());
    stopButton.setEnabled(online > 0);
    fleetButton.setEnabled(!picked.isEmpty());
  }

  private void addControls() {
    HorizontalListWidget walk = new HorizontalListWidget();
    walk.addId("afkfleets-pad");
    walk.addEntry(padButton("ggbot.afkfleets.walk.forward", WALK_BLOCKS, 0, 0, false));
    walk.addEntry(padButton("ggbot.afkfleets.walk.back", -WALK_BLOCKS, 0, 0, false));
    walk.addEntry(padButton("ggbot.afkfleets.walk.left", 0, WALK_BLOCKS, 0, false));
    walk.addEntry(padButton("ggbot.afkfleets.walk.right", 0, -WALK_BLOCKS, 0, false));
    list.addChildInitialized(walk);

    HorizontalListWidget turn = new HorizontalListWidget();
    turn.addId("afkfleets-pad");
    turn.addEntry(padButton("ggbot.afkfleets.walk.turnLeft", 0, 0, -TURN_DEGREES, false));
    turn.addEntry(padButton("ggbot.afkfleets.walk.turnRight", 0, 0, TURN_DEGREES, false));
    turn.addEntry(padButton("ggbot.afkfleets.walk.jump", 0, 0, 0, true));
    list.addChildInitialized(turn);
  }

  private ButtonWidget padButton(String key, double forward, double strafe, double turn,
      boolean jump) {
    ButtonWidget button = ButtonWidget.i18n(key, () -> {
      de.ggbot.core.utils.GuiSounds.click();
      BotRequests.walkAfkBotsAsync(addon, AfkOverview.tokens(targets(), AfkOverview::isOnline),
          forward, strafe, turn, jump);
    });
    button.addId("afkfleets-pad-button");
    onlineButtons.add(button);
    return button;
  }

  private ButtonWidget actionButton(String key, String id, boolean enabled, Runnable action) {
    ButtonWidget button = ButtonWidget.i18n(key);
    button.addId(id);
    button.setEnabled(enabled);
    button.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      button.setEnabled(false);
      action.run();
    });
    return button;
  }

  private void onStateRequested() {
    AfkCbxManager.get().onBotsStarted();
    fetchDelayed();
  }

  private void showInspect(ComponentWidget details, AfkInspect inspect) {
    if (inspect == null) {
      details.setComponent(Component.translatable("ggbot.afkfleets.noAnswer"));
      return;
    }
    List<BigDecimal> pos = inspect.getPos();
    String location = pos != null && pos.size() >= 3
        ? pos.get(0).intValue() + " " + pos.get(1).intValue() + " " + pos.get(2).intValue() : "?";
    details.setComponent(Component.translatable("ggbot.afkfleets.details",
        Component.text(String.valueOf(inspect.getHealth())),
        Component.text(String.valueOf(inspect.getFood())),
        Component.text(location)));
  }

  private static String botName(AfkBot bot) {
    String linkName = bot.getLinkName();
    boolean hasName = linkName != null && !linkName.isEmpty() && !"unknown".equals(linkName);
    return hasName ? linkName : shortToken(bot);
  }

  private static String shortToken(AfkBot bot) {
    String token = bot.getToken();
    return token == null ? "?" : token.substring(0, Math.min(3, token.length()));
  }

  private static String statusKey(AfkBot bot) {
    if (bot.getStatus() == null) return "ggbot.afkfleets.status.offline";
    return switch (bot.getStatus()) {
      case LOGGED_IN -> "ggbot.afkfleets.status.online";
      case STARTING, CONNECTING -> "ggbot.afkfleets.status.connecting";
      case RECONNECTING -> "ggbot.afkfleets.status.reconnecting";
      default -> "ggbot.afkfleets.status.offline";
    };
  }
}
