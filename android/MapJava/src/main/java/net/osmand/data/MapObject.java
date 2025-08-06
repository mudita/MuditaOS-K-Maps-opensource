package net.osmand.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.osm.CommonPoiElement;
import net.osmand.util.Algorithms;
import net.osmand.util.TransliterationHelper;
import org.json.JSONObject;

public abstract class MapObject implements Comparable<MapObject>, CommonPoiElement {
   public static final MapObject.MapObjectComparator BY_NAME_COMPARATOR = new MapObject.MapObjectComparator();
   public static final byte AMENITY_ID_RIGHT_SHIFT = 1;
   public static final byte WAY_MODULO_REMAINDER = 1;
   protected String name = null;
   protected String enName = null;
   protected Map<String, String> names = null;
   protected LatLon location = null;
   protected int fileOffset = 0;
   protected Long id = null;
   private Object referenceFile = null;

   public void setId(Long id) {
      this.id = id;
   }

   public Long getId() {
      return this.id != null ? this.id : null;
   }

   public String getName() {
      return this.name != null ? this.unzipContent(this.name) : "";
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setName(String lang, String name) {
      if (Algorithms.isEmpty(lang)) {
         this.setName(name);
      } else if (lang.equals("en")) {
         this.setEnName(name);
      } else {
         if (this.names == null) {
            this.names = new HashMap<>();
         }

         this.names.put(lang, this.unzipContent(name));
      }
   }

   public void setNames(Map<String, String> name) {
      if (name != null) {
         if (this.names == null) {
            this.names = new HashMap<>();
         }

         this.names.putAll(name);
      }
   }

   public Map<String, String> getNamesMap(boolean includeEn) {
      if ((!includeEn || Algorithms.isEmpty(this.enName)) && this.names == null) {
         return Collections.emptyMap();
      } else {
         Map<String, String> mp = new HashMap<>();
         if (this.names != null) {
            for(Entry<String, String> e : this.names.entrySet()) {
               mp.put(e.getKey(), this.unzipContent(e.getValue()));
            }
         }

         if (includeEn && !Algorithms.isEmpty(this.enName)) {
            mp.put("en", this.unzipContent(this.enName));
         }

         return mp;
      }
   }

   public List<String> getOtherNames() {
      return this.getOtherNames(false);
   }

   public List<String> getOtherNames(boolean transliterate) {
      List<String> l = new ArrayList<>();
      String enName = this.getEnName(transliterate);
      if (!Algorithms.isEmpty(enName)) {
         l.add(enName);
      }

      if (this.names != null) {
         l.addAll(this.names.values());
      }

      return l;
   }

   public void copyNames(String otherName, String otherEnName, Map<String, String> otherNames, boolean overwrite) {
      if (!Algorithms.isEmpty(otherName) && (overwrite || Algorithms.isEmpty(this.name))) {
         this.name = otherName;
      }

      if (!Algorithms.isEmpty(otherEnName) && (overwrite || Algorithms.isEmpty(this.enName))) {
         this.enName = otherEnName;
      }

      if (!Algorithms.isEmpty(otherNames)) {
         if (otherNames.containsKey("name:en")) {
            this.enName = otherNames.get("name:en");
         } else if (otherNames.containsKey("en")) {
            this.enName = otherNames.get("en");
         }

         for(Entry<String, String> e : otherNames.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("name:")) {
               key = key.substring("name:".length());
            }

            if (this.names == null) {
               this.names = new HashMap<>();
            }

            if (overwrite || Algorithms.isEmpty(this.names.get(key))) {
               this.names.put(key, e.getValue());
            }
         }
      }
   }

   public void copyNames(String otherName, String otherEnName, Map<String, String> otherNames) {
      this.copyNames(otherName, otherEnName, otherNames, false);
   }

   public void copyNames(MapObject s, boolean copyName, boolean copyEnName, boolean overwrite) {
      this.copyNames(copyName ? s.name : null, copyEnName ? s.enName : null, s.names, overwrite);
   }

   public void copyNames(MapObject s) {
      this.copyNames(s, true, true, false);
   }

   public String getName(String lang) {
      return this.getName(lang, false);
   }

   public String getName(String lang, boolean transliterate) {
      if (lang != null && lang.length() > 0) {
         if (lang.equals("en")) {
            String enName = this.getEnName(transliterate);
            return !Algorithms.isEmpty(enName) ? enName : this.getName();
         }

         if (this.names != null) {
            String nm = this.names.get(lang);
            if (!Algorithms.isEmpty(nm)) {
               return this.unzipContent(nm);
            }

            if (transliterate) {
               return TransliterationHelper.transliterate(this.getName());
            }
         }
      }

      return this.getName();
   }

   public String getEnName(boolean transliterate) {
      if (!Algorithms.isEmpty(this.enName)) {
         return this.unzipContent(this.enName);
      } else {
         return !Algorithms.isEmpty(this.getName()) && transliterate ? TransliterationHelper.transliterate(this.getName()) : "";
      }
   }

   public void setEnName(String enName) {
      this.enName = enName;
   }

   public LatLon getLocation() {
      return this.location;
   }

   public void setLocation(double latitude, double longitude) {
      this.location = new LatLon(latitude, longitude);
   }

   public void setLocation(LatLon loc) {
      this.location = loc;
   }

   public int compareTo(MapObject o) {
      return OsmAndCollator.primaryCollator().compare(this.getName(), o.getName());
   }

   public int getFileOffset() {
      return this.fileOffset;
   }

   public void setFileOffset(int fileOffset) {
      this.fileOffset = fileOffset;
   }

   public String toStringEn() {
      return this.getClass().getSimpleName() + ":" + this.getEnName(true);
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName() + " " + this.name + "(" + this.id + ")";
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      return 31 * result + (this.id == null ? 0 : this.id.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         MapObject other = (MapObject)obj;
         if (this.id == null) {
            if (other.id != null) {
               return false;
            }
         } else if (!this.id.equals(other.id)) {
            return false;
         }

         return true;
      }
   }

   public boolean compareObject(MapObject thatObj) {
      if (this == thatObj) {
         return true;
      } else if (thatObj != null && this.id != null && thatObj.id != null) {
         return this.id == thatObj.id
            && Algorithms.objectEquals(this.getLocation(), thatObj.getLocation())
            && Algorithms.objectEquals(this.getName(), thatObj.getName())
            && Algorithms.objectEquals(this.getNamesMap(true), thatObj.getNamesMap(true));
      } else {
         return false;
      }
   }

   public void setReferenceFile(Object referenceFile) {
      this.referenceFile = referenceFile;
   }

   public Object getReferenceFile() {
      return this.referenceFile;
   }

   public JSONObject toJSON() {
      JSONObject json = new JSONObject();
      json.put("name", this.unzipContent(this.name));
      json.put("enName", this.unzipContent(this.enName));
      if (this.names != null && this.names.size() > 0) {
         JSONObject namesObj = new JSONObject();

         for(Entry<String, String> e : this.names.entrySet()) {
            namesObj.put(e.getKey(), this.unzipContent(e.getValue()));
         }

         json.put("names", namesObj);
      }

      if (this.location != null) {
         json.put("lat", String.format(Locale.US, "%.5f", this.location.getLatitude()));
         json.put("lon", String.format(Locale.US, "%.5f", this.location.getLongitude()));
      }

      json.put("id", this.id);
      return json;
   }

   String unzipContent(String str) {
      if (this.isContentZipped(str)) {
         try {
            int ind = 4;
            byte[] bytes = new byte[str.length() - ind];

            for(int i = ind; i < str.length(); ++i) {
               char ch = str.charAt(i);
               bytes[i - ind] = (byte)(ch - 128 - 32);
            }

            GZIPInputStream gzn = new GZIPInputStream(new ByteArrayInputStream(bytes));
            BufferedReader br = new BufferedReader(new InputStreamReader(gzn, "UTF-8"));
            StringBuilder bld = new StringBuilder();

            String s;
            while((s = br.readLine()) != null) {
               bld.append(s);
            }

            br.close();
            str = bld.toString();
            if (this.isContentZipped(str)) {
               str = this.unzipContent(str);
            }
         } catch (IOException var8) {
            var8.printStackTrace();
         }
      }

      return str;
   }

   boolean isContentZipped(String str) {
      return str != null && str.startsWith(" gz ");
   }

   protected static void parseJSON(JSONObject json, MapObject o) {
      if (json.has("name")) {
         o.name = json.getString("name");
      }

      if (json.has("enName")) {
         o.enName = json.getString("enName");
      }

      if (json.has("names")) {
         JSONObject namesObj = json.getJSONObject("names");
         o.names = new HashMap<>();
         Iterator<String> iterator = namesObj.keys();

         while(iterator.hasNext()) {
            String key = iterator.next();
            String value = namesObj.getString(key);
            o.names.put(key, value);
         }
      }

      if (json.has("lat") && json.has("lon")) {
         o.location = new LatLon(json.getDouble("lat"), json.getDouble("lon"));
      }

      if (json.has("id")) {
         o.id = json.getLong("id");
      }
   }

   public static class MapObjectComparator implements Comparator<MapObject> {
      private final String l;
      Collator collator = OsmAndCollator.primaryCollator();
      private boolean transliterate;

      public MapObjectComparator() {
         this.l = null;
      }

      public MapObjectComparator(String lang, boolean transliterate) {
         this.l = lang;
         this.transliterate = transliterate;
      }

      public int compare(MapObject o1, MapObject o2) {
         if (o1 == null ^ o2 == null) {
            return o1 == null ? -1 : 1;
         } else {
            return o1 == o2 ? 0 : this.collator.compare(o1.getName(this.l, this.transliterate), o2.getName(this.l, this.transliterate));
         }
      }

      public boolean areEqual(MapObject o1, MapObject o2) {
         if (o1 == null ^ o2 == null) {
            return false;
         } else {
            return o1 == o2 ? true : this.collator.equals(o1.getName(this.l, this.transliterate), o2.getName(this.l, this.transliterate));
         }
      }
   }
}
