package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;



public class BotLogsSubConfig extends Config {
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> botLog = new ConfigProperty<>(true);

  @SliderSetting(min = 1, max = 10)
  public final ConfigProperty<Integer> minutes = new ConfigProperty<>(1)
      .addChangeListener(minutesValue -> {
        if (GGBot.getInstance().configuration().botlogSub.botLog.get()
            && GGBot.getInstance().labyAPI().serverController().isConnected()) {
          GGBot.getInstance().getTimerListener().cancelLogTimer();
          GGBot.getInstance().getTimerListener().startLogTimer(minutesValue * 60 * 1000L);
        }
      });


}
