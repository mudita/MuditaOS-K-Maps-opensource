package net.osmand.router;

import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

public class RoutePlannerFrontEnd {
   protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
   protected static final double GPS_POSSIBLE_ERROR = 7.0;
   public boolean useSmartRouteRecalculation = true;
   public boolean useNativeApproximation = true;
   private static final boolean TRACE_ROUTING = false;

   public RoutingContext buildRoutingContext(
      RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RoutePlannerFrontEnd.RouteCalculationMode rm
   ) {
      return new RoutingContext(config, nativeLibrary, map, rm);
   }

   public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
      return new RoutingContext(config, nativeLibrary, map, RoutePlannerFrontEnd.RouteCalculationMode.NORMAL);
   }

   private static double squareDist(int x1, int y1, int x2, int y2) {
      double dy = MapUtils.convert31YToMeters(y1, y2, x1);
      double dx = MapUtils.convert31XToMeters(x1, x2, y1);
      return dx * dx + dy * dy;
   }

   public BinaryRoutePlanner.RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<BinaryRoutePlanner.RouteSegmentPoint> list) throws IOException {
      return this.findRouteSegment(lat, lon, ctx, list, false);
   }

   public BinaryRoutePlanner.RouteSegmentPoint findRouteSegment(
      double lat, double lon, RoutingContext ctx, List<BinaryRoutePlanner.RouteSegmentPoint> list, boolean transportStop
   ) throws IOException {
      return this.findRouteSegment(lat, lon, ctx, list, false, false);
   }

   public BinaryRoutePlanner.RouteSegmentPoint findRouteSegment(
      double lat, double lon, RoutingContext ctx, List<BinaryRoutePlanner.RouteSegmentPoint> list, boolean transportStop, boolean allowDuplications
   ) throws IOException {
      long now = System.nanoTime();
      int px = MapUtils.get31TileNumberX(lon);
      int py = MapUtils.get31TileNumberY(lat);
      ArrayList<RouteDataObject> dataObjects = new ArrayList<>();
      ctx.loadTileData(px, py, 17, dataObjects, allowDuplications);
      if (dataObjects.isEmpty()) {
         ctx.loadTileData(px, py, 15, dataObjects, allowDuplications);
      }

      if (dataObjects.isEmpty()) {
         ctx.loadTileData(px, py, 14, dataObjects, allowDuplications);
      }

      if (list == null) {
         list = new ArrayList<>();
      }

      for(RouteDataObject r : dataObjects) {
         if (r.getPointsLength() > 1) {
            BinaryRoutePlanner.RouteSegmentPoint road = null;

            for(int j = 1; j < r.getPointsLength(); ++j) {
               QuadPoint pr = MapUtils.getProjectionPoint31(
                  px, py, r.getPoint31XTile(j - 1), r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j)
               );
               double currentsDistSquare = squareDist((int)pr.x, (int)pr.y, px, py);
               if (road == null || currentsDistSquare < road.distSquare) {
                  RouteDataObject ro = new RouteDataObject(r);
                  road = new BinaryRoutePlanner.RouteSegmentPoint(ro, j, currentsDistSquare);
                  road.preciseX = (int)pr.x;
                  road.preciseY = (int)pr.y;
               }
            }

            if (road != null) {
               if (!transportStop) {
                  float prio = ctx.getRouter().defineDestinationPriority(road.road);
                  if (prio > 0.0F) {
                     road.distSquare = (road.distSquare + 49.0) / (double)(prio * prio);
                     list.add(road);
                  }
               } else {
                  list.add(road);
               }
            }
         }
      }

      Collections.sort(list, new Comparator<BinaryRoutePlanner.RouteSegmentPoint>() {
         public int compare(BinaryRoutePlanner.RouteSegmentPoint o1, BinaryRoutePlanner.RouteSegmentPoint o2) {
            return Double.compare(o1.distSquare, o2.distSquare);
         }
      });
      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.timeToFindInitialSegments += System.nanoTime() - now;
      }

      if (list.size() <= 0) {
         return null;
      } else {
         BinaryRoutePlanner.RouteSegmentPoint ps = null;
         if (ctx.publicTransport) {
            for(BinaryRoutePlanner.RouteSegmentPoint p : list) {
               if (transportStop && p.distSquare > 49.0) {
                  break;
               }

               boolean platform = p.road.platform();
               if (transportStop && platform) {
                  ps = p;
                  break;
               }

               if (!transportStop && !platform) {
                  ps = p;
                  break;
               }
            }
         }

         if (ps == null) {
            ps = list.get(0);
         }

         ps.others = list;
         return ps;
      }
   }

   public List<RouteSegmentResult> searchRoute(RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) throws IOException, InterruptedException {
      return this.searchRoute(ctx, start, end, intermediates, null);
   }

   public void setUseFastRecalculation(boolean use) {
      this.useSmartRouteRecalculation = use;
   }

   public void setUseNativeApproximation(boolean useNativeApproximation) {
      this.useNativeApproximation = useNativeApproximation;
   }

   public RoutePlannerFrontEnd.GpxRouteApproximation searchGpxRoute(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx,
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
      ResultMatcher<RoutePlannerFrontEnd.GpxRouteApproximation> resultMatcher
   ) throws IOException, InterruptedException {
      long timeToCalculate = System.nanoTime();
      NativeLibrary nativeLib = gctx.ctx.nativeLib;
      if (nativeLib != null && this.useNativeApproximation) {
         gctx = nativeLib.runNativeSearchGpxRoute(gctx, gpxPoints);
      } else {
         gctx.ctx.keepNativeRoutingContext = true;
         if (gctx.ctx.calculationProgress == null) {
            gctx.ctx.calculationProgress = new RouteCalculationProgress();
         }

         RoutePlannerFrontEnd.GpxPoint start = null;
         RoutePlannerFrontEnd.GpxPoint prev = null;
         if (gpxPoints.size() > 0) {
            gctx.ctx.calculationProgress.totalApproximateDistance = (float)gpxPoints.get(gpxPoints.size() - 1).cumDist;
            start = gpxPoints.get(0);
         }

         while(start != null && !gctx.ctx.calculationProgress.isCancelled) {
            double routeDist = (double)gctx.ctx.config.maxStepApproximation;
            RoutePlannerFrontEnd.GpxPoint next = this.findNextGpxPointWithin(gpxPoints, start, routeDist);
            boolean routeFound = false;
            if (next != null && this.initRoutingPoint(start, gctx, (double)gctx.ctx.config.minPointApproximation)) {
               while(routeDist >= (double)gctx.ctx.config.minStepApproximation && !routeFound) {
                  routeFound = this.initRoutingPoint(next, gctx, (double)gctx.ctx.config.minPointApproximation);
                  if (routeFound) {
                     routeFound = this.findGpxRouteSegment(gctx, gpxPoints, start, next, prev != null);
                     if (routeFound) {
                        routeFound = this.isRouteCloseToGpxPoints(gctx, gpxPoints, start, next);
                        if (!routeFound) {
                           start.routeToTarget = null;
                        }
                     }

                     if (routeFound && next.ind == gpxPoints.size() - 1) {
                        this.makeSegmentPointPrecise(start.routeToTarget.get(start.routeToTarget.size() - 1), next.loc, false);
                     } else if (routeFound) {
                        boolean stepBack = this.stepBackAndFindPrevPointInRoute(gctx, gpxPoints, start, next);
                        if (!stepBack) {
                           log.info("Consider to increase routing.xml maxStepApproximation to: " + routeDist * 2.0);
                           start.routeToTarget = null;
                           routeFound = false;
                        } else if (gctx.ctx.getVisitor() != null) {
                           gctx.ctx.getVisitor().visitApproximatedSegments(start.routeToTarget, start, next);
                        }
                     }
                  }

                  if (!routeFound) {
                     routeDist /= 2.0;
                     if (routeDist < (double)gctx.ctx.config.minStepApproximation && routeDist > (double)(gctx.ctx.config.minStepApproximation / 2.0F + 1.0F)) {
                        routeDist = (double)gctx.ctx.config.minStepApproximation;
                     }

                     next = this.findNextGpxPointWithin(gpxPoints, start, routeDist);
                     if (next != null) {
                        routeDist = Math.min(next.cumDist - start.cumDist, routeDist);
                     }
                  }
               }
            }

            if (!routeFound && next != null) {
               next = this.findNextGpxPointWithin(gpxPoints, start, (double)gctx.ctx.config.minStepApproximation);
               if (prev != null) {
                  prev.routeToTarget.addAll(prev.stepBackRoute);
                  this.makeSegmentPointPrecise(prev.routeToTarget.get(prev.routeToTarget.size() - 1), start.loc, false);
                  if (next != null) {
                     log.warn("NOT found route from: " + start.pnt.getRoad() + " at " + start.pnt.getSegmentStart());
                  }
               }

               prev = null;
            } else {
               prev = start;
            }

            start = next;
            if (gctx.ctx.calculationProgress != null && next != null) {
               gctx.ctx.calculationProgress.approximatedDistance = (float)next.cumDist;
            }
         }

         if (gctx.ctx.calculationProgress != null) {
            gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
         }

         gctx.ctx.deleteNativeRoutingContext();
         this.calculateGpxRoute(gctx, gpxPoints);
         if (!gctx.result.isEmpty() && !gctx.ctx.calculationProgress.isCancelled) {
            RouteResultPreparation.printResults(gctx.ctx, gpxPoints.get(0).loc, gpxPoints.get(gpxPoints.size() - 1).loc, gctx.result);
            log.info(gctx);
         }
      }

      if (resultMatcher != null) {
         resultMatcher.publish(gctx.ctx.calculationProgress.isCancelled ? null : gctx);
      }

      return gctx;
   }

   private boolean isRouteCloseToGpxPoints(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx,
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
      RoutePlannerFrontEnd.GpxPoint start,
      RoutePlannerFrontEnd.GpxPoint next
   ) {
      boolean routeIsClose = true;

      for(RouteSegmentResult r : start.routeToTarget) {
         int st = r.getStartPointIndex();

         for(int end = r.getEndPointIndex(); st != end; st += st < end ? 1 : -1) {
            LatLon point = r.getPoint(st);
            boolean pointIsClosed = false;

            for(int k = start.ind; !pointIsClosed && k < next.ind; ++k) {
               pointIsClosed = this.pointCloseEnough(gctx, point, gpxPoints.get(k), gpxPoints.get(k + 1));
            }

            if (!pointIsClosed) {
               routeIsClose = false;
               break;
            }
         }
      }

      return routeIsClose;
   }

   private boolean stepBackAndFindPrevPointInRoute(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx,
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
      RoutePlannerFrontEnd.GpxPoint start,
      RoutePlannerFrontEnd.GpxPoint next
   ) throws IOException {
      double STEP_BACK_DIST = (double)Math.max(gctx.ctx.config.minPointApproximation, gctx.ctx.config.minStepApproximation);
      double d = 0.0;
      int segmendInd = start.routeToTarget.size() - 1;
      boolean search = true;

      label52:
      for(start.stepBackRoute = new ArrayList<>(); segmendInd >= 0 && search; --segmendInd) {
         RouteSegmentResult rr = start.routeToTarget.get(segmendInd);
         boolean minus = rr.getStartPointIndex() < rr.getEndPointIndex();

         int nextInd;
         for(int j = rr.getEndPointIndex(); j != rr.getStartPointIndex(); j = nextInd) {
            nextInd = minus ? j - 1 : j + 1;
            d += MapUtils.getDistance(rr.getPoint(j), rr.getPoint(nextInd));
            if (d > STEP_BACK_DIST) {
               if (nextInd == rr.getStartPointIndex()) {
                  --segmendInd;
               } else {
                  start.stepBackRoute.add(new RouteSegmentResult(rr.getObject(), nextInd, rr.getEndPointIndex()));
                  rr.setEndPointIndex(nextInd);
               }

               search = false;
               break label52;
            }
         }
      }

      if (segmendInd == -1) {
         return false;
      } else {
         while(start.routeToTarget.size() > segmendInd + 1) {
            RouteSegmentResult removed = start.routeToTarget.remove(segmendInd + 1);
            start.stepBackRoute.add(removed);
         }

         RouteSegmentResult res = start.routeToTarget.get(segmendInd);
         next.pnt = new BinaryRoutePlanner.RouteSegmentPoint(res.getObject(), res.getEndPointIndex(), 0.0);
         return true;
      }
   }

   private void calculateGpxRoute(RoutePlannerFrontEnd.GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints) {
      BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
      reg.initRouteEncodingRule(0, "highway", "unmatched");
      List<LatLon> lastStraightLine = null;
      RoutePlannerFrontEnd.GpxPoint straightPointStart = null;
      int i = 0;

      while(i < gpxPoints.size() && !gctx.ctx.calculationProgress.isCancelled) {
         RoutePlannerFrontEnd.GpxPoint pnt = gpxPoints.get(i);
         if (pnt.routeToTarget != null && !pnt.routeToTarget.isEmpty()) {
            LatLon startPoint = pnt.routeToTarget.get(0).getStartPoint();
            if (lastStraightLine != null) {
               lastStraightLine.add(startPoint);
               this.addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
               lastStraightLine = null;
            }

            if (gctx.distFromLastPoint(startPoint) > 1.0) {
               gctx.routeGapDistance = (int)((double)gctx.routeGapDistance + gctx.distFromLastPoint(startPoint));
               System.out
                  .println(
                     String.format(
                        "????? gap of route point = %f, gap of actual gpxPoint = %f, %s ",
                        gctx.distFromLastPoint(startPoint),
                        gctx.distFromLastPoint(pnt.loc),
                        pnt.loc
                     )
                  );
            }

            gctx.finalPoints.add(pnt);
            gctx.result.addAll(pnt.routeToTarget);
            i = pnt.targetInd;
         } else {
            if (lastStraightLine == null) {
               lastStraightLine = new ArrayList<>();
               straightPointStart = pnt;
               if (gctx.distFromLastPoint(pnt.loc) > 1.0) {
                  lastStraightLine.add(gctx.getLastPoint());
               }
            }

            lastStraightLine.add(pnt.loc);
            ++i;
         }
      }

      if (lastStraightLine != null) {
         this.addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
         lastStraightLine = null;
      }

      this.cleanupResultAndAddTurns(gctx);
   }

   public static RouteSegmentResult generateStraightLineSegment(float averageSpeed, List<LatLon> points) {
      BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
      reg.initRouteEncodingRule(0, "highway", "unmatched");
      RouteDataObject rdo = new RouteDataObject(reg);
      int size = points.size();
      TIntArrayList x = new TIntArrayList(size);
      TIntArrayList y = new TIntArrayList(size);
      double distance = 0.0;
      double distOnRoadToPass = 0.0;
      LatLon prev = null;

      for(int i = 0; i < size; ++i) {
         LatLon l = points.get(i);
         if (l != null) {
            x.add(MapUtils.get31TileNumberX(l.getLongitude()));
            y.add(MapUtils.get31TileNumberY(l.getLatitude()));
            if (prev != null) {
               double d = MapUtils.getDistance(l, prev);
               distance += d;
               distOnRoadToPass += d / (double)averageSpeed;
            }
         }

         prev = l;
      }

      rdo.pointsX = x.toArray();
      rdo.pointsY = y.toArray();
      rdo.types = new int[]{0};
      rdo.id = -1L;
      RouteSegmentResult segment = new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1);
      segment.setSegmentTime((float)distOnRoadToPass);
      segment.setSegmentSpeed(averageSpeed);
      segment.setDistance((float)distance);
      segment.setTurnType(TurnType.straight());
      return segment;
   }

   public List<RoutePlannerFrontEnd.GpxPoint> generateGpxPoints(RoutePlannerFrontEnd.GpxRouteApproximation gctx, LocationsHolder locationsHolder) {
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints = new ArrayList<>(locationsHolder.getSize());
      RoutePlannerFrontEnd.GpxPoint prev = null;

      for(int i = 0; i < locationsHolder.getSize(); ++i) {
         RoutePlannerFrontEnd.GpxPoint p = new RoutePlannerFrontEnd.GpxPoint();
         p.ind = i;
         p.loc = locationsHolder.getLatLon(i);
         if (prev != null) {
            p.cumDist = MapUtils.getDistance(p.loc, prev.loc) + prev.cumDist;
         }

         gpxPoints.add(p);
         gctx.routeDistance = (int)p.cumDist;
         prev = p;
      }

      return gpxPoints;
   }

   private void cleanupResultAndAddTurns(RoutePlannerFrontEnd.GpxRouteApproximation gctx) {
      int LOOK_AHEAD = 4;

      for(int i = 0; i < gctx.result.size() && !gctx.ctx.calculationProgress.isCancelled; ++i) {
         RouteSegmentResult s = gctx.result.get(i);

         for(int j = i + 2; j <= i + LOOK_AHEAD && j < gctx.result.size(); ++j) {
            RouteSegmentResult e = gctx.result.get(j);
            if (e.getStartPoint().equals(s.getEndPoint())) {
               while(--j != i) {
                  gctx.result.remove(j);
               }
               break;
            }
         }
      }

      RouteResultPreparation preparation = new RouteResultPreparation();

      for(RouteSegmentResult r : gctx.result) {
         r.setTurnType(null);
         r.setDescription("");
      }

      if (!gctx.ctx.calculationProgress.isCancelled) {
         preparation.prepareTurnResults(gctx.ctx, gctx.result);
      }
   }

   private void addStraightLine(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx,
      List<LatLon> lastStraightLine,
      RoutePlannerFrontEnd.GpxPoint strPnt,
      BinaryMapRouteReaderAdapter.RouteRegion reg
   ) {
      RouteDataObject rdo = new RouteDataObject(reg);
      if (gctx.ctx.config.smoothenPointsNoRoute > 0.0F) {
         this.simplifyDouglasPeucker(lastStraightLine, (double)gctx.ctx.config.smoothenPointsNoRoute, 0, lastStraightLine.size() - 1);
      }

      int s = lastStraightLine.size();
      TIntArrayList x = new TIntArrayList(s);
      TIntArrayList y = new TIntArrayList(s);

      for(int i = 0; i < s; ++i) {
         if (lastStraightLine.get(i) != null) {
            LatLon l = lastStraightLine.get(i);
            int t = x.size() - 1;
            x.add(MapUtils.get31TileNumberX(l.getLongitude()));
            y.add(MapUtils.get31TileNumberY(l.getLatitude()));
            if (t >= 0) {
               double dist = MapUtils.squareRootDist31(x.get(t), y.get(t), x.get(t + 1), y.get(t + 1));
               gctx.routeDistanceUnmatched = (int)((double)gctx.routeDistanceUnmatched + dist);
            }
         }
      }

      rdo.pointsX = x.toArray();
      rdo.pointsY = y.toArray();
      rdo.types = new int[]{0};
      rdo.id = -1L;
      strPnt.routeToTarget = new ArrayList<>();
      strPnt.straightLine = true;
      strPnt.routeToTarget.add(new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1));
      RouteResultPreparation preparation = new RouteResultPreparation();

      try {
         preparation.prepareResult(gctx.ctx, strPnt.routeToTarget, false);
      } catch (IOException var14) {
         throw new IllegalStateException(var14);
      }

      gctx.finalPoints.add(strPnt);
      gctx.result.addAll(strPnt.routeToTarget);
   }

   private void simplifyDouglasPeucker(List<LatLon> l, double eps, int start, int end) {
      double dmax = -1.0;
      int index = -1;
      LatLon s = l.get(start);
      LatLon e = l.get(end);

      for(int i = start + 1; i <= end - 1; ++i) {
         LatLon ip = l.get(i);
         double dist = MapUtils.getOrthogonalDistance(
            ip.getLatitude(), ip.getLongitude(), s.getLatitude(), s.getLongitude(), e.getLatitude(), e.getLongitude()
         );
         if (dist > dmax) {
            dmax = dist;
            index = i;
         }
      }

      if (dmax >= eps) {
         this.simplifyDouglasPeucker(l, eps, start, index);
         this.simplifyDouglasPeucker(l, eps, index, end);
      } else {
         for(int i = start + 1; i < end; ++i) {
            l.set(i, null);
         }
      }
   }

   private boolean initRoutingPoint(RoutePlannerFrontEnd.GpxPoint start, RoutePlannerFrontEnd.GpxRouteApproximation gctx, double distThreshold) throws IOException {
      if (start != null && start.pnt == null) {
         ++gctx.routePointsSearched;
         BinaryRoutePlanner.RouteSegmentPoint rsp = this.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(), gctx.ctx, null, false);
         if (rsp != null && MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
            start.pnt = rsp;
         }
      }

      return start != null && start.pnt != null;
   }

   private RoutePlannerFrontEnd.GpxPoint findNextGpxPointWithin(
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints, RoutePlannerFrontEnd.GpxPoint start, double dist
   ) {
      int plus = dist > 0.0 ? 1 : -1;
      int targetInd = start.ind + plus;

      RoutePlannerFrontEnd.GpxPoint target;
      for(target = null; targetInd < gpxPoints.size() && targetInd >= 0; targetInd += plus) {
         target = gpxPoints.get(targetInd);
         if (Math.abs(target.cumDist - start.cumDist) > Math.abs(dist)) {
            break;
         }
      }

      return target;
   }

   private boolean findGpxRouteSegment(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx,
      List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
      RoutePlannerFrontEnd.GpxPoint start,
      RoutePlannerFrontEnd.GpxPoint target,
      boolean prevRouteCalculated
   ) throws IOException, InterruptedException {
      List<RouteSegmentResult> res = null;
      boolean routeIsCorrect = false;
      if (start.pnt != null && target.pnt != null) {
         start.pnt = new BinaryRoutePlanner.RouteSegmentPoint(start.pnt);
         target.pnt = new BinaryRoutePlanner.RouteSegmentPoint(target.pnt);
         gctx.routeDistCalculations = (int)((double)gctx.routeDistCalculations + (target.cumDist - start.cumDist));
         ++gctx.routeCalculations;
         RoutingContext local = new RoutingContext(gctx.ctx);
         res = this.searchRouteInternalPrepare(local, start.pnt, target.pnt, null, false);
         routeIsCorrect = res != null && !res.isEmpty();

         for(int k = start.ind + 1; routeIsCorrect && k < target.ind; ++k) {
            RoutePlannerFrontEnd.GpxPoint ipoint = gpxPoints.get(k);
            if (!this.pointCloseEnough(gctx, ipoint, res)) {
               routeIsCorrect = false;
            }
         }

         if (routeIsCorrect) {
            if (!prevRouteCalculated) {
               this.makeSegmentPointPrecise(res.get(0), start.loc, true);
            } else if (res.get(0).getObject().getId() == start.pnt.getRoad().getId()) {
               res.get(0).setStartPointIndex(start.pnt.getSegmentStart());
            } else {
               System.out.println("??? not found " + start.pnt.getRoad().getId() + " instead " + res.get(0).getObject().getId());
            }

            start.routeToTarget = res;
            start.targetInd = target.ind;
         }

         if (gctx.ctx.getVisitor() != null) {
            gctx.ctx.getVisitor().visitApproximatedSegments(res, start, target);
         }
      }

      return routeIsCorrect;
   }

   private boolean pointCloseEnough(
      RoutePlannerFrontEnd.GpxRouteApproximation gctx, LatLon point, RoutePlannerFrontEnd.GpxPoint gpxPoint, RoutePlannerFrontEnd.GpxPoint gpxPointNext
   ) {
      LatLon gpxPointLL = gpxPoint.pnt != null ? gpxPoint.pnt.getPreciseLatLon() : gpxPoint.loc;
      LatLon gpxPointNextLL = gpxPointNext.pnt != null ? gpxPointNext.pnt.getPreciseLatLon() : gpxPointNext.loc;
      LatLon projection = MapUtils.getProjection(
         point.getLatitude(),
         point.getLongitude(),
         gpxPointLL.getLatitude(),
         gpxPointLL.getLongitude(),
         gpxPointNextLL.getLatitude(),
         gpxPointNextLL.getLongitude()
      );
      return MapUtils.getDistance(projection, point) <= (double)gctx.ctx.config.minPointApproximation;
   }

   private boolean pointCloseEnough(RoutePlannerFrontEnd.GpxRouteApproximation gctx, RoutePlannerFrontEnd.GpxPoint ipoint, List<RouteSegmentResult> res) {
      int px = MapUtils.get31TileNumberX(ipoint.loc.getLongitude());
      int py = MapUtils.get31TileNumberY(ipoint.loc.getLatitude());
      double SQR = (double)gctx.ctx.config.minPointApproximation;
      SQR *= SQR;

      for(RouteSegmentResult sr : res) {
         int start = sr.getStartPointIndex();
         int end = sr.getEndPointIndex();
         if (sr.getStartPointIndex() > sr.getEndPointIndex()) {
            start = sr.getEndPointIndex();
            end = sr.getStartPointIndex();
         }

         for(int i = start; i < end; ++i) {
            RouteDataObject r = sr.getObject();
            QuadPoint pp = MapUtils.getProjectionPoint31(
               px, py, r.getPoint31XTile(i), r.getPoint31YTile(i), r.getPoint31XTile(i + 1), r.getPoint31YTile(i + 1)
            );
            double currentsDist = squareDist((int)pp.x, (int)pp.y, px, py);
            if (currentsDist <= SQR) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean needRequestPrivateAccessRouting(RoutingContext ctx, List<LatLon> points) throws IOException {
      boolean res = false;
      if (ctx.nativeLib != null) {
         int size = points.size();
         int[] y31Coordinates = new int[size];
         int[] x31Coordinates = new int[size];

         for(int i = 0; i < size; ++i) {
            y31Coordinates[i] = MapUtils.get31TileNumberY(points.get(i).getLatitude());
            x31Coordinates[i] = MapUtils.get31TileNumberX(points.get(i).getLongitude());
         }

         res = ctx.nativeLib.needRequestPrivateAccessRouting(ctx, x31Coordinates, y31Coordinates);
      } else {
         GeneralRouter router = (GeneralRouter)ctx.getRouter();
         if (router == null) {
            return false;
         }

         Map<String, GeneralRouter.RoutingParameter> parameters = router.getParameters();
         String allowPrivateKey = null;
         if (parameters.containsKey("allow_private")) {
            allowPrivateKey = "allow_private";
         } else if (parameters.containsKey("allow_private_for_truck")) {
            allowPrivateKey = "allow_private";
         }

         if (!router.isAllowPrivate() && allowPrivateKey != null) {
            ctx.unloadAllData();
            LinkedHashMap<String, String> mp = new LinkedHashMap<>();
            mp.put(allowPrivateKey, "true");
            mp.put("check_allow_private_needed", "true");
            ctx.setRouter(new GeneralRouter(router.getProfile(), mp));

            for(LatLon latLon : points) {
               BinaryRoutePlanner.RouteSegmentPoint rp = this.findRouteSegment(latLon.getLatitude(), latLon.getLongitude(), ctx, null);
               if (rp != null && rp.road != null && rp.road.hasPrivateAccess()) {
                  res = true;
                  break;
               }
            }

            ctx.unloadAllData();
            ctx.setRouter(router);
         }
      }

      return res;
   }

   public List<RouteSegmentResult> searchRoute(
      RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates, PrecalculatedRouteDirection routeDirection
   ) throws IOException, InterruptedException {
      long timeToCalculate = System.nanoTime();
      if (ctx.calculationProgress == null) {
         ctx.calculationProgress = new RouteCalculationProgress();
      }

      boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
      List<LatLon> targets = new ArrayList<>();
      targets.add(end);
      if (!intermediatesEmpty) {
         targets.addAll(intermediates);
      }

      if (this.needRequestPrivateAccessRouting(ctx, targets)) {
         ctx.calculationProgress.requestPrivateAccessRouting = true;
      }

      double maxDistance = MapUtils.getDistance(start, end);
      if (!intermediatesEmpty) {
         LatLon b = start;

         for(LatLon l : intermediates) {
            maxDistance = Math.max(MapUtils.getDistance(b, l), maxDistance);
            b = l;
         }
      }

      if (ctx.calculationMode == RoutePlannerFrontEnd.RouteCalculationMode.COMPLEX && routeDirection == null && maxDistance > 18000.0) {
         ++ctx.calculationProgress.totalIterations;
         RoutingContext nctx = this.buildRoutingContext(ctx.config, ctx.nativeLib, ctx.getMaps(), RoutePlannerFrontEnd.RouteCalculationMode.BASE);
         nctx.calculationProgress = ctx.calculationProgress;
         List<RouteSegmentResult> ls = this.searchRoute(nctx, start, end, intermediates);
         if (ls == null) {
            return null;
         }

         routeDirection = PrecalculatedRouteDirection.build(ls, 3000.0F, ctx.getRouter().getMaxSpeed());
         ctx.calculationProgressFirstPhase = RouteCalculationProgress.capture(ctx.calculationProgress);
      }

      List<RouteSegmentResult> res;
      if (intermediatesEmpty && ctx.nativeLib != null) {
         ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
         ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
         ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
         ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
         BinaryRoutePlanner.RouteSegmentPoint recalculationEnd = this.getRecalculationEnd(ctx);
         if (recalculationEnd != null) {
            ctx.initTargetPoint(recalculationEnd);
         }

         if (routeDirection != null) {
            ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
         }

         ctx.calculationProgress.nextIteration();
         res = this.runNativeRouting(ctx, recalculationEnd);
         this.makeStartEndPointsPrecise(res, start, end, intermediates);
      } else {
         int indexNotFound = 0;
         List<BinaryRoutePlanner.RouteSegmentPoint> points = new ArrayList<>();
         if (!this.addSegment(start, ctx, indexNotFound++, points, ctx.startTransportStop)) {
            return null;
         }

         if (intermediates != null) {
            for(LatLon l : intermediates) {
               if (!this.addSegment(l, ctx, indexNotFound++, points, false)) {
                  System.out.println(points.get(points.size() - 1).getRoad().toString());
                  return null;
               }
            }
         }

         if (!this.addSegment(end, ctx, indexNotFound++, points, ctx.targetTransportStop)) {
            return null;
         }

         ctx.calculationProgress.nextIteration();
         res = this.searchRouteImpl(ctx, points, routeDirection);
      }

      ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
      RouteResultPreparation.printResults(ctx, start, end, res);
      return res;
   }

   protected void makeStartEndPointsPrecise(List<RouteSegmentResult> res, LatLon start, LatLon end, List<LatLon> intermediates) {
      if (res.size() > 0) {
         this.makeSegmentPointPrecise(res.get(0), start, true);
         this.makeSegmentPointPrecise(res.get(res.size() - 1), end, false);
      }
   }

   protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
      RouteSegmentResult sr = res.get(k);
      RouteDataObject r = sr.getObject();
      QuadPoint pp = MapUtils.getProjectionPoint31(
         px,
         py,
         r.getPoint31XTile(sr.getStartPointIndex()),
         r.getPoint31YTile(sr.getStartPointIndex()),
         r.getPoint31XTile(sr.getEndPointIndex()),
         r.getPoint31YTile(sr.getEndPointIndex())
      );
      return squareDist((int)pp.x, (int)pp.y, px, py);
   }

   private void makeSegmentPointPrecise(RouteSegmentResult routeSegmentResult, LatLon point, boolean st) {
      int px = MapUtils.get31TileNumberX(point.getLongitude());
      int py = MapUtils.get31TileNumberY(point.getLatitude());
      int pind = st ? routeSegmentResult.getStartPointIndex() : routeSegmentResult.getEndPointIndex();
      RouteDataObject r = new RouteDataObject(routeSegmentResult.getObject());
      routeSegmentResult.setObject(r);
      QuadPoint before = null;
      QuadPoint after = null;
      if (pind > 0) {
         before = MapUtils.getProjectionPoint31(
            px, py, r.getPoint31XTile(pind - 1), r.getPoint31YTile(pind - 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind)
         );
      }

      if (pind < r.getPointsLength() - 1) {
         after = MapUtils.getProjectionPoint31(
            px, py, r.getPoint31XTile(pind + 1), r.getPoint31YTile(pind + 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind)
         );
      }

      int insert = 0;
      double dd = MapUtils.getDistance(point, MapUtils.get31LatitudeY(r.getPoint31YTile(pind)), MapUtils.get31LongitudeX(r.getPoint31XTile(pind)));
      double ddBefore = Double.POSITIVE_INFINITY;
      double ddAfter = Double.POSITIVE_INFINITY;
      QuadPoint i = null;
      if (before != null) {
         ddBefore = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int)before.y), MapUtils.get31LongitudeX((int)before.x));
         if (ddBefore < dd) {
            insert = -1;
            i = before;
         }
      }

      if (after != null) {
         ddAfter = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int)after.y), MapUtils.get31LongitudeX((int)after.x));
         if (ddAfter < dd && ddAfter < ddBefore) {
            insert = 1;
            i = after;
         }
      }

      if (insert != 0) {
         if (st && routeSegmentResult.getStartPointIndex() < routeSegmentResult.getEndPointIndex()) {
            routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
         }

         if (!st && routeSegmentResult.getStartPointIndex() > routeSegmentResult.getEndPointIndex()) {
            routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
         }

         if (insert > 0) {
            r.insert(pind + 1, (int)i.x, (int)i.y);
            if (st) {
               routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
            }

            if (!st) {
               routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
            }
         } else {
            r.insert(pind, (int)i.x, (int)i.y);
         }
      }
   }

   private boolean addSegment(LatLon s, RoutingContext ctx, int indexNotFound, List<BinaryRoutePlanner.RouteSegmentPoint> res, boolean transportStop) throws IOException {
      BinaryRoutePlanner.RouteSegmentPoint f = this.findRouteSegment(s.getLatitude(), s.getLongitude(), ctx, null, transportStop);
      if (f == null) {
         ctx.calculationProgress.segmentNotFound = indexNotFound;
         return false;
      } else {
         log.info("Route segment found " + f.road);
         res.add(f);
         return true;
      }
   }

   private List<RouteSegmentResult> searchRouteInternalPrepare(
      RoutingContext ctx,
      BinaryRoutePlanner.RouteSegmentPoint start,
      BinaryRoutePlanner.RouteSegmentPoint end,
      PrecalculatedRouteDirection routeDirection,
      boolean isIntermediatePoint
   ) throws IOException, InterruptedException {
      BinaryRoutePlanner.RouteSegmentPoint recalculationEnd = this.getRecalculationEnd(ctx);
      if (recalculationEnd != null) {
         ctx.initStartAndTargetPoints(start, recalculationEnd);
      } else {
         ctx.initStartAndTargetPoints(start, end);
      }

      if (routeDirection != null) {
         ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
      }

      if (ctx.nativeLib != null) {
         ctx.startX = start.preciseX;
         ctx.startY = start.preciseY;
         ctx.startRoadId = start.road.id;
         ctx.startSegmentInd = start.segStart;
         ctx.targetX = end.preciseX;
         ctx.targetY = end.preciseY;
         ctx.targetRoadId = end.road.id;
         ctx.targetSegmentInd = end.segStart;
         return this.runNativeRouting(ctx, recalculationEnd);
      } else {
         this.refreshProgressDistance(ctx);
         ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(ctx, start, end, recalculationEnd);
         return new RouteResultPreparation().prepareResult(ctx, ctx.finalRouteSegment, isIntermediatePoint);
      }
   }

   public BinaryRoutePlanner.RouteSegmentPoint getRecalculationEnd(RoutingContext ctx) {
      BinaryRoutePlanner.RouteSegmentPoint recalculationEnd = null;
      boolean runRecalculation = ctx.previouslyCalculatedRoute != null && !ctx.previouslyCalculatedRoute.isEmpty() && ctx.config.recalculateDistance != 0.0F;
      if (runRecalculation) {
         List<RouteSegmentResult> rlist = new ArrayList<>();
         float distanceThreshold = ctx.config.recalculateDistance;
         float threshold = 0.0F;

         for(RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
            threshold += rr.getDistance();
            if (threshold > distanceThreshold) {
               rlist.add(rr);
            }
         }

         if (!rlist.isEmpty()) {
            BinaryRoutePlanner.RouteSegment previous = null;

            for(int i = 0; i < rlist.size(); ++i) {
               RouteSegmentResult rr = rlist.get(i);
               if (previous != null) {
                  BinaryRoutePlanner.RouteSegment segment = new BinaryRoutePlanner.RouteSegment(rr.getObject(), rr.getStartPointIndex(), rr.getEndPointIndex());
                  previous.setParentRoute(segment);
                  previous = segment;
               } else {
                  recalculationEnd = new BinaryRoutePlanner.RouteSegmentPoint(rr.getObject(), rr.getStartPointIndex(), 0.0);
                  if (Math.abs(rr.getEndPointIndex() - rr.getStartPointIndex()) > 1) {
                     BinaryRoutePlanner.RouteSegment segment = new BinaryRoutePlanner.RouteSegment(
                        rr.getObject(), recalculationEnd.segEnd, rr.getEndPointIndex()
                     );
                     recalculationEnd.setParentRoute(segment);
                     previous = segment;
                  } else {
                     previous = recalculationEnd;
                  }
               }
            }
         }
      }

      return recalculationEnd;
   }

   private void refreshProgressDistance(RoutingContext ctx) {
      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.distanceFromBegin = 0.0F;
         ctx.calculationProgress.distanceFromEnd = 0.0F;
         ctx.calculationProgress.reverseSegmentQueueSize = 0;
         ctx.calculationProgress.directSegmentQueueSize = 0;
         float rd = (float)MapUtils.squareRootDist31(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY);
         float speed = 0.9F * ctx.config.router.getMaxSpeed();
         ctx.calculationProgress.totalEstimatedDistance = rd / speed;
      }
   }

   private List<RouteSegmentResult> runNativeRouting(RoutingContext ctx, BinaryRoutePlanner.RouteSegment recalculationEnd) throws IOException {
      this.refreshProgressDistance(ctx);
      if (recalculationEnd != null) {
      }

      BinaryMapRouteReaderAdapter.RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new BinaryMapRouteReaderAdapter.RouteRegion[0]);
      RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx, regions, ctx.calculationMode == RoutePlannerFrontEnd.RouteCalculationMode.BASE);
      List<RouteSegmentResult> result = new ArrayList<>(Arrays.asList(res));
      if (recalculationEnd != null) {
         log.info("Native routing use precalculated route");
         BinaryRoutePlanner.RouteSegment current = recalculationEnd;
         if (!this.hasSegment(result, recalculationEnd)) {
            result.add(new RouteSegmentResult(recalculationEnd.getRoad(), recalculationEnd.getSegmentStart(), recalculationEnd.getSegmentEnd()));
         }

         while(current.getParentRoute() != null) {
            BinaryRoutePlanner.RouteSegment pr = current.getParentRoute();
            result.add(new RouteSegmentResult(pr.getRoad(), pr.getSegmentStart(), pr.getSegmentEnd()));
            current = pr;
         }
      }

      ctx.routingTime += ctx.calculationProgress.routingCalculatedTime;
      return new RouteResultPreparation().prepareResult(ctx, result, recalculationEnd != null);
   }

   private boolean hasSegment(List<RouteSegmentResult> result, BinaryRoutePlanner.RouteSegment current) {
      for(RouteSegmentResult r : result) {
         long currentId = r.getObject().id;
         if (currentId == current.getRoad().id && r.getStartPointIndex() == current.getSegmentStart() && r.getEndPointIndex() == current.getSegmentEnd()) {
            return true;
         }
      }

      return false;
   }

   private List<RouteSegmentResult> searchRouteImpl(
      RoutingContext ctx, List<BinaryRoutePlanner.RouteSegmentPoint> points, PrecalculatedRouteDirection routeDirection
   ) throws IOException, InterruptedException {
      if (points.size() <= 2) {
         if (!this.useSmartRouteRecalculation) {
            ctx.previouslyCalculatedRoute = null;
         }

         this.pringGC(ctx, true);
         List<RouteSegmentResult> res = this.searchRouteInternalPrepare(ctx, points.get(0), points.get(1), routeDirection, false);
         this.pringGC(ctx, false);
         this.makeStartEndPointsPrecise(res, points.get(0).getPreciseLatLon(), points.get(1).getPreciseLatLon(), null);
         return res;
      } else {
         ArrayList<RouteSegmentResult> firstPartRecalculatedRoute = null;
         ArrayList<RouteSegmentResult> restPartRecalculatedRoute = null;
         if (ctx.previouslyCalculatedRoute != null) {
            List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
            long id = points.get(1).getRoad().id;
            int ss = points.get(1).getSegmentStart();
            int px = points.get(1).getRoad().getPoint31XTile(ss);
            int py = points.get(1).getRoad().getPoint31YTile(ss);

            for(int i = 0; i < prev.size(); ++i) {
               RouteSegmentResult rsr = prev.get(i);
               if (id == rsr.getObject().getId()
                  && MapUtils.getDistance(rsr.getPoint(rsr.getEndPointIndex()), MapUtils.get31LatitudeY(py), MapUtils.get31LongitudeX(px)) < 50.0) {
                  firstPartRecalculatedRoute = new ArrayList<>(i + 1);
                  restPartRecalculatedRoute = new ArrayList<>(prev.size() - i);

                  for(int k = 0; k < prev.size(); ++k) {
                     if (k <= i) {
                        firstPartRecalculatedRoute.add(prev.get(k));
                     } else {
                        restPartRecalculatedRoute.add(prev.get(k));
                     }
                  }

                  System.out.println("Recalculate only first part of the route");
                  break;
               }
            }
         }

         List<RouteSegmentResult> results = new ArrayList<>();

         for(int i = 0; i < points.size() - 1; ++i) {
            RoutingContext local = new RoutingContext(ctx);
            if (i == 0 && ctx.nativeLib == null && this.useSmartRouteRecalculation) {
               local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
            }

            List<RouteSegmentResult> res = this.searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), routeDirection, i > 0);
            this.makeStartEndPointsPrecise(res, points.get(i).getPreciseLatLon(), points.get(i + 1).getPreciseLatLon(), null);
            results.addAll(res);
            ctx.routingTime += local.routingTime;
            if (restPartRecalculatedRoute != null) {
               results.addAll(restPartRecalculatedRoute);
               break;
            }
         }

         ctx.unloadAllData();
         return results;
      }
   }

   private void pringGC(RoutingContext ctx, boolean before) {
      if (RoutingContext.SHOW_GC_SIZE && before) {
         long h1 = RoutingContext.runGCUsedMemory();
         float mb = 1048576.0F;
         log.warn("Used before routing " + (float)h1 / mb + " actual");
      } else if (RoutingContext.SHOW_GC_SIZE && !before) {
         int sz = ctx.global.size;
         log.warn("Subregion size " + ctx.subregionTiles.size() + "  tiles " + ctx.indexedSubregions.size());
         RoutingContext.runGCUsedMemory();
         long h1 = RoutingContext.runGCUsedMemory();
         ctx.unloadAllData();
         RoutingContext.runGCUsedMemory();
         long h2 = RoutingContext.runGCUsedMemory();
         float mb = 1048576.0F;
         log.warn("Unload context :  estimated " + (float)sz / mb + " ?= " + (float)(h1 - h2) / mb + " actual");
      }
   }

   public static class GpxPoint {
      public int ind;
      public LatLon loc;
      public double cumDist;
      public BinaryRoutePlanner.RouteSegmentPoint pnt;
      public List<RouteSegmentResult> routeToTarget;
      public List<RouteSegmentResult> stepBackRoute;
      public int targetInd = -1;
      public boolean straightLine = false;

      public GpxPoint() {
      }

      public GpxPoint(RoutePlannerFrontEnd.GpxPoint point) {
         this.ind = point.ind;
         this.loc = point.loc;
         this.cumDist = point.cumDist;
      }
   }

   public static class GpxRouteApproximation {
      public double MINIMUM_POINT_APPROXIMATION = 200.0;
      public double MAXIMUM_STEP_APPROXIMATION = 3000.0;
      public double MINIMUM_STEP_APPROXIMATION = 100.0;
      public double SMOOTHEN_POINTS_NO_ROUTE = 5.0;
      public final RoutingContext ctx;
      public int routeCalculations = 0;
      public int routePointsSearched = 0;
      public int routeDistCalculations = 0;
      public List<RoutePlannerFrontEnd.GpxPoint> finalPoints = new ArrayList<>();
      public List<RouteSegmentResult> result = new ArrayList<>();
      public int routeDistance;
      public int routeGapDistance;
      public int routeDistanceUnmatched;

      public GpxRouteApproximation(RoutingContext ctx) {
         this.ctx = ctx;
      }

      public GpxRouteApproximation(RoutePlannerFrontEnd.GpxRouteApproximation gctx) {
         this.ctx = gctx.ctx;
         this.routeDistance = gctx.routeDistance;
      }

      @Override
      public String toString() {
         return String.format(
            ">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m umatched",
            this.routeCalculations,
            this.routeDistCalculations,
            this.routePointsSearched,
            this.routeDistance,
            this.routeDistanceUnmatched
         );
      }

      public double distFromLastPoint(LatLon startPoint) {
         return this.result.size() > 0 ? MapUtils.getDistance(this.getLastPoint(), startPoint) : 0.0;
      }

      public LatLon getLastPoint() {
         return this.result.size() > 0 ? this.result.get(this.result.size() - 1).getEndPoint() : null;
      }
   }

   public static enum RouteCalculationMode {
      BASE,
      NORMAL,
      COMPLEX;
   }
}
