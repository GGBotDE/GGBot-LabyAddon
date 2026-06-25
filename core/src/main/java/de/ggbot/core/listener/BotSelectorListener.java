package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.botselector.BotSelectorActivity;
import de.ggbot.core.utils.KeyComboTrigger;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

public class BotSelectorListener {

  private final GGBot addon;
  private final KeyComboTrigger comboTrigger = new KeyComboTrigger();

  public BotSelectorListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent e) {
    if (!addon.configuration().generalSub.botSelectorEnabled.get()) return;
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.botselector")) return;

    // Don't trigger hotkeys while a screen (chat or any menu) is open; reset so stale
    // key state from before the screen cannot cause a re-trigger.
    if (Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      comboTrigger.reset();
      return;
    }

    if (!comboTrigger.test(e, addon.configuration().generalSub.botSelectorKey.get())) return;

    Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
      BotSelectorActivity activity = new BotSelectorActivity();
      Laby.labyAPI().minecraft().minecraftWindow().displayScreen(activity);
    });
  }
}
