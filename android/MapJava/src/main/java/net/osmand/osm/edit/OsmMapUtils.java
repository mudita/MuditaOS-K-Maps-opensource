package net.osmand.osm.edit;

import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class OsmMapUtils {

   public static double getDistance(Node e1, Node e2) {
      return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), e2.getLatitude(), e2.getLongitude());
   }

   public static double getDistance(Node e1, double latitude, double longitude) {
      return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), latitude, longitude);
   }

   public static double getDistance(Node e1, LatLon point) {
      return MapUtils.getDistance(e1.getLatitude(), e1.getLongitude(), point.getLatitude(), point.getLongitude());
   }

   public static LatLon getComplexPolyCenter(Collection<Node> outer, List<List<Node>> inner) {
      if (outer.size() > 3 && outer.size() <= 5 && inner == null) {
         List<Node> sub = new ArrayList<>(outer);
         return getWeightCenterForNodes(sub.subList(0, sub.size() - 1));
      } else {
         List<List<LatLon>> rings = new ArrayList<>();
         List<LatLon> outerRing = new ArrayList<>();

         for(Node n : outer) {
            outerRing.add(new LatLon(n.getLatitude(), n.getLongitude()));
         }

         rings.add(outerRing);
         if (!Algorithms.isEmpty(inner)) {
            for(List<Node> ring : inner) {
               if (!Algorithms.isEmpty(ring)) {
                  List<LatLon> ringll = new ArrayList<>();

                  for(Node n : ring) {
                     ringll.add(n.getLatLon());
                  }

                  rings.add(ringll);
               }
            }
         }

         return getPolylabelPoint(rings);
      }
   }

   public static LatLon getWeightCenterForNodes(Collection<Node> nodes) {
      if (nodes.isEmpty()) {
         return null;
      } else {
         double longitude = 0.0;
         double latitude = 0.0;
         int count = 0;

         for(Node n : nodes) {
            if (n != null) {
               ++count;
               longitude += n.getLongitude();
               latitude += n.getLatitude();
            }
         }

         return count == 0 ? null : new LatLon(latitude / (double)count, longitude / (double)count);
      }
   }

   public static LatLon getWeightCenterForWay(Way w) {
      List<Node> nodes = w.getNodes();
      if (nodes.isEmpty()) {
         return null;
      } else {
         boolean area = w.getFirstNodeId() == w.getLastNodeId();
         if (area) {
            Node fn = w.getFirstNode();
            Node ln = w.getLastNode();
            if (fn != null && fn != null && MapUtils.getDistance(fn.getLatLon(), ln.getLatLon()) < 50.0) {
               area = true;
            } else {
               area = false;
            }
         }

         LatLon ll = area ? getComplexPolyCenter(nodes, null) : getWeightCenterForNodes(nodes);
         if (ll == null) {
            return null;
         } else {
            double flat = ll.getLatitude();
            double flon = ll.getLongitude();
            if (!area || !MapAlgorithms.containsPoint(nodes, ll.getLatitude(), ll.getLongitude())) {
               double minDistance = Double.MAX_VALUE;

               for(Node n : nodes) {
                  if (n != null) {
                     double d = MapUtils.getDistance(n.getLatitude(), n.getLongitude(), ll.getLatitude(), ll.getLongitude());
                     if (d < minDistance) {
                        flat = n.getLatitude();
                        flon = n.getLongitude();
                        minDistance = d;
                     }
                  }
               }
            }

            return new LatLon(flat, flon);
         }
      }
   }

   public static void simplifyDouglasPeucker(List<Node> nodes, int start, int end, List<Node> survivedNodes, double epsilon) {
      double dmax = Double.NEGATIVE_INFINITY;
      int index = -1;
      Node startPt = nodes.get(start);
      Node endPt = nodes.get(end);

      for(int i = start + 1; i < end; ++i) {
         Node pt = nodes.get(i);
         double d = MapUtils.getOrthogonalDistance(
            pt.getLatitude(), pt.getLongitude(), startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude()
         );
         if (d > dmax) {
            dmax = d;
            index = i;
         }
      }

      if (dmax > epsilon) {
         simplifyDouglasPeucker(nodes, start, index, survivedNodes, epsilon);
         simplifyDouglasPeucker(nodes, index, end, survivedNodes, epsilon);
      } else {
         survivedNodes.add(nodes.get(end));
      }
   }

   public static double ray_intersect_lon(Node node, Node node2, double latitude, double longitude) {
      Node a = node.getLatitude() < node2.getLatitude() ? node : node2;
      Node b = a == node2 ? node : node2;
      if (latitude == a.getLatitude() || latitude == b.getLatitude()) {
         latitude += 1.0E-8;
      }

      if (!(latitude < a.getLatitude()) && !(latitude > b.getLatitude())) {
         if (longitude < Math.min(a.getLongitude(), b.getLongitude())) {
            return -360.0;
         } else if (a.getLongitude() == b.getLongitude() && longitude == a.getLongitude()) {
            return longitude;
         } else {
            double lon = b.getLongitude() - (b.getLatitude() - latitude) * (b.getLongitude() - a.getLongitude()) / (b.getLatitude() - a.getLatitude());
            return lon <= longitude ? lon : -360.0;
         }
      } else {
         return -360.0;
      }
   }

   public static double getArea(List<Node> nodes) {
      double refX = 500.0;
      double refY = 500.0;

      for(Node n : nodes) {
         if (n.getLatitude() < refY) {
            refY = n.getLatitude();
         }

         if (n.getLongitude() < refX) {
            refX = n.getLongitude();
         }
      }

      List<Double> xVal = new ArrayList<>();
      List<Double> yVal = new ArrayList<>();

      for(Node n : nodes) {
         double xDist = MapUtils.getDistance(refY, refX, refY, n.getLongitude());
         double yDist = MapUtils.getDistance(refY, refX, n.getLatitude(), refX);
         xVal.add(xDist);
         yVal.add(yDist);
      }

      double area = 0.0;

      for(int i = 1; i < xVal.size(); ++i) {
         area += xVal.get(i - 1) * yVal.get(i) - xVal.get(i) * yVal.get(i - 1);
      }

      return Math.abs(area) / 2.0;
   }

   public static LatLon getPolylabelPoint(List<List<LatLon>> rings) {
      double minX = Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;

      for(LatLon p : rings.get(0)) {
         double lat = p.getLatitude();
         double lon = p.getLongitude();
         minX = StrictMath.min(minX, lon);
         minY = StrictMath.min(minY, lat);
         maxX = StrictMath.max(maxX, lon);
         maxY = StrictMath.max(maxY, lat);
      }

      double width = maxX - minX;
      double height = maxY - minY;
      double cellSize = Math.min(width, height);
      double h = cellSize / 2.0;
      if (cellSize == 0.0) {
         return new LatLon(minX, minY);
      } else {
         PriorityQueue<OsmMapUtils.Cell> cellQueue = new PriorityQueue<>(new OsmMapUtils.CellComparator());

         for(double x = minX; x < maxX; x += cellSize) {
            for(double y = minY; y < maxY; y += cellSize) {
               cellQueue.add(new OsmMapUtils.Cell(x + h, y + h, h, rings));
            }
         }

         OsmMapUtils.Cell bestCell = getCentroidCell(rings);
         if (bestCell == null) {
            return new LatLon(minX, minY);
         } else {
            OsmMapUtils.Cell bboxCell = new OsmMapUtils.Cell(minX + width / 2.0, minY + height / 2.0, 0.0, rings);
            if (bboxCell.d > bestCell.d) {
               bestCell = bboxCell;
            }

            int count = 0;

            while(!cellQueue.isEmpty()) {
               if (count > 10000000) {
                  System.err.println("Error loop limitation: 10000000");
                  break;
               }

               OsmMapUtils.Cell cell = cellQueue.poll();
               if (cell.d > bestCell.d) {
                  bestCell = cell;
               }

               if (!(cell.max - bestCell.d <= 1.0E-6)) {
                  h = cell.h / 2.0;
                  cellQueue.add(new OsmMapUtils.Cell(cell.x - h, cell.y - h, h, rings));
                  cellQueue.add(new OsmMapUtils.Cell(cell.x + h, cell.y - h, h, rings));
                  cellQueue.add(new OsmMapUtils.Cell(cell.x - h, cell.y + h, h, rings));
                  cellQueue.add(new OsmMapUtils.Cell(cell.x + h, cell.y + h, h, rings));
                  ++count;
               }
            }

            return new LatLon(bestCell.y, bestCell.x);
         }
      }
   }

   private static OsmMapUtils.Cell getCentroidCell(List<List<LatLon>> rings) {
      double area = 0.0;
      double x = 0.0;
      double y = 0.0;
      List<LatLon> points = rings.get(0);
      int i = 0;
      int len = points.size();

      for(int j = len - 1; i < len; j = i++) {
         LatLon a = points.get(i);
         LatLon b = points.get(j);
         double aLon = a.getLongitude();
         double aLat = a.getLatitude();
         double bLon = b.getLongitude();
         double bLat = b.getLatitude();
         double f = aLon * bLat - bLon * aLat;
         x += (aLon + bLon) * f;
         y += (aLat + bLat) * f;
         area += f * 3.0;
      }

      if (area == 0.0) {
         if (points.size() == 0) {
            return null;
         } else {
            LatLon p = points.get(0);
            return new OsmMapUtils.Cell(p.getLatitude(), p.getLongitude(), 0.0, rings);
         }
      } else {
         return new OsmMapUtils.Cell(x / area, y / area, 0.0, rings);
      }
   }

   private static class Cell {
      private final double x;
      private final double y;
      private final double h;
      private final double d;
      private final double max;

      private Cell(double x, double y, double h, List<List<LatLon>> rings) {
         this.x = x;
         this.y = y;
         this.h = h;
         this.d = this.pointToPolygonDist(x, y, rings);
         this.max = this.d + this.h * Math.sqrt(2.0);
      }

      private double pointToPolygonDist(double x, double y, List<List<LatLon>> rings) {
         boolean inside = false;
         double minDistSq = Double.MAX_VALUE;

         for(List<LatLon> ring : rings) {
            int i = 0;
            int len = ring.size();

            for(int j = len - 1; i < len; j = i++) {
               LatLon a = ring.get(i);
               LatLon b = ring.get(j);
               double aLon = a.getLongitude();
               double aLat = a.getLatitude();
               double bLon = b.getLongitude();
               double bLat = b.getLatitude();
               if (aLat > y != bLat > y && x < (bLon - aLon) * (y - aLat) / (bLat - aLat) + aLon) {
                  inside = !inside;
               }

               minDistSq = Math.min(minDistSq, this.getSegmentDistanceSqared(x, y, a, b));
            }
         }

         return (double)(inside ? 1 : -1) * Math.sqrt(minDistSq);
      }

      private double getSegmentDistanceSqared(double px, double py, LatLon a, LatLon b) {
         double x = a.getLongitude();
         double y = a.getLatitude();
         double dx = b.getLongitude() - x;
         double dy = b.getLatitude() - y;
         if (dx != 0.0 || dy != 0.0) {
            double t = ((px - x) * dx + (py - y) * dy) / (dx * dx + dy * dy);
            if (t > 1.0) {
               x = b.getLongitude();
               y = b.getLatitude();
            } else if (t > 0.0) {
               x += dx * t;
               y += dy * t;
            }
         }

         dx = px - x;
         dy = py - y;
         return dx * dx + dy * dy;
      }
   }

   private static class CellComparator implements Comparator<OsmMapUtils.Cell> {
      private CellComparator() {
      }

      public int compare(OsmMapUtils.Cell o1, OsmMapUtils.Cell o2) {
         return Double.compare(o2.max, o1.max);
      }
   }
}
