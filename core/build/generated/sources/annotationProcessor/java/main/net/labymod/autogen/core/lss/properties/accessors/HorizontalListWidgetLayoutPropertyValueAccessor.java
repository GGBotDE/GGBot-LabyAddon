package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;

public final class HorizontalListWidgetLayoutPropertyValueAccessor implements PropertyValueAccessor<HorizontalListWidget, HorizontalListWidget.HorizontalListLayout, HorizontalListWidget.HorizontalListLayout> {
  @Override
  public LssProperty getProperty(HorizontalListWidget widget) {
    return widget.layout();
  }

  @Override
  public Class<?> type() {
    return HorizontalListWidget.HorizontalListLayout.class;
  }

  @Override
  public HorizontalListWidget.HorizontalListLayout[] toArray(Object[] objects) {
    HorizontalListWidget.HorizontalListLayout[] array = new HorizontalListWidget.HorizontalListLayout[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (HorizontalListWidget.HorizontalListLayout)objects[i];
    }
    return array;
  }

  @Override
  public HorizontalListWidget.HorizontalListLayout[] toArray(
      Collection<HorizontalListWidget.HorizontalListLayout> collection) {
    return collection.toArray(new HorizontalListWidget.HorizontalListLayout[0]);
  }
}
