package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;

public final class AbstractWidgetBackgroundColorTransitionDurationPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, Long, Long> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.backgroundColorTransitionDuration();
  }

  @Override
  public Class<?> type() {
    return Long.class;
  }

  @Override
  public Long[] toArray(Object[] objects) {
    Long[] array = new Long[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (Long)objects[i];
    }
    return array;
  }

  @Override
  public Long[] toArray(Collection<Long> collection) {
    return collection.toArray(new Long[0]);
  }
}
