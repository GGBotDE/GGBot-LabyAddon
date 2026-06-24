package de.ggbot.core;

import de.ggbot.core.api.VersioningHandler;
import de.ggbot.core.cfg.BotConfiguration;
import de.ggbot.core.interactions.CheckGGBot;
import de.ggbot.core.listener.AuthEvent;
import de.ggbot.core.listener.ChatListener;
import de.ggbot.core.listener.BotSelectorListener;
import de.ggbot.core.listener.BotMenuListener;
import de.ggbot.core.listener.ShopListener;
import de.ggbot.core.listener.SetupBotLogsChannel;
import de.ggbot.core.listener.StartTimerOnJoin;
import de.ggbot.core.overlay.ChestContentRenderer;
import de.ggbot.core.overlay.OverlayManager;
import de.ggbot.core.overlay.OverlayRenderer;
import de.ggbot.core.nametag.TeamFetcher;
import de.ggbot.core.nametag.TeamNameTagIcon;
import de.ggbot.core.nametag.TeamNameTagIconBadge;
import de.ggbot.core.widget.ggfeatures.BotMoneyWidget;
import de.ggbot.core.widget.ggfeatures.CitybuildWidget;
import de.ggbot.core.widget.ggfeatures.PlotWidget;
import de.ggbot.core.widget.ingame.HealthWidget;
import de.ggbot.core.widget.info.BotNameWidget;
import de.ggbot.core.widget.info.StatusWidget;
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

import static de.ggbot.core.api.BotRequests.updateBotList;

@AddonMain
public class GGBot extends LabyAddon<BotConfiguration> {

  private static GGBot instance;

  private VersioningHandler versioningHandler;
  private StartTimerOnJoin timerListener;

  private HudWidgetCategory ggfeaturesCategory;
  private HudWidgetCategory ticketCategory;
  private HudWidgetCategory ingameCategory;
  private HudWidgetCategory infoCategory;

  private CheckGGBot checkGGBotInteraction;
  private OverlayRenderer overlayRenderer;
  private de.ggbot.core.listener.OnboardingListener onboardingListener;

  /** Whether the user currently holds a valid authentication token. */
  private static boolean authenticated = false;

  /** Whether the previously stored token has expired. */
  private static boolean tokenExpired = false;

  /**
   * Invoked by LabyMod when the addon is loaded. Initializes all subsystems.
   */
  @Override
  protected void enable() {
    instance = this;
    // Make the generated SDK tolerant of backend field additions before any API call.
    de.ggbot.core.api.SdkLeniency.apply();
    configuration().init(this);
    versioningHandler = new VersioningHandler(this);
    this.registerSettingCategory();
    try {
      checkAuth();
    } catch (ApiException e) {
      this.logger().error("Failed to check authentication status: " + e.getMessage());
      versioningHandler.reportError(e);
    }
    registerListeners();
    registerWidgetCategories();
    registerWidgets();
    registerInteractions();
    registerNameTags();
    // Show the setup guide if the addon was (re)loaded while already on a server.
    if (onboardingListener != null) {
      onboardingListener.showIfPendingOnServer();
    }
    this.logger().info("GGBot addon enabled.");
  }

  /**
   * Returns the configuration class used by this addon.
   */
  @Override
  protected Class<BotConfiguration> configurationClass() {
    return BotConfiguration.class;
  }

  /**
   * Verifies the locally stored OAuth token and updates the authentication state.
   *
   * <p>Steps:
   * <ol>
   *   <li>No token &rarr; not authenticated.</li>
   *   <li>Token present &rarr; compare expiry timestamp.</li>
   *   <li>Expired &rarr; {@code authenticated = false}, {@code tokenExpired = true}.</li>
   *   <li>Valid &rarr; {@code authenticated = true}, bot list refreshed.</li>
   * </ol>
   *
   * @throws ApiException if the bot-list refresh fails
   */
  public void checkAuth() throws ApiException {
    String token = configuration().token.get();
    if (token.isEmpty()) {
      authenticated = false;
      return;
    }
    String expiresAtRaw = configuration().expiresAt.get();
    long expiresAt = expiresAtRaw.isEmpty() ? 0L : Long.parseLong(expiresAtRaw);
    if (System.currentTimeMillis() > expiresAt) {
      authenticated = false;
      tokenExpired = true;
    } else {
      authenticated = true;
      updateBotList(this);
    }
  }

  /**
   * Returns the singleton addon instance set during {@link #enable()}.
   *
   * @return the active {@link GGBot} instance
   */
  public static GGBot getInstance() {
    return instance;
  }

  /**
   * Returns whether the user currently holds a valid authentication token.
   *
   * @return {@code true} if authenticated
   */
  public static boolean isAuthenticated() {
    return authenticated;
  }

  /**
   * Returns whether the previously stored token has expired.
   *
   * @return {@code true} if the token is expired
   */
  public static boolean isTokenExpired() {
    return tokenExpired;
  }

  /**
   * Updates the authentication state. Passing {@code true} also clears the
   * token-expired flag.
   *
   * @param state {@code true} if the user is now authenticated
   */
  public static void setAuthenticated(boolean state) {
    authenticated = state;
    if (state) tokenExpired = false;
  }

  /**
   * Marks the current token as expired and clears the authenticated flag.
   */
  public static void markTokenExpired() {
    tokenExpired = true;
    authenticated = false;
  }

  /**
   * Returns the {@link StartTimerOnJoin} instance for external timer management
   * (e.g. restarting timers when configuration changes).
   *
   * @return the timer listener
   */
  public StartTimerOnJoin getTimerListener() {
    return timerListener;
  }

  /**
   * Returns the versioning handler used for feature flags and error reporting.
   *
   * @return the active {@link VersioningHandler}
   */
  public VersioningHandler getVersioningHandler() {
    return versioningHandler;
  }

  /**
   * Returns the {@link CheckGGBot} interaction instance for external state checks
   * (e.g. enabling/disabling the interaction based on authentication).
   *
   * @return the check-GGBot interaction
   */
  public CheckGGBot getCheckGGBotInteraction() {
    return checkGGBotInteraction;
  }

  /**
   * Creates and registers all event listeners.
   */
  private void registerListeners() {
    this.registerListener(versioningHandler);
    this.registerListener(new AuthEvent(this));
    this.registerListener(new ChatListener(this));
    this.registerListener(new SetupBotLogsChannel(this));
    timerListener = new StartTimerOnJoin(this);
    this.registerListener(timerListener);
    this.registerListener(new ShopListener(this));
    this.registerListener(new BotSelectorListener(this));
    this.registerListener(new BotMenuListener(this));
    onboardingListener = new de.ggbot.core.listener.OnboardingListener(this);
    this.registerListener(onboardingListener);
    this.registerListener(de.ggbot.core.gui.botmenu.ControlModeManager.get());
    this.registerCommand(new de.ggbot.core.commands.ServerBotsCommand(this));
    overlayRenderer = new OverlayRenderer(OverlayManager.getInstance());
    this.registerListener(overlayRenderer);
    this.registerListener(new ChestContentRenderer());
    this.registerListener(new de.ggbot.core.overlay.ShopHintRenderer(this));
    this.registerListener(new de.ggbot.core.overlay.TabListBotCountRenderer(this));
  }

  /**
   * Registers HUD widget categories based on enabled feature flags.
   * The global {@code de.ggbot.addon.widgets} flag must be active.
   */
  private void registerWidgetCategories() {
    if (!versioningHandler.isFeatureEnabled("de.ggbot.addon.widgets")) return;

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ggfeatures"))
      labyAPI().hudWidgetRegistry().categoryRegistry().register(
          ggfeaturesCategory = new HudWidgetCategory("botggfeatures"));

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ticket"))
      labyAPI().hudWidgetRegistry().categoryRegistry().register(
          ticketCategory = new HudWidgetCategory("botticket"));

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ingame"))
      labyAPI().hudWidgetRegistry().categoryRegistry().register(
          ingameCategory = new HudWidgetCategory("botingame"));

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.info"))
      labyAPI().hudWidgetRegistry().categoryRegistry().register(
          infoCategory = new HudWidgetCategory("botinfo"));
  }

  /**
   * Registers individual HUD widgets based on their respective feature flags.
   */
  private void registerWidgets() {
    if (!versioningHandler.isFeatureEnabled("de.ggbot.addon.widgets")) return;

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.info")) {
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.info.botname"))
        labyAPI().hudWidgetRegistry().register(new BotNameWidget());
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.info.status"))
        labyAPI().hudWidgetRegistry().register(new StatusWidget());
    }

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ggfeatures")) {
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ggfeatures.money"))
        labyAPI().hudWidgetRegistry().register(new BotMoneyWidget());
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ggfeatures.citybuild"))
        labyAPI().hudWidgetRegistry().register(new CitybuildWidget());
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ggfeatures.plot"))
        labyAPI().hudWidgetRegistry().register(new PlotWidget());
    }

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ingame")) {
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ingame.hearts"))
        labyAPI().hudWidgetRegistry().register(new HealthWidget());
    }

    if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ticket")) {
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ticket.amount"))
        labyAPI().hudWidgetRegistry().register(new TicketAmountWidget());
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ticket.closedamount"))
        labyAPI().hudWidgetRegistry().register(new TicketClosedAmountWidget());
      if (versioningHandler.isFeatureEnabled("de.ggbot.addon.widget.ticket.openamount"))
        labyAPI().hudWidgetRegistry().register(new TicketOpenAmountWidget());
    }
  }

  /**
   * Registers player interaction menu entries.
   */
  private void registerInteractions() {
    checkGGBotInteraction = new CheckGGBot(this);

    // Registered unconditionally; their isVisible() gates display by setting + module.
    labyAPI().interactionMenuRegistry().register("de.ggbot.addon.followPlayerInteraction",
        new de.ggbot.core.interactions.FollowPlayerInteraction(this));
    labyAPI().interactionMenuRegistry().register("de.ggbot.addon.attackPlayerInteraction",
        new de.ggbot.core.interactions.AttackPlayerInteraction(this));

    if(!this.configuration().generalSub.checkBotEnabled.get()) return;
    labyAPI().interactionMenuRegistry().register("de.ggbot.addon.checkGGBotInteraction",checkGGBotInteraction);
  }

  /**
   * Registers nametag icons and tab-list badges for GGBot team members.
   * Triggers an initial team data fetch.
   */
  private void registerNameTags() {
    if (!versioningHandler.isFeatureEnabled("de.ggbot.addon.nametag")) return;
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
}
