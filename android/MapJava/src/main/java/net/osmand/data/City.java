package net.osmand.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class City extends MapObject {
   private City.CityType type = null;
   private List<Street> listOfStreets = new ArrayList<>();
   private String postcode = null;
   private City closestCity = null;
   private static long POSTCODE_INTERNAL_ID = -1000L;
   private String isin = null;

   public static City createPostcode(String postcode) {
      return new City(postcode, (long)(POSTCODE_INTERNAL_ID--));
   }

   public City(City.CityType type) {
      if (type == null) {
         throw new NullPointerException();
      } else {
         this.type = type;
      }
   }

   public City(String postcode, long id) {
      this.type = null;
      this.name = this.enName = postcode;
      this.id = id;
   }

   public String getIsInValue() {
      return this.isin;
   }

   public boolean isPostcode() {
      return this.type == null;
   }

   public String getPostcode() {
      return this.postcode;
   }

   public void setPostcode(String postcode) {
      this.postcode = postcode;
   }

   public City getClosestCity() {
      return this.closestCity;
   }

   public void setClosestCity(City closestCity) {
      this.closestCity = closestCity;
   }

   public void registerStreet(Street street) {
      this.listOfStreets.add(street);
   }

   public void unregisterStreet(Street candidate) {
      this.listOfStreets.remove(candidate);
   }

   public City.CityType getType() {
      return this.type;
   }

   public List<Street> getStreets() {
      return this.listOfStreets;
   }

   @Override
   public String toString() {
      return this.isPostcode()
         ? "Postcode : " + this.getName() + " " + this.getLocation()
         : "City [" + this.type + "] " + this.getName() + " " + this.getLocation();
   }

   public Street getStreetByName(String name) {
      for(Street s : this.listOfStreets) {
         if (s.getName().equalsIgnoreCase(name)) {
            return s;
         }
      }

      return null;
   }

   public void setIsin(String isin) {
      this.isin = isin;
   }

   public Map<Street, Street> mergeWith(City city) {
      Map<Street, Street> m = new LinkedHashMap<>();

      for(Street street : city.listOfStreets) {
         if (this.listOfStreets.contains(street)) {
            this.listOfStreets.get(this.listOfStreets.indexOf(street)).mergeWith(street);
         } else {
            Street s = new Street(this);
            s.copyNames(street);
            s.setLocation(street.getLocation().getLatitude(), street.getLocation().getLongitude());
            s.setId(street.getId());
            s.buildings.addAll(street.getBuildings());
            m.put(street, s);
            this.listOfStreets.add(s);
         }
      }

      this.copyNames(city);
      return m;
   }

   @Override
   public JSONObject toJSON() {
      return this.toJSON(true);
   }

   public JSONObject toJSON(boolean includingBuildings) {
      JSONObject json = super.toJSON();
      if (this.type != null) {
         json.put("type", this.type.name());
      }

      if (this.postcode != null) {
         json.put("postcode", this.postcode);
      }

      JSONArray listOfStreetsArr = new JSONArray();

      for(Street s : this.listOfStreets) {
         listOfStreetsArr.put(s.toJSON(includingBuildings));
      }

      json.put("listOfStreets", listOfStreetsArr);
      return json;
   }

   public static City parseJSON(JSONObject json) throws IllegalArgumentException {
      if (json.has("type")) {
         City.CityType type = City.CityType.valueOf(json.getString("type"));
         City c = new City(type);
         MapObject.parseJSON(json, c);
         if (json.has("postcode")) {
            c.postcode = json.getString("postcode");
         }

         if (json.has("listOfStreets")) {
            JSONArray streetsArr = json.getJSONArray("listOfStreets");
            c.listOfStreets = new ArrayList<>();

            for(int i = 0; i < streetsArr.length(); ++i) {
               JSONObject streetObj = streetsArr.getJSONObject(i);
               Street street = Street.parseJSON(c, streetObj);
               if (street != null) {
                  c.listOfStreets.add(street);
               }
            }
         }

         return c;
      } else {
         throw new IllegalArgumentException();
      }
   }

   public static enum CityType {
      CITY(10000.0),
      TOWN(4000.0),
      VILLAGE(1300.0),
      HAMLET(1000.0),
      SUBURB(400.0),
      BOROUGH(400.0),
      DISTRICT(400.0),
      NEIGHBOURHOOD(300.0);

      private double radius;

      private CityType(double radius) {
         this.radius = radius;
      }

      public double getRadius() {
         return this.radius;
      }

      public boolean storedAsSeparateAdminEntity() {
         return this != DISTRICT && this != NEIGHBOURHOOD && this != BOROUGH;
      }

      public static String valueToString(City.CityType t) {
         return t.toString().toLowerCase();
      }

      public static City.CityType valueFromString(String place) {
         if (place == null) {
            return null;
         } else if ("township".equals(place)) {
            return TOWN;
         } else {
            for(City.CityType t : values()) {
               if (t.name().equalsIgnoreCase(place)) {
                  return t;
               }
            }

            return null;
         }
      }
   }
}
