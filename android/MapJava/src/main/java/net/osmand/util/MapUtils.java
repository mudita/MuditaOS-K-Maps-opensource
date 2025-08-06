package net.osmand.util;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapUtils {
   public static final int ROUNDING_ERROR = 3;
   private static final int EARTH_RADIUS_B = 6356752;
   private static final int EARTH_RADIUS_A = 6378137;
   public static final double MIN_LATITUDE = -85.0511;
   public static final double MAX_LATITUDE = 85.0511;
   public static final double LATITUDE_TURN = 180.0;
   public static final double MIN_LONGITUDE = -180.0;
   public static final double MAX_LONGITUDE = 180.0;
   public static final double LONGITUDE_TURN = 360.0;
   private static final String BASE_SHORT_OSM_URL = "https://openstreetmap.org/go/";
   private static final char[] intToBase64 = new char[]{
      'A',
      'B',
      'C',
      'D',
      'E',
      'F',
      'G',
      'H',
      'I',
      'J',
      'K',
      'L',
      'M',
      'N',
      'O',
      'P',
      'Q',
      'R',
      'S',
      'T',
      'U',
      'V',
      'W',
      'X',
      'Y',
      'Z',
      'a',
      'b',
      'c',
      'd',
      'e',
      'f',
      'g',
      'h',
      'i',
      'j',
      'k',
      'l',
      'm',
      'n',
      'o',
      'p',
      'q',
      'r',
      's',
      't',
      'u',
      'v',
      'w',
      'x',
      'y',
      'z',
      '0',
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
      '7',
      '8',
      '9',
      '_',
      '~'
   };
   private static double[] coefficientsY = new double[1024];
   private static boolean initializeYArray = false;
   private static double[] coefficientsX = new double[1024];

   public static int calculateFromBaseZoomPrecisionXY(int baseZoom, int finalZoom, int xFinal, int yFinal) {
      int px = xFinal;
      int py = yFinal;
      int precisionNumber = 1;

      for(int zoom = finalZoom - 1; zoom >= baseZoom; --zoom) {
         int x = px / 2;
         int y = py / 2;
         int deltax = px - x * 2;
         int deltay = py - y * 2;
         precisionNumber = (precisionNumber << 2) + (deltax << 1) + deltay;
         px = x;
         py = y;
      }

      return precisionNumber;
   }

   public static int[] calculateFinalXYFromBaseAndPrecisionXY(
      int bazeZoom, int finalZoom, int precisionXY, int xBase, int yBase, boolean ignoreNotEnoughPrecision
   ) {
      int finalX = xBase;
      int finalY = yBase;
      int precisionCalc = precisionXY;

      for(int zoom = bazeZoom; zoom < finalZoom; ++zoom) {
         if (precisionCalc <= 1 && precisionCalc > 0 && !ignoreNotEnoughPrecision) {
            throw new IllegalArgumentException("Not enough bits to retrieve zoom approximation");
         }

         finalY = finalY * 2 + (precisionXY & 1);
         finalX = finalX * 2 + ((precisionXY & 2) >> 1);
         precisionXY >>= 2;
      }

      return new int[]{finalX, finalY};
   }

   public static double getDistance(LatLon l, double latitude, double longitude) {
      return getDistance(l.getLatitude(), l.getLongitude(), latitude, longitude);
   }

   private static double scalarMultiplication(double xA, double yA, double xB, double yB, double xC, double yC) {
      return (xB - xA) * (xC - xA) + (yB - yA) * (yC - yA);
   }

   public static Location calculateMidPoint(Location s1, Location s2) {
      double[] latLon = calculateMidPoint(s1.getLatitude(), s1.getLongitude(), s2.getLatitude(), s2.getLongitude());
      return new Location("", latLon[0], latLon[1]);
   }

   public static LatLon calculateMidPoint(LatLon s1, LatLon s2) {
      double[] latLon = calculateMidPoint(s1.getLatitude(), s1.getLongitude(), s2.getLatitude(), s2.getLongitude());
      return new LatLon(latLon[0], latLon[1]);
   }

   public static double[] calculateMidPoint(double firstLat, double firstLon, double secondLat, double secondLon) {
      double lat1 = firstLat / 180.0 * Math.PI;
      double lon1 = firstLon / 180.0 * Math.PI;
      double lat2 = secondLat / 180.0 * Math.PI;
      double lon2 = secondLon / 180.0 * Math.PI;
      double Bx = Math.cos(lat2) * Math.cos(lon2 - lon1);
      double By = Math.cos(lat2) * Math.sin(lon2 - lon1);
      double latMid = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
      double lonMid = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
      return new double[]{checkLatitude(latMid * 180.0 / Math.PI), checkLongitude(lonMid * 180.0 / Math.PI)};
   }

   public static Location calculateInterpolatedPoint(Location start, Location end, double fraction) {
      double lat = start.getLatitude() + fraction * (end.getLatitude() - start.getLatitude());
      double lon = start.getLongitude() + fraction * (end.getLongitude() - start.getLongitude());
      return new Location("", lat, lon);
   }

   public static double getOrthogonalDistance(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
      return getDistance(getProjection(lat, lon, fromLat, fromLon, toLat, toLon), lat, lon);
   }

   public static LatLon getProjection(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
      double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
      double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
      double prlat;
      double prlon;
      if (projection < 0.0) {
         prlat = fromLat;
         prlon = fromLon;
      } else if (projection >= mDist) {
         prlat = toLat;
         prlon = toLon;
      } else {
         prlat = fromLat + (toLat - fromLat) * (projection / mDist);
         prlon = fromLon + (toLon - fromLon) * (projection / mDist);
      }

      return new LatLon(prlat, prlon);
   }

   public static double getProjectionCoeff(double lat, double lon, double fromLat, double fromLon, double toLat, double toLon) {
      double mDist = (fromLat - toLat) * (fromLat - toLat) + (fromLon - toLon) * (fromLon - toLon);
      double projection = scalarMultiplication(fromLat, fromLon, toLat, toLon, lat, lon);
      if (projection < 0.0) {
         return 0.0;
      } else {
         return projection >= mDist ? 1.0 : projection / mDist;
      }
   }

   private static double toRadians(double angdeg) {
      return angdeg / 180.0 * Math.PI;
   }

   public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
      double R = 6372.8;
      double dLat = toRadians(lat2 - lat1);
      double dLon = toRadians(lon2 - lon1);
      double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
         + Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
      return 2.0 * R * 1000.0 * Math.asin(Math.sqrt(a));
   }

   public static double getDistance(LatLon l1, LatLon l2) {
      return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
   }

   public static double getDistance(Location l1, Location l2) {
      return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
   }

   public static double checkLongitude(double longitude) {
      if (longitude >= -180.0 && longitude <= 180.0) {
         return longitude;
      } else {
         while(longitude <= -180.0 || longitude > 180.0) {
            if (longitude < 0.0) {
               longitude += 360.0;
            } else {
               longitude -= 360.0;
            }
         }

         return longitude;
      }
   }

   public static double checkLatitude(double latitude) {
      if (latitude >= -85.0511 && latitude <= 85.0511) {
         return latitude;
      } else {
         while(latitude < -90.0 || latitude > 90.0) {
            if (latitude < 0.0) {
               latitude += 180.0;
            } else {
               latitude -= 180.0;
            }
         }

         if (latitude < -85.0511) {
            return -85.0511;
         } else {
            return latitude > 85.0511 ? 85.0511 : latitude;
         }
      }
   }

   public static int get31TileNumberX(double longitude) {
      longitude = checkLongitude(longitude);
      long l = 2147483648L;
      return (int)((longitude + 180.0) / 360.0 * (double)l);
   }

   public static int get31TileNumberY(double latitude) {
      latitude = checkLatitude(latitude);
      double eval = Math.log(Math.tan(toRadians(latitude)) + 1.0 / Math.cos(toRadians(latitude)));
      long l = 2147483648L;
      if (eval > Math.PI) {
         eval = Math.PI;
      }

      return (int)((1.0 - eval / Math.PI) / 2.0 * (double)l);
   }

   public static double get31LongitudeX(int tileX) {
      return getLongitudeFromTile(21.0, (double)tileX / 1024.0);
   }

   public static double get31LatitudeY(int tileY) {
      return getLatitudeFromTile(21.0F, (double)tileY / 1024.0);
   }

   public static double getTileNumberX(float zoom, double longitude) {
      longitude = checkLongitude(longitude);
      double powZoom = getPowZoom((double)zoom);
      double dz = (longitude + 180.0) / 360.0 * powZoom;
      return dz >= powZoom ? powZoom - 0.01 : dz;
   }

   public static double getTileNumberY(float zoom, double latitude) {
      latitude = checkLatitude(latitude);
      double eval = Math.log(Math.tan(toRadians(latitude)) + 1.0 / Math.cos(toRadians(latitude)));
      if (Double.isInfinite(eval) || Double.isNaN(eval)) {
         latitude = latitude < 0.0 ? -89.9 : 89.9;
         eval = Math.log(Math.tan(toRadians(latitude)) + 1.0 / Math.cos(toRadians(latitude)));
      }

      return (1.0 - eval / Math.PI) / 2.0 * getPowZoom((double)zoom);
   }

   public static double getTileEllipsoidNumberY(float zoom, double latitude) {
      double E2 = latitude * Math.PI / 180.0;
      long sradiusa = 6378137L;
      long sradiusb = 6356752L;
      double J2 = Math.sqrt(2.72335601265E11) / 6378137.0;
      double M2 = Math.log((1.0 + Math.sin(E2)) / (1.0 - Math.sin(E2))) / 2.0 - J2 * Math.log((1.0 + J2 * Math.sin(E2)) / (1.0 - J2 * Math.sin(E2))) / 2.0;
      double B2 = getPowZoom((double)zoom);
      return B2 / 2.0 - M2 * B2 / 2.0 / Math.PI;
   }

   public static double[] getTileEllipsoidNumberAndOffsetY(int zoom, double latitude, int tileSize) {
      double E2 = latitude * Math.PI / 180.0;
      long sradiusa = 6378137L;
      long sradiusb = 6356752L;
      double J2 = Math.sqrt(2.72335601265E11) / 6378137.0;
      double M2 = Math.log((1.0 + Math.sin(E2)) / (1.0 - Math.sin(E2))) / 2.0 - J2 * Math.log((1.0 + J2 * Math.sin(E2)) / (1.0 - J2 * Math.sin(E2))) / 2.0;
      double B2 = getPowZoom((double)zoom);
      double tileY = B2 / 2.0 - M2 * B2 / 2.0 / Math.PI;
      double tilesCount = (double)(1 << zoom);
      double yTileNumber = Math.floor(tilesCount * (0.5 - M2 / 2.0 / Math.PI));
      double offsetY = Math.floor((tilesCount * (0.5 - M2 / 2.0 / Math.PI) - yTileNumber) * (double)tileSize);
      return new double[]{tileY, offsetY};
   }

   public static double getLatitudeFromEllipsoidTileY(float zoom, float tileNumberY) {
      double MerkElipsK = 1.0E-7;
      long sradiusa = 6378137L;
      long sradiusb = 6356752L;
      double FExct = Math.sqrt(2.72335601265E11) / 6378137.0;
      double TilesAtZoom = getPowZoom((double)zoom);
      double result = ((double)tileNumberY - TilesAtZoom / 2.0) / -(TilesAtZoom / (Math.PI * 2));
      result = (2.0 * Math.atan(Math.exp(result)) - (Math.PI / 2)) * 180.0 / Math.PI;
      double Zu = result / (180.0 / Math.PI);
      double yy = (double)tileNumberY - TilesAtZoom / 2.0;
      double Zum1 = Zu;

      for(Zu = Math.asin(
            1.0
               - (1.0 + Math.sin(Zu))
                  * Math.pow(1.0 - FExct * Math.sin(Zu), FExct)
                  / (Math.exp(2.0 * yy / -(TilesAtZoom / (Math.PI * 2))) * Math.pow(1.0 + FExct * Math.sin(Zu), FExct))
         );
         Math.abs(Zum1 - Zu) >= 1.0E-7;
         Zu = Math.asin(
            1.0
               - (1.0 + Math.sin(Zu))
                  * Math.pow(1.0 - FExct * Math.sin(Zu), FExct)
                  / (Math.exp(2.0 * yy / -(TilesAtZoom / (Math.PI * 2))) * Math.pow(1.0 + FExct * Math.sin(Zu), FExct))
         )
      ) {
         Zum1 = Zu;
      }

      return Zu * 180.0 / Math.PI;
   }

   public static double getTileDistanceWidth(float zoom) {
      return getTileDistanceWidth(30.0, zoom);
   }

   public static double getTileDistanceWidth(double lat, float zoom) {
      LatLon ll = new LatLon(lat, getLongitudeFromTile((double)zoom, 0.0));
      LatLon ll2 = new LatLon(lat, getLongitudeFromTile((double)zoom, 1.0));
      return getDistance(ll, ll2);
   }

   public static double getLongitudeFromTile(double zoom, double x) {
      return x / getPowZoom(zoom) * 360.0 - 180.0;
   }

   public static double getPowZoom(double zoom) {
      return zoom >= 0.0 && zoom - Math.floor(zoom) < 0.001F ? (double)(1 << (int)zoom) : Math.pow(2.0, zoom);
   }

   public static float calcDiffPixelX(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
      return (rotateCos * dTileX - rotateSin * dTileY) * tileSize;
   }

   public static float calcDiffPixelY(float rotateSin, float rotateCos, float dTileX, float dTileY, float tileSize) {
      return (rotateSin * dTileX + rotateCos * dTileY) * tileSize;
   }

   public static double getLatitudeFromTile(float zoom, double y) {
      int sign = y < 0.0 ? -1 : 1;
      return Math.atan((double)sign * Math.sinh(Math.PI * (1.0 - 2.0 * y / getPowZoom((double)zoom)))) * 180.0 / Math.PI;
   }

   public static int getPixelShiftX(float zoom, double long1, double long2, double tileSize) {
      return (int)((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize);
   }

   public static int getPixelShiftY(float zoom, double lat1, double lat2, double tileSize) {
      return (int)((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize);
   }

   public static void sortListOfMapObject(List<? extends MapObject> list, final double lat, final double lon) {
      Collections.sort(list, new Comparator<MapObject>() {
         public int compare(MapObject o1, MapObject o2) {
            return Double.compare(MapUtils.getDistance(o1.getLocation(), lat, lon), MapUtils.getDistance(o2.getLocation(), lat, lon));
         }
      });
   }

   public static String buildGeoUrl(String latitude, String longitude, int zoom) {
      return "geo:" + latitude + "," + longitude + "?z=" + zoom;
   }

   public static String buildShortOsmUrl(double latitude, double longitude, int zoom) {
      return "https://openstreetmap.org/go/" + createShortLinkString(latitude, longitude, zoom) + "?m";
   }

   public static String createShortLinkString(double latitude, double longitude, int zoom) {
      long lat = (long)((latitude + 90.0) / 180.0 * 4.2949673E9F);
      long lon = (long)((longitude + 180.0) / 360.0 * 4.2949673E9F);
      long code = interleaveBits(lon, lat);
      String str = "";

      for(int i = 0; (double)i < Math.ceil((double)(zoom + 8) / 3.0); ++i) {
         str = str + intToBase64[(int)(code >> 58 - 6 * i & 63L)];
      }

      for(int j = 0; j < (zoom + 8) % 3; ++j) {
         str = str + '-';
      }

      return str;
   }

   public static GeoParsedPoint decodeShortLinkString(String s) {
      s = s.replaceAll("@", "~");
      int i = 0;
      long x = 0L;
      long y = 0L;
      int z = -8;

      for(i = 0; i < s.length(); ++i) {
         int digit = -1;
         char c = s.charAt(i);

         for(int j = 0; j < intToBase64.length; ++j) {
            if (c == intToBase64[j]) {
               digit = j;
               break;
            }
         }

         if (digit < 0 || digit < 0) {
            break;
         }

         x <<= 3;
         y <<= 3;

         for(int j = 2; j >= 0; --j) {
            x |= (long)((digit & 1 << j + j + 1) == 0 ? 0 : 1 << j);
            y |= (long)((digit & 1 << j + j) == 0 ? 0 : 1 << j);
         }

         z += 3;
      }

      double lon = (double)x * Math.pow(2.0, (double)(2 - 3 * i)) * 90.0 - 180.0;
      double lat = (double)y * Math.pow(2.0, (double)(2 - 3 * i)) * 45.0 - 90.0;
      if (i < s.length() && s.charAt(i) == '-') {
         z -= 2;
         if (i + 1 < s.length() && s.charAt(i + 1) == '-') {
            ++z;
         }
      }

      return new GeoParsedPoint(lat, lon, z);
   }

   public static long interleaveBits(long x, long y) {
      long c = 0L;

      for(byte b = 31; b >= 0; --b) {
         c = c << 1 | x >> b & 1L;
         c = c << 1 | y >> b & 1L;
      }

      return c;
   }

   public static float unifyRotationDiff(float rotate, float targetRotate) {
      float d = targetRotate - rotate;

      while(d >= 180.0F) {
         d -= 360.0F;
      }

      while(d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   public static float unifyRotationTo360(float rotate) {
      while(rotate < -180.0F) {
         rotate += 360.0F;
      }

      while(rotate > 180.0F) {
         rotate -= 360.0F;
      }

      return rotate;
   }

   public static float normalizeDegrees360(float degrees) {
      while(degrees < 0.0F) {
         degrees += 360.0F;
      }

      while(degrees >= 360.0F) {
         degrees -= 360.0F;
      }

      return degrees;
   }

   public static double alignAngleDifference(double diff) {
      while(diff > Math.PI) {
         diff -= Math.PI * 2;
      }

      while(diff <= -Math.PI) {
         diff += Math.PI * 2;
      }

      return diff;
   }

   public static double degreesDiff(double a1, double a2) {
      double diff = a1 - a2;

      while(diff > 180.0) {
         diff -= 360.0;
      }

      while(diff <= -180.0) {
         diff += 360.0;
      }

      return diff;
   }

   public static double convert31YToMeters(int y1, int y2, int x) {
      int power = 10;
      int pw = 1 << power;
      if (!initializeYArray) {
         coefficientsY[0] = 0.0;

         for(int i = 0; i < pw - 1; ++i) {
            coefficientsY[i + 1] = coefficientsY[i] + measuredDist31(0, i << 31 - power, 0, i + 1 << 31 - power);
         }

         initializeYArray = true;
      }

      int div = 1 << 31 - power;
      int div1 = y1 / div;
      int mod1 = y1 % div;
      int div2 = y2 / div;
      int mod2 = y2 % div;
      double h1;
      if (div1 + 1 >= coefficientsY.length) {
         h1 = coefficientsY[div1] + (double)mod1 / (double)div * (coefficientsY[div1] - coefficientsY[div1 - 1]);
      } else {
         h1 = coefficientsY[div1] + (double)mod1 / (double)div * (coefficientsY[div1 + 1] - coefficientsY[div1]);
      }

      double h2;
      if (div2 + 1 >= coefficientsY.length) {
         h2 = coefficientsY[div2] + (double)mod2 / (double)div * (coefficientsY[div2] - coefficientsY[div2 - 1]);
      } else {
         h2 = coefficientsY[div2] + (double)mod2 / (double)div * (coefficientsY[div2 + 1] - coefficientsY[div2]);
      }

      return h1 - h2;
   }

   public static double convert31XToMeters(int x1, int x2, int y) {
      int ind = y >> 21;
      if (coefficientsX[ind] == 0.0) {
         double md = measuredDist31(x1, y, x2, y);
         if (md < 10.0) {
            return md;
         }

         coefficientsX[ind] = md / (double)Math.abs(x1 - x2);
      }

      return (double)(x1 - x2) * coefficientsX[ind];
   }

   public static QuadPoint getProjectionPoint31(int px, int py, int st31x, int st31y, int end31x, int end31y) {
      double projection = calculateProjection31TileMetric(st31x, st31y, end31x, end31y, px, py);
      double mDist = measuredDist31(end31x, end31y, st31x, st31y);
      int pry;
      int prx;
      if (projection < 0.0) {
         prx = st31x;
         pry = st31y;
      } else if (projection >= mDist * mDist) {
         prx = end31x;
         pry = end31y;
      } else {
         prx = (int)((double)st31x + (double)(end31x - st31x) * (projection / (mDist * mDist)));
         pry = (int)((double)st31y + (double)(end31y - st31y) * (projection / (mDist * mDist)));
      }

      return new QuadPoint((float)prx, (float)pry);
   }

   public static double squareRootDist31(int x1, int y1, int x2, int y2) {
      double dy = convert31YToMeters(y1, y2, x1);
      double dx = convert31XToMeters(x1, x2, y1);
      return Math.sqrt(dx * dx + dy * dy);
   }

   public static double measuredDist31(int x1, int y1, int x2, int y2) {
      return getDistance(get31LatitudeY(y1), get31LongitudeX(x1), get31LatitudeY(y2), get31LongitudeX(x2));
   }

   public static double squareDist31TileMetric(int x1, int y1, int x2, int y2) {
      double dy = convert31YToMeters(y1, y2, x1);
      double dx = convert31XToMeters(x1, x2, y1);
      return dx * dx + dy * dy;
   }

   public static double calculateProjection31TileMetric(int xA, int yA, int xB, int yB, int xC, int yC) {
      return convert31XToMeters(xB, xA, yA) * convert31XToMeters(xC, xA, yA) + convert31YToMeters(yB, yA, xA) * convert31YToMeters(yC, yA, xA);
   }

   public static boolean rightSide(double lat, double lon, double aLat, double aLon, double bLat, double bLon) {
      double ax = aLon - lon;
      double ay = aLat - lat;
      double bx = bLon - lon;
      double by = bLat - lat;
      double sa = ax * by - bx * ay;
      return sa < 0.0;
   }

   public static long deinterleaveY(long coord) {
      long x = 0L;

      for(byte b = 31; b >= 0; --b) {
         x = x << 1 | 1L & coord >> b * 2;
      }

      return x;
   }

   public static long deinterleaveX(long coord) {
      long x = 0L;

      for(byte b = 31; b >= 0; --b) {
         x = x << 1 | 1L & coord >> b * 2 + 1;
      }

      return x;
   }

   public static QuadRect calculateLatLonBbox(double latitude, double longitude, int radiusMeters) {
      int zoom = 16;
      float coeff = (float)((double)radiusMeters / getTileDistanceWidth((float)zoom));
      double tx = getTileNumberX((float)zoom, longitude);
      double ty = getTileNumberY((float)zoom, latitude);
      double topLeftX = Math.max(0.0, tx - (double)coeff);
      double topLeftY = Math.max(0.0, ty - (double)coeff);
      int max = (1 << zoom) - 1;
      double bottomRightX = Math.min((double)max, tx + (double)coeff);
      double bottomRightY = Math.min((double)max, ty + (double)coeff);
      double pw = getPowZoom((double)(31 - zoom));
      QuadRect rect = new QuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw);
      rect.left = get31LongitudeX((int)rect.left);
      rect.top = get31LatitudeY((int)rect.top);
      rect.right = get31LongitudeX((int)rect.right);
      rect.bottom = get31LatitudeY((int)rect.bottom);
      return rect;
   }

   public static float getInterpolatedY(float x1, float y1, float x2, float y2, float x) {
      float a = y1 - y2;
      float b = x2 - x1;
      float d = -a * b;
      if (d != 0.0F) {
         float c1 = y2 * x1 - x2 * y1;
         float c2 = x * (y2 - y1);
         return a * (c1 - c2) / d;
      } else {
         return y1;
      }
   }

   public static void insetLatLonRect(QuadRect r, double latitude, double longitude) {
      if (r.left == 0.0 && r.right == 0.0) {
         r.left = longitude;
         r.right = longitude;
         r.top = latitude;
         r.bottom = latitude;
      } else {
         r.left = Math.min(r.left, longitude);
         r.right = Math.max(r.right, longitude);
         r.top = Math.max(r.top, latitude);
         r.bottom = Math.min(r.bottom, latitude);
      }
   }

   public static boolean areLatLonEqual(Location l1, Location l2) {
      return l1 == null && l2 == null || l2 != null && areLatLonEqual(l1, l2.getLatitude(), l2.getLongitude());
   }

   public static boolean areLatLonEqualPrecise(Location l1, Location l2) {
      return l1 == null && l2 == null || l2 != null && areLatLonEqualPrecise(l1, l2.getLatitude(), l2.getLongitude());
   }

   public static boolean areLatLonEqual(Location l, double lat, double lon) {
      return l != null && Math.abs(l.getLatitude() - lat) < 1.0E-5 && Math.abs(l.getLongitude() - lon) < 1.0E-5;
   }

   public static boolean areLatLonEqualPrecise(Location l, double lat, double lon) {
      return l != null && Math.abs(l.getLatitude() - lat) < 1.0E-7 && Math.abs(l.getLongitude() - lon) < 1.0E-7;
   }

   public static LatLon rhumbDestinationPoint(LatLon latLon, double distance, double bearing) {
      double radius = 6378137.0;
      double d = distance / radius;
      double phi1 = Math.toRadians(latLon.getLatitude());
      double lambda1 = Math.toRadians(latLon.getLongitude());
      double theta = Math.toRadians(bearing);
      double deltaPhi = d * Math.cos(theta);
      double phi2 = phi1 + deltaPhi;
      double deltaPsi = Math.log(Math.tan(phi2 / 2.0 + (Math.PI / 4)) / Math.tan(phi1 / 2.0 + (Math.PI / 4)));
      double q = Math.abs(deltaPsi) > 1.0E-11 ? deltaPhi / deltaPsi : Math.cos(phi1);
      double deltalambda = d * Math.sin(theta) / q;
      double lambda2 = lambda1 + deltalambda;
      return new LatLon(Math.toDegrees(phi2), Math.toDegrees(lambda2));
   }

   public static double getSqrtDistance(int startX, int startY, int endX, int endY) {
      return Math.sqrt((double)(endX - startX) * (double)(endX - startX) + (double)(endY - startY) * (double)(endY - startY));
   }

   public static double getSqrtDistance(float startX, float startY, float endX, float endY) {
      return Math.sqrt((double)((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)));
   }

   public static String convertDistToChar(int dist, char firstLetter, int firstDist, int mult1, int mult2) {
      int iteration;
      for(iteration = 0; dist - firstDist > 0; firstDist *= iteration % 2 == 1 ? mult1 : mult2) {
         ++iteration;
      }

      return String.valueOf((char)(firstLetter + iteration));
   }

   public static int convertCharToDist(char ch, char firstLetter, int firstDist, int mult1, int mult2) {
      int dist = firstDist;

      for(int iteration = 1; iteration < ch - firstLetter + 1; ++iteration) {
         dist *= iteration % 2 == 1 ? mult1 : mult2;
      }

      return dist;
   }

   public static double getAngleBetweenLocations(Location start, Location end, Location center) {
      double centerStartX = start.getLongitude() - center.getLongitude();
      double centerStartY = start.getLatitude() - center.getLatitude();
      double centerEndX = end.getLongitude() - center.getLongitude();
      double centerEndY = end.getLatitude() - center.getLatitude();
      double dotProduct = centerStartX * centerEndX + centerStartY * centerEndY;
      double magnitudeCenterStart =
              Math.sqrt(centerStartX * centerStartX + centerStartY * centerStartY);
      double magnitudeCenterEnd = Math.sqrt(centerEndX * centerEndX + centerEndY * centerEndY);
      return Math.toDegrees(Math.acos(dotProduct / (magnitudeCenterStart * magnitudeCenterEnd)));
   }
}
