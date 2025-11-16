package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.theme.renderer.ThemeRenderer;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;

public final class AbstractWidgetRendererPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, ThemeRenderer, ThemeRenderer> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.renderer();
  }

  @Override
  public Class<?> type() {
    return ThemeRenderer.class;
  }

  @Override
  public ThemeRenderer[] toArray(Object[] objects) {
    ThemeRenderer[] array = new ThemeRenderer[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (ThemeRenderer)objects[i];
    }
    return array;
  }

  @Override
  public ThemeRenderer[] toArray(Collection<ThemeRenderer> collection) {
    return collection.toArray(new ThemeRenderer[0]);
  }
}
