package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.DirtBackgroundType;

public final class AbstractWidgetBackgroundDirtTypePropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, DirtBackgroundType, DirtBackgroundType> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.backgroundDirtType();
  }

  @Override
  public Class<?> type() {
    return DirtBackgroundType.class;
  }

  @Override
  public DirtBackgroundType[] toArray(Object[] objects) {
    DirtBackgroundType[] array = new DirtBackgroundType[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (DirtBackgroundType)objects[i];
    }
    return array;
  }

  @Override
  public DirtBackgroundType[] toArray(Collection<DirtBackgroundType> collection) {
    return collection.toArray(new DirtBackgroundType[0]);
  }
}
