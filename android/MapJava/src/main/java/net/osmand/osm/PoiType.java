package net.osmand.osm;

import java.util.LinkedHashSet;
import java.util.Map;

public class PoiType extends AbstractPoiType {
   private PoiCategory category;
   private PoiFilter filter;
   private AbstractPoiType parentType;
   private PoiType referenceType;
   private String osmTag;
   private String osmTag2;
   private String osmValue;
   private String osmValue2;
   private String editTag;
   private String editValue;
   private String editTag2;
   private String editValue2;
   private boolean filterOnly;
   private String nameTag;
   private boolean text;
   private boolean nameOnly;
   private boolean relation;
   private int order = 90;

   public PoiType(MapPoiTypes poiTypes, PoiCategory category, PoiFilter filter, String keyName) {
      super(keyName, poiTypes);
      this.category = category;
      this.filter = filter;
   }

   public PoiType getReferenceType() {
      return this.referenceType;
   }

   public void setReferenceType(PoiType referenceType) {
      this.referenceType = referenceType;
   }

   public boolean isReference() {
      return this.referenceType != null;
   }

   public String getOsmTag() {
      if (this.isReference()) {
         return this.referenceType.getOsmTag();
      } else if (this.editTag != null) {
         return this.editTag;
      } else {
         return this.osmTag != null && this.osmTag.startsWith("osmand_amenity") ? "amenity" : this.osmTag;
      }
   }

   public String getRawOsmTag() {
      return this.isReference() ? this.referenceType.getOsmTag() : this.osmTag;
   }

   public void setOsmEditTagValue(String osmTag, String editValue) {
      this.editTag = osmTag;
      this.editValue = editValue;
   }

   public void setOsmEditTagValue2(String osmTag, String editValue) {
      this.editTag2 = osmTag;
      this.editValue2 = editValue;
   }

   public String getEditOsmTag() {
      if (this.isReference()) {
         return this.referenceType.getEditOsmTag();
      } else {
         return this.editTag == null ? this.getOsmTag() : this.editTag;
      }
   }

   public String getEditOsmValue() {
      if (this.isReference()) {
         return this.referenceType.getEditOsmValue();
      } else {
         return this.editValue == null ? this.getOsmValue() : this.editValue;
      }
   }

   public String getEditOsmTag2() {
      return this.isReference() ? this.referenceType.getEditOsmTag2() : this.editTag2;
   }

   public String getEditOsmValue2() {
      return this.isReference() ? this.referenceType.getEditOsmValue2() : this.editValue2;
   }

   public void setOsmTag(String osmTag) {
      this.osmTag = osmTag;
   }

   public String getOsmTag2() {
      return this.isReference() ? this.referenceType.getOsmTag2() : this.osmTag2;
   }

   public void setOsmTag2(String osmTag2) {
      this.osmTag2 = osmTag2;
   }

   public String getOsmValue() {
      return this.isReference() ? this.referenceType.getOsmValue() : this.osmValue;
   }

   public void setOsmValue(String osmValue) {
      this.osmValue = osmValue;
   }

   public String getOsmValue2() {
      return this.isReference() ? this.referenceType.getOsmValue2() : this.osmValue2;
   }

   public void setOsmValue2(String osmValue2) {
      this.osmValue2 = osmValue2;
   }

   public boolean isFilterOnly() {
      return this.filterOnly;
   }

   public void setFilterOnly(boolean filterOnly) {
      this.filterOnly = filterOnly;
   }

   public PoiCategory getCategory() {
      return this.category;
   }

   public PoiFilter getFilter() {
      return this.filter;
   }

   @Override
   public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
      if (this.isAdditional()) {
         return this.parentType.putTypes(acceptedTypes);
      } else {
         PoiType rt = this.getReferenceType();
         PoiType poiType = rt != null ? rt : this;
         if (!acceptedTypes.containsKey(poiType.category)) {
            acceptedTypes.put(poiType.category, new LinkedHashSet());
         }

         LinkedHashSet<String> set = acceptedTypes.get(poiType.category);
         if (set != null) {
            set.add(poiType.getKeyName());
         }

         return acceptedTypes;
      }
   }

   public void setAdditional(AbstractPoiType parentType) {
      this.parentType = parentType;
   }

   @Override
   public boolean isAdditional() {
      return this.parentType != null;
   }

   public AbstractPoiType getParentType() {
      return this.parentType;
   }

   public boolean isText() {
      return this.text;
   }

   public void setText(boolean text) {
      this.text = text;
   }

   public String getNameTag() {
      return this.nameTag;
   }

   public void setNameTag(String nameTag) {
      this.nameTag = nameTag;
   }

   public boolean isNameOnly() {
      return this.nameOnly;
   }

   public void setNameOnly(boolean nameOnly) {
      this.nameOnly = nameOnly;
   }

   public boolean isRelation() {
      return this.relation;
   }

   public void setRelation(boolean relation) {
      this.relation = relation;
   }

   public int getOrder() {
      return this.order;
   }

   public void setOrder(int order) {
      this.order = order;
   }

   @Override
   public String toString() {
      return "PoiType{category="
         + this.category
         + ", parentType="
         + this.parentType
         + ", referenceType="
         + this.referenceType
         + ", osmTag='"
         + this.osmTag
         + '\''
         + ", osmTag2='"
         + this.osmTag2
         + '\''
         + ", osmValue='"
         + this.osmValue
         + '\''
         + ", osmValue2='"
         + this.osmValue2
         + '\''
         + ", text="
         + this.text
         + ", nameOnly="
         + this.nameOnly
         + ", relation="
         + this.relation
         + ", order="
         + this.order
         + '}';
   }
}
