package net.labymod.autogen.core.lss.properties.direct;

import java.lang.Override;
import java.lang.String;
import net.labymod.api.client.gui.lss.property.DirectPropertyValueAccessor;
import net.labymod.api.client.gui.lss.property.LssPropertyResetter;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.resetters.StyledWidgetLssPropertyResetter;

public class StyledWidgetDirectPropertyValueAccessor implements DirectPropertyValueAccessor {
  LssPropertyResetter StyledWidgetResetter = new StyledWidgetLssPropertyResetter();

  @Override
  public PropertyValueAccessor<?, ?, ?> getPropertyValueAccessor(String key) {
    return null;
  }

  @Override
  public boolean hasPropertyValueAccessor(String key) {
    return false;
  }

  @Override
  public LssPropertyResetter getPropertyResetter() {
    return StyledWidgetResetter;
  }
}
