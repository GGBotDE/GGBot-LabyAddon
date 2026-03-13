package de.ggbot.core.gui.shop.widgets.cart;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Horizontal button bar at the bottom of the cart containing Cancel and Purchase buttons. */
@AutoWidget
@Link("shopgui.lss")
public class CartBottomButtonsWidget extends HorizontalListWidget {

  private final List<Runnable> purchaseButtonClickListeners = new ArrayList<>();
  private final List<Runnable> cancelButtonClickListeners = new ArrayList<>();

  /** The cancel button. */
  public ButtonWidget cancelButton;

  /** The purchase/checkout button. */
  public ButtonWidget purchaseButton;

  /** {@inheritDoc} */
  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    cancelButton = new ButtonWidget();
    cancelButton.text().set(Component.translatable("ggbot.gui.shop.cancel"));
    cancelButton.icon().set(Icon.texture(
        ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/x.png")));
    cancelButton.addId("cart-bottom-button");

    purchaseButton = new ButtonWidget();
    purchaseButton.text().set(Component.translatable("ggbot.gui.shop.buy"));
    purchaseButton.icon().set(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/cart.png")));
    purchaseButton.addId("cart-bottom-button");

    this.addEntry(cancelButton);
    this.addEntry(purchaseButton);

    cancelButton.setPressable(() -> {
      for(Runnable listener : cancelButtonClickListeners)
        listener.run();
    });

    purchaseButton.setPressable(() -> {
      for(Runnable listener : purchaseButtonClickListeners)
        listener.run();
    });
  }

  /**
   * Registers a listener called when the cancel button is pressed.
   *
   * @param listener the callback
   */
  public void onCancelButtonClick(Runnable listener) {
    cancelButtonClickListeners.add(listener);
  }

  /**
   * Registers a listener called when the purchase button is pressed.
   *
   * @param listener the callback
   */
  public void onPurchaseButtonClick(Runnable listener) {
    purchaseButtonClickListeners.add(listener);
  }
}
