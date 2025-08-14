package net.osmand.osm.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.osmand.data.LatLon;

public class Relation extends Entity {
   List<Relation.RelationMember> members = null;

   public Relation(long id) {
      super(id);
   }

   public void addMember(Long id, Entity.EntityType type, String role) {
      this.addMember(new Entity.EntityId(type, id), role);
   }

   public void addMember(Entity.EntityId id, String role) {
      if (this.members == null) {
         this.members = new ArrayList<>();
      }

      this.members.add(new Relation.RelationMember(id, role));
   }

   public List<Relation.RelationMember> getMembers(String role) {
      if (this.members == null) {
         return Collections.emptyList();
      } else if (role == null) {
         return this.members;
      } else {
         List<Relation.RelationMember> l = new ArrayList<>();

         for(Relation.RelationMember m : this.members) {
            if (role.equals(m.role)) {
               l.add(m);
            }
         }

         return l;
      }
   }

   public List<Entity> getMemberEntities(String role) {
      if (this.members == null) {
         return Collections.emptyList();
      } else {
         List<Entity> l = new ArrayList<>();

         for(Relation.RelationMember m : this.members) {
            if ((role == null || role.equals(m.role)) && m.entity != null) {
               l.add(m.entity);
            }
         }

         return l;
      }
   }

   public List<Relation.RelationMember> getMembers() {
      return this.members == null ? Collections.<RelationMember>emptyList() : this.members;
   }

   @Override
   public void initializeLinks(Map<Entity.EntityId, Entity> entities) {
      if (this.members != null) {
         for(Relation.RelationMember rm : this.members) {
            if (rm.entityId != null && entities.containsKey(rm.entityId)) {
               rm.entity = entities.get(rm.entityId);
            }
         }
      }
   }

   @Override
   public LatLon getLatLon() {
      return null;
   }

   public void update(Relation.RelationMember r, Entity.EntityId newEntityId) {
      r.entity = null;
      r.entityId = newEntityId;
   }

   public void updateRole(Relation.RelationMember r, String newRole) {
      r.role = newRole;
   }

   public boolean remove(Entity.EntityId key) {
      if (this.members != null) {
         for(Relation.RelationMember rm : this.members) {
            if (key.equals(rm.getEntityId())) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean remove(Relation.RelationMember key) {
      if (this.members != null) {
         Iterator<Relation.RelationMember> it = this.members.iterator();

         while(it.hasNext()) {
            Relation.RelationMember rm = it.next();
            if (rm == key) {
               it.remove();
               return true;
            }
         }
      }

      return false;
   }

   public static class RelationMember {
      private Entity.EntityId entityId;
      private Entity entity;
      private String role;

      public RelationMember(Entity.EntityId entityId, String role) {
         this.entityId = entityId;
         this.role = role;
      }

      public Entity.EntityId getEntityId() {
         return this.entityId == null && this.entity != null ? Entity.EntityId.valueOf(this.entity) : this.entityId;
      }

      public String getRole() {
         return this.role;
      }

      public Entity getEntity() {
         return this.entity;
      }

      public boolean hasName() {
         return this.entity != null && this.entity.getTag(OSMSettings.OSMTagKey.NAME) != null;
      }

      @Override
      public String toString() {
         return this.entityId.toString() + " " + this.role;
      }
   }
}
