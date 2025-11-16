package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.Filter;

public final class AbstractWidgetFilterPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, Filter[], Filter> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.filter();
  }

  @Override
  public Class<?> type() {
    return Filter[].class;
  }

  @Override
  public Filter[] toArray(Object[] objects) {
    Filter[] array = new Filter[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (Filter)objects[i];
    }
    return array;
  }

  @Override
  public Filter[] toArray(Collection<Filter> collection) {
    return collection.toArray(new Filter[0]);
  }
}
