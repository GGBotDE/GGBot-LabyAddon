package de.ggbot.core.gui.shop.widgets.cart.item;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.resources.ResourceLocation;

@AutoWidget
@Link("shopgui.lss")
public class CartCountSelectionArrowsWidget extends VerticalListWidget<IconWidget> {
  public final IconWidget upArrow = new IconWidget(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/caret-big-up.png"))).addId("cart-count-selection-up-arrow", "cart-count-selection-arrow");
  public final IconWidget downArrow = new IconWidget(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/caret-big-down.png"))).addId("cart-count-selection-down-arrow", "cart-count-selection-arrow");

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(upArrow);
    this.addChild(downArrow);
  }
}
