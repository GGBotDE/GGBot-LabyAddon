package de.ggbot.core.gui.shop.widgets.main.items;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.utils.CustomSellItem;
import de.ggbot.core.gui.shop.utils.NBTParser.Enchantment;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.util.I18n;
import java.util.ArrayList;
import java.util.List;

@AutoWidget
@Link("shopgui.lss")
public class MainShopItemsWidget extends VerticalListWidget<MainShopHorizontalGroupWidget> {
  private final ShopInterfaceActivity activity;

  public MainShopItemsWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("main-shop-items-widget");
  }

  @Override
  public void postInitialize() {
    super.postInitialize();
    activity.shopWidget.mainShopWidget.navWidget.onTyped(this::refreshItems);
  }

  public void refreshItems() {
    Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
      for (MainShopHorizontalGroupWidget widget : new ArrayList<>(this.getChildren())) {
        this.removeChild(widget);
      }

      String searchTerm = activity.shopWidget.mainShopWidget.navWidget.searchField.getText().toLowerCase();
      List<CustomSellItem> filteredItems = activity.customSellItems;
      if (!searchTerm.isEmpty()) {
        List<CustomSellItem> filtered = new ArrayList<>();
        for (CustomSellItem item : filteredItems) {
          if (item.getName().toLowerCase().contains(searchTerm)
              || item.getType().toLowerCase().contains(searchTerm)) {
            filtered.add(item);
          }
        }
        filteredItems = filtered;
      }
      int itemsPerRow = 4;
      for (int i = 0; i < filteredItems.size(); i += itemsPerRow) {
        int end = Math.min(i + itemsPerRow, filteredItems.size());
        List<CustomSellItem> rowItems = filteredItems.subList(i, end);
        MainShopItemWidget[] itemWidgets = new MainShopItemWidget[rowItems.size()];
        for (int j = 0; j < rowItems.size(); j++) {
          CustomSellItem item = rowItems.get(j);
          List<String> finalLore = new ArrayList<>(item.getLore());
          if (!item.getEnchantments().isEmpty())
            // @LabyMod: The Displayname, lore and so on contains legacy color codes anyway. 
            // We are not able to change that to relyable modern color codes.
            // So we just use them here too. It would make no difference if we would not. 
            // If it breaks the displayname colors break anyway.
            finalLore.add("§e"); 
          for (Enchantment enchantment : item.getEnchantments()) {
            finalLore.add("§7" + enchantment.getDisplayName() + " " + enchantment.getDisplayLevel());
          }
          finalLore.add("§8" + I18n.translate("ggbot.gui.shop.item", item.getType()));
          MainShopItemWidget itemWidget = new MainShopItemWidget(
              item.getName(),
              item.getIcon(),
              finalLore.toArray(new String[0]),
              item.getPrice(),
              item.getQuantity()
          );
          itemWidgets[j] = itemWidget;
          itemWidget.onClick(() -> activity.shopWidget.cartShopWidget.addCartItem(item));
        }
        this.addChildInitialized(new MainShopHorizontalGroupWidget(itemWidgets));
      }
    });
  }
}
