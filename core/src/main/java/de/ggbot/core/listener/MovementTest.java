package de.ggbot.core.listener;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget.CartItemEntry;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

public class MovementTest {
  public boolean isInMove = false;

  @Subscribe
  public void KeyEvent(KeyEvent e){

    // Temporary shop test. Do not use in production!
    // This is just to demonstrate how to open the shop and handle purchases.
    // This will be removed in the future.
    if(e.key().equals(Key.U)) {
      Laby.labyAPI().minecraft().executeNextTick(() -> {
        ShopInterfaceActivity activity = new ShopInterfaceActivity("GGBotDE", "griefergames.net");
        activity.onPurchase((a) -> {
          activity.closeScreen();
          System.out.println("Purchase completed!");
          new Thread(() -> {
            for(CartItemEntry entry : a) {
              for(int i = 0; i < entry.getQuantity(); i++){
                Laby.labyAPI().minecraft().executeNextTick(() -> {
                  Laby.references().chatExecutor().chat("/pay GGBotDE " + entry.getItem().getPrice());

                });
                try {
                  Thread.sleep(3000);
                } catch (InterruptedException ex) {

                }
              }
            }
          }) .start();

        });

        activity.onMoneyCheck((a -> {
          System.out.println("Money check requested!");
          // Here you would normally check the user's balance and return true or false
          return true; // For testing purposes, we just return true
        }));

        activity.onCancel(() -> {
          System.out.println("Shop canceled!");
          activity.closeScreen();
        });

        Laby.labyAPI().minecraft().minecraftWindow().displayScreen(activity);
      });
    }

    if(!isInMove){
      return;
    }
    if(!Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()){
      if(e.state().name().equalsIgnoreCase("PRESS")){
        if(e.key().getTranslationKey().equalsIgnoreCase("W")){
          //TODO: Movement
        }
      }
    }
  }

}