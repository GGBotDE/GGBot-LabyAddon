package de.ggbot.core.gui.replays;

import de.ggbot.core.replay.ReplayEvents;
import de.ggbot.sdk.model.Bot;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;

/** Texts and links shared by the replay screens. */
final class ReplayText {

  private static final String VIEWER_URL = "https://panel.ggbot.de/modules/module/replay/viewer";
  private static final String[] KINDS = {null, "chat", "command", "player_join", "player_leave",
      "player_in_view", "entity_spawn", "entity_remove", "block_change", "item_pickup",
      "container_open", "bot_death", "bot_kicked", "damage", "explosion", "inventory_change",
      "respawn", "bot_position"};

  private ReplayText() {}

  /** Opens the replay in the panel's viewer, for its bot. */
  static void openInWeb(Bot bot, String replayId) {
    String id = replayId.replaceAll("\\D", "");
    Laby.references().chatExecutor().openUrl(VIEWER_URL + "?id=" + id + "&bot=" + bot.getId());
  }

  /** The name of an event kind. */
  static Component kind(int kind) {
    return kind > 0 && kind < KINDS.length
        ? Component.translatable("ggbot.replays.event." + KINDS[kind])
        : Component.text("#" + kind);
  }

  /** An event in one line: its kind, who, what and where. */
  static Component describe(ReplayEvents.Event event) {
    Component line = kind(event.kind()).copy().color(NamedTextColor.AQUA);
    StringBuilder text = new StringBuilder();
    if (event.actor() != null) text.append(' ').append(event.actor());
    if (event.target() != null) text.append(" > ").append(event.target());
    if (event.text() != null) text.append(' ').append(event.text());
    if (event.count() != null) text.append(" x").append(event.count());

    line = line.append(Component.text(text.toString(), NamedTextColor.WHITE));

    if (event.x() != null) {
      String position = event.x() + " " + event.y() + " " + event.z()
          + (event.dim() != null ? " " + event.dim() : "");
      line = line.append(Component.text("  " + position, NamedTextColor.GRAY));
    }

    return line;
  }

  /** A time in the replay, h:mm:ss or m:ss. */
  static String clock(long ms) {
    long seconds = Math.max(0, ms) / 1000;
    long minutes = seconds / 60;
    if (seconds < 3600) 
      return String.format(Locale.ROOT, "%d:%02d", minutes, seconds % 60);

    return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, minutes % 60, seconds % 60);
  }

  /** A date and time in the user's format. */
  static String date(long ms) {
    return DateFormat
      .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
      .format(new Date(ms));
  }

  /** The number of event kinds. */
  static int kinds() {
    return KINDS.length - 1;
  }
}
