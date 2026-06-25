package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import de.ggbot.core.utils.RenderEngine;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget.DropdownSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.ShowSettingInParent;
import net.labymod.api.configuration.loader.property.ConfigProperty;

/**
 * General addon settings that don't belong to a specific feature sub-config.
 * Changes take effect immediately without requiring a restart.
 */
public class GeneralSubConfig extends Config {
  private transient GGBot addon;

  GeneralSubConfig(GGBot addon) {
    this.addon = addon;
  }

  public GeneralSubConfig() {} // Required for LabyMod's config loader

  /**
   * Whether the "Check GGBot" entry appears in the player middle-click menu.
   * Also shown in the parent settings list for quick access.
   */
  @SwitchSetting
  public final ConfigProperty<Boolean> checkBotEnabled = new ConfigProperty<>(true).addChangeListener((v) -> {
    if(v)
      addon.labyAPI().interactionMenuRegistry().register("de.ggbot.addon.checkGGBotInteraction",
          addon.getCheckGGBotInteraction());
    else
      addon.labyAPI().interactionMenuRegistry().unregister("de.ggbot.addon.checkGGBotInteraction");
  });

  /**
   * Whether to show the GGBot authentication prompt in chat when joining a server
   * without a valid token.
   */
  @SwitchSetting
  public final ConfigProperty<Boolean> joinNotificationEnabled = new ConfigProperty<>(true);

  @SwitchSetting
  public final ConfigProperty<Boolean> botSelectorEnabled = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  public final ConfigProperty<Key[]> botSelectorKey = new ConfigProperty<>(new Key[]{Key.L_CONTROL, Key.B});

  /** Show "Follow Player" in the player middle-click menu (utils module). */
  @SwitchSetting
  public final ConfigProperty<Boolean> followPlayerEnabled = new ConfigProperty<>(true);

  /** Show "Attack Player" in the player middle-click menu (combat module). */
  @SwitchSetting
  public final ConfigProperty<Boolean> attackPlayerEnabled = new ConfigProperty<>(true);

  /** Show the number of online GGBots on the current server in the tab list. */
  @SwitchSetting
  public final ConfigProperty<Boolean> tabListBotCount = new ConfigProperty<>(false);

  /** Horizontal nudge (pixels) for the tab-list bot count, for fine positioning. */
  @SliderSetting(min = -200, max = 200)
  public final ConfigProperty<Integer> tabListCountXOffset = new ConfigProperty<>(0);

  /**
   * Overlay render engine. AUTOMATIC selects the legacy or canvas engine based on the
   * Minecraft version (canvas is required on 1.21.8+); ALWAYS / OFF force the canvas
   * engine on or off.
   */
  @DropdownSetting
  public final ConfigProperty<RenderEngine.Mode> renderMode =
      new ConfigProperty<>(RenderEngine.Mode.AUTOMATIC);

  /**
   * Whether the first-time setup guide has already been shown. Hidden from the UI;
   * persisted only so the guide appears once. Reopen it from the addon settings via
   * the "Setup Guide" button.
   */
  public final ConfigProperty<Boolean> onboardingCompleted =
      new ConfigProperty<>(false).visibilitySupplier(() -> false);
}
