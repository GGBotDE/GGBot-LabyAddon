package de.ggbot.core.overlay;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.shop.utils.ShopDataCache;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.GetBotsOnServer200Response;
import de.ggbot.sdk.model.GetBotsOnServer200ResponseBotsInner;
import de.ggbot.sdk.model.Server;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.overlay.IngameOverlayRenderEvent;

/**
 * Shows a top-center hint ("Shop GUI for &lt;bot&gt; available - press &lt;key&gt; to
 * open it") whenever the player is near an online GGBot whose shop hotkey is enabled.
 *
 * <p>The set of GGBots on the server is taken from the same public, online-only bot
 * list the shop hotkey uses, so the hint and the hotkey always agree on whether a
 * nearby player is a shoppable bot. That list is fetched at most every
 * {@value #FETCH_INTERVAL_MS} ms and cached; the nearest-bot lookup and hint
 * component are recomputed at most every {@value #RECOMPUTE_INTERVAL_MS} ms.
 */
public class ShopHintRenderer {

  private static final long RECOMPUTE_INTERVAL_MS = 500L;
  private static final long FETCH_INTERVAL_MS = 15_000L;
  private static final String FEATURE = "de.ggbot.addon.shop";

  private final GGBot addon;

  private long lastComputeMs = 0L;
  private Component cachedHint = null;
  private boolean sawPost = false;

  /** Lower-case link names of the online GGBots on the current server. */
  private volatile Set<String> serverBots = new HashSet<>();
  private long lastFetchMs = 0L;
  private volatile boolean fetching = false;

  /** Normalized server host from the last bot-list fetch, for the sell-item cache. */
  private volatile String resolvedServerIp = null;

  public ShopHintRenderer(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onOverlayRender(IngameOverlayRenderEvent event) {
    if (event.phase() == Phase.POST) {
      sawPost = true;
    }
    // Prefer POST; if a version never emits POST, draw on PRE instead.
    if (!(event.phase() == Phase.POST || (!sawPost && event.phase() == Phase.PRE))) return;
    if (event.isGuiHidden()) return;
    if (!addon.configuration().shopSub.shopEnabled.get()) return;

    long now = System.currentTimeMillis();
    // The backend can pace this repeating fetch via the feature's interval config.
    long fetchIntervalMs = addon.getVersioningHandler()
        .clampIntervalMs(FEATURE, FETCH_INTERVAL_MS);
    if (!fetching && now - lastFetchMs >= fetchIntervalMs) {
      lastFetchMs = now;
      fetchServerBots();
    }
    if (now - lastComputeMs >= RECOMPUTE_INTERVAL_MS) {
      lastComputeMs = now;
      cachedHint = computeHint();
    }
    if (cachedHint == null) return;

    try {
      float screenW = Laby.labyAPI().minecraft().minecraftWindow().getScaledWidth();
      if (altRenderMode()) {
        // Alternate path (1.21.8+): measure with RenderableComponent (the legacy
        // width() returns 0 here), then submit to the canvas. Param order is
        // (component, x, y, color, maxWidth); -1 = opaque, component colors preserved.
        // submitComponent left-aligns at x, so centre by placing the left edge at
        // (screenW - width) / 2. Explicit scale 1.0 avoids inheriting a previous
        // scaled submission (that leftover scale was shifting this off-centre).
        float w = net.labymod.api.client.render.font.RenderableComponent.of(cachedHint).getWidth();
        event.context().canvas().submitComponent(cachedHint, (screenW - w) / 2f, 6, -1,
            1.0f, (int) Math.ceil(w));
      } else {
        float width = Laby.references().renderPipeline().componentRenderer().width(cachedHint);
        Laby.references().renderPipeline().componentRenderer().builder()
            .text(cachedHint)
            .pos((screenW - width) / 2f, 6)
            .shadow(true)
            .render(event.stack());
      }
    } catch (Throwable t) {
      if (!loggedDrawError) {
        loggedDrawError = true;
        addon.logger().error("[GGBot] Shop hint draw failed (render API may differ "
            + "on this version): " + t);
      }
    }
  }

  private boolean altRenderMode() {
    return de.ggbot.core.utils.RenderEngine.useCanvas();
  }

  private boolean loggedDrawError = false;

  /** Builds the hint component for the nearest in-range bot, or {@code null}. */
  private Component computeHint() {
    String botName = nearestBotName();
    if (botName == null) return null;

    // Prefetch the bot's sell items early: a bot without sell items gets no
    // hint at all, and by the time the user presses the key the shop GUI can
    // reuse the cached result instead of re-doing the request.
    String serverIp = resolvedServerIp;
    if (serverIp == null) return null;
    ShopDataCache.Entry shopData =
        ShopDataCache.get(botName, serverIp, ShopDataCache.HINT_MAX_AGE_MS);
    if (shopData == null) {
      ShopDataCache.fetchAsync(addon.getVersioningHandler(), botName, serverIp, null);
      return null;
    }
    if (shopData.getSellItems().isEmpty()) return null;

    String keyCombo = formatCombo(addon.configuration().shopSub.shopKey.get());
    return Component.translatable("ggbot.botmenu.shopHint",
        NamedTextColor.AQUA,
        Component.text(botName, NamedTextColor.YELLOW),
        Component.text(keyCombo, NamedTextColor.GOLD));
  }

  /**
   * Returns the bot the shop hotkey would open: the in-range online GGBot with the
   * best look-alignment score (same scoring the hotkey uses), so the hint always
   * names exactly the bot that pressing the key will open - never a closer non-bot.
   */
  private String nearestBotName() {
    Set<String> bots = serverBots;
    if (bots.isEmpty()) return null;
    ClientPlayer local = Laby.labyAPI().minecraft().getClientPlayer();
    if (local == null || Laby.labyAPI().minecraft().clientWorld() == null) return null;

    double range = addon.configuration().shopSub.shopRange.get();
    double yawRad = Math.toRadians(local.getRotationYaw());
    double pitchRad = Math.toRadians(local.getRotationPitch());
    double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
    double lookY = -Math.sin(pitchRad);
    double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

    double bestScore = -Double.MAX_VALUE;
    String best = null;

    for (Player player : Laby.labyAPI().minecraft().clientWorld().getPlayers()) {
      if (player == local) continue;
      String name = player.getName();
      // Only consider actual online GGBots.
      if (name == null || !bots.contains(name.toLowerCase(Locale.ROOT))) continue;
      double dx = player.position().getX() - local.position().getX();
      double dy = player.position().getY() - local.position().getY();
      double dz = player.position().getZ() - local.position().getZ();
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist > range || dist < 0.001) continue;
      double alignment = (dx * lookX + dy * lookY + dz * lookZ) / dist;
      double score = alignment * 10.0 - dist;
      if (score > bestScore) {
        bestScore = score;
        best = name;
      }
    }
    return best;
  }

  /** Refreshes the cached online server-bot names from the public list. */
  private void fetchServerBots() {
    if (!addon.getVersioningHandler().isFeatureEnabled(FEATURE)
        || addon.labyAPI().serverController().getCurrentServerData() == null) {
      serverBots = new HashSet<>();
      return;
    }
    fetching = true;
    Thread thread = new Thread(() -> {
      Set<String> names = new HashSet<>();
      try {
        PublicApi api = new PublicApi();
        api.setCustomBaseUrl(addon.getVersioningHandler().getBaseUrlForFeature(FEATURE));
        String serverIp = resolveServerDomain(api);
        resolvedServerIp = serverIp;
        GetBotsOnServer200Response response = api.getBotsOnServer(serverIp);
        if (response != null && response.getBots() != null) {
          for (GetBotsOnServer200ResponseBotsInner bot : response.getBots()) {
            if (bot.getLinkName() != null && Boolean.TRUE.equals(bot.getOnline())) {
              names.add(bot.getLinkName().toLowerCase(Locale.ROOT));
            }
          }
        }
      } catch (ApiException e) {
        addon.logger().error("Failed to fetch server bots for hint: " + e.getMessage());
        addon.getVersioningHandler().reportError(e);
      } finally {
        serverBots = names;
        fetching = false;
      }
    }, "ggbot-shophint-fetch");
    thread.setDaemon(true);
    thread.start();
  }

  private String resolveServerDomain(PublicApi api) {
    if (Laby.labyAPI().serverController().getCurrentServerData() == null) {
      return "";
    }
    String rawIp = Laby.labyAPI().serverController()
        .getCurrentServerData().address().getHost().toLowerCase(Locale.ROOT);
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

  private static String formatCombo(Key[] combo) {
    if (combo == null || combo.length == 0) return "?";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < combo.length; i++) {
      if (i > 0) sb.append(" + ");
      sb.append(combo[i].getActualName());
    }
    return sb.toString();
  }
}
