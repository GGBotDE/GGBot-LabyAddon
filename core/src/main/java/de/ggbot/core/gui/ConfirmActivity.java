package de.ggbot.core.gui;

import de.ggbot.core.utils.GuiSounds;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;

/** Asks before something the user should know about happens, such as leaving the server. */
@AutoActivity
@Link("messagepopup.lss")
public class ConfirmActivity extends SimpleActivity {

  /** A button of the dialog. */
  public record Choice(String key, Runnable action) {}

  private final Component title;
  private final List<Component> lines;
  private final List<Choice> choices;
  private final Runnable onCancel;

  /**
   * @param title    the heading
   * @param lines    the body text, one component per paragraph
   * @param choices  the buttons aside from Cancel, each dismisses the dialog before running its action
   * @param onCancel runs once Cancel has dismissed the dialog, may be {@code null}
   */
  public ConfirmActivity(Component title, List<Component> lines, List<Choice> choices,
      Runnable onCancel) {
    this.title = title;
    this.lines = lines;
    this.choices = choices;
    this.onCancel = onCancel;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);

    VerticalListWidget<AbstractWidget<?>> dialog = new VerticalListWidget<>();
    dialog.addId("message-widget");
    dialog.addChild(ComponentWidget.component(title.copy().color(NamedTextColor.GREEN)));

    for (Component line : lines) {
      dialog.addChild(ComponentWidget.component(line).addId("message-text"));
    }

    List<ButtonWidget> buttons = new ArrayList<>();

    for (Choice choice : choices) {
      buttons.add(ButtonWidget.i18n(choice.key(), () -> {
        GuiSounds.click();
        closeScreen();
        choice.action().run();
      }));
    }

    buttons.add(ButtonWidget.i18n("ggbot.confirm.cancel", () -> {
      GuiSounds.click();
      closeScreen();
      if (onCancel != null) onCancel.run();
    }));

    if (buttons.size() > 3) {
      buttons.get(0).addId("confirm-stacked-first");
      for (ButtonWidget button : buttons) dialog.addChild(button.addId("confirm-stacked"));
    } else {
      HorizontalListWidget row = new HorizontalListWidget();
      row.addId("confirm-buttons");
      for (ButtonWidget button : buttons) row.addEntry(button);
      dialog.addChild(row);
    }

    this.document().addChild(dialog);
  }
}
