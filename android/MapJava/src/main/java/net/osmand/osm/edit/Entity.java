package net.osmand.osm.edit;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

public abstract class Entity implements Serializable {
   private Map<String, String> tags = null;
   private Set<String> changedTags;
   private final long id;
   private boolean dataLoaded;
   private int modify;
   private int version;
   private double latitude;
   private double longitude;
   public static final int MODIFY_UNKNOWN = 0;
   public static final int MODIFY_DELETED = -1;
   public static final int MODIFY_MODIFIED = 1;
   public static final int MODIFY_CREATED = 2;
   public static final String POI_TYPE_TAG = "poi_type_tag";
   public static final String REMOVE_TAG_PREFIX = "----";

   public Entity(long id) {
      this.id = id;
   }

   public Entity(long id, double latitude, double longitude) {
      this.id = id;
      this.latitude = latitude;
      this.longitude = longitude;
   }

   public Entity(Entity copy, long id) {
      this.id = id;
      this.copyTags(copy);
      this.dataLoaded = copy.dataLoaded;
      this.latitude = copy.latitude;
      this.longitude = copy.longitude;
   }

   public void copyTags(Entity copy) {
      for(String t : copy.getTagKeySet()) {
         this.putTagNoLC(t, copy.getTag(t));
      }
   }

   public Set<String> getChangedTags() {
      return this.changedTags;
   }

   public void setChangedTags(Set<String> changedTags) {
      this.changedTags = changedTags;
   }

   public int getModify() {
      return this.modify;
   }

   public void setModify(int modify) {
      this.modify = modify;
   }

   public long getId() {
      return this.id;
   }

   public double getLatitude() {
      return this.latitude;
   }

   public double getLongitude() {
      return this.longitude;
   }

   public void setLatitude(double latitude) {
      this.latitude = latitude;
   }

   public void setLongitude(double longitude) {
      this.longitude = longitude;
   }

   public String removeTag(String key) {
      return this.tags != null ? this.tags.remove(key) : null;
   }

   public void removeTags(String... keys) {
      if (this.tags != null) {
         for(String key : keys) {
            this.tags.remove(key);
         }
      }
   }

   public String putTag(String key, String value) {
      return this.putTagNoLC(key.toLowerCase(), value);
   }

   public String putTagNoLC(String key, String value) {
      if (this.tags == null) {
         this.tags = new LinkedHashMap<>();
      }

      return this.tags.put(key, value);
   }

   public void replaceTags(Map<String, String> toPut) {
      this.tags = new LinkedHashMap<>(toPut);
   }

   public String getTag(OSMSettings.OSMTagKey key) {
      return this.getTag(key.getValue());
   }

   public String getTag(String key) {
      return this.tags == null ? null : this.tags.get(key);
   }

   public Map<String, String> getNameTags() {
      Map<String, String> result = new LinkedHashMap<>();

      for(Entry<String, String> e : this.tags.entrySet()) {
         if (e.getKey().startsWith("name:")) {
            result.put(e.getKey(), e.getValue());
         }
      }

      return result;
   }

   public int getVersion() {
      return this.version;
   }

   public void setVersion(int version) {
      this.version = version;
   }

   public Map<String, String> getTags() {
      return this.tags == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(this.tags);
   }

   public boolean isNotValid(String tag) {
      String val = this.getTag(tag);
      return val == null || val.length() == 0 || tag.length() == 0 || tag.startsWith("----") || tag.equals("poi_type_tag");
   }

   public Collection<String> getTagKeySet() {
      return (Collection<String>)(this.tags == null ? Collections.emptyList() : this.tags.keySet());
   }

   public abstract void initializeLinks(Map<Entity.EntityId, Entity> var1);

   public abstract LatLon getLatLon();

   public boolean isVirtual() {
      return this.id < 0L;
   }

   public String getOsmUrl() {
      return Entity.EntityId.valueOf(this).getOsmUrl();
   }

   @Override
   public String toString() {
      return Entity.EntityId.valueOf(this).toString();
   }

   @Override
   public int hashCode() {
      return this.id < 0L ? System.identityHashCode(this) : (int)this.id;
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
         Entity other = (Entity)obj;
         if (this.id != other.id) {
            return false;
         } else {
            return this.id >= 0L;
         }
      }
   }

   public Set<String> getIsInNames() {
      String values = this.getTag(OSMSettings.OSMTagKey.IS_IN);
      if (values == null) {
         String city = this.getTag(OSMSettings.OSMTagKey.ADDR_CITY);
         String place = this.getTag(OSMSettings.OSMTagKey.ADDR_PLACE);
         if (!Algorithms.isEmpty(city)) {
            return Collections.singleton(city.trim());
         } else {
            return !Algorithms.isEmpty(place) ? Collections.singleton(place.trim()) : Collections.<String>emptySet();
         }
      } else if (values.indexOf(59) == -1) {
         return Collections.singleton(values.trim());
      } else {
         String[] splitted = values.split(";");
         Set<String> set = new HashSet<>(splitted.length);

         for(int i = 0; i < splitted.length; ++i) {
            set.add(splitted[i].trim());
         }

         return set;
      }
   }

   public void entityDataLoaded() {
      this.dataLoaded = true;
   }

   public boolean isDataLoaded() {
      return this.dataLoaded;
   }

   public Map<String, String> getModifiableTags() {
      return this.tags == null ? Collections.<String, String>emptyMap() : this.tags;
   }

   public boolean compareEntity(Entity thatObj) {
      if (this == thatObj) {
         return true;
      } else {
         return this.id == thatObj.id
            && Math.abs(this.latitude - thatObj.latitude) < 1.0E-5
            && Math.abs(this.longitude - thatObj.longitude) < 1.0E-5
            && Algorithms.objectEquals(this.tags, thatObj.tags);
      }
   }

   public static class EntityId {
      private final Entity.EntityType type;
      private final Long id;

      public EntityId(Entity.EntityType type, Long id) {
         this.type = type;
         this.id = id;
      }

      public static Entity.EntityId valueOf(Entity e) {
         return new Entity.EntityId(Entity.EntityType.valueOf(e), e.getId());
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + (this.id == null ? 0 : this.id.hashCode());
         return 31 * result + (this.type == null ? 0 : this.type.hashCode());
      }

      @Override
      public String toString() {
         return this.type + " " + this.id;
      }

      public Entity.EntityType getType() {
         return this.type;
      }

      public Long getId() {
         return this.id;
      }

      public String getOsmUrl() {
         String browseUrl = "https://www.openstreetmap.org/browse/";
         if (this.type == Entity.EntityType.NODE) {
            return "https://www.openstreetmap.org/browse/node/" + this.id;
         } else {
            return this.type == Entity.EntityType.WAY ? "https://www.openstreetmap.org/browse/way/" + this.id : null;
         }
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
            Entity.EntityId other = (Entity.EntityId)obj;
            if (this.id == null) {
               if (other.id != null) {
                  return false;
               }
            } else if (!this.id.equals(other.id)) {
               return false;
            }

            if (this.type == null) {
               if (other.type != null) {
                  return false;
               }
            } else if (!this.type.equals(other.type)) {
               return false;
            }

            return true;
         }
      }
   }

   public static enum EntityType {
      NODE,
      WAY,
      RELATION,
      WAY_BOUNDARY;

      public static Entity.EntityType valueOf(Entity e) {
         if (e instanceof Node) {
            return NODE;
         } else if (e instanceof Way) {
            return WAY;
         } else {
            return e instanceof Relation ? RELATION : null;
         }
      }
   }
}
