package net.labymod.autogen.core.lss.properties.direct;

import java.lang.Override;
import java.lang.String;
import net.labymod.api.client.gui.lss.property.LssPropertyResetter;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.HorizontalListWidgetLayoutPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.HorizontalListWidgetSpaceBetweenEntriesPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.HorizontalListWidgetSpaceLeftPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.HorizontalListWidgetSpaceRightPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.resetters.HorizontalListWidgetLssPropertyResetter;

public class HorizontalListWidgetDirectPropertyValueAccessor extends ListWidgetDirectPropertyValueAccessor {
  protected PropertyValueAccessor<?, ?, ?> spaceLeft = new HorizontalListWidgetSpaceLeftPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> spaceRight = new HorizontalListWidgetSpaceRightPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> spaceBetweenEntries = new HorizontalListWidgetSpaceBetweenEntriesPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> layout = new HorizontalListWidgetLayoutPropertyValueAccessor();

  LssPropertyResetter HorizontalListWidgetResetter = new HorizontalListWidgetLssPropertyResetter();

  @Override
  public PropertyValueAccessor<?, ?, ?> getPropertyValueAccessor(String key) {
    switch(key) {
      case "spaceLeft":return spaceLeft;
      case "spaceRight":return spaceRight;
      case "spaceBetweenEntries":return spaceBetweenEntries;
      case "layout":return layout;
    }
    return super.getPropertyValueAccessor(key);
  }

  @Override
  public boolean hasPropertyValueAccessor(String key) {
    switch(key) {
      case "spaceLeft":return true;
      case "spaceRight":return true;
      case "spaceBetweenEntries":return true;
      case "layout":return true;
    }
    return super.hasPropertyValueAccessor(key);
  }

  @Override
  public LssPropertyResetter getPropertyResetter() {
    return HorizontalListWidgetResetter;
  }
}
