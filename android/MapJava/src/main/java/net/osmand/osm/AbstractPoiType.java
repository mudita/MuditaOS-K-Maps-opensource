package net.osmand.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class AbstractPoiType implements CommonPoiElement {
   protected final String keyName;
   protected final MapPoiTypes registry;
   private List<PoiType> poiAdditionals = null;
   private List<PoiType> poiAdditionalsCategorized = null;
   private boolean topVisible;
   private String lang;
   private AbstractPoiType baseLangType;
   private boolean notEditableOsm;
   private String poiAdditionalCategory;
   private List<String> excludedPoiAdditionalCategories;
   private String synonyms;
   private String enTranslation;
   private String translation;
   private String translationLanguage;

   public AbstractPoiType(String keyName, MapPoiTypes registry) {
      this.keyName = keyName;
      this.registry = registry;
   }

   public void setBaseLangType(AbstractPoiType baseLangType) {
      this.baseLangType = baseLangType;
   }

   public AbstractPoiType getBaseLangType() {
      return this.baseLangType;
   }

   public void setLang(String lang) {
      this.lang = lang;
   }

   public String getLang() {
      return this.lang;
   }

   public String getKeyName() {
      return this.keyName;
   }

   public String getIconKeyName() {
      String kn = this.getKeyName();
      if (kn.startsWith("osmand_")) {
         kn = kn.substring("osmand_".length());
      }

      return kn.replace(':', '_');
   }

   public void setTopVisible(boolean topVisible) {
      this.topVisible = topVisible;
   }

   public boolean isTopVisible() {
      return this.topVisible;
   }

   public boolean isAdditional() {
      return this instanceof PoiType && this.isAdditional();
   }

   public String getTranslation() {
      String currentLanguage = registry.getTranslationLanguage();
      if (this.translation == null || !currentLanguage.equals(translationLanguage)) {
         this.translation = this.registry.getTranslation(this);
         translationLanguage = currentLanguage;
      }

      return this.translation;
   }

   public String getSynonyms() {
      if (this.synonyms == null) {
         this.synonyms = this.registry.getSynonyms(this);
      }

      return this.synonyms;
   }

   public String getEnTranslation() {
      if (this.enTranslation == null) {
         this.enTranslation = this.registry.getEnTranslation(this);
      }

      return this.enTranslation;
   }

   public String getPoiAdditionalCategoryTranslation() {
      return this.poiAdditionalCategory != null ? this.registry.getPoiTranslation(this.poiAdditionalCategory) : null;
   }

   public void addPoiAdditional(PoiType tp) {
      if (this.poiAdditionals == null) {
         this.poiAdditionals = new ArrayList<>();
      }

      this.poiAdditionals.add(tp);
      if (tp.getPoiAdditionalCategory() != null) {
         if (this.poiAdditionalsCategorized == null) {
            this.poiAdditionalsCategorized = new ArrayList<>();
         }

         this.poiAdditionalsCategorized.add(tp);
      }
   }

   public void addPoiAdditionalsCategorized(List<PoiType> tps) {
      if (this.poiAdditionals == null) {
         this.poiAdditionals = new ArrayList<>();
      }

      this.poiAdditionals.addAll(tps);
      if (this.poiAdditionalsCategorized == null) {
         this.poiAdditionalsCategorized = new ArrayList<>();
      }

      this.poiAdditionalsCategorized.addAll(tps);
   }

   public List<PoiType> getPoiAdditionals() {
      return this.poiAdditionals == null ? Collections.<PoiType>emptyList() : this.poiAdditionals;
   }

   public List<PoiType> getPoiAdditionalsCategorized() {
      return this.poiAdditionalsCategorized == null ? Collections.<PoiType>emptyList() : this.poiAdditionalsCategorized;
   }

   public boolean isNotEditableOsm() {
      return this.notEditableOsm;
   }

   public void setNotEditableOsm(boolean notEditableOsm) {
      this.notEditableOsm = notEditableOsm;
   }

   public String getPoiAdditionalCategory() {
      return this.poiAdditionalCategory;
   }

   public void setPoiAdditionalCategory(String poiAdditionalCategory) {
      this.poiAdditionalCategory = poiAdditionalCategory;
   }

   public List<String> getExcludedPoiAdditionalCategories() {
      return this.excludedPoiAdditionalCategories;
   }

   public void addExcludedPoiAdditionalCategories(String[] excludedPoiAdditionalCategories) {
      if (this.excludedPoiAdditionalCategories == null) {
         this.excludedPoiAdditionalCategories = new ArrayList<>();
      }

      Collections.addAll(this.excludedPoiAdditionalCategories, excludedPoiAdditionalCategories);
   }

   public abstract Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> var1);

   @Override
   public String toString() {
      return this.keyName;
   }
}
