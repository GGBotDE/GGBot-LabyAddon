package de.ggbot.core.gui.shop.widgets.cart.item;

import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.action.ListSession;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;

public class CartItemsScrollWidget extends AbstractWidget<ScrollWidget> {

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addChild(new ScrollWidget(new CartItemListWidget(), new ListSession<>()));
  }
}
