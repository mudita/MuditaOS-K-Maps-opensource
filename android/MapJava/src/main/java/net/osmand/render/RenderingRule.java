package net.osmand.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.util.Algorithms;

public class RenderingRule {
   private RenderingRuleProperty[] properties;
   private int[] intProperties;
   private RenderingRule[] attributesRef;
   private float[] floatProperties;
   private List<RenderingRule> ifElseChildren;
   private List<RenderingRule> ifChildren;
   private final boolean isGroup;
   private final RenderingRulesStorage storage;
   private Map<String, String> attributes;

   public RenderingRule(Map<String, String> attributes, boolean isGroup, RenderingRulesStorage storage) {
      this.isGroup = isGroup;
      this.storage = storage;
      this.init(attributes);
   }

   public void storeAttributes(Map<String, String> attributes) {
      this.attributes = new HashMap<>(attributes);
   }

   public Map<String, String> getAttributes() {
      return this.attributes == null ? Collections.EMPTY_MAP : this.attributes;
   }

   public boolean hasAttributes(Map<String, String> attributes) {
      for(Entry<String, String> tagValue : attributes.entrySet()) {
         if (!tagValue.getValue().equals(this.attributes.get(tagValue.getKey()))) {
            return false;
         }
      }

      return true;
   }

   public void init(Map<String, String> attributes) {
      ArrayList<RenderingRuleProperty> props = new ArrayList<>(attributes.size());
      this.intProperties = new int[attributes.size()];
      this.floatProperties = new float[attributes.size()];
      this.attributesRef = null;
      int i = 0;

      for(Entry<String, String> e : attributes.entrySet()) {
         RenderingRuleProperty property = this.storage.PROPS.get(e.getKey());
         if (property != null) {
            props.add(property);
            String vl = e.getValue();
            if (vl.startsWith("$")) {
               if (this.attributesRef == null) {
                  this.attributesRef = new RenderingRule[attributes.size()];
               }

               this.attributesRef[i] = this.storage.getRenderingAttributeRule(vl.substring(1));
            } else if (property.isString()) {
               this.intProperties[i] = this.storage.getDictionaryValue(vl);
            } else {
               float floatVal = property.parseFloatValue(vl);
               this.floatProperties[i] = floatVal;
               this.intProperties[i] = property.parseIntValue(vl);
            }

            ++i;
         }
      }

      this.properties = props.toArray(new RenderingRuleProperty[0]);
   }

   private int getPropertyIndex(String property) {
      for(int i = 0; i < this.properties.length; ++i) {
         RenderingRuleProperty prop = this.properties[i];
         if (prop.getAttrName().equals(property)) {
            return i;
         }
      }

      return -1;
   }

   public String getStringPropertyValue(String property) {
      int i = this.getPropertyIndex(property);
      return i >= 0 ? this.storage.getStringValue(this.intProperties[i]) : null;
   }

   public float getFloatPropertyValue(String property) {
      int i = this.getPropertyIndex(property);
      return i >= 0 ? this.floatProperties[i] : 0.0F;
   }

   public String getColorPropertyValue(String property) {
      int i = this.getPropertyIndex(property);
      return i >= 0 ? Algorithms.colorToString(this.intProperties[i]) : null;
   }

   public int getIntPropertyValue(String property) {
      int i = this.getPropertyIndex(property);
      return i >= 0 ? this.intProperties[i] : -1;
   }

   protected int getIntProp(int ind) {
      return this.intProperties[ind];
   }

   protected RenderingRule getAttrProp(int ind) {
      return this.attributesRef == null ? null : this.attributesRef[ind];
   }

   protected float getFloatProp(int ind) {
      return this.floatProperties[ind];
   }

   public RenderingRuleProperty[] getProperties() {
      return this.properties;
   }

   public List<RenderingRule> getIfChildren() {
      return this.ifChildren != null ? this.ifChildren : Collections.EMPTY_LIST;
   }

   public List<RenderingRule> getIfElseChildren() {
      return this.ifElseChildren != null ? this.ifElseChildren : Collections.EMPTY_LIST;
   }

   public void addIfChildren(RenderingRule rr) {
      if (this.ifChildren == null) {
         this.ifChildren = new ArrayList<>();
      }

      this.ifChildren.add(rr);
   }

   public void addIfElseChildren(RenderingRule rr) {
      if (this.ifElseChildren == null) {
         this.ifElseChildren = new ArrayList<>();
      }

      this.ifElseChildren.add(rr);
   }

   public void addToBeginIfElseChildren(RenderingRule rr) {
      if (this.ifElseChildren == null) {
         this.ifElseChildren = new ArrayList<>();
      }

      this.ifElseChildren.add(0, rr);
   }

   public boolean isGroup() {
      return this.isGroup;
   }

   public void removeIfChildren(RenderingRule rule) {
      if (this.ifChildren != null) {
         List<RenderingRule> children = new ArrayList<>(this.ifChildren);
         children.remove(rule);
         this.ifChildren = children;
      }
   }

   public void removeIfElseChildren(RenderingRule rule) {
      if (this.ifElseChildren != null) {
         List<RenderingRule> children = new ArrayList<>(this.ifElseChildren);
         children.remove(rule);
         this.ifElseChildren = children;
      }
   }

   @Override
   public String toString() {
      StringBuilder bls = new StringBuilder();
      this.toString("", bls);
      return bls.toString();
   }

   public StringBuilder toString(String indent, StringBuilder bls) {
      if (this.isGroup) {
         bls.append("switch test [");
      } else {
         bls.append(" test [");
      }

      this.printAttrs(bls, true);
      bls.append("]");
      bls.append(" set [");
      this.printAttrs(bls, false);
      bls.append("]");

      for(RenderingRule rc : this.getIfElseChildren()) {
         String cindent = indent + "* case ";
         bls.append("\n").append(cindent);
         rc.toString(indent + "*    ", bls);
      }

      for(RenderingRule rc : this.getIfChildren()) {
         String cindent = indent + "* apply ";
         bls.append("\n").append(cindent);
         rc.toString(indent + "*    ", bls);
      }

      return bls;
   }

   protected void printAttrs(StringBuilder bls, boolean in) {
      for(RenderingRuleProperty p : this.getProperties()) {
         if (p.isInputProperty() == in) {
            bls.append(" ").append(p.getAttrName()).append("= ");
            if (p.isString()) {
               bls.append("\"").append(this.getStringPropertyValue(p.getAttrName())).append("\"");
            } else if (p.isFloat()) {
               bls.append(this.getFloatPropertyValue(p.getAttrName()));
            } else if (p.isColor()) {
               bls.append(this.getColorPropertyValue(p.getAttrName()));
            } else if (p.isIntParse()) {
               bls.append(this.getIntPropertyValue(p.getAttrName()));
            }
         }
      }
   }
}
