package de.ggbot.core.gui.replays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.botselector.BotSelectorWidget;
import de.ggbot.core.replay.ReplayEvents;
import de.ggbot.core.replay.ReplayExport;
import de.ggbot.core.replay.ReplayModSupport;
import de.ggbot.core.replay.ReplayStorage;
import de.ggbot.core.utils.GuiSounds;
import de.ggbot.sdk.model.Bot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;

import static de.ggbot.core.replay.ReplayJson.number;
import static de.ggbot.core.replay.ReplayJson.string;

/**
 * A bot's replay: its details, players and events. Events are read solely from the
 * events sections of its segments; the recording itself is not loaded.
 */
@AutoActivity
@Link("replays.lss")
public class ReplayDetailsActivity extends SimpleActivity {

  private static final int MAX_ROWS = 300;
  private static final int MS_PER_TICK = 50;

  private record Entry(long timeMs, ReplayEvents.Event event, String dim) {}

  /** What was loaded: the replay, its events and players. */
  private record Loaded(JsonObject replay, List<Entry> entries, Set<String> players) {}

  private final GGBot addon;
  private final ReplaysActivity replays;
  private final Bot bot;
  private final String replayId;
  private JsonObject replay;
  private final List<Entry> entries = new ArrayList<>();
  private final Set<String> players = new LinkedHashSet<>();
  private boolean loaded;
  private boolean failed;
  private String filter = "";
  private int kind;
  private VerticalListWidget<Widget> list;

  public ReplayDetailsActivity(GGBot addon, ReplaysActivity replays, Bot bot, String replayId) {
    this.addon = addon;
    this.replays = replays;
    this.bot = bot;
    this.replayId = replayId;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    DivWidget panel = new DivWidget();
    panel.addId("replays-panel");

    ComponentWidget header = ComponentWidget.text(title());
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
    if (!loaded && !failed) load();
  }

  private HorizontalListWidget controls() {
    HorizontalListWidget controls = new HorizontalListWidget();
    controls.addId("replays-controls");

    TextFieldWidget search = new TextFieldWidget();
    search.addId("replays-search");
    search.placeholder(Component.translatable("ggbot.replays.details.filter"));
    search.setText(filter);
    search.updateListener(text -> {
      filter = text == null ? "" : text.trim();
      rebuildRows();
    });
    controls.addEntry(search);

    Component kindName = kind == 0
        ? Component.translatable("ggbot.replays.allEvents")
        : ReplayText.kind(kind);

    ButtonWidget kindButton = ButtonWidget.component(kindName);
    kindButton.addId("replays-filter");
    kindButton.setPressable(() -> {
      GuiSounds.click();
      kind = kind >= ReplayText.kinds() ? 0 : kind + 1;
      reload();
    });
    controls.addEntry(kindButton);
    return controls;
  }

  private HorizontalListWidget buttons() {
    HorizontalListWidget buttons = new HorizontalListWidget();
    buttons.addId("replays-buttons");

    String version = replay == null ? null : string(replay, "mc_version");
    if (replays.canPlay(version)) {
      long date = number(replay, "started_at");
      buttons.addEntry(button("ggbot.replays.open.button", "replays-refresh",
          () -> replays.openRemote(bot, replayId, date)));
    }

    if (ReplayModSupport.has(addon, ReplayModSupport.F_WEB)) {
      buttons.addEntry(button("ggbot.replays.web", "replays-refresh",
          () -> ReplayText.openInWeb(bot, replayId)));
    }

    buttons.addEntry(button("ggbot.replays.back", "replays-close", replays::reopen));
    return buttons;
  }

  private static ButtonWidget button(String key, String id, Runnable run) {
    ButtonWidget button = ButtonWidget.i18n(key, () -> {
      GuiSounds.click();
      run.run();
    });
    button.addId(id);
    return button;
  }

  private String title() {
    if (replay == null) return "#" + replayId;

    String server = string(replay, "custom_server_name");
    if (server == null || server.isEmpty()) server = string(replay, "server_name");
    if (server == null || server.isEmpty()) server = "#" + replayId;

    return server + "  " + ReplayText.date(number(replay, "started_at"));
  }

  private void load() {
    Thread thread = new Thread(() -> {
      Loaded result;

      try {
        result = loadEvents();
      } catch (Exception e) {
        addon.logger().warn("Replay details failed: {}", String.valueOf(e.getMessage()));
        result = null;
      }

      Loaded value = result;
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> show(value));
    }, "ggbot-replay-details");
    thread.setDaemon(true);
    thread.start();
  }

  /** Reads the events of every snippet and puts them on one clock, like the export does. */
  private Loaded loadEvents() throws Exception {
    JsonObject detail = BotRequests.getReplay(addon, bot, replayId);
    JsonObject loadedReplay = detail.getAsJsonObject("replay");
    ReplayStorage storage = ReplayExport.storage(detail);

    List<Entry> found = new ArrayList<>();
    Set<String> names = new LinkedHashSet<>();
    long first = -1;
    long clockEnd = 0;

    for (JsonElement element : loadedReplay.getAsJsonArray("snippets")) {
      JsonObject row = element.getAsJsonObject();
      JsonObject manifest = storage.manifest(ReplayStorage.HTTP,
          ReplayExport.manifestKey(loadedReplay, row));
      if (manifest == null || manifest.getAsJsonArray("segments").size() == 0) continue;

      long startedAt = number(row, "started_at");
      if (first < 0) first = startedAt;

      long offset = Math.max(startedAt - first, clockEnd);
      long ticks = Math.max(number(row, "duration_ticks"), number(manifest, "durationTicks"));
      clockEnd = offset + ticks * MS_PER_TICK;

      for (JsonElement player : manifest.getAsJsonArray("players")) {
        JsonObject entry = player.getAsJsonObject();
        String name = string(entry, "name");
        names.add(name != null ? name : string(entry, "uuid"));
      }

      found.addAll(entries(storage, manifest, offset));
    }

    return new Loaded(loadedReplay, found, names);
  }

  private static List<Entry> entries(ReplayStorage storage, JsonObject manifest, long offset)
      throws IOException {
    List<Entry> entries = new ArrayList<>();
    String dim = "overworld";

    for (ReplayEvents.Event event : storage.events(ReplayStorage.HTTP, manifest)) {
      if (event.kind() == ReplayEvents.RESPAWN && event.dim() != null) dim = event.dim();

      entries.add(new Entry(offset + (long) event.tick() * MS_PER_TICK, event, dim));
    }

    return entries;
  }

  private void show(Loaded result) {
    replay = result == null ? null : result.replay();
    failed = result == null;
    loaded = result != null;
    entries.clear();
    players.clear();

    if (result != null) {
      entries.addAll(result.entries());
      players.addAll(result.players());
    }

    reload();
  }

  private void rebuildRows() {
    if (list == null) return;

    list.removeChildIf(w -> true);

    if (failed) {
      line(Component.translatable("ggbot.replays.failedReplay"));
      return;
    }

    if (!loaded) {
      line(Component.translatable("ggbot.replays.details.loading"));
      return;
    }

    line(info());

    if (!players.isEmpty()) {
      Component names = Component.text(String.join(", ", players));
      line(Component.translatable("ggbot.replays.details.players", names));
    }

    showEvents();
  }

  private Component info() {
    long durationMs = number(replay, "total_duration_ticks") * MS_PER_TICK;
    double megabytes = number(replay, "stored_size") / 1048576.0;

    return Component.translatable("ggbot.replays.details.info",
        Component.text(BotSelectorWidget.formatBotDisplay(bot)),
        Component.text(String.valueOf(string(replay, "mc_version"))),
        Component.text(ReplayText.clock(durationMs)),
        Component.text(String.valueOf(number(replay, "snippet_count"))),
        Component.text(String.format(Locale.ROOT, "%.1f MB", megabytes)));
  }

  /** The events matching the filter: its words, or a player named like it. */
  private void showEvents() {
    Set<Integer> kinds = kind == 0 ? null : Set.of(kind);
    List<String> byName = filter.isEmpty() ? null : List.of(filter);
    String byWords = filter.isEmpty() ? null : filter;
    ReplayEvents.Query byPlayer = new ReplayEvents.Query(kinds, byName, null, null);
    ReplayEvents.Query byText = new ReplayEvents.Query(kinds, null, byWords, null);

    int shown = 0;
    int matching = 0;

    for (Entry entry : entries) {
      boolean matches = ReplayEvents.matches(entry.event(), byPlayer, entry.dim())
          || ReplayEvents.matches(entry.event(), byText, entry.dim());
      if (!matches) continue;

      matching++;
      if (shown >= MAX_ROWS) continue;

      Component time = Component.text("[" + ReplayText.clock(entry.timeMs()) + "] ");
      Component line = time.append(ReplayText.describe(entry.event()));

      ComponentWidget row = ComponentWidget.component(line);
      row.addId("replays-event");
      list.addChildInitialized(row);
      shown++;
    }

    if (matching == 0) {
      String key = entries.isEmpty() ? "ggbot.replays.details.noEvents" : "ggbot.replays.noMatch";
      line(Component.translatable(key));
    } else if (matching > shown) {
      Component more = Component.text(String.valueOf(matching - shown));
      line(Component.translatable("ggbot.replays.details.more", more));
    }
  }

  private void line(Component text) {
    ComponentWidget widget = ComponentWidget.component(text);
    widget.addId("replays-status");
    list.addChildInitialized(widget);
  }
}
