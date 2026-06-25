package de.ggbot.core.gui.serverbots;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.GetBotsOnServer200Response;
import de.ggbot.sdk.model.GetBotsOnServer200ResponseBotsInner;
import de.ggbot.sdk.model.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.notification.Notification;

/**
 * GUI that lists the GGBots currently on the server (from the public bot list).
 * Each entry shows the bot's head, name and online status, plus a button to copy
 * its name to the clipboard. The list is scrollable, searchable, and can be
 * filtered to online bots only. Data is fetched on a background thread.
 */
@AutoActivity
@Link("serverbots.lss")
public class ServerBotsActivity extends SimpleActivity {

  private final GGBot addon;
  private final List<BotInfo> bots = new ArrayList<>();
  private final VerticalListWidget<Widget> list = new VerticalListWidget<>();

  private String search = "";
  private boolean onlineOnly = true;
  private boolean loaded = false;

  public ServerBotsActivity(GGBot addon) {
    this.addon = addon;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();

    VerticalListWidget<Widget> panel = new VerticalListWidget<>();
    panel.addId("serverbots-panel");

    ComponentWidget header = ComponentWidget.i18n("ggbot.serverbots.title");
    header.addId("serverbots-header");
    panel.addChildInitialized(header);

    HorizontalListWidget controls = new HorizontalListWidget();
    controls.addId("serverbots-controls");

    TextFieldWidget searchField = new TextFieldWidget();
    searchField.addId("serverbots-search");
    searchField.placeholder(Component.translatable("ggbot.serverbots.searchPlaceholder"));
    searchField.updateListener(text -> {
      search = text == null ? "" : text.toLowerCase(Locale.ROOT);
      rebuildRows();
    });
    controls.addEntry(searchField);

    ButtonWidget filter = ButtonWidget.i18n(filterKey());
    filter.addId("serverbots-filter");
    filter.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      onlineOnly = !onlineOnly;
      filter.updateComponent(Component.translatable(filterKey()));
      rebuildRows();
    });
    controls.addEntry(filter);
    panel.addChildInitialized(controls);

    list.addId("serverbots-list");
    ScrollWidget scroll = new ScrollWidget(list);
    scroll.addId("serverbots-scroll");
    panel.addChildInitialized(scroll);

    ButtonWidget close = ButtonWidget.i18n("ggbot.serverbots.close", this::closeScreen);
    close.addId("serverbots-close");
    panel.addChildInitialized(close);

    this.document().addChild(panel);

    rebuildRows();
    if (!loaded) {
      fetch();
    }
  }

  private String filterKey() {
    return onlineOnly ? "ggbot.serverbots.filter.online" : "ggbot.serverbots.filter.all";
  }

  private void fetch() {
    Thread thread = new Thread(() -> {
      List<BotInfo> result = new ArrayList<>();
      try {
        PublicApi api = new PublicApi();
        api.setCustomBaseUrl(
            addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.serverbots"));
        String serverIp = resolveServerDomain(api);
        GetBotsOnServer200Response response = api.getBotsOnServer(serverIp);
        if (response != null && response.getBots() != null) {
          for (GetBotsOnServer200ResponseBotsInner bot : response.getBots()) {
            String name = bot.getLinkName();
            // Skip unnamed/placeholder bots (link name "unknown").
            if (name != null && !name.isEmpty() && !"unknown".equalsIgnoreCase(name)) {
              result.add(new BotInfo(name, Boolean.TRUE.equals(bot.getOnline())));
            }
          }
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to fetch bots on server: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
        bots.clear();
        bots.addAll(result);
        loaded = true;
        rebuildRows();
      });
    }, "ggbot-serverbots");
    thread.setDaemon(true);
    thread.start();
  }

  /** Rebuilds the visible rows applying the current search text and online filter. */
  private void rebuildRows() {
    list.removeChildIf(w -> true);

    if (!loaded) {
      addStatus("ggbot.serverbots.loading");
      return;
    }

    int shown = 0;
    for (BotInfo bot : bots) {
      if (onlineOnly && !bot.online) {
        continue;
      }
      if (!search.isEmpty() && !bot.name.toLowerCase(Locale.ROOT).contains(search)) {
        continue;
      }
      list.addChildInitialized(buildRow(bot));
      shown++;
    }

    if (shown == 0) {
      addStatus(bots.isEmpty() ? "ggbot.serverbots.empty" : "ggbot.serverbots.noMatch");
    }
  }

  private void addStatus(String key) {
    ComponentWidget status = ComponentWidget.i18n(key);
    status.addId("serverbots-status");
    list.addChildInitialized(status);
  }

  private HorizontalListWidget buildRow(BotInfo bot) {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("serverbots-row");

    IconWidget head = new IconWidget(Icon.head(bot.name));
    head.addId("serverbots-head");
    row.addEntry(head);

    ComponentWidget label = ComponentWidget.component(Component.text(bot.name));
    label.addId("serverbots-name");
    row.addEntry(label);

    ComponentWidget statusLabel = ComponentWidget.i18n(
        bot.online ? "ggbot.gui.botselector.online" : "ggbot.gui.botselector.offline");
    statusLabel.addId(bot.online ? "serverbots-online" : "serverbots-offline");
    row.addEntry(statusLabel);

    ButtonWidget copy = ButtonWidget.i18n("ggbot.serverbots.copy", () -> copyName(bot.name));
    copy.addId("serverbots-copy");
    row.addEntry(copy);

    return row;
  }

  private void copyName(String name) {
    de.ggbot.core.utils.GuiSounds.click();
    Laby.labyAPI().minecraft().setClipboard(name);
    Notification.Builder builder = Notification.builder()
        .title(Component.translatable("ggbot.serverbots.copied.title"))
        .text(Component.translatable("ggbot.serverbots.copied.text", Component.text(name)))
        .type(Notification.Type.SYSTEM);
    addon.labyAPI().notificationController().push(builder.build());
  }

  /**
   * Resolves the current server hostname to its base domain (e.g.
   * {@code play.griefergames.net} to {@code griefergames.net}) by matching against
   * the known public servers; falls back to the raw host on failure.
   */
  private String resolveServerDomain(PublicApi api) {
    String rawIp = Laby.labyAPI().serverController()
        .getCurrentServerData().address().getHost().toLowerCase();
    String[] parts = rawIp.split("\\.");
    if (parts.length <= 2) {
      return rawIp;
    }
    String baseDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
    try {
      for (Server server : api.getPublicServers()) {
        if (server.getName().equals(baseDomain)) {
          return baseDomain;
        }
      }
    } catch (ApiException e) {
      addon.logger().error("Failed to resolve server domain: " + e.getMessage());
      addon.getVersioningHandler().reportError(e);
    }
    return rawIp;
  }

  /** A bot's display name and online state. */
  private static final class BotInfo {
    final String name;
    final boolean online;

    BotInfo(String name, boolean online) {
      this.name = name;
      this.online = online;
    }
  }
}
