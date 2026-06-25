package de.ggbot.core.gui.botmenu;

import de.ggbot.core.GGBot;
import de.ggbot.core.api.BotRequests;
import de.ggbot.sdk.model.Bot;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;

@AutoActivity
@Link("botmenu.lss")
public class BotMenuActivity extends SimpleActivity {

  public final BotMenuWidget menuWidget;

  public BotMenuActivity() {
    super();
    Bot bot = BotRequests.getBotFromCache(GGBot.getInstance());
    this.menuWidget = new BotMenuWidget(bot);
    this.menuWidget.setCloseAction(this::closeScreen);
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();
    this.document().addChild(menuWidget);
  }
}
