package de.ggbot.core.gui.shop.widgets.cart.item;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

@AutoWidget
@Link("shopgui.lss")
public class CartItemTopWidget extends HorizontalListWidget {
  private ShopInterfaceActivity activity;
  private List<Runnable> deleteButtonClickListeners = new ArrayList<>();
  private final IconWidget itemIcon;
  public final CartCountSelectionWidget countSelectionWidget;
  public final ButtonWidget deleteButton;
  public String itemName;

  public CartItemTopWidget(ShopInterfaceActivity activity, Icon itemIcon, String itemName, String itemId) {
    super();
    this.activity = activity;
    this.itemIcon = new IconWidget(itemIcon).addId("cart-item-icon");
    this.countSelectionWidget = new CartCountSelectionWidget(activity, itemId);
    this.deleteButton = new ButtonWidget();
    this.deleteButton.icon().set(Icon.texture(
        ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/trash-x.png")));
    this.deleteButton.addId("cart-item-top-delete-button");

    this.deleteButton.setPressable(() -> {
      for(Runnable listener : deleteButtonClickListeners)
        listener.run();
    });
    this.itemName = itemName;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addEntry(itemIcon);
    this.addEntry(countSelectionWidget);
    this.addEntry(deleteButton);
  }

  public void onDeleteButtonClick(Runnable listener) {
    deleteButtonClickListeners.add(listener);
  }
}
