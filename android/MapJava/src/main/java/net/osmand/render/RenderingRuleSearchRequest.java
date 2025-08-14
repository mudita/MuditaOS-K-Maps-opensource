package net.osmand.render;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.util.Algorithms;

public class RenderingRuleSearchRequest {
   public final RenderingRuleStorageProperties ALL;
   private BinaryMapDataObject object;
   private final RenderingRuleProperty[] props;
   private final RenderingRulesStorage storage;
   private final int[] values;
   private final float[] fvalues;
   private int[] savedValues;
   private float[] savedFvalues;
   private boolean searchResult = false;

   public RenderingRuleSearchRequest(RenderingRulesStorage storage) {
      this.storage = storage;
      this.ALL = storage.PROPS;
      this.props = storage.PROPS.getPoperties();
      this.values = new int[this.props.length];

      for(int i = 0; i < this.props.length; ++i) {
         if (!this.props[i].isColor()) {
            this.values[i] = -1;
         }
      }

      this.fvalues = new float[this.props.length];
      this.setBooleanFilter(storage.PROPS.R_TEST, true);
      this.saveState();
   }

   public RenderingRuleSearchRequest(RenderingRuleSearchRequest searchRequest) {
      this.storage = searchRequest.storage;
      this.props = searchRequest.props;
      this.values = new int[searchRequest.values.length];
      this.fvalues = new float[searchRequest.fvalues.length];
      this.object = searchRequest.object;
      this.searchResult = searchRequest.searchResult;
      this.ALL = searchRequest.ALL;
      System.arraycopy(searchRequest.values, 0, this.values, 0, searchRequest.values.length);
      System.arraycopy(searchRequest.fvalues, 0, this.fvalues, 0, searchRequest.fvalues.length);
      this.saveState();
   }

   RenderingRulesStorage getStorage() {
      return this.storage;
   }

   public BinaryMapDataObject getObject() {
      return this.object;
   }

   public void setStringFilter(RenderingRuleProperty p, String filter) {
      assert p.isInputProperty();

      this.values[p.getId()] = this.storage.getDictionaryValue(filter);
   }

   public void setIntFilter(RenderingRuleProperty p, int filter) {
      assert p.isInputProperty();

      this.values[p.getId()] = filter;
   }

   public void setBooleanFilter(RenderingRuleProperty p, boolean filter) {
      assert p.isInputProperty();

      this.values[p.getId()] = filter ? 1 : 0;
   }

   public void setFloatFilter(RenderingRuleProperty p, float filter) {
      assert p.isInputProperty();

      this.fvalues[p.getId()] = filter;
   }

   public void saveState() {
      this.savedValues = new int[this.values.length];
      this.savedFvalues = new float[this.fvalues.length];
      System.arraycopy(this.values, 0, this.savedValues, 0, this.values.length);
      System.arraycopy(this.fvalues, 0, this.savedFvalues, 0, this.fvalues.length);
   }

   public void clearState() {
      System.arraycopy(this.savedValues, 0, this.values, 0, this.values.length);
      System.arraycopy(this.savedFvalues, 0, this.fvalues, 0, this.fvalues.length);
      this.object = null;
   }

   public void clearValue(RenderingRuleProperty p) {
      if (!p.isIntParse()) {
         this.fvalues[p.getId()] = this.savedFvalues[p.getId()];
      }

      this.values[p.getId()] = this.savedValues[p.getId()];
   }

   public void setInitialTagValueZoom(String tag, String val, int zoom, BinaryMapDataObject obj) {
      this.clearState();
      this.object = obj;
      this.setIntFilter(this.ALL.R_MINZOOM, zoom);
      this.setIntFilter(this.ALL.R_MAXZOOM, zoom);
      this.setStringFilter(this.ALL.R_TAG, tag);
      this.setStringFilter(this.ALL.R_VALUE, val);
   }

   public void setTagValueZoomLayer(String tag, String val, int zoom, int layer, BinaryMapDataObject obj) {
      this.object = obj;
      this.setIntFilter(this.ALL.R_MINZOOM, zoom);
      this.setIntFilter(this.ALL.R_MAXZOOM, zoom);
      this.setIntFilter(this.ALL.R_LAYER, layer);
      this.setStringFilter(this.ALL.R_TAG, tag);
      this.setStringFilter(this.ALL.R_VALUE, val);
   }

   public boolean isFound() {
      return this.searchResult;
   }

   public boolean searchRenderingAttribute(String attribute) {
      this.searchResult = false;
      RenderingRule rule = this.storage.getRenderingAttributeRule(attribute);
      if (rule == null) {
         return false;
      } else {
         this.searchResult = this.visitRule(rule, true);
         return this.searchResult;
      }
   }

   public boolean search(int state) {
      return this.search(state, true);
   }

   public boolean search(int state, boolean loadOutput) {
      this.searchResult = false;
      int tagKey = this.values[this.storage.PROPS.R_TAG.getId()];
      int valueKey = this.values[this.storage.PROPS.R_VALUE.getId()];
      boolean result = this.searchInternal(state, tagKey, valueKey, loadOutput);
      if (result) {
         this.searchResult = true;
         return true;
      } else {
         result = this.searchInternal(state, tagKey, 0, loadOutput);
         if (result) {
            this.searchResult = true;
            return true;
         } else {
            result = this.searchInternal(state, 0, 0, loadOutput);
            if (result) {
               this.searchResult = true;
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private boolean searchInternal(int state, int tagKey, int valueKey, boolean loadOutput) {
      this.values[this.storage.PROPS.R_TAG.getId()] = tagKey;
      this.values[this.storage.PROPS.R_VALUE.getId()] = valueKey;
      this.values[this.storage.PROPS.R_DISABLE.getId()] = 0;
      RenderingRule accept = this.storage.getRule(state, tagKey, valueKey);
      if (accept == null) {
         return false;
      } else {
         boolean match = this.visitRule(accept, loadOutput);
         return match && this.values[this.storage.PROPS.R_DISABLE.getId()] != 0 ? false : match;
      }
   }

   private boolean visitRule(RenderingRule rule, boolean loadOutput) {
      boolean input = this.checkInputProperties(rule);
      if (!input) {
         return false;
      } else if (!loadOutput && !rule.isGroup()) {
         return true;
      } else {
         if (!rule.isGroup()) {
            this.loadOutputProperties(rule, true);
         }

         boolean match = false;

         for(RenderingRule rr : rule.getIfElseChildren()) {
            match = this.visitRule(rr, loadOutput);
            if (match) {
               break;
            }
         }

         boolean fit = match || !rule.isGroup();
         if (fit && loadOutput) {
            if (rule.isGroup()) {
               this.loadOutputProperties(rule, false);
            }

            for(RenderingRule rr : rule.getIfChildren()) {
               this.visitRule(rr, loadOutput);
            }
         }

         return fit;
      }
   }

   protected void loadOutputProperties(RenderingRule rule, boolean override) {
      RenderingRuleProperty[] properties = rule.getProperties();

      for(int i = 0; i < properties.length; ++i) {
         RenderingRuleProperty rp = properties[i];
         if (rp.isOutputProperty() && (!this.isSpecified(rp) || override)) {
            RenderingRule rr = rule.getAttrProp(i);
            if (rr != null) {
               this.visitRule(rr, true);
               if (this.isSpecified(this.storage.PROPS.R_ATTR_COLOR_VALUE)) {
                  this.values[rp.getId()] = this.getIntPropertyValue(this.storage.PROPS.R_ATTR_COLOR_VALUE);
               } else if (this.isSpecified(this.storage.PROPS.R_ATTR_INT_VALUE)) {
                  this.values[rp.getId()] = this.getIntPropertyValue(this.storage.PROPS.R_ATTR_INT_VALUE);
                  this.fvalues[rp.getId()] = this.getFloatPropertyValue(this.storage.PROPS.R_ATTR_INT_VALUE);
               } else if (this.isSpecified(this.storage.PROPS.R_ATTR_BOOL_VALUE)) {
                  this.values[rp.getId()] = this.getIntPropertyValue(this.storage.PROPS.R_ATTR_BOOL_VALUE);
               }
            } else if (rp.isFloat()) {
               this.fvalues[rp.getId()] = rule.getFloatProp(i);
               this.values[rp.getId()] = rule.getIntProp(i);
            } else {
               this.values[rp.getId()] = rule.getIntProp(i);
            }
         }
      }
   }

   protected boolean checkInputProperties(RenderingRule rule) {
      RenderingRuleProperty[] properties = rule.getProperties();

      for(int i = 0; i < properties.length; ++i) {
         RenderingRuleProperty rp = properties[i];
         if (rp.isInputProperty()) {
            boolean match;
            if (rp.isFloat()) {
               match = rp.accept(rule.getFloatProp(i), this.fvalues[rp.getId()], this);
            } else {
               match = rp.accept(rule.getIntProp(i), this.values[rp.getId()], this);
            }

            if (!match) {
               return false;
            }
         } else if (rp == this.storage.PROPS.R_DISABLE) {
            this.values[rp.getId()] = rule.getIntProp(i);
         }
      }

      return true;
   }

   public boolean isSpecified(RenderingRuleProperty property) {
      if (!property.isFloat()) {
         int val = this.values[property.getId()];
         if (property.isColor()) {
            return val != 0;
         } else {
            return val != -1;
         }
      } else {
         return this.fvalues[property.getId()] != 0.0F || this.values[property.getId()] != -1;
      }
   }

   public RenderingRuleProperty[] getProperties() {
      return this.props;
   }

   public String getStringPropertyValue(RenderingRuleProperty property) {
      int val = this.values[property.getId()];
      return val < 0 ? null : this.storage.getStringValue(val);
   }

   public float getFloatPropertyValue(RenderingRuleProperty property) {
      return this.fvalues[property.getId()];
   }

   public float getFloatPropertyValue(RenderingRuleProperty property, float defVal) {
      float f = this.fvalues[property.getId()];
      return f == 0.0F ? defVal : f;
   }

   public String getColorStringPropertyValue(RenderingRuleProperty property) {
      return Algorithms.colorToString(this.values[property.getId()]);
   }

   public int getIntPropertyValue(RenderingRuleProperty property) {
      return this.values[property.getId()];
   }

   public boolean getBoolPropertyValue(RenderingRuleProperty property) {
      int val = this.values[property.getId()];
      return val != -1 && val != 0;
   }

   public int getIntPropertyValue(RenderingRuleProperty property, int defValue) {
      int val = this.values[property.getId()];
      return val == -1 ? defValue : val;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("object ").append(this.object).append("\n");

      for(int i = 0; i < this.props.length; ++i) {
         RenderingRuleProperty property = this.props[i];
         if (this.isSpecified(property)) {
            builder.append(i).append(" ").append(property.getAttrName()).append(" ");
            if (property.type == 1) {
               builder.append(this.getIntPropertyValue(property));
            } else if (property.type == 2) {
               builder.append(this.getFloatPropertyValue(property));
            } else if (property.type == 3) {
               builder.append(this.getStringPropertyValue(property));
            } else if (property.type == 4) {
               builder.append(this.getColorStringPropertyValue(property));
            } else if (property.type == 5) {
               builder.append(this.getBoolPropertyValue(property));
            }

            builder.append("\n");
         }
      }

      return builder.toString();
   }
}
