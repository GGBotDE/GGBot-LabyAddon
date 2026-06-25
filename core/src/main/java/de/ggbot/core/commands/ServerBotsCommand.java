package de.ggbot.core.commands;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.serverbots.ServerBotsActivity;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;

/**
 * Chat command that opens a GUI listing the GGBots currently on the server, each
 * with a button to copy its name to the clipboard. Usable by anyone (no account
 * needed) since it relies only on the public bot list.
 */
public class ServerBotsCommand extends Command {

  private final GGBot addon;

  public ServerBotsCommand(GGBot addon) {
    super("ggbots", "serverbots");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    if (!addon.getVersioningHandler().isFeatureEnabled("de.ggbot.addon.serverbots")) {
      return false;
    }
    if (Laby.labyAPI().serverController().getCurrentServerData() == null) {
      addon.displayMessage(Component.translatable("ggbot.serverbots.notConnected"));
      return true;
    }
    // Open on the next tick so the chat screen has finished closing first; otherwise
    // closing the chat after the command would immediately discard our screen.
    Laby.labyAPI().minecraft().executeNextTick(() ->
        Laby.labyAPI().minecraft().minecraftWindow().displayScreen(new ServerBotsActivity(addon)));
    return true;
  }
}
