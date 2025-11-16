package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.BoxSizing;

public final class AbstractWidgetBoxSizingPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, BoxSizing, BoxSizing> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.boxSizing();
  }

  @Override
  public Class<?> type() {
    return BoxSizing.class;
  }

  @Override
  public BoxSizing[] toArray(Object[] objects) {
    BoxSizing[] array = new BoxSizing[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (BoxSizing)objects[i];
    }
    return array;
  }

  @Override
  public BoxSizing[] toArray(Collection<BoxSizing> collection) {
    return collection.toArray(new BoxSizing[0]);
  }
}
