package de.ggbot.core.cfg;

import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.color.ColorPickerWidget.ColorPickerSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Customization for the world overlays (module location markers, sell area and kick
 * areas). Colours are stored as ARGB integers, matching the overlay renderer, so
 * they can be used directly without conversion.
 */
public class OverlaySubConfig extends Config {

  @ColorPickerSetting(alpha = true)
  public final ConfigProperty<Integer> sellAreaColor = new ConfigProperty<>(0xFF00FF00);

  @ColorPickerSetting(alpha = true)
  public final ConfigProperty<Integer> sellDropColor = new ConfigProperty<>(0xFFFFFF00);

  @ColorPickerSetting(alpha = true)
  public final ConfigProperty<Integer> chestOverrideColor = new ConfigProperty<>(0xFF0000FF);

  @ColorPickerSetting(alpha = true)
  public final ConfigProperty<Integer> trashChestColor = new ConfigProperty<>(0xFFFF8800);

  @ColorPickerSetting(alpha = true)
  public final ConfigProperty<Integer> kickAreaColor = new ConfigProperty<>(0xFFFFB84C);

  /** Outline thickness (in pixels) of overlay boxes. */
  @SliderSetting(min = 1, max = 6)
  public final ConfigProperty<Integer> lineWidth = new ConfigProperty<>(2);

  /** Distance (in blocks) beyond which overlays are not drawn. */
  @SliderSetting(min = 16, max = 128)
  public final ConfigProperty<Integer> maxRenderDistance = new ConfigProperty<>(64);

  /** Whether area boxes are filled with a translucent colour. */
  @SwitchSetting
  public final ConfigProperty<Boolean> fillAreas = new ConfigProperty<>(true);

  /** Opacity (0-200) of the area fill when {@link #fillAreas} is enabled. */
  @SliderSetting(min = 0, max = 200)
  public final ConfigProperty<Integer> fillOpacity = new ConfigProperty<>(50);

  /** Horizontal position (percent of screen width) of the chest-contents overlay. */
  @SliderSetting(min = 0, max = 100)
  public final ConfigProperty<Integer> chestContentsX = new ConfigProperty<>(2);

  /** Vertical position (percent of screen height) of the chest-contents overlay. */
  @SliderSetting(min = 0, max = 100)
  public final ConfigProperty<Integer> chestContentsY = new ConfigProperty<>(22);
}
