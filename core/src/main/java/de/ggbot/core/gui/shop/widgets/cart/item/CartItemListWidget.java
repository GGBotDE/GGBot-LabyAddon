package de.ggbot.core.gui.shop.widgets.cart.item;

import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;

@AutoWidget
@Link("shopgui.lss")
public class CartItemListWidget extends VerticalListWidget<CartItemWidget> {

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
  }
}
