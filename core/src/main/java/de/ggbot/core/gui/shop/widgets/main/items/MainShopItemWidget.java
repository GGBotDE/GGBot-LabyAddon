package de.ggbot.core.gui.shop.widgets.main.items;

import de.ggbot.core.gui.botmenu.ItemIcon;
import de.ggbot.core.gui.botmenu.ItemIconWidget;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.SimpleWidget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import java.util.ArrayList;
import java.util.List;

@AutoWidget
@Link("shopgui.lss")
public class MainShopItemWidget extends SimpleWidget {
  private final List<Runnable> clickListeners = new ArrayList<>();
  private final String itemName;
  private final ItemIcon itemIcon;
  private final String[] itemLore;
  private final float itemPrice;
  private final long itemCount;

  public MainShopItemWidget(String itemName, ItemIcon itemIcon, String[] itemLore, float itemPrice,
      long itemCount) {
    this.itemName = itemName;
    this.itemIcon = itemIcon;
    this.itemLore = itemLore;
    this.itemPrice = itemPrice;
    this.itemCount = itemCount;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("main-shop-item-widget");

    this.addChild(new ItemIconWidget(itemIcon).addId("main-shop-item-icon"));
    this.addChild(ComponentWidget.text(itemName).addId("main-shop-item-title"));
    VerticalListWidget<ComponentWidget> listWidget = new VerticalListWidget<>();
    listWidget.addId("main-shop-item-lore-container");
    this.addChild(listWidget);
    for (String loreLine : itemLore) {
      listWidget.addChild(ComponentWidget.text(loreLine).addId("main-shop-item-lore-line"));
    }
    this.addChild(new MainShopItemPriceContainerWidget(itemPrice, itemCount)).addId("main-shop-item-price-container");

    this.setPressable(() -> {
      for (Runnable listener : clickListeners)
        listener.run();
    });
  }

  public void onClick(Runnable listener) {
    clickListeners.add(listener);
  }
}
