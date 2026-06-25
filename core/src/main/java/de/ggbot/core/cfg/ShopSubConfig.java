package de.ggbot.core.cfg;

import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Configuration for the in-game bot shop feature.
 * Changes take effect immediately without requiring a restart.
 */
public class ShopSubConfig extends Config {

  /**
   * Whether the shop hotkey is active.
   * When disabled, pressing the configured key does nothing.
   */
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> shopEnabled = new ConfigProperty<>(true);

  /**
   * The key that opens the nearest bot's sell shop.
   * The shop will only open if a GGBot player is within range.
   */
  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> shopKey = new ConfigProperty<>(new Key[]{Key.L_CONTROL, Key.O});

  /**
   * Maximum radius (in blocks) to search for nearby bot players when the shop
   * key is pressed.
   */
  @SliderSetting(min = 16, max = 128)
  public final ConfigProperty<Integer> shopRange = new ConfigProperty<>(64);
}
