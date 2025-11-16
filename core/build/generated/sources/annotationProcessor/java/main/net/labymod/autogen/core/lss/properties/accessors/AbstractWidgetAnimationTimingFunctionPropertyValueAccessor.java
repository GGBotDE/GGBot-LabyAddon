package net.labymod.autogen.core.lss.properties.accessors;

import java.lang.Class;
import java.lang.Override;
import java.util.Collection;
import net.labymod.api.client.gui.lss.property.LssProperty;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.attributes.animation.CubicBezier;

public final class AbstractWidgetAnimationTimingFunctionPropertyValueAccessor implements PropertyValueAccessor<AbstractWidget, CubicBezier, CubicBezier> {
  @Override
  public LssProperty getProperty(AbstractWidget widget) {
    return widget.animationTimingFunction();
  }

  @Override
  public Class<?> type() {
    return CubicBezier.class;
  }

  @Override
  public CubicBezier[] toArray(Object[] objects) {
    CubicBezier[] array = new CubicBezier[objects.length];
    for (int i = 0; i < objects.length; i++) {
      array[i] = (CubicBezier)objects[i];
    }
    return array;
  }

  @Override
  public CubicBezier[] toArray(Collection<CubicBezier> collection) {
    return collection.toArray(new CubicBezier[0]);
  }
}
