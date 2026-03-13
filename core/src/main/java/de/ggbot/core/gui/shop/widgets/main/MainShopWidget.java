package de.ggbot.core.gui.shop.widgets.main;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.main.items.MainShopItemsWidget;
import de.ggbot.core.gui.shop.widgets.main.nav.MainShopNavWidget;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.action.ListSession;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;

/** Left panel of the shop displaying the navigation bar and a scrollable item grid. */
@AutoWidget
@Link("shopgui.lss")
public class MainShopWidget extends DivWidget {
  private final ShopInterfaceActivity activity;

  /** Navigation bar with the GGBot logo, shop title and search field. */
  public final MainShopNavWidget navWidget;

  /** Scrollable item grid. */
  public final MainShopItemsWidget itemsWidget;

  /**
   * @param activity the owning shop activity
   */
  public MainShopWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
    this.navWidget = new MainShopNavWidget(activity);
    this.itemsWidget = new MainShopItemsWidget(activity);
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addChild(navWidget);
    this.addChild(new ScrollWidget(itemsWidget, new ListSession<>()).addId("main-shop-items-scroll"));
  }
}
