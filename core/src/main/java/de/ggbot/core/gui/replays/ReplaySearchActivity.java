package de.ggbot.core.gui.replays;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.botselector.BotSelectorWidget;
import de.ggbot.core.replay.ReplayEvents;
import de.ggbot.core.replay.ReplayModSupport;
import de.ggbot.core.replay.ReplayStorage;
import de.ggbot.core.utils.GuiSounds;
import de.ggbot.sdk.model.Bot;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import static de.ggbot.core.replay.ReplayJson.string;

/**
 * Searches the events of the bots' replays the same way the panel does: the backend
 * returns the snippets that may match, their events are read from the replay storage
 * and matched here. Runs only when the user searches.
 */
@AutoActivity
@Link("replays.lss")
public class ReplaySearchActivity extends SimpleActivity {

  private static final int MAX_SNIPPETS = 40;
  private static final int MAX_ROWS = 200;
  private static final int MS_PER_TICK = 50;

  private record Result(Bot bot, String replayId, String server, long timeMs,
      ReplayEvents.Event event) {}

  private final GGBot addon;
  private final ReplaysActivity replays;
  private final List<Bot> bots;
  private final List<Result> results = new ArrayList<>();
  private final Map<Long, Long> next = new HashMap<>();
  private String text = "";
  private String player = "";
  private int kind;
  private boolean searching;
  private boolean searched;
  private boolean failed;
  private VerticalListWidget<Widget> list;

  public ReplaySearchActivity(GGBot addon, ReplaysActivity replays, List<Bot> bots) {
    this.addon = addon;
    this.replays = replays;
    this.bots = bots;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    DivWidget panel = new DivWidget();
    panel.addId("replays-panel");

    ComponentWidget header = ComponentWidget.i18n("ggbot.replays.search.title");
    header.addId("replays-header");
    panel.addChild(header);

    panel.addChild(controls());

    list = new VerticalListWidget<>();
    list.addId("replays-list");

    ScrollWidget scroll = new ScrollWidget(list);
    scroll.addId("replays-scroll");
    panel.addChild(scroll);

    HorizontalListWidget buttons = new HorizontalListWidget();
    buttons.addId("replays-buttons");

    if (!next.isEmpty() && !searching) {
      ButtonWidget more = ButtonWidget.i18n("ggbot.replays.search.more", () -> search(true));
      more.addId("replays-refresh");
      buttons.addEntry(more);
    }

    ButtonWidget back = ButtonWidget.i18n("ggbot.replays.back", () -> {
      GuiSounds.click();
      replays.reopen();
    });
    back.addId("replays-close");
    buttons.addEntry(back);
    panel.addChild(buttons);

    this.document().addChild(panel);
    rebuildRows();
  }

  private HorizontalListWidget controls() {
    HorizontalListWidget controls = new HorizontalListWidget();
    controls.addId("replays-controls");

    TextFieldWidget words = field("ggbot.replays.search.words", text);
    words.updateListener(value -> text = value == null ? "" : value.trim());
    controls.addEntry(words);

    TextFieldWidget name = field("ggbot.replays.search.player", player);
    name.updateListener(value -> player = value == null ? "" : value.trim());
    controls.addEntry(name);

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

    ButtonWidget go = ButtonWidget.i18n("ggbot.replays.search.button", () -> search(false));
    go.addId("replays-open");
    go.setEnabled(!searching);
    controls.addEntry(go);
    return controls;
  }

  private TextFieldWidget field(String placeholder, String value) {
    TextFieldWidget field = new TextFieldWidget();
    field.addId("replays-search-field");
    field.maximalLength(100);
    field.placeholder(Component.translatable(placeholder));
    field.setText(value);
    field.submitHandler(submitted -> search(false));
    return field;
  }

  private void search(boolean more) {
    if (searching || (text.isEmpty() && player.isEmpty() && kind == 0)) return;

    GuiSounds.click();

    if (!more) {
      results.clear();
      next.clear();
    }

    Map<Long, Long> before = new HashMap<>(next);
    next.clear();
    searching = true;
    failed = false;
    reload();

    ReplayEvents.Query query = new ReplayEvents.Query(kind == 0 ? null : Set.of(kind),
        player.isEmpty() ? null : List.of(player), text.isEmpty() ? null : text, null);

    Thread thread = new Thread(() -> {
      List<Result> found = new ArrayList<>();
      Map<Long, Long> continued = new HashMap<>();
      boolean error = false;
      int snippets = 0;

      for (Bot bot : bots) {
        if (more && !before.containsKey(bot.getId())) continue;

        try {
          snippets += searchBot(bot, before.get(bot.getId()), query, MAX_SNIPPETS - snippets,
              found, continued);
        } catch (Exception e) {
          addon.logger().warn("Replay search failed: {}", String.valueOf(e.getMessage()));
          error = true;
        }
      }

      found.sort((a, b) -> Long.compare(b.timeMs(), a.timeMs()));
      boolean anyError = error;

      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        results.addAll(found);
        next.putAll(continued);
        searching = false;
        searched = true;
        failed = anyError && results.isEmpty();
        reload();
      });
    }, "ggbot-replay-search");
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Searches the replays of one bot: asks the backend which snippets may match, then
   * matches the events of at most {@code maxSnippets} of them.
   *
   * @return how many snippets were read
   */
  private int searchBot(Bot bot, Long before, ReplayEvents.Query query, int maxSnippets,
      List<Result> found, Map<Long, Long> continued) throws Exception {
    JsonObject answer = BotRequests.searchReplays(addon, bot, queryString(before));

    if (answer.get("next") != null && !answer.get("next").isJsonNull()) {
      continued.put(bot.getId(), answer.get("next").getAsLong());
    }

    JsonObject downloads = answer.get("downloads") instanceof JsonObject all
        ? all : new JsonObject();
    String baseUrl = string(answer, "baseUrl");
    int snippets = 0;

    for (JsonElement element : answer.getAsJsonArray("hits")) {
      if (snippets >= maxSnippets) break;

      snippets++;

      JsonObject hit = element.getAsJsonObject();
      String replayId = string(hit, "replayId");
      JsonObject access = downloads.get(replayId) instanceof JsonObject replay ? replay : null;
      ReplayStorage storage = new ReplayStorage(access, baseUrl);

      JsonObject manifest = storage.manifest(ReplayStorage.HTTP, string(hit, "manifestKey"));
      if (manifest == null) continue;

      long startedAt = hit.get("startedAt").getAsLong();
      String server = string(hit, "server");
      List<ReplayEvents.Event> events = storage.events(ReplayStorage.HTTP, manifest);

      for (ReplayEvents.Event event : ReplayEvents.search(events, query)) {
        long timeMs = startedAt + (long) event.tick() * MS_PER_TICK;
        found.add(new Result(bot, replayId, server, timeMs, event));
      }
    }

    return snippets;
  }

  private String queryString(Long before) {
    StringBuilder query = new StringBuilder();
    if (!text.isEmpty()) append(query, "text", text);
    if (!player.isEmpty()) append(query, "players", player);
    if (kind != 0) append(query, "kinds", String.valueOf(kind));
    if (before != null) append(query, "before", String.valueOf(before));
    return query.toString();
  }

  private static void append(StringBuilder query, String key, String value) {
    if (query.length() > 0) query.append('&');

    query.append(key).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
  }

  private void rebuildRows() {
    if (list == null) return;

    list.removeChildIf(w -> true);

    if (searching) {
      status("ggbot.replays.search.searching");
    } else if (failed) {
      status("ggbot.replays.search.failed");
    } else if (!searched) {
      status("ggbot.replays.search.hint");
    } else if (results.isEmpty()) {
      status("ggbot.replays.noMatch");
    } else {
      for (int i = 0; i < Math.min(results.size(), MAX_ROWS); i++) {
        list.addChildInitialized(row(results.get(i)));
      }
    }
  }

  private HorizontalListWidget row(Result result) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("replays-result");

    VerticalListWidget<Widget> text = new VerticalListWidget<>();
    text.addId("replays-text");

    ComponentWidget what = ComponentWidget.component(ReplayText.describe(result.event()));
    what.addId("replays-event");
    text.addChild(what);

    String where = ReplayText.date(result.timeMs())
        + (result.server() != null ? "  " + result.server() : "")
        + (bots.size() > 1 ? "  " + BotSelectorWidget.formatBotDisplay(result.bot()) : "");

    ComponentWidget whereWidget = ComponentWidget.text(where);
    whereWidget.addId("replays-details");
    text.addChild(whereWidget);

    HorizontalListWidget actions = new HorizontalListWidget();
    actions.addId("replays-actions");

    if (ReplayModSupport.has(addon, ReplayModSupport.F_DETAILS)) {
      ButtonWidget details = ButtonWidget.i18n("ggbot.replays.details.button",
          () -> Laby.labyAPI().minecraft().minecraftWindow().displayScreen(
              new ReplayDetailsActivity(addon, replays, result.bot(), result.replayId())));
      details.addId("replays-open");
      actions.addEntry(details);
    }

    if (ReplayModSupport.has(addon, ReplayModSupport.F_WEB)) {
      ButtonWidget web = ButtonWidget.i18n("ggbot.replays.web",
          () -> ReplayText.openInWeb(result.bot(), result.replayId()));
      web.addId("replays-open");
      actions.addEntry(web);
    }

    text.addChild(actions);
    row.addEntry(text);
    return row;
  }

  private void status(String key) {
    ComponentWidget status = ComponentWidget.i18n(key);
    status.addId("replays-status");
    list.addChildInitialized(status);
  }
}
