package net.labymod.autogen.core.lss.properties.direct;

import java.lang.Override;
import java.lang.String;
import net.labymod.api.client.gui.lss.property.LssPropertyResetter;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.resetters.BotDropDownLssPropertyResetter;

public class BotDropDownDirectPropertyValueAccessor extends HorizontalListWidgetDirectPropertyValueAccessor {
  LssPropertyResetter BotDropDownResetter = new BotDropDownLssPropertyResetter();

  @Override
  public PropertyValueAccessor<?, ?, ?> getPropertyValueAccessor(String key) {
    return super.getPropertyValueAccessor(key);
  }

  @Override
  public boolean hasPropertyValueAccessor(String key) {
    return super.hasPropertyValueAccessor(key);
  }

  @Override
  public LssPropertyResetter getPropertyResetter() {
    return BotDropDownResetter;
  }
}
