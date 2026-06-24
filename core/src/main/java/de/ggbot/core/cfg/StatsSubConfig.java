package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Configuration for the bot statistics polling timer (money, health, CityBuild,
 * plot, and ticket widgets). Changes take effect immediately without requiring a rejoin.
 */
public class StatsSubConfig extends Config {

  /**
   * Whether the statistics timer is active. Toggling this off cancels the running
   * timer; toggling back on restarts it with the current interval.
   */
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> statsEnabled = new ConfigProperty<>(true)
      .addChangeListener(enabled -> {
        if (!GGBot.getInstance().labyAPI().serverController().isConnected()) return;
        if (enabled) {
          GGBot.getInstance().getTimerListener().startStatsTimer(GGBot.getInstance().configuration().statsSub.statsMinutes.get() * 60_000L);
        } else {
          GGBot.getInstance().getTimerListener().cancelStatsTimer();
        }
      });

  /** How often (in minutes) the money, health, CityBuild, plot and ticket widgets are refreshed. */
  @SliderSetting(min = 1, max = 30)
  public final ConfigProperty<Integer> statsMinutes = new ConfigProperty<>(10)
      .addChangeListener(minutes -> {
        if (!GGBot.getInstance().configuration().statsSub.statsEnabled.get()) return;
        if (!GGBot.getInstance().labyAPI().serverController().isConnected()) return;
        GGBot.getInstance().getTimerListener().cancelStatsTimer();
        GGBot.getInstance().getTimerListener().startStatsTimer(minutes * 60_000L);
      });
}
