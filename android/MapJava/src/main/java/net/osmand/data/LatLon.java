package net.osmand.data;

import java.io.Serializable;

public class LatLon implements Serializable {
   private final double longitude;
   private final double latitude;

   public LatLon(double latitude, double longitude) {
      this.latitude = latitude;
      this.longitude = longitude;
   }

   public static LatLon zero = new LatLon(0, 0);

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      int temp = (int)Math.floor(this.latitude * 10000.0);
      result = 31 * result + temp;
      temp = (int)Math.floor(this.longitude * 10000.0);
      return 31 * result + temp;
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
         LatLon other = (LatLon)obj;
         return Math.abs(this.latitude - other.latitude) < 1.0E-5 && Math.abs(this.longitude - other.longitude) < 1.0E-5;
      }
   }

   @Override
   public String toString() {
      return "Lat " + (float)this.latitude + " Lon " + (float)this.longitude;
   }

   public double getLatitude() {
      return this.latitude;
   }

   public double getLongitude() {
      return this.longitude;
   }
}
