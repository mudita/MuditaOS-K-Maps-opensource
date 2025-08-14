package net.osmand.util;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.util.Collection;
import net.osmand.GPXUtilities;
import net.osmand.data.LatLon;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;

public class MapAlgorithms {
   public static void main(String[] args) {
      int STEP = 30;
      TIntArrayList altIncs = new TIntArrayList(new int[]{0, 10, -1, 5, -3, 2, 3, -16, -1});
      String str = encodeIntHeightArrayGraph(STEP, altIncs, 3);
      TIntArrayList decodedSteps = decodeIntHeightArrayGraph(str, 3);
      GPXUtilities.GPXFile gpx = GPXUtilities.loadGPXFile(new File("/Users/victorshcherb/osmand/maps/tracks/rec/2015-01-19_02-43_Mon.gpx"));
      GPXUtilities.TrkSegment sgm = gpx.tracks.get(0).segments.get(0);
      double startEle = 130.0;
      augmentTrkSegmentWithAltitudes(sgm, decodedSteps, startEle);
   }

   public static void augmentTrkSegmentWithAltitudes(GPXUtilities.TrkSegment sgm, TIntArrayList decodedSteps, double startEle) {
      int stepDist = decodedSteps.get(0);
      int stepHNextInd = 1;
      double prevHDistX = 0.0;
      sgm.points.get(0).ele = startEle;

      for(int i = 1; i < sgm.points.size(); ++i) {
         GPXUtilities.WptPt prev = sgm.points.get(i - 1);
         GPXUtilities.WptPt cur = sgm.points.get(i);
         double origHDistX = prevHDistX;
         double len = MapUtils.getDistance(prev.getLatitude(), prev.getLongitude(), cur.getLatitude(), cur.getLongitude()) / (double)stepDist;
         double curHDistX = len + prevHDistX;

         double hInc;
         for(hInc = 0.0; curHDistX > (double)stepHNextInd && stepHNextInd < decodedSteps.size(); ++stepHNextInd) {
            if (prevHDistX < (double)stepHNextInd) {
               hInc += ((double)stepHNextInd - prevHDistX) * (double)decodedSteps.get(stepHNextInd);
               if ((double)stepHNextInd - prevHDistX > 0.5) {
                  double fraction = ((double)stepHNextInd - prevHDistX) / (curHDistX - origHDistX);
                  GPXUtilities.WptPt newPt = new GPXUtilities.WptPt();
                  newPt.lat = prev.getLatitude() + fraction * (cur.getLatitude() - prev.getLatitude());
                  newPt.lon = prev.getLongitude() + fraction * (cur.getLongitude() - prev.getLongitude());
                  newPt.ele = prev.ele + hInc;
                  sgm.points.add(i, newPt);
                  ++i;
               }

               prevHDistX = (double)stepHNextInd;
            }
         }

         if (stepHNextInd < decodedSteps.size()) {
            hInc += (curHDistX - prevHDistX) * (double)decodedSteps.get(stepHNextInd);
         }

         cur.ele = prev.ele + hInc;
         prevHDistX = curHDistX;
      }
   }

   public static TIntArrayList decodeIntHeightArrayGraph(String str, int repeatBits) {
      int maxRepeats = (1 << repeatBits) - 1;
      TIntArrayList res = new TIntArrayList();
      char[] ch = str.toCharArray();
      res.add(ch[0]);

      for(int i = 1; i < ch.length; ++i) {
         char c = ch[i];

         for(int rept = c & maxRepeats; rept > 0; --rept) {
            res.add(0);
         }

         int num = c >> repeatBits;
         if (num % 2 == 0) {
            res.add(num >> 1);
         } else {
            res.add(-(num >> 1));
         }
      }

      return res;
   }

   public static String encodeIntHeightArrayGraph(int step, TIntArrayList array, int repeatBits) {
      int maxRepeats = (1 << repeatBits) - 1;
      TIntArrayList ch = new TIntArrayList();
      ch.add(step);
      int repeat = 0;

      for(int i = 0; i < array.size(); ++i) {
         int altInc = array.get(i);
         if (altInc == 0 && repeat != maxRepeats) {
            ++repeat;
         } else {
            int posAltInc = Math.abs(altInc);
            int sign = altInc < 0 ? 1 : 0;
            char c = (char)((posAltInc << 1) + sign << repeatBits);
            c = (char)(c + repeat);
            ch.add(c);
            repeat = 0;
         }
      }

      char[] c = new char[ch.size()];

      for(int i = 0; i < ch.size(); ++i) {
         c[i] = (char)ch.get(i);
      }

      return new String(c);
   }

   public static boolean isClockwiseWay(TLongList c) {
      if (c.size() == 0) {
         return true;
      } else {
         long mask = 4294967295L;
         long middleY = 0L;

         for(int i = 0; i < c.size(); ++i) {
            middleY += c.get(i) & mask;
         }

         middleY /= (long)c.size();
         double clockwiseSum = 0.0;
         boolean firstDirectionUp = false;
         int previousX = Integer.MIN_VALUE;
         int firstX = Integer.MIN_VALUE;
         int prevX = (int)(c.get(0) >> 32);
         int prevY = (int)(c.get(0) & mask);

         for(int i = 1; i < c.size(); ++i) {
            int x = (int)(c.get(i) >> 32);
            int y = (int)(c.get(i) & mask);
            int rX = ray_intersect_x(prevX, prevY, x, y, (int)middleY);
            if (rX != Integer.MIN_VALUE) {
               boolean skipSameSide = (long)y <= middleY == (long)prevY <= middleY;
               if (skipSameSide) {
                  continue;
               }

               boolean directionUp = (long)prevY >= middleY;
               if (firstX == Integer.MIN_VALUE) {
                  firstDirectionUp = directionUp;
                  firstX = rX;
               } else {
                  boolean clockwise = !directionUp == previousX < rX;
                  if (clockwise) {
                     clockwiseSum += (double)Math.abs(previousX - rX);
                  } else {
                     clockwiseSum -= (double)Math.abs(previousX - rX);
                  }
               }

               previousX = rX;
            }

            prevX = x;
            prevY = y;
         }

         if (firstX != Integer.MIN_VALUE) {
            boolean clockwise = !firstDirectionUp == previousX < firstX;
            if (clockwise) {
               clockwiseSum += (double)Math.abs(previousX - firstX);
            } else {
               clockwiseSum -= (double)Math.abs(previousX - firstX);
            }
         }

         return clockwiseSum >= 0.0;
      }
   }

   public static int ray_intersect_x(int prevX, int prevY, int x, int y, int middleY) {
      if (prevY > y) {
         int tx = x;
         int ty = y;
         x = prevX;
         y = prevY;
         prevX = tx;
         prevY = ty;
      }

      if (y == middleY || prevY == middleY) {
         --middleY;
      }

      if (prevY > middleY || y < middleY) {
         return Integer.MIN_VALUE;
      } else if (y == prevY) {
         return x;
      } else {
         double rx = (double)x + ((double)middleY - (double)y) * ((double)x - (double)prevX) / ((double)y - (double)prevY);
         return (int)rx;
      }
   }

   private static long combine2Points(int x, int y) {
      return (long)x << 32 | (long)y;
   }

   public static long calculateIntersection(int inx, int iny, int outx, int outy, int leftX, int rightX, int bottomY, int topY) {
      int by = -1;
      int bx = -1;
      if (outy < topY && iny >= topY) {
         int tx = (int)((double)outx + (double)(inx - outx) * (double)(topY - outy) / (double)(iny - outy));
         if (leftX <= tx && tx <= rightX) {
            return combine2Points(tx, topY);
         }
      }

      if (outy > bottomY && iny <= bottomY) {
         int tx = (int)((double)outx + (double)(inx - outx) * (double)(outy - bottomY) / (double)(outy - iny));
         if (leftX <= tx && tx <= rightX) {
            return combine2Points(tx, bottomY);
         }
      }

      if (outx < leftX && inx >= leftX) {
         int ty = (int)((double)outy + (double)(iny - outy) * (double)(leftX - outx) / (double)(inx - outx));
         if (ty >= topY && ty <= bottomY) {
            return combine2Points(leftX, ty);
         }
      }

      if (outx > rightX && inx <= rightX) {
         int ty = (int)((double)outy + (double)(iny - outy) * (double)(outx - rightX) / (double)(outx - inx));
         if (ty >= topY && ty <= bottomY) {
            return combine2Points(rightX, ty);
         }
      }

      if (outy > topY && iny <= topY) {
         int tx = (int)((double)outx + (double)(inx - outx) * (double)(topY - outy) / (double)(iny - outy));
         if (leftX <= tx && tx <= rightX) {
            return combine2Points(tx, topY);
         }
      }

      if (outy < bottomY && iny >= bottomY) {
         int tx = (int)((double)outx + (double)(inx - outx) * (double)(outy - bottomY) / (double)(outy - iny));
         if (leftX <= tx && tx <= rightX) {
            return combine2Points(tx, bottomY);
         }
      }

      if (outx > leftX && inx <= leftX) {
         int ty = (int)((double)outy + (double)(iny - outy) * (double)(leftX - outx) / (double)(inx - outx));
         if (ty >= topY && ty <= bottomY) {
            return combine2Points(leftX, ty);
         }
      }

      if (outx < rightX && inx >= rightX) {
         int ty = (int)((double)outy + (double)(iny - outy) * (double)(outx - rightX) / (double)(outx - inx));
         if (ty >= topY && ty <= bottomY) {
            return combine2Points(rightX, ty);
         }
      }

      if ((outx == rightX || outx == leftX) && outy >= topY && outy <= bottomY) {
         return combine2Points(outx, outy);
      } else {
         return (outy == topY || outy == bottomY) && leftX <= outx && outx <= rightX ? combine2Points(outx, outy) : -1L;
      }
   }

   public static boolean linesIntersect(LatLon a, LatLon b, LatLon c, LatLon d) {
      return linesIntersect(
         a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude(), c.getLatitude(), c.getLongitude(), d.getLatitude(), d.getLongitude()
      );
   }

   public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
      if ((x1 != x2 || y1 != y2) && (x3 != x4 || y3 != y4)) {
         double ax = x2 - x1;
         double ay = y2 - y1;
         double bx = x3 - x4;
         double by = y3 - y4;
         double cx = x1 - x3;
         double cy = y1 - y3;
         double alphaNumerator = by * cx - bx * cy;
         double commonDenominator = ay * bx - ax * by;
         if (commonDenominator > 0.0) {
            if (alphaNumerator < 0.0 || alphaNumerator > commonDenominator) {
               return false;
            }
         } else if (commonDenominator < 0.0 && (alphaNumerator > 0.0 || alphaNumerator < commonDenominator)) {
            return false;
         }

         double betaNumerator = ax * cy - ay * cx;
         if (commonDenominator > 0.0) {
            if (betaNumerator < 0.0 || betaNumerator > commonDenominator) {
               return false;
            }
         } else if (commonDenominator < 0.0 && (betaNumerator > 0.0 || betaNumerator < commonDenominator)) {
            return false;
         }

         if (commonDenominator != 0.0) {
            return true;
         } else {
            double y3LessY1 = y3 - y1;
            double collinearityTestForP3 = x1 * (y2 - y3) + x2 * y3LessY1 + x3 * (y1 - y2);
            return collinearityTestForP3 == 0.0
               && (
                  x1 >= x3 && x1 <= x4 || x1 <= x3 && x1 >= x4 || x2 >= x3 && x2 <= x4 || x2 <= x3 && x2 >= x4 || x3 >= x1 && x3 <= x2 || x3 <= x1 && x3 >= x2
               )
               && (
                  y1 >= y3 && y1 <= y4 || y1 <= y3 && y1 >= y4 || y2 >= y3 && y2 <= y4 || y2 <= y3 && y2 >= y4 || y3 >= y1 && y3 <= y2 || y3 <= y1 && y3 >= y2
               );
         }
      } else {
         return false;
      }
   }

   public static boolean containsPoint(Collection<Node> polyNodes, double latitude, double longitude) {
      return countIntersections(polyNodes, latitude, longitude) % 2 == 1;
   }

   public static int countIntersections(Collection<Node> polyNodes, double latitude, double longitude) {
      int intersections = 0;
      if (polyNodes.size() == 0) {
         return 0;
      } else {
         Node prev = null;
         Node first = null;
         Node last = null;

         for(Node n : polyNodes) {
            if (prev == null) {
               prev = n;
               first = n;
            } else if (n != null) {
               last = n;
               if (OsmMapUtils.ray_intersect_lon(prev, n, latitude, longitude) != -360.0) {
                  ++intersections;
               }

               prev = n;
            }
         }

         if (first != null && last != null) {
            if (OsmMapUtils.ray_intersect_lon(first, last, latitude, longitude) != -360.0) {
               ++intersections;
            }

            return intersections;
         } else {
            return 0;
         }
      }
   }
}
