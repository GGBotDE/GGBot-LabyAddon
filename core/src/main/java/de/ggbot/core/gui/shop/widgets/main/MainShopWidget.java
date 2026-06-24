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

@AutoWidget
@Link("shopgui.lss")
public class MainShopWidget extends DivWidget {
  public final MainShopNavWidget navWidget;
  public final MainShopItemsWidget itemsWidget;

  public MainShopWidget(ShopInterfaceActivity activity) {
    super();
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
