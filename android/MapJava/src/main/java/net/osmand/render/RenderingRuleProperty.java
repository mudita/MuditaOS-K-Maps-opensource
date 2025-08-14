package net.osmand.render;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import org.apache.commons.logging.Log;

public class RenderingRuleProperty {
   private static final Log log = PlatformUtil.getLog(RenderingRuleProperty.class);
   public static final int INT_TYPE = 1;
   public static final int FLOAT_TYPE = 2;
   public static final int STRING_TYPE = 3;
   public static final int COLOR_TYPE = 4;
   public static final int BOOLEAN_TYPE = 5;
   public static final int TRUE_VALUE = 1;
   public static final int FALSE_VALUE = 0;
   protected final int type;
   protected final boolean input;
   protected final String attrName;
   protected int id = -1;
   protected String name;
   protected String description;
   protected String defaultValueDescription;
   protected String[] possibleValues;
   protected String category;

   protected RenderingRuleProperty(String attrName, int type, boolean input) {
      this.attrName = attrName;
      this.type = type;
      this.input = input;
   }

   public boolean isInputProperty() {
      return this.input;
   }

   public boolean isOutputProperty() {
      return !this.input;
   }

   public void setId(int id) {
      if (this.id != -1) {
         throw new IllegalArgumentException();
      } else {
         this.id = id;
      }
   }

   public int getId() {
      return this.id;
   }

   public String getAttrName() {
      return this.attrName;
   }

   public String getName() {
      return this.name;
   }

   public String getCategory() {
      return this.category;
   }

   public String getDescription() {
      return this.description;
   }

   public String getDefaultValueDescription() {
      return this.defaultValueDescription;
   }

   protected void setName(String name) {
      this.name = name;
   }

   protected void setDescription(String description) {
      this.description = description;
   }

   public void setDefaultValueDescription(String defaultValueDescription) {
      this.defaultValueDescription = defaultValueDescription;
   }

   public void setCategory(String category) {
      this.category = category;
   }

   protected void setPossibleValues(String[] possibleValues) {
      this.possibleValues = possibleValues;
   }

   public String[] getPossibleValues() {
      return this.isBoolean() ? new String[]{"true", "false"} : this.possibleValues;
   }

   public boolean isBoolean() {
      return this.type == 5;
   }

   public boolean isFloat() {
      return this.type == 2;
   }

   public boolean isInt() {
      return this.type == 1;
   }

   public boolean isColor() {
      return this.type == 4;
   }

   public boolean isString() {
      return this.type == 3;
   }

   public boolean isIntParse() {
      return this.type == 1 || this.type == 3 || this.type == 4 || this.type == 5;
   }

   public boolean accept(int ruleValue, int renderingProperty, RenderingRuleSearchRequest req) {
      if (this.isIntParse() && this.input) {
         return ruleValue == renderingProperty;
      } else {
         return false;
      }
   }

   public boolean accept(float ruleValue, float renderingProperty, RenderingRuleSearchRequest req) {
      if (this.type == 2 && this.input) {
         return ruleValue == renderingProperty;
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "#RenderingRuleProperty " + this.getAttrName();
   }

   public int parseIntValue(String value) {
      if (this.type == 1) {
         try {
            int colon = value.indexOf(58);
            return colon != -1 ? (int)Float.parseFloat(value.substring(colon + 1)) : (int)Float.parseFloat(value);
         } catch (NumberFormatException var3) {
            log.error("Rendering parse " + value + " in " + this.attrName);
            return -1;
         }
      } else if (this.type == 5) {
         return Boolean.parseBoolean(value) ? 1 : 0;
      } else if (this.type == 3) {
         return -1;
      } else if (this.type == 4) {
         try {
            return parseColor(value);
         } catch (RuntimeException var4) {
            log.error("Rendering parse " + var4.getMessage() + " in " + this.attrName);
            return -1;
         }
      } else if (this.type == 2) {
         try {
            int colon = value.indexOf(58);
            return colon != -1 ? (int)Float.parseFloat(value.substring(colon + 1)) : 0;
         } catch (NumberFormatException var5) {
            log.error("Rendering parse " + value + " in " + this.attrName);
            return 0;
         }
      } else {
         return -1;
      }
   }

   public float parseFloatValue(String value) {
      try {
         if (this.type == 2) {
            int colon = value.indexOf(58);
            if (colon != -1) {
               if (colon > 0) {
                  return Float.parseFloat(value.substring(0, colon));
               }

               return 0.0F;
            }

            return Float.parseFloat(value);
         }

         if (this.type == 1) {
            int colon = value.indexOf(58);
            if (colon != -1 && colon > 0) {
               return Float.parseFloat(value.substring(0, colon));
            }

            return 0.0F;
         }
      } catch (NumberFormatException var3) {
         log.error("Rendering parse " + value + " in " + this.attrName);
      }

      return 0.0F;
   }

   public static RenderingRuleProperty createOutputIntProperty(String name) {
      return new RenderingRuleProperty(name, 1, false);
   }

   public static RenderingRuleProperty createOutputBooleanProperty(String name) {
      return new RenderingRuleProperty(name, 5, false);
   }

   public static RenderingRuleProperty createInputBooleanProperty(String name) {
      return new RenderingRuleProperty(name, 5, true);
   }

   public static RenderingRuleProperty createOutputFloatProperty(String name) {
      return new RenderingRuleProperty(name, 2, false);
   }

   public static RenderingRuleProperty createOutputStringProperty(String name) {
      return new RenderingRuleProperty(name, 3, false);
   }

   public static RenderingRuleProperty createInputIntProperty(String name) {
      return new RenderingRuleProperty(name, 1, true);
   }

   public static RenderingRuleProperty createInputColorProperty(String name) {
      return new RenderingRuleProperty(name, 4, true);
   }

   public static RenderingRuleProperty createOutputColorProperty(String name) {
      return new RenderingRuleProperty(name, 4, false);
   }

   public static RenderingRuleProperty createInputStringProperty(String name) {
      return new RenderingRuleProperty(name, 3, true);
   }

   public static RenderingRuleProperty createInputLessIntProperty(String name) {
      return new RenderingRuleProperty(name, 1, true) {
         @Override
         public boolean accept(int ruleValue, int renderingProperty, RenderingRuleSearchRequest req) {
            if (this.isIntParse() && this.input) {
               return ruleValue >= renderingProperty;
            } else {
               return false;
            }
         }
      };
   }

   public static RenderingRuleProperty createInputGreaterIntProperty(String name) {
      return new RenderingRuleProperty(name, 1, true) {
         @Override
         public boolean accept(int ruleValue, int renderingProperty, RenderingRuleSearchRequest req) {
            if (this.isIntParse() && this.input) {
               return ruleValue <= renderingProperty;
            } else {
               return false;
            }
         }
      };
   }

   public static RenderingRuleProperty createAdditionalStringProperty(String name) {
      return new RenderingRuleProperty(name, 3, true) {
         @Override
         public boolean accept(int ruleValue, int renderingProperty, RenderingRuleSearchRequest req) {
            BinaryMapDataObject obj = req.getObject();
            String val = req.getStorage().getStringValue(ruleValue);
            if (obj == null) {
               int vl = req.getIntPropertyValue(this);
               if (vl == -1) {
                  return false;
               } else {
                  String val2 = req.getStorage().getStringValue(vl);
                  return val != null && (val.equals(val2) || val2.indexOf(59) != -1 && val2.contains(val + ';'));
               }
            } else {
               int k = val.indexOf(61);
               if (k != -1) {
                  String ts = val.substring(0, k);
                  String vs = val.substring(k + 1);
                  Integer ruleInd = req.getObject().getMapIndex().getRule(ts, vs);
                  if (ruleInd != null && req.getObject().containsAdditionalType(ruleInd)) {
                     return true;
                  }
               } else {
                  String ts = val;
                  int[] additionalTypes = obj.getAdditionalTypes();

                  for(int i = 0; i < additionalTypes.length; ++i) {
                     BinaryMapIndexReader.TagValuePair vp = obj.getMapIndex().decodeType(additionalTypes[i]);
                     if (vp != null && ts.equals(vp.tag)) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      };
   }

   public static int parseColor(String colorString) {
      if (colorString.charAt(0) == '#') {
         long color = Long.parseLong(colorString.substring(1), 16);
         if (colorString.length() == 7) {
            color |= -16777216L;
         } else if (colorString.length() != 9) {
            throw new IllegalArgumentException("Unknown color " + colorString);
         }

         return (int)color;
      } else {
         throw new IllegalArgumentException("Unknown color " + colorString);
      }
   }
}
