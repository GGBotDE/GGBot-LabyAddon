package de.ggbot.core.gui.shop.widgets.cart.item;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;

@AutoWidget
@Link("shopgui.lss")
public class CartItemListWidget extends VerticalListWidget<CartItemWidget> {
  private final ShopInterfaceActivity activity;

  public CartItemListWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
  }
}
