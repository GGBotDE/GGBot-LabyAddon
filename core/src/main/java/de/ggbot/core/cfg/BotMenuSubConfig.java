package de.ggbot.core.cfg;

import de.ggbot.core.gui.botmenu.BotMenuState;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * Settings for the bot management menu, its world overlays, and the WorldEdit-style
 * position selection. The toggle settings are mirrored into {@link BotMenuState} so
 * the menu and the persistent settings stay in sync.
 */
public class BotMenuSubConfig extends Config {

  /** Whether the bot management menu hotkey is active. */
  @SwitchSetting
  public final ConfigProperty<Boolean> menuEnabled = new ConfigProperty<>(true);

  /** Hotkey that opens the bot management menu. */
  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> menuKey = new ConfigProperty<>(new Key[]{Key.L_CONTROL, Key.G});

  /** Highlight kick areas of the selected bot in the world. */
  @SwitchSetting
  public final ConfigProperty<Boolean> highlightKickAreas = new ConfigProperty<>(false)
      .addChangeListener(v -> BotMenuState.get().setHighlightKickAreas(v));

  /** Show the contents of a buy/sell chest when looking at it. */
  @SwitchSetting
  public final ConfigProperty<Boolean> showChestContents = new ConfigProperty<>(false)
      .addChangeListener(v -> BotMenuState.get().setShowChestContents(v));

  /** Key to set WorldEdit position 1 to the block currently looked at. */
  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> pos1Key = new ConfigProperty<>(new Key[]{Key.NUMPAD1});

  /** Key to set WorldEdit position 2 to the block currently looked at. */
  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> pos2Key = new ConfigProperty<>(new Key[]{Key.NUMPAD2});

  /** Toggles control mode (steer the bot with WASD + mouse). */
  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> controlKey = new ConfigProperty<>(new Key[]{Key.L_CONTROL, Key.K});
}
