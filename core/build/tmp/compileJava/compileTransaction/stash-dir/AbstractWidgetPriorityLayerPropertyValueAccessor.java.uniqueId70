package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.PriorityLayer;

public final class AbstractWidgetPriorityLayerPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, PriorityLayer, PriorityLayer> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.priorityLayer();
  }

  @Override
  public Class<?> type() {
    return PriorityLayer.class;
  }

  @Override
  public PriorityLayer[] toArray(Object[] objects) {
    PriorityLayer[] array = new PriorityLayer[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (PriorityLayer)objects[i];
    }
    return array;
  }

  @Override
  public PriorityLayer[] toArray(Collection<PriorityLayer> collection) {
    return collection.toArray(new PriorityLayer[0]);
  }
}
