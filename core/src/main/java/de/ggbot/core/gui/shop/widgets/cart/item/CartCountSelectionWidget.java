package de.ggbot.core.gui.shop.widgets.cart.item;

import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import java.util.ArrayList;
import java.util.List;

/**
 * A numeric count field with up/down arrows for choosing an item quantity.
 *
 * <p>Keyboard input is handled through the text field's own update listener (which
 * keeps only digits) rather than by overriding key/char events, because overriding
 * those on the wrapper widget intercepted the field's normal input routing and
 * prevented typing.
 */
public class CartCountSelectionWidget extends AbstractWidget<TextFieldWidget> {
  private final List<Runnable> changeListeners = new ArrayList<>();

  public TextFieldWidget countField;

  /** Guards against re-entrant updates while sanitizing the field text. */
  private boolean sanitizing = false;

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("cart-count-selection-widget");

    countField = new TextFieldWidget();
    countField.addId("cart-count-selection-textfield");
    // Cap the length so a huge number cannot overflow an int and crash the client.
    countField.maximalLength(5);
    this.addChild(countField);
    countField.setText("1");

    // Keep only digits and notify listeners on every change (typed or programmatic).
    countField.updateListener(text -> {
      if (sanitizing) {
        return;
      }
      String digits = text == null ? "" : text.replaceAll("\\D", "");
      if (!digits.equals(text)) {
        sanitizing = true;
        countField.setText(digits);
        sanitizing = false;
      }
      fireChangeListeners();
    });

    CartCountSelectionArrowsWidget arrowsWidget = new CartCountSelectionArrowsWidget();
    countField.addChild(arrowsWidget);

    arrowsWidget.upArrow.setPressable(() -> {
      int count = currentCount();
      count++;
      countField.setText(String.valueOf(count));
      fireChangeListeners();
    });
    arrowsWidget.downArrow.setPressable(() -> {
      int count = currentCount();
      if (count > 1) {
        count--;
        countField.setText(String.valueOf(count));
        fireChangeListeners();
      }
    });
  }

  private int currentCount() {
    String text = countField.getText();
    if (text == null || text.isEmpty()) {
      return 1;
    }
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private void fireChangeListeners() {
    for (Runnable listener : changeListeners) {
      listener.run();
    }
  }

  public void onChanged(Runnable listener) {
    changeListeners.add(listener);
  }
}
