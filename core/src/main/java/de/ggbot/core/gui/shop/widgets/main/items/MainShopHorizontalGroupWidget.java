package de.ggbot.core.gui.shop.widgets.main.items;

import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;

/** A horizontal row of up to four {@link MainShopItemWidget} tiles in the item grid. */
@AutoWidget
@Link("shopgui.lss")
public class MainShopHorizontalGroupWidget extends HorizontalListWidget {

  /** Item tiles contained in this row (1–4 entries). */
  final MainShopItemWidget[] children;

  /**
   * @param children item tiles to display in this row
   */
  public MainShopHorizontalGroupWidget(MainShopItemWidget[] children) {
    this.children = children;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addId("main-shop-horizontal-group");
    for (MainShopItemWidget child : children) {
      this.addEntry(child);
    }
  }
}
