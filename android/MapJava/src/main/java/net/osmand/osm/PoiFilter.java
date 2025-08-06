package net.osmand.osm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PoiFilter extends AbstractPoiType {
   private PoiCategory pc;
   private List<PoiType> poiTypes = new ArrayList<>();
   private Map<String, PoiType> map = new LinkedHashMap<>();

   public PoiFilter(MapPoiTypes registry, PoiCategory pc, String keyName) {
      super(keyName, registry);
      this.pc = pc;
   }

   public PoiCategory getPoiCategory() {
      return this.pc;
   }

   public PoiType getPoiTypeByKeyName(String kn) {
      return this.map.get(kn);
   }

   public void addExtraPoiTypes(Map<String, PoiType> poiTypesToAdd) {
      List<PoiType> npoiTypes = null;
      Map<String, PoiType> nmap = null;

      for(PoiType poiType : poiTypesToAdd.values()) {
         String keyName = poiType.getKeyName();
         if (!this.map.containsKey(keyName) && !this.registry.isTypeForbidden(keyName)) {
            if (npoiTypes == null) {
               npoiTypes = new ArrayList<>(this.poiTypes);
               nmap = new LinkedHashMap<>(this.map);
            }

            npoiTypes.add(poiType);
            nmap.put(keyName, poiType);
         }
      }

      if (npoiTypes != null) {
         this.poiTypes = npoiTypes;
         this.map = nmap;
      }
   }

   public void addPoiType(PoiType type) {
      if (!this.registry.isTypeForbidden(type.keyName)) {
         if (!this.map.containsKey(type.getKeyName())) {
            this.poiTypes.add(type);
            this.map.put(type.getKeyName(), type);
         } else {
            PoiType prev = this.map.get(type.getKeyName());
            if (prev.isReference()) {
               this.poiTypes.remove(prev);
               this.poiTypes.add(type);
               this.map.put(type.getKeyName(), type);
            }
         }
      }
   }

   @Override
   public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
      if (!acceptedTypes.containsKey(this.pc)) {
         acceptedTypes.put(this.pc, new LinkedHashSet());
      }

      LinkedHashSet<String> set = acceptedTypes.get(this.pc);

      for(PoiType pt : this.poiTypes) {
         set.add(pt.getKeyName());
      }

      this.addReferenceTypes(acceptedTypes);
      return acceptedTypes;
   }

   protected void addReferenceTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
      for(PoiType pt : this.getPoiTypes()) {
         if (pt.isReference()) {
            PoiCategory refCat = pt.getReferenceType().getCategory();
            if (!acceptedTypes.containsKey(refCat)) {
               acceptedTypes.put(refCat, new LinkedHashSet());
            }

            LinkedHashSet<String> ls = acceptedTypes.get(refCat);
            if (ls != null) {
               ls.add(pt.getKeyName());
            }
         }
      }
   }

   public List<PoiType> getPoiTypes() {
      return this.poiTypes;
   }
}
