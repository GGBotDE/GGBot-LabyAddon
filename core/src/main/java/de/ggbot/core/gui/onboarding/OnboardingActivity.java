package de.ggbot.core.gui.onboarding;

import de.ggbot.core.GGBot;
import net.labymod.api.Laby;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.mouse.MutableMouse;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.MouseButton;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.util.I18n;

/**
 * Multi-step setup guide shown once to new users (and re-openable from the
 * settings). It first asks which kind of user they are - a shop customer who only
 * needs the shop GUI, or a bot owner who uses every feature - and then walks
 * through the relevant setup steps and default hotkeys.
 *
 * <p>Each step is rendered by recreating the activity with the chosen path and step
 * index (the same pattern the bot selector uses for refresh), which keeps the step
 * logic simple and avoids in-place rebuild issues. A cancel button is always
 * present; cancelling or finishing both mark the onboarding as completed so it does
 * not reappear (it can always be reopened from the settings).
 */
@AutoActivity
@Link("onboarding.lss")
public class OnboardingActivity extends SimpleActivity {

  /** Which user group the guide is tailored to. */
  public enum Path {
    NONE, SHOP, OWNER
  }

  private final Path path;
  private final int step;
  private final List<MultiKeybindWidget> keybindWidgets = new ArrayList<>();

  public OnboardingActivity() {
    this(Path.NONE, 0);
  }

  public OnboardingActivity(Path path, int step) {
    this.path = path;
    this.step = step;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();

    VerticalListWidget<Widget> panel = new VerticalListWidget<>();
    panel.addId("onboarding-panel");

    ComponentWidget header = ComponentWidget.i18n("ggbot.onboarding.title");
    header.addId("onboarding-header");
    panel.addChildInitialized(header);

    buildStep(panel);

    this.document().addChild(panel);
  }

  private void buildStep(VerticalListWidget<Widget> panel) {
    if (path == Path.NONE) {
      title(panel, "ggbot.onboarding.welcome.title");
      paragraph(panel, "ggbot.onboarding.welcome.body");
      // Stacked, full-width choices (each with a single icon on the left) read more
      // clearly than two icon buttons side by side.
      panel.addChildInitialized(pathButton("ggbot.onboarding.path.shop", "cart.png",
          () -> go(Path.SHOP, 1)));
      panel.addChildInitialized(pathButton("ggbot.onboarding.path.owner", "layers.png",
          () -> go(Path.OWNER, 1)));

      // Privacy choices, shown to every user (logged in or not) on the very
      // first step so neither path can skip them. Both are opt-out
      // (default on) and can be changed later in the general settings.
      title(panel, "ggbot.onboarding.privacy.title");
      paragraph(panel, "ggbot.onboarding.privacy.body");
      panel.addChildInitialized(privacyToggle("ggbot.onboarding.privacy.errorReports",
          GGBot.getInstance().configuration().generalSub.errorReportingEnabled));
      panel.addChildInitialized(privacyToggle("ggbot.onboarding.privacy.versionReport",
          GGBot.getInstance().configuration().generalSub.versionReportEnabled));

      HorizontalListWidget buttons = buttonRow();
      buttons.addEntry(cancel());
      panel.addChildInitialized(buttons);
      return;
    }

    if (path == Path.SHOP) {
      switch (step) {
        case 1 -> {
          title(panel, "ggbot.onboarding.shop.title");
          paragraph(panel, "ggbot.onboarding.shop.body");
          keybind(panel, "ggbot.settings.shopSub.shopKey.name",
              GGBot.getInstance().configuration().shopSub.shopKey,
              new Key[]{Key.L_CONTROL, Key.O});
          navRow(panel, () -> go(Path.NONE, 0), () -> go(Path.SHOP, 2));
        }
        default -> finishStep(panel);
      }
      return;
    }

    // OWNER
    switch (step) {
      case 1 -> {
        title(panel, "ggbot.onboarding.owner.auth.title");
        paragraph(panel, "ggbot.onboarding.owner.auth.body");
        ButtonWidget auth = ButtonWidget.i18n("ggbot.settings.auth.name", this::runAuth);
        auth.addId("onboarding-btn-primary");
        panel.addChildInitialized(auth);
        navRow(panel, () -> go(Path.NONE, 0), () -> go(Path.OWNER, 2));
      }
      case 2 -> {
        title(panel, "ggbot.onboarding.owner.hotkeys.title");
        paragraph(panel, "ggbot.onboarding.owner.hotkeys.body");
        keybind(panel, "ggbot.settings.generalSub.botSelectorKey.name",
            GGBot.getInstance().configuration().generalSub.botSelectorKey,
            new Key[]{Key.L_CONTROL, Key.B});
        keybind(panel, "ggbot.settings.botMenuSub.menuKey.name",
            GGBot.getInstance().configuration().botMenuSub.menuKey,
            new Key[]{Key.L_CONTROL, Key.G});
        keybind(panel, "ggbot.settings.botMenuSub.controlKey.name",
            GGBot.getInstance().configuration().botMenuSub.controlKey,
            new Key[]{Key.L_CONTROL, Key.K});
        navRow(panel, () -> go(Path.OWNER, 1), () -> go(Path.OWNER, 3));
      }
      default -> finishStep(panel);
    }
  }

  /**
   * A full-width on/off toggle bound to a boolean config property. Clicking it
   * flips the property and rebuilds the step (the same recreate pattern every
   * other step change uses), so the label always reflects the stored value.
   */
  private ButtonWidget privacyToggle(String labelKey, ConfigProperty<Boolean> property) {
    boolean enabled = Boolean.TRUE.equals(property.get());
    String state = I18n.getTranslation(enabled
        ? "ggbot.onboarding.privacy.on" : "ggbot.onboarding.privacy.off");
    ButtonWidget toggle = ButtonWidget.text(I18n.getTranslation(labelKey) + ": " + state);
    toggle.setPressListener(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      property.set(!Boolean.TRUE.equals(property.get()));
      go(path, step);
      return true;
    });
    toggle.addId(enabled ? "onboarding-toggle-on" : "onboarding-toggle-off");
    return toggle;
  }

  /**
   * Adds a labelled, editable hotkey field (bound to a config property) with a reset
   * button. Clicking the field starts capturing; clicking anywhere else commits and
   * stops capturing (handled in {@link #mouseClicked}).
   */
  private void keybind(VerticalListWidget<Widget> panel, String labelKey,
      ConfigProperty<Key[]> property, Key[] defaults) {
    ComponentWidget label = ComponentWidget.i18n(labelKey);
    label.addId("onboarding-line");
    panel.addChildInitialized(label);

    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("onboarding-keybind-row");

    MultiKeybindWidget field = new MultiKeybindWidget(property::set);
    field.addId("onboarding-keybind");
    if (property.get() != null) {
      field.setKeys(new LinkedHashSet<>(Arrays.asList(property.get())));
    }
    keybindWidgets.add(field);
    row.addEntry(field);

    ButtonWidget reset = ButtonWidget.i18n("ggbot.onboarding.reset", () -> {
      property.set(defaults);
      field.setKeys(new LinkedHashSet<>(Arrays.asList(defaults)));
    });
    reset.addId("onboarding-keybind-reset");
    row.addEntry(reset);

    panel.addChildInitialized(row);
  }

  @Override
  public boolean mouseClicked(MutableMouse mouse, MouseButton mouseButton) {
    boolean result = super.mouseClicked(mouse, mouseButton);
    // Clicking outside a hotkey field commits and stops its capture.
    for (MultiKeybindWidget field : keybindWidgets) {
      if (!field.isHovered()) {
        field.unfocus();
      }
    }
    return result;
  }

  /** Runs the GGBot account OAuth flow (same as the settings "Auth" button). */
  private void runAuth() {
    GGBot addon = GGBot.getInstance();
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.change.auth")) {
      return;
    }
    try {
      de.ggbot.core.auth.OAuthServer authServer = new de.ggbot.core.auth.OAuthServer(addon);
      authServer.listenForCodeAsync(code -> authServer.getTokenAsync(code, token -> {
        addon.configuration().token.set(token.get("access_token").getAsString());
        addon.configuration().expiresAt.set(String.valueOf(
            System.currentTimeMillis() + (token.get("expires_in").getAsInt() * 1000L)));
        GGBot.setAuthenticated(true);
      }));
      Laby.references().chatExecutor().openUrl(authServer.getStringUrl());
    } catch (Exception e) {
      addon.logger().error("Onboarding auth failed: " + e.getMessage());
      addon.getVersioningHandler().reportError(e);
    }
  }

  /** Renders the shared final step with a Back and a Finish button. */
  private void finishStep(VerticalListWidget<Widget> panel) {
    title(panel, "ggbot.onboarding.finish.title");
    paragraph(panel, "ggbot.onboarding.finish.body");
    HorizontalListWidget buttons = buttonRow();
    buttons.addEntry(secondary("ggbot.onboarding.back",
        () -> go(path, path == Path.SHOP ? 1 : 2)));
    buttons.addEntry(primary("ggbot.onboarding.finish.button", this::complete));
    buttons.addEntry(cancel());
    panel.addChildInitialized(buttons);
  }

  /** Adds a Back / Next / Cancel button row. */
  private void navRow(VerticalListWidget<Widget> panel, Runnable back, Runnable next) {
    HorizontalListWidget buttons = buttonRow();
    buttons.addEntry(secondary("ggbot.onboarding.back", back));
    buttons.addEntry(primary("ggbot.onboarding.next", next));
    buttons.addEntry(cancel());
    panel.addChildInitialized(buttons);
  }

  private void title(VerticalListWidget<Widget> panel, String key) {
    ComponentWidget w = ComponentWidget.i18n(key);
    w.addId("onboarding-step-title");
    panel.addChildInitialized(w);
  }

  /** Splits a multi-line translation into one line widget each (reliable wrapping). */
  private void paragraph(VerticalListWidget<Widget> panel, String key) {
    String text = I18n.getTranslation(key);
    for (String line : text.split("\n")) {
      ComponentWidget w = ComponentWidget.text(line);
      w.addId("onboarding-line");
      panel.addChildInitialized(w);
    }
  }

  private HorizontalListWidget buttonRow() {
    HorizontalListWidget row = new HorizontalListWidget();
    row.addId("onboarding-buttons");
    return row;
  }

  private ButtonWidget primary(String key, Runnable action) {
    ButtonWidget b = ButtonWidget.i18n(key, withClick(action));
    b.addId("onboarding-btn-primary");
    return b;
  }

  /** A full-width choice button with a single icon on the left. */
  private ButtonWidget pathButton(String key, String iconFile, Runnable action) {
    Icon icon = Icon.texture(ResourceLocation.create(
        "ggbot", "themes/vanilla/textures/icons/" + iconFile));
    ButtonWidget b = ButtonWidget.i18n(key, icon, withClick(action));
    b.addId("onboarding-path-btn");
    return b;
  }

  private ButtonWidget secondary(String key, Runnable action) {
    ButtonWidget b = ButtonWidget.i18n(key, withClick(action));
    b.addId("onboarding-btn");
    return b;
  }

  private ButtonWidget cancel() {
    ButtonWidget b = ButtonWidget.i18n("ggbot.onboarding.cancel", withClick(this::complete));
    b.addId("onboarding-btn-cancel");
    return b;
  }

  /** Wraps a button action so it plays the UI click sound first. */
  private net.labymod.api.client.gui.screen.widget.action.Pressable withClick(Runnable action) {
    return () -> {
      de.ggbot.core.utils.GuiSounds.click();
      action.run();
    };
  }

  private void go(Path newPath, int newStep) {
    Laby.labyAPI().minecraft().minecraftWindow()
        .displayScreen(new OnboardingActivity(newPath, newStep));
  }

  /** Marks onboarding done (so it does not reappear) and closes the guide. */
  private void complete() {
    GGBot.getInstance().configuration().generalSub.onboardingCompleted.set(true);
    closeScreen();
  }
}
