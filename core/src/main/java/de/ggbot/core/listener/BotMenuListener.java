package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.botmenu.BotMenuActivity;
import de.ggbot.core.gui.botmenu.BotMenuPositions;
import de.ggbot.core.gui.botmenu.BotMenuState;
import de.ggbot.core.gui.botmenu.ChestItemCache;
import de.ggbot.core.gui.botmenu.ControlModeManager;
import de.ggbot.core.utils.KeyComboTrigger;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/**
 * Opens the bot management menu via its hotkey and handles the WorldEdit
 * {@code pos1}/{@code pos2} keybinds.
 */
public class BotMenuListener {

  private final GGBot addon;
  private final KeyComboTrigger menuTrigger = new KeyComboTrigger();
  private final KeyComboTrigger pos1Trigger = new KeyComboTrigger();
  private final KeyComboTrigger pos2Trigger = new KeyComboTrigger();
  private final KeyComboTrigger controlTrigger = new KeyComboTrigger();

  public BotMenuListener(GGBot addon) {
    this.addon = addon;
    // Sync persistent toggle settings into the live runtime state on startup.
    BotMenuState.get().setHighlightKickAreas(addon.configuration().botMenuSub.highlightKickAreas.get());
    BotMenuState.get().setShowChestContents(addon.configuration().botMenuSub.showChestContents.get());
  }

  @Subscribe
  public void onKey(KeyEvent e) {
    if (!addon.configuration().botMenuSub.menuEnabled.get()) return;

    // Hotkeys must not fire while a screen (chat or any menu) is open; reset every
    // combo so stale key state from before the screen cannot cause a re-trigger.
    if (Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      menuTrigger.reset();
      pos1Trigger.reset();
      pos2Trigger.reset();
      controlTrigger.reset();
      return;
    }

    if (pos1Trigger.test(e, addon.configuration().botMenuSub.pos1Key.get())) {
      BotMenuPositions.setFromLook(true);
    }
    if (pos2Trigger.test(e, addon.configuration().botMenuSub.pos2Key.get())) {
      BotMenuPositions.setFromLook(false);
    }

    if (controlTrigger.test(e, addon.configuration().botMenuSub.controlKey.get())
        && addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.controlmode")) {
      Bot controlBot = BotRequests.getBotFromCache(addon);
      if (controlBot != null) {
        ControlModeManager.get().toggle(controlBot);
      }
    }

    if (menuTrigger.test(e, addon.configuration().botMenuSub.menuKey.get())
        && addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.botmenu")) {
      Bot bot = BotRequests.getBotFromCache(addon);
      if (bot != null) {
        ChestItemCache.refresh(bot);
        BotRequests.getActiveModulesAsync(addon, bot,
            modules -> BotMenuState.get().setActiveModules(modules));
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(() ->
          Laby.labyAPI().minecraft().minecraftWindow().displayScreen(new BotMenuActivity()));
    }
  }

  /** Safety net: never leave control states active after leaving a server. */
  @Subscribe
  public void onDisconnect(ServerDisconnectEvent e) {
    ControlModeManager.get().stop();
  }

  /** Refresh the cached active modules so player interactions show/hide correctly. */
  @Subscribe
  public void onJoin(ServerJoinEvent e) {
    Bot bot = BotRequests.getBotFromCache(addon);
    if (bot != null) {
      BotRequests.getActiveModulesAsync(addon, bot,
          modules -> BotMenuState.get().setActiveModules(modules));
    }
  }
}

