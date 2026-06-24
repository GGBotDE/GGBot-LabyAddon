package de.ggbot.core.widget.ggfeatures;

import de.ggbot.core.GGBot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;

public class BotMoneyWidget extends TextHudWidget<TextHudWidgetConfig> {

  /** Registry ID for this widget. */
  public static final String WIDGET_ID = "botmoney";

  private static TextLine botMoneyLine;

  /**
   * Creates the bot-money HUD widget and binds it to the GGFeatures category.
   */
  public BotMoneyWidget() {
    super(WIDGET_ID);
    this.bindCategory(GGBot.getInstance().labyAPI().hudWidgetRegistry().categoryRegistry().getById("botggfeatures"));
    this.setIcon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/dollar-circle.png")));
  }

  /**
   * Initializes the text line with its label and a default placeholder value.
   *
   * @param config the widget configuration
   */
  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    botMoneyLine = createLine(
        Component.translatable("ggbot.widget.money.name"),
        Component.translatable("ggbot.widget.unknown"));
  }

  /**
   * Updates the displayed money value.
   *
   * @param value the new value to display
   */
  public static void update(Object value) {
    if (botMoneyLine != null) botMoneyLine.updateAndFlush(value);
  }
}
