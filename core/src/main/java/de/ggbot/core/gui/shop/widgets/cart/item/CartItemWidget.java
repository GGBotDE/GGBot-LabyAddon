package de.ggbot.core.gui.shop.widgets.cart.item;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;

/** A single cart row widget displaying the item icon, name, count selector and description. */
@AutoWidget
@Link("shopgui.lss")
public class CartItemWidget extends DivWidget {
  private final ShopInterfaceActivity activity;

  /** Top row: icon, quantity selector, and delete button. */
  public final CartItemTopWidget topWidget;

  /** Bottom row: item description label. */
  public final ComponentWidget bottomWidget;

  /** Price charged per single purchase of this item. */
  public final float pricePerPurchase;

  /** Number of items delivered per purchase (stack size). */
  public final long itemCountPerPurchase;

  /** Display name of the item. */
  public final String itemName;

  /** Resolved item texture icon. */
  public final Icon icon;

  /**
   * @param activity           the owning shop activity
   * @param pricePerPurchase   price charged per click
   * @param itemCountPerPurchase items delivered per purchase
   * @param itemName           display name
   * @param icon               item texture icon
   * @param itemId             unique item identifier
   */
  public CartItemWidget(ShopInterfaceActivity activity, float pricePerPurchase, long itemCountPerPurchase, String itemName,
      Icon icon, String itemId) {
    super();
    this.activity = activity;
    this.pricePerPurchase = pricePerPurchase;
    this.itemCountPerPurchase = itemCountPerPurchase;
    this.itemName = itemName;
    this.icon = icon;
    topWidget = new CartItemTopWidget(activity, icon, itemName, itemId);
    bottomWidget = ComponentWidget.text(itemName).addId("cart-item-description");
  }


  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(topWidget);
    this.addChild(bottomWidget);
  }

}
