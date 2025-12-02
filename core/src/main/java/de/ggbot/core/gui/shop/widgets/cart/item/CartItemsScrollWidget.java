package de.ggbot.core.gui.shop.widgets.cart.item;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.main.items.MainShopItemsWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.action.ListSession;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollbarWidget;

public class CartItemsScrollWidget extends AbstractWidget<ScrollWidget> {
  private final ShopInterfaceActivity activity;

  public CartItemsScrollWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(new ScrollWidget(new CartItemListWidget(activity), new ListSession<>()));
  }

}
