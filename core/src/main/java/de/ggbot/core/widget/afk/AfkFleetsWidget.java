package de.ggbot.core.widget.afk;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

public class AfkFleetsWidget extends TextHudWidget<TextHudWidgetConfig> {

  /**
   * Identifies the unique registry ID for the AFK fleets widget.
   */
  public static final String WIDGET_ID = "afkfleets";

  private static TextLine afkFleetsLine;

  /**
   * Initializes the AFK fleets widget with its configuration and category binding.
   */
  public AfkFleetsWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botafk"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/items/name_tag.png")));
  }

  /**
   * Sets up the text line with a label and default placeholder using the provided configuration.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    afkFleetsLine = createLine(
        Component.translatable("ggbot.widget.afk.fleets.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the text line with the new value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (afkFleetsLine != null) afkFleetsLine.updateAndFlush(value);
  }
}
