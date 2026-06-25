package de.ggbot.core.gui.shop.widgets.cart.item;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;

@AutoWidget
@Link("shopgui.lss")
public class CartItemWidget extends DivWidget {

  public final CartItemTopWidget topWidget;
  public final ComponentWidget bottomWidget;
  public final float pricePerPurchase;
  public final long itemCountPerPurchase;
  public final String itemName;
  public final Icon icon;

  public CartItemWidget(float pricePerPurchase, long itemCountPerPurchase, String itemName,
      Icon icon, String itemId) {
    super();
    this.pricePerPurchase = pricePerPurchase;
    this.itemCountPerPurchase = itemCountPerPurchase;
    this.itemName = itemName;
    this.icon = icon;
    topWidget = new CartItemTopWidget(icon, itemName, itemId);
    bottomWidget = ComponentWidget.text(itemName).addId("cart-item-description");
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(topWidget);
    this.addChild(bottomWidget);
  }
}
