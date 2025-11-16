package net.labymod.autogen.core.lss.properties.direct;

import java.lang.Override;
import java.lang.String;
import net.labymod.api.client.gui.lss.property.LssPropertyResetter;
import net.labymod.api.client.gui.lss.property.PropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetAlignmentXPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetAlignmentYPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetAlwaysFocusedPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetAnimationDurationPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetAnimationTimingFunctionPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundAlwaysDirtPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundColorPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundColorTransitionDurationPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundDirtBrightnessPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundDirtShiftPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBackgroundDirtTypePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBottomPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetBoxSizingPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetCancelParentHoverComponentPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetClearDepthPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetDestroyDelayPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetDistinctPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetDraggablePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetFilterPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetFitOuterPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetFontWeightPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetForceVanillaFontPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetHeightPrecisionPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetHoverBoxDelayPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetInteractableOutsidePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetInteractablePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetLeftPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetMarginBottomPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetMarginLeftPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetMarginRightPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetMarginTopPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetMouseRenderDistancePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetOpacityPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPaddingBottomPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPaddingLeftPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPaddingRightPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPaddingTopPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPressablePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetPriorityLayerPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetRenderChildrenPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetRendererPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetRightPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetScaleXPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetScaleYPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetStencilTranslationPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetStencilXPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetStencilYPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetTopPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetTranslateXPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetTranslateYPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetUseFloatingPointPositionPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetVisiblePropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetWidthPrecisionPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetWriteToStencilBufferPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.accessors.AbstractWidgetZIndexPropertyValueAccessor;
import net.labymod.autogen.core.lss.properties.resetters.AbstractWidgetLssPropertyResetter;

public class AbstractWidgetDirectPropertyValueAccessor extends StyledWidgetDirectPropertyValueAccessor {
  protected PropertyValueAccessor<?, ?, ?> renderer = new AbstractWidgetRendererPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> boxSizing = new AbstractWidgetBoxSizingPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> animationDuration = new AbstractWidgetAnimationDurationPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> draggable = new AbstractWidgetDraggablePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> interactable = new AbstractWidgetInteractablePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> pressable = new AbstractWidgetPressablePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> visible = new AbstractWidgetVisiblePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> left = new AbstractWidgetLeftPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> top = new AbstractWidgetTopPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> right = new AbstractWidgetRightPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> bottom = new AbstractWidgetBottomPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> marginLeft = new AbstractWidgetMarginLeftPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> marginTop = new AbstractWidgetMarginTopPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> marginRight = new AbstractWidgetMarginRightPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> marginBottom = new AbstractWidgetMarginBottomPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> paddingLeft = new AbstractWidgetPaddingLeftPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> paddingTop = new AbstractWidgetPaddingTopPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> paddingRight = new AbstractWidgetPaddingRightPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> paddingBottom = new AbstractWidgetPaddingBottomPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> widthPrecision = new AbstractWidgetWidthPrecisionPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> heightPrecision = new AbstractWidgetHeightPrecisionPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> priorityLayer = new AbstractWidgetPriorityLayerPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> alignmentX = new AbstractWidgetAlignmentXPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> alignmentY = new AbstractWidgetAlignmentYPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> translateX = new AbstractWidgetTranslateXPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> translateY = new AbstractWidgetTranslateYPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> zIndex = new AbstractWidgetZIndexPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> scaleX = new AbstractWidgetScaleXPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> scaleY = new AbstractWidgetScaleYPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> opacity = new AbstractWidgetOpacityPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> mouseRenderDistance = new AbstractWidgetMouseRenderDistancePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> stencilX = new AbstractWidgetStencilXPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> stencilY = new AbstractWidgetStencilYPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> stencilTranslation = new AbstractWidgetStencilTranslationPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> writeToStencilBuffer = new AbstractWidgetWriteToStencilBufferPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> useFloatingPointPosition = new AbstractWidgetUseFloatingPointPositionPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> alwaysFocused = new AbstractWidgetAlwaysFocusedPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> interactableOutside = new AbstractWidgetInteractableOutsidePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> destroyDelay = new AbstractWidgetDestroyDelayPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> fitOuter = new AbstractWidgetFitOuterPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> distinct = new AbstractWidgetDistinctPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> cancelParentHoverComponent = new AbstractWidgetCancelParentHoverComponentPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> renderChildren = new AbstractWidgetRenderChildrenPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundColor = new AbstractWidgetBackgroundColorPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundColorTransitionDuration = new AbstractWidgetBackgroundColorTransitionDurationPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundAlwaysDirt = new AbstractWidgetBackgroundAlwaysDirtPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundDirtShift = new AbstractWidgetBackgroundDirtShiftPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundDirtType = new AbstractWidgetBackgroundDirtTypePropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> fontWeight = new AbstractWidgetFontWeightPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> animationTimingFunction = new AbstractWidgetAnimationTimingFunctionPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> filter = new AbstractWidgetFilterPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> hoverBoxDelay = new AbstractWidgetHoverBoxDelayPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> clearDepth = new AbstractWidgetClearDepthPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> forceVanillaFont = new AbstractWidgetForceVanillaFontPropertyValueAccessor();

  protected PropertyValueAccessor<?, ?, ?> backgroundDirtBrightness = new AbstractWidgetBackgroundDirtBrightnessPropertyValueAccessor();

  LssPropertyResetter AbstractWidgetResetter = new AbstractWidgetLssPropertyResetter();

  @Override
  public PropertyValueAccessor<?, ?, ?> getPropertyValueAccessor(String key) {
    switch(key) {
      case "renderer":return renderer;
      case "boxSizing":return boxSizing;
      case "animationDuration":return animationDuration;
      case "draggable":return draggable;
      case "interactable":return interactable;
      case "pressable":return pressable;
      case "visible":return visible;
      case "left":return left;
      case "top":return top;
      case "right":return right;
      case "bottom":return bottom;
      case "marginLeft":return marginLeft;
      case "marginTop":return marginTop;
      case "marginRight":return marginRight;
      case "marginBottom":return marginBottom;
      case "paddingLeft":return paddingLeft;
      case "paddingTop":return paddingTop;
      case "paddingRight":return paddingRight;
      case "paddingBottom":return paddingBottom;
      case "widthPrecision":return widthPrecision;
      case "heightPrecision":return heightPrecision;
      case "priorityLayer":return priorityLayer;
      case "alignmentX":return alignmentX;
      case "alignmentY":return alignmentY;
      case "translateX":return translateX;
      case "translateY":return translateY;
      case "zIndex":return zIndex;
      case "scaleX":return scaleX;
      case "scaleY":return scaleY;
      case "opacity":return opacity;
      case "mouseRenderDistance":return mouseRenderDistance;
      case "stencilX":return stencilX;
      case "stencilY":return stencilY;
      case "stencilTranslation":return stencilTranslation;
      case "writeToStencilBuffer":return writeToStencilBuffer;
      case "useFloatingPointPosition":return useFloatingPointPosition;
      case "alwaysFocused":return alwaysFocused;
      case "interactableOutside":return interactableOutside;
      case "destroyDelay":return destroyDelay;
      case "fitOuter":return fitOuter;
      case "distinct":return distinct;
      case "cancelParentHoverComponent":return cancelParentHoverComponent;
      case "renderChildren":return renderChildren;
      case "backgroundColor":return backgroundColor;
      case "backgroundColorTransitionDuration":return backgroundColorTransitionDuration;
      case "backgroundAlwaysDirt":return backgroundAlwaysDirt;
      case "backgroundDirtShift":return backgroundDirtShift;
      case "backgroundDirtType":return backgroundDirtType;
      case "fontWeight":return fontWeight;
      case "animationTimingFunction":return animationTimingFunction;
      case "filter":return filter;
      case "hoverBoxDelay":return hoverBoxDelay;
      case "clearDepth":return clearDepth;
      case "forceVanillaFont":return forceVanillaFont;
      case "backgroundDirtBrightness":return backgroundDirtBrightness;
    }
    return super.getPropertyValueAccessor(key);
  }

  @Override
  public boolean hasPropertyValueAccessor(String key) {
    switch(key) {
      case "renderer":return true;
      case "boxSizing":return true;
      case "animationDuration":return true;
      case "draggable":return true;
      case "interactable":return true;
      case "pressable":return true;
      case "visible":return true;
      case "left":return true;
      case "top":return true;
      case "right":return true;
      case "bottom":return true;
      case "marginLeft":return true;
      case "marginTop":return true;
      case "marginRight":return true;
      case "marginBottom":return true;
      case "paddingLeft":return true;
      case "paddingTop":return true;
      case "paddingRight":return true;
      case "paddingBottom":return true;
      case "widthPrecision":return true;
      case "heightPrecision":return true;
      case "priorityLayer":return true;
      case "alignmentX":return true;
      case "alignmentY":return true;
      case "translateX":return true;
      case "translateY":return true;
      case "zIndex":return true;
      case "scaleX":return true;
      case "scaleY":return true;
      case "opacity":return true;
      case "mouseRenderDistance":return true;
      case "stencilX":return true;
      case "stencilY":return true;
      case "stencilTranslation":return true;
      case "writeToStencilBuffer":return true;
      case "useFloatingPointPosition":return true;
      case "alwaysFocused":return true;
      case "interactableOutside":return true;
      case "destroyDelay":return true;
      case "fitOuter":return true;
      case "distinct":return true;
      case "cancelParentHoverComponent":return true;
      case "renderChildren":return true;
      case "backgroundColor":return true;
      case "backgroundColorTransitionDuration":return true;
      case "backgroundAlwaysDirt":return true;
      case "backgroundDirtShift":return true;
      case "backgroundDirtType":return true;
      case "fontWeight":return true;
      case "animationTimingFunction":return true;
      case "filter":return true;
      case "hoverBoxDelay":return true;
      case "clearDepth":return true;
      case "forceVanillaFont":return true;
      case "backgroundDirtBrightness":return true;
    }
    return super.hasPropertyValueAccessor(key);
  }

  @Override
  public LssPropertyResetter getPropertyResetter() {
    return AbstractWidgetResetter;
  }
}
