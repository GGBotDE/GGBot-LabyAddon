package net.labymod.autogen.core.lss.properties.resetters;

import java.lang.Override;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;

public class HorizontalListWidgetLssPropertyResetter extends ListWidgetLssPropertyResetter {
  @Override
  public void reset(Widget widget) {
    if (widget instanceof HorizontalListWidget) {
      if (((HorizontalListWidget)widget).spaceLeft() != null) {((HorizontalListWidget)widget).spaceLeft().reset();
      }if (((HorizontalListWidget)widget).spaceRight() != null) {((HorizontalListWidget)widget).spaceRight().reset();
      }if (((HorizontalListWidget)widget).spaceBetweenEntries() != null) {((HorizontalListWidget)widget).spaceBetweenEntries().reset();
      }if (((HorizontalListWidget)widget).layout() != null) {((HorizontalListWidget)widget).layout().reset();
      }}
    super.reset(widget);
  }
}
