package de.ggbot.core.gui;

import de.ggbot.core.gui.shop.ShopInterfaceActivity;
import de.ggbot.core.gui.shop.widgets.cart.CartShopWidget;
import de.ggbot.core.gui.shop.widgets.main.MainShopWidget;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.action.Pressable;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

@AutoActivity
@Link("messagepopup.lss")
public class MessagePopupActivity extends SimpleActivity {
  private final String message;
  private final String link;

  public MessagePopupActivity(String message, String link) {
    super();
    this.message = message;
    this.link = link;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    MessagePopupWidget messagePopupWidget = new MessagePopupWidget(this);
    this.document().addChild(messagePopupWidget);
  }


  @AutoWidget
  @Link("messagepopup.lss")
  public class MessagePopupWidget extends VerticalListWidget<AbstractWidget> {
    private final MessagePopupActivity activity;
    private IconWidget iconWidget;
    private ComponentWidget titleWidget;
    private ComponentWidget textWidget;
    private ButtonWidget closeButtonWidget;
    private ButtonWidget openButtonWidget;

    public MessagePopupWidget(MessagePopupActivity activity) {
      super();
      this.activity = activity;
    }

    @Override
    public void initialize(Parent parent) {
      super.initialize(parent);
      this.addId("message-widget");

      this.titleWidget = ComponentWidget.i18n("ggbot.messages.system.title", NamedTextColor.GREEN);
      this.iconWidget = new IconWidget(Icon.url("https://www.ggbot.de/assets/img/logo.png")).addId("image"); // GGBot logo as icon (logo updates seasonally, so the url gets used)
      this.textWidget = ComponentWidget.text(activity.message, NamedTextColor.GREEN).addId("message-text");
      this.closeButtonWidget = ButtonWidget.component(Component.translatable("ggbot.messages.system.close")).addId("message-close-button");
      this.openButtonWidget = ButtonWidget.component(Component.translatable("ggbot.messages.system.button")).addId("message-open-button");

      var buttonList = new HorizontalListWidget();
      if(!activity.link.isEmpty()) buttonList.addEntry(openButtonWidget);
      buttonList.addEntry(closeButtonWidget);

      closeButtonWidget.setPressable(activity::closeScreen);
      openButtonWidget.setPressable(() -> {
        activity.closeScreen();
        Laby.references().chatExecutor().openUrl(activity.link);
      });

      this.addChild(iconWidget);
      this.addChild(this.titleWidget);
      this.addChild(this.textWidget);
      this.addChild(buttonList);
    }
  }
}



