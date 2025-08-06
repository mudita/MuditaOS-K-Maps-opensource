package net.osmand.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.osmand.util.Algorithms;
import org.json.JSONObject;

public class Building extends MapObject {
   private String postcode;
   private LatLon latLon2;
   private Building.BuildingInterpolation interpolationType;
   private int interpolationInterval;
   private String name2;
   private Map<String, LatLon> entrances = null;

   public String getPostcode() {
      return this.postcode;
   }

   public Map<String, LatLon> getEntrances() {
      return this.entrances == null ? Collections.<String, LatLon>emptyMap() : this.entrances;
   }

   public void addEntrance(String ref, LatLon location) {
      if (this.entrances == null) {
         this.entrances = new LinkedHashMap<>();
      }

      this.entrances.put(ref, location);
   }

   public int getInterpolationInterval() {
      return this.interpolationInterval;
   }

   public void setInterpolationInterval(int interpolationNumber) {
      this.interpolationInterval = interpolationNumber;
   }

   public Building.BuildingInterpolation getInterpolationType() {
      return this.interpolationType;
   }

   public void setInterpolationType(Building.BuildingInterpolation interpolationType) {
      this.interpolationType = interpolationType;
   }

   public LatLon getLatLon2() {
      return this.latLon2;
   }

   public void setLatLon2(LatLon latlon2) {
      this.latLon2 = latlon2;
   }

   public String getName2() {
      return this.name2;
   }

   public void setName2(String name2) {
      this.name2 = name2;
   }

   public void setPostcode(String postcode) {
      this.postcode = postcode;
   }

   @Override
   public String getName(String lang) {
      String fname = super.getName(lang);
      if (this.interpolationInterval != 0) {
         return fname + "-" + this.name2 + " (+" + this.interpolationInterval + ") ";
      } else {
         return this.interpolationType != null ? fname + "-" + this.name2 + " (" + this.interpolationType.toString().toLowerCase() + ") " : this.name;
      }
   }

   public float interpolation(String hno) {
      if (this.getInterpolationType() == null && this.getInterpolationInterval() <= 0) {
         return -1.0F;
      } else {
         int num = Algorithms.extractFirstIntegerNumber(hno);
         String fname = super.getName();
         int numB = Algorithms.extractFirstIntegerNumber(fname);
         int numT = numB;
         String sname = this.getName2();
         if (this.getInterpolationType() == Building.BuildingInterpolation.ALPHABETIC) {
            if (num != numB) {
               return -1.0F;
            } else {
               int hint = hno.charAt(hno.length() - 1);
               int fch = fname.charAt(fname.length() - 1);
               int sch = sname.charAt(sname.length() - 1);
               if (fch == sch) {
                  return -1.0F;
               } else {
                  float res = ((float)hint - (float)fch) / ((float)sch - (float)fch);
                  return !(res > 1.0F) && !(res < 0.0F) ? res : -1.0F;
               }
            }
         } else if (num >= numB) {
            if (fname.contains("-") && sname == null) {
               int l = fname.indexOf(45);
               sname = fname.substring(l + 1, fname.length());
            }

            if (sname != null) {
               numT = Algorithms.extractFirstIntegerNumber(sname);
               if (numT < num) {
                  return -1.0F;
               }
            }

            if (this.getInterpolationType() == Building.BuildingInterpolation.EVEN && num % 2 == 1) {
               return -1.0F;
            } else if (this.getInterpolationType() == Building.BuildingInterpolation.ODD && num % 2 == 0) {
               return -1.0F;
            } else if (this.getInterpolationInterval() != 0 && (num - numB) % this.getInterpolationInterval() != 0) {
               return -1.0F;
            } else {
               return numT > numB ? ((float)num - (float)numB) / ((float)numT - (float)numB) : 0.0F;
            }
         } else {
            return -1.0F;
         }
      }
   }

   protected boolean checkNameAsInterpolation() {
      String nm = super.getName();
      boolean interpolation = nm.contains("-");
      if (interpolation) {
         for(int i = 0; i < nm.length(); ++i) {
            if ((nm.charAt(i) < '0' || nm.charAt(i) > '9') && nm.charAt(i) != '-') {
               interpolation = false;
               break;
            }
         }
      }

      return interpolation;
   }

   public boolean belongsToInterpolation(String hno) {
      return this.interpolation(hno) >= 0.0F;
   }

   @Override
   public String toString() {
      if (this.interpolationInterval != 0) {
         return this.name + "-" + this.name2 + " (+" + this.interpolationInterval + ") ";
      } else {
         return this.interpolationType != null ? this.name + "-" + this.name2 + " (" + this.interpolationType + ") " : this.name;
      }
   }

   public LatLon getLocation(float interpolation) {
      LatLon loc = this.getLocation();
      LatLon latLon2 = this.getLatLon2();
      if (latLon2 != null) {
         double lat1 = loc.getLatitude();
         double lat2 = latLon2.getLatitude();
         double lon1 = loc.getLongitude();
         double lon2 = latLon2.getLongitude();
         return new LatLon((double)interpolation * (lat2 - lat1) + lat1, (double)interpolation * (lon2 - lon1) + lon1);
      } else {
         return loc;
      }
   }

   @Override
   public boolean equals(Object o) {
      boolean res = super.equals(o);
      return res && o instanceof Building ? Algorithms.stringsEqual(((MapObject)o).getName(), this.getName()) : res;
   }

   public String getInterpolationName(double coeff) {
      if (!Algorithms.isEmpty(this.getName2())) {
         int fi = Algorithms.extractFirstIntegerNumber(this.getName());
         int si = Algorithms.extractFirstIntegerNumber(this.getName2());
         if (si != 0 && fi != 0) {
            int num = (int)((double)fi + (double)(si - fi) * coeff);
            Building.BuildingInterpolation type = this.getInterpolationType();
            if (type == Building.BuildingInterpolation.EVEN || type == Building.BuildingInterpolation.ODD) {
               if (num % 2 == (type == Building.BuildingInterpolation.EVEN ? 1 : 0)) {
                  --num;
               }
            } else if (this.getInterpolationInterval() > 0) {
               int intv = this.getInterpolationInterval();
               if ((num - fi) % intv != 0) {
                  num = (num - fi) / intv * intv + fi;
               }
            }

            return num + "";
         }
      }

      return "";
   }

   @Override
   public JSONObject toJSON() {
      JSONObject json = super.toJSON();
      json.put("postcode", this.postcode);
      if (this.latLon2 != null) {
         json.put("lat2", String.format(Locale.US, "%.5f", this.latLon2.getLatitude()));
         json.put("lon2", String.format(Locale.US, "%.5f", this.latLon2.getLongitude()));
      }

      if (this.interpolationType != null) {
         json.put("interpolationType", this.interpolationType.name());
      }

      if (this.interpolationInterval != 0) {
         json.put("interpolationInterval", this.interpolationInterval);
      }

      json.put("name2", this.name2);
      return json;
   }

   public static Building parseJSON(JSONObject json) throws IllegalArgumentException {
      Building b = new Building();
      MapObject.parseJSON(json, b);
      if (json.has("postcode")) {
         b.postcode = json.getString("postcode");
      }

      if (json.has("lat2") && json.has("lon2")) {
         b.latLon2 = new LatLon(json.getDouble("lat2"), json.getDouble("lon2"));
      }

      if (json.has("interpolationType")) {
         b.interpolationType = Building.BuildingInterpolation.valueOf(json.getString("interpolationType"));
      }

      if (json.has("interpolationInterval")) {
         b.interpolationInterval = json.getInt("interpolationInterval");
      }

      if (json.has("name2")) {
         b.name2 = json.getString("name2");
      }

      return b;
   }

   public static enum BuildingInterpolation {
      ALL(-1),
      EVEN(-2),
      ODD(-3),
      ALPHABETIC(-4);

      private final int val;

      private BuildingInterpolation(int val) {
         this.val = val;
      }

      public int getValue() {
         return this.val;
      }

      public static Building.BuildingInterpolation fromValue(int i) {
         for(Building.BuildingInterpolation b : values()) {
            if (b.val == i) {
               return b;
            }
         }

         return null;
      }
   }
}
