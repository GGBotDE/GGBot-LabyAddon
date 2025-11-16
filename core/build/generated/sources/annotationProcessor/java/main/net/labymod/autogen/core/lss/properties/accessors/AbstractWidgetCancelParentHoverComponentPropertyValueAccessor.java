package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Boolean;
import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;

public final class AbstractWidgetCancelParentHoverComponentPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, Boolean, Boolean> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.cancelParentHoverComponent();
  }

  @Override
  public Class<?> type() {
    return Boolean.class;
  }

  @Override
  public Boolean[] toArray(Object[] objects) {
    Boolean[] array = new Boolean[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (Boolean)objects[i];
    }
    return array;
  }

  @Override
  public Boolean[] toArray(Collection<Boolean> collection) {
    return collection.toArray(new Boolean[0]);
  }
}
