package de.ggbot.core.gui.shop.widgets.cart;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.utils.CustomSellItem;
import de.ggbot.core.gui.shop.widgets.cart.item.CartItemListWidget;
import de.ggbot.core.gui.shop.widgets.cart.item.CartItemWidget;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.action.ListSession;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Cart panel showing queued items, the top bar and checkout controls. */
@AutoWidget
@Link("shopgui.lss")
public class CartShopWidget extends DivWidget {
  private final ShopInterfaceActivity activity;

  /** Top bar with the cart title, item count, and clear button. */
  public final CartTopBarWidget cartTopBar;

  /** Scrollable list of cart item rows. */
  public final CartItemListWidget cartItemListWidget;

  /** Bottom bar with price total and action buttons. */
  public final CartBottomWidget cartBottomWidget;

  private final List<CartItemEntry> cartItems = new ArrayList<>();

  /**
   * @param activity the owning shop activity
   */
  public CartShopWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
    this.cartTopBar = new CartTopBarWidget(activity);
    this.cartItemListWidget = new CartItemListWidget(activity);
    this.cartBottomWidget = new CartBottomWidget(activity);
  }

  @Override
  public void postInitialize() {
    super.postInitialize();
  }

  /**
   * Adds an item to the cart. If the item is already present its quantity is incremented by one.
   *
   * @param item the sell item to add
   */
  public void addCartItem(CustomSellItem item) {
    for(CartItemEntry entry : cartItems) {
      if(entry.getItem().equals(item)) {
        entry.setQuantity(entry.getQuantity() + 1);
        cartBottomWidget.updateTotalPrice();
        updateTopBarItemCount();
        updateEnoughMoney();
        return;
      }
    }
    CartItemEntry newEntry = new CartItemEntry(item, 1);
    cartItems.add(newEntry);
    this.cartItemListWidget.addChildInitialized(newEntry.widget);
    cartBottomWidget.updateTotalPrice();
    updateTopBarItemCount();
    updateEnoughMoney();
  }

  /** @return an unmodifiable view of all current cart entries */
  public List<CartItemEntry> getCartItems() {
    return cartItems;
  }

  /**
   * Removes a specific cart entry.
   *
   * @param entry the entry to remove
   */
  public void removeCartItem(CartItemEntry entry) {
    cartItems.remove(entry);
    this.cartItemListWidget.removeChild(entry.widget);
    cartBottomWidget.updateTotalPrice();
    updateTopBarItemCount();
    updateEnoughMoney();
  }

  /**
   * Removes the cart entry that matches the given sell item.
   *
   * @param item the sell item whose entry should be removed
   */
  public void removeCartItem(CustomSellItem item) {
    CartItemEntry toRemove = null;
    for(CartItemEntry entry : cartItems) {
      if(entry.getItem().equals(item)) {
        toRemove = entry;
        break;
      }
    }
    if(toRemove != null) {
      cartItems.remove(toRemove);
      this.cartItemListWidget.removeChild(toRemove.widget);
      cartBottomWidget.updateTotalPrice();
      updateTopBarItemCount();
      updateEnoughMoney();
    }
  }
  
  /**
   * Removes the cart entry whose item has the given ID.
   *
   * @param id the item ID to remove
   */
  public void removeCartItem(String id) {
    CartItemEntry toRemove = null;
    for(CartItemEntry entry : cartItems) {
      if(entry.getItem().getId().equals(id)) {
        toRemove = entry;
        break;
      }
    }
    if(toRemove != null) {
      cartItems.remove(toRemove);
      this.cartItemListWidget.removeChild(toRemove.widget);
      cartBottomWidget.updateTotalPrice();
      updateTopBarItemCount();
      updateEnoughMoney();
    }
  }

  /** Removes all items from the cart and resets all totals. */
  public void clearCart() {
    for(CartItemEntry entry : new ArrayList<>(cartItems)) {
      this.cartItemListWidget.removeChild(entry.widget);
    }
    cartItems.clear();
    cartBottomWidget.updateTotalPrice();
    updateTopBarItemCount();
    updateEnoughMoney();
  }

  /**
   * Looks up the cart entry for the item with the given ID.
   *
   * @param id the item ID to find
   * @return the matching {@link CartItemEntry}, or {@code null} if not in the cart
   */
  public CartItemEntry getCartItem(String id) {
    for(CartItemEntry entry : cartItems) {
      if(entry.getItem().getId().equals(id)) {
        return entry;
      }
    }
    return null;
  }

  private void updateTopBarItemCount() {
    long totalItems = 0;
    for(CartItemEntry entry : cartItems)
      totalItems += entry.getQuantity()*entry.getItem().getQuantity();
    cartTopBar.itemCountWidget.setComponent(Component.translatable("ggbot.gui.shop.itemCount").argument(Component.text(totalItems+"")));
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(cartTopBar);
    this.addChild(new ScrollWidget(cartItemListWidget, new ListSession<>()).addId("cart-items-scroll"));
    this.addChild(cartBottomWidget);

    this.cartTopBar.onClearCartButtonClick(this::clearCart);
  }

  /**
   * Represents a single line item in the shopping cart, pairing a {@link CustomSellItem}
   * with the chosen purchase quantity and its UI widget.
   */
  public class CartItemEntry {
    private final CustomSellItem item;
    private int quantity;
    private float totalPrice;
    private final CartItemWidget widget;

    public CartItemEntry(CustomSellItem item, int quantity) {
      this.item = item;
      this.quantity = quantity;
      this.totalPrice = item.getPrice() * quantity;
      this.widget = new CartItemWidget(activity, item.getPrice(), item.getQuantity(), item.getName(), item.getIcon(), item.getId());

      this.widget.topWidget.countSelectionWidget.onChanged(() -> {
        if(this.widget.topWidget.countSelectionWidget.countField.getText().isEmpty()) return;
        setQuantity(Integer.parseInt(this.widget.topWidget.countSelectionWidget.countField.getText()));
        cartBottomWidget.updateTotalPrice();
        updateTopBarItemCount();
        updateEnoughMoney();
      });
      this.widget.topWidget.onDeleteButtonClick(() -> {
        removeCartItem(this);
      });
    }

    /** @return the sell item this entry represents */
    public CustomSellItem getItem() {
      return item;
    }

    /** @return the number of purchases the user has selected */
    public int getQuantity() {
      return quantity;
    }

    /** @return price multiplied by quantity (does not account for per-purchase item counts) */
    public float getTotalPrice() {
      return totalPrice;
    }

    /** @return the UI widget rendering this entry in the cart list */
    public CartItemWidget getWidget() {
      return widget;
    }

    /**
     * Updates the purchase quantity and refreshes the total price label.
     *
     * @param quantity the new quantity (must be &ge; 1)
     */
    public void setQuantity(int quantity) {
      this.quantity = quantity;
      this.totalPrice = item.getPrice() * quantity;
      this.widget.topWidget.countSelectionWidget.countField.setText(quantity + "");
      updateEnoughMoney();
    }
  }

  private void updateEnoughMoney() {
    double total = 0;
    for(CartItemEntry item : activity.shopWidget.cartShopWidget.getCartItems()) {
      total += item.getItem().getPrice() * item.getQuantity();
    }
    boolean hasEnoughMoney = true;
    for(Function<Double, Boolean> listener : activity.moneyCheckListeners) {
      if(!listener.apply(total)) {
        hasEnoughMoney = false;
        break;
      }
    }

    cartBottomWidget.cartBottomButtonsWidget.purchaseButton.setEnabled(hasEnoughMoney);
    if(hasEnoughMoney && cartBottomWidget.totalPriceWidget.hasId("not-enough-money")) {
      cartBottomWidget.totalPriceWidget.removeId("not-enough-money");
    } else if(!hasEnoughMoney && !cartBottomWidget.totalPriceWidget.hasId("not-enough-money")) {
      cartBottomWidget.totalPriceWidget.addId("not-enough-money");
    }
  }
}
