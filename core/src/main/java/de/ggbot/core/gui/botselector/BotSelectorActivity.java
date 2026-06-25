package de.ggbot.core.gui.botselector;

import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.Parent;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.activity.Link;
import net.labymod.api.client.gui.screen.activity.types.SimpleActivity;

@AutoActivity
@Link("botselector.lss")
public class BotSelectorActivity extends SimpleActivity {

  public final BotSelectorWidget selectorWidget;

  public BotSelectorActivity() {
    super();
    this.selectorWidget = new BotSelectorWidget();
    this.selectorWidget.setCloseAction(this::closeScreen);
    this.selectorWidget.setRefreshAction(() ->
        Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
          BotSelectorActivity fresh = new BotSelectorActivity();
          Laby.labyAPI().minecraft().minecraftWindow().displayScreen(fresh);
        })
    );
  }

  @Override
  public void initialize(Parent parent) {
    super.initialize(parent);
    de.ggbot.core.utils.GuiSounds.click();
    this.document().addChild(selectorWidget);
  }
}
