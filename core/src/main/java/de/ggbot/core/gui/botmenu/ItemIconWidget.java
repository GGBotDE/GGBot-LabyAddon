package de.ggbot.core.gui.botmenu;

import net.labymod.api.client.gui.lss.property.annotation.AutoWidget;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.widget.attributes.bounds.Bounds;
import net.labymod.api.client.gui.screen.widget.widgets.renderer.IconWidget;

/** Renders an {@link ItemIcon} using the dimensions defined by its stylesheet. */
@AutoWidget
public class ItemIconWidget extends IconWidget {

  private final ItemIcon item;

  public ItemIconWidget(ItemIcon item) {
    super(item.icon());
    this.item = item;
  }

  @Override
  public void renderWidget(ScreenContext context) {
    if (item.stack() == null) {
      super.renderWidget(context);
      return;
    }
    Bounds bounds = this.bounds();
    item.submit(context, bounds.getX(), bounds.getY(),
        Math.min(bounds.getWidth(), bounds.getHeight()));
  }
}
