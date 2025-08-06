package net.osmand.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RenderingRuleStorageProperties {
   public static final String UI_CATEGORY_HIDDEN = "ui_hidden";
   public static final String UI_CATEGORY_DETAILS = "details";
   public static final String UI_CATEGORY_HIDE = "hide";
   public static final String UI_CATEGORY_ROUTES = "routes";
   public static final String A_ENGINE_V1 = "engine_v1";
   public static final String A_APP_MODE = "appMode";
   public static final String A_BASE_APP_MODE = "baseAppMode";
   public static final String A_DEFAULT_COLOR = "defaultColor";
   public static final String A_SHADOW_RENDERING = "shadowRendering";
   public static final String ATTR_INT_VALUE = "attrIntValue";
   public static final String ATTR_BOOL_VALUE = "attrBoolValue";
   public static final String ATTR_COLOR_VALUE = "attrColorValue";
   public static final String ATTR_STRING_VALUE = "attrStringValue";
   public static final String TEST = "test";
   public static final String DISABLE = "disable";
   public static final String INTERSECTION_MARGIN = "intersectionMargin";
   public static final String INTERSECTION_SIZE_FACTOR = "intersectionSizeFactor";
   public static final String TEXT_ITALIC = "textItalic";
   public static final String TEXT_BOLD = "textBold";
   public static final String TEXT_LENGTH = "textLength";
   public static final String NAME_TAG = "nameTag";
   public static final String NAME_TAG2 = "nameTag2";
   public static final String TEXT_SHIELD = "textShield";
   public static final String SHIELD = "shield";
   public static final String SHADOW_RADIUS = "shadowRadius";
   public static final String SHADOW_COLOR = "shadowColor";
   public static final String ONEWAY_ARROWS_COLOR = "onewayArrowsColor";
   public static final String IGNORE_POLYGON_AS_POINT_AREA = "ignorePolygonAsPointArea";
   public static final String SHADER = "shader";
   public static final String CAP_5 = "cap_5";
   public static final String CAP_4 = "cap_4";
   public static final String CAP_3 = "cap_3";
   public static final String CAP_2 = "cap_2";
   public static final String CAP = "cap";
   public static final String CAP_0 = "cap_0";
   public static final String CAP__1 = "cap__1";
   public static final String CAP__2 = "cap__2";
   public static final String PATH_EFFECT_5 = "pathEffect_5";
   public static final String PATH_EFFECT_4 = "pathEffect_4";
   public static final String PATH_EFFECT_3 = "pathEffect_3";
   public static final String PATH_EFFECT_2 = "pathEffect_2";
   public static final String PATH_EFFECT = "pathEffect";
   public static final String PATH_EFFECT_0 = "pathEffect_0";
   public static final String PATH_EFFECT__1 = "pathEffect__1";
   public static final String PATH_EFFECT__2 = "pathEffect__2";
   public static final String STROKE_WIDTH_5 = "strokeWidth_5";
   public static final String STROKE_WIDTH_4 = "strokeWidth_4";
   public static final String STROKE_WIDTH_3 = "strokeWidth_3";
   public static final String STROKE_WIDTH_2 = "strokeWidth_2";
   public static final String STROKE_WIDTH = "strokeWidth";
   public static final String STROKE_WIDTH_0 = "strokeWidth_0";
   public static final String STROKE_WIDTH__1 = "strokeWidth__1";
   public static final String STROKE_WIDTH__2 = "strokeWidth__2";
   public static final String COLOR_5 = "color_5";
   public static final String COLOR_4 = "color_4";
   public static final String COLOR_3 = "color_3";
   public static final String COLOR = "color";
   public static final String COLOR_2 = "color_2";
   public static final String COLOR_0 = "color_0";
   public static final String COLOR__1 = "color__1";
   public static final String COLOR__2 = "color__2";
   public static final String TEXT_ORDER = "textOrder";
   public static final String ICON_ORDER = "iconOrder";
   public static final String ICON_VISIBLE_SIZE = "iconVisibleSize";
   public static final String TEXT_MIN_DISTANCE = "textMinDistance";
   public static final String TEXT_ON_PATH = "textOnPath";
   public static final String ICON = "icon";
   public static final String LAYER = "layer";
   public static final String ORDER = "order";
   public static final String OBJECT_TYPE = "objectType";
   public static final String POINT = "point";
   public static final String AREA = "area";
   public static final String CYCLE = "cycle";
   public static final String TAG = "tag";
   public static final String VALUE = "value";
   public static final String MINZOOM = "minzoom";
   public static final String MAXZOOM = "maxzoom";
   public static final String ADDITIONAL = "additional";
   public static final String NIGHT_MODE = "nightMode";
   public static final String TEXT_DY = "textDy";
   public static final String TEXT_SIZE = "textSize";
   public static final String TEXT_COLOR = "textColor";
   public static final String TEXT_HALO_RADIUS = "textHaloRadius";
   public static final String TEXT_HALO_COLOR = "textHaloColor";
   public static final String TEXT_WRAP_WIDTH = "textWrapWidth";
   public static final String SHADOW_LEVEL = "shadowLevel";
   public static final String ADD_POINT = "addPoint";
   public static final String ORDER_BY_DENSITY = "orderByDensity";
   public RenderingRuleProperty R_TEST;
   public RenderingRuleProperty R_DISABLE;
   public RenderingRuleProperty R_ATTR_INT_VALUE;
   public RenderingRuleProperty R_ATTR_BOOL_VALUE;
   public RenderingRuleProperty R_ATTR_COLOR_VALUE;
   public RenderingRuleProperty R_ATTR_STRING_VALUE;
   public RenderingRuleProperty R_TEXT_LENGTH;
   public RenderingRuleProperty R_NAME_TAG;
   public RenderingRuleProperty R_NAME_TAG2;
   public RenderingRuleProperty R_TEXT_SHIELD;
   public RenderingRuleProperty R_SHIELD;
   public RenderingRuleProperty R_SHADOW_RADIUS;
   public RenderingRuleProperty R_SHADOW_COLOR;
   public RenderingRuleProperty R_SHADER;
   public RenderingRuleProperty R_ONEWAY_ARROWS_COLOR;
   public RenderingRuleProperty R_IGNORE_POLYGON_AS_POINT_AREA;
   public RenderingRuleProperty R_CAP_5;
   public RenderingRuleProperty R_CAP_4;
   public RenderingRuleProperty R_CAP_3;
   public RenderingRuleProperty R_CAP_2;
   public RenderingRuleProperty R_CAP;
   public RenderingRuleProperty R_CAP_0;
   public RenderingRuleProperty R_CAP__1;
   public RenderingRuleProperty R_CAP__2;
   public RenderingRuleProperty R_PATH_EFFECT_5;
   public RenderingRuleProperty R_PATH_EFFECT_4;
   public RenderingRuleProperty R_PATH_EFFECT_3;
   public RenderingRuleProperty R_PATH_EFFECT_2;
   public RenderingRuleProperty R_PATH_EFFECT;
   public RenderingRuleProperty R_PATH_EFFECT_0;
   public RenderingRuleProperty R_PATH_EFFECT__1;
   public RenderingRuleProperty R_PATH_EFFECT__2;
   public RenderingRuleProperty R_STROKE_WIDTH_5;
   public RenderingRuleProperty R_STROKE_WIDTH_4;
   public RenderingRuleProperty R_STROKE_WIDTH_3;
   public RenderingRuleProperty R_STROKE_WIDTH_2;
   public RenderingRuleProperty R_STROKE_WIDTH;
   public RenderingRuleProperty R_STROKE_WIDTH_0;
   public RenderingRuleProperty R_STROKE_WIDTH__1;
   public RenderingRuleProperty R_STROKE_WIDTH__2;
   public RenderingRuleProperty R_COLOR_5;
   public RenderingRuleProperty R_COLOR_4;
   public RenderingRuleProperty R_COLOR_3;
   public RenderingRuleProperty R_COLOR;
   public RenderingRuleProperty R_COLOR_2;
   public RenderingRuleProperty R_COLOR_0;
   public RenderingRuleProperty R_COLOR__1;
   public RenderingRuleProperty R_COLOR__2;
   public RenderingRuleProperty R_TEXT_BOLD;
   public RenderingRuleProperty R_TEXT_ITALIC;
   public RenderingRuleProperty R_TEXT_ORDER;
   public RenderingRuleProperty R_ICON_ORDER;
   public RenderingRuleProperty R_TEXT_MIN_DISTANCE;
   public RenderingRuleProperty R_TEXT_ON_PATH;
   public RenderingRuleProperty R_ICON_SHIFT_PX;
   public RenderingRuleProperty R_ICON_SHIFT_PY;
   public RenderingRuleProperty R_ICON__1;
   public RenderingRuleProperty R_ICON;
   public RenderingRuleProperty R_ICON_2;
   public RenderingRuleProperty R_ICON_3;
   public RenderingRuleProperty R_ICON_4;
   public RenderingRuleProperty R_ICON_5;
   public RenderingRuleProperty R_ICON_VISIBLE_SIZE;
   public RenderingRuleProperty R_INTERSECTION_MARGIN;
   public RenderingRuleProperty R_INTERSECTION_SIZE_FACTOR;
   public RenderingRuleProperty R_LAYER;
   public RenderingRuleProperty R_ORDER;
   public RenderingRuleProperty R_POINT;
   public RenderingRuleProperty R_AREA;
   public RenderingRuleProperty R_CYCLE;
   public RenderingRuleProperty R_OBJECT_TYPE;
   public RenderingRuleProperty R_TAG;
   public RenderingRuleProperty R_VALUE;
   public RenderingRuleProperty R_MINZOOM;
   public RenderingRuleProperty R_ADDITIONAL;
   public RenderingRuleProperty R_SHADOW_LEVEL;
   public RenderingRuleProperty R_MAXZOOM;
   public RenderingRuleProperty R_NIGHT_MODE;
   public RenderingRuleProperty R_TEXT_DY;
   public RenderingRuleProperty R_TEXT_SIZE;
   public RenderingRuleProperty R_TEXT_COLOR;
   public RenderingRuleProperty R_TEXT_HALO_RADIUS;
   public RenderingRuleProperty R_TEXT_HALO_COLOR;
   public RenderingRuleProperty R_TEXT_WRAP_WIDTH;
   public RenderingRuleProperty R_ADD_POINT;
   public RenderingRuleProperty R_ORDER_BY_DENSITY;
   final Map<String, RenderingRuleProperty> properties;
   final List<RenderingRuleProperty> rules;
   final List<RenderingRuleProperty> customRules;

   public RenderingRuleStorageProperties() {
      this.properties = new LinkedHashMap<>();
      this.rules = new ArrayList<>();
      this.customRules = new ArrayList<>();
      this.createDefaultRenderingRuleProperties();
   }

   public RenderingRuleStorageProperties(RenderingRuleStorageProperties toClone) {
      this.properties = new LinkedHashMap<>(toClone.properties);
      this.rules = new ArrayList<>(toClone.rules);
      this.customRules = new ArrayList<>(toClone.customRules);
      this.createDefaultRenderingRuleProperties();
   }

   public void createDefaultRenderingRuleProperties() {
      this.R_TEST = this.registerRuleInternal(RenderingRuleProperty.createInputBooleanProperty("test"));
      this.R_TAG = this.registerRuleInternal(RenderingRuleProperty.createInputStringProperty("tag"));
      this.R_VALUE = this.registerRuleInternal(RenderingRuleProperty.createInputStringProperty("value"));
      this.R_ADDITIONAL = this.registerRuleInternal(RenderingRuleProperty.createAdditionalStringProperty("additional"));
      this.R_MINZOOM = this.registerRuleInternal(RenderingRuleProperty.createInputGreaterIntProperty("minzoom"));
      this.R_MAXZOOM = this.registerRuleInternal(RenderingRuleProperty.createInputLessIntProperty("maxzoom"));
      this.R_NIGHT_MODE = this.registerRuleInternal(RenderingRuleProperty.createInputBooleanProperty("nightMode"));
      this.R_LAYER = this.registerRuleInternal(RenderingRuleProperty.createInputIntProperty("layer"));
      this.R_POINT = this.registerRuleInternal(RenderingRuleProperty.createInputBooleanProperty("point"));
      this.R_AREA = this.registerRuleInternal(RenderingRuleProperty.createInputBooleanProperty("area"));
      this.R_CYCLE = this.registerRuleInternal(RenderingRuleProperty.createInputBooleanProperty("cycle"));
      this.R_INTERSECTION_MARGIN = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("intersectionMargin"));
      this.R_INTERSECTION_SIZE_FACTOR = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("intersectionSizeFactor"));
      this.R_TEXT_LENGTH = this.registerRuleInternal(RenderingRuleProperty.createInputIntProperty("textLength"));
      this.R_NAME_TAG = this.registerRuleInternal(RenderingRuleProperty.createInputStringProperty("nameTag"));
      this.R_NAME_TAG2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("nameTag2"));
      this.R_DISABLE = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("disable"));
      this.R_ATTR_INT_VALUE = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("attrIntValue"));
      this.R_ATTR_BOOL_VALUE = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("attrBoolValue"));
      this.R_ATTR_COLOR_VALUE = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("attrColorValue"));
      this.R_ATTR_STRING_VALUE = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("attrStringValue"));
      this.R_ORDER = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("order"));
      this.R_OBJECT_TYPE = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("objectType"));
      this.R_SHADOW_LEVEL = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("shadowLevel"));
      this.R_TEXT_WRAP_WIDTH = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("textWrapWidth"));
      this.R_TEXT_DY = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("textDy"));
      this.R_TEXT_HALO_RADIUS = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("textHaloRadius"));
      this.R_TEXT_HALO_COLOR = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("textHaloColor"));
      this.R_TEXT_SIZE = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("textSize"));
      this.R_TEXT_ORDER = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("textOrder"));
      this.R_TEXT_MIN_DISTANCE = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("textMinDistance"));
      this.R_TEXT_SHIELD = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("textShield"));
      this.R_TEXT_COLOR = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("textColor"));
      this.R_TEXT_BOLD = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("textBold"));
      this.R_TEXT_ITALIC = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("textItalic"));
      this.R_TEXT_ON_PATH = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("textOnPath"));
      this.R_ICON_SHIFT_PX = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("icon_shift_px"));
      this.R_ICON_SHIFT_PY = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("icon_shift_py"));
      this.R_ICON__1 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon__1"));
      this.R_ICON = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon"));
      this.R_ICON_2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon_2"));
      this.R_ICON_3 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon_3"));
      this.R_ICON_4 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon_4"));
      this.R_ICON_5 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("icon_5"));
      this.R_ICON_ORDER = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("iconOrder"));
      this.R_SHIELD = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("shield"));
      this.R_ICON_VISIBLE_SIZE = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("iconVisibleSize"));
      this.R_COLOR = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color"));
      this.R_COLOR_2 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color_2"));
      this.R_COLOR_3 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color_3"));
      this.R_COLOR_4 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color_4"));
      this.R_COLOR_5 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color_5"));
      this.R_COLOR_0 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color_0"));
      this.R_COLOR__1 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color__1"));
      this.R_COLOR__2 = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("color__2"));
      this.R_STROKE_WIDTH = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth"));
      this.R_STROKE_WIDTH_2 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth_2"));
      this.R_STROKE_WIDTH_3 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth_3"));
      this.R_STROKE_WIDTH_4 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth_4"));
      this.R_STROKE_WIDTH_5 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth_5"));
      this.R_STROKE_WIDTH_0 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth_0"));
      this.R_STROKE_WIDTH__1 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth__1"));
      this.R_STROKE_WIDTH__2 = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("strokeWidth__2"));
      this.R_PATH_EFFECT = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect"));
      this.R_PATH_EFFECT_2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect_2"));
      this.R_PATH_EFFECT_3 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect_3"));
      this.R_PATH_EFFECT_4 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect_4"));
      this.R_PATH_EFFECT_5 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect_5"));
      this.R_PATH_EFFECT_0 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect_0"));
      this.R_PATH_EFFECT__1 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect__1"));
      this.R_PATH_EFFECT__2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("pathEffect__2"));
      this.R_CAP = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap"));
      this.R_CAP_2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap_2"));
      this.R_CAP_3 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap_3"));
      this.R_CAP_4 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap_4"));
      this.R_CAP_5 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap_5"));
      this.R_CAP_0 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap_0"));
      this.R_CAP__1 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap__1"));
      this.R_CAP__2 = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("cap__2"));
      this.R_SHADER = this.registerRuleInternal(RenderingRuleProperty.createOutputStringProperty("shader"));
      this.R_SHADOW_COLOR = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("shadowColor"));
      this.R_SHADOW_RADIUS = this.registerRuleInternal(RenderingRuleProperty.createOutputFloatProperty("shadowRadius"));
      this.R_ONEWAY_ARROWS_COLOR = this.registerRuleInternal(RenderingRuleProperty.createOutputColorProperty("onewayArrowsColor"));
      this.R_ADD_POINT = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("addPoint"));
      this.R_IGNORE_POLYGON_AS_POINT_AREA = this.registerRuleInternal(RenderingRuleProperty.createOutputBooleanProperty("ignorePolygonAsPointArea"));
      this.R_ORDER_BY_DENSITY = this.registerRuleInternal(RenderingRuleProperty.createOutputIntProperty("orderByDensity"));
   }

   public RenderingRuleProperty get(String name) {
      return this.properties.get(name);
   }

   public RenderingRuleProperty[] getPoperties() {
      return this.rules.toArray(new RenderingRuleProperty[0]);
   }

   public List<RenderingRuleProperty> getCustomRules() {
      return this.customRules;
   }

   public RenderingRuleProperty getCustomRule(String attrName) {
      for(RenderingRuleProperty p : this.customRules) {
         if (p.getAttrName().equals(attrName)) {
            return p;
         }
      }

      return null;
   }

   private RenderingRuleProperty registerRuleInternal(RenderingRuleProperty p) {
      RenderingRuleProperty existing = this.get(p.getAttrName());
      this.properties.put(p.getAttrName(), p);
      if (existing == null) {
         p.setId(this.rules.size());
         this.rules.add(p);
      } else {
         p.setId(existing.getId());
         this.rules.set(existing.getId(), p);
         this.customRules.remove(existing);
      }

      return p;
   }

   public RenderingRuleProperty registerRule(RenderingRuleProperty p) {
      RenderingRuleProperty ps = this.registerRuleInternal(p);
      if (!this.customRules.contains(ps)) {
         this.customRules.add(p);
      }

      return ps;
   }
}
