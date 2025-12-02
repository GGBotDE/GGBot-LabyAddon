package de.ggbot.core.gui.shop.widgets.main.nav;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.key.InputType;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.CheckBoxWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import java.util.List;

@AutoWidget
@Link("shopgui.lss")
public class MainShopNavWidget extends HorizontalListWidget {
  public TextFieldWidget searchField;
  private List<Runnable> searchListeners;
  private final ShopInterfaceActivity activity;

  public MainShopNavWidget(ShopInterfaceActivity activity) {
    super();
    this.activity = activity;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    this.addEntry(new GGBotLogoWidget());
    this.addEntry(ComponentWidget.i18n("ggbot.gui.shop.title", activity.botName).addId("shop-title"));
    searchField = new TextFieldWidget();
    searchField.placeholder(Component.translatable("ggbot.gui.shop.search"));
    searchField.addId("shop-search-field");
    this.addEntry(searchField);
  }

  @Override
  public boolean keyPressed(Key key, InputType type) {
    boolean result = super.keyPressed(key, type);
    if(!isFocused()) return result;

    for(Runnable listener : searchListeners)
      listener.run();

    return result;
  }

  @Override
  public boolean charTyped(Key key, char character) {
    boolean result = super.charTyped(key, character);
    if(!isFocused()) return result;

    for(Runnable listener : searchListeners)
      listener.run();

    return result;
  }

  public void onTyped(Runnable listener) {
    if(searchListeners == null)
      searchListeners = new java.util.ArrayList<>();
    searchListeners.add(listener);
  }
}