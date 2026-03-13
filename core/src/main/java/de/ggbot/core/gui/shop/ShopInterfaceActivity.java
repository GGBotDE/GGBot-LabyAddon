package de.ggbot.core.gui.shop;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.VersioningHandler;
import de.ggbot.core.gui.shop.utils.CustomSellItem;
import de.ggbot.core.gui.shop.utils.NBTParser;
import de.ggbot.core.gui.shop.widgets.ShopWidget;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget.CartItemEntry;
import de.ggbot.sdk.api.BotsApi;
import de.ggbot.sdk.api.ModulesApi;
import de.ggbot.sdk.api.PublicApi;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.BlockPosition;
import de.ggbot.sdk.model.PublicBot;
import de.ggbot.sdk.model.SellItem;
import de.ggbot.sdk.model.SellItemPrice;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@AutoActivity
@Link("shopgui.lss")
public class ShopInterfaceActivity extends SimpleActivity {

  /** The bot username displayed in the shop header. */
  public final String botName;

  /** The server IP used to look up the public bot. */
  public final String serverIp;

  private final VersioningHandler versioningHandler;

  /** Root widget containing the shop UI. */
  public final ShopWidget shopWidget;

  /** Public bot data fetched from the API on initialization. */
  public PublicBot publicBot;

  /** Public API client for fetching bot info. */
  public final PublicApi publicApi = new PublicApi();

  /** Modules API client for fetching sell items. */
  public final ModulesApi modulesApi = new ModulesApi();

  /** Parsed sell items available for purchase. */
  public List<CustomSellItem> customSellItems = new ArrayList<>();

  private final List<Runnable> cancelListeners = new ArrayList<>();
  private final List<Consumer<List<CartItemEntry>>> purchaseListeners = new ArrayList<>();

  /** Registered money balance checkers invoked before purchase confirmation. */
  public final List<Function<Double, Boolean>> moneyCheckListeners = new ArrayList<>();

  /**
   * @param botName           the bot username to display
   * @param serverIp          server IP used for public API lookups
   * @param versioningHandler feature-flag service
   */
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

    this.document().addChild(shopWidget);

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.onCancelButtonClick(() -> {
      for(Runnable listener : cancelListeners)
        listener.run();
    });

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.onPurchaseButtonClick(() -> {
      if(!versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.buy")) return;
      for(Consumer<List<CartItemEntry>> listener : purchaseListeners)
        listener.accept(shopWidget.cartShopWidget.getCartItems());
    });
  }

  @Override
  protected void postInitialize() {
    super.postInitialize();

    shopWidget.cartShopWidget.cartBottomWidget.cartBottomButtonsWidget.purchaseButton.setEnabled(versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.buy"));

    if(!versioningHandler.isFeatureEnabled("de.ggbot.addon.shop.fetchitems")) return;
    try {
      publicApi.setCustomBaseUrl(versioningHandler.getBaseUrlForFeature("de.ggbot.addon.shop.fetchitems"));
      modulesApi.setCustomBaseUrl(versioningHandler.getBaseUrlForFeature("de.ggbot.addon.shop.fetchitems"));
      publicBot = publicApi.getPublicBotByLink(botName, serverIp);

      List<SellItem> fetchedItems = modulesApi.getPublicSellItems(publicBot.getToken());
      for(SellItem item : fetchedItems) {
        for(SellItemPrice price : item.getPrices()) {
          CustomSellItem customSellItem = new CustomSellItem(
              item.getId(),
              item.getName(),
              item.getItemType(),
              item.getNbt(),
              price.getPrice(),
              price.getAmount(),
              item.getChestPosition()
          );
          customSellItems.add(customSellItem);
        }
      }

      shopWidget.mainShopWidget.itemsWidget.refreshItems();
    } catch (ApiException e) {
      GGBot.getInstance().logger().error("Failed to fetch public bot data for bot: " + botName + " on server: " + serverIp, e);
      GGBot.getInstance().getVersioningHandler().reportError(e);
    }
  }

  @Override
  protected void postStyleSheetLoad() {
    super.postStyleSheetLoad();
  }

  /**
   * Registers a listener that is called when the user clicks "Cancel".
   *
   * @param run the cancel callback
   */
  public void onCancel(Runnable run) {
    cancelListeners.add(run);
  }

  /**
   * Registers a listener that receives the cart items when the user confirms a purchase.
   *
   * @param run the purchase callback
   */
  public void onPurchase(Consumer<List<CartItemEntry>> run) {
    purchaseListeners.add(run);
  }

  /**
   * Registers a money-check function. The function receives the total cart value and
   * should return {@code true} if the player can afford it.
   *
   * @param run the money-check function
   */
  public void onMoneyCheck(Function<Double, Boolean> run) {
    moneyCheckListeners.add(run);
  }
}