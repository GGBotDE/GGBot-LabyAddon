package de.ggbot.core.gui.shop.widgets.cart;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget.CartItemEntry;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;

@AutoWidget
@Link("shopgui.lss")
public class CartBottomWidget extends DivWidget {
  private final ShopInterfaceActivity activity;
  public final CartBottomButtonsWidget cartBottomButtonsWidget;
  public final ComponentWidget totalPriceWidget;

  public CartBottomWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
    this.cartBottomButtonsWidget = new CartBottomButtonsWidget();
    this.totalPriceWidget = ComponentWidget.text("0.0").addId("cart-total-price");
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(cartBottomButtonsWidget);
    this.addChild(totalPriceWidget);
  }

  public void updateTotalPrice() {
    double total = 0;
    for(CartItemEntry item : activity.shopWidget.cartShopWidget.getCartItems()) {
      total += item.getItem().getPrice() * item.getQuantity();
    }
    this.totalPriceWidget.setText(String.format("%.2f", total));
  }
}
