package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.model.Bot;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.entity.Entity;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.render.camera.CameraSetupEvent;
import net.labymod.api.event.client.render.model.entity.player.PlayerModelRenderEvent;
import net.labymod.api.util.math.vector.DoubleVector3;

/**
 * "Control mode": steer the selected bot with your mouse and movement keys while
 * the camera is placed on the bot and the bot's own model is hidden, so you see
 * what the bot sees.
 *
 * <p>How each part is done without mixins / version-specific code:
 * <ul>
 *   <li><b>Camera position</b>: the camera entity's position is set to the bot's
 *       position every tick, which relocates the viewpoint onto the bot. This is the
 *       version-independent way to actually move the view (there is no camera
 *       position setter on the camera itself).</li>
 *   <li><b>Look</b>: the real camera yaw/pitch (driven by your mouse) is read and
 *       sent to the bot as an absolute rotation, so the bot turns to look exactly
 *       where you look. The rotation API sets an absolute yaw/pitch, so the camera's
 *       absolute angles are sent directly (sending deltas was the long-standing pitch
 *       bug - tiny deltas were interpreted as absolute angles).</li>
 *   <li><b>Hiding the bot</b>: the bot's player model render is cancelled so its body
 *       does not block the camera that now sits inside it.</li>
 *   <li><b>Movement</b>: your movement/jump/sneak/sprint keys are suppressed for your
 *       own character and forwarded to the bot instead.</li>
 * </ul>
 *
 * <p>Only starts when the bot is online and within render distance; auto-stops and
 * clears every control state when the bot leaves range or the mode is toggled off.
 */
public final class ControlModeManager {

  private static final ControlModeManager INSTANCE = new ControlModeManager();

  public static ControlModeManager get() {
    return INSTANCE;
  }

  private static final String[] CONTROLS =
      {"forward", "back", "left", "right", "jump", "sneak", "sprint"};

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ggbot-control");
        t.setDaemon(true);
        return t;
      });

  private volatile boolean active = false;
  private Bot bot;
  private String botName;
  private ScheduledFuture<?> task;
  private final Map<String, Boolean> lastStates = new HashMap<>();

  /** Cached action-bar component shown while controlling (rebuilt on each start). */
  private volatile net.labymod.api.client.component.Component actionBar;

  // Latest look angles read from the camera (main thread) and sent by the executor.
  private volatile float targetYaw;
  private volatile float targetPitch;
  private volatile boolean rotationDirty = false;

  private ControlModeManager() {
  }

  public boolean isActive() {
    return active;
  }

  /** Toggles control mode for the given bot. */
  public synchronized void toggle(Bot bot) {
    if (active) {
      stop();
    } else {
      start(bot);
    }
  }

  public synchronized void start(Bot bot) {
    if (active || bot == null) {
      return;
    }
    if (!Boolean.TRUE.equals(bot.getOnline())) {
      notify("ggbot.botmenu.control.offline");
      return;
    }

    String name = bot.getLinkName();
    Optional<Player> entity = findBotEntity(name);
    ClientPlayer self = Laby.labyAPI().minecraft().getClientPlayer();
    // The bot entity is only loaded while it is within render/tracking distance, so
    // its presence is a reliable, version-independent "in range" check.
    if (name == null || entity.isEmpty() || self == null) {
      notify("ggbot.botmenu.control.tooFar");
      return;
    }

    this.bot = bot;
    this.botName = name;
    this.active = true;
    this.lastStates.clear();
    Entity camera = Laby.labyAPI().minecraft().getCameraEntity();
    this.targetYaw = camera != null ? camera.getRotationYaw() : 0f;
    this.targetPitch = camera != null ? camera.getRotationPitch() : 0f;
    this.rotationDirty = false;
    this.actionBar = buildActionBar(name);

    // Prefer the live CBX connection for the control inputs: connect lazily
    // when allowed (with the on-login flag disabled this is where CBX starts)
    // and subscribe to the movement data. Every send in sendStates falls back
    // to the HTTP endpoints while CBX is unavailable, so control works
    // identically whether the connection is up, still connecting or disabled.
    de.ggbot.core.cbx.CbxManager.get().onControlStarted();

    this.task = executor.scheduleAtFixedRate(this::sendStates, 0, 100, TimeUnit.MILLISECONDS);
    notify("ggbot.botmenu.control.started");
  }

  /** Public stop (menu button / hotkey): reports it as a manual disable. */
  public void stop() {
    stop("ggbot.botmenu.control.stopped");
  }

  private synchronized void stop(String reasonKey) {
    if (!active) {
      return;
    }
    active = false;
    actionBar = null;
    if (task != null) {
      task.cancel(false);
      task = null;
    }
    // Drop the movement subscription; the CBX connection itself may stay.
    de.ggbot.core.cbx.CbxManager.get().onControlStopped();

    // Release every movement key so the bot actually stops moving.
    Bot b = this.bot;
    if (b != null) {
      executor.submit(() -> {
        for (String c : CONTROLS) {
          try {
            BotRequests.setControlState(GGBot.getInstance(), b, c, false);
          } catch (Exception ignored) {
            // Best-effort release; nothing actionable if the bot already stopped.
          }
        }
      });
    }
    notify(reasonKey);
  }

  /** Builds the cached "you are controlling X - press <keys> to exit" action bar. */
  private Component buildActionBar(String name) {
    String keys = Key.concat(java.util.Arrays.asList(
        GGBot.getInstance().configuration().botMenuSub.controlKey.get()));
    return Component.translatable(
            "ggbot.botmenu.control.actionbar",
            Component.text(name, NamedTextColor.AQUA),
            Component.text(keys, NamedTextColor.YELLOW))
        .color(NamedTextColor.GRAY);
  }

  // ---- main thread: camera, look capture, range, hiding -------------------

  /**
   * Each tick (main thread): place the camera on the bot, capture the look angles
   * for the bot, and stop if the bot is gone or out of range.
   */
  @Subscribe
  public void onTick(GameTickEvent event) {
    if (!active) {
      return;
    }
    ClientPlayer self = Laby.labyAPI().minecraft().getClientPlayer();
    if (self == null) {
      return;
    }
    Optional<Player> entity = findBotEntity(botName);
    if (entity.isEmpty()) {
      // Bot is no longer loaded, i.e. out of render distance: end control so it does
      // not keep moving while we can no longer see it.
      stop("ggbot.botmenu.control.outOfRange");
      return;
    }
    Player botEntity = entity.get();

    // Capture the mouse-driven look (the view is relocated to the bot in
    // onCameraSetup) and queue it for the bot. Skip while a screen is open so moving
    // the mouse in a menu does not turn the bot.
    Entity camera = Laby.labyAPI().minecraft().getCameraEntity();
    if (camera != null && !screenOpen()) {
      float yaw = camera.getRotationYaw();
      float pitch = camera.getRotationPitch();
      if (yaw != targetYaw || pitch != targetPitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        rotationDirty = true;
      }
    }

    // Legacy versions (<= 1.12): move the camera entity to the bot once per tick and
    // let the game interpolate between ticks (smooth). Do NOT set previousPosition,
    // otherwise that native interpolation is defeated and the view stutters.
    if (isLegacyCamera() && camera != null) {
      camera.position().setX(botEntity.position().getX());
      camera.position().setY(botEntity.position().getY());
      camera.position().setZ(botEntity.position().getZ());
    }

    // Keep the "you are controlling X" hint on screen.
    if (actionBar != null) {
      Laby.labyAPI().minecraft().chatExecutor().displayActionBar(actionBar);
    }
  }

  /**
   * Offsets the rendered view onto the bot without ever moving the player. This
   * translates the camera-setup stack by the difference between the real camera eye
   * and the bot's eye, so the world is drawn as if seen from the bot. Because no
   * entity position is mutated, this cannot teleport or flicker the player (the
   * entity-position approach desynced badly and flung the player on newer versions).
   */
  @Subscribe
  public void onCameraSetup(CameraSetupEvent event) {
    if (!active || event.phase() != Phase.PRE) {
      return;
    }
    Optional<Player> entity = findBotEntity(botName);
    if (entity.isEmpty()) {
      return;
    }
    // Legacy versions (<= 1.12) move the camera entity position in onTick (per tick)
    // and let the game interpolate it smoothly; nothing to do here.
    if (isLegacyCamera()) {
      return;
    }
    Player botEntity = entity.get();

    // Follow the bot's smoothly interpolated render position (lerp between its previous
    // and current tick position), not the raw tick position, otherwise the view jumps
    // ~20 times a second and looks stuttery.
    float partial = Laby.labyAPI().minecraft().getPartialTicks();
    double botX = lerp(botEntity.previousPosition().getX(), botEntity.position().getX(), partial);
    double botFeetY = lerp(botEntity.previousPosition().getY(), botEntity.position().getY(), partial);
    double botZ = lerp(botEntity.previousPosition().getZ(), botEntity.position().getZ(), partial);

    Entity camEntity = Laby.labyAPI().minecraft().getCameraEntity();

    // Modern versions (1.16.5+): translate the render view to the bot (never moves
    // the player, so it cannot teleport/flicker).
    DoubleVector3 camPos = Laby.labyAPI().minecraft().getCamera().renderPosition();
    double camEyeY = camPos.getY();
    if (camEntity != null) {
      // renderPosition() is feet on some versions and eye on others; normalize to eye.
      double feetY = camEntity.position().getY();
      double eyeY = camEntity.eyePosition().getY();
      if (Math.abs(camEyeY - feetY) < Math.abs(camEyeY - eyeY)) {
        camEyeY += (eyeY - feetY);
      }
    }
    double botEyeHeight = botEntity.eyePosition().getY() - botEntity.position().getY();
    double botEyeY = botFeetY + botEyeHeight;

    event.stack().translate(
        (float) (camPos.getX() - botX),
        (float) (camEyeY - botEyeY),
        (float) (camPos.getZ() - botZ));
  }

  /** Whether this Minecraft version (<= 1.12.2) needs the entity-position camera. */
  private boolean isLegacyCamera() {
    int protocol = Laby.labyAPI().minecraft().getProtocolVersion();
    return protocol > 0 && protocol <= 340;
  }

  private static double lerp(double from, double to, double t) {
    return from + (to - from) * t;
  }

  /** Hides the controlled bot's body so it does not obscure the camera inside it. */
  @Subscribe
  public void onPlayerModelRender(PlayerModelRenderEvent event) {
    if (!active || botName == null) {
      return;
    }
    Player rendered = event.player();
    if (rendered != null && botName.equalsIgnoreCase(rendered.getName())) {
      event.setCancelled(true);
    }
  }

  /** Blocks breaking/placing/opening blocks with your own character while controlling. */
  @Subscribe
  public void onInteract(net.labymod.api.event.client.entity.player.ClientPlayerInteractEvent event) {
    if (active) {
      event.setCancelled(true);
    }
  }

  /** Blocks using items on blocks (placing/opening) while controlling. */
  @Subscribe
  public void onUseItemOnBlock(
      net.labymod.api.event.client.entity.player.ClientPlayerUseItemOnBlockEvent event) {
    if (active) {
      event.setCancelled(true);
    }
  }

  /** Suppresses the local character's movement keys while controlling. */
  @Subscribe
  public void onKey(KeyEvent event) {
    if (!active || screenOpen()) {
      return; // Let keys work normally while a screen (chat/menu) is open.
    }
    Key k = event.key();
    if (k.equals(Key.W) || k.equals(Key.A) || k.equals(Key.S) || k.equals(Key.D)
        || k.equals(Key.SPACE) || k.equals(Key.L_SHIFT) || k.equals(Key.L_CONTROL)) {
      // Only suppress presses/holds; let the release (UNPRESSED) through. Otherwise a
      // key held when control started never gets released and the player walks forever.
      if (event.state() != KeyEvent.State.UNPRESSED) {
        event.setCancelled(true);
      }
    }
  }

  // ---- executor thread: network ------------------------------------------

  private void sendStates() {
    if (!active) {
      return;
    }
    GGBot addon = GGBot.getInstance();
    Bot b = this.bot;
    if (b == null) {
      return;
    }
    try {
      // While a screen (chat/menu) is open, do not steer the bot: release every key
      // and skip rotation so typing in chat never moves or turns the bot.
      if (screenOpen()) {
        for (String c : CONTROLS) {
          updateControl(addon, b, c, false);
        }
        return;
      }
      if (rotationDirty) {
        rotationDirty = false;
        // Minecraft yaw accumulates unbounded; wrap to [-180, 180) first.
        float yawDeg = ((targetYaw % 360f) + 540f) % 360f - 180f;
        // The bot's rotate endpoint expects radians, so convert from Minecraft degrees
        // before sending. Both axes are negated because the bot's rotation direction is
        // inverted relative to the client's, and the yaw zero point is offset by 180
        // degrees (PI), so the client's south aligns with the bot's south.
        float yaw = -(float) Math.toRadians(yawDeg) + (float) Math.PI;
        float pitch = -(float) Math.toRadians(targetPitch);
        BotRequests.rotateBot(addon, b, pitch, yaw);
      }
      updateControl(addon, b, "forward", Key.W.isPressed());
      updateControl(addon, b, "back", Key.S.isPressed());
      updateControl(addon, b, "left", Key.A.isPressed());
      updateControl(addon, b, "right", Key.D.isPressed());
      updateControl(addon, b, "jump", Key.SPACE.isPressed());
      updateControl(addon, b, "sneak", Key.L_SHIFT.isPressed());
      updateControl(addon, b, "sprint", Key.L_CONTROL.isPressed());
    } catch (Exception e) {
      addon.logger().error("Control update failed: {}", String.valueOf(e.getMessage()));
    }
  }

  private void updateControl(GGBot addon, Bot b, String control, boolean pressed) throws Exception {
    Boolean prev = lastStates.get(control);
    if (prev != null && prev == pressed) {
      return;
    }
    BotRequests.setControlState(addon, b, control, pressed);
    lastStates.put(control, pressed);
  }

  // ---- helpers ------------------------------------------------------------

  private Optional<Player> findBotEntity(String name) {
    if (name == null || Laby.labyAPI().minecraft().clientWorld() == null) {
      return Optional.empty();
    }
    return Laby.labyAPI().minecraft().clientWorld().getPlayer(name);
  }

  /** Whether a screen (chat or any menu) is currently open. */
  private boolean screenOpen() {
    return Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened();
  }

  private void notify(String key) {
    Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
      var builder = net.labymod.api.notification.Notification.builder()
          .title(net.labymod.api.client.component.Component.translatable(
              "ggbot.botmenu.control.title"))
          .text(net.labymod.api.client.component.Component.translatable(key))
          .type(net.labymod.api.notification.Notification.Type.SYSTEM);
      Laby.labyAPI().notificationController().push(builder.build());
    });
  }
}
