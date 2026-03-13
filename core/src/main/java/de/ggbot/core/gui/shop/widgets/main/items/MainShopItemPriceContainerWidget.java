package de.ggbot.core.gui.shop.widgets.main.items;

import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.resources.ResourceLocation;

/** Displays the price (and optional stack count) badge on a shop item tile. */
@AutoWidget
@Link("shopgui.lss")
public class MainShopItemPriceContainerWidget extends HorizontalListWidget {
  private final float price;
  private final long count;

  /**
   * @param price price per purchase
   * @param count items delivered per purchase (stack size); the count badge is
   *              hidden when this is &le; 1
   */
  public MainShopItemPriceContainerWidget(float price, long count) {
    this.price = price;
    this.count = count;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addEntry(ComponentWidget
        .component(Component.icon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/dollar-circle.png")))
            .append(Component.space())
            .append(Component.text(String.format("%.2f", price))))
        .addId("main-shop-item-price"));

    if (count > 1) {
      this.addEntry(ComponentWidget
          .component(Component.icon(Icon.texture(ResourceLocation.create("ggbot", "themes/vanilla/textures/icons/layers.png")))
              .append(Component.space())
              .append(Component.text(count))).addId("main-shop-item-count"));
    }
  }
}
