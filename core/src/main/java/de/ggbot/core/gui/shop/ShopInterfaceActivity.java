package de.ggbot.core.gui.shop;

import de.ggbot.core.api.VersioningHandler;
import de.ggbot.core.gui.shop.utils.CustomSellItem;
import de.ggbot.core.gui.shop.utils.ShopDataCache;
import de.ggbot.core.gui.shop.widgets.ShopWidget;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget.CartItemEntry;
import de.ggbot.sdk.model.PublicBot;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@AutoActivity
@Link("shopgui.lss")
public class ShopInterfaceActivity extends SimpleActivity {

  public final String botName;
  public final String serverIp;
  private final VersioningHandler versioningHandler;
  public final ShopWidget shopWidget;
  public PublicBot publicBot;
  public List<CustomSellItem> customSellItems = new ArrayList<>();

  private final List<Runnable> cancelListeners = new ArrayList<>();
  private final List<Consumer<List<CartItemEntry>>> purchaseListeners = new ArrayList<>();
  public final List<Function<Double, Boolean>> moneyCheckListeners = new ArrayList<>();

  public ShopInterfaceActivity(String botName, String serverIp, VersioningHandler versioningHandler) {
    super();
    this.botName = botName;
    this.serverIp = serverIp;
    this.shopWidget = new ShopWidget(this);
    this.versioningHandler = versioningHandler;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();

    this.document().addChild(shopWidget);

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.onCancelButtonClick(() -> {
      for (Runnable listener : cancelListeners)
        listener.run();
    });

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.onPurchaseButtonClick(() -> {
      if (!versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.buy")) return;
      for (Consumer<List<CartItemEntry>> listener : purchaseListeners)
        listener.accept(shopWidget.cartShopWidget.getCartItems());
    });
  }

  @Override
  protected void postInitialize() {
    super.postInitialize();

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.purchaseButton
        .setEnabled(versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.buy"));

    if (!versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.fetchitems")) return;

    // The shop hint prefetches this data as soon as a bot is nearby, so
    // opening the GUI usually reuses the cached result and loads instantly.
    // Entries older than the GUI validity window are refetched here (prices
    // shown for purchase should be fresh); the hint keeps using its own,
    // longer validity. An in-flight hint fetch is joined, never duplicated.
    ShopDataCache.Entry cached = ShopDataCache.get(botName, serverIp, ShopDataCache.GUI_MAX_AGE_MS);
    if (cached != null) {
      applyShopData(cached);
      return;
    }

    ShopDataCache.fetchAsync(versioningHandler, botName, serverIp, entry -> {
      if (entry == null) return;
      Laby.labyAPI().minecraft().executeOnRenderThread(() -> applyShopData(entry));
    });
  }

  /** Applies fetched shop data and refreshes the item grid. */
  private void applyShopData(ShopDataCache.Entry entry) {
    publicBot = entry.getPublicBot();
    customSellItems = new ArrayList<>(entry.getSellItems());
    shopWidget.mainShopWidget.itemsWidget.refreshItems();
  }

  public void onCancel(Runnable run) {
    cancelListeners.add(run);
  }

  public void onPurchase(Consumer<List<CartItemEntry>> run) {
    purchaseListeners.add(run);
  }

  public void onMoneyCheck(Function<Double, Boolean> run) {
    moneyCheckListeners.add(run);
  }
}
