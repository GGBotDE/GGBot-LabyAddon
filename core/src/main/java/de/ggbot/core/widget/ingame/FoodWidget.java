package de.ggbot.core.widget.ingame;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class FoodWidget extends TextHudWidget<TextHudWidgetConfig> {

  /**
   * Identifies the unique registry ID for the food widget.
   */
  public static final String WIDGET_ID = "botfood";

  private static TextLine foodLine;

  /**
   * Initializes the food widget with its configuration and category binding.
   */
  public FoodWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botingame"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/cooked_beef.png")));
  }

  /**
   * Sets up the text line with a label and default placeholder using the provided configuration.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    foodLine = createLine(
        Component.translatable("ggbot.widget.food.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the text line with the new value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (foodLine != null) foodLine.updateAndFlush(value);
  }

  /**
   * Converts the food level into a formatted string for display.
   *
   * @param food the current food level as a double
   * @return a string in the format 'value/20' representing the food level
   */
  public static String format(double food) {
    return (int) food + "/20";
  }
}
