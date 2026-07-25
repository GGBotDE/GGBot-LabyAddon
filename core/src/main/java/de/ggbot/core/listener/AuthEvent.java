package de.ggbot.core.listener;

import de.ggbot.core.utils.AsyncScheduler;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.TextDecoration;
import net.labymod.api.configuration.labymod.chat.ChatWindow;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;
import de.ggbot.core.GGBot;
import de.ggbot.core.auth.OAuthServer;
import net.labymod.api.util.I18n;

import java.io.IOException;

import static de.ggbot.core.api.BotRequests.getCachedBots;
import static de.ggbot.core.listener.SetupBotLogsChannel.getChatWindow;
import static net.labymod.api.client.component.format.NamedTextColor.BLUE;
import static net.labymod.api.client.component.format.NamedTextColor.GRAY;

/**
 * Listens for server connect/disconnect events to manage authentication prompts
 * and the local OAuth redirect server.
 */
public class AuthEvent {

  private final GGBot addon;
  private OAuthServer authServer;

  /**
   * Creates a new {@link AuthEvent} listener.
   *
   * @param addon the addon instance
   */
  public AuthEvent(GGBot addon) {
    this.addon = addon;
  }

  /**
   * Called when the player joins a server. If not authenticated, shows login
   * instructions and starts the OAuth redirect flow. If authenticated, validates
   * the currently configured bot.
   *
   * @param e the server join event
   * @throws IOException if the OAuth server cannot be started
   */
  @Subscribe
  public void onServerJoin(ServerJoinEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.base")) return;
    if (GGBot.isAuthenticated() && !GGBot.isTokenExpired()) {
      // Already logged in: no auth flow, so no local redirect socket either.
      loadBotData();
      return;
    }
    // The localhost redirect server is only opened while an auth flow is
    // actually pending; it is closed again on disconnect or once the code
    // arrives. A still-open server from an earlier join is closed first, so
    // its socket is released instead of forcing the replacement onto another
    // port.
    if (authServer != null) {
      authServer.close();
      authServer = null;
    }
    OAuthServer server;
    try {
      server = new OAuthServer(addon);
    } catch (IOException ioEx) {
      addon.logger().error("Failed to start OAuth server: " + ioEx.getMessage());
      addon.getVersioningHandler().reportError(ioEx);
      return;
    }
    authServer = server;
    if (addon.configuration().generalSub.joinNotificationEnabled.get()) {
      displayAuthPrompt();
    }
    startAuth(server);
  }

  /**
   * Called when the player disconnects from a server.
   * Closes the OAuth server if it is still running.
   *
   * @param e the server disconnect event
   */
  @Subscribe
  public void onServerDisconnect(ServerDisconnectEvent e) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.base")) return;
    if (authServer != null) authServer.close();
  }

  /**
   * Displays the welcome header and the appropriate authentication prompt
   * in the client chat (expired token or first-time login).
   */
  private void displayAuthPrompt() {
    Component header = Component.text()
        .append(Component.translatable("ggbot.messages.join.prefix1").color(NamedTextColor.BLUE))
        .append(Component.translatable("ggbot.messages.join.prefixname").color(NamedTextColor.AQUA))
        .append(Component.translatable("ggbot.messages.join.prefix2").color(NamedTextColor.BLUE))
        .build();

    Component reason;
    Component action;
    try {
      if (GGBot.isTokenExpired()) {
        reason = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.expired.text"))
            .color(NamedTextColor.RED).build();
        action = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.expired.text2")
                .decoration(TextDecoration.BOLD, true))
            .append(Component.translatable("ggbot.messages.join.error.expired.text3")
                .decoration(TextDecoration.BOLD, false))
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.openUrl(authServer.getUrl()))
            .build();
      } else {
        reason = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.authenticate.text"))
            .color(NamedTextColor.RED).build();
        action = Component.text()
            .append(Component.translatable("ggbot.messages.join.error.authenticate.text2")
                .decoration(TextDecoration.BOLD, true))
            .append(Component.translatable("ggbot.messages.join.error.authenticate.text3")
                .decoration(TextDecoration.BOLD, false))
            .color(NamedTextColor.GREEN)
            .clickEvent(ClickEvent.openUrl(authServer.getUrl()))
            .build();
      }
    } catch (IOException e) {
      addon.logger().error("Failed to generate auth prompt: " + e.getMessage());
      addon.getVersioningHandler().reportError(e);
      reason = Component.text()
          .append(Component.translatable("ggbot.messages.join.error.authenticate.text"))
          .color(NamedTextColor.RED).build();
      action = Component.text()
          .append(Component.translatable("ggbot.messages.join.error.authenticate.text2")
              .decoration(TextDecoration.BOLD, true))
          .append(Component.translatable("ggbot.messages.join.error.authenticate.text3")
              .decoration(TextDecoration.BOLD, false))
          .color(NamedTextColor.GREEN)
          .build();
    }

    Component footer = Component.text()
        .append(Component.translatable("ggbot.messages.join.prefix3").color(NamedTextColor.BLUE))
        .build();

    addon.displayMessage(header);
    addon.displayMessage(reason);
    addon.displayMessage(action);
    addon.displayMessage(footer);

    final Component finalReason = reason.copy();
    final Component finalAction = action.copy();
    AsyncScheduler.runLater(() -> {
      Laby.labyAPI().minecraft().executeNextTick(() -> {
        if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.api.logs"))
          return;
        ChatWindow mainWindow = getChatWindow();
        if (mainWindow == null)
          return;
        boolean tabExists = false;
        for (var tab : mainWindow.getTabs()) {
          if (tab.getName().equalsIgnoreCase(I18n.getTranslation("ggbot.messages.log.tab"))) {
            tabExists = true;
            break;
          }
        }
        if (!tabExists)
          return;

        addon.displayMessage(
            header.append(Component.translatable("ggbot.messages.log.textfilter", GRAY, Component.empty())));
        addon.displayMessage(
            finalReason.append(Component.translatable("ggbot.messages.log.textfilter", GRAY, Component.empty())));
        addon.displayMessage(
            finalAction.append(Component.translatable("ggbot.messages.log.textfilter", GRAY, Component.empty())));
        addon.displayMessage(
            footer.append(Component.translatable("ggbot.messages.log.textfilter", GRAY, Component.empty())));
      });
    }, 100);
  }

  /**
   * Begins the asynchronous OAuth flow: listens for the redirect code and
   * exchanges it for an access token, then persists the token to configuration.
   *
   * @param server the server this flow belongs to; the callbacks stay bound to
   *               it even when a later join replaces the field
   */
  private void startAuth(OAuthServer server) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.base")) return;
    try {
      server.listenForCodeAsync(code ->
          server.getTokenAsync(code, token -> {
            addon.configuration().token.set(token.get("access_token").getAsString());
            addon.configuration().expiresAt.set(String.valueOf(
                System.currentTimeMillis() + (token.get("expires_in").getAsInt() * 1000L)));
            GGBot.setAuthenticated(true);
          }));
    } catch (Exception e) {
      addon.logger().error("Error during authentication: " + e.getMessage());
      addon.getVersioningHandler().reportError(e);
    }
  }

  /**
   * Validates the selected bot entry in the configuration after a successful login.
   * Logs a warning if the stored bot ID is no longer present in the cached bot list.
   */
  private void loadBotData() {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.base")) return;
    String selectedBot = addon.configuration().botlist.get();
    if (selectedBot.isEmpty() || !selectedBot.contains("(") || !selectedBot.contains(")")) return;
    String id = selectedBot.substring(selectedBot.lastIndexOf('(') + 1, selectedBot.lastIndexOf(')'));
    boolean found = false;
    for (var bot : getCachedBots()) {
      if (String.valueOf(bot.getId()).equals(id)) {
        found = true;
        break;
      }
    }
    if (!found) {
      addon.logger().warn("Configured bot ID '" + id + "' not found in the cached bot list.");
    }
  }
}
