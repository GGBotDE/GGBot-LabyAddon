package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.utils.ShopDataCache;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, String> serverBots = fetchServerBotTokens(serverIp);
        String botName = null;
        for (String candidate : candidates) {
          if (serverBots.containsKey(candidate.toLowerCase())) {
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
        final String botToken = serverBots.get(botName.toLowerCase());
        // Ensures the bot's sell area is checked before allowing payment.
        ShopDataCache.getOrFetch(addon.getVersioningHandler(), finalBotName, finalServerIp,
            botToken, ShopDataCache.GUI_MAX_AGE_MS,
            entry -> Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
              if (!ShopDataCache.playerInSellArea(entry)) {
                notifyOutsideArea();
                return;
              }
              openShop(finalBotName, finalServerIp, botToken);
            }));
      }, "ggbot-shop-verify");
      verifyThread.setDaemon(true);
      verifyThread.start();
    });
  }

  /** Builds and shows the shop interface for the resolved bot. */
  private void openShop(String botName, String serverIp, String botToken) {
    ShopInterfaceActivity activity = new ShopInterfaceActivity(
        botName, serverIp, botToken, addon.getVersioningHandler());

    activity.onCancel(activity::closeScreen);
    activity.onMoneyCheck(requiredAmount -> true);

    /*
     * @LabyMod review team: sending payment commands on the user's behalf is
     * guarded on multiple levels. The user explicitly clicked "buy" in the
     * shop GUI for a concrete cart (as you mentioned); the commands only target verified
     * online GGBots on this server; and we do not let bots activate any features the
     * server does not allow - a bot on a server where selling is not permitted
     * does not have the sell feature enabled, its sell item list is empty on
     * request, so the shop hint never shows and there is nothing to buy in the
     * first place.
     */
    activity.onPurchase(cartItems -> {
      activity.closeScreen();
      String payTemplate = addon.configuration().shopSub.payCommand.get();
      Thread purchaseThread = new Thread(() -> {
        for (var entry : cartItems) {
          for (int i = 0; i < entry.getQuantity(); i++) {
            String command = payTemplate
                .replace("%bot%", botName)
                .replace("%amount%", String.valueOf(entry.getItem().getPrice()));
            Laby.labyAPI().minecraft().executeNextTick(() ->
                Laby.references().chatExecutor().chat(command));
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
    notify("ggbot.messages.shop.nobot");
  }

  private void notifyOutsideArea() {
    notify("ggbot.messages.shop.outsideArea");
  }

  private void notify(String textKey) {
    Notification.Builder builder = Notification.builder()
        .title(Component.text("GGBot", NamedTextColor.RED))
        .text(Component.translatable(textKey))
        .type(Notification.Type.SYSTEM);
    Laby.labyAPI().notificationController().push(builder.build());
  }

  /**
   * Retrieves the publicly available, unauthenticated GGBots on the specified
   * normalized server using {@code getBotsOnServer}, returning a map of
   * lower-case link names to their corresponding bot tokens.
   * Only online bots are included, since matching a name alone does not
   * guarantee that the account is currently running GGBot.
   *
   * @param serverIp the normalized server host
   * @return a map of lowercase bot names to their tokens
   */
  private Map<String, String> fetchServerBotTokens(String serverIp) {
    Map<String, String> bots = new HashMap<>();
    try {
      PublicApi api = new PublicApi();
      api.setCustomBaseUrl(addon.getVersioningHandler().getBaseUrlForFeature("de.ggbot.addon.shop"));
      GetBotsOnServer200Response response = api.getBotsOnServer(serverIp, true);
      if (response != null && response.getBots() != null) {
        for (GetBotsOnServer200ResponseBotsInner bot : response.getBots()) {
          if (bot.getLinkName() != null && bot.getToken() != null) {
            bots.put(bot.getLinkName().toLowerCase(), bot.getToken());
          }
        }
      }
    } catch (ApiException ex) {
      addon.logger().error("Failed to fetch bots on server: " + ex.getMessage());
      addon.getVersioningHandler().reportError(ex);
    }
    return bots;
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
    ClientPlayer local = Laby.labyAPI().minecraft().getClientPlayer();
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
