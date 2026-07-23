package de.ggbot.core.cfg;

import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Configuration for the in-game bot shop feature.
 * Changes take effect immediately without requiring a restart.
 */
public class ShopSubConfig extends Config {

  /**
   * Whether the shop feature (hotkey + hint overlay) is active.
   * Disabled by default so users without a linked account do not cause any
   * shop-related API traffic (server hostname lookups) unless they opt in,
   * matching the opt-in behaviour of the tab-list bot counter.
   */
  @ShowSettingInParent
  @SwitchSetting
  public final ConfigProperty<Boolean> shopEnabled = new ConfigProperty<>(false);

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

  /**
   * Chat command template used to pay the bot when buying. Supports the
   * placeholders {@code %bot%} (the bot's player name) and {@code %amount%}
   * (the price of the purchased item), so servers with a different payment
   * command than {@code /pay} keep working.
   */
  @TextFieldSetting
  public final ConfigProperty<String> payCommand = new ConfigProperty<>("/pay %bot% %amount%");
}
