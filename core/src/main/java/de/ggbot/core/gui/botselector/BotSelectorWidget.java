package de.ggbot.core.gui.botselector;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import java.util.List;
import java.util.Objects;

@AutoWidget
@Link("botselector.lss")
public class BotSelectorWidget extends DivWidget {

  private Runnable closeAction;
  private Runnable refreshAction;

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    this.addId("bot-selector-widget");

    ComponentWidget title = ComponentWidget.i18n("ggbot.gui.botselector.title");
    title.addId("bot-selector-title");
    this.addChild(title);

    VerticalListWidget<BotEntryWidget> botList = new VerticalListWidget<>();
    botList.addId("bot-list");

    List<Bot> bots = BotRequests.getCachedBots();
    for (Bot bot : bots) {
      botList.addChild(new BotEntryWidget(bot));
    }

    ScrollWidget scrollWidget = new ScrollWidget(botList);
    scrollWidget.addId("bot-list-scroll");
    this.addChild(scrollWidget);

    if (bots.isEmpty()) {
      ComponentWidget emptyLabel = ComponentWidget.i18n("ggbot.gui.botselector.empty");
      emptyLabel.addId("bot-selector-empty");
      this.addChild(emptyLabel);
    }

    HorizontalListWidget buttonBar = new HorizontalListWidget();
    buttonBar.addId("bot-selector-buttons");

    ButtonWidget refreshButton = ButtonWidget.i18n("ggbot.gui.botselector.refresh");
    refreshButton.addId("bot-selector-refresh-button");
    refreshButton.setPressable(() -> {
      refreshButton.setEnabled(false);
      BotRequests.updateBotListAsync(GGBot.getInstance(), () -> {
        if (refreshAction != null) refreshAction.run();
      });
    });
    buttonBar.addEntry(refreshButton);

    ButtonWidget closeButton = ButtonWidget.i18n("ggbot.gui.botselector.close");
    closeButton.addId("bot-selector-close-button");
    closeButton.setPressable(() -> {
      if (closeAction != null) closeAction.run();
    });
    buttonBar.addEntry(closeButton);

    this.addChild(buttonBar);
  }

  public void setCloseAction(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  public void setRefreshAction(Runnable refreshAction) {
    this.refreshAction = refreshAction;
  }

  static String formatBotDisplay(Bot bot) {
    if (bot.getDescription() != null && !bot.getDescription().isEmpty()) {
      return bot.getDescription() + " (" + bot.getId() + ")";
    }
    if (Objects.equals(bot.getLinkName(), "unknown") || bot.getLinkName().isEmpty()) {
      return bot.getToken().substring(0, 3) + " (" + bot.getId() + ")";
    }
    return bot.getLinkName() + " (" + bot.getId() + ")";
  }
}
