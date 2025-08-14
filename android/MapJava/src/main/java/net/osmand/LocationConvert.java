package net.osmand;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.StringTokenizer;

public class LocationConvert {
   public static final int FORMAT_DEGREES = 0;
   public static final int FORMAT_MINUTES = 1;
   public static final int FORMAT_SECONDS = 2;
   public static final int UTM_FORMAT = 3;
   public static final int OLC_FORMAT = 4;
   public static final int MGRS_FORMAT = 5;
   public static final int SWISS_GRID_FORMAT = 6;
   public static final int SWISS_GRID_PLUS_FORMAT = 7;
   private static final char DELIM = ':';
   private static final char DELIMITER_DEGREES = '°';
   private static final char DELIMITER_MINUTES = '′';
   private static final char DELIMITER_SECONDS = '″';
   private static final char DELIMITER_SPACE = ' ';

   public static double convert(String coordinate, boolean throwException) {
      coordinate = coordinate.replace(' ', ':').replace('#', ':').replace(',', '.').replace('\'', ':').replace('"', ':');
      if (coordinate == null) {
         if (!throwException) {
            return Double.NaN;
         } else {
            throw new NullPointerException("coordinate");
         }
      } else {
         boolean negative = false;
         if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            negative = true;
         }

         StringTokenizer st = new StringTokenizer(coordinate, ":");
         int tokens = st.countTokens();
         if (tokens < 1) {
            if (!throwException) {
               return Double.NaN;
            } else {
               throw new IllegalArgumentException("coordinate=" + coordinate);
            }
         } else {
            try {
               String degrees = st.nextToken();
               if (tokens == 1) {
                  double val = Double.parseDouble(degrees);
                  return negative ? -val : val;
               } else {
                  String minutes = st.nextToken();
                  int deg = Integer.parseInt(degrees);
                  double sec = 0.0;
                  double min;
                  if (st.hasMoreTokens()) {
                     min = (double)Integer.parseInt(minutes);
                     String seconds = st.nextToken();
                     sec = Double.parseDouble(seconds);
                  } else {
                     min = Double.parseDouble(minutes);
                  }

                  boolean isNegative180 = negative && deg == 180 && min == 0.0 && sec == 0.0;
                  if ((double)deg < 0.0 || deg > 180 && !isNegative180) {
                     if (!throwException) {
                        return Double.NaN;
                     } else {
                        throw new IllegalArgumentException("coordinate=" + coordinate);
                     }
                  } else if (!(min < 0.0) && !(min > 60.0)) {
                     if (!(sec < 0.0) && !(sec > 60.0)) {
                        double val = (double)deg * 3600.0 + min * 60.0 + sec;
                        val /= 3600.0;
                        return negative ? -val : val;
                     } else if (!throwException) {
                        return Double.NaN;
                     } else {
                        throw new IllegalArgumentException("coordinate=" + coordinate);
                     }
                  } else if (!throwException) {
                     return Double.NaN;
                  } else {
                     throw new IllegalArgumentException("coordinate=" + coordinate);
                  }
               }
            } catch (NumberFormatException var15) {
               if (!throwException) {
                  return Double.NaN;
               } else {
                  throw new IllegalArgumentException("coordinate=" + coordinate);
               }
            }
         }
      }
   }

   public static String convert(double coordinate, int outputType) {
      if (coordinate < -180.0 || coordinate > 180.0 || Double.isNaN(coordinate)) {
         throw new IllegalArgumentException("coordinate=" + coordinate);
      } else if (outputType != 0 && outputType != 1 && outputType != 2) {
         throw new IllegalArgumentException("outputType=" + outputType);
      } else {
         StringBuilder sb = new StringBuilder();
         if (coordinate < 0.0) {
            sb.append('-');
            coordinate = -coordinate;
         }

         DecimalFormat df = new DecimalFormat("##0.00000", new DecimalFormatSymbols(Locale.US));
         if (outputType == 1 || outputType == 2) {
            coordinate = formatCoordinate(coordinate, sb, ':');
            if (outputType == 2) {
               coordinate = formatCoordinate(coordinate, sb, ':');
            }
         }

         sb.append(df.format(coordinate));
         return sb.toString();
      }
   }

   public static String convertLatitude(double latitude, int outputType, boolean addCardinalDirection) {
      if (latitude < -90.0 || latitude > 90.0 || Double.isNaN(latitude)) {
         throw new IllegalArgumentException("latitude=" + latitude);
      } else if (outputType != 0 && outputType != 1 && outputType != 2) {
         throw new IllegalArgumentException("outputType=" + outputType);
      } else {
         StringBuilder sb = new StringBuilder();
         if (!addCardinalDirection && latitude < 0.0) {
            sb.append('-');
         }

         formatDegrees(latitude < 0.0 ? -latitude : latitude, outputType, sb);
         if (addCardinalDirection) {
            sb.append(' ').append((char)(latitude < 0.0 ? 'S' : 'N'));
         }

         return sb.toString();
      }
   }

   public static String convertLongitude(double longitude, int outputType, boolean addCardinalDirection) {
      if (longitude < -180.0 || longitude > 180.0 || Double.isNaN(longitude)) {
         throw new IllegalArgumentException("longitude=" + longitude);
      } else if (outputType != 0 && outputType != 1 && outputType != 2) {
         throw new IllegalArgumentException("outputType=" + outputType);
      } else {
         StringBuilder sb = new StringBuilder();
         if (!addCardinalDirection && longitude < 0.0) {
            sb.append('-');
         }

         formatDegrees(longitude < 0.0 ? -longitude : longitude, outputType, sb);
         if (addCardinalDirection) {
            sb.append(' ').append((char)(longitude < 0.0 ? 'W' : 'E'));
         }

         return sb.toString();
      }
   }

   private static double formatCoordinate(double coordinate, StringBuilder sb, char delimiter) {
      int deg = (int)Math.floor(coordinate);
      sb.append(deg);
      sb.append(delimiter);
      coordinate -= (double)deg;
      return coordinate * 60.0;
   }

   private static String formatDegrees(double coordinate, int outputType, StringBuilder sb) {
      if (outputType == 0) {
         sb.append(new DecimalFormat("##0.00000", new DecimalFormatSymbols(Locale.US)).format(coordinate));
         sb.append('°');
      } else if (outputType == 1) {
         coordinate = formatCoordinate(coordinate, sb, '°');
         sb.append(' ');
         sb.append(new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.US)).format(coordinate));
         sb.append('′');
      } else if (outputType == 2) {
         coordinate = formatCoordinate(coordinate, sb, '°');
         sb.append(' ');
         coordinate = formatCoordinate(coordinate, sb, '′');
         sb.append(' ');
         sb.append(new DecimalFormat("00.0", new DecimalFormatSymbols(Locale.US)).format(coordinate));
         sb.append('″');
      }

      return sb.toString();
   }
}
