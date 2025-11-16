package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.WidgetAlignment;

public final class AbstractWidgetAlignmentYPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, WidgetAlignment, WidgetAlignment> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.alignmentY();
  }

  @Override
  public Class<?> type() {
    return WidgetAlignment.class;
  }

  @Override
  public WidgetAlignment[] toArray(Object[] objects) {
    WidgetAlignment[] array = new WidgetAlignment[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (WidgetAlignment)objects[i];
    }
    return array;
  }

  @Override
  public WidgetAlignment[] toArray(Collection<WidgetAlignment> collection) {
    return collection.toArray(new WidgetAlignment[0]);
  }
}
