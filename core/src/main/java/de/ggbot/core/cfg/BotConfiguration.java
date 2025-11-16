package de.ggbot.core.cfg;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.auth.OAuthServer;
import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
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
import net.labymod.api.notification.Notification.Type;
import net.labymod.api.util.MethodOrder;
import org.openapitools.client.ApiException;
import org.openapitools.client.BotLogEntry;
import java.io.IOException;

@ConfigName("settings")
@SpriteTexture("settings.png")
public class BotConfiguration extends AddonConfig {
  private boolean debug = false;

  @SwitchSetting @SettingSection("Addon")
  @SpriteSlot(x = 6)
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);
  @TextFieldSetting @SettingSection("Bot")
  public final ConfigProperty<String> token = new ConfigProperty<>("").visibilitySupplier(() -> debug);
  @TextFieldSetting
  public final ConfigProperty<String> expiresAt = new ConfigProperty<>("").visibilitySupplier(() -> debug);
  @BotDropDownMenu
  @SpriteSlot(x = 5)
  public final ConfigProperty<String> botlist = new ConfigProperty<>("");



  @MethodOrder(after = "botlist")
  @SpriteSlot(x = 3)
  @ButtonSetting
  public void startSelectedBot(Setting setting) throws ApiException {
    GGBot.checkAuth(GGBot.getInstance());
    if(GGBot.isAuth){
      BotRequests.startBot(GGBot.getInstance());
    }
  }
  @MethodOrder(after = "startSelectedBot")
  @SpriteSlot(x = 4)
  @ButtonSetting
  public void stopSelectedBot(Setting setting) throws ApiException {
    GGBot.checkAuth(GGBot.getInstance());
    if(GGBot.isAuth){
      if(BotRequests.isOnline(GGBot.getInstance())) {
        BotRequests.stopBot(GGBot.getInstance());
      }else{
        Notification.Builder builder = Notification.builder()
            .title(Component.text("INFO", NamedTextColor.GREEN))
            .text(Component.translatable("ggbot.commands.stop.error.offline"))
            .type(Type.SYSTEM);
        Laby.labyAPI().notificationController().push(builder.build());
      }
    }
  }
  @MethodOrder(after = "stopSelectedBot")
  public final BotCommandsSubConfig prefixSub = new BotCommandsSubConfig();
  @MethodOrder(after = "prefixSub")
  public final BotLogsSubConfig botlogSub = new BotLogsSubConfig();

  @MethodOrder(after = "botlogSub") @SettingSection("Authentication")
  @SpriteSlot(x = 1)
  @ButtonSetting
  public void auth(Setting setting) throws IOException {
    if(!GGBot.isAuth) {
      OAuthServer authServer = new OAuthServer(GGBot.getInstance());
      try {
        authServer.listenForCodeAsync((Code) -> authServer.getTokenAsync(Code, (Token) -> {
          GGBot.getInstance().configuration().token.set(Token.get("access_token").getAsString());
          GGBot.code = GGBot.getInstance().configuration().token.get();
          GGBot.getInstance().configuration().expiresAt.set(String.valueOf(
              System.currentTimeMillis() + (Token.get("expires_in").getAsInt() * 1000L)));
          GGBot.isAuth = true;
          GGBot.isExpired = false;
        }));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      Laby.references().chatExecutor().openUrl(authServer.getStringUrl());
    }
  }
  @MethodOrder(after = "auth")
  @SpriteSlot(x = 2)
  @ButtonSetting
  public void reauth(Setting setting) throws IOException {
    OAuthServer authServer = new OAuthServer(GGBot.getInstance());
    try {
      authServer.listenForCodeAsync((Code) -> authServer.getTokenAsync(Code, (Token) -> {
        GGBot.getInstance().configuration().token.set(Token.get("access_token").getAsString());
        GGBot.code = GGBot.getInstance().configuration().token.get();
        GGBot.getInstance().configuration().expiresAt.set(String.valueOf(
            System.currentTimeMillis() + (Token.get("expires_in").getAsInt() * 1000L)));
        GGBot.isAuth = true;
        GGBot.isExpired = false;
      }));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Laby.references().chatExecutor().openUrl(authServer.getStringUrl());
  }

  @MethodOrder(after = "reauth")
  @SpriteSlot()
  @ButtonSetting
  public void discord(Setting setting) throws ApiException {
    Laby.references().chatExecutor().openUrl("https://discord.ggbot.de/");
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }
  public ConfigProperty<String> getToken() {
    return token;
  }
}