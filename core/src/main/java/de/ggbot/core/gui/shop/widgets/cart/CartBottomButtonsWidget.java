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

@AutoWidget
@Link("shopgui.lss")
public class CartBottomButtonsWidget extends HorizontalListWidget {
  private List<Runnable> purchaseButtonClickListeners = new ArrayList<>();
  private List<Runnable> cancelButtonClickListeners = new ArrayList<>();
  public ButtonWidget cancelButton;
  public ButtonWidget purchaseButton;

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

  public void onCancelButtonClick(Runnable listener) {
    cancelButtonClickListeners.add(listener);
  }

  public void onPurchaseButtonClick(Runnable listener) {
    purchaseButtonClickListeners.add(listener);
  }
}
