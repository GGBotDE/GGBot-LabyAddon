package de.ggbot.core.cfg;

import de.ggbot.core.api.BotRequests;
import de.ggbot.core.GGBot;
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
    /** Set to {@code true} while programmatically repopulating the dropdown
     *  to prevent the change-listener from overwriting the saved selection. */
    private boolean populatingDropdown = false;

    public BotDropDown(String customText, String initialValue) {
        this.customText = customText;
        this.initialValue = initialValue;
    }

    public void setCustomUpdateListener(Consumer<String> customUpdateListener) {
        this.customUpdateListener = customUpdateListener;
    }

    /** Populate {@code dropdown} from the current cached bot list. */
    private void populateDropdown(DropdownWidget<String> dropdown) {
        for (Bot bot : BotRequests.getCachedBots()) {
            if (bot.getDescription() == null || Objects.equals(bot.getDescription(), "")) {
                if (bot.getLinkName().equals("unknown") || bot.getLinkName().isEmpty()) {
                    String display = bot.getToken().substring(0, 3);
                    dropdown.add(display + " (" + bot.getId() + ")");
                } else {
                    dropdown.add(bot.getLinkName() + " (" + bot.getId() + ")");
                }
            } else {
                dropdown.add(bot.getDescription() + " (" + bot.getId() + ")");
            }
        }
    }

    @Override
    public void initialize(Parent parent) {
        super.initialize(parent);

        DropdownWidget<String> dropdown = new DropdownWidget<>();
        populateDropdown(dropdown);
        dropdown.addId("bot-dropdown");

        dropdown.setChangeListener(value -> {
            if (!populatingDropdown && this.customUpdateListener != null) {
                this.customUpdateListener.accept(value);
            }
        });

        if (this.initialValue != null) {
            dropdown.setSelected(this.initialValue);
        }

        this.addEntry(dropdown);

        GGBot addon = GGBot.getInstance();
        ButtonWidget button = ButtonWidget.text(this.customText);
        button.setPressListener(() -> {
            button.setEnabled(false);
            String savedSelection = dropdown.getSelected();
            BotRequests.updateBotListAsync(addon, () -> {
                populatingDropdown = true;
                dropdown.clear();
                populateDropdown(dropdown);
                if (savedSelection != null) {
                    dropdown.setSelected(savedSelection);
                }
                populatingDropdown = false;
                button.setEnabled(true);
            });
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