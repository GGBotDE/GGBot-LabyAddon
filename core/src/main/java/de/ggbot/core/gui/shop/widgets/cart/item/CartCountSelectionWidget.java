package de.ggbot.core.gui.shop.widgets.cart.item;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.gui.mouse.MutableMouse;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.key.InputType;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import java.util.ArrayList;
import java.util.List;

/**
 * A numeric text field with up/down arrow buttons that controls how many units
 * of a cart item the player wishes to purchase.
 */
public class CartCountSelectionWidget extends AbstractWidget<TextFieldWidget> {
  private final ShopInterfaceActivity activity;
  private final String id;
  private final List<Runnable> changeListeners = new ArrayList<>();

  /** The text field displaying and accepting the current quantity. */
  public TextFieldWidget countField;

  /**
   * @param activity the owning shop activity
   * @param id       the item ID this widget controls
   */
  public CartCountSelectionWidget(ShopInterfaceActivity activity, String id) {
    this.activity = activity;
    this.id = id;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("cart-count-selection-widget");

    countField = new TextFieldWidget();
    countField.addId("cart-count-selection-textfield");
    this.addChild(countField);
    countField.setText("1");
    CartCountSelectionArrowsWidget arrowsWidget = new CartCountSelectionArrowsWidget();
    countField.addChild(arrowsWidget);

    arrowsWidget.upArrow.setPressable(() -> {
      int count = Integer.parseInt(countField.getText().isEmpty() ? "1" : countField.getText());
      count++;
      countField.setText(String.valueOf(count));

      for(Runnable listener : changeListeners)
        listener.run();
    });
    arrowsWidget.downArrow.setPressable(() -> {
      int count = Integer.parseInt(countField.getText().isEmpty() ? "1" : countField.getText());
      if (count > 1) {
        count--;
        countField.setText(String.valueOf(count));

        for(Runnable listener : changeListeners)
          listener.run();
      }
    });
  }

  @Override
  public boolean keyPressed(Key key, InputType type) {
    if(!countField.isFocused()) return super.keyPressed(key, type);
    if (key == Key.BACK || key == Key.DELETE
        || key == Key.ARROW_LEFT || key == Key.ARROW_RIGHT || key == Key.HOME || key == Key.END
        || key.getId() >= Key.NUM0.getId() && key.getId() <= Key.NUM9.getId()
        || key.getId() >= Key.NUMPAD0.getId() && key.getId() <= Key.NUMPAD9.getId()) {
      boolean result = super.keyPressed(key, type);

      for(Runnable listener : changeListeners)
        listener.run();

      return result;
    } else {
      return true;
    }
  }

  @Override
  public boolean charTyped(Key key, char character) {
    if(!isFocused()) return super.charTyped(key, character);
    if (key == Key.BACK || key == Key.DELETE
        || key == Key.ARROW_LEFT || key == Key.ARROW_RIGHT || key == Key.HOME || key == Key.END
        || key.getId() >= Key.NUM0.getId() && key.getId() <= Key.NUM9.getId()
        || key.getId() >= Key.NUMPAD0.getId() && key.getId() <= Key.NUMPAD9.getId()) {
      boolean result = super.charTyped(key, character);

      for(Runnable listener : changeListeners)
        listener.run();

      return result;
    } else {
      return true;
    }
  }

  /**
   * Registers a listener that is called whenever the displayed quantity changes.
   *
   * @param listener the callback
   */
  public void onChanged(Runnable listener) {
    changeListeners.add(listener);
  }
}
