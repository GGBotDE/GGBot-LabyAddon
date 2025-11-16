package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Float;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;

public final class AbstractWidgetTranslateYPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, Float, Float> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.translateY();
  }

  @Override
  public Class<?> type() {
    return Float.class;
  }

  @Override
  public Float[] toArray(Object[] objects) {
    Float[] array = new Float[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (Float)objects[i];
    }
    return array;
  }

  @Override
  public Float[] toArray(Collection<Float> collection) {
    return collection.toArray(new Float[0]);
  }
}
