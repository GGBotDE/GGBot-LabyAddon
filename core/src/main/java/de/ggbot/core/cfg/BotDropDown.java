package de.ggbot.core.cfg;

import de.ggbot.core.api.BotRequests;
import de.ggbot.core.GGBot;
import de.ggbot.sdk.core.ApiException;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.configuration.settings.SettingInfo;
import net.labymod.api.configuration.settings.accessor.SettingAccessor;
import net.labymod.api.configuration.settings.annotation.SettingElement;
import net.labymod.api.configuration.settings.annotation.SettingFactory;
import net.labymod.api.configuration.settings.annotation.SettingWidget;
import net.labymod.api.configuration.settings.widget.WidgetFactory;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.function.Consumer;

@AutoWidget
@SettingWidget
public class BotDropDown extends HorizontalListWidget {

    private final String customText;
    private final String initialValue;

    private Consumer<String> customUpdateListener;

    public BotDropDown(String customText, String initialValue) {
        this.customText = customText;
        this.initialValue = initialValue;
    }

    public void setCustomUpdateListener(Consumer<String> customUpdateListener) {
        this.customUpdateListener = customUpdateListener;
    }

    @Override
    public void initialize(Parent parent) {
        super.initialize(parent);

        DropdownWidget<String> dropdown = new DropdownWidget<>();
        for (Bot bot : BotRequests.bots){
          if(bot.getDescription() == null || Objects.equals(bot.getDescription(), "")){
            if(bot.getLinkName().equals("unknown") || bot.getLinkName().isEmpty()){
              String token = bot.getToken();
              String display = token.substring(0,3);
              dropdown.add(display + " (" + bot.getId() + ")");
            }else{
              dropdown.add(bot.getLinkName() + " (" + bot.getId() + ")");
            }
          }else{
            dropdown.add(bot.getDescription() + " (" + bot.getId() + ")");
          }
        }
        dropdown.addId("bot-dropdown");

        dropdown.setChangeListener(value -> {
            if (this.customUpdateListener != null) {
                this.customUpdateListener.accept(value);
            }
        });

        if (this.initialValue != null) {
            dropdown.setSelected(this.initialValue);
        }

        this.addEntry(dropdown);

        ButtonWidget button = ButtonWidget.text(this.customText);
        button.setPressListener(() -> {
          try {
            BotRequests.updateBotList(GGBot.getInstance());
          } catch (ApiException e) {
            throw new RuntimeException(e);
          }
          dropdown.clear();
          for (Bot bot : BotRequests.bots){
            if(bot.getDescription() == null || Objects.equals(bot.getDescription(), "")){
              if(bot.getLinkName().equals("unknown") || bot.getLinkName().isEmpty()){
                String token = bot.getToken();
                String display = token.substring(0,3);
                dropdown.add(display + " (" + bot.getId() + ")");
              }else{
                dropdown.add(bot.getLinkName() + " (" + bot.getId() + ")");
              }
            }else{
              dropdown.add(bot.getDescription() + " (" + bot.getId() + ")");
            }
          }
          return true;
        });
        button.addId("custom-button");
        this.addEntry(button);
    }

    @SettingElement(
    )
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BotDropDownMenu {

        String customText() default "Refresh";
    }

    @SettingFactory
    public static class Factory implements WidgetFactory<BotDropDownMenu, BotDropDown> {

        @Override
        public Class<?>[] types() {
            return new Class[]{String.class};
        }

        @Override
        public BotDropDown[] create(Setting setting, BotDropDownMenu annotation, SettingInfo<?> info, SettingAccessor accessor) {
            BotDropDown customWidget = new BotDropDown(
                annotation.customText(),
                accessor.get()
            );

            customWidget.setCustomUpdateListener(accessor::set);

            return new BotDropDown[]{customWidget};
        }
    }
}