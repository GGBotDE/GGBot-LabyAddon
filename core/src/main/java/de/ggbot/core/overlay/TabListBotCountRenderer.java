package de.ggbot.core.overlay;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.GetBotsOnServer200Response;
import de.ggbot.sdk.model.GetBotsOnServer200ResponseBotsInner;
import de.ggbot.sdk.model.Server;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.overlay.IngameOverlayRenderEvent;

/**
 * Optionally shows how many GGBots are online on the current server, rendered at the
 * top-centre of the tab list while it is open. The footer of the vanilla tab list is
 * server-controlled and read-only via the API, so the count is drawn as an overlay
 * instead.
 *
 * <p>The count is fetched from the public bot list at most once every
 * {@value #FETCH_INTERVAL_MS} ms and cached, so the render path does no network work.
 */
public class TabListBotCountRenderer {

  private static final String FEATURE = "de.ggbot.addon.tablistcount";
  private static final long FETCH_INTERVAL_MS = 30_000L;

  private final GGBot addon;

  private volatile int onlineCount = -1;
  private long lastFetchMs = 0L;
  private volatile boolean fetching = false;
  private boolean sawPost = false;

  public TabListBotCountRenderer(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onOverlayRender(IngameOverlayRenderEvent event) {
    if (event.phase() == Phase.POST) {
      sawPost = true;
    }
    if (!(event.phase() == Phase.POST || (!sawPost && event.phase() == Phase.PRE))) {
      return;
    }
    if (event.isGuiHidden()) {
      return;
    }
    if (!Boolean.TRUE.equals(addon.configuration().generalSub.tabListBotCount.get())) {
      return;
    }
    if (!addon.getVersioningHandler().isFeatureEnabled(FEATURE)) {
      return;
    }
    if (!addon.labyAPI().serverController().isConnected()) {
      return;
    }
    net.labymod.api.client.scoreboard.TabList tabList = Laby.labyAPI().minecraft().getTabList();
    if (tabList == null || !tabList.isVisible()) {
      return;
    }

    long now = System.currentTimeMillis();
    if (!fetching && now - lastFetchMs >= FETCH_INTERVAL_MS) {
      lastFetchMs = now;
      fetchCount();
    }
    if (onlineCount < 0) {
      return;
    }

    Component text = Component.translatable("ggbot.tablist.online",
        NamedTextColor.AQUA,
        Component.text(String.valueOf(onlineCount), NamedTextColor.GREEN));
    float screenW = Laby.labyAPI().minecraft().minecraftWindow().getScaledWidth();
    float y = 2;
    // Manual horizontal nudge (the canvas centering for this element is unreliable in
    // the tab-list render state, so it can be corrected from the settings).
    float offset = addon.configuration().generalSub.tabListCountXOffset.get();
    if (altRenderMode()) {
      float w = net.labymod.api.client.render.font.RenderableComponent.of(text).getWidth();
      event.context().canvas().submitComponent(text, (screenW - w) / 2f + offset, y, -1,
          1.0f, (int) Math.ceil(w));
    } else {
      float width = Laby.references().renderPipeline().componentRenderer().width(text);
      Laby.references().renderPipeline().componentRenderer().builder()
          .text(text)
          .pos((screenW - width) / 2f + offset, y)
          .shadow(true)
          .render(event.stack());
    }
  }

  private boolean altRenderMode() {
    return de.ggbot.core.utils.RenderEngine.useCanvas();
  }

  private void fetchCount() {
    fetching = true;
    Thread thread = new Thread(() -> {
      int count = -1;
      try {
        PublicApi api = new PublicApi();
        api.setCustomBaseUrl(addon.getVersioningHandler().getBaseUrlForFeature(FEATURE));
        String serverIp = resolveServerDomain(api);
        GetBotsOnServer200Response response = api.getBotsOnServer(serverIp);
        if (response != null && response.getBots() != null) {
          count = 0;
          for (GetBotsOnServer200ResponseBotsInner bot : response.getBots()) {
            if (Boolean.TRUE.equals(bot.getOnline())) {
              count++;
            }
          }
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to fetch tab list bot count: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      } finally {
        onlineCount = count;
        fetching = false;
      }
    }, "ggbot-tablist-count");
    thread.setDaemon(true);
    thread.start();
  }

  private String resolveServerDomain(PublicApi api) {
    if (Laby.labyAPI().serverController().getCurrentServerData() == null) {
      return "";
    }
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
}
