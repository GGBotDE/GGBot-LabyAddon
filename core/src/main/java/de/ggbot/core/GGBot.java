package de.ggbot.core;

import de.ggbot.core.listener.ChatListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;
import de.ggbot.core.cfg.BotConfiguration;
import de.ggbot.core.listener.AuthEvent;
import de.ggbot.core.widget.BotNameWidget;
import de.ggbot.core.widget.StatusWidget;
import org.openapitools.client.ApiException;

import static de.ggbot.core.api.BotRequests.updateBotList;

@AddonMain
public class GGBot extends LabyAddon<BotConfiguration> {
  public static String code = "";
  public static boolean isAuth = false;
  public static boolean isExpired = false;
  public static GGBot instance;

  @Override
  protected void enable() {
    instance = this;
    this.registerSettingCategory();
    try {
      checkAuth(this);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }

    this.registerListener(new AuthEvent(this));
    this.registerListener(new ChatListener(this));
    BotNameWidget botNameWidget = new BotNameWidget();
    StatusWidget statusWidget = new StatusWidget();
    labyAPI().hudWidgetRegistry().register(botNameWidget);
    labyAPI().hudWidgetRegistry().register(statusWidget);

    this.logger().info("Enabled the Addon");

  }

  @Override
  protected Class<BotConfiguration> configurationClass() {
    return BotConfiguration.class;
  }
  /**
   * Prüft den lokalen Auth-Status des Nutzers anhand des gespeicherten Tokens
   * und der Ablaufzeit. Setzt die Flags isAuth und isExpired entsprechend
   * und aktualisiert bei gültigem Token die Botliste.
   * Ablauf:
   * 1. Kein Token → nicht authentifiziert.
   * 2. Token vorhanden → Ablaufzeit prüfen.
   * 3. Abgelaufen → nicht authentifiziert, isExpired = true.
   * 4. Gültig → authentifiziert und Botliste wird aktualisiert.
   *
   * @param addon Instanz des Addons, dessen Konfiguration geprüft wird
   * @throws ApiException falls beim Aktualisieren der Botliste ein Fehler auftritt
   */
  public static void checkAuth(GGBot addon) throws ApiException {
    if (addon.configuration().token.get().isEmpty()) {
      isAuth = false;
    } else {
      if (!addon.configuration().token.get().isEmpty()) {
        long expiresAt = Long.parseLong(addon.configuration().expiresAt.get());
        if (System.currentTimeMillis() > expiresAt) {
          isAuth = false;
          isExpired = true;
        } else {
          isAuth = true;
          updateBotList(addon);
        }
      }
    }
  }


  public static GGBot getInstance() {
    return instance;
  }
}
