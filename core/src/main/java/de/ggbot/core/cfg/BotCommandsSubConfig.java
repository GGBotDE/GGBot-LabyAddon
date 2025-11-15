package de.ggbot.core.cfg;

import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

public class BotCommandsSubConfig extends Config {
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> botCommands = new ConfigProperty<>(true);

  @TextFieldSetting(maxLength = 1)
  public final ConfigProperty<String> prefix = new ConfigProperty<>("!");

}
