package net.osmand.util;

import com.google.openlocationcode.OpenLocationCode;
import com.google.openlocationcode.OpenLocationCode.CodeArea;
import com.jwetherell.openmap.common.LatLonPoint;
import com.jwetherell.openmap.common.MGRSPoint;
import com.jwetherell.openmap.common.UTMPoint;
import java.util.ArrayList;
import java.util.List;
import net.osmand.data.LatLon;

public class LocationParser {
   public static boolean isValidOLC(String code) {
      return OpenLocationCode.isValidCode(code);
   }

   public static LocationParser.ParsedOpenLocationCode parseOpenLocationCode(String locPhrase) {
      LocationParser.ParsedOpenLocationCode parsedCode = new LocationParser.ParsedOpenLocationCode(locPhrase.trim());
      return !parsedCode.isValidCode() ? null : parsedCode;
   }

   public static LatLon parseLocation(String locPhrase) {
      locPhrase = locPhrase.trim();
      boolean valid = isValidLocPhrase(locPhrase);
      if (!valid) {
         String[] split = locPhrase.split(" ");
         if (split.length == 4 && split[1].contains(".") && split[3].contains(".")) {
            locPhrase = split[1] + " " + split[3];
            valid = isValidLocPhrase(locPhrase);
         }
      }

      if (!valid) {
         return null;
      } else {
         List<Double> d = new ArrayList<>();
         List<Object> all = new ArrayList<>();
         List<String> strings = new ArrayList<>();
         splitObjects(locPhrase, d, all, strings);
         if (d.size() == 0) {
            return null;
         } else {
            if (all.size() == 4 && d.size() == 3 && all.get(1) instanceof String && ((String)all.get(1)).length() == 1) {
               char ch = all.get(1).toString().charAt(0);
               if (Character.isLetter(ch)) {
                  UTMPoint upoint = new UTMPoint(d.get(2), d.get(1), d.get(0).intValue(), ch);
                  LatLonPoint ll = upoint.toLatLonPoint();
                  return validateAndCreateLatLon((double)ll.getLatitude(), (double)ll.getLongitude());
               }
            }

            if (all.size() == 3 && d.size() == 2 && all.get(1) instanceof String && ((String)all.get(1)).length() == 1) {
               char ch = all.get(1).toString().charAt(0);
               String combined = strings.get(2);
               if (Character.isLetter(ch)) {
                  try {
                     String east = combined.substring(0, combined.length() / 2);
                     String north = combined.substring(combined.length() / 2);
                     UTMPoint upoint = new UTMPoint(Double.parseDouble(north), Double.parseDouble(east), d.get(0).intValue(), ch);
                     LatLonPoint ll = upoint.toLatLonPoint();
                     return validateAndCreateLatLon((double)ll.getLatitude(), (double)ll.getLongitude());
                  } catch (NumberFormatException var20) {
                  }
               }
            }

            if (all.size() >= 3 && (d.size() == 2 || d.size() == 3) && all.get(1) instanceof String) {
               try {
                  MGRSPoint mgrsPoint = new MGRSPoint(locPhrase);
                  LatLonPoint ll = mgrsPoint.toLatLonPoint();
                  return validateAndCreateLatLon((double)ll.getLatitude(), (double)ll.getLongitude());
               } catch (NumberFormatException var19) {
               }
            }

            int jointNumbers = 0;
            int lastJoin = 0;
            int degSplit = -1;
            int degType = -1;
            boolean finishDegSplit = false;
            int northSplit = -1;
            int eastSplit = -1;

            for(int i = 1; i < all.size(); ++i) {
               if (all.get(i - 1) instanceof Double && all.get(i) instanceof Double) {
                  ++jointNumbers;
                  lastJoin = i;
               }

               if (all.get(i).equals("n") || all.get(i).equals("s") || all.get(i).equals("N") || all.get(i).equals("S")) {
                  northSplit = i + 1;
               }

               if (all.get(i).equals("e") || all.get(i).equals("w") || all.get(i).equals("E") || all.get(i).equals("W")) {
                  eastSplit = i;
               }

               int dg = -1;
               if (all.get(i).equals("°")) {
                  dg = 0;
               } else if (all.get(i).equals("'") || all.get(i).equals("′")) {
                  dg = 1;
               } else if (all.get(i).equals("″") || all.get(i).equals("\"")) {
                  dg = 2;
               }

               if (dg != -1) {
                  if (!finishDegSplit) {
                     if (degType < dg) {
                        degSplit = i + 1;
                        degType = dg;
                     } else {
                        finishDegSplit = true;
                        degType = dg;
                     }
                  } else if (degType < dg) {
                     degType = dg;
                  } else {
                     degSplit = -1;
                  }
               }
            }

            int split = -1;
            if (jointNumbers == 1) {
               split = lastJoin;
            }

            if (northSplit != -1 && northSplit < all.size() - 1) {
               split = northSplit;
            } else if (eastSplit != -1 && eastSplit < all.size() - 1) {
               split = eastSplit;
            } else if (degSplit != -1 && degSplit < all.size() - 1) {
               split = degSplit;
            }

            if (split != -1) {
               double lat = parse1Coordinate(all, 0, split);
               double lon = parse1Coordinate(all, split, all.size());
               return validateAndCreateLatLon(lat, lon);
            } else if (d.size() == 2) {
               return validateAndCreateLatLon(d.get(0), d.get(1));
            } else {
               if (locPhrase.contains("://")) {
                  double lat = 0.0;
                  double lon = 0.0;
                  boolean only2decimals = true;

                  for(int i = 0; i < d.size(); ++i) {
                     if (d.get(i) != (double)d.get(i).intValue()) {
                        if (lat == 0.0) {
                           lat = d.get(i);
                        } else if (lon == 0.0) {
                           lon = d.get(i);
                        } else {
                           only2decimals = false;
                        }
                     }
                  }

                  if (lat != 0.0 && lon != 0.0 && only2decimals) {
                     return validateAndCreateLatLon(lat, lon);
                  }
               }

               if (d.size() > 2 && d.size() % 2 == 0) {
                  int ind = d.size() / 2 + 1;
                  int splitEq = -1;

                  for(int i = 0; i < all.size(); ++i) {
                     if (all.get(i) instanceof Double) {
                        --ind;
                     }

                     if (ind == 0) {
                        splitEq = i;
                        break;
                     }
                  }

                  if (splitEq != -1) {
                     double lat = parse1Coordinate(all, 0, splitEq);
                     double lon = parse1Coordinate(all, splitEq, all.size());
                     return validateAndCreateLatLon(lat, lon);
                  }
               }

               return null;
            }
         }
      }
   }

   private static LatLon validateAndCreateLatLon(double lat, double lon) {
      return Math.abs(lat) <= 90.0 && Math.abs(lon) <= 180.0 ? new LatLon(lat, lon) : null;
   }

   private static boolean isValidLocPhrase(String locPhrase) {
      if (locPhrase.isEmpty()) {
         return false;
      } else {
         char ch = Character.toLowerCase(locPhrase.charAt(0));
         return ch == '-' || Character.isDigit(ch) || ch == 's' || ch == 'n' || locPhrase.contains("://");
      }
   }

   public static double parse1Coordinate(List<Object> all, int begin, int end) {
      boolean neg = false;
      double d = 0.0;
      int type = 0;
      Double prevDouble = null;

      for(int i = begin; i <= end; ++i) {
         Object o = i == end ? "" : all.get(i);
         if (o.equals("S") || o.equals("s") || o.equals("W") || o.equals("w") || o.equals(-0.0)) {
            neg = !neg;
         }

         if (prevDouble != null) {
            if (o.equals("°")) {
               type = 0;
            } else if (o.equals("′")) {
               type = 1;
            } else if (o.equals("\"") || o.equals("″")) {
               type = 2;
            }

            if (type == 0) {
               double ld = prevDouble;
               if (ld < 0.0) {
                  ld = -ld;
                  neg = true;
               }

               d += ld;
            } else if (type == 1) {
               d += prevDouble / 60.0;
            } else {
               d += prevDouble / 3600.0;
            }

            ++type;
         }

         if (o instanceof Double) {
            prevDouble = (Double)o;
         } else {
            prevDouble = null;
         }
      }

      if (neg) {
         d = -d;
      }

      return d;
   }

   public static void splitObjects(String s, List<Double> d, List<Object> all, List<String> strings) {
      splitObjects(s, d, all, strings, new boolean[]{false});
   }

   public static void splitObjects(String s, List<Double> d, List<Object> all, List<String> strings, boolean[] partial) {
      boolean digit = false;
      int word = -1;
      int firstNumeralIdx = -1;

      for(int i = 0; i <= s.length(); ++i) {
         char ch = i == s.length() ? 32 : s.charAt(i);
         boolean dg = Character.isDigit(ch);
         boolean nonwh = ch != ',' && ch != ' ' && ch != ';';
         if (ch != '.' && !dg && ch != '-') {
            if (digit && word != -1) {
               try {
                  double dl = Double.parseDouble(s.substring(word, i));
                  d.add(dl);
                  all.add(dl);
                  if (firstNumeralIdx == -1) {
                     firstNumeralIdx = all.size() - 1;
                  }

                  strings.add(s.substring(word, i));
                  digit = false;
                  word = -1;
               } catch (NumberFormatException var14) {
               }
            }

            if (nonwh) {
               if (!Character.isLetter(ch)) {
                  if (word != -1) {
                     all.add(s.substring(word, i));
                     strings.add(s.substring(word, i));
                  }

                  all.add(s.substring(i, i + 1));
                  strings.add(s.substring(i, i + 1));
                  word = -1;
               } else if (word == -1) {
                  word = i;
               }
            } else {
               if (word != -1) {
                  all.add(s.substring(word, i));
                  strings.add(s.substring(word, i));
               }

               word = -1;
            }
         } else if (!digit) {
            if (word != -1) {
               all.add(s.substring(word, i));
               strings.add(s.substring(word, i));
            }

            digit = true;
            word = i;
         } else if (word == -1) {
            word = i;
         }
      }

      partial[0] = false;
      if (firstNumeralIdx != -1) {
         int nextTokenIdx = firstNumeralIdx + 1;
         if (all.size() <= nextTokenIdx) {
            partial[0] = true;
         }
      }
   }

   public static class ParsedOpenLocationCode {
      private final String text;
      private String code;
      private boolean full;
      private String placeName;
      private OpenLocationCode olc;
      private LatLon latLon;

      private ParsedOpenLocationCode(String text) {
         this.text = text;
         this.parse();
      }

      private void parse() {
         if (!Algorithms.isEmpty(this.text)) {
            String[] split = this.text.split(" ");
            if (split.length > 0) {
               this.code = split[0];

               try {
                  this.olc = new OpenLocationCode(this.code);
                  this.full = this.olc.isFull();
                  if (this.full) {
                     CodeArea codeArea = this.olc.decode();
                     this.latLon = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
                  } else if (split.length > 1) {
                     this.placeName = this.text.substring(this.code.length() + 1);
                  }
               } catch (IllegalArgumentException var3) {
                  this.code = null;
               }
            }
         }
      }

      public LatLon recover(LatLon searchLocation) {
         if (this.olc != null) {
            CodeArea codeArea = this.olc.recover(searchLocation.getLatitude(), searchLocation.getLongitude()).decode();
            this.latLon = new LatLon(codeArea.getCenterLatitude(), codeArea.getCenterLongitude());
         }

         return this.latLon;
      }

      boolean isValidCode() {
         return !Algorithms.isEmpty(this.code);
      }

      public String getText() {
         return this.text;
      }

      public String getCode() {
         return this.code;
      }

      public boolean isFull() {
         return this.full;
      }

      public String getPlaceName() {
         return this.placeName;
      }

      public LatLon getLatLon() {
         return this.latLon;
      }
   }
}
