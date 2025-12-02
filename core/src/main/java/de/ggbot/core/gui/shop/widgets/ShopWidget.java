package de.ggbot.core.gui.shop.widgets;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget;
import de.ggbot.core.gui.shop.widgets.main.MainShopWidget;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;

@AutoWidget
@Link("shopgui.lss")
public class ShopWidget extends AbstractWidget<DivWidget> {
  private final ShopInterfaceActivity activity;
  public final MainShopWidget mainShopWidget;
  public final CartShopWidget cartShopWidget;

  public ShopWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
    this.mainShopWidget = new MainShopWidget(activity);
    this.cartShopWidget = new CartShopWidget(activity);
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addId("shop-widget");
    this.addChild(mainShopWidget);
    this.addChild(cartShopWidget);
  }
}
