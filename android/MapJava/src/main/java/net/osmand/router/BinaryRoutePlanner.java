package net.osmand.router;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.errors.RouteCalculationError;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

public class BinaryRoutePlanner {
   private static final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
   static final int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 220;
   static final int STANDARD_ROAD_VISITED_OVERHEAD = 150;
   protected static final Log log = PlatformUtil.getLog(BinaryRoutePlanner.class);
   private static final int ROUTE_POINTS = 11;
   private static final boolean ASSERT_CHECKS = true;
   private static final boolean TRACE_ROUTING = false;
   private static final int TEST_ID = 50725;
   private static final boolean TEST_SPECIFIC = true;

   public static double squareRootDist(int x1, int y1, int x2, int y2) {
      return MapUtils.squareRootDist31(x1, y1, x2, y2);
   }

   BinaryRoutePlanner.FinalRouteSegment searchRouteInternal(
      RoutingContext ctx,
      BinaryRoutePlanner.RouteSegmentPoint start,
      BinaryRoutePlanner.RouteSegmentPoint end,
      BinaryRoutePlanner.RouteSegment recalculationEnd
   ) throws InterruptedException, IOException {
      ctx.memoryOverhead = 1000;
      Comparator<BinaryRoutePlanner.RouteSegment> nonHeuristicSegmentsComparator = new BinaryRoutePlanner.NonHeuristicSegmentsComparator();
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphDirectSegments = new PriorityQueue<>(50, new BinaryRoutePlanner.SegmentsComparator(ctx));
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphReverseSegments = new PriorityQueue<>(50, new BinaryRoutePlanner.SegmentsComparator(ctx));
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedDirectSegments = new TLongObjectHashMap();
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedOppositeSegments = new TLongObjectHashMap();
      this.initQueuesWithStartEnd(ctx, start, end, recalculationEnd, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
      BinaryRoutePlanner.FinalRouteSegment finalSegment = null;
      boolean onlyBackward = ctx.getPlanRoadDirection() < 0;
      boolean onlyForward = ctx.getPlanRoadDirection() > 0;
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments = onlyForward ? graphReverseSegments : graphDirectSegments;
      boolean forwardSearch = !onlyForward;

      while(!graphSegments.isEmpty()) {
         BinaryRoutePlanner.RouteSegment segment = graphSegments.poll();
         ctx.memoryOverhead = (visitedDirectSegments.size() + visitedOppositeSegments.size()) * 150
            + (graphDirectSegments.size() + graphReverseSegments.size()) * 220;
         if (segment instanceof BinaryRoutePlanner.FinalRouteSegment) {
            if (RoutingContext.SHOW_GC_SIZE) {
               log.warn("Estimated overhead " + ctx.memoryOverhead / 1048576 + " mb");
               this.printMemoryConsumption("Memory occupied after calculation : ");
            }

            finalSegment = (BinaryRoutePlanner.FinalRouteSegment)segment;
            break;
         }

         if ((double)ctx.memoryOverhead > (double)ctx.config.memoryLimitation * 0.95 && RoutingContext.SHOW_GC_SIZE) {
            this.printMemoryConsumption("Memory occupied before exception : ");
         }

         if ((double)ctx.memoryOverhead > (double)ctx.config.memoryLimitation * 0.95) {
            throw new RouteCalculationError.RouteIsTooComplex("There is not enough memory " + ctx.config.memoryLimitation / 1048576L + " Mb");
         }

         if (ctx.calculationProgress != null) {
            ++ctx.calculationProgress.visitedSegments;
         }

         if (forwardSearch) {
            this.processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, segment, visitedOppositeSegments, onlyBackward);
         } else {
            this.processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, segment, visitedDirectSegments, onlyForward);
         }

         this.updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments);
         this.checkIfGraphIsEmpty(
            ctx, ctx.getPlanRoadDirection() <= 0, true, graphReverseSegments, end, visitedOppositeSegments, "Route is not found to selected target point."
         );
         this.checkIfGraphIsEmpty(
            ctx, ctx.getPlanRoadDirection() >= 0, false, graphDirectSegments, start, visitedDirectSegments, "Route is not found from selected start point."
         );
         if (ctx.planRouteIn2Directions()) {
            if (graphDirectSegments.isEmpty() || graphReverseSegments.isEmpty()) {
               break;
            }

            forwardSearch = nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) <= 0;
         } else {
            forwardSearch = onlyForward;
            if (onlyBackward && !graphDirectSegments.isEmpty()) {
               forwardSearch = true;
            }

            if (onlyForward && !graphReverseSegments.isEmpty()) {
               forwardSearch = false;
            }
         }

         if (forwardSearch) {
            graphSegments = graphDirectSegments;
         } else {
            graphSegments = graphReverseSegments;
         }

         if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
            throw new InterruptedException("Route calculation interrupted");
         }
      }

      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.visitedDirectSegments += visitedDirectSegments.size();
         ctx.calculationProgress.visitedOppositeSegments += visitedOppositeSegments.size();
         ctx.calculationProgress.directQueueSize += graphDirectSegments.size();
         ctx.calculationProgress.oppositeQueueSize += graphReverseSegments.size();
      }

      return finalSegment;
   }

   protected void checkIfGraphIsEmpty(
      RoutingContext ctx,
      boolean allowDirection,
      boolean reverseWaySearch,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments,
      BinaryRoutePlanner.RouteSegmentPoint pnt,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visited,
      String msg
   ) {
      if (allowDirection && graphSegments.isEmpty() && pnt.others != null) {
         Iterator<BinaryRoutePlanner.RouteSegmentPoint> pntIterator = pnt.others.iterator();

         while(pntIterator.hasNext()) {
            BinaryRoutePlanner.RouteSegmentPoint next = pntIterator.next();
            pntIterator.remove();
            float estimatedDistance = this.estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX, ctx.startY);
            BinaryRoutePlanner.RouteSegment pos = next.initRouteSegment(true);
            if (pos != null && !visited.containsKey(this.calculateRoutePointId(pos)) && this.checkMovementAllowed(ctx, reverseWaySearch, pos)) {
               pos.setParentRoute(null);
               pos.distanceFromStart = 0.0F;
               pos.distanceToEnd = estimatedDistance;
               graphSegments.add(pos);
            }

            BinaryRoutePlanner.RouteSegment neg = next.initRouteSegment(false);
            if (neg != null && !visited.containsKey(this.calculateRoutePointId(neg)) && this.checkMovementAllowed(ctx, reverseWaySearch, neg)) {
               neg.setParentRoute(null);
               neg.distanceFromStart = 0.0F;
               neg.distanceToEnd = estimatedDistance;
               graphSegments.add(neg);
            }

            if (!graphSegments.isEmpty()) {
               println("Reiterate point with new " + (!reverseWaySearch ? "start " : "destination ") + next.getRoad());
               break;
            }
         }

         if (graphSegments.isEmpty()) {
            throw new RouteCalculationError.RouteNotFound(msg);
         }
      }
   }

   public BinaryRoutePlanner.RouteSegment initRouteSegment(
      RoutingContext ctx, BinaryRoutePlanner.RouteSegment segment, boolean positiveDirection, boolean reverseSearchWay
   ) {
      if (segment.getSegmentStart() == 0 && !positiveDirection && segment.getRoad().getPointsLength() > 0) {
         segment = this.loadSameSegment(ctx, segment, 1, reverseSearchWay);
      } else if (segment.getSegmentStart() > 0 && positiveDirection) {
         segment = this.loadSameSegment(ctx, segment, segment.getSegmentStart() - 1, reverseSearchWay);
      }

      BinaryRoutePlanner.RouteSegment initSegment = null;
      if (segment != null) {
         initSegment = segment.initRouteSegment(positiveDirection);
      }

      if (initSegment != null) {
         initSegment.setParentRoute(BinaryRoutePlanner.RouteSegment.NULL);
         initSegment.distanceFromStart = (float)((double)initSegment.distanceFromStart + this.initDistFromStart(ctx, initSegment, reverseSearchWay));
      }

      return initSegment;
   }

   private double initDistFromStart(RoutingContext ctx, BinaryRoutePlanner.RouteSegment initSegment, boolean reverseSearchWay) {
      int prevX = initSegment.road.getPoint31XTile(initSegment.getSegmentStart());
      int prevY = initSegment.road.getPoint31YTile(initSegment.getSegmentStart());
      int x = initSegment.road.getPoint31XTile(initSegment.getSegmentEnd());
      int y = initSegment.road.getPoint31YTile(initSegment.getSegmentEnd());
      float priority = ctx.getRouter().defineSpeedPriority(initSegment.road);
      float speed = ctx.getRouter().defineRoutingSpeed(initSegment.road) * priority;
      if (speed == 0.0F) {
         speed = ctx.getRouter().getDefaultSpeed() * priority;
      }

      if (speed > ctx.getRouter().getMaxSpeed()) {
         speed = ctx.getRouter().getMaxSpeed();
      }

      double fullDist = squareRootDist(prevX, prevY, x, y);
      double distFromStart = squareRootDist(x, y, !reverseSearchWay ? ctx.startX : ctx.targetX, !reverseSearchWay ? ctx.startY : ctx.targetY);
      return (distFromStart - fullDist) / (double)speed;
   }

   protected BinaryRoutePlanner.RouteSegment loadSameSegment(RoutingContext ctx, BinaryRoutePlanner.RouteSegment segment, int ind, boolean reverseSearchWay) {
      int x31 = segment.getRoad().getPoint31XTile(ind);
      int y31 = segment.getRoad().getPoint31YTile(ind);

      for(BinaryRoutePlanner.RouteSegment s = ctx.loadRouteSegment(x31, y31, 0L, reverseSearchWay); s != null; s = s.getNext()) {
         if (s.getRoad().getId() == segment.getRoad().getId()) {
            segment = s;
            break;
         }
      }

      return segment;
   }

   private void initQueuesWithStartEnd(
      RoutingContext ctx,
      BinaryRoutePlanner.RouteSegmentPoint start,
      BinaryRoutePlanner.RouteSegmentPoint end,
      BinaryRoutePlanner.RouteSegment recalculationEnd,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphDirectSegments,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphReverseSegments,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedDirectSegments,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedOppositeSegments
   ) {
      BinaryRoutePlanner.RouteSegment startPos = this.initRouteSegment(ctx, start, true, false);
      BinaryRoutePlanner.RouteSegment startNeg = this.initRouteSegment(ctx, start, false, false);
      BinaryRoutePlanner.RouteSegment endPos = this.initRouteSegment(ctx, end, true, true);
      BinaryRoutePlanner.RouteSegment endNeg = this.initRouteSegment(ctx, end, false, true);
      if (ctx.config.initialDirection != null) {
         double plusDir = start.getRoad().directionRoute(start.getSegmentStart(), true);
         double diff = plusDir - ctx.config.initialDirection;
         if (Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
            if (startNeg != null) {
               startNeg.distanceFromStart += 500.0F;
            }
         } else if (Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3 && startPos != null) {
            startPos.distanceFromStart += 500.0F;
         }
      }

      if (recalculationEnd != null) {
         ctx.targetX = recalculationEnd.getRoad().getPoint31XTile(recalculationEnd.getSegmentStart());
         ctx.targetY = recalculationEnd.getRoad().getPoint31YTile(recalculationEnd.getSegmentStart());
      }

      float estimatedDistance = this.estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX, ctx.startY);
      if (startPos != null && this.checkMovementAllowed(ctx, false, startPos)) {
         startPos.distanceToEnd = estimatedDistance;
         graphDirectSegments.add(startPos);
      }

      if (startNeg != null && this.checkMovementAllowed(ctx, false, startNeg)) {
         startNeg.distanceToEnd = estimatedDistance;
         graphDirectSegments.add(startNeg);
      }

      if (recalculationEnd != null) {
         graphReverseSegments.add(recalculationEnd);
      } else {
         if (endPos != null && this.checkMovementAllowed(ctx, true, endPos)) {
            endPos.distanceToEnd = estimatedDistance;
            graphReverseSegments.add(endPos);
         }

         if (endNeg != null && this.checkMovementAllowed(ctx, true, endNeg)) {
            endNeg.distanceToEnd = estimatedDistance;
            graphReverseSegments.add(endNeg);
         }
      }
   }

   private void printMemoryConsumption(String string) {
      long h1 = RoutingContext.runGCUsedMemory();
      float mb = 1048576.0F;
      log.warn(string + (float)h1 / mb);
   }

   private void updateCalculationProgress(
      RoutingContext ctx,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphDirectSegments,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphReverseSegments
   ) {
      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.reverseSegmentQueueSize = graphReverseSegments.size();
         ctx.calculationProgress.directSegmentQueueSize = graphDirectSegments.size();
         if (graphDirectSegments.size() > 0 && ctx.getPlanRoadDirection() >= 0) {
            BinaryRoutePlanner.RouteSegment peek = graphDirectSegments.peek();
            ctx.calculationProgress.distanceFromBegin = Math.max(peek.distanceFromStart, ctx.calculationProgress.distanceFromBegin);
            ctx.calculationProgress.directDistance = peek.distanceFromStart + peek.distanceToEnd;
         }

         if (graphReverseSegments.size() > 0 && ctx.getPlanRoadDirection() <= 0) {
            BinaryRoutePlanner.RouteSegment peek = graphReverseSegments.peek();
            ctx.calculationProgress.distanceFromEnd = Math.max(peek.distanceFromStart + peek.distanceToEnd, ctx.calculationProgress.distanceFromEnd);
            ctx.calculationProgress.reverseDistance = peek.distanceFromStart + peek.distanceToEnd;
         }
      }
   }

   private void printRoad(String prefix, BinaryRoutePlanner.RouteSegment segment, Boolean reverseWaySearch) {
      String p = "";
      if (reverseWaySearch != null) {
         p = reverseWaySearch ? "B" : "F";
      }

      if (segment == null) {
         println(p + prefix + " Segment=null");
      } else {
         String pr;
         if (segment.parentRoute != null) {
            pr = " pend=" + segment.parentRoute.segEnd + " parent=" + segment.parentRoute.road;
         } else {
            pr = "";
         }

         println(
            p
               + prefix
               + ""
               + segment.road
               + " ind="
               + segment.getSegmentStart()
               + "->"
               + segment.getSegmentEnd()
               + " ds="
               + segment.distanceFromStart
               + " es="
               + segment.distanceToEnd
               + pr
         );
      }
   }

   private float estimatedDistance(RoutingContext ctx, int targetEndX, int targetEndY, int startX, int startY) {
      double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
      return (float)(distance / (double)ctx.getRouter().getMaxSpeed());
   }

   protected static float h(RoutingContext ctx, int begX, int begY, int endX, int endY) {
      double distToFinalPoint = squareRootDist(begX, begY, endX, endY);
      double result = distToFinalPoint / (double)ctx.getRouter().getMaxSpeed();
      if (ctx.precalculatedRouteDirection != null) {
         float te = ctx.precalculatedRouteDirection.timeEstimate(begX, begY, endX, endY);
         if (te > 0.0F) {
            return te;
         }
      }

      return (float)result;
   }

   private static void println(String logMsg) {
      System.out.println(logMsg);
   }

   private double calculateRouteSegmentTime(RoutingContext ctx, boolean reverseWaySearch, BinaryRoutePlanner.RouteSegment segment) {
      RouteDataObject road = segment.road;
      short segmentInd = !reverseWaySearch ? segment.getSegmentStart() : segment.getSegmentEnd();
      short prevSegmentInd = reverseWaySearch ? segment.getSegmentStart() : segment.getSegmentEnd();
      int x = road.getPoint31XTile(segmentInd);
      int y = road.getPoint31YTile(segmentInd);
      int prevX = road.getPoint31XTile(prevSegmentInd);
      int prevY = road.getPoint31YTile(prevSegmentInd);
      double distOnRoadToPass = squareRootDist(x, y, prevX, prevY);
      float priority = ctx.getRouter().defineSpeedPriority(road);
      float speed = ctx.getRouter().defineRoutingSpeed(road) * priority;
      if (speed == 0.0F) {
         speed = ctx.getRouter().getDefaultSpeed() * priority;
      }

      if (speed > ctx.getRouter().getMaxSpeed()) {
         speed = ctx.getRouter().getMaxSpeed();
      }

      double obstacle = (double)ctx.getRouter().defineRoutingObstacle(road, segmentInd, prevSegmentInd > segmentInd);
      if (obstacle < 0.0) {
         return -1.0;
      } else {
         double heightObstacle = ctx.getRouter().defineHeightObstacle(road, segmentInd, prevSegmentInd);
         return heightObstacle < 0.0 ? -1.0 : obstacle + heightObstacle + distOnRoadToPass / (double)speed;
      }
   }

   private void processRouteSegment(
      RoutingContext ctx,
      boolean reverseWaySearch,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedSegments,
      BinaryRoutePlanner.RouteSegment startSegment,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> oppositeSegments,
      boolean doNotAddIntersections
   ) {
      if (!this.checkMovementAllowed(ctx, reverseWaySearch, startSegment)) {
         throw new IllegalStateException();
      } else {
         RouteDataObject road = startSegment.getRoad();
         if (road.getId() >> 6 == 50725L) {
            this.printRoad(" ! " + startSegment.distanceFromStart + " ", startSegment, reverseWaySearch);
         }

         boolean directionAllowed = true;
         BinaryRoutePlanner.RouteSegment nextCurrentSegment = startSegment;

         BinaryRoutePlanner.RouteSegment currentSegment;
         for(currentSegment = null;
            nextCurrentSegment != null;
            nextCurrentSegment = this.processIntersections(ctx, graphSegments, visitedSegments, nextCurrentSegment, reverseWaySearch, doNotAddIntersections)
         ) {
            currentSegment = nextCurrentSegment;
            BinaryRoutePlanner.RouteSegment var20 = null;
            float segmentAndObstaclesTime = (float)this.calculateRouteSegmentTime(ctx, reverseWaySearch, nextCurrentSegment);
            if (segmentAndObstaclesTime < 0.0F) {
               directionAllowed = false;
               break;
            }

            float distFromStartPlusSegmentTime = nextCurrentSegment.distanceFromStart + segmentAndObstaclesTime;
            boolean alreadyVisited = this.checkIfOppositeSegmentWasVisited(reverseWaySearch, graphSegments, nextCurrentSegment, oppositeSegments);
            if (alreadyVisited) {
            }

            long nextPntId = this.calculateRoutePointId(nextCurrentSegment);
            BinaryRoutePlanner.RouteSegment existingSegment = (BinaryRoutePlanner.RouteSegment)visitedSegments.put(nextPntId, nextCurrentSegment);
            if (existingSegment != null) {
               if (distFromStartPlusSegmentTime > existingSegment.distanceFromStart) {
                  visitedSegments.put(nextPntId, existingSegment);
                  directionAllowed = false;
                  break;
               }

               if (ctx.config.heuristicCoefficient <= 1.0F) {
                  if (RoutingContext.PRINT_ROUTING_ALERTS) {
                     System.err
                        .println(
                           "! ALERT slower segment was visited earlier "
                              + distFromStartPlusSegmentTime
                              + " > "
                              + existingSegment.distanceFromStart
                              + ": "
                              + nextCurrentSegment
                              + " - "
                              + existingSegment
                        );
                  } else {
                     ++ctx.alertSlowerSegmentedWasVisitedEarlier;
                  }
               }
            }

            nextCurrentSegment.distanceFromStart = distFromStartPlusSegmentTime;
         }

         if (ctx.visitor != null) {
            ctx.visitor.visitSegment(startSegment, currentSegment.getSegmentEnd(), true);
         }
      }
   }

   private boolean checkMovementAllowed(RoutingContext ctx, boolean reverseWaySearch, BinaryRoutePlanner.RouteSegment segment) {
      int oneway = ctx.getRouter().isOneWay(segment.getRoad());
      boolean directionAllowed;
      if (!reverseWaySearch) {
         if (segment.isPositive()) {
            directionAllowed = oneway >= 0;
         } else {
            directionAllowed = oneway <= 0;
         }
      } else if (segment.isPositive()) {
         directionAllowed = oneway <= 0;
      } else {
         directionAllowed = oneway >= 0;
      }

      return directionAllowed;
   }

   private boolean checkViaRestrictions(BinaryRoutePlanner.RouteSegment from, BinaryRoutePlanner.RouteSegment to) {
      if (from != null && to != null) {
         long fid = to.getRoad().getId();

         for(int i = 0; i < from.getRoad().getRestrictionLength(); ++i) {
            long id = from.getRoad().getRestrictionId(i);
            int tp = from.getRoad().getRestrictionType(i);
            if (fid == id) {
               if (tp == 2 || tp == 1 || tp == 4 || tp == 3) {
                  return false;
               }
               break;
            }

            if (tp == 7) {
               return false;
            }
         }
      }

      return true;
   }

   private BinaryRoutePlanner.RouteSegment getParentDiffId(BinaryRoutePlanner.RouteSegment s) {
      while(s.getParentRoute() != null && s.getParentRoute().getRoad().getId() == s.getRoad().getId()) {
         s = s.getParentRoute();
      }

      return s.getParentRoute();
   }

   private boolean checkIfOppositeSegmentWasVisited(
      boolean reverseWaySearch,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments,
      BinaryRoutePlanner.RouteSegment currentSegment,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> oppositeSegments
   ) {
      long currPoint = this.calculateRoutePointInternalId(currentSegment.getRoad(), currentSegment.getSegmentEnd(), currentSegment.getSegmentStart());
      if (oppositeSegments.containsKey(currPoint)) {
         BinaryRoutePlanner.RouteSegment opposite = (BinaryRoutePlanner.RouteSegment)oppositeSegments.get(currPoint);
         BinaryRoutePlanner.RouteSegment curParent = this.getParentDiffId(currentSegment);
         BinaryRoutePlanner.RouteSegment oppParent = this.getParentDiffId(opposite);
         BinaryRoutePlanner.RouteSegment to = reverseWaySearch ? curParent : oppParent;
         BinaryRoutePlanner.RouteSegment from = !reverseWaySearch ? curParent : oppParent;
         if (this.checkViaRestrictions(from, to)) {
            BinaryRoutePlanner.FinalRouteSegment frs = new BinaryRoutePlanner.FinalRouteSegment(
               currentSegment.getRoad(), currentSegment.getSegmentStart(), currentSegment.getSegmentEnd()
            );
            frs.setParentRoute(currentSegment.getParentRoute());
            frs.reverseWaySearch = reverseWaySearch;
            frs.distanceFromStart = opposite.distanceFromStart + currentSegment.distanceFromStart;
            frs.distanceToEnd = 0.0F;
            frs.opposite = opposite;
            graphSegments.add(frs);
            return true;
         }
      }

      return false;
   }

   private long calculateRoutePointInternalId(RouteDataObject road, int pntId, int nextPntId) {
      int positive = nextPntId - pntId;
      int pntLen = road.getPointsLength();
      if (pntId >= 0 && nextPntId >= 0 && pntId < pntLen && nextPntId < pntLen && (positive == -1 || positive == 1)) {
         return (road.getId() << 11) + (long)(pntId << 1) + (long)(positive > 0 ? 1 : 0);
      } else {
         throw new IllegalStateException("Assert failed");
      }
   }

   private long calculateRoutePointId(BinaryRoutePlanner.RouteSegment segm) {
      return this.calculateRoutePointInternalId(
         segm.getRoad(), segm.getSegmentStart(), segm.isPositive() ? segm.getSegmentStart() + 1 : segm.getSegmentStart() - 1
      );
   }

   private boolean proccessRestrictions(
      RoutingContext ctx, BinaryRoutePlanner.RouteSegment segment, BinaryRoutePlanner.RouteSegment inputNext, boolean reverseWay
   ) {
      if (!ctx.getRouter().restrictionsAware()) {
         return false;
      } else {
         RouteDataObject road = segment.getRoad();
         BinaryRoutePlanner.RouteSegment parent = this.getParentDiffId(segment);
         if (reverseWay || road.getRestrictionLength() != 0 || parent != null && parent.getRoad().getRestrictionLength() != 0) {
            ctx.segmentsToVisitPrescripted.clear();
            ctx.segmentsToVisitNotForbidden.clear();
            this.processRestriction(ctx, inputNext, reverseWay, 0L, road);
            if (parent != null) {
               this.processRestriction(ctx, inputNext, reverseWay, road.id, parent.getRoad());
            }

            return true;
         } else {
            return false;
         }
      }
   }

   protected void processRestriction(RoutingContext ctx, BinaryRoutePlanner.RouteSegment inputNext, boolean reverseWay, long viaId, RouteDataObject road) {
      boolean via = viaId != 0L;
      BinaryRoutePlanner.RouteSegment next = inputNext;

      for(boolean exclusiveRestriction = false; next != null; next = next.next) {
         int type = -1;
         if (!reverseWay) {
            for(int i = 0; i < road.getRestrictionLength(); ++i) {
               int rt = road.getRestrictionType(i);
               long rv = road.getRestrictionVia(i);
               if (road.getRestrictionId(i) == next.road.id && (!via || rv == viaId)) {
                  type = rt;
                  break;
               }

               if (rv == viaId && via && rt == 7) {
                  type = 4;
                  break;
               }
            }
         } else {
            for(int i = 0; i < next.road.getRestrictionLength(); ++i) {
               int rt = next.road.getRestrictionType(i);
               long rv = next.road.getRestrictionVia(i);
               long restrictedTo = next.road.getRestrictionId(i);
               if (restrictedTo == road.id && (!via || rv == viaId)) {
                  type = rt;
                  break;
               }

               if (rv == viaId && via && rt == 7) {
                  type = 4;
                  break;
               }

               if (rt == 5 || rt == 6 || rt == 7) {
                  BinaryRoutePlanner.RouteSegment foundNext = inputNext;

                  while(foundNext != null && foundNext.getRoad().id != restrictedTo) {
                     foundNext = foundNext.next;
                  }

                  if (foundNext != null) {
                     type = 1024;
                  }
               }
            }
         }

         if (type != 1024 && (type != -1 || !exclusiveRestriction)) {
            if (type != 2 && type != 1 && type != 4 && type != 3) {
               if (type == -1) {
                  ctx.segmentsToVisitNotForbidden.add(next);
               } else if (!via) {
                  if (!reverseWay) {
                     exclusiveRestriction = true;
                     ctx.segmentsToVisitNotForbidden.clear();
                     ctx.segmentsToVisitPrescripted.add(next);
                  } else {
                     ctx.segmentsToVisitNotForbidden.add(next);
                  }
               }
            } else if (via) {
               ctx.segmentsToVisitPrescripted.remove(next);
            }
         }
      }

      if (!via) {
         ctx.segmentsToVisitPrescripted.addAll(ctx.segmentsToVisitNotForbidden);
      }
   }

   private BinaryRoutePlanner.RouteSegment processIntersections(
      RoutingContext ctx,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedSegments,
      BinaryRoutePlanner.RouteSegment currentSegment,
      boolean reverseWaySearch,
      boolean doNotAddIntersections
   ) {
      BinaryRoutePlanner.RouteSegment nextCurrentSegment = null;
      int targetEndX = reverseWaySearch ? ctx.startX : ctx.targetX;
      int targetEndY = reverseWaySearch ? ctx.startY : ctx.targetY;
      int x = currentSegment.getRoad().getPoint31XTile(currentSegment.getSegmentEnd());
      int y = currentSegment.getRoad().getPoint31YTile(currentSegment.getSegmentEnd());
      float distanceToEnd = h(ctx, x, y, targetEndX, targetEndY);
      currentSegment.distanceToEnd = distanceToEnd;
      BinaryRoutePlanner.RouteSegment connectedNextSegment = ctx.loadRouteSegment(
         x, y, ctx.config.memoryLimitation - (long)ctx.memoryOverhead, reverseWaySearch
      );
      BinaryRoutePlanner.RouteSegment roadIter = connectedNextSegment;
      boolean directionAllowed = true;

      boolean singleRoad;
      for(singleRoad = true; roadIter != null; roadIter = roadIter.getNext()) {
         if (currentSegment.getSegmentEnd() == roadIter.getSegmentStart() && roadIter.road.getId() == currentSegment.getRoad().getId()) {
            nextCurrentSegment = roadIter.initRouteSegment(currentSegment.isPositive());
            if (nextCurrentSegment == null) {
               directionAllowed = false;
            } else if (nextCurrentSegment.isSegmentAttachedToStart()) {
               directionAllowed = this.processOneRoadIntersection(ctx, reverseWaySearch, null, visitedSegments, currentSegment, nextCurrentSegment);
            } else {
               nextCurrentSegment.setParentRoute(currentSegment);
               nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart;
               nextCurrentSegment.distanceToEnd = distanceToEnd;
               int nx = nextCurrentSegment.getRoad().getPoint31XTile(nextCurrentSegment.getSegmentEnd());
               int ny = nextCurrentSegment.getRoad().getPoint31YTile(nextCurrentSegment.getSegmentEnd());
               if (nx == x && ny == y) {
                  return nextCurrentSegment;
               }
            }
         } else {
            singleRoad = false;
         }
      }

      if (singleRoad) {
         return nextCurrentSegment;
      } else {
         Iterator<BinaryRoutePlanner.RouteSegment> nextIterator = null;
         boolean thereAreRestrictions = this.proccessRestrictions(ctx, currentSegment, connectedNextSegment, reverseWaySearch);
         if (thereAreRestrictions) {
            nextIterator = ctx.segmentsToVisitPrescripted.iterator();
         }

         BinaryRoutePlanner.RouteSegment next = connectedNextSegment;
         boolean hasNext = nextIterator != null ? nextIterator.hasNext() : connectedNextSegment != null;

         while(hasNext) {
            if (nextIterator != null) {
               next = nextIterator.next();
            }

            if ((next.getSegmentStart() != currentSegment.getSegmentEnd() || next.getRoad().getId() != currentSegment.getRoad().getId())
               && !doNotAddIntersections) {
               BinaryRoutePlanner.RouteSegment nextPos = next.initRouteSegment(true);
               this.processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextPos);
               BinaryRoutePlanner.RouteSegment nextNeg = next.initRouteSegment(false);
               this.processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextNeg);
            }

            if (nextIterator == null) {
               next = next.next;
               hasNext = next != null;
            } else {
               hasNext = nextIterator.hasNext();
            }
         }

         if (nextCurrentSegment == null && directionAllowed && ctx.calculationMode == RoutePlannerFrontEnd.RouteCalculationMode.BASE) {
            int newEnd = currentSegment.getSegmentEnd() + (currentSegment.isPositive() ? 1 : -1);
            if (newEnd >= 0 && newEnd < currentSegment.getRoad().getPointsLength() - 1) {
               nextCurrentSegment = new BinaryRoutePlanner.RouteSegment(currentSegment.getRoad(), currentSegment.getSegmentEnd(), newEnd);
               nextCurrentSegment.setParentRoute(currentSegment);
               nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart;
               nextCurrentSegment.distanceToEnd = distanceToEnd;
            }
         }

         return nextCurrentSegment;
      }
   }

   private boolean processOneRoadIntersection(
      RoutingContext ctx,
      boolean reverseWaySearch,
      PriorityQueue<BinaryRoutePlanner.RouteSegment> graphSegments,
      TLongObjectHashMap<BinaryRoutePlanner.RouteSegment> visitedSegments,
      BinaryRoutePlanner.RouteSegment segment,
      BinaryRoutePlanner.RouteSegment next
   ) {
      if (next != null) {
         if (!this.checkMovementAllowed(ctx, reverseWaySearch, next)) {
            return false;
         }

         float obstaclesTime = (float)ctx.getRouter()
            .calculateTurnTime(next, next.isPositive() ? next.getRoad().getPointsLength() - 1 : 0, segment, segment.getSegmentEnd());
         if (obstaclesTime < 0.0F) {
            return false;
         }

         float distFromStart = obstaclesTime + segment.distanceFromStart;
         if (next.road.getId() >> 6 == 50725L) {
            this.printRoad(
               " !? distFromStart="
                  + distFromStart
                  + " from "
                  + segment.getRoad().getId()
                  + " distToEnd="
                  + segment.distanceFromStart
                  + " segmentPoint="
                  + segment.getSegmentEnd()
                  + " -- ",
               next,
               null
            );
         }

         BinaryRoutePlanner.RouteSegment visIt = (BinaryRoutePlanner.RouteSegment)visitedSegments.get(this.calculateRoutePointId(next));
         boolean toAdd = true;
         if (visIt != null) {
            toAdd = false;
            if (distFromStart < visIt.distanceFromStart) {
               double routeSegmentTime = this.calculateRouteSegmentTime(ctx, reverseWaySearch, visIt);
               if ((double)distFromStart + routeSegmentTime < (double)visIt.distanceFromStart) {
                  toAdd = true;
                  if (ctx.config.heuristicCoefficient <= 1.0F) {
                     if (RoutingContext.PRINT_ROUTING_ALERTS) {
                        System.err
                           .println(
                              "! ALERT new faster path to a visited segment: "
                                 + ((double)distFromStart + routeSegmentTime)
                                 + " < "
                                 + visIt.distanceFromStart
                                 + ": "
                                 + next
                                 + " - "
                                 + visIt
                           );
                     } else {
                        ++ctx.alertFasterRoadToVisitedSegments;
                     }
                  }
               }
            }
         }

         if (toAdd
            && (
               !next.isSegmentAttachedToStart()
                  || ctx.roadPriorityComparator(
                        (double)next.distanceFromStart, (double)next.distanceToEnd, (double)distFromStart, (double)segment.distanceToEnd
                     )
                     > 0
            )) {
            next.distanceFromStart = distFromStart;
            next.distanceToEnd = segment.distanceToEnd;
            next.setParentRoute(segment);
            if (graphSegments != null) {
               graphSegments.add(next);
            }

            return true;
         }
      }

      return false;
   }

   static int roadPriorityComparator(
      double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient
   ) {
      return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, o2DistanceFromStart + heuristicCoefficient * o2DistanceToEnd);
   }

   static class FinalRouteSegment extends BinaryRoutePlanner.RouteSegment {
      boolean reverseWaySearch;
      BinaryRoutePlanner.RouteSegment opposite;

      public FinalRouteSegment(RouteDataObject road, int segmentStart, int segmentEnd) {
         super(road, segmentStart, segmentEnd);
      }
   }

   private static class NonHeuristicSegmentsComparator implements Comparator<BinaryRoutePlanner.RouteSegment> {
      public NonHeuristicSegmentsComparator() {
      }

      public int compare(BinaryRoutePlanner.RouteSegment o1, BinaryRoutePlanner.RouteSegment o2) {
         return BinaryRoutePlanner.roadPriorityComparator(
            (double)o1.distanceFromStart, (double)o1.distanceToEnd, (double)o2.distanceFromStart, (double)o2.distanceToEnd, 0.5
         );
      }
   }

   public static class RouteSegment {
      public static final BinaryRoutePlanner.RouteSegment NULL = new BinaryRoutePlanner.RouteSegment(null, 0, 1);
      final short segStart;
      final short segEnd;
      final RouteDataObject road;
      BinaryRoutePlanner.RouteSegment nextLoaded = null;
      BinaryRoutePlanner.RouteSegment next = null;
      BinaryRoutePlanner.RouteSegment oppositeDirection = null;
      BinaryRoutePlanner.RouteSegment reverseSearch = null;
      BinaryRoutePlanner.RouteSegment parentRoute = null;
      float distanceFromStart = 0.0F;
      float distanceToEnd = 0.0F;

      public RouteSegment(RouteDataObject road, int segmentStart, int segmentEnd) {
         this.road = road;
         this.segStart = (short)segmentStart;
         this.segEnd = (short)segmentEnd;
      }

      public RouteSegment(RouteDataObject road, int segmentStart) {
         this(road, segmentStart, segmentStart < road.getPointsLength() - 1 ? segmentStart + 1 : segmentStart - 1);
      }

      public BinaryRoutePlanner.RouteSegment initRouteSegment(boolean positiveDirection) {
         if (this.segStart == 0 && !positiveDirection) {
            return null;
         } else if (this.segStart == this.road.getPointsLength() - 1 && positiveDirection) {
            return null;
         } else if (this.segStart == this.segEnd) {
            throw new IllegalArgumentException();
         } else if (positiveDirection == this.segEnd > this.segStart) {
            return this;
         } else {
            if (this.oppositeDirection == null) {
               this.oppositeDirection = new BinaryRoutePlanner.RouteSegment(
                  this.road, this.segStart, this.segEnd > this.segStart ? this.segStart - 1 : this.segStart + 1
               );
               this.oppositeDirection.oppositeDirection = this;
            }

            return this.oppositeDirection;
         }
      }

      public boolean isSegmentAttachedToStart() {
         return this.parentRoute != null;
      }

      public BinaryRoutePlanner.RouteSegment getParentRoute() {
         return this.parentRoute == NULL ? null : this.parentRoute;
      }

      public boolean isPositive() {
         return this.segEnd > this.segStart;
      }

      public void setParentRoute(BinaryRoutePlanner.RouteSegment parentRoute) {
         this.parentRoute = parentRoute;
      }

      public BinaryRoutePlanner.RouteSegment getNext() {
         return this.next;
      }

      public short getSegmentStart() {
         return this.segStart;
      }

      public short getSegmentEnd() {
         return this.segEnd;
      }

      public float getDistanceFromStart() {
         return this.distanceFromStart;
      }

      public void setDistanceFromStart(float distanceFromStart) {
         this.distanceFromStart = distanceFromStart;
      }

      public RouteDataObject getRoad() {
         return this.road;
      }

      public String getTestName() {
         return MessageFormat.format("s{0,number,#.##} e{1,number,#.##}", this.distanceFromStart, this.distanceToEnd);
      }

      @Override
      public String toString() {
         int x = this.road.getPoint31XTile(this.segStart);
         int y = this.road.getPoint31YTile(this.segStart);
         int xe = this.road.getPoint31XTile(this.segEnd);
         int ye = this.road.getPoint31YTile(this.segEnd);
         float dst = (float)((int)(Math.sqrt(MapUtils.squareDist31TileMetric(x, y, xe, ye)) * 10.0)) / 10.0F;
         return (this.road == null ? "NULL" : this.road.toString()) + " [" + this.segStart + "-" + this.segEnd + "] " + dst + " m";
      }

      public Iterator<BinaryRoutePlanner.RouteSegment> getIterator() {
         return new Iterator<BinaryRoutePlanner.RouteSegment>() {
            BinaryRoutePlanner.RouteSegment next = RouteSegment.this;

            @Override
            public void remove() {
               throw new UnsupportedOperationException();
            }

            public BinaryRoutePlanner.RouteSegment next() {
               BinaryRoutePlanner.RouteSegment c = this.next;
               if (this.next != null) {
                  this.next = this.next.next;
               }

               return c;
            }

            @Override
            public boolean hasNext() {
               return this.next != null;
            }
         };
      }
   }

   public static class RouteSegmentPoint extends BinaryRoutePlanner.RouteSegment {
      public double distSquare;
      public int preciseX;
      public int preciseY;
      public List<BinaryRoutePlanner.RouteSegmentPoint> others;

      public RouteSegmentPoint(RouteDataObject road, int segmentStart, double distSquare) {
         super(road, segmentStart);
         this.distSquare = distSquare;
         this.preciseX = road.getPoint31XTile(segmentStart);
         this.preciseY = road.getPoint31YTile(segmentStart);
      }

      public RouteSegmentPoint(BinaryRoutePlanner.RouteSegmentPoint pnt) {
         super(pnt.road, pnt.segStart, pnt.segEnd);
         this.distSquare = pnt.distSquare;
         this.preciseX = pnt.preciseX;
         this.preciseY = pnt.preciseY;
      }

      public LatLon getPreciseLatLon() {
         return new LatLon(MapUtils.get31LatitudeY(this.preciseY), MapUtils.get31LongitudeX(this.preciseX));
      }

      @Override
      public String toString() {
         return String.format("%d (%s): %s", this.segStart, this.getPreciseLatLon(), this.road);
      }
   }

   public interface RouteSegmentVisitor {
      void visitSegment(BinaryRoutePlanner.RouteSegment var1, int var2, boolean var3);

      void visitApproximatedSegments(List<RouteSegmentResult> var1, RoutePlannerFrontEnd.GpxPoint var2, RoutePlannerFrontEnd.GpxPoint var3);
   }

   private static class SegmentsComparator implements Comparator<BinaryRoutePlanner.RouteSegment> {
      final RoutingContext ctx;

      public SegmentsComparator(RoutingContext ctx) {
         this.ctx = ctx;
      }

      public int compare(BinaryRoutePlanner.RouteSegment o1, BinaryRoutePlanner.RouteSegment o2) {
         return this.ctx
            .roadPriorityComparator((double)o1.distanceFromStart, (double)o1.distanceToEnd, (double)o2.distanceFromStart, (double)o2.distanceToEnd);
      }
   }
}
