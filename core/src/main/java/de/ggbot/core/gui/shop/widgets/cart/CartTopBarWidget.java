package de.ggbot.core.gui.shop.widgets.cart;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

@AutoWidget
@Link("shopgui.lss")
public class CartTopBarWidget extends HorizontalListWidget {
  private final ShopInterfaceActivity activity;
  public final ComponentWidget titleWidget;
  public final ComponentWidget itemCountWidget;
  public final ButtonWidget clearCartButton;
  private List<Runnable> clearCartButtonClickListeners = new ArrayList<>();

  public CartTopBarWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
    titleWidget = ComponentWidget.i18n("ggbot.gui.shop.cart").addId("cart-top-bar-title");
    itemCountWidget = ComponentWidget.i18n("ggbot.gui.shop.itemCount", 0).addId("cart-top-bar-item-count");
    clearCartButton = new ButtonWidget();
    clearCartButton.icon().set(Icon.texture(
        ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/trash-x.png")));
    clearCartButton.addId("cart-top-bar-clear-cart-button");
    this.addId("cart-top-bar");
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addEntry(titleWidget);
    this.addEntry(itemCountWidget);
    this.addEntry(clearCartButton);

    clearCartButton.setPressable(() -> {
      for(Runnable listener : clearCartButtonClickListeners)
        listener.run();
    });
  }

  public void onClearCartButtonClick(Runnable listener) {
    clearCartButtonClickListeners.add(listener);
  }
}
