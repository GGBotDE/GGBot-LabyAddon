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

@AutoWidget
@Link("shopgui.lss")
public class CartShopWidget extends DivWidget {
  private final ShopInterfaceActivity activity;
  public final CartTopBarWidget cartTopBar;
  public final CartItemListWidget cartItemListWidget;
  public final CartBottomWidget cartBottomWidget;
  private List<CartItemEntry> cartItems = new ArrayList<>();

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

  public List<CartItemEntry> getCartItems() {
    return cartItems;
  }

  public void removeCartItem(CartItemEntry entry) {
    cartItems.remove(entry);
    this.cartItemListWidget.removeChild(entry.widget);
    cartBottomWidget.updateTotalPrice();
    updateTopBarItemCount();
    updateEnoughMoney();
  }

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

  public void clearCart() {
    for(CartItemEntry entry : new ArrayList<>(cartItems)) {
      this.cartItemListWidget.removeChild(entry.widget);
    }
    cartItems.clear();
    cartBottomWidget.updateTotalPrice();
    updateTopBarItemCount();
    updateEnoughMoney();
  }

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

    public CustomSellItem getItem() {
      return item;
    }

    public int getQuantity() {
      return quantity;
    }

    public float getTotalPrice() {
      return totalPrice;
    }

    public CartItemWidget getWidget() {
      return widget;
    }

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
