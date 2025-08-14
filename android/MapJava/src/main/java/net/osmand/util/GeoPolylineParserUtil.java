package net.osmand.util;

import java.util.ArrayList;
import java.util.List;
import net.osmand.data.LatLon;

public class GeoPolylineParserUtil {
   public static final double PRECISION_6 = 1000000.0;
   public static final double PRECISION_5 = 100000.0;

   public static List<LatLon> parse(String encoded, double precision) {
      List<LatLon> track = new ArrayList<>();
      int index = 0;
      int lat = 0;
      int lng = 0;

      while(index < encoded.length()) {
         int shift = 0;
         int result = 0;

         int b;
         do {
            b = encoded.charAt(index++) - '?';
            result |= (b & 31) << shift;
            shift += 5;
         } while(b >= 32);

         int dlat = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
         lat += dlat;
         shift = 0;
         result = 0;

         do {
            b = encoded.charAt(index++) - '?';
            result |= (b & 31) << shift;
            shift += 5;
         } while(b >= 32);

         int dlng = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
         lng += dlng;
         LatLon p = new LatLon((double)lat / precision, (double)lng / precision);
         track.add(p);
      }

      return track;
   }
}
