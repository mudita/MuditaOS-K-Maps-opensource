package net.osmand.osm.edit;

import java.io.Serializable;
import java.util.Map;
import net.osmand.Location;
import net.osmand.data.LatLon;

public class Node extends Entity implements Serializable {
   private static final long serialVersionUID = -2981499160640211082L;

   public Node(double latitude, double longitude, long id) {
      super(id, latitude, longitude);
   }

   public Node(Node n, long newId) {
      super(n, newId);
   }

   @Override
   public LatLon getLatLon() {
      return new LatLon(this.getLatitude(), this.getLongitude());
   }

   public Location getLocation() {
      Location l = new Location("");
      l.setLatitude(this.getLatitude());
      l.setLongitude(this.getLongitude());
      return l;
   }

   @Override
   public void initializeLinks(Map<Entity.EntityId, Entity> entities) {
   }

   @Override
   public String toString() {
      return "Node{latitude=" + this.getLatitude() + ", longitude=" + this.getLongitude() + ", tags=" + this.getTags() + '}';
   }

   public boolean compareNode(Node thatObj) {
      return this.compareEntity(thatObj);
   }
}
