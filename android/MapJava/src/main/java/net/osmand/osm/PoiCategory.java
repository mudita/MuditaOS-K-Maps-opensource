package net.osmand.osm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PoiCategory extends PoiFilter {
   private List<PoiFilter> poiFilters = new ArrayList<>();
   private Set<PoiType> basemapPoi = null;
   private int regId;
   private String defaultTag;

   public PoiCategory(MapPoiTypes registry, String keyName, int regId) {
      super(registry, null, keyName);
      this.regId = regId;
   }

   public void addPoiType(PoiFilter poi) {
      this.poiFilters.add(poi);
   }

   public List<PoiFilter> getPoiFilters() {
      return this.poiFilters;
   }

   public PoiFilter getPoiFilterByName(String keyName) {
      for(PoiFilter f : this.poiFilters) {
         if (f.getKeyName().equals(keyName)) {
            return f;
         }
      }

      return null;
   }

   public String getDefaultTag() {
      return this.defaultTag == null ? this.keyName : this.defaultTag;
   }

   public void setDefaultTag(String defaultTag) {
      this.defaultTag = defaultTag;
   }

   @Override
   public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
      acceptedTypes.put(this, null);
      this.addReferenceTypes(acceptedTypes);
      return acceptedTypes;
   }

   public boolean isWiki() {
      return this.keyName.equals("osmwiki");
   }

   public boolean isRoutes() {
      return this.keyName.equals("routes");
   }

   public int ordinal() {
      return this.regId;
   }

   public void addBasemapPoi(PoiType pt) {
      if (this.basemapPoi == null) {
         this.basemapPoi = new HashSet<>();
      }

      this.basemapPoi.add(pt);
   }

   public boolean containsBasemapPoi(PoiType pt) {
      return this.basemapPoi == null ? false : this.basemapPoi.contains(pt);
   }
}
