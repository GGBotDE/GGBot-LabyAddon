package de.ggbot.core.gui.botselector;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

@AutoWidget
@Link("botselector.lss")
public class BotEntryWidget extends DivWidget {

  private final Bot bot;

  public BotEntryWidget(Bot bot) {
    this.bot = bot;
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("bot-entry");

    boolean online = Boolean.TRUE.equals(bot.getOnline());
    String linkName = bot.getLinkName();
    boolean hasName = linkName != null && !linkName.isEmpty() && !"unknown".equals(linkName);

    HorizontalListWidget leftSide = new HorizontalListWidget();
    leftSide.addId("bot-entry-left");

    if (hasName) {
      IconWidget headIcon = new IconWidget(Icon.head(linkName));
      headIcon.addId("bot-entry-head");
      leftSide.addEntry(headIcon);
    }

    ComponentWidget nameLabel = ComponentWidget.text(formatName(bot));
    nameLabel.addId("bot-entry-name");
    leftSide.addEntry(nameLabel);

    ComponentWidget statusLabel = ComponentWidget.i18n(
        online ? "ggbot.gui.botselector.online" : "ggbot.gui.botselector.offline");
    statusLabel.addId(online ? "bot-entry-status-online" : "bot-entry-status-offline");
    leftSide.addEntry(statusLabel);

    this.addChild(leftSide);

    HorizontalListWidget rightSide = new HorizontalListWidget();
    rightSide.addId("bot-entry-right");

    ButtonWidget startButton = ButtonWidget.i18n("ggbot.gui.botselector.start");
    startButton.addId("bot-entry-start");
    startButton.setEnabled(!online);
    startButton.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      startButton.setEnabled(false);
      try {
        BotRequests.startBot(GGBot.getInstance(), bot);
      } catch (Exception e) {
        GGBot.getInstance().logger().error("Failed to start bot: " + e.getMessage());
      }
    });
    rightSide.addEntry(startButton);

    ButtonWidget stopButton = ButtonWidget.i18n("ggbot.gui.botselector.stop");
    stopButton.addId("bot-entry-stop");
    stopButton.setEnabled(online);
    stopButton.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      stopButton.setEnabled(false);
      try {
        BotRequests.stopBot(GGBot.getInstance(), bot);
      } catch (Exception e) {
        GGBot.getInstance().logger().error("Failed to stop bot: " + e.getMessage());
      }
    });
    rightSide.addEntry(stopButton);

    ButtonWidget selectButton = ButtonWidget.i18n("ggbot.gui.botselector.select");
    selectButton.addId("bot-entry-select");
    selectButton.setPressable(() -> {
      de.ggbot.core.utils.GuiSounds.click();
      GGBot addon = GGBot.getInstance();
      // Set only the selection (same as the settings dropdown). The token field is the
      // OAuth token and must not be overwritten with the bot token, which previously
      // broke authentication and left the selection inconsistent.
      addon.configuration().botlist.set(BotSelectorWidget.formatBotDisplay(bot));
      net.labymod.api.notification.Notification.Builder builder =
          net.labymod.api.notification.Notification.builder()
              .title(Component.translatable("ggbot.gui.botselector.selected.title"))
              .text(Component.translatable("ggbot.gui.botselector.selected.text",
                  Component.text(formatName(bot))))
              .type(net.labymod.api.notification.Notification.Type.SYSTEM);
      addon.labyAPI().notificationController().push(builder.build());
    });
    rightSide.addEntry(selectButton);

    this.addChild(rightSide);
  }

  private static String formatName(Bot bot) {
    if (bot.getDescription() != null && !bot.getDescription().isEmpty()) {
      return bot.getDescription();
    }
    String linkName = bot.getLinkName();
    if (linkName != null && !linkName.isEmpty() && !"unknown".equals(linkName)) {
      return linkName;
    }
    return "Bot #" + bot.getId();
  }
}
