package net.osmand.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class TransportStop extends MapObject {
   private static final int DELETED_STOP = -1;
   public static final String MISSING_STOP_NAME = "#Missing Stop";
   private int[] referencesToRoutes = null;
   private long[] deletedRoutesIds;
   private long[] routesIds;
   public int distance;
   public int x31;
   public int y31;
   private List<TransportStopExit> exits;
   private List<TransportRoute> routes = null;
   private TransportStopAggregated transportStopAggregated;

   public List<TransportRoute> getRoutes() {
      return this.routes;
   }

   public boolean isMissingStop() {
      return "#Missing Stop".equals(this.getName());
   }

   public void setRoutes(List<TransportRoute> routes) {
      this.routes = routes;
   }

   public void addRoute(TransportRoute rt) {
      if (this.routes == null) {
         this.routes = new ArrayList<>();
      }

      this.routes.add(rt);
   }

   public int[] getReferencesToRoutes() {
      return this.referencesToRoutes;
   }

   public void setReferencesToRoutes(int[] referencesToRoutes) {
      this.referencesToRoutes = referencesToRoutes;
   }

   public long[] getRoutesIds() {
      return this.routesIds;
   }

   public void setRoutesIds(long[] routesIds) {
      this.routesIds = routesIds;
   }

   public boolean hasRoute(long routeId) {
      return this.routesIds != null && Arrays.binarySearch(this.routesIds, routeId) >= 0;
   }

   public boolean isDeleted() {
      return this.referencesToRoutes != null && this.referencesToRoutes.length == 1 && this.referencesToRoutes[0] == -1;
   }

   public void setDeleted() {
      this.referencesToRoutes = new int[]{-1};
   }

   public long[] getDeletedRoutesIds() {
      return this.deletedRoutesIds;
   }

   public void setDeletedRoutesIds(long[] deletedRoutesIds) {
      this.deletedRoutesIds = deletedRoutesIds;
   }

   public void addRouteId(long routeId) {
      this.routesIds = Algorithms.addToArrayL(this.routesIds, routeId, true);
   }

   public void addDeletedRouteId(long routeId) {
      this.deletedRoutesIds = Algorithms.addToArrayL(this.deletedRoutesIds, routeId, true);
   }

   public boolean isRouteDeleted(long routeId) {
      return this.deletedRoutesIds != null && Arrays.binarySearch(this.deletedRoutesIds, routeId) >= 0;
   }

   public boolean hasReferencesToRoutes() {
      return !this.isDeleted() && this.referencesToRoutes != null && this.referencesToRoutes.length > 0;
   }

   public Amenity getAmenity() {
      return this.transportStopAggregated != null ? this.transportStopAggregated.getAmenity() : null;
   }

   public void setAmenity(Amenity amenity) {
      if (this.transportStopAggregated == null) {
         this.transportStopAggregated = new TransportStopAggregated();
      }

      this.transportStopAggregated.setAmenity(amenity);
   }

   public void addLocalTransportStop(TransportStop stop) {
      if (this.transportStopAggregated == null) {
         this.transportStopAggregated = new TransportStopAggregated();
      }

      this.transportStopAggregated.addLocalTransportStop(stop);
   }

   public List<TransportStop> getNearbyTransportStops() {
      return this.transportStopAggregated != null ? this.transportStopAggregated.getNearbyTransportStops() : Collections.<TransportStop>emptyList();
   }

   public void addNearbyTransportStop(TransportStop stop) {
      if (this.transportStopAggregated == null) {
         this.transportStopAggregated = new TransportStopAggregated();
      }

      this.transportStopAggregated.addNearbyTransportStop(stop);
   }

   public TransportStopAggregated getTransportStopAggregated() {
      return this.transportStopAggregated;
   }

   public void setTransportStopAggregated(TransportStopAggregated stopAggregated) {
      this.transportStopAggregated = stopAggregated;
   }

   @Override
   public void setLocation(double latitude, double longitude) {
      super.setLocation(latitude, longitude);
   }

   public void setLocation(int zoom, int dx, int dy) {
      this.x31 = dx << 31 - zoom;
      this.y31 = dy << 31 - zoom;
      this.setLocation(MapUtils.getLatitudeFromTile((float)zoom, (double)dy), MapUtils.getLongitudeFromTile((double)zoom, (double)dx));
   }

   public void addExit(TransportStopExit transportStopExit) {
      if (this.exits == null) {
         this.exits = new ArrayList<>();
      }

      this.exits.add(transportStopExit);
   }

   public List<TransportStopExit> getExits() {
      return this.exits == null ? Collections.<TransportStopExit>emptyList() : this.exits;
   }

   public String getExitsString() {
      String exitsString = "";
      String refString = "";
      if (this.exits != null) {
         int i = 1;
         exitsString = exitsString + " Exits: [";

         for(TransportStopExit e : this.exits) {
            if (e.getRef() != null) {
               refString = " [ref:" + e.getRef() + "] ";
            }

            exitsString = exitsString + " " + i + ")" + refString + e.getName() + " " + e.getLocation() + " ]";
            ++i;
         }
      }

      return exitsString;
   }

   public boolean compareStop(TransportStop thatObj) {
      if (this.compareObject(thatObj)
         && (this.exits == null && thatObj.exits == null || this.exits != null && thatObj.exits != null && this.exits.size() == thatObj.exits.size())) {
         if (this.exits != null) {
            for(TransportStopExit exit1 : this.exits) {
               if (exit1 == null) {
                  return false;
               }

               boolean contains = false;

               for(TransportStopExit exit2 : thatObj.exits) {
                  if (Algorithms.objectEquals(exit1, exit2)) {
                     contains = true;
                     if (!exit1.compareExit(exit2)) {
                        return false;
                     }
                     break;
                  }
               }

               if (!contains) {
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
