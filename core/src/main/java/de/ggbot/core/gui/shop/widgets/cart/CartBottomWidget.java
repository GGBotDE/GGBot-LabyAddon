package de.ggbot.core.gui.shop.widgets.cart;

import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget.CartItemEntry;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import java.util.List;
import java.util.function.Supplier;

@AutoWidget
@Link("shopgui.lss")
public class CartBottomWidget extends DivWidget {
  private final Supplier<List<CartItemEntry>> cartItemsSupplier;

  public final CartBottomButtonsWidget cartBottomButtonsWidget;
  public final ComponentWidget totalPriceWidget;

  public CartBottomWidget(Supplier<List<CartItemEntry>> cartItemsSupplier) {
    super();
    this.cartItemsSupplier = cartItemsSupplier;
    this.cartBottomButtonsWidget = new CartBottomButtonsWidget();
    this.totalPriceWidget = ComponentWidget.text("0.00").addId("cart-total-price");
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(cartBottomButtonsWidget);
    this.addChild(totalPriceWidget);
  }

  public void updateTotalPrice() {
    double total = 0;
    for (CartItemEntry item : cartItemsSupplier.get()) {
      total += item.getItem().getPrice() * item.getQuantity();
    }
    // Always two decimals with a dot, regardless of system locale (avoids "0,0").
    this.totalPriceWidget.setText(String.format(java.util.Locale.US, "%.2f", total));
  }
}
