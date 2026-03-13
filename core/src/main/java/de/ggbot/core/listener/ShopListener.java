package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.Bot;
import de.ggbot.sdk.model.Server;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.notification.Notification;
import java.util.List;

/**
 * Listens for the configurable shop key and opens the {@link ShopInterfaceActivity}
 * for the nearest visible GGBot player in the world.
 *
 * <p>Bot candidates are scored by a combination of distance and look-direction
 * alignment so that the bot the player is facing is preferred over one that is
 * simply close but behind them.
 */
public class ShopListener {

  private final GGBot addon;

  public ShopListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.shop")) return;
    if (!addon.configuration().shopSub.shopEnabled.get()) return;
    if (!e.state().name().equalsIgnoreCase("PRESS")) return;

    // Support multi-key combos — the pressed key must be part of the configured combo.
    Key[] combo = addon.configuration().shopSub.shopKey.get();
    if (combo == null || combo.length == 0) return;
    boolean keyInCombo = false;
    for (Key k : combo) {
      if (e.key().equals(k)) {
        keyInCombo = true;
        break;
      }
    }
    if (!keyInCombo) return;

    // Ensure ALL combo keys are currently pressed
    for (Key k : combo) {
      if (!k.isPressed()) {
        return; // wait until the rest are pressed
      }
    }

    // Entity access must happen on the tick thread; network work goes to a background thread.
    Laby.labyAPI().minecraft().executeNextTick(() -> {
      Player bestBot = findBestNearbyBot();
      if (bestBot == null) {
        Notification.Builder builder = Notification.builder()
            .title(Component.text("GGBot", NamedTextColor.RED))
            .text(Component.translatable("ggbot.messages.shop.nobot"))
            .type(Notification.Type.SYSTEM);
        Laby.labyAPI().notificationController().push(builder.build());
        addon.displayMessage(Component.translatable("ggbot.messages.shop.nobot"));
        return;
      }

      String botName = bestBot.getName();
      String rawServerIp = addon.labyAPI().serverController()
          .getCurrentServerData().address().getHost().toLowerCase();

      Thread verifyThread = new Thread(() -> {
        String serverIp = resolveServerDomain(rawServerIp);

        try {
          PublicApi verifyApi = new PublicApi();
          verifyApi.setCustomBaseUrl(
              addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.shop"));
          verifyApi.getPublicBotByLink(botName, serverIp);
        } catch (ApiException ex) {
          // Not a registered GGBot — abort silently.
          return;
        }

        final String finalServerIp = serverIp;
        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          ShopInterfaceActivity activity = new ShopInterfaceActivity(
              botName, finalServerIp, addon.getVersioningHandler());

          activity.onCancel(activity::closeScreen);
          activity.onMoneyCheck(requiredAmount -> true);

          activity.onPurchase(cartItems -> {
            activity.closeScreen();
            Thread purchaseThread = new Thread(() -> {
              for (var entry : cartItems) {
                for (int i = 0; i < entry.getQuantity(); i++) {
                  Laby.labyAPI().minecraft().executeNextTick(() ->
                      Laby.references().chatExecutor().chat(
                          "/pay " + botName + " " + entry.getItem().getPrice()));
                  try {
                    Thread.sleep(3000);
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                  }
                }
              }
            }, "ggbot-purchase");
            purchaseThread.setDaemon(true);
            purchaseThread.start();
          });

          Laby.labyAPI().minecraft().minecraftWindow().displayScreen(activity);
        });
      }, "ggbot-shop-verify");
      verifyThread.setDaemon(true);
      verifyThread.start();
    });
  }

  /**
   * Finds the closest GGBot player in the world, biased toward the direction
   * the local player is looking. Returns {@code null} if no matching bot is within
   * 64 blocks or if no bots are cached.
   *
   * <p>Score formula: {@code alignment * 10.0 - distanceBlocks}
   * where {@code alignment} is the dot-product cosine of the look-vector and the
   * direction towards the candidate (range −1 … 1). Higher score wins.
   */
  private Player findBestNearbyBot() {
    List<Bot> cached = BotRequests.getCachedBots();
    if (cached.isEmpty()) return null;

    ClientPlayer local = Laby.labyAPI().minecraft().clientPlayer();
    if (local == null) return null;

    // Build unit look-vector from yaw/pitch.
    double yawRad   = Math.toRadians(local.getRotationYaw());
    double pitchRad = Math.toRadians(local.getRotationPitch());
    double lookX =  -Math.sin(yawRad) * Math.cos(pitchRad);
    double lookY =  -Math.sin(pitchRad);
    double lookZ =   Math.cos(yawRad) * Math.cos(pitchRad);

    Player bestPlayer = null;
    double bestScore  = Double.NEGATIVE_INFINITY;

    for (Player player : Laby.labyAPI().minecraft().clientWorld().getPlayers()) {
      if (player == local) continue;
      if (!isBotName(player.getName(), cached)) continue;

      double dx   = player.position().getX() - local.position().getX();
      double dy   = player.position().getY() - local.position().getY();
      double dz   = player.position().getZ() - local.position().getZ();
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

      if (dist > addon.configuration().shopSub.shopRange.get() || dist < 0.001) continue;

      double alignment = (dx * lookX + dy * lookY + dz * lookZ) / dist;
      double score     = alignment * 10.0 - dist;

      if (score > bestScore) {
        bestScore  = score;
        bestPlayer = player;
      }
    }

    return bestPlayer;
  }

  /**
   * Normalizes a raw server hostname to its base domain by verifying against
   * known GGBot servers (e.g. {@code play.griefergames.net} →
   * {@code griefergames.net}). Returns {@code rawIp} unchanged on failure or
   * when the hostname is already two parts.
   */
  private String resolveServerDomain(String rawIp) {
    String[] parts = rawIp.split("\\.");
    if (parts.length <= 2) return rawIp; // Already base domain or plain IP.
    String baseDomain = parts[parts.length - 2] + "." + parts[parts.length - 1];
    try {
      PublicApi api = new PublicApi();
      api.setCustomBaseUrl(
          addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.shop"));
      for (Server server : api.getPublicServers()) {
        if (server.getName().equals(baseDomain)) return baseDomain;
      }
    } catch (ApiException ex) {
      addon.logger().error("Failed to resolve server domain: " + ex.getMessage());
      addon.getVersioningHandler().reportError(ex);
    }
    return rawIp;
  }

  /** Returns {@code true} if {@code name} matches the link-name of any cached bot. */
  private static boolean isBotName(String name, List<Bot> bots) {
    for (Bot bot : bots) {
      if (name.equalsIgnoreCase(bot.getLinkName())) return true;
    }
    return false;
  }
}
