package net.osmand.osm.edit;

import gnu.trove.list.array.TLongArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;

public class Way extends Entity {
   private TLongArrayList nodeIds = null;
   private List<Node> nodes = null;

   public Way(long id) {
      super(id);
   }

   public Way(Way w, long id) {
      super(w, id);
      if (w.nodeIds != null) {
         this.nodeIds = new TLongArrayList(w.nodeIds);
      }

      if (w.nodes != null) {
         this.nodes = new ArrayList<>(w.nodes);
      }
   }

   public Way(long id, List<Node> nodes) {
      super(id);
      this.nodes = new ArrayList<>(nodes);
      this.nodeIds = new TLongArrayList(nodes.size());

      for(Node n : nodes) {
         this.nodeIds.add(n.getId());
      }
   }

   public Way(long id, TLongArrayList nodeIds, double lat, double lon) {
      super(id, lat, lon);
      this.nodeIds = nodeIds;
   }

   public void addNode(long id) {
      if (this.nodeIds == null) {
         this.nodeIds = new TLongArrayList();
      }

      this.nodeIds.add(id);
   }

   public long getFirstNodeId() {
      return this.nodeIds == null ? -1L : this.nodeIds.get(0);
   }

   public long getLastNodeId() {
      return this.nodeIds == null ? -1L : this.nodeIds.get(this.nodeIds.size() - 1);
   }

   public Node getFirstNode() {
      return this.nodes != null && this.nodes.size() != 0 ? this.nodes.get(0) : null;
   }

   public Node getLastNode() {
      return this.nodes != null && this.nodes.size() != 0 ? this.nodes.get(this.nodes.size() - 1) : null;
   }

   public void addNode(Node n) {
      if (this.nodeIds == null) {
         this.nodeIds = new TLongArrayList();
      }

      if (this.nodes == null) {
         this.nodes = new ArrayList<>();
      }

      this.nodeIds.add(n.getId());
      this.nodes.add(n);
   }

   public void addNode(Node n, int index) {
      if (this.nodeIds == null) {
         this.nodeIds = new TLongArrayList();
      }

      if (this.nodes == null) {
         this.nodes = new ArrayList<>();
      }

      this.nodeIds.insert(index, n.getId());
      this.nodes.add(index, n);
   }

   public long removeNodeByIndex(int i) {
      if (this.nodeIds == null) {
         return -1L;
      } else {
         long toReturn = this.nodeIds.removeAt(i);
         if (this.nodes != null && this.nodes.size() > i) {
            this.nodes.remove(i);
         }

         return toReturn;
      }
   }

   public TLongArrayList getNodeIds() {
      return this.nodeIds == null ? new TLongArrayList(0) : this.nodeIds;
   }

   public List<Entity.EntityId> getEntityIds() {
      if (this.nodeIds == null) {
         return Collections.emptyList();
      } else {
         List<Entity.EntityId> ls = new ArrayList<>();

         for(int i = 0; i < this.nodeIds.size(); ++i) {
            ls.add(new Entity.EntityId(Entity.EntityType.NODE, this.nodeIds.get(i)));
         }

         return ls;
      }
   }

   public List<Node> getNodes() {
      return this.nodes == null ? Collections.<Node>emptyList() : this.nodes;
   }

   @Override
   public void initializeLinks(Map<Entity.EntityId, Entity> entities) {
      if (this.nodeIds != null) {
         if (this.nodes == null) {
            this.nodes = new ArrayList<>();
         } else {
            this.nodes.clear();
         }

         int nIsize = this.nodeIds.size();

         for(int i = 0; i < nIsize; ++i) {
            this.nodes.add((Node)entities.get(new Entity.EntityId(Entity.EntityType.NODE, this.nodeIds.get(i))));
         }
      }
   }

   public QuadRect getLatLonBBox() {
      QuadRect qr = null;
      if (this.nodes != null) {
         for(Node n : this.nodes) {
            if (qr == null) {
               qr = new QuadRect();
               qr.left = (double)((float)n.getLongitude());
               qr.right = (double)((float)n.getLongitude());
               qr.top = (double)((float)n.getLatitude());
               qr.bottom = (double)((float)n.getLatitude());
            }

            if (n.getLongitude() < qr.left) {
               qr.left = (double)((float)n.getLongitude());
            } else if (n.getLongitude() > qr.right) {
               qr.right = (double)((float)n.getLongitude());
            }

            if (n.getLatitude() > qr.top) {
               qr.top = (double)((float)n.getLatitude());
            } else if (n.getLatitude() < qr.bottom) {
               qr.bottom = (double)((float)n.getLatitude());
            }
         }
      }

      return qr;
   }

   @Override
   public LatLon getLatLon() {
      return this.nodes == null ? null : OsmMapUtils.getWeightCenterForWay(this);
   }

   public void reverseNodes() {
      if (this.nodes != null) {
         Collections.reverse(this.nodes);
      }

      if (this.nodeIds != null) {
         this.nodeIds.reverse();
      }
   }

   public boolean compareWay(Way thatObj) {
      if (this.compareEntity(thatObj)
         && Algorithms.objectEquals(this.nodeIds, thatObj.nodeIds)
         && (this.nodes == null && thatObj.nodes == null || this.nodes != null && thatObj.nodes != null && this.nodes.size() == thatObj.nodes.size())) {
         if (this.nodes != null) {
            for(int i = 0; i < this.nodes.size(); ++i) {
               if (!this.nodes.get(i).compareNode(thatObj.nodes.get(i))) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
