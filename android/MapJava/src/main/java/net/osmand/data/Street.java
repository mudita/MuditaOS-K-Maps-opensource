package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.osmand.util.Algorithms;
import org.json.JSONArray;
import org.json.JSONObject;

public class Street extends MapObject {
   protected List<Building> buildings = new ArrayList<>();
   protected List<Street> intersectedStreets = null;
   protected final City city;

   public Street(City city) {
      this.city = city;
   }

   public void addBuilding(Building building) {
      this.buildings.add(building);
   }

   public List<Street> getIntersectedStreets() {
      return this.intersectedStreets == null ? Collections.<Street>emptyList() : this.intersectedStreets;
   }

   public void addIntersectedStreet(Street s) {
      if (this.intersectedStreets == null) {
         this.intersectedStreets = new ArrayList<>();
      }

      this.intersectedStreets.add(s);
   }

   public void addBuildingCheckById(Building building) {
      for(Building b : this.buildings) {
         if (b.equals(building)) {
            return;
         }
      }

      this.buildings.add(building);
   }

   public List<Building> getBuildings() {
      return this.buildings;
   }

   public City getCity() {
      return this.city;
   }

   public void sortBuildings() {
      Collections.sort(this.buildings, new Comparator<Building>() {
         public int compare(Building o1, Building o2) {
            String s1 = o1.getName();
            String s2 = o2.getName();
            int i1 = Algorithms.extractFirstIntegerNumber(s1);
            int i2 = Algorithms.extractFirstIntegerNumber(s2);
            if (i1 == i2) {
               String t1 = Algorithms.extractIntegerSuffix(s1);
               String t2 = Algorithms.extractIntegerSuffix(s2);
               return t1.compareTo(t2);
            } else {
               return i1 - i2;
            }
         }
      });
   }

   public void mergeWith(Street street) {
      this.buildings.addAll(street.getBuildings());
      this.copyNames(street);
   }

   public String getNameWithoutCityPart(String lang, boolean transliterate) {
      String nm = this.getName(lang, transliterate);
      int t = nm.lastIndexOf(40);
      return t > 0 ? nm.substring(0, t) : nm;
   }

   @Override
   public JSONObject toJSON() {
      return this.toJSON(true);
   }

   public JSONObject toJSON(boolean includingBuildings) {
      JSONObject json = super.toJSON();
      if (this.buildings.size() > 0 && includingBuildings) {
         JSONArray buildingsArr = new JSONArray();

         for(Building b : this.buildings) {
            buildingsArr.put(b.toJSON());
         }

         json.put("buildings", buildingsArr);
      }

      if (this.intersectedStreets != null) {
         JSONArray intersectedStreetsArr = new JSONArray();

         for(Street s : this.intersectedStreets) {
            intersectedStreetsArr.put(s.toJSON());
         }

         json.put("intersectedStreets", intersectedStreetsArr);
      }

      return json;
   }

   public static Street parseJSON(City city, JSONObject json) throws IllegalArgumentException {
      Street s = new Street(city);
      MapObject.parseJSON(json, s);
      if (json.has("buildings")) {
         JSONArray buildingsArr = json.getJSONArray("buildings");
         s.buildings = new ArrayList<>();

         for(int i = 0; i < buildingsArr.length(); ++i) {
            JSONObject buildingObj = buildingsArr.getJSONObject(i);
            Building building = Building.parseJSON(buildingObj);
            if (building != null) {
               s.buildings.add(building);
            }
         }
      }

      if (json.has("intersectedStreets")) {
         JSONArray streetsArr = json.getJSONArray("intersectedStreets");
         s.intersectedStreets = new ArrayList<>();

         for(int i = 0; i < streetsArr.length(); ++i) {
            JSONObject streetObj = streetsArr.getJSONObject(i);
            Street street = parseJSON(city, streetObj);
            if (street != null) {
               s.intersectedStreets.add(street);
            }
         }
      }

      return s;
   }
}
