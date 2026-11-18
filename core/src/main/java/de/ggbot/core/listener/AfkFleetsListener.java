package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.afkfleets.AfkFleetsActivity;
import de.ggbot.core.utils.KeyComboTrigger;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

public class AfkFleetsListener {

  private final GGBot addon;
  private final KeyComboTrigger comboTrigger = new KeyComboTrigger();

  public AfkFleetsListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent e) {
    if (!addon.configuration().generalSub.afkFleetsEnabled.get()) return;
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.afkfleets")) return;

    if (Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      comboTrigger.reset();
      return;
    }

    if (!comboTrigger.test(e, addon.configuration().generalSub.afkFleetsKey.get())) return;

    Laby.labyAPI().minecraft().executeOnRenderThread(() ->
        Laby.labyAPI().minecraft().minecraftWindow().displayScreen(new AfkFleetsActivity(addon)));
  }
}
