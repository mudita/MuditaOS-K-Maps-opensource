package net.osmand.data;

import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class TransportStopExit extends MapObject {
   public int x31;
   public int y31;
   public String ref = null;

   public TransportStopExit() {
   }

   public TransportStopExit(int x31, int y31, String ref) {
      this.x31 = x31;
      this.y31 = y31;
      this.ref = ref;
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

   public void setRef(String ref) {
      this.ref = ref;
   }

   public String getRef() {
      return this.ref != null ? this.ref : "";
   }

   public boolean compareExit(TransportStopExit thatObj) {
      return this.compareObject(thatObj) && Algorithms.objectEquals(this.ref, thatObj.ref);
   }
}
