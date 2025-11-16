package net.labymod.autogen.core.lss;

import de.ggbot.core.cfg.BotDropDown;
import java.lang.Class;
import java.lang.Override;
import java.util.Map;
import net.labymod.api.client.gui.lss.property.DirectPropertyValueAccessor;
import net.labymod.api.client.gui.lss.property.DirectPropertyValueAccessorRegisterBulk;
import net.labymod.api.client.gui.screen.widget.AbstractWidget;
import net.labymod.api.client.gui.screen.widget.StyledWidget;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.ListWidget;
import net.labymod.autogen.core.lss.properties.direct.AbstractWidgetDirectPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.direct.BotDropDownDirectPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.direct.HorizontalListWidgetDirectPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.direct.ListWidgetDirectPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.direct.StyledWidgetDirectPropertyValueAccessor;

public final class CoreGgbot_addonDirectPropertyValueAccessorRegisterBulk implements DirectPropertyValueAccessorRegisterBulk {
  @Override
  public void register(Map<Class<? extends Widget>, DirectPropertyValueAccessor> map) {
    map.put(StyledWidget.class, new StyledWidgetDirectPropertyValueAccessor());
    map.put(AbstractWidget.class, new AbstractWidgetDirectPropertyValueAccessor());
    map.put(ListWidget.class, new ListWidgetDirectPropertyValueAccessor());
    map.put(HorizontalListWidget.class, new HorizontalListWidgetDirectPropertyValueAccessor());
    map.put(BotDropDown.class, new BotDropDownDirectPropertyValueAccessor());
  }
}
