package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import de.ggbot.sdk.model.Position;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class PositionWidget extends TextHudWidget<TextHudWidgetConfig> {

  /**
   * Identifies the unique registry ID for the position widget.
   */
  public static final String WIDGET_ID = "botposition";

  private static TextLine positionLine;

  /**
   * Initializes the position widget with its configuration and category binding.
   */
  public PositionWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/compass.png")));
  }

  /**
   * Sets up the text line with a label and default placeholder using the provided configuration.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    positionLine = createLine(
        Component.translatable("ggbot.widget.position.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the text line with the new value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (positionLine != null) positionLine.updateAndFlush(value);
  }

  /**
   * Converts the position coordinates into a formatted string for display.
   *
   * @param position the Position object containing x, y, and z coordinates
   * @return a string in the format 'x y z' representing the block coordinates
   */
  public static String format(Position position) {
    return floor(position.getX()) + " " + floor(position.getY()) + " " + floor(position.getZ());
  }

  private static String floor(Double value) {
    return value == null ? "?" : String.valueOf((long) Math.floor(value));
  }
}
