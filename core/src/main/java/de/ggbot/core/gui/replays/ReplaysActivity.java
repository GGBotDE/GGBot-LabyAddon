package de.ggbot.core.gui.replays;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.ConfirmActivity;
import de.ggbot.core.gui.botselector.BotSelectorWidget;
import de.ggbot.core.replay.ReplayExport;
import de.ggbot.core.replay.ReplayFiles;
import de.ggbot.core.replay.ReplayInfo;
import de.ggbot.core.replay.ReplayModSupport;
import de.ggbot.core.utils.GuiSounds;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.ReplaySummary;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.ScreenInstance;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget.State;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.notification.Notification;
import net.labymod.api.util.I18n;

/**
 * Shows the replays of the account's bots, or of a single bot, and optionally those in
 * ReplayMod's folder, so they can be watched with ReplayMod, viewed in the panel, have
 * their events inspected, be downloaded or deleted. Loaded only when the screen opens
 * and on refresh.
 */
@AutoActivity
@Link("replays.lss")
public class ReplaysActivity extends SimpleActivity {

  private static final int MS_PER_TICK = 50;

  private static boolean includeLocal = false;

  /** A replay of one of the bots. */
  record Remote(Bot bot, ReplaySummary replay) {}

  private final GGBot addon;
  private Bot filter;
  private List<Remote> remote;
  private List<ReplayInfo> local = new ArrayList<>();
  private boolean loaded;
  private String busyKey;
  private VerticalListWidget<Widget> list;

  /**
   * The replays of every bot, with one bot's replays listed first.
   *
   * @param addon the addon
   * @param bot   the bot listed first, every bot when it is {@code null}
   */
  public ReplaysActivity(GGBot addon, Bot bot) {
    this.addon = addon;
    this.filter = bot;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    DivWidget panel = new DivWidget();
    panel.addId("replays-panel");

    ComponentWidget header = ComponentWidget.i18n("ggbot.replays.title");
    header.addId("replays-header");
    panel.addChild(header);

    panel.addChild(controls());

    list = new VerticalListWidget<>();
    list.addId("replays-list");

    ScrollWidget scroll = new ScrollWidget(list);
    scroll.addId("replays-scroll");
    panel.addChild(scroll);

    panel.addChild(buttons());

    this.document().addChild(panel);
    rebuildRows();
    if (!loaded) fetch();
  }

  private HorizontalListWidget controls() {
    HorizontalListWidget controls = new HorizontalListWidget();
    controls.addId("replays-controls");

    if (bots().size() > 1) {
      controls.addEntry(botFilter());
    }

    if (ReplayModSupport.hasFiles(addon)) {
      CheckBoxWidget localBox = new CheckBoxWidget();
      localBox.addId("replays-local-box");
      localBox.setState(includeLocal ? State.CHECKED : State.UNCHECKED);
      localBox.setPressable(() -> {
        includeLocal = localBox.state() == State.CHECKED;
        fetch();
      });
      controls.addEntry(localBox);

      ComponentWidget localLabel = ComponentWidget.i18n("ggbot.replays.includeLocal");
      localLabel.addId("replays-local-label");
      controls.addEntry(localLabel);
    }

    if (busyKey != null) {
      ComponentWidget busy = ComponentWidget.i18n(busyKey);
      busy.addId("replays-busy");
      controls.addEntry(busy);
    }

    return controls;
  }

  private DropdownWidget<String> botFilter() {
    String all = I18n.getTranslation("ggbot.replays.allBots");

    DropdownWidget<String> botFilter = new DropdownWidget<>();
    botFilter.addId("replays-bot-select");
    botFilter.add(all);
    for (Bot bot : bots()) botFilter.add(BotSelectorWidget.formatBotDisplay(bot));

    botFilter.setSelected(filter == null ? all : BotSelectorWidget.formatBotDisplay(filter));
    botFilter.setChangeListener(value -> {
      filter = null;
      for (Bot bot : bots()) {
        if (BotSelectorWidget.formatBotDisplay(bot).equals(value)) filter = bot;
      }

      rebuildRows();
    });
    return botFilter;
  }

  private HorizontalListWidget buttons() {
    HorizontalListWidget buttons = new HorizontalListWidget();
    buttons.addId("replays-buttons");
    buttons.addEntry(button("ggbot.replays.refresh", "replays-refresh", this::fetch));

    boolean canSearch = GGBot.isAuthenticated() && !bots().isEmpty()
        && ReplayModSupport.has(addon, ReplayModSupport.F_SEARCH);

    if (canSearch) {
      buttons.addEntry(button("ggbot.replays.search.button", "replays-refresh",
          () -> show(new ReplaySearchActivity(addon, this,
              filter != null ? List.of(filter) : bots()))));
    }

    if (ReplayModSupport.hasFiles(addon) && ReplayFiles.folderExists()) {
      buttons.addEntry(button("ggbot.replays.folder", "replays-refresh", ReplayFiles::show));
    }

    ButtonWidget close = ButtonWidget.i18n("ggbot.replays.close", this::closeScreen);
    close.addId("replays-close");
    buttons.addEntry(close);
    return buttons;
  }

  private List<Bot> bots() {
    List<Bot> bots = BotRequests.getCachedBots();
    return bots.isEmpty() && filter != null ? List.of(filter) : bots;
  }

  private void fetch() {
    loaded = false;
    rebuildRows();

    boolean withLocal = includeLocal && ReplayModSupport.hasFiles(addon);

    Thread thread = new Thread(() -> {
      List<ReplayInfo> files = withLocal ? ReplayFiles.list() : new ArrayList<>();
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        local = files;
        fetchRemote();
      });
    }, "ggbot-replays");
    thread.setDaemon(true);
    thread.start();
  }

  private void fetchRemote() {
    List<Bot> bots = bots();
    boolean online = GGBot.isAuthenticated()
        && ReplayModSupport.has(addon, ReplayModSupport.F_ONLINE);

    if (bots.isEmpty() || !online) {
      remote = new ArrayList<>();
      loaded = true;
      rebuildRows();
      return;
    }

    List<Remote> found = new ArrayList<>();
    int[] pending = {bots.size()};
    boolean[] anyLoaded = {false};

    for (Bot bot : bots) {
      BotRequests.getReplaysAsync(addon, bot, replays -> {
        if (replays != null) {
          anyLoaded[0] = true;
          for (ReplaySummary replay : replays) found.add(new Remote(bot, replay));
        }

        if (--pending[0] > 0) return;

        found.sort((a, b) -> Long.compare(startedAt(b), startedAt(a)));
        remote = anyLoaded[0] ? found : null;
        loaded = true;
        rebuildRows();
      });
    }
  }

  private void rebuildRows() {
    if (list == null) return;

    list.removeChildIf(w -> true);

    if (!loaded) {
      addStatus("ggbot.replays.loading");
      return;
    }

    if (!GGBot.isAuthenticated()) {
      addStatus("ggbot.replays.login");
    } else if (bots().isEmpty()) {
      addStatus("ggbot.replays.noBot");
    } else if (remote == null) {
      addStatus("ggbot.replays.failed");
    }

    int shown = 0;
    Set<String> listed = new HashSet<>();

    if (remote != null) {
      for (Remote entry : remote) {
        if (filter != null && !Objects.equals(filter.getId(), entry.bot().getId())) continue;

        list.addChildInitialized(buildRemoteRow(entry));
        listed.add(entry.replay().getId());
        shown++;
      }
    }

    for (ReplayInfo replay : local) {
      if (replay.isGgbot() && listed.contains(replay.getRemoteId())) continue;

      list.addChildInitialized(buildLocalRow(replay));
      shown++;
    }

    if (shown == 0 && remote != null && !bots().isEmpty()) addStatus("ggbot.replays.empty");
  }

  private void addStatus(String key) {
    ComponentWidget status = ComponentWidget.i18n(key);
    status.addId("replays-status");
    list.addChildInitialized(status);
  }

  private HorizontalListWidget buildRemoteRow(Remote entry) {
    Bot bot = entry.bot();
    ReplaySummary replay = entry.replay();
    String id = replay.getId();
    String version = replay.getMcVersion();
    String title = title(replay);
    long date = orZero(replay.getStartedAt());
    ReplayInfo downloaded = downloaded(id);

    Icon icon = downloaded != null ? thumbnail(downloaded) : ReplayThumbnails.placeholder();
    long durationMs = orZero(replay.getDurationTicks()) * MS_PER_TICK;
    Component info = details(date, durationMs, version, orZero(replay.getStoredSize()),
        downloaded != null ? "ggbot.replays.downloaded" : null);

    if (bots().size() > 1) {
      info = info.append(Component.text("  " + BotSelectorWidget.formatBotDisplay(bot)));
    }

    HorizontalListWidget actions = actions();
    HorizontalListWidget row = row(actions, icon, title, info);

    if (canPlay(version) && (downloaded != null || canStream() || canDownload())) {
      actions.addEntry(action("ggbot.replays.open.button", "replays-open",
          () -> openRemote(bot, id, date)));
    }

    if (ReplayModSupport.has(addon, ReplayModSupport.F_DETAILS)) {
      actions.addEntry(action("ggbot.replays.details.button", "replays-open",
          () -> show(new ReplayDetailsActivity(addon, this, bot, id))));
    }

    if (ReplayModSupport.has(addon, ReplayModSupport.F_WEB)) {
      actions.addEntry(action("ggbot.replays.web", "replays-open",
          () -> ReplayText.openInWeb(bot, id)));
    }

    if (canDownload() && downloaded == null) {
      actions.addEntry(action("ggbot.replays.download", "replays-open",
          () -> download(bot, id, date)));
    }

    if (ReplayModSupport.has(addon, ReplayModSupport.F_DELETE)) {
      actions.addEntry(action("ggbot.replays.delete.button", "replays-delete",
          () -> confirmDeleteRemote(bot, id, title, downloaded)));
    }

    return row;
  }

  private HorizontalListWidget buildLocalRow(ReplayInfo replay) {
    String tag = replay.isGgbot() ? "ggbot.replays.ggbot" : "ggbot.replays.local";
    Component info = details(replay.getDate(), replay.getDurationMs(), replay.getMcVersion(),
        replay.getSize(), tag);

    HorizontalListWidget actions = actions();
    HorizontalListWidget row = row(actions, thumbnail(replay), replay.getTitle(), info);

    if (canPlay(replay.getMcVersion())) {
      actions.addEntry(action("ggbot.replays.open.button", "replays-open",
          () -> confirmOpen(replay.getFile())));
    }

    actions.addEntry(action("ggbot.replays.show", "replays-open", ReplayFiles::show));

    if (ReplayModSupport.has(addon, ReplayModSupport.F_DELETE)) {
      actions.addEntry(action("ggbot.replays.delete.button", "replays-delete",
          () -> confirmDeleteLocal(replay)));
    }

    return row;
  }

  private HorizontalListWidget row(HorizontalListWidget actions, Icon icon, String title,
      Component details) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("replays-row");

    IconWidget thumbnail = new IconWidget(icon);
    thumbnail.addId("replays-thumbnail");
    row.addEntry(thumbnail);

    VerticalListWidget<Widget> text = new VerticalListWidget<>();
    text.addId("replays-text");

    ComponentWidget name = ComponentWidget.text(title);
    name.addId("replays-name");
    text.addChild(name);

    ComponentWidget info = ComponentWidget.component(details);
    info.addId("replays-details");
    text.addChild(info);

    text.addChild(actions);
    row.addEntry(text);
    return row;
  }

  private HorizontalListWidget actions() {
    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("replays-actions");
    return actions;
  }

  private ButtonWidget action(String key, String id, Runnable run) {
    ButtonWidget button = button(key, id, run);
    button.setEnabled(busyKey == null);
    return button;
  }

  private static ButtonWidget button(String key, String id, Runnable run) {
    ButtonWidget button = ButtonWidget.i18n(key, () -> {
      GuiSounds.click();
      run.run();
    });
    button.addId(id);
    return button;
  }

  boolean canPlay(String mcVersion) {
    return ReplayModSupport.has(addon, ReplayModSupport.F_PLAY)
        && ReplayModSupport.canPlay(mcVersion);
  }

  private boolean canStream() {
    return ReplayModSupport.has(addon, ReplayModSupport.F_STREAM);
  }

  private boolean canDownload() {
    return ReplayModSupport.hasFiles(addon)
        && ReplayModSupport.has(addon, ReplayModSupport.F_DOWNLOAD);
  }

  private void download(Bot bot, String id, long date) {
    runBusy("ggbot.replays.busy.downloading",
        () -> ReplayModSupport.download(addon, bot, id, date),
        file -> notify("ggbot.replays.downloadDone"));
  }

  /** Asks before a downloaded replay opens in ReplayMod. */
  void confirmOpen(Path file) {
    confirm(Component.translatable("ggbot.replays.open.title"),
        List.of(Component.translatable("ggbot.replays.open.leave")),
        List.of(new ConfirmActivity.Choice("ggbot.replays.open.open",
            () -> ReplayModSupport.play(file, this::playFailed))));
  }

  /** Opens a replay of a bot in ReplayMod, downloaded or from memory, after asking. */
  void openRemote(Bot bot, String id, long date) {
    ReplayInfo downloaded = downloaded(id);
    if (downloaded != null) {
      confirmOpen(downloaded.getFile());
      return;
    }

    List<ConfirmActivity.Choice> choices = new ArrayList<>();

    if (canDownload()) {
      choices.add(new ConfirmActivity.Choice("ggbot.replays.open.downloadAndOpen", () -> {
        reopen();
        runBusy("ggbot.replays.busy.downloading",
            () -> ReplayModSupport.download(addon, bot, id, date),
            file -> ReplayModSupport.play(file, this::playFailed));
      }));
    }

    if (canStream()) {
      choices.add(new ConfirmActivity.Choice("ggbot.replays.open.open", () -> {
        reopen();
        runBusy("ggbot.replays.busy.loading",
            () -> ReplayModSupport.load(addon, bot, id),
            entries -> ReplayModSupport.play(entries, this::playFailed));
      }));
    }

    confirm(Component.translatable("ggbot.replays.open.title"),
        List.of(Component.translatable("ggbot.replays.open.notSaved"),
            Component.translatable("ggbot.replays.open.leave")),
        choices);
  }

  /** Asks which copies of a replay of the bot to delete: the bot's, the downloaded one or both. */
  private void confirmDeleteRemote(Bot bot, String id, String title, ReplayInfo downloaded) {
    Runnable deleteRemote = () -> BotRequests.deleteReplayAsync(addon, bot, id, deleted -> {
      if (!deleted) notify("ggbot.replays.deleteFailed");

      reopen();
      fetch();
    });

    if (downloaded == null) {
      confirm(Component.translatable("ggbot.replays.delete.title"),
          List.of(Component.translatable("ggbot.replays.delete.remote", Component.text(title))),
          List.of(new ConfirmActivity.Choice("ggbot.replays.delete.confirm", deleteRemote)));
      return;
    }

    confirm(Component.translatable("ggbot.replays.delete.title"),
        List.of(Component.translatable("ggbot.replays.delete.which", Component.text(title))),
        List.of(
            new ConfirmActivity.Choice("ggbot.replays.delete.bot", deleteRemote),
            new ConfirmActivity.Choice("ggbot.replays.delete.localCopy", () -> {
              deleteFile(downloaded.getFile());
              reopen();
              fetch();
            }),
            new ConfirmActivity.Choice("ggbot.replays.delete.both", () -> {
              deleteFile(downloaded.getFile());
              deleteRemote.run();
            })));
  }

  private void confirmDeleteLocal(ReplayInfo replay) {
    String name = replay.getFile().getFileName().toString();
    confirm(Component.translatable("ggbot.replays.delete.title"),
        List.of(Component.translatable("ggbot.replays.delete.local", Component.text(name))),
        List.of(new ConfirmActivity.Choice("ggbot.replays.delete.confirm", () -> {
          deleteFile(replay.getFile());
          reopen();
          fetch();
        })));
  }

  private void deleteFile(Path file) {
    try {
      ReplayFiles.delete(file);
    } catch (IOException e) {
      notify("ggbot.replays.deleteFailed");
    }
  }

  private void confirm(Component title, List<Component> lines,
      List<ConfirmActivity.Choice> choices) {
    show(new ConfirmActivity(title, lines, choices, this::reopen));
  }

  void reopen() {
    show(this);
  }

  private static void show(ScreenInstance screen) {
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen(screen);
  }

  private interface Work<T> {
    T run() throws Exception;
  }

  /** Loads in the background, the screen shows what it is doing meanwhile. */
  private <T> void runBusy(String key, Work<T> work, Consumer<T> done) {
    if (busyKey != null) return;

    busyKey = key;
    reload();

    Thread thread = new Thread(() -> {
      T result = null;
      Exception failure = null;

      try {
        result = work.run();
      } catch (Exception e) {
        failure = e;
      }

      T value = result;
      Exception error = failure;

      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        busyKey = null;
        reload();

        if (error != null) {
          addon.logger().warn("Replay failed: {}", String.valueOf(error.getMessage()));
          notify(error instanceof ReplayExport.TooLargeException
              ? "ggbot.replays.tooLarge" : "ggbot.replays.failedReplay");
          return;
        }

        done.accept(value);
        if (includeLocal) fetch();
      });
    }, "ggbot-replay-load");
    thread.setDaemon(true);
    thread.start();
  }

  private void playFailed(Exception e) {
    addon.logger().warn("ReplayMod could not open the replay: {}", String.valueOf(e.getMessage()));
    notify("ggbot.replays.playFailed");
  }

  private void notify(String key) {
    addon.labyAPI().notificationController().push(Notification.builder()
        .title(Component.translatable("ggbot.replays.title"))
        .text(Component.translatable(key))
        .type(Notification.Type.SYSTEM)
        .build());
  }

  private ReplayInfo downloaded(String id) {
    for (ReplayInfo replay : local) {
      if (replay.isGgbot() && id != null && id.equals(replay.getRemoteId())) return replay;
    }

    return null;
  }

  private static String title(ReplaySummary replay) {
    String server = replay.getCustomServerName();
    if (server == null || server.isEmpty()) server = replay.getServerName();

    return server == null || server.isEmpty() ? "#" + replay.getId() : server;
  }

  private static Icon thumbnail(ReplayInfo replay) {
    if (replay.getThumbnail() == null) return ReplayThumbnails.placeholder();

    String key = replay.getFile() + ":" + replay.getSize();
    return ReplayThumbnails.icon(key, replay.getThumbnail());
  }

  private static Component details(long date, long durationMs, String version, long size,
      String tagKey) {
    StringBuilder text = new StringBuilder();
    if (date > 0) text.append(ReplayText.date(date)).append("  ");

    text.append(ReplayText.clock(durationMs)).append("  ").append(version == null ? "?" : version);
    if (size > 0) text.append(String.format(Locale.ROOT, "  %.1f MB", size / 1048576.0));

    Component details = Component.text(text.toString());
    if (tagKey == null) return details;

    return details.append(Component.text("  ")).append(Component.translatable(tagKey));
  }

  private static long startedAt(Remote entry) {
    return orZero(entry.replay().getStartedAt());
  }

  private static long orZero(Number value) {
    return value == null ? 0 : value.longValue();
  }
}
