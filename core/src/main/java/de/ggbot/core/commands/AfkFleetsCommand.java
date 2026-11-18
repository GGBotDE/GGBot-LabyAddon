package de.ggbot.core.commands;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.AfkOverview;
import de.ggbot.core.api.BotRequests;
import de.ggbot.core.gui.afkfleets.AfkFleetsActivity;
import de.ggbot.sdk.model.AfkBot;
import de.ggbot.sdk.model.AfkFleet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;

/**
 * Handles chat commands for managing AFK fleets. Opens the GUI when invoked
 * without arguments. Otherwise, allows users to list fleets, start or stop
 * individual fleets, and retrieve information about their online bots.
 */
public class AfkFleetsCommand extends Command {

  /**
   * One typed command can reach this class twice in a row. The repeat is consumed
   * without a response on purpose, as the first call already answered.
   */
  private static final long REPEAT_WINDOW_MS = 150L;

  private final GGBot addon;
  private String lastCommand = "";
  private long lastCommandAt = 0L;

  public AfkFleetsCommand(GGBot addon) {
    super("afkfleets", "fleets");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.afkfleets")) {
      return false;
    }
    if (isRepeat(arguments)) {
      return true;
    }
    if (arguments.length == 0) {
      // Delays opening the GUI until after the chat screen has closed.
      Laby.labyAPI().minecraft().executeNextTick(() ->
          Laby.labyAPI().minecraft().minecraftWindow().displayScreen(new AfkFleetsActivity(addon)));
      return true;
    }
    if (!GGBot.isAuthenticated()) {
      addon.displayMessage(Component.translatable("ggbot.afkfleets.login"));
      return true;
    }
    String action = arguments[0].toLowerCase(Locale.ROOT);
    String rest = String.join(" ", Arrays.copyOfRange(arguments, 1, arguments.length));
    switch (action) {
      case "list" -> BotRequests.getAfkOverviewAsync(addon, this::list);
      case "start", "stop", "say" -> BotRequests.getAfkOverviewAsync(addon,
          overview -> run(action, rest, overview));
      default -> addon.displayMessage(Component.translatable("ggbot.afkfleets.command.usage"));
    }
    return true;
  }

  private boolean isRepeat(String[] arguments) {
    String command = String.join(" ", arguments);
    long now = System.currentTimeMillis();
    boolean repeat = command.equals(lastCommand) && now - lastCommandAt < REPEAT_WINDOW_MS;
    lastCommand = command;
    lastCommandAt = now;
    return repeat;
  }

  private void list(AfkOverview overview) {
    if (overview.getFleets().isEmpty()) {
      addon.displayMessage(Component.translatable("ggbot.afkfleets.empty"));
      return;
    }
    for (AfkFleet fleet : overview.getFleets()) {
      List<AfkBot> bots = overview.botsOf(fleet);
      int online = AfkOverview.onlineCount(bots);
      addon.displayMessage(Component.translatable("ggbot.afkfleets.command.entry",
          Component.text(fleet.getName()), Component.text(online), Component.text(bots.size())));
    }
  }

  private void run(String action, String rest, AfkOverview overview) {
    AfkFleet fleet = overview.findByPrefix(rest);
    if (fleet == null) {
      addon.displayMessage(Component.translatable("ggbot.afkfleets.command.notFound",
          Component.text(rest)));
      return;
    }
    Component name = Component.text(fleet.getName());
    switch (action) {
      case "start" -> BotRequests.startAfkFleetAsync(addon, fleet, () -> addon.displayMessage(
          Component.translatable("ggbot.afkfleets.command.started", name)));
      case "stop" -> BotRequests.stopAfkFleetAsync(addon, fleet, () -> addon.displayMessage(
          Component.translatable("ggbot.afkfleets.command.stopped", name)));
      default -> say(fleet, AfkOverview.textAfter(rest, fleet), overview);
    }
  }

  private void say(AfkFleet fleet, String text, AfkOverview overview) {
    if (text.isEmpty()) {
      addon.displayMessage(Component.translatable("ggbot.afkfleets.command.usage"));
      return;
    }
    List<String> tokens = new ArrayList<>();
    for (AfkBot bot : overview.botsOf(fleet)) {
      if (AfkOverview.isOnline(bot)) tokens.add(bot.getToken());
    }
    BotRequests.chatAfkFleetAsync(addon, fleet, tokens, text);
    addon.displayMessage(Component.translatable("ggbot.afkfleets.command.sent",
        Component.text(fleet.getName())));
  }
}
