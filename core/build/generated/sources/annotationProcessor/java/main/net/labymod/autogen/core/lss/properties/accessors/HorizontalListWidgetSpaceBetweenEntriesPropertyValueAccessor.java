package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;

public final class HorizontalListWidgetSpaceBetweenEntriesPropertyValueAccessor implements PropertyValueAccessor<HorizontalListWidget, Integer, Integer> {
  @Override
  public LssProperty getProperty(HorizontalListWidget widget) {
    return widget.spaceBetweenEntries();
  }

  @Override
  public Class<?> type() {
    return Integer.class;
  }

  @Override
  public Integer[] toArray(Object[] objects) {
    Integer[] array = new Integer[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (Integer)objects[i];
    }
    return array;
  }

  @Override
  public Integer[] toArray(Collection<Integer> collection) {
    return collection.toArray(new Integer[0]);
  }
}
