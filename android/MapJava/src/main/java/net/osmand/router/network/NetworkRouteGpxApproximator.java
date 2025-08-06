package net.osmand.router.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.osmand.GPXUtilities;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.util.MapUtils;

public class NetworkRouteGpxApproximator {
   private static final double GPX_MAX_DISTANCE_POINT_MATCH = 5.0;
   private static final double GPX_MAX_INTER_SKIP_DISTANCE = 10.0;
   private static final int GPX_SKIP_POINTS_GPX_MAX = 5;
   private final NetworkRouteSelector selector;
   public List<RouteSegmentResult> result = null;

   public NetworkRouteGpxApproximator(BinaryMapIndexReader[] files, boolean routing) {
      this.selector = new NetworkRouteSelector(files, new NetworkRouteSelector.NetworkRouteSelectorFilter() {
         @Override
         public List<NetworkRouteSelector.RouteKey> convert(RouteDataObject obj) {
            return Collections.singletonList(null);
         }

         @Override
         public List<NetworkRouteSelector.RouteKey> convert(BinaryMapDataObject obj) {
            return Collections.singletonList(null);
         }
      }, null, routing);
   }

   public List<RouteSegmentResult> approximate(GPXUtilities.GPXFile gpxFile, RoutingContext rCtx) throws IOException {
      List<NetworkRouteContext.NetworkRouteSegment> loaded = this.loadDataByGPX(gpxFile);
      List<RouteSegmentResult> res = new ArrayList<>();

      for(NetworkRouteContext.NetworkRouteSegment segment : loaded) {
         res.add(new RouteSegmentResult(segment.robj, segment.start, segment.end));
      }

      new RouteResultPreparation().prepareResult(rCtx, res, true);
      return this.result = res;
   }

   private double getDistance(NetworkRouteGpxApproximator.GpxRoutePoint start, NetworkRouteGpxApproximator.GpxRoutePoint end) {
      return MapUtils.getDistance(start.lat, start.lon, end.lat, end.lon);
   }

   private NetworkRouteContext.NetworkRouteSegment getMatchingGpxSegments(
      NetworkRouteGpxApproximator.GpxRoutePoint p1, NetworkRouteGpxApproximator.GpxRoutePoint p2
   ) {
      List<NetworkRouteContext.NetworkRouteSegment> segments = new ArrayList<>();

      for(NetworkRouteContext.NetworkRouteSegment segStart : p1.getObjects()) {
         for(NetworkRouteContext.NetworkRouteSegment segEnd : p2.getObjects()) {
            if (segEnd.getId() == segStart.getId() && segStart.start != segEnd.start) {
               segments.add(new NetworkRouteContext.NetworkRouteSegment(segStart, segStart.start, segEnd.start));
            }
         }
      }

      NetworkRouteContext.NetworkRouteSegment res = null;
      if (!segments.isEmpty()) {
         double minLength = Double.MAX_VALUE;

         for(NetworkRouteContext.NetworkRouteSegment segment : segments) {
            double length = segment.robj.distance(segment.start, segment.end);
            if (length < minLength) {
               minLength = length;
               res = segment;
            }
         }
      }

      return res;
   }

   private List<NetworkRouteContext.NetworkRouteSegment> loadDataByGPX(GPXUtilities.GPXFile gpxFile) throws IOException {
      List<NetworkRouteGpxApproximator.GpxRoutePoint> gpxRoutePoints = new ArrayList<>();
      List<NetworkRouteContext.NetworkRouteSegment> res = new ArrayList<>();
      List<NetworkRouteContext.NetworkRoutePoint> passedRoutePoints = new ArrayList<>();
      int totalDistance = 0;
      int unmatchedDistance = 0;
      int unmatchedPoints = 0;

      for(GPXUtilities.Track t : gpxFile.tracks) {
         for(GPXUtilities.TrkSegment ts : t.segments) {
            for(int i = 0; i < ts.points.size() - 1; ++i) {
               GPXUtilities.WptPt ps = ts.points.get(i);
               NetworkRouteContext.NetworkRoutePoint nearesetPoint = this.selector
                  .getNetworkRouteContext()
                  .getClosestNetworkRoutePoint(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat));
               NetworkRouteGpxApproximator.GpxRoutePoint gpxRoutePoint = new NetworkRouteGpxApproximator.GpxRoutePoint(ps.lat, ps.lon);
               if (MapUtils.squareRootDist31(MapUtils.get31TileNumberX(ps.lon), MapUtils.get31TileNumberY(ps.lat), nearesetPoint.x31, nearesetPoint.y31) < 5.0
                  )
                {
                  gpxRoutePoint.routePoint = nearesetPoint;
               }

               if (!passedRoutePoints.contains(nearesetPoint)) {
                  passedRoutePoints.add(nearesetPoint);
                  gpxRoutePoints.add(gpxRoutePoint);
               }
            }
         }
      }

      for(int idx = 0; idx < gpxRoutePoints.size() - 1; ++idx) {
         NetworkRouteGpxApproximator.GpxRoutePoint start = gpxRoutePoints.get(idx);
         NetworkRouteGpxApproximator.GpxRoutePoint nextPoint = gpxRoutePoints.get(idx + 1);
         totalDistance = (int)((double)totalDistance + this.getDistance(start, nextPoint));
         NetworkRouteContext.NetworkRouteSegment matchingGpxSegment = this.getMatchingGpxSegments(start, nextPoint);
         if (matchingGpxSegment != null) {
            res.add(matchingGpxSegment);
         } else {
            int[] idxa = new int[]{idx};
            matchingGpxSegment = this.getGpxSegmentWithoutExtraGpxPoints(gpxRoutePoints, idxa, start);
            idx = idxa[0];
            if (matchingGpxSegment != null) {
               res.add(matchingGpxSegment);
            } else {
               nextPoint = gpxRoutePoints.get(idx + 1);
               res.add(this.createStraightSegment(start, nextPoint));
               unmatchedDistance = (int)((double)unmatchedDistance + this.getDistance(start, nextPoint));
               ++unmatchedPoints;
            }
         }
      }

      int matchingGpxSegmentSize = res.size();
      System.out
         .printf(
            ">> GPX approximation (%d route points matched, %d points unmatched) for %d m: %d m unmatched\n",
            matchingGpxSegmentSize,
            unmatchedPoints,
            totalDistance,
            unmatchedDistance
         );
      return res;
   }

   private NetworkRouteContext.NetworkRouteSegment createStraightSegment(
      NetworkRouteGpxApproximator.GpxRoutePoint startPoint, NetworkRouteGpxApproximator.GpxRoutePoint nextPoint
   ) {
      BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
      reg.initRouteEncodingRule(0, "highway", "unmatched");
      RouteDataObject rdo = new RouteDataObject(reg);
      rdo.pointsX = new int[]{MapUtils.get31TileNumberX(startPoint.lon), MapUtils.get31TileNumberX(nextPoint.lon)};
      rdo.pointsY = new int[]{MapUtils.get31TileNumberY(startPoint.lat), MapUtils.get31TileNumberY(nextPoint.lat)};
      rdo.types = new int[]{0};
      rdo.id = -1L;
      return new NetworkRouteContext.NetworkRouteSegment(rdo, null, 0, 1);
   }

   private NetworkRouteContext.NetworkRouteSegment getGpxSegmentWithoutExtraGpxPoints(
      List<NetworkRouteGpxApproximator.GpxRoutePoint> gpxRoutePoints, int[] idxa, NetworkRouteGpxApproximator.GpxRoutePoint start
   ) {
      NetworkRouteContext.NetworkRouteSegment matchingGpxSegment = null;
      int idx = idxa[0];
      int maxSkipGpxPoints = Math.min(gpxRoutePoints.size() - idx, 5);

      for(int j = 2; j < maxSkipGpxPoints; ++j) {
         NetworkRouteGpxApproximator.GpxRoutePoint nextPoint = gpxRoutePoints.get(idx + j);
         matchingGpxSegment = this.getMatchingGpxSegments(start, nextPoint);
         if (matchingGpxSegment != null) {
            boolean notFarAway = true;

            for(int t = 1; t < j; ++t) {
               NetworkRouteGpxApproximator.GpxRoutePoint gpxRoutePoint = gpxRoutePoints.get(idx + t);
               if (gpxRoutePoint.routePoint != null && this.getOrthogonalDistance(gpxRoutePoint, matchingGpxSegment) > 10.0) {
                  notFarAway = false;
                  break;
               }
            }

            if (notFarAway) {
               idxa[0] = idx + j - 1;
               break;
            }
         }
      }

      return matchingGpxSegment;
   }

   private double getOrthogonalDistance(NetworkRouteGpxApproximator.GpxRoutePoint gpxRoutePoint, NetworkRouteContext.NetworkRouteSegment matchingGpxSegment) {
      double minDistance = Double.MAX_VALUE;
      int px31 = gpxRoutePoint.routePoint.x31;
      int py31 = gpxRoutePoint.routePoint.y31;
      int step = matchingGpxSegment.start < matchingGpxSegment.end ? 1 : -1;

      for(int i = matchingGpxSegment.start; i < matchingGpxSegment.end; i += step) {
         int x1 = matchingGpxSegment.robj.pointsX[i];
         int y1 = matchingGpxSegment.robj.pointsY[i];
         int x2 = matchingGpxSegment.robj.pointsX[i + step];
         int y2 = matchingGpxSegment.robj.pointsY[i + step];
         QuadPoint pp = MapUtils.getProjectionPoint31(px31, py31, x1, y1, x2, y2);
         double distance = MapUtils.squareRootDist31(px31, py31, (int)pp.x, (int)pp.y);
         minDistance = Math.min(minDistance, distance);
      }

      return minDistance;
   }

   public static class GpxRoutePoint {
      int idx;
      double lat;
      double lon;
      NetworkRouteContext.NetworkRoutePoint routePoint = null;

      public GpxRoutePoint(double lat, double lon) {
         this.lat = lat;
         this.lon = lon;
      }

      public List<NetworkRouteContext.NetworkRouteSegment> getObjects() {
         return this.routePoint == null ? Collections.<NetworkRouteContext.NetworkRouteSegment>emptyList() : this.routePoint.objects;
      }
   }
}
