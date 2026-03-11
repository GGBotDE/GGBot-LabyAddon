package de.ggbot.core;

import de.ggbot.core.api.VersioningHandler;
import de.ggbot.core.api.versioning.VersionCheckRequest;
import de.ggbot.core.api.versioning.VersionCheckResponse;
import de.ggbot.core.api.versioning.VersioningApiClient;
import de.ggbot.core.api.versioning.response.MessageResponse;
import de.ggbot.core.interactions.CheckGGBot;
import de.ggbot.core.listener.ChatListener;
import de.ggbot.core.listener.MovementTest;
import de.ggbot.core.listener.SetupBotLogsChannel;
import de.ggbot.core.listener.StartTimerOnJoin;
import de.ggbot.core.nametag.TeamFetcher;
import de.ggbot.core.nametag.TeamNameTagIcon;
import de.ggbot.core.nametag.TeamNameTagIconBadge;
import de.ggbot.core.widget.ggfeatures.BotMoneyWidget;
import de.ggbot.core.widget.ggfeatures.CitybuildWidget;
import de.ggbot.core.widget.ingame.HealthWidget;
import de.ggbot.core.widget.ggfeatures.PlotWidget;
import de.ggbot.core.widget.ticket.TicketAmountWidget;
import de.ggbot.core.widget.ticket.TicketClosedAmountWidget;
import de.ggbot.core.widget.ticket.TicketOpenAmountWidget;
import de.ggbot.sdk.core.ApiException;
import net.labymod.api.Laby;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.client.entity.player.tag.TagRegistry;
import net.labymod.api.client.gui.hud.binding.category.HudWidgetCategory;
import net.labymod.api.models.addon.annotation.AddonMain;
import de.ggbot.core.cfg.BotConfiguration;
import de.ggbot.core.listener.AuthEvent;
import de.ggbot.core.widget.info.BotNameWidget;
import de.ggbot.core.widget.info.StatusWidget;
import net.labymod.api.models.addon.info.dependency.MavenDependency;

import static de.ggbot.core.api.BotRequests.updateBotList;

@AddonMain
public class GGBot extends LabyAddon<BotConfiguration> {
  private VersioningHandler versioningHandler;
  private HudWidgetCategory ggfeatures;
  private HudWidgetCategory ticket;
  private HudWidgetCategory ingame;
  private HudWidgetCategory info;

  public static String code = "";
  public static boolean isAuth = false;
  public static boolean isExpired = false;
  public static GGBot instance;

  @Override
  protected void enable() {
    instance = this;
    versioningHandler = new VersioningHandler(this);
    this.registerSettingCategory();
    try {
      checkAuth(this);
    } catch (ApiException e) {
      throw new RuntimeException(e);
    }
    registerListeners();
    registerWidgetsCategories();
    registerWidgets();
    createInteractions();
    registerTags();
    this.logger().info("Enabled the Addon");


  }

  @Override
  protected Class<BotConfiguration> configurationClass() {
    return BotConfiguration.class;
  }
  /**
   * Checks the user's local authentication status based on the stored token
   * and its expiration time. Sets the flags isAuth and isExpired accordingly
   * and updates the bot list if the token is valid.
   *
   * Process:
   * 1. No token → not authenticated.
   * 2. Token exists → check expiration time.
   * 3. Expired → not authenticated, isExpired = true.
   * 4. Valid → authenticated and bot list will be refreshed.
   *
   * @param addon Instance of the addon whose configuration is being verified
   * @throws ApiException if an error occurs while updating the bot list
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
  public void createInteractions() {
    labyAPI().interactionMenuRegistry().register(new CheckGGBot());
  }
  public void registerListeners() {
    this.registerListener(new AuthEvent(this));
    this.registerListener(new ChatListener(this));
    this.registerListener(new SetupBotLogsChannel());
    this.registerListener(new StartTimerOnJoin(this));
    this.registerListener(new MovementTest());

  }
  public void registerWidgetsCategories() {
    labyAPI().hudWidgetRegistry().categoryRegistry().register(this.ggfeatures = new HudWidgetCategory("botggfeatures"));
    labyAPI().hudWidgetRegistry().categoryRegistry().register(this.ticket = new HudWidgetCategory("botticket"));
    labyAPI().hudWidgetRegistry().categoryRegistry().register(this.ingame = new HudWidgetCategory("botingame"));
    labyAPI().hudWidgetRegistry().categoryRegistry().register(this.ingame = new HudWidgetCategory("botinfo"));
  }

  public void registerWidgets() {
    labyAPI().hudWidgetRegistry().register(new BotNameWidget());
    labyAPI().hudWidgetRegistry().register(new StatusWidget());
    labyAPI().hudWidgetRegistry().register(new BotMoneyWidget());
    labyAPI().hudWidgetRegistry().register(new HealthWidget());
    labyAPI().hudWidgetRegistry().register(new CitybuildWidget());
    labyAPI().hudWidgetRegistry().register(new PlotWidget());
    labyAPI().hudWidgetRegistry().register(new TicketAmountWidget());
    labyAPI().hudWidgetRegistry().register(new TicketClosedAmountWidget());
    labyAPI().hudWidgetRegistry().register(new TicketOpenAmountWidget());
  }

  public void registerTags() {
    new TeamFetcher().fetch();

    TagRegistry tagRegistry = this.labyAPI().tagRegistry();
    tagRegistry.registerAfter(
        "labymod_role",
        "ggbot_role",
        PositionType.LEFT_TO_NAME,
        new TeamNameTagIcon(() -> 1F)
    );

    Laby.references().badgeRegistry().registerBefore(
        "labymod_role",
        "ggbot_role",
        net.labymod.api.client.entity.player.badge.PositionType.LEFT_TO_NAME,
        new TeamNameTagIconBadge());
  }

  public VersioningHandler getVersioningHandler() {
    return versioningHandler;
  }
}
