package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransportStopAggregated {
   private Amenity amenity;
   private List<TransportStop> localTransportStops;
   private List<TransportStop> nearbyTransportStops;

   public Amenity getAmenity() {
      return this.amenity;
   }

   public void setAmenity(Amenity amenity) {
      this.amenity = amenity;
   }

   public void addLocalTransportStop(TransportStop stop) {
      if (this.localTransportStops == null) {
         this.localTransportStops = new ArrayList<>();
      }

      this.localTransportStops.add(stop);
   }

   public void addLocalTransportStops(List<TransportStop> stops) {
      if (this.localTransportStops == null) {
         this.localTransportStops = new ArrayList<>();
      }

      this.localTransportStops.addAll(stops);
   }

   public List<TransportStop> getNearbyTransportStops() {
      return this.nearbyTransportStops == null ? Collections.<TransportStop>emptyList() : this.nearbyTransportStops;
   }

   public void addNearbyTransportStop(TransportStop stop) {
      if (this.nearbyTransportStops == null) {
         this.nearbyTransportStops = new ArrayList<>();
      }

      this.nearbyTransportStops.add(stop);
   }

   public void addNearbyTransportStops(List<TransportStop> stops) {
      if (this.nearbyTransportStops == null) {
         this.nearbyTransportStops = new ArrayList<>();
      }

      this.nearbyTransportStops.addAll(stops);
   }
}
