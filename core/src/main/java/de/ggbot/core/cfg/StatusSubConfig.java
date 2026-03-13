package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Configuration for the bot status polling timer.
 * Changes to both fields take effect immediately without requiring a rejoin.
 */
public class StatusSubConfig extends Config {

  /**
   * Whether the status timer is active. Toggling this off cancels the running timer;
   * toggling it back on restarts it with the current interval.
   */
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> statusEnabled = new ConfigProperty<>(true)
      .addChangeListener(enabled -> {
        if (!GGBot.getInstance().labyAPI().serverController().isConnected()) return;
        if (enabled) {
          GGBot.getInstance().getTimerListener().startStatusTimer(
              GGBot.getInstance().configuration().statusSub.statusMinutes.get() * 60_000L);
        } else {
          GGBot.getInstance().getTimerListener().cancelStatusTimer();
        }
      });

  /** How often (in minutes) the bot name and status widgets are refreshed. */
  @SliderSetting(min = 1, max = 10)
  public final ConfigProperty<Integer> statusMinutes = new ConfigProperty<>(1)
      .addChangeListener(minutes -> {
        if (!GGBot.getInstance().configuration().statusSub.statusEnabled.get()) return;
        if (!GGBot.getInstance().labyAPI().serverController().isConnected()) return;
        GGBot.getInstance().getTimerListener().cancelStatusTimer();
        GGBot.getInstance().getTimerListener().startStatusTimer(minutes * 60_000L);
      });
}
