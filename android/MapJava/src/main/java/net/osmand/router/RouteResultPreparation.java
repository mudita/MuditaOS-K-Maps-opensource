package net.osmand.router;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

public class RouteResultPreparation {
   public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
   public static String PRINT_TO_GPX_FILE = null;
   private static final float TURN_DEGREE_MIN = 45.0F;
   private static final float UNMATCHED_TURN_DEGREE_MINIMUM = 45.0F;
   private static final float SPLIT_TURN_DEGREE_NOT_STRAIGHT = 100.0F;
   public static final int SHIFT_ID = 6;
   protected static final Log LOG = PlatformUtil.getLog(RouteResultPreparation.class);
   public static final String UNMATCHED_HIGHWAY_TYPE = "unmatched";
   private static final double SLOW_DOWN_SPEED_THRESHOLD = 15.0;
   private static final double SLOW_DOWN_SPEED = 2.0;
   private static final int MAX_SPEAK_PRIORITY = 5;

   List<RouteSegmentResult> prepareResult(
           RoutingContext ctx,
           BinaryRoutePlanner.FinalRouteSegment finalSegment,
           boolean isIntermediatePoint
   ) throws IOException {
      List<RouteSegmentResult> result = this.convertFinalSegmentToResults(ctx, finalSegment, isIntermediatePoint);
      this.prepareResult(ctx, result, false);
      return result;
   }

   private void combineWayPointsForAreaRouting(RoutingContext ctx, List<RouteSegmentResult> result) {
      for(int i = 0; i < result.size(); ++i) {
         RouteSegmentResult rsr = result.get(i);
         RouteDataObject obj = rsr.getObject();
         boolean area = false;
         if (obj.getPoint31XTile(0) == obj.getPoint31XTile(obj.getPointsLength() - 1)
            && obj.getPoint31YTile(0) == obj.getPoint31YTile(obj.getPointsLength() - 1)) {
            area = true;
         }

         if (area && ctx.getRouter().isArea(obj)) {
            List<RouteResultPreparation.CombineAreaRoutePoint> originalWay = new ArrayList<>();
            List<RouteResultPreparation.CombineAreaRoutePoint> routeWay = new ArrayList<>();

            for(int j = 0; j < obj.getPointsLength(); ++j) {
               RouteResultPreparation.CombineAreaRoutePoint pnt = new RouteResultPreparation.CombineAreaRoutePoint();
               pnt.x31 = obj.getPoint31XTile(j);
               pnt.y31 = obj.getPoint31YTile(j);
               pnt.originalIndex = j;
               originalWay.add(pnt);
               if (j >= rsr.getStartPointIndex() && j <= rsr.getEndPointIndex()) {
                  routeWay.add(pnt);
               } else if (j <= rsr.getStartPointIndex() && j >= rsr.getEndPointIndex()) {
                  routeWay.add(0, pnt);
               }
            }

            int originalSize = routeWay.size();
            this.simplifyAreaRouteWay(routeWay, originalWay);
            int newsize = routeWay.size();
            if (routeWay.size() != originalSize) {
               RouteDataObject nobj = new RouteDataObject(obj);
               nobj.pointsX = new int[newsize];
               nobj.pointsY = new int[newsize];

               for(int k = 0; k < newsize; ++k) {
                  nobj.pointsX[k] = routeWay.get(k).x31;
                  nobj.pointsY[k] = routeWay.get(k).y31;
               }

               nobj.restrictions = null;
               nobj.restrictionsVia = null;
               nobj.pointTypes = null;
               nobj.pointNames = null;
               nobj.pointNameTypes = null;
               RouteSegmentResult nrsr = new RouteSegmentResult(nobj, 0, newsize - 1);
               result.set(i, nrsr);
            }
         }
      }
   }

   private void simplifyAreaRouteWay(
      List<RouteResultPreparation.CombineAreaRoutePoint> routeWay, List<RouteResultPreparation.CombineAreaRoutePoint> originalWay
   ) {
      boolean changed = true;

      while(changed) {
         changed = false;
         int connectStart = -1;
         int connectLen = 0;
         double dist = 0.0;

         for(int length = routeWay.size() - 1; length > 0 && connectLen == 0; --length) {
            for(int i = 0; i < routeWay.size() - length; ++i) {
               RouteResultPreparation.CombineAreaRoutePoint p = routeWay.get(i);
               RouteResultPreparation.CombineAreaRoutePoint n = routeWay.get(i + length);
               if (this.segmentLineBelongsToPolygon(p, n, originalWay)) {
                  double ndist = BinaryRoutePlanner.squareRootDist(p.x31, p.y31, n.x31, n.y31);
                  if (ndist > dist) {
                     connectStart = i;
                     connectLen = length;
                  }
               }
            }
         }

         while(connectLen > 1) {
            routeWay.remove(connectStart + 1);
            --connectLen;
            changed = true;
         }
      }
   }

   private boolean segmentLineBelongsToPolygon(
      RouteResultPreparation.CombineAreaRoutePoint p,
      RouteResultPreparation.CombineAreaRoutePoint n,
      List<RouteResultPreparation.CombineAreaRoutePoint> originalWay
   ) {
      int intersections = 0;
      int mx = p.x31 / 2 + n.x31 / 2;
      int my = p.y31 / 2 + n.y31 / 2;

      for(int i = 1; i < originalWay.size(); ++i) {
         RouteResultPreparation.CombineAreaRoutePoint p2 = originalWay.get(i - 1);
         RouteResultPreparation.CombineAreaRoutePoint n2 = originalWay.get(i);
         if (p.originalIndex != i
            && p.originalIndex != i - 1
            && n.originalIndex != i
            && n.originalIndex != i - 1
            && MapAlgorithms.linesIntersect(
               (double)p.x31, (double)p.y31, (double)n.x31, (double)n.y31, (double)p2.x31, (double)p2.y31, (double)n2.x31, (double)n2.y31
            )) {
            return false;
         }

         int fx = MapAlgorithms.ray_intersect_x(p2.x31, p2.y31, n2.x31, n2.y31, my);
         if (Integer.MIN_VALUE != fx && mx >= fx) {
            ++intersections;
         }
      }

      return intersections % 2 == 1;
   }

   public List<RouteSegmentResult> prepareResult(RoutingContext ctx, List<RouteSegmentResult> result, boolean recalculation) throws IOException {
      for(int i = 0; i < result.size(); ++i) {
         RouteDataObject road = result.get(i).getObject();
         this.checkAndInitRouteRegion(ctx, road);
         if (road.region != null) {
            road.region.findOrCreateRouteType("osmand_dp", "osmand_delete_point");
         }
      }

      this.combineWayPointsForAreaRouting(ctx, result);
      this.validateAllPointsConnected(result);
      this.splitRoadsAndAttachRoadSegments(ctx, result, recalculation);

      for(int i = 0; i < result.size(); ++i) {
         this.filterMinorStops(result.get(i));
      }

      calculateTimeSpeed(ctx, result);
      this.prepareTurnResults(ctx, result);
      return result;
   }

   public RouteSegmentResult filterMinorStops(RouteSegmentResult seg) {
      List<Integer> stops = null;
      boolean plus = seg.getStartPointIndex() < seg.getEndPointIndex();

      int next;
      for(int i = seg.getStartPointIndex(); i != seg.getEndPointIndex(); i = next) {
         next = plus ? i + 1 : i - 1;
         int[] pointTypes = seg.getObject().getPointTypes(i);
         if (pointTypes != null) {
            for(int j = 0; j < pointTypes.length; ++j) {
               if (pointTypes[j] == seg.getObject().region.stopMinor) {
                  if (stops == null) {
                     stops = new ArrayList<>();
                  }

                  stops.add(i);
               }
            }
         }
      }

      if (stops != null) {
         for(int stop : stops) {
            for(RouteSegmentResult attached : seg.getAttachedRoutes(stop)) {
               int attStopPriority = this.highwaySpeakPriority(attached.getObject().getHighway());
               int segStopPriority = this.highwaySpeakPriority(seg.getObject().getHighway());
               if (segStopPriority < attStopPriority) {
                  seg.getObject().removePointType(stop, seg.getObject().region.stopSign);
                  break;
               }
            }
         }
      }

      return seg;
   }

   public void prepareTurnResults(RoutingContext ctx, List<RouteSegmentResult> result) {
      for(int i = 0; i < result.size(); ++i) {
         TurnType turnType = this.getTurnInfo(result, i, ctx.leftSideNavigation);
         result.get(i).setTurnType(turnType);
      }

      this.determineTurnsToMerge(ctx.leftSideNavigation, result);
      this.ignorePrecedingStraightsOnSameIntersection(ctx.leftSideNavigation, result);
      this.justifyUTurns(ctx.leftSideNavigation, result);
      this.addTurnInfoDescriptions(result);
   }

   protected void ignorePrecedingStraightsOnSameIntersection(boolean leftside, List<RouteSegmentResult> result) {
      RouteSegmentResult nextSegment = null;
      double distanceToNextTurn = 999999.0;

      for(int i = result.size() - 1; i >= 0; --i) {
         if (nextSegment != null
            && nextSegment.getTurnType() != null
            && nextSegment.getTurnType().getValue() != 1
            && !this.isMotorway(nextSegment)
            && distanceToNextTurn == 999999.0) {
            distanceToNextTurn = 0.0;
         }

         RouteSegmentResult currentSegment = result.get(i);
         if (currentSegment != null) {
            distanceToNextTurn += (double)currentSegment.getDistance();
            if (currentSegment.getTurnType() != null && currentSegment.getTurnType().getValue() == 1 && distanceToNextTurn <= 100.0) {
               result.get(i).getTurnType().setSkipToSpeak(true);
            } else {
               nextSegment = currentSegment;
               distanceToNextTurn = 999999.0;
            }
         }
      }
   }

   private void justifyUTurns(boolean leftSide, List<RouteSegmentResult> result) {
      int next;
      for(int i = 1; i < result.size() - 1; i = next) {
         next = i + 1;
         TurnType t = result.get(i).getTurnType();
         if (t != null) {
            TurnType jt = this.justifyUTurn(leftSide, result, i, t);
            if (jt != null) {
               result.get(i).setTurnType(jt);
               next = i + 2;
            }
         }
      }
   }

   public static void calculateTimeSpeed(RoutingContext ctx, List<RouteSegmentResult> result) {
      boolean usePedestrianHeight = ((GeneralRouter)ctx.getRouter()).getProfile() == GeneralRouter.GeneralRouterProfile.PEDESTRIAN
         && ((GeneralRouter)ctx.getRouter()).getHeightObstacles();
      double scarfSeconds = (double)(7.92F / ctx.getRouter().getDefaultSpeed());

      for(int i = 0; i < result.size(); ++i) {
         RouteSegmentResult rr = result.get(i);
         RouteDataObject road = rr.getObject();
         double distOnRoadToPass = 0.0;
         double speed = (double)ctx.getRouter().defineVehicleSpeed(road);
         if (speed == 0.0) {
            speed = (double)ctx.getRouter().getDefaultSpeed();
         } else if (speed > 15.0) {
            speed -= (speed / 15.0 - 1.0) * 2.0;
         }

         boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
         double distance = 0.0;
         float[] heightDistanceArray = null;
         if (usePedestrianHeight) {
            road.calculateHeightArray();
            heightDistanceArray = road.heightDistanceArray;
         }

         int next;
         for(int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
            next = plus ? j + 1 : j - 1;
            double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next), road.getPoint31YTile(next));
            distance += d;
            double obstacle = (double)ctx.getRouter().defineObstacle(road, j, plus);
            if (obstacle < 0.0) {
               obstacle = 0.0;
            }

            distOnRoadToPass += d / speed + obstacle;
            if (usePedestrianHeight) {
               int heightIndex = 2 * j + 1;
               int nextHeightIndex = 2 * next + 1;
               if (heightDistanceArray != null && heightIndex < heightDistanceArray.length && nextHeightIndex < heightDistanceArray.length) {
                  float heightDiff = heightDistanceArray[nextHeightIndex] - heightDistanceArray[heightIndex];
                  if (heightDiff > 0.0F) {
                     distOnRoadToPass += (double)heightDiff * scarfSeconds;
                  }
               }
            }
         }

         rr.setDistance((float)distance);
         rr.setSegmentTime((float)distOnRoadToPass);
         if (distOnRoadToPass != 0.0) {
            rr.setSegmentSpeed((float)(distance / distOnRoadToPass));
         } else {
            rr.setSegmentSpeed((float)speed);
         }
      }
   }

   public static void recalculateTimeDistance(List<RouteSegmentResult> result) {
      for(int i = 0; i < result.size(); ++i) {
         RouteSegmentResult rr = result.get(i);
         RouteDataObject road = rr.getObject();
         double distOnRoadToPass = 0.0;
         double speed = (double)rr.getSegmentSpeed();
         if (speed != 0.0) {
            boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
            double distance = 0.0;

            int next;
            for(int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
               next = plus ? j + 1 : j - 1;
               double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next), road.getPoint31YTile(next));
               distance += d;
               distOnRoadToPass += d / speed;
            }

            rr.setSegmentTime((float)distOnRoadToPass);
            rr.setSegmentSpeed((float)speed);
            rr.setDistance((float)distance);
         }
      }
   }

   private void splitRoadsAndAttachRoadSegments(RoutingContext ctx, List<RouteSegmentResult> result, boolean recalculation) throws IOException {
      for(int i = 0; i < result.size(); ++i) {
         if (ctx.checkIfMemoryLimitCritical(ctx.config.memoryLimitation)) {
            ctx.unloadUnusedTiles(ctx.config.memoryLimitation);
         }

         RouteSegmentResult rr = result.get(i);
         boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
         boolean unmatched = "unmatched".equals(rr.getObject().getHighway());

         int next;
         for(int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
            next = plus ? j + 1 : j - 1;
            if (j == rr.getStartPointIndex()) {
               this.attachRoadSegments(ctx, result, i, j, plus, recalculation);
            }

            if (next != rr.getEndPointIndex()) {
               this.attachRoadSegments(ctx, result, i, next, plus, recalculation);
            }

            List<RouteSegmentResult> attachedRoutes = rr.getAttachedRoutes(next);
            boolean tryToSplit = next != rr.getEndPointIndex() && !rr.getObject().roundabout() && attachedRoutes != null;
            if (rr.getDistance(next, plus) == 0.0F) {
               tryToSplit = false;
            }

            if (tryToSplit) {
               float distBearing = unmatched ? 50.0F : 15.0F;
               float before = rr.getBearingEnd(next, distBearing);
               float after = rr.getBearingBegin(next, distBearing);
               if (rr.getDistance(next, plus) < distBearing) {
                  after = before;
               } else if (rr.getDistance(next, !plus) < distBearing) {
                  before = after;
               }

               double contAngle = Math.abs(MapUtils.degreesDiff((double)before, (double)after));
               boolean straight = contAngle < 45.0;
               boolean isSplit = false;
               if (unmatched && Math.abs(contAngle) >= 45.0) {
                  isSplit = true;
               }

               for(RouteSegmentResult rs : attachedRoutes) {
                  double diff = MapUtils.degreesDiff((double)before, (double)rs.getBearingBegin());
                  if (Math.abs(diff) <= 45.0) {
                     isSplit = true;
                  } else if (!straight && Math.abs(diff) < 100.0) {
                     isSplit = true;
                  }
               }

               if (isSplit) {
                  int endPointIndex = rr.getEndPointIndex();
                  RouteSegmentResult split = new RouteSegmentResult(rr.getObject(), next, endPointIndex);
                  split.copyPreattachedRoutes(rr, Math.abs(next - rr.getStartPointIndex()));
                  rr.setEndPointIndex(next);
                  result.add(i + 1, split);
                  ++i;
                  rr = split;
               }
            }
         }
      }
   }

   private void checkAndInitRouteRegion(RoutingContext ctx, RouteDataObject road) throws IOException {
      BinaryMapIndexReader reader = ctx.reverseMap.get(road.region);
      if (reader != null) {
         reader.initRouteRegion(road.region);
      }
   }

   private void validateAllPointsConnected(List<RouteSegmentResult> result) {
      for(int i = 1; i < result.size(); ++i) {
         RouteSegmentResult rr = result.get(i);
         RouteSegmentResult pr = result.get(i - 1);
         double d = MapUtils.getDistance(pr.getPoint(pr.getEndPointIndex()), rr.getPoint(rr.getStartPointIndex()));
         if (d > 0.0) {
            System.err
               .println(
                  "Points are not connected : "
                     + pr.getObject()
                     + "("
                     + pr.getEndPointIndex()
                     + ") -> "
                     + rr.getObject()
                     + "("
                     + rr.getStartPointIndex()
                     + ") "
                     + d
                     + " meters"
               );
         }
      }
   }

   private List<RouteSegmentResult> convertFinalSegmentToResults(
           RoutingContext ctx,
           BinaryRoutePlanner.FinalRouteSegment finalSegment,
           boolean isIntermediatePoint
   ) {
      List<RouteSegmentResult> result = new ArrayList<>();
      if (finalSegment != null) {
         ctx.routingTime += finalSegment.distanceFromStart;
         BinaryRoutePlanner.RouteSegment segment = finalSegment.reverseWaySearch ? finalSegment.parentRoute : finalSegment.opposite;

         while(segment != null) {
            RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.getSegmentEnd(), segment.getSegmentStart());
            float parentRoutingTime = segment.getParentRoute() != null ? segment.getParentRoute().distanceFromStart : 0.0F;
            res.setRoutingTime(segment.distanceFromStart - parentRoutingTime);
            segment = segment.getParentRoute();
            this.addRouteSegmentToResult(ctx, result, res, false);
         }

         Collections.reverse(result);
         segment = finalSegment.reverseWaySearch ? finalSegment.opposite : finalSegment.parentRoute;

         while(segment != null) {
            RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.getSegmentStart(), segment.getSegmentEnd());
            float parentRoutingTime = segment.getParentRoute() != null ? segment.getParentRoute().distanceFromStart : 0.0F;
            res.setRoutingTime(segment.distanceFromStart - parentRoutingTime);
            segment = segment.getParentRoute();
            this.addRouteSegmentToResult(ctx, result, res, true);
         }

         Collections.reverse(result);

         if (!result.isEmpty() && isIntermediatePoint) {
            result.get(0).setIntermediatePoint(true);
         }
         this.checkTotalRoutingTime(result, finalSegment.distanceFromStart);
      }

      return result;
   }

   protected void checkTotalRoutingTime(List<RouteSegmentResult> result, float cmp) {
      float totalRoutingTime = 0.0F;

      for(RouteSegmentResult r : result) {
         totalRoutingTime += r.getRoutingTime();
      }

      if (Math.abs(totalRoutingTime - cmp) > 1.0F) {
         println("Total sum routing time ! " + totalRoutingTime + " == " + cmp);
      }
   }

   private void addRouteSegmentToResult(RoutingContext ctx, List<RouteSegmentResult> result, RouteSegmentResult res, boolean reverse) {
      if (res.getStartPointIndex() != res.getEndPointIndex()) {
         if (result.size() > 0) {
            RouteSegmentResult last = result.get(result.size() - 1);
            if (last.getObject().id == res.getObject().id
               && ctx.calculationMode != RoutePlannerFrontEnd.RouteCalculationMode.BASE
               && this.combineTwoSegmentResult(res, last, reverse)) {
               return;
            }
         }

         result.add(res);
      }
   }

   private boolean combineTwoSegmentResult(RouteSegmentResult toAdd, RouteSegmentResult previous, boolean reverse) {
      boolean ld = previous.getEndPointIndex() > previous.getStartPointIndex();
      boolean rd = toAdd.getEndPointIndex() > toAdd.getStartPointIndex();
      if (rd == ld) {
         if (toAdd.getStartPointIndex() == previous.getEndPointIndex() && !reverse) {
            previous.setEndPointIndex(toAdd.getEndPointIndex());
            previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime());
            return true;
         }

         if (toAdd.getEndPointIndex() == previous.getStartPointIndex() && reverse) {
            previous.setStartPointIndex(toAdd.getStartPointIndex());
            previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime());
            return true;
         }
      }

      return false;
   }

   public static void printResults(RoutingContext ctx, LatLon start, LatLon end, List<RouteSegmentResult> result) {
      Map<String, Object> info = new LinkedHashMap<>();
      Map<String, Object> route = new LinkedHashMap<>();
      info.put("route", route);
      route.put("routing_time", String.format("%.1f", ctx.routingTime));
      route.put("vehicle", ctx.config.routerName);
      route.put("base", ctx.calculationMode == RoutePlannerFrontEnd.RouteCalculationMode.BASE);
      route.put("start_lat", String.format("%.5f", start.getLatitude()));
      route.put("start_lon", String.format("%.5f", start.getLongitude()));
      route.put("target_lat", String.format("%.5f", end.getLatitude()));
      route.put("target_lon", String.format("%.5f", end.getLongitude()));
      if (result != null) {
         float completeTime = 0.0F;
         float completeDistance = 0.0F;

         for(RouteSegmentResult r : result) {
            completeTime += r.getSegmentTime();
            completeDistance += r.getDistance();
         }

         route.put("complete_distance", String.format("%.1f", completeDistance));
         route.put("complete_time", String.format("%.1f", completeTime));
      }

      route.put("native", ctx.nativeLib != null);
      if (ctx.calculationProgress != null && ctx.calculationProgress.timeToCalculate > 0L) {
         info.putAll(ctx.calculationProgress.getInfo(ctx.calculationProgressFirstPhase));
      }

      String alerts = String.format(
         "Alerts during routing: %d fastRoads, %d slowSegmentsEearlier", ctx.alertFasterRoadToVisitedSegments, ctx.alertSlowerSegmentedWasVisitedEarlier
      );
      if (ctx.alertFasterRoadToVisitedSegments + ctx.alertSlowerSegmentedWasVisitedEarlier == 0) {
         alerts = "No alerts";
      }

      println("ROUTE. " + alerts);
      List<String> routeInfo = new ArrayList<>();
      StringBuilder extraInfo = buildRouteMessagesFromInfo(info, routeInfo);
      if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST && result != null) {
         println(String.format("<test %s>", extraInfo.toString()));
         printRouteInfoSegments(result);
         println("</test>");
         if (ctx.calculationProgressFirstPhase != null) {
            println("<<<1st Phase>>>>");
            List<String> baseRouteInfo = new ArrayList<>();
            buildRouteMessagesFromInfo(ctx.calculationProgressFirstPhase.getInfo(null), baseRouteInfo);

            for(String msg : baseRouteInfo) {
               println(msg);
            }

            println("<<<2nd Phase>>>>");
         }
      }

      for(String msg : routeInfo) {
         println(msg);
      }
   }

   private static StringBuilder buildRouteMessagesFromInfo(Map<String, Object> info, List<String> routeMessages) {
      StringBuilder extraInfo = new StringBuilder();

      for(String key : info.keySet()) {
         if (info.get(key) instanceof Map) {
            Map<String, Object> mp = (Map)info.get(key);
            StringBuilder msg = new StringBuilder("Route <" + key + ">");
            int i = 0;

            for(String mkey : mp.keySet()) {
               msg.append(i++ == 0 ? ": " : ", ");
               Object obj = mp.get(mkey);
               String valueString = obj.toString();
               if (obj instanceof Double || obj instanceof Float) {
                  valueString = String.format("%.1f", ((Number)obj).doubleValue());
               }

               msg.append(mkey).append("=").append(valueString);
               extraInfo.append(" ").append(key + "_" + mkey).append("=\"").append(valueString).append("\"");
            }

            if (routeMessages != null) {
               routeMessages.add(msg.toString());
            }
         }
      }

      return extraInfo;
   }

   private static void printRouteInfoSegments(List<RouteSegmentResult> result) {
      XmlSerializer serializer = null;
      if (PRINT_TO_GPX_FILE != null) {

         try {
            serializer = PlatformUtil.newSerializer();
            serializer.setOutput(new FileWriter(PRINT_TO_GPX_FILE));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "gpx");
            serializer.attribute("", "version", "1.1");
            serializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
            serializer.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            serializer.attribute("", "xmlns:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
            serializer.startTag("", "trk");
            serializer.startTag("", "trkseg");
         } catch (IOException var22) {
            var22.printStackTrace();
            serializer = null;
         } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
         }
      }

      double lastHeight = -180.0;

      for(RouteSegmentResult res : result) {
         String name = res.getObject().getName();
         String ref = res.getObject().getRef("", false, res.isForwardDirection());
         if (name == null) {
            name = "";
         }

         if (ref != null) {
            name = name + " (" + ref + ") ";
         }

         StringBuilder additional = new StringBuilder();
         additional.append("time = \"").append((float)((int)res.getSegmentTime() * 100) / 100.0F).append("\" ");
         if (res.getRoutingTime() > 0.0F) {
            additional.append("rtime = \"").append((float)((int)res.getRoutingTime() * 100) / 100.0F).append("\" ");
         }

         additional.append("name = \"").append(name).append("\" ");
         float ms = res.getObject().getMaximumSpeed(res.isForwardDirection());
         if (ms > 0.0F) {
            additional.append("maxspeed = \"").append(Math.round(ms * 3.6F)).append("\" ");
         }

         additional.append("distance = \"").append((float)((int)res.getDistance() * 100) / 100.0F).append("\" ");
         additional.append(res.getObject().getHighway()).append(" ");
         if (res.getTurnType() != null) {
            additional.append("turn = \"").append(res.getTurnType()).append("\" ");
            additional.append("turn_angle = \"").append(res.getTurnType().getTurnAngle()).append("\" ");
            if (res.getTurnType().getLanes() != null) {
               additional.append("lanes = \"").append(Arrays.toString(res.getTurnType().getLanes())).append("\" ");
            }
         }

         additional.append("start_bearing = \"").append(res.getBearingBegin()).append("\" ");
         additional.append("end_bearing = \"").append(res.getBearingEnd()).append("\" ");
         additional.append("height = \"").append(Arrays.toString(res.getHeightValues())).append("\" ");
         additional.append("description = \"").append(res.getDescription()).append("\" ");
         println(
            MessageFormat.format(
               "\t<segment id=\"{0}\" oid=\"{1}\" start=\"{2}\" end=\"{3}\" {4}/>",
               (res.getObject().getId() >> 6) + "",
               res.getObject().getId() + "",
               res.getStartPointIndex() + "",
               res.getEndPointIndex() + "",
               additional.toString()
            )
         );
         int inc = res.getStartPointIndex() < res.getEndPointIndex() ? 1 : -1;
         int indexnext = res.getStartPointIndex();
         LatLon prev = null;
         int index = res.getStartPointIndex();

         while(index != res.getEndPointIndex()) {
            index = indexnext;
            indexnext += inc;
            if (serializer != null) {
               try {
                  LatLon l = res.getPoint(index);
                  serializer.startTag("", "trkpt");
                  serializer.attribute("", "lat", l.getLatitude() + "");
                  serializer.attribute("", "lon", l.getLongitude() + "");
                  float[] vls = res.getObject().heightDistanceArray;
                  double dist = prev == null ? 0.0 : MapUtils.getDistance(prev, l);
                  if (index * 2 + 1 < vls.length) {
                     double h = (double)vls[2 * index + 1];
                     serializer.startTag("", "ele");
                     serializer.text(h + "");
                     serializer.endTag("", "ele");
                     if (lastHeight != -180.0 && dist > 0.0) {
                        serializer.startTag("", "cmt");
                        serializer.text(
                           (float)((h - lastHeight) / dist * 100.0)
                              + "%  degree "
                              + (double)((float)Math.atan((h - lastHeight) / dist)) / Math.PI * 180.0
                              + " asc "
                              + (float)(h - lastHeight)
                              + " dist "
                              + (float)dist
                        );
                        serializer.endTag("", "cmt");
                        serializer.startTag("", "slope");
                        serializer.text((h - lastHeight) / dist * 100.0 + "");
                        serializer.endTag("", "slope");
                     }

                     serializer.startTag("", "desc");
                     serializer.text((res.getObject().getId() >> 6) + " " + index);
                     serializer.endTag("", "desc");
                     lastHeight = h;
                  } else if (lastHeight != -180.0) {
                  }

                  serializer.endTag("", "trkpt");
                  prev = l;
               } catch (IOException var21) {
                  var21.printStackTrace();
               }
            }
         }

         printAdditionalPointInfo(res);
      }

      if (serializer != null) {
         try {
            serializer.endTag("", "trkseg");
            serializer.endTag("", "trk");
            serializer.endTag("", "gpx");
            serializer.endDocument();
            serializer.flush();
         } catch (IOException var20) {
            var20.printStackTrace();
         }
      }
   }

   protected void calculateStatistics(List<RouteSegmentResult> result) {
      InputStream is = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
      Map<String, String> renderingConstants = new LinkedHashMap<>();

      try {
         InputStream pis = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");

         try {
            XmlPullParser parser = PlatformUtil.newXMLPullParser();
            parser.setInput(pis, "UTF-8");

            int tok;
            while((tok = parser.next()) != 1) {
               if (tok == 2) {
                  String tagName = parser.getName();
                  if (tagName.equals("renderingConstant") && !renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
                     renderingConstants.put(parser.getAttributeValue("", "name"), parser.getAttributeValue("", "value"));
                  }
               }
            }
         } finally {
            pis.close();
         }

         RenderingRulesStorage rrs = new RenderingRulesStorage("default", renderingConstants);
         rrs.parseRulesFromXmlInputStream(
            is,
            new RenderingRulesStorage.RenderingRulesStorageResolver() {
               @Override
               public RenderingRulesStorage resolve(String name, RenderingRulesStorage.RenderingRulesStorageResolver ref) throws XmlPullParserException, IOException {
                  throw new UnsupportedOperationException();
               }
            },
            false
         );
         RenderingRuleSearchRequest var15 = new RenderingRuleSearchRequest(rrs);

         for(RouteStatisticsHelper.RouteStatistics r : RouteStatisticsHelper.calculateRouteStatistic(result, null, rrs, null, var15)) {
            System.out.println(r);
         }
      } catch (Exception var13) {
         throw new IllegalStateException(var13.getMessage(), var13);
      }
   }

   private static void printAdditionalPointInfo(RouteSegmentResult res) {
      boolean plus = res.getStartPointIndex() < res.getEndPointIndex();
      int k = res.getStartPointIndex();

      while(k != res.getEndPointIndex()) {
         int[] tp = res.getObject().getPointTypes(k);
         String[] pointNames = res.getObject().getPointNames(k);
         int[] pointNameTypes = res.getObject().getPointNameTypes(k);
         if (tp != null || pointNameTypes != null) {
            StringBuilder bld = new StringBuilder();
            bld.append("<point " + k);
            if (tp != null) {
               for(int t = 0; t < tp.length; ++t) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule rr = res.getObject().region.quickGetEncodingRule(tp[t]);
                  bld.append(" " + rr.getTag() + "=\"" + rr.getValue() + "\"");
               }
            }

            if (pointNameTypes != null) {
               for(int t = 0; t < pointNameTypes.length; ++t) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule rr = res.getObject().region.quickGetEncodingRule(pointNameTypes[t]);
                  bld.append(" " + rr.getTag() + "=\"" + pointNames[t] + "\"");
               }
            }

            bld.append("/>");
            println("\t" + bld.toString());
         }

         if (plus) {
            ++k;
         } else {
            --k;
         }
      }
   }

   public void addTurnInfoDescriptions(List<RouteSegmentResult> result) {
      int prevSegment = -1;
      float dist = 0.0F;

      for(int i = 0; i <= result.size(); ++i) {
         if (i == result.size() || result.get(i).getTurnType() != null) {
            if (prevSegment >= 0) {
               String turn = result.get(prevSegment).getTurnType().toString();
               result.get(prevSegment).setDescription(turn + MessageFormat.format(" and go {0,number,#.##} meters", dist));
               if (result.get(prevSegment).getTurnType().isSkipToSpeak()) {
                  result.get(prevSegment).setDescription("[MUTE] " + result.get(prevSegment).getDescription());
               }
            }

            prevSegment = i;
            dist = 0.0F;
         }

         if (i < result.size()) {
            dist += result.get(i).getDistance();
         }
      }
   }

   protected TurnType justifyUTurn(boolean leftside, List<RouteSegmentResult> result, int i, TurnType t) {
      boolean tl = TurnType.isLeftTurnNoUTurn(t.getValue());
      boolean tr = TurnType.isRightTurnNoUTurn(t.getValue());
      if (tl || tr) {
         TurnType tnext = result.get(i + 1).getTurnType();
         if (tnext != null && result.get(i).getDistance() < 50.0F) {
            boolean ut = true;
            if (i > 0) {
               double uTurn = MapUtils.degreesDiff((double)result.get(i - 1).getBearingEnd(), (double)result.get(i + 1).getBearingBegin());
               if (Math.abs(uTurn) < 120.0) {
                  ut = false;
               }
            }

            if (result.get(i - 1).getObject().getOneway() == 0 || result.get(i + 1).getObject().getOneway() == 0) {
               ut = false;
            }

            if (!Algorithms.objectEquals(this.getStreetName(result, i - 1, false), this.getStreetName(result, i + 1, true))) {
               ut = false;
            }

            if (ut) {
               tnext.setSkipToSpeak(true);
               if (tl && TurnType.isLeftTurnNoUTurn(tnext.getValue())) {
                  TurnType tt = TurnType.valueOf(10, false);
                  tt.setLanes(t.getLanes());
                  return tt;
               }

               if (tr && TurnType.isRightTurnNoUTurn(tnext.getValue())) {
                  TurnType tt = TurnType.valueOf(10, true);
                  tt.setLanes(t.getLanes());
                  return tt;
               }
            }
         }
      }

      return null;
   }

   private String getStreetName(List<RouteSegmentResult> result, int i, boolean dir) {
      String nm = result.get(i).getObject().getName();
      if (Algorithms.isEmpty(nm)) {
         if (!dir) {
            if (i > 0) {
               nm = result.get(i - 1).getObject().getName();
            }
         } else if (i < result.size() - 1) {
            nm = result.get(i + 1).getObject().getName();
         }
      }

      return nm;
   }

   private void determineTurnsToMerge(boolean leftside, List<RouteSegmentResult> result) {
      RouteSegmentResult nextSegment = null;
      double dist = 0.0;

      for(int i = result.size() - 1; i >= 0; --i) {
         RouteSegmentResult currentSegment = result.get(i);
         TurnType currentTurn = currentSegment.getTurnType();
         dist += (double)currentSegment.getDistance();
         if (currentTurn != null && currentTurn.getLanes() != null) {
            boolean merged = false;
            if (nextSegment != null) {
               String hw = currentSegment.getObject().getHighway();
               double mergeDistance = 200.0;
               if (hw != null && (hw.startsWith("trunk") || hw.startsWith("motorway"))) {
                  mergeDistance = 400.0;
               }

               if (dist < mergeDistance) {
                  this.mergeTurnLanes(leftside, currentSegment, nextSegment);
                  this.inferCommonActiveLane(currentSegment.getTurnType(), nextSegment.getTurnType());
                  merged = true;
               }
            }

            if (!merged) {
               TurnType tt = currentSegment.getTurnType();
               this.inferActiveTurnLanesFromTurn(tt, tt.getValue());
            }

            nextSegment = currentSegment;
            dist = 0.0;
         }
      }
   }

   private void inferActiveTurnLanesFromTurn(TurnType tt, int type) {
      boolean found = false;
      if (tt.getValue() == type && tt.getLanes() != null) {
         for(int it = 0; it < tt.getLanes().length; ++it) {
            int turn = tt.getLanes()[it];
            if (TurnType.getPrimaryTurn(turn) == type || TurnType.getSecondaryTurn(turn) == type || TurnType.getTertiaryTurn(turn) == type) {
               found = true;
               break;
            }
         }
      }

      if (found) {
         for(int it = 0; it < tt.getLanes().length; ++it) {
            int turn = tt.getLanes()[it];
            if (TurnType.getPrimaryTurn(turn) != type) {
               if (TurnType.getSecondaryTurn(turn) == type) {
                  int st = TurnType.getSecondaryTurn(turn);
                  TurnType.setSecondaryTurn(tt.getLanes(), it, TurnType.getPrimaryTurn(turn));
                  TurnType.setPrimaryTurn(tt.getLanes(), it, st);
               } else if (TurnType.getTertiaryTurn(turn) == type) {
                  int st = TurnType.getTertiaryTurn(turn);
                  TurnType.setTertiaryTurn(tt.getLanes(), it, TurnType.getPrimaryTurn(turn));
                  TurnType.setPrimaryTurn(tt.getLanes(), it, st);
               } else {
                  tt.getLanes()[it] = turn & -2;
               }
            }
         }
      }
   }

   private boolean mergeTurnLanes(boolean leftSide, RouteSegmentResult currentSegment, RouteSegmentResult nextSegment) {
      RouteResultPreparation.MergeTurnLaneTurn active = new RouteResultPreparation.MergeTurnLaneTurn(currentSegment);
      RouteResultPreparation.MergeTurnLaneTurn target = new RouteResultPreparation.MergeTurnLaneTurn(nextSegment);
      if (active.activeLen < 2) {
         return false;
      } else if (target.activeStartIndex == -1) {
         return false;
      } else {
         boolean changed = false;
         if (target.isActiveTurnMostLeft()) {
            if (target.activeLen < active.activeLen) {
               active.activeEndIndex -= active.activeLen - target.activeLen;
               changed = true;
            }
         } else if (target.isActiveTurnMostRight()) {
            if (target.activeLen < active.activeLen) {
               active.activeStartIndex += active.activeLen - target.activeLen;
               changed = true;
            }
         } else if (target.activeLen < active.activeLen) {
            if (target.originalLanes.length == active.activeLen) {
               active.activeEndIndex = active.activeStartIndex + target.activeEndIndex;
               active.activeStartIndex += target.activeStartIndex;
               changed = true;
            } else {
               int straightActiveLen = 0;
               int straightActiveBegin = -1;

               for(int i = active.activeStartIndex; i <= active.activeEndIndex; ++i) {
                  if (TurnType.hasAnyTurnLane(active.originalLanes[i], 1)) {
                     ++straightActiveLen;
                     if (straightActiveBegin == -1) {
                        straightActiveBegin = i;
                     }
                  }
               }

               if (straightActiveBegin != -1 && straightActiveLen <= target.activeLen) {
                  active.activeStartIndex = straightActiveBegin;
                  active.activeEndIndex = straightActiveBegin + straightActiveLen - 1;
                  changed = true;
               } else {
                  if (active.activeStartIndex == 0) {
                     ++active.activeStartIndex;
                     --active.activeLen;
                  }

                  if (active.activeEndIndex == active.originalLanes.length - 1) {
                     --active.activeEndIndex;
                     --active.activeLen;
                  }

                  float ratio = (float)(active.activeLen - target.activeLen) / 2.0F;
                  if (ratio > 0.0F) {
                     active.activeEndIndex = (int)Math.ceil((double)((float)active.activeEndIndex - ratio));
                     active.activeStartIndex = (int)Math.floor((double)((float)active.activeStartIndex + ratio));
                  }

                  changed = true;
               }
            }
         }

         if (!changed) {
            return false;
         } else {
            for(int i = 0; i < active.disabledLanes.length; ++i) {
               if (i >= active.activeStartIndex && i <= active.activeEndIndex && active.originalLanes[i] % 2 == 1) {
                  active.disabledLanes[i] |= 1;
               }
            }

            TurnType currentTurn = currentSegment.getTurnType();
            currentTurn.setLanes(active.disabledLanes);
            return true;
         }
      }
   }

   private void inferCommonActiveLane(TurnType currentTurn, TurnType nextTurn) {
      int[] lanes = currentTurn.getLanes();
      TIntHashSet turnSet = new TIntHashSet();

      for(int i = 0; i < lanes.length; ++i) {
         if (lanes[i] % 2 == 1) {
            int singleTurn = TurnType.getPrimaryTurn(lanes[i]);
            turnSet.add(singleTurn);
            if (TurnType.getSecondaryTurn(lanes[i]) != 0) {
               turnSet.add(TurnType.getSecondaryTurn(lanes[i]));
            }

            if (TurnType.getTertiaryTurn(lanes[i]) != 0) {
               turnSet.add(TurnType.getTertiaryTurn(lanes[i]));
            }
         }
      }

      int singleTurn = 0;
      if (turnSet.size() == 1) {
         singleTurn = turnSet.iterator().next();
      } else if ((currentTurn.goAhead() || currentTurn.keepLeft() || currentTurn.keepRight()) && turnSet.contains(nextTurn.getValue())) {
         if (currentTurn.isPossibleLeftTurn() && TurnType.isLeftTurn(nextTurn.getValue())) {
            singleTurn = nextTurn.getValue();
         } else if (currentTurn.isPossibleLeftTurn() && TurnType.isLeftTurn(nextTurn.getActiveCommonLaneTurn())) {
            singleTurn = nextTurn.getActiveCommonLaneTurn();
         } else if (currentTurn.isPossibleRightTurn() && TurnType.isRightTurn(nextTurn.getValue())) {
            singleTurn = nextTurn.getValue();
         } else if (currentTurn.isPossibleRightTurn() && TurnType.isRightTurn(nextTurn.getActiveCommonLaneTurn())) {
            singleTurn = nextTurn.getActiveCommonLaneTurn();
         }
      }

      if (singleTurn == 0) {
         singleTurn = currentTurn.getValue();
         if (singleTurn == 8 || singleTurn == 9) {
            return;
         }
      }

      for(int i = 0; i < lanes.length; ++i) {
         if (lanes[i] % 2 == 1 && TurnType.getPrimaryTurn(lanes[i]) != singleTurn) {
            if (TurnType.getSecondaryTurn(lanes[i]) == singleTurn) {
               TurnType.setSecondaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]));
               TurnType.setPrimaryTurn(lanes, i, singleTurn);
            } else if (TurnType.getTertiaryTurn(lanes[i]) == singleTurn) {
               TurnType.setTertiaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]));
               TurnType.setPrimaryTurn(lanes, i, singleTurn);
            } else {
               lanes[i]--;
            }
         }
      }
   }

   private int highwaySpeakPriority(String highway) {
      if (highway == null || highway.endsWith("track") || highway.endsWith("services") || highway.endsWith("service") || highway.endsWith("path")) {
         return 5;
      } else {
         return !highway.endsWith("_link")
               && !highway.endsWith("unclassified")
               && !highway.endsWith("road")
               && !highway.endsWith("living_street")
               && !highway.endsWith("residential")
               && !highway.endsWith("tertiary")
            ? 0
            : 1;
      }
   }

   private TurnType getTurnInfo(List<RouteSegmentResult> result, int i, boolean leftSide) {
      if (i == 0) {
         return TurnType.valueOf(1, false);
      } else {
         RouteSegmentResult prev = result.get(i - 1);
         if (prev.getObject().roundabout()) {
            return null;
         } else {
            RouteSegmentResult rr = result.get(i);
            if (rr.getObject().roundabout()) {
               return this.processRoundaboutTurn(result, i, leftSide, prev, rr);
            } else {
               TurnType t = null;
               if (prev != null) {
                  float bearingDist = 15.0F;
                  if ("unmatched".equals(rr.getObject().getHighway())) {
                     bearingDist = 50.0F;
                  }

                  double mpi = MapUtils.degreesDiff(
                     (double)prev.getBearingEnd(prev.getEndPointIndex(), Math.min(prev.getDistance(), bearingDist)),
                     (double)rr.getBearingBegin(rr.getStartPointIndex(), Math.min(rr.getDistance(), bearingDist))
                  );
                  if (mpi >= 45.0) {
                     if (mpi < 45.0) {
                        t = TurnType.valueOf(3, leftSide);
                     } else if (mpi < 120.0) {
                        t = TurnType.valueOf(2, leftSide);
                     } else if (!(mpi < 150.0) && !leftSide) {
                        t = TurnType.valueOf(10, leftSide);
                     } else {
                        t = TurnType.valueOf(4, leftSide);
                     }

                     int[] lanes = this.getTurnLanesInfo(prev, t.getValue());
                     t.setLanes(lanes);
                  } else if (!(mpi < -45.0)) {
                     t = this.attachKeepLeftInfoAndLanes(leftSide, prev, rr);
                  } else {
                     if (mpi > -45.0) {
                        t = TurnType.valueOf(6, leftSide);
                     } else if (mpi > -120.0) {
                        t = TurnType.valueOf(5, leftSide);
                     } else if (!(mpi > -150.0) && leftSide) {
                        t = TurnType.valueOf(11, leftSide);
                     } else {
                        t = TurnType.valueOf(7, leftSide);
                     }

                     int[] lanes = this.getTurnLanesInfo(prev, t.getValue());
                     t.setLanes(lanes);
                  }

                  if (t != null) {
                     t.setTurnAngle((float)(-mpi));
                  }
               }

               return t;
            }
         }
      }
   }

   private int[] getTurnLanesInfo(RouteSegmentResult prevSegm, int mainTurnType) {
      String turnLanes = getTurnLanesString(prevSegm);
      int[] lanesArray;
      if (turnLanes == null) {
         if (prevSegm.getTurnType() == null || prevSegm.getTurnType().getLanes() == null || !(prevSegm.getDistance() < 100.0F)) {
            return null;
         }

         int[] lns = prevSegm.getTurnType().getLanes();
         TIntArrayList lst = new TIntArrayList();

         for(int i = 0; i < lns.length; ++i) {
            if (lns[i] % 2 == 1) {
               lst.add(lns[i] >> 1 << 1);
            }
         }

         if (lst.isEmpty()) {
            return null;
         }

         lanesArray = lst.toArray();
      } else {
         lanesArray = calculateRawTurnLanes(turnLanes, mainTurnType);
      }

      boolean isSet = this.setAllowedLanes(mainTurnType, lanesArray);
      if (!isSet && lanesArray.length > 0) {
         boolean leftTurn = TurnType.isLeftTurn(mainTurnType);
         int ind = leftTurn ? 0 : lanesArray.length - 1;
         int primaryTurn = TurnType.getPrimaryTurn(lanesArray[ind]);
         int st = TurnType.getSecondaryTurn(lanesArray[ind]);
         if (leftTurn) {
            if (!TurnType.isLeftTurn(primaryTurn)) {
               TurnType.setPrimaryTurnAndReset(lanesArray, ind, 2);
               TurnType.setSecondaryTurn(lanesArray, ind, primaryTurn);
               TurnType.setTertiaryTurn(lanesArray, ind, st);
               primaryTurn = 2;
               lanesArray[ind] |= 1;
            }
         } else if (!TurnType.isRightTurn(primaryTurn)) {
            TurnType.setPrimaryTurnAndReset(lanesArray, ind, 5);
            TurnType.setSecondaryTurn(lanesArray, ind, primaryTurn);
            TurnType.setTertiaryTurn(lanesArray, ind, st);
            primaryTurn = 5;
            lanesArray[ind] |= 1;
         }

         this.setAllowedLanes(primaryTurn, lanesArray);
      }

      return lanesArray;
   }

   protected boolean setAllowedLanes(int mainTurnType, int[] lanesArray) {
      boolean turnSet = false;

      for(int i = 0; i < lanesArray.length; ++i) {
         if (TurnType.getPrimaryTurn(lanesArray[i]) == mainTurnType) {
            lanesArray[i] |= 1;
            turnSet = true;
         }
      }

      return turnSet;
   }

   private TurnType processRoundaboutTurn(List<RouteSegmentResult> result, int i, boolean leftSide, RouteSegmentResult prev, RouteSegmentResult rr) {
      int exit = 1;
      RouteSegmentResult last = rr;
      RouteSegmentResult lastRoundabout = rr;

      for(int j = i; j < result.size(); ++j) {
         RouteSegmentResult rnext = result.get(j);
         last = rnext;
         if (!rnext.getObject().roundabout()) {
            break;
         }

         lastRoundabout = rnext;
         boolean plus = rnext.getStartPointIndex() < rnext.getEndPointIndex();
         int k = rnext.getStartPointIndex();
         if (j == i) {
         }

         for(; k != rnext.getEndPointIndex(); k = plus ? k + 1 : k - 1) {
            int attachedRoads = rnext.getAttachedRoutes(k).size();
            if (attachedRoads > 0) {
               ++exit;
            }
         }
      }

      TurnType t = TurnType.getExitTurn(exit, 0.0F, leftSide);
      float turnAngleBasedOnOutRoads = (float)MapUtils.degreesDiff((double)last.getBearingBegin(), (double)prev.getBearingEnd());
      float turnAngleBasedOnCircle = (float)(-MapUtils.degreesDiff((double)rr.getBearingBegin(), (double)(lastRoundabout.getBearingEnd() + 180.0F)));
      if (Math.abs(turnAngleBasedOnOutRoads) > 120.0F) {
         t.setTurnAngle(turnAngleBasedOnCircle);
      } else {
         t.setTurnAngle(turnAngleBasedOnOutRoads);
      }

      return t;
   }

   private TurnType attachKeepLeftInfoAndLanes(boolean leftSide, RouteSegmentResult prevSegm, RouteSegmentResult currentSegm) {
      List<RouteSegmentResult> attachedRoutes = currentSegm.getAttachedRoutes(currentSegm.getStartPointIndex());
      if (attachedRoutes != null && !attachedRoutes.isEmpty()) {
         String turnLanesPrevSegm = getTurnLanesString(prevSegm);
         RouteResultPreparation.RoadSplitStructure rs = this.calculateRoadSplitStructure(prevSegm, currentSegm, attachedRoutes, turnLanesPrevSegm);
         if (rs.roadsOnLeft + rs.roadsOnRight == 0) {
            return null;
         } else if (turnLanesPrevSegm != null) {
            return this.createKeepLeftRightTurnBasedOnTurnTypes(rs, prevSegm, currentSegm, turnLanesPrevSegm, leftSide);
         } else {
            return !rs.keepLeft && !rs.keepRight ? null : this.createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
         }
      } else {
         return null;
      }
   }

   protected TurnType createKeepLeftRightTurnBasedOnTurnTypes(
      RouteResultPreparation.RoadSplitStructure rs, RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, String turnLanes, boolean leftSide
   ) {
      TurnType t = TurnType.valueOf(1, leftSide);
      int[] rawLanes = calculateRawTurnLanes(turnLanes, 1);
      boolean possiblyLeftTurn = rs.roadsOnLeft == 0;
      boolean possiblyRightTurn = rs.roadsOnRight == 0;

      for(int k = 0; k < rawLanes.length; ++k) {
         int turn = TurnType.getPrimaryTurn(rawLanes[k]);
         int sturn = TurnType.getSecondaryTurn(rawLanes[k]);
         int tturn = TurnType.getTertiaryTurn(rawLanes[k]);
         if (turn == 10 || sturn == 10 || tturn == 10) {
            possiblyLeftTurn = true;
         }

         if (turn == 11 || sturn == 11 || tturn == 11) {
            possiblyRightTurn = true;
         }
      }

      if (!rs.keepLeft && !rs.keepRight) {
         Integer[] possibleTurns = this.getPossibleTurns(rawLanes, false, false);
         int tp = 1;
         if (possibleTurns.length == 1) {
            tp = possibleTurns[0];
         } else if (possibleTurns.length == 3 && (!possiblyLeftTurn || !possiblyRightTurn) && TurnType.isSlightTurn(possibleTurns[1])) {
            tp = possibleTurns[1];
            t = TurnType.valueOf(tp, leftSide);
            t.setSkipToSpeak(true);
         }

         for(int k = 0; k < rawLanes.length; ++k) {
            int turn = TurnType.getPrimaryTurn(rawLanes[k]);
            int sturn = TurnType.getSecondaryTurn(rawLanes[k]);
            int tturn = TurnType.getTertiaryTurn(rawLanes[k]);
            boolean active = false;
            if ((!TurnType.isRightTurn(sturn) || !possiblyRightTurn) && (!TurnType.isLeftTurn(sturn) || !possiblyLeftTurn)) {
               if ((!TurnType.isRightTurn(tturn) || !possiblyRightTurn) && (!TurnType.isLeftTurn(tturn) || !possiblyLeftTurn)) {
                  if ((!TurnType.isRightTurn(turn) || !possiblyRightTurn) && (!TurnType.isLeftTurn(turn) || !possiblyLeftTurn)) {
                     if (TurnType.isSlightTurn(turn) && !possiblyRightTurn && !possiblyLeftTurn) {
                        active = true;
                     } else if (turn == tp) {
                        active = true;
                     }
                  } else {
                     active = true;
                  }
               } else {
                  TurnType.setTertiaryToPrimary(rawLanes, k);
                  active = true;
               }
            } else {
               TurnType.setSecondaryToPrimary(rawLanes, k);
               active = true;
            }

            if (active) {
               rawLanes[k] |= 1;
            }
         }
      } else {
         String[] splitLaneOptions = turnLanes.split("\\|", -1);
         int activeBeginIndex = this.findActiveIndex(rawLanes, splitLaneOptions, rs.leftLanes, true, rs.leftLanesInfo, rs.roadsOnLeft, rs.addRoadsOnLeft);
         int activeEndIndex = this.findActiveIndex(rawLanes, splitLaneOptions, rs.rightLanes, false, rs.rightLanesInfo, rs.roadsOnRight, rs.addRoadsOnRight);
         if (activeBeginIndex == -1 || activeEndIndex == -1 || activeBeginIndex > activeEndIndex) {
            return this.createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
         }

         for(int k = 0; k < rawLanes.length; ++k) {
            if (k >= activeBeginIndex && k <= activeEndIndex) {
               rawLanes[k] |= 1;
            }
         }

         int tp = this.inferSlightTurnFromActiveLanes(rawLanes, rs.keepLeft, rs.keepRight);
         if (tp != 0) {
            for(int i = 0; i < rawLanes.length; ++i) {
               if (TurnType.getSecondaryTurn(rawLanes[i]) == tp) {
                  TurnType.setSecondaryToPrimary(rawLanes, i);
                  rawLanes[i] |= 1;
               } else if (TurnType.getPrimaryTurn(rawLanes[i]) == tp) {
                  rawLanes[i] |= 1;
               }
            }
         }

         if (tp != t.getValue() && tp != 0) {
            t = TurnType.valueOf(tp, leftSide);
         } else if (rs.keepRight) {
            t = this.getTurnByCurrentTurns(rs.leftLanesInfo, currentSegm, rawLanes, 9, leftSide);
         } else if (rs.keepLeft) {
            t = this.getTurnByCurrentTurns(rs.rightLanesInfo, currentSegm, rawLanes, 8, leftSide);
         }
      }

      if (TurnType.isKeepDirectionTurn(t.getValue())) {
         t.setSkipToSpeak(true);
      }

      t.setLanes(rawLanes);
      t.setPossibleLeftTurn(possiblyLeftTurn);
      t.setPossibleRightTurn(possiblyRightTurn);
      return t;
   }

   private TurnType getTurnByCurrentTurns(List<int[]> otherSideLanesInfo, RouteSegmentResult currentSegm, int[] rawLanes, int keepTurnType, boolean leftSide) {
      TIntHashSet otherSideTurns = new TIntHashSet();
      if (otherSideLanesInfo != null) {
         for(int[] li : otherSideLanesInfo) {
            if (li != null) {
               for(int i : li) {
                  TurnType.collectTurnTypes(i, otherSideTurns);
               }
            }
         }
      }

      TIntHashSet currentTurns = new TIntHashSet();

      for(int ln : rawLanes) {
         TurnType.collectTurnTypes(ln, currentTurns);
      }

      if (currentTurns.containsAll(otherSideTurns)) {
         currentTurns.removeAll(otherSideTurns);
         if (currentTurns.size() == 1) {
            return TurnType.valueOf(currentTurns.iterator().next(), leftSide);
         }
      }

      return TurnType.valueOf(keepTurnType, leftSide);
   }

   protected int findActiveIndex(int[] rawLanes, String[] splitLaneOptions, int lanes, boolean left, List<int[]> lanesInfo, int roads, int addRoads) {
      int activeStartIndex = -1;
      boolean lookupSlightTurn = addRoads > 0;
      TIntHashSet addedTurns = new TIntHashSet();
      int diffTurnRoads = roads;
      int increaseTurnRoads = 0;

      for(int[] li : lanesInfo) {
         TIntHashSet set = new TIntHashSet();
         if (li != null) {
            for(int i : li) {
               TurnType.collectTurnTypes(i, set);
            }
         }

         increaseTurnRoads = Math.max(set.size() - 1, 0);
      }

      for(int i = 0; i < rawLanes.length; ++i) {
         int ind = left ? i : rawLanes.length - i - 1;
         if (!lookupSlightTurn || TurnType.hasAnySlightTurnLane(rawLanes[ind])) {
            String[] laneTurns = splitLaneOptions[ind].split(";");
            int cnt = 0;

            for(String lTurn : laneTurns) {
               boolean added = addedTurns.add(TurnType.convertType(lTurn));
               if (added) {
                  ++cnt;
                  --diffTurnRoads;
               }
            }

            lanes -= cnt;
            lookupSlightTurn = false;
         }

         if (lanes < 0 || diffTurnRoads + increaseTurnRoads < 0) {
            activeStartIndex = ind;
            break;
         }

         if (diffTurnRoads < 0 && activeStartIndex < 0) {
            activeStartIndex = ind;
         }
      }

      return activeStartIndex;
   }

   protected RouteResultPreparation.RoadSplitStructure calculateRoadSplitStructure(
      RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, List<RouteSegmentResult> attachedRoutes, String turnLanesPrevSegm
   ) {
      RouteResultPreparation.RoadSplitStructure rs = new RouteResultPreparation.RoadSplitStructure();
      int speakPriority = Math.max(
         this.highwaySpeakPriority(prevSegm.getObject().getHighway()), this.highwaySpeakPriority(currentSegm.getObject().getHighway())
      );

      for(RouteSegmentResult attached : attachedRoutes) {
         boolean restricted = false;

         for(int k = 0; k < prevSegm.getObject().getRestrictionLength(); ++k) {
            if (prevSegm.getObject().getRestrictionId(k) == attached.getObject().getId() && prevSegm.getObject().getRestrictionType(k) <= 4) {
               restricted = true;
               break;
            }
         }

         if (!restricted) {
            double ex = MapUtils.degreesDiff((double)attached.getBearingBegin(), (double)currentSegm.getBearingBegin());
            double mpi = Math.abs(MapUtils.degreesDiff((double)prevSegm.getBearingEnd(), (double)attached.getBearingBegin()));
            int rsSpeakPriority = this.highwaySpeakPriority(attached.getObject().getHighway());
            int lanes = this.countLanesMinOne(attached);
            int[] turnLanesAttachedRoad = parseTurnLanes(attached.getObject(), (double)attached.getBearingBegin() * Math.PI / 180.0);
            boolean smallStraightVariation = mpi < 45.0;
            boolean smallTargetVariation = Math.abs(ex) < 45.0;
            boolean attachedOnTheRight = ex >= 0.0;
            boolean verySharpTurn = Math.abs(ex) > 150.0;
            boolean prevSegmHasTU = this.hasTU(turnLanesPrevSegm, attachedOnTheRight);
            if (!verySharpTurn && !prevSegmHasTU) {
               if (attachedOnTheRight) {
                  ++rs.roadsOnRight;
               } else {
                  ++rs.roadsOnLeft;
               }
            }

            if (turnLanesPrevSegm != null || rsSpeakPriority != 5 || speakPriority == 5) {
               if (!smallTargetVariation && !smallStraightVariation) {
                  if (attachedOnTheRight) {
                     ++rs.addRoadsOnRight;
                  } else {
                     ++rs.addRoadsOnLeft;
                  }
               } else {
                  if (attachedOnTheRight) {
                     rs.keepLeft = true;
                     rs.rightLanes += lanes;
                     if (turnLanesAttachedRoad != null) {
                        rs.rightLanesInfo.add(turnLanesAttachedRoad);
                     }
                  } else {
                     rs.keepRight = true;
                     rs.leftLanes += lanes;
                     if (turnLanesAttachedRoad != null) {
                        rs.leftLanesInfo.add(turnLanesAttachedRoad);
                     }
                  }

                  rs.speak = rs.speak || rsSpeakPriority <= speakPriority;
               }
            }
         }
      }

      return rs;
   }

   private boolean hasTU(String turnLanesPrevSegm, boolean attachedOnTheRight) {
      if (turnLanesPrevSegm != null) {
         int[] turns = calculateRawTurnLanes(turnLanesPrevSegm, 1);
         int lane = attachedOnTheRight ? turns[turns.length - 1] : turns[0];
         List<Integer> turnList = new ArrayList<>();
         turnList.add(TurnType.getPrimaryTurn(lane));
         turnList.add(TurnType.getSecondaryTurn(lane));
         turnList.add(TurnType.getTertiaryTurn(lane));
         if (attachedOnTheRight) {
            Collections.reverse(turnList);
         }

         return this.foundTUturn(turnList);
      } else {
         return false;
      }
   }

   private boolean foundTUturn(List<Integer> turnList) {
      for(int t : turnList) {
         if (t != 0) {
            return t == 10;
         }
      }

      return false;
   }

   protected TurnType createSimpleKeepLeftRightTurn(
      boolean leftSide, RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, RouteResultPreparation.RoadSplitStructure rs
   ) {
      double devation = Math.abs(MapUtils.degreesDiff((double)prevSegm.getBearingEnd(), (double)currentSegm.getBearingBegin()));
      boolean makeSlightTurn = devation > 5.0 && (!this.isMotorway(prevSegm) || !this.isMotorway(currentSegm));
      TurnType t = null;
      int laneType = 1;
      if (rs.keepLeft && rs.keepRight) {
         t = TurnType.valueOf(1, leftSide);
      } else if (rs.keepLeft) {
         t = TurnType.valueOf(makeSlightTurn ? 3 : 8, leftSide);
         if (makeSlightTurn) {
            laneType = 3;
         }
      } else {
         if (!rs.keepRight) {
            return null;
         }

         t = TurnType.valueOf(makeSlightTurn ? 6 : 9, leftSide);
         if (makeSlightTurn) {
            laneType = 6;
         }
      }

      int current = this.countLanesMinOne(currentSegm);
      int ls = current + rs.leftLanes + rs.rightLanes;
      int[] lanes = new int[ls];

      for(int it = 0; it < ls; ++it) {
         if (it >= rs.leftLanes && it < rs.leftLanes + current) {
            lanes[it] = (laneType << 1) + 1;
         } else {
            lanes[it] = 2;
         }
      }

      t.setSkipToSpeak(!rs.speak);
      t.setLanes(lanes);
      return t;
   }

   protected int countLanesMinOne(RouteSegmentResult attached) {
      boolean oneway = attached.getObject().getOneway() != 0;
      int lns = attached.getObject().getLanes();
      if (lns == 0) {
         String tls = getTurnLanesString(attached);
         if (tls != null) {
            return Math.max(1, this.countOccurrences(tls, (char)124));
         }
      }

      if (oneway) {
         return Math.max(1, lns);
      } else {
         try {
            if (attached.isForwardDirection() && attached.getObject().getValue("lanes:forward") != null) {
               return Integer.parseInt(attached.getObject().getValue("lanes:forward"));
            }

            if (!attached.isForwardDirection() && attached.getObject().getValue("lanes:backward") != null) {
               return Integer.parseInt(attached.getObject().getValue("lanes:backward"));
            }
         } catch (NumberFormatException var5) {
            var5.printStackTrace();
         }

         return Math.max(1, (lns + 1) / 2);
      }
   }

   protected static String getTurnLanesString(RouteSegmentResult segment) {
      if (segment.getObject().getOneway() == 0) {
         return segment.isForwardDirection() ? segment.getObject().getValue("turn:lanes:forward") : segment.getObject().getValue("turn:lanes:backward");
      } else {
         return segment.getObject().getValue("turn:lanes");
      }
   }

   private int countOccurrences(String haystack, char needle) {
      int count = 0;

      for(int i = 0; i < haystack.length(); ++i) {
         if (haystack.charAt(i) == needle) {
            ++count;
         }
      }

      return count;
   }

   public static int[] parseTurnLanes(RouteDataObject ro, double dirToNorthEastPi) {
      String turnLanes = null;
      if (ro.getOneway() == 0) {
         double cmp = ro.directionRoute(0, true);
         if (Math.abs(MapUtils.alignAngleDifference(dirToNorthEastPi - cmp)) < Math.PI / 2) {
            turnLanes = ro.getValue("turn:lanes:forward");
         } else {
            turnLanes = ro.getValue("turn:lanes:backward");
         }
      } else {
         turnLanes = ro.getValue("turn:lanes");
      }

      return turnLanes == null ? null : calculateRawTurnLanes(turnLanes, 0);
   }

   public static int[] parseLanes(RouteDataObject ro, double dirToNorthEastPi) {
      int lns = 0;

      try {
         if (ro.getOneway() == 0) {
            double cmp = ro.directionRoute(0, true);
            if (Math.abs(MapUtils.alignAngleDifference(dirToNorthEastPi - cmp)) < Math.PI / 2) {
               if (ro.getValue("lanes:forward") != null) {
                  lns = Integer.parseInt(ro.getValue("lanes:forward"));
               }
            } else if (ro.getValue("lanes:backward") != null) {
               lns = Integer.parseInt(ro.getValue("lanes:backward"));
            }

            if (lns == 0 && ro.getValue("lanes") != null) {
               lns = Integer.parseInt(ro.getValue("lanes")) / 2;
            }
         } else {
            lns = Integer.parseInt(ro.getValue("lanes"));
         }

         if (lns > 0) {
            return new int[lns];
         }
      } catch (NumberFormatException var6) {
      }

      return null;
   }

   public static int[] calculateRawTurnLanes(String turnLanes, int calcTurnType) {
      String[] splitLaneOptions = turnLanes.split("\\|", -1);
      int[] lanes = new int[splitLaneOptions.length];

      for(int i = 0; i < splitLaneOptions.length; ++i) {
         String[] laneOptions = splitLaneOptions[i].split(";");

         for(int j = 0; j < laneOptions.length; ++j) {
            int turn = TurnType.convertType(laneOptions[j]);
            int primary = TurnType.getPrimaryTurn(lanes[i]);
            if (primary == 0) {
               TurnType.setPrimaryTurnAndReset(lanes, i, turn);
            } else if (turn != calcTurnType
               && (!TurnType.isRightTurn(calcTurnType) || !TurnType.isRightTurn(turn))
               && (!TurnType.isLeftTurn(calcTurnType) || !TurnType.isLeftTurn(turn))) {
               if (TurnType.getSecondaryTurn(lanes[i]) == 0) {
                  TurnType.setSecondaryTurn(lanes, i, turn);
               } else if (TurnType.getTertiaryTurn(lanes[i]) == 0) {
                  TurnType.setTertiaryTurn(lanes, i, turn);
               }
            } else {
               TurnType.setPrimaryTurnShiftOthers(lanes, i, turn);
            }
         }
      }

      return lanes;
   }

   private int inferSlightTurnFromActiveLanes(int[] oLanes, boolean mostLeft, boolean mostRight) {
      Integer[] possibleTurns = this.getPossibleTurns(oLanes, false, false);
      if (possibleTurns.length == 0) {
         return 0;
      } else {
         int infer = 0;
         if (possibleTurns.length == 1) {
            infer = possibleTurns[0];
         } else if (possibleTurns.length == 2) {
            if (mostLeft && !mostRight) {
               infer = possibleTurns[0];
            } else if (mostRight && !mostLeft) {
               infer = possibleTurns[possibleTurns.length - 1];
            } else {
               infer = possibleTurns[1];
            }
         }

         return infer;
      }
   }

   private Integer[] getPossibleTurns(int[] oLanes, boolean onlyPrimary, boolean uniqueFromActive) {
      Set<Integer> possibleTurns = new LinkedHashSet<>();
      Set<Integer> upossibleTurns = new LinkedHashSet<>();

      for(int i = 0; i < oLanes.length; ++i) {
         upossibleTurns.clear();
         upossibleTurns.add(TurnType.getPrimaryTurn(oLanes[i]));
         if (!onlyPrimary && TurnType.getSecondaryTurn(oLanes[i]) != 0) {
            upossibleTurns.add(TurnType.getSecondaryTurn(oLanes[i]));
         }

         if (!onlyPrimary && TurnType.getTertiaryTurn(oLanes[i]) != 0) {
            upossibleTurns.add(TurnType.getTertiaryTurn(oLanes[i]));
         }

         if (!uniqueFromActive) {
            possibleTurns.addAll(upossibleTurns);
         } else if ((oLanes[i] & 1) == 1) {
            if (!possibleTurns.isEmpty()) {
               possibleTurns.retainAll(upossibleTurns);
               if (possibleTurns.isEmpty()) {
                  break;
               }
            } else {
               possibleTurns.addAll(upossibleTurns);
            }
         }
      }

      if (uniqueFromActive) {
         for(int i = 0; i < oLanes.length; ++i) {
            if ((oLanes[i] & 1) == 0) {
               possibleTurns.remove(TurnType.getPrimaryTurn(oLanes[i]));
               if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
                  possibleTurns.remove(TurnType.getSecondaryTurn(oLanes[i]));
               }

               if (TurnType.getTertiaryTurn(oLanes[i]) != 0) {
                  possibleTurns.remove(TurnType.getTertiaryTurn(oLanes[i]));
               }
            }
         }
      }

      Integer[] array = possibleTurns.toArray(new Integer[0]);
      Arrays.sort(array, new Comparator<Integer>() {
         public int compare(Integer o1, Integer o2) {
            return Integer.compare(TurnType.orderFromLeftToRight(o1), TurnType.orderFromLeftToRight(o2));
         }
      });
      return array;
   }

   private boolean isMotorway(RouteSegmentResult s) {
      String h = s.getObject().getHighway();
      return "motorway".equals(h) || "motorway_link".equals(h) || "trunk".equals(h) || "trunk_link".equals(h);
   }

   private void attachRoadSegments(RoutingContext ctx, List<RouteSegmentResult> result, int routeInd, int pointInd, boolean plus, boolean recalculation) throws IOException {
      RouteSegmentResult rr = result.get(routeInd);
      RouteDataObject road = rr.getObject();
      long nextL = pointInd < road.getPointsLength() - 1 ? this.getPoint(road, pointInd + 1) : 0L;
      long prevL = pointInd > 0 ? this.getPoint(road, pointInd - 1) : 0L;
      RouteSegmentResult previousResult = null;
      long previousRoadId = road.getId();
      if (pointInd == rr.getStartPointIndex() && routeInd > 0) {
         previousResult = result.get(routeInd - 1);
         previousRoadId = previousResult.getObject().getId();
         if (previousRoadId != road.getId()) {
            if (previousResult.getStartPointIndex() < previousResult.getEndPointIndex()
               && previousResult.getEndPointIndex() < previousResult.getObject().getPointsLength() - 1) {
               rr.attachRoute(
                  pointInd,
                  new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(), previousResult.getObject().getPointsLength() - 1)
               );
            } else if (previousResult.getStartPointIndex() > previousResult.getEndPointIndex() && previousResult.getEndPointIndex() > 0) {
               rr.attachRoute(pointInd, new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(), 0));
            }
         }
      }

      Iterator<BinaryRoutePlanner.RouteSegment> it;
      if (rr.getPreAttachedRoutes(pointInd) != null) {
         final RouteSegmentResult[] list = rr.getPreAttachedRoutes(pointInd);
         it = new Iterator<BinaryRoutePlanner.RouteSegment>() {
            int i = 0;

            @Override
            public boolean hasNext() {
               return this.i < list.length;
            }

            public BinaryRoutePlanner.RouteSegment next() {
               RouteSegmentResult r = list[this.i++];
               return new BinaryRoutePlanner.RouteSegment(r.getObject(), r.getStartPointIndex(), r.getEndPointIndex());
            }

            @Override
            public void remove() {
            }
         };
      } else if (!recalculation && ctx.nativeLib != null) {
         it = null;
      } else {
         BinaryRoutePlanner.RouteSegment rt = ctx.loadRouteSegment(road.getPoint31XTile(pointInd), road.getPoint31YTile(pointInd), ctx.config.memoryLimitation);
         it = rt == null ? null : rt.getIterator();
      }

      while(it != null && it.hasNext()) {
         BinaryRoutePlanner.RouteSegment routeSegment = it.next();
         if (routeSegment.road.getId() != road.getId() && routeSegment.road.getId() != previousRoadId) {
            RouteDataObject addRoad = routeSegment.road;
            this.checkAndInitRouteRegion(ctx, addRoad);
            int oneWay = ctx.getRouter().isOneWay(addRoad);
            if (oneWay >= 0 && routeSegment.getSegmentStart() < addRoad.getPointsLength() - 1) {
               long pointL = this.getPoint(addRoad, routeSegment.getSegmentStart() + 1);
               if (pointL != nextL && pointL != prevL) {
                  rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.getSegmentStart(), addRoad.getPointsLength() - 1));
               }
            }

            if (oneWay <= 0 && routeSegment.getSegmentStart() > 0) {
               long pointL = this.getPoint(addRoad, routeSegment.getSegmentStart() - 1);
               if (pointL != nextL && pointL != prevL) {
                  rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.getSegmentStart(), 0));
               }
            }
         }
      }
   }

   private static void println(String logMsg) {
      System.out.println(logMsg);
   }

   private long getPoint(RouteDataObject road, int pointInd) {
      return ((long)road.getPoint31XTile(pointInd) << 31) + (long)road.getPoint31YTile(pointInd);
   }

   private static double measuredDist(int x1, int y1, int x2, int y2) {
      return MapUtils.getDistance(MapUtils.get31LatitudeY(y1), MapUtils.get31LongitudeX(x1), MapUtils.get31LatitudeY(y2), MapUtils.get31LongitudeX(x2));
   }

   private static class CombineAreaRoutePoint {
      int x31;
      int y31;
      int originalIndex;

      private CombineAreaRoutePoint() {
      }
   }

   private class MergeTurnLaneTurn {
      TurnType turn;
      int[] originalLanes;
      int[] disabledLanes;
      int activeStartIndex = -1;
      int activeEndIndex = -1;
      int activeLen = 0;

      public MergeTurnLaneTurn(RouteSegmentResult segment) {
         this.turn = segment.getTurnType();
         if (this.turn != null) {
            this.originalLanes = this.turn.getLanes();
         }

         if (this.originalLanes != null) {
            this.disabledLanes = new int[this.originalLanes.length];

            for(int i = 0; i < this.originalLanes.length; ++i) {
               int ln = this.originalLanes[i];
               this.disabledLanes[i] = ln & -2;
               if ((ln & 1) > 0) {
                  if (this.activeStartIndex == -1) {
                     this.activeStartIndex = i;
                  }

                  this.activeEndIndex = i;
                  ++this.activeLen;
               }
            }
         }
      }

      public boolean isActiveTurnMostLeft() {
         return this.activeStartIndex == 0;
      }

      public boolean isActiveTurnMostRight() {
         return this.activeEndIndex == this.originalLanes.length - 1;
      }
   }

   private class RoadSplitStructure {
      boolean keepLeft = false;
      boolean keepRight = false;
      boolean speak = false;
      List<int[]> leftLanesInfo = new ArrayList<>();
      int leftLanes = 0;
      List<int[]> rightLanesInfo = new ArrayList<>();
      int rightLanes = 0;
      int roadsOnLeft = 0;
      int addRoadsOnLeft = 0;
      int roadsOnRight = 0;
      int addRoadsOnRight = 0;

      private RoadSplitStructure() {
      }
   }
}
