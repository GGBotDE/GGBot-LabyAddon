package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.api.versioning.ApiException;
import de.ggbot.core.auth.OAuthServer;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.mouse.MouseAction;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import de.ggbot.core.cfg.BotDropDown.BotDropDownMenu;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Builder;
import net.labymod.api.notification.Notification.Type;
import net.labymod.api.util.MethodOrder;
import java.io.IOException;

@ConfigName("settings")
@SpriteTexture("settings.png")
public class BotConfiguration extends AddonConfig {
  private final boolean debug = true;
  private transient GGBot addon;

  /** Required by LabyMod's config loader — use {@link #init(GGBot)} afterwards. */
  public BotConfiguration() {}

  /**
   * Injects the addon reference after LabyMod has deserialized this configuration.
   *
   * @param addon the loaded addon instance
   */
  public void init(GGBot addon) {
    this.addon = addon;
  }

  @SwitchSetting @SettingSection("Addon")
  @SpriteSlot(x = 6)
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);
  @TextFieldSetting @SettingSection("Bot")
  public final ConfigProperty<String> token = new ConfigProperty<>("").visibilitySupplier(() -> debug);
  @TextFieldSetting
  public final ConfigProperty<String> expiresAt = new ConfigProperty<>("").visibilitySupplier(() -> debug);
  @TextFieldSetting
  public final ConfigProperty<String> viewedSystemMessages = new ConfigProperty<>("").visibilitySupplier(() -> debug);
  @BotDropDownMenu
  @SpriteSlot(x = 5)
  public final ConfigProperty<String> botlist = new ConfigProperty<>("");



  @MethodOrder(after = "botlist")
  @SpriteSlot(x = 3)
  @ButtonSetting
  public void startSelectedBot(Setting setting) throws de.ggbot.sdk.core.ApiException {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if (!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.action.startbot")) return;
    addon.checkAuth();
    if (GGBot.isAuthenticated()) {
      BotRequests.updateBotListAsync(addon, () -> {
        try { BotRequests.startBot(addon); }
        catch (Exception e) {
          addon.logger().error("Failed to start bot: " + e.getMessage());
          addon.getVersioningHandler().reportError(e);
        }
      });
    }
  }
  @MethodOrder(after = "startSelectedBot")
  @SpriteSlot(x = 4)
  @ButtonSetting
  public void stopSelectedBot(Setting setting) throws de.ggbot.sdk.core.ApiException {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if (!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.action.stopbot")) return;
    addon.checkAuth();
    if (GGBot.isAuthenticated()) {
      if (BotRequests.isOnlineCached(addon)) {
        BotRequests.updateBotListAsync(addon, () -> {
          try { BotRequests.stopBot(addon); }
          catch (Exception e) {
            addon.logger().error("Failed to stop bot: " + e.getMessage());
            addon.getVersioningHandler().reportError(e);
          }
        });
      } else {
        Builder builder = Notification.builder()
            .title(Component.text("INFO", NamedTextColor.GREEN))
            .text(Component.translatable("ggbot.messages.command.stop.error.offline"))
            .type(Type.SYSTEM);
        Laby.labyAPI().notificationController().push(builder.build());
      }
    }
  }

  @MethodOrder(after = "stopSelectedBot") @SettingSection("Authentication")
  @SpriteSlot(x = 1)
  @ButtonSetting
  public void auth(Setting setting) throws IOException {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if(!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.change.statsminutes")) return;
    if(!GGBot.isAuthenticated()) {
      OAuthServer authServer = new OAuthServer(addon);
      try {
        authServer.listenForCodeAsync((Code) -> authServer.getTokenAsync(Code, (Token) -> {
          addon.configuration().token.set(Token.get("access_token").getAsString());
          addon.configuration().expiresAt.set(String.valueOf(
              System.currentTimeMillis() + (Token.get("expires_in").getAsInt() * 1000L)));
          GGBot.setAuthenticated(true);
        }));
      } catch (Exception e) {
        addon.logger().error("Error during authentication", e);
        addon.getVersioningHandler().reportError(e);
      }
      Laby.references().chatExecutor().openUrl(authServer.getStringUrl());
    }
  }
  @MethodOrder(after = "auth")
  @SpriteSlot(x = 2)
  @ButtonSetting
  public void reauth(Setting setting) throws IOException {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if(!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.action.reauth")) return;
    OAuthServer authServer = new OAuthServer(addon);
    try {
      authServer.listenForCodeAsync((Code) -> authServer.getTokenAsync(Code, (Token) -> {
        addon.configuration().token.set(Token.get("access_token").getAsString());
        addon.configuration().expiresAt.set(String.valueOf(
            System.currentTimeMillis() + (Token.get("expires_in").getAsInt() * 1000L)));
        GGBot.setAuthenticated(true);
      }));
    } catch (Exception e) {
      addon.logger().error("Error during re-authentication", e);
      addon.getVersioningHandler().reportError(e);
    }
    Laby.references().chatExecutor().openUrl(authServer.getStringUrl());
  }

  @MethodOrder(after = "reauth")
  @SpriteSlot()
  @ButtonSetting
  public void discord(Setting setting) throws ApiException {
    this.addon.getVersioningHandler().checkMessagesOnInteraction();
    if(!this.addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.configuration.action.discord")) return;
    Laby.references().chatExecutor().openUrl("https://discord.ggbot.de/");
  }

  @MethodOrder(after = "discord") @SettingSection("Addon")
  public final BotCommandsSubConfig prefixSub = new BotCommandsSubConfig();
  @MethodOrder(after = "prefixSub")
  public final BotLogsSubConfig botlogSub = new BotLogsSubConfig();
  @MethodOrder(after = "botlogSub")
  public final ShopSubConfig shopSub = new ShopSubConfig();
  @MethodOrder(after = "shopSub")
  public final StatusSubConfig statusSub = new StatusSubConfig();
  @MethodOrder(after = "statusSub")
  public final StatsSubConfig statsSub = new StatsSubConfig();
  @MethodOrder(after = "statsSub")
  public final GeneralSubConfig generalSub = new GeneralSubConfig(addon);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }
  public ConfigProperty<String> getToken() {
    return token;
  }
}