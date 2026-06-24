package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.utils.KeyComboTrigger;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.GetBotsOnServer200Response;
import de.ggbot.sdk.model.GetBotsOnServer200ResponseBotsInner;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  private final KeyComboTrigger comboTrigger = new KeyComboTrigger();

  public ShopListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.shop")) return;
    if (!addon.configuration().shopSub.shopEnabled.get()) return;

    // Hotkeys must not trigger while a screen (chat or any menu) is open; reset the
    // combo so the stale key state from before the screen cannot cause a re-trigger.
    if (Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      comboTrigger.reset();
      return;
    }

    // Rising-edge combo detection prevents the screen-focus "toggle" bug.
    if (!comboTrigger.test(e, addon.configuration().shopSub.shopKey.get())) return;

    // Entity access must happen on the tick thread; network work goes to a background thread.
    Laby.labyAPI().minecraft().executeNextTick(() -> {
      // Nearby player names ordered by how closely the player is looking at them.
      List<String> candidates = findNearbyPlayerNames();
      if (candidates.isEmpty()) {
        notifyNoBot();
        return;
      }
      String rawServerIp = addon.labyAPI().serverController()
          .getCurrentServerData().address().getHost().toLowerCase();

      Thread verifyThread = new Thread(() -> {
        String serverIp = resolveServerDomain(rawServerIp);

        // Public, unauthenticated lookup of every GGBot on this server.
        Set<String> serverBots = fetchServerBotNames(serverIp);
        String botName = null;
        for (String candidate : candidates) {
          if (serverBots.contains(candidate.toLowerCase())) {
            botName = candidate;
            break;
          }
        }
        if (botName == null) {
          Laby.labyAPI().minecraft().executeOnRenderThread(this::notifyNoBot);
          return;
        }

        final String finalServerIp = serverIp;
        final String finalBotName = botName;
        Laby.labyAPI().minecraft().executeOnRenderThread(
            () -> openShop(finalBotName, finalServerIp));
      }, "ggbot-shop-verify");
      verifyThread.setDaemon(true);
      verifyThread.start();
    });
  }

  /** Builds and shows the shop interface for the resolved bot. */
  private void openShop(String botName, String serverIp) {
    ShopInterfaceActivity activity = new ShopInterfaceActivity(
        botName, serverIp, addon.getVersioningHandler());

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
  }

  private void notifyNoBot() {
    Notification.Builder builder = Notification.builder()
        .title(Component.text("GGBot", NamedTextColor.RED))
        .text(Component.translatable("ggbot.messages.shop.nobot"))
        .type(Notification.Type.SYSTEM);
    Laby.labyAPI().notificationController().push(builder.build());
  }

  /**
   * Returns the public, unauthenticated set of GGBot link-names present on the
   * given (normalized) server, via {@code getBotsOnServer}.
   */
  private Set<String> fetchServerBotNames(String serverIp) {
    Set<String> names = new HashSet<>();
    try {
      PublicApi api = new PublicApi();
      api.setCustomBaseUrl(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.shop"));
      GetBotsOnServer200Response response = api.getBotsOnServer(serverIp);
      if (response != null && response.getBots() != null) {
        for (GetBotsOnServer200ResponseBotsInner bot : response.getBots()) {
          // Only treat a player as a shoppable GGBot when it is actually online;
          // otherwise the account could just be logged in without running GGBot.
          if (bot.getLinkName() != null && Boolean.TRUE.equals(bot.getOnline())) {
            names.add(bot.getLinkName().toLowerCase());
          }
        }
      }
    } catch (ApiException ex) {
      addon.logger().error("Failed to fetch bots on server: " + ex.getMessage());
      addon.getVersioningHandler().reportError(ex);
    }
    return names;
  }

  /**
   * Returns up to 10 nearby player names ordered by how closely the local player
   * is looking at them (within the configured shop range). Must be called on the
   * tick thread because it reads world entities.
   *
   * <p>Score formula: {@code alignment * 10.0 - distanceBlocks}, where alignment is
   * the cosine between the look vector and the direction to the candidate.
   */
  private List<String> findNearbyPlayerNames() {
    List<String> result = new ArrayList<>();
    ClientPlayer local = Laby.labyAPI().minecraft().clientPlayer();
    if (local == null || Laby.labyAPI().minecraft().clientWorld() == null) {
      return result;
    }

    double yawRad = Math.toRadians(local.getRotationYaw());
    double pitchRad = Math.toRadians(local.getRotationPitch());
    double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
    double lookY = -Math.sin(pitchRad);
    double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);

    class Candidate {
      final String name;
      final double score;

      Candidate(String name, double score) {
        this.name = name;
        this.score = score;
      }
    }

    List<Candidate> candidates = new ArrayList<>();
    double range = addon.configuration().shopSub.shopRange.get();

    for (Player player : Laby.labyAPI().minecraft().clientWorld().getPlayers()) {
      if (player == local || player.getName() == null) continue;

      double dx = player.position().getX() - local.position().getX();
      double dy = player.position().getY() - local.position().getY();
      double dz = player.position().getZ() - local.position().getZ();
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist > range || dist < 0.001) continue;

      double alignment = (dx * lookX + dy * lookY + dz * lookZ) / dist;
      candidates.add(new Candidate(player.getName(), alignment * 10.0 - dist));
    }

    candidates.sort((a, b) -> Double.compare(b.score, a.score));
    int limit = Math.min(10, candidates.size());
    for (int i = 0; i < limit; i++) {
      result.add(candidates.get(i).name);
    }
    return result;
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

}
