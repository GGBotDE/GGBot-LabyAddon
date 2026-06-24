package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;

public class HealthWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "health";

  private static TextLine healthLine;

  /**
   * Creates the health HUD widget and binds it to the in-game category.
   */
  public HealthWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/golden_apple.png")));
  }

  /**
   * Initializes the text line with its label and a default placeholder value.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    healthLine = createLine(
        Component.translatable("ggbot.widget.health.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed health value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (healthLine != null) healthLine.updateAndFlush(value);
  }
}
