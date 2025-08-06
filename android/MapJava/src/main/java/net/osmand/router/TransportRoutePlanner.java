package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportRoutePlanner {
   private static final boolean MEASURE_TIME = false;
   private static final int MIN_DIST_STOP_TO_GEOMETRY = 150;
   public static final long GEOMETRY_WAY_ID = -1L;
   public static final long STOPS_WAY_ID = -2L;

   public List<TransportRouteResult> buildRoute(TransportRoutingContext ctx, LatLon start, LatLon end) throws IOException, InterruptedException {
      ctx.startCalcTime = System.currentTimeMillis();
      double totalDistance = MapUtils.getDistance(start, end);
      List<TransportRoutePlanner.TransportRouteSegment> startStops = ctx.getTransportStops(start);
      List<TransportRoutePlanner.TransportRouteSegment> endStops = ctx.getTransportStops(end);
      TLongObjectHashMap<TransportRoutePlanner.TransportRouteSegment> endSegments = new TLongObjectHashMap();

      for(TransportRoutePlanner.TransportRouteSegment s : endStops) {
         endSegments.put(s.getId(), s);
      }

      if (startStops.size() == 0) {
         return Collections.emptyList();
      } else {
         PriorityQueue<TransportRoutePlanner.TransportRouteSegment> queue = new PriorityQueue<>(
            startStops.size(), new TransportRoutePlanner.SegmentsComparator(ctx)
         );

         for(TransportRoutePlanner.TransportRouteSegment r : startStops) {
            r.walkDist = (double)((float)MapUtils.getDistance(r.getLocation(), start));
            r.distFromStart = r.walkDist / (double)ctx.cfg.walkSpeed;
            queue.add(r);
         }

         double finishTime = (double)ctx.cfg.maxRouteTime;
         ctx.finishTimeSeconds = ctx.cfg.finishTimeSeconds;
         if (totalDistance > (double)ctx.cfg.maxRouteDistance && ctx.cfg.maxRouteIncreaseSpeed > 0) {
            int increaseTime = (int)((totalDistance - (double)ctx.cfg.maxRouteDistance) * 3.6 / (double)ctx.cfg.maxRouteIncreaseSpeed);
            finishTime += (double)increaseTime;
            ctx.finishTimeSeconds += increaseTime / 6;
         }

         double maxTravelTimeCmpToWalk = totalDistance / (double)ctx.cfg.walkSpeed - (double)(ctx.cfg.changeTime / 2);
         List<TransportRoutePlanner.TransportRouteSegment> results = new ArrayList<>();
         this.initProgressBar(ctx, start, end);

         while(!queue.isEmpty()) {
            long beginMs = 0L;
            if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
               return null;
            }

            TransportRoutePlanner.TransportRouteSegment segment = queue.poll();
            TransportRoutePlanner.TransportRouteSegment ex = (TransportRoutePlanner.TransportRouteSegment)ctx.visitedSegments.get(segment.getId());
            if (ex != null) {
               if (ex.distFromStart > segment.distFromStart) {
                  System.err.println(String.format("%.1f (%s) > %.1f (%s)", ex.distFromStart, ex, segment.distFromStart, segment));
               }
            } else {
               ++ctx.visitedRoutesCount;
               ctx.visitedSegments.put(segment.getId(), segment);
               if (segment.getDepth() <= ctx.cfg.maxNumberOfChanges + 1) {
                  if (segment.distFromStart > finishTime + (double)ctx.finishTimeSeconds || segment.distFromStart > maxTravelTimeCmpToWalk) {
                     break;
                  }

                  long segmentId = segment.getId();
                  TransportRoutePlanner.TransportRouteSegment finish = null;
                  double minDist = 0.0;
                  double travelDist = 0.0;
                  double travelTime = 0.0;
                  float routeTravelSpeed = ctx.cfg.getSpeedByRouteType(segment.road.getType());
                  if (routeTravelSpeed != 0.0F) {
                     TransportStop prevStop = segment.getStop(segment.segStart);
                     List<TransportRoutePlanner.TransportRouteSegment> sgms = new ArrayList<>();

                     for(int ind = 1 + segment.segStart; ind < segment.getLength(); ++ind) {
                        if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
                           return null;
                        }

                        ctx.visitedSegments.put(++segmentId, segment);
                        TransportStop stop = segment.getStop(ind);
                        double segmentDist = MapUtils.getDistance(prevStop.getLocation(), stop.getLocation());
                        travelDist += segmentDist;
                        if (ctx.cfg.useSchedule) {
                           TransportSchedule sc = segment.road.getSchedule();
                           int interval = sc.avgStopIntervals.get(ind - 1);
                           travelTime += (double)(interval * 10);
                        } else {
                           travelTime += (double)ctx.cfg.stopTime + segmentDist / (double)routeTravelSpeed;
                        }

                        if (segment.distFromStart + travelTime > finishTime + (double)ctx.finishTimeSeconds) {
                           break;
                        }

                        sgms.clear();
                        sgms = ctx.getTransportStops(stop.x31, stop.y31, true, sgms);
                        ++ctx.visitedStops;

                        for(TransportRoutePlanner.TransportRouteSegment sgm : sgms) {
                           if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
                              return null;
                           }

                           if (!segment.wasVisited(sgm)) {
                              TransportRoutePlanner.TransportRouteSegment nextSegment = new TransportRoutePlanner.TransportRouteSegment(sgm);
                              nextSegment.parentRoute = segment;
                              nextSegment.parentStop = ind;
                              nextSegment.walkDist = MapUtils.getDistance(nextSegment.getLocation(), stop.getLocation());
                              nextSegment.parentTravelTime = travelTime;
                              nextSegment.parentTravelDist = travelDist;
                              double walkTime = nextSegment.walkDist / (double)ctx.cfg.walkSpeed
                                 + (double)ctx.cfg.getChangeTime()
                                 + (double)ctx.cfg.getBoardingTime();
                              nextSegment.distFromStart = segment.distFromStart + travelTime + walkTime;
                              if (ctx.cfg.useSchedule) {
                                 int tm = (sgm.departureTime - ctx.cfg.scheduleTimeOfDay) * 10;
                                 if ((double)tm >= nextSegment.distFromStart) {
                                    nextSegment.distFromStart = (double)tm;
                                    queue.add(nextSegment);
                                 }
                              } else {
                                 queue.add(nextSegment);
                              }
                           }
                        }

                        TransportRoutePlanner.TransportRouteSegment finalSegment = (TransportRoutePlanner.TransportRouteSegment)endSegments.get(segmentId);
                        double distToEnd = MapUtils.getDistance(stop.getLocation(), end);
                        if (finalSegment != null && distToEnd < (double)ctx.cfg.walkRadius && (finish == null || minDist > distToEnd)) {
                           minDist = distToEnd;
                           finish = new TransportRoutePlanner.TransportRouteSegment(finalSegment);
                           finish.parentRoute = segment;
                           finish.parentStop = ind;
                           finish.walkDist = distToEnd;
                           finish.parentTravelTime = travelTime;
                           finish.parentTravelDist = travelDist;
                           double walkTime = distToEnd / (double)ctx.cfg.walkSpeed;
                           finish.distFromStart = segment.distFromStart + travelTime + walkTime;
                        }

                        prevStop = stop;
                     }

                     if (finish != null) {
                        if (finishTime > finish.distFromStart) {
                           finishTime = finish.distFromStart;
                        }

                        if (finish.distFromStart < finishTime + (double)ctx.finishTimeSeconds
                           && (finish.distFromStart < maxTravelTimeCmpToWalk || results.size() == 0)) {
                           results.add(finish);
                        }
                     }

                     if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
                        throw new InterruptedException("Route calculation interrupted");
                     }

                     this.updateCalculationProgress(ctx, queue);
                  }
               }
            }
         }

         return this.prepareResults(ctx, results);
      }
   }

   private void initProgressBar(TransportRoutingContext ctx, LatLon start, LatLon end) {
      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.distanceFromEnd = 0.0F;
         ctx.calculationProgress.reverseSegmentQueueSize = 0;
         ctx.calculationProgress.directSegmentQueueSize = 0;
         float speed = ctx.cfg.defaultTravelSpeed + 1.0F;
         ctx.calculationProgress.totalEstimatedDistance = (float)(MapUtils.getDistance(start, end) / (double)speed);
      }
   }

   private void updateCalculationProgress(TransportRoutingContext ctx, PriorityQueue<TransportRoutePlanner.TransportRouteSegment> queue) {
      if (ctx.calculationProgress != null) {
         ctx.calculationProgress.directSegmentQueueSize = queue.size();
         if (queue.size() > 0) {
            TransportRoutePlanner.TransportRouteSegment peek = queue.peek();
            ctx.calculationProgress.distanceFromBegin = (float)Math.max(peek.distFromStart, (double)ctx.calculationProgress.distanceFromBegin);
         }
      }
   }

   private List<TransportRouteResult> prepareResults(TransportRoutingContext ctx, List<TransportRoutePlanner.TransportRouteSegment> results) {
      Collections.sort(results, new TransportRoutePlanner.SegmentsComparator(ctx));
      List<TransportRouteResult> lst = new ArrayList<>();
      System.out
         .println(
            String.format(
               Locale.US,
               "Calculated %.1f seconds, found %d results, visited %d routes / %d stops, loaded %d tiles (%d ms read, %d ms total), loaded ways %d (%d wrong)",
               (double)(System.currentTimeMillis() - ctx.startCalcTime) / 1000.0,
               results.size(),
               ctx.visitedRoutesCount,
               ctx.visitedStops,
               ctx.quadTree.size(),
               ctx.readTime / 1000000L,
               ctx.loadTime / 1000000L,
               ctx.loadedWays,
               ctx.wrongLoadedWays
            )
         );

      for(TransportRoutePlanner.TransportRouteSegment res : results) {
         if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
            return null;
         }

         TransportRouteResult route = new TransportRouteResult(ctx);
         route.routeTime = res.distFromStart;
         route.finishWalkDist = res.walkDist;

         for(TransportRoutePlanner.TransportRouteSegment p = res; p != null; p = p.parentRoute) {
            if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
               return null;
            }

            if (p.parentRoute != null) {
               TransportRoutePlanner.TransportRouteResultSegment sg = new TransportRoutePlanner.TransportRouteResultSegment();
               sg.route = p.parentRoute.road;
               sg.start = p.parentRoute.segStart;
               sg.end = p.parentStop;
               sg.walkDist = p.parentRoute.walkDist;
               sg.walkTime = sg.walkDist / (double)ctx.cfg.walkSpeed;
               sg.depTime = p.departureTime;
               sg.travelDistApproximate = p.parentTravelDist;
               sg.travelTime = p.parentTravelTime;
               route.segments.add(0, sg);
            }
         }

         boolean include = false;

         for(TransportRouteResult s : lst) {
            if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
               return null;
            }

            if (this.includeRoute(s, route)) {
               include = true;
               break;
            }
         }

         if (!include) {
            lst.add(route);
            System.out.println(route.toString());
         }
      }

      return lst;
   }

   private boolean includeRoute(TransportRouteResult fastRoute, TransportRouteResult testRoute) {
      if (testRoute.segments.size() < fastRoute.segments.size()) {
         return false;
      } else {
         int j = 0;

         for(int i = 0; i < fastRoute.segments.size(); ++j) {
            for(TransportRoutePlanner.TransportRouteResultSegment fs = fastRoute.segments.get(i); j < testRoute.segments.size(); ++j) {
               TransportRoutePlanner.TransportRouteResultSegment ts = testRoute.segments.get(j);
               if (fs.route.getId() == ts.route.getId()) {
                  break;
               }
            }

            if (j >= testRoute.segments.size()) {
               return false;
            }

            ++i;
         }

         return true;
      }
   }

   public static String formatTransportTime(int i) {
      int h = i / 60 / 6;
      int mh = i - h * 60 * 6;
      int m = mh / 6;
      int s = (mh - m * 6) * 10;
      return String.format(Locale.US, "%02d:%02d:%02d ", h, m, s);
   }

   public static List<TransportRouteResult> convertToTransportRoutingResult(NativeTransportRoutingResult[] res, TransportRoutingConfiguration cfg) {
      TLongObjectHashMap<TransportRoute> convertedRoutesCache = new TLongObjectHashMap();
      TLongObjectHashMap<TransportStop> convertedStopsCache = new TLongObjectHashMap();
      if (res.length == 0) {
         return new ArrayList<>();
      } else {
         List<TransportRouteResult> convertedRes = new ArrayList<>();

         for(NativeTransportRoutingResult ntrr : res) {
            TransportRouteResult trr = new TransportRouteResult(cfg);
            trr.setFinishWalkDist(ntrr.finishWalkDist);
            trr.setRouteTime(ntrr.routeTime);

            for(NativeTransportRouteResultSegment ntrs : ntrr.segments) {
               TransportRoutePlanner.TransportRouteResultSegment trs = new TransportRoutePlanner.TransportRouteResultSegment();
               trs.route = convertTransportRoute(ntrs.route, convertedRoutesCache, convertedStopsCache);
               trs.walkTime = ntrs.walkTime;
               trs.travelDistApproximate = ntrs.travelDistApproximate;
               trs.travelTime = ntrs.travelTime;
               trs.start = ntrs.start;
               trs.end = ntrs.end;
               trs.walkDist = ntrs.walkDist;
               trs.depTime = ntrs.depTime;
               trr.addSegment(trs);
            }

            convertedRes.add(trr);
         }

         convertedStopsCache.clear();
         convertedRoutesCache.clear();
         return convertedRes;
      }
   }

   private static TransportRoute convertTransportRoute(
      NativeTransportRoute nr, TLongObjectHashMap<TransportRoute> convertedRoutesCache, TLongObjectHashMap<TransportStop> convertedStopsCache
   ) {
      TransportRoute r = new TransportRoute();
      r.setId(Long.valueOf(nr.id));
      r.setLocation(nr.routeLat, nr.routeLon);
      r.setName(nr.name);
      r.setEnName(nr.enName);
      if (nr.namesLng.length > 0 && nr.namesLng.length == nr.namesNames.length) {
         for(int i = 0; i < nr.namesLng.length; ++i) {
            r.setName(nr.namesLng[i], nr.namesNames[i]);
         }
      }

      r.setFileOffset(nr.fileOffset);
      r.setForwardStops(convertTransportStops(nr.forwardStops, convertedStopsCache));
      r.setRef(nr.ref);
      r.setOperator(nr.routeOperator);
      r.setType(nr.type);
      r.setDist(nr.dist);
      r.setColor(nr.color);
      if (nr.intervals != null
         && nr.intervals.length > 0
         && nr.avgStopIntervals != null
         && nr.avgStopIntervals.length > 0
         && nr.avgWaitIntervals != null
         && nr.avgWaitIntervals.length > 0) {
         r.setSchedule(new TransportSchedule(new TIntArrayList(nr.intervals), new TIntArrayList(nr.avgStopIntervals), new TIntArrayList(nr.avgWaitIntervals)));
      }

      for(int i = 0; i < nr.waysIds.length; ++i) {
         List<Node> wnodes = new ArrayList<>();

         for(int j = 0; j < nr.waysNodesLats[i].length; ++j) {
            wnodes.add(new Node(nr.waysNodesLats[i][j], nr.waysNodesLons[i][j], -1L));
         }

         r.addWay(new Way(nr.waysIds[i], wnodes));
      }

      if (convertedRoutesCache.get(r.getId()) == null) {
         convertedRoutesCache.put(r.getId(), r);
      }

      return r;
   }

   private static List<TransportStop> convertTransportStops(NativeTransportStop[] nstops, TLongObjectHashMap<TransportStop> convertedStopsCache) {
      List<TransportStop> stops = new ArrayList<>();

      for(NativeTransportStop ns : nstops) {
         if (convertedStopsCache != null && convertedStopsCache.get(ns.id) != null) {
            stops.add((TransportStop)convertedStopsCache.get(ns.id));
         } else {
            TransportStop s = new TransportStop();
            s.setId(Long.valueOf(ns.id));
            s.setLocation(ns.stopLat, ns.stopLon);
            s.setName(ns.name);
            s.setEnName(ns.enName);
            if (ns.namesLng.length > 0 && ns.namesLng.length == ns.namesNames.length) {
               for(int i = 0; i < ns.namesLng.length; ++i) {
                  s.setName(ns.namesLng[i], ns.namesNames[i]);
               }
            }

            s.setFileOffset(ns.fileOffset);
            s.setReferencesToRoutes(ns.referencesToRoutes);
            s.setDeletedRoutesIds(ns.deletedRoutesIds);
            s.setRoutesIds(ns.routesIds);
            s.distance = ns.distance;
            s.x31 = ns.x31;
            s.y31 = ns.y31;
            if (ns.pTStopExit_refs != null && ns.pTStopExit_refs.length > 0) {
               for(int i = 0; i < ns.pTStopExit_refs.length; ++i) {
                  s.addExit(new TransportStopExit(ns.pTStopExit_x31s[i], ns.pTStopExit_y31s[i], ns.pTStopExit_refs[i]));
               }
            }

            if (convertedStopsCache == null) {
               convertedStopsCache = new TLongObjectHashMap();
            }

            if (convertedStopsCache.get(s.getId()) == null) {
               convertedStopsCache.put(s.getId(), s);
            }

            stops.add(s);
         }
      }

      return stops;
   }

   private static class SegmentsComparator implements Comparator<TransportRoutePlanner.TransportRouteSegment> {
      public SegmentsComparator(TransportRoutingContext ctx) {
      }

      public int compare(TransportRoutePlanner.TransportRouteSegment o1, TransportRoutePlanner.TransportRouteSegment o2) {
         return Double.compare(o1.distFromStart, o2.distFromStart);
      }
   }

   public static class TransportRouteResultSegment {
      private static final boolean DISPLAY_FULL_SEGMENT_ROUTE = false;
      private static final int DISPLAY_SEGMENT_IND = 0;
      public TransportRoute route;
      public double walkTime;
      public double travelDistApproximate;
      public double travelTime;
      public int start;
      public int end;
      public double walkDist;
      public int depTime;

      public int getArrivalTime() {
         if (this.route.getSchedule() != null && this.depTime != -1) {
            int tm = this.depTime;
            TIntArrayList intervals = this.route.getSchedule().avgStopIntervals;

            for(int i = this.start; i <= this.end; ++i) {
               if (i == this.end) {
                  return tm;
               }

               if (intervals.size() <= i) {
                  break;
               }

               tm += intervals.get(i);
            }
         }

         return -1;
      }

      public double getTravelTime() {
         return this.travelTime;
      }

      public TransportStop getStart() {
         return this.route.getForwardStops().get(this.start);
      }

      public TransportStop getEnd() {
         return this.route.getForwardStops().get(this.end);
      }

      public List<TransportStop> getTravelStops() {
         return this.route.getForwardStops().subList(this.start, this.end + 1);
      }

      public QuadRect getSegmentRect() {
         double left = 0.0;
         double right = 0.0;
         double top = 0.0;
         double bottom = 0.0;

         for(Node n : this.getNodes()) {
            if (left == 0.0 && right == 0.0) {
               left = n.getLongitude();
               right = n.getLongitude();
               top = n.getLatitude();
               bottom = n.getLatitude();
            } else {
               left = Math.min(left, n.getLongitude());
               right = Math.max(right, n.getLongitude());
               top = Math.max(top, n.getLatitude());
               bottom = Math.min(bottom, n.getLatitude());
            }
         }

         return left == 0.0 && right == 0.0 ? null : new QuadRect(left, top, right, bottom);
      }

      public List<Node> getNodes() {
         List<Node> nodes = new ArrayList<>();

         for(Way way : this.getGeometry()) {
            nodes.addAll(way.getNodes());
         }

         return nodes;
      }

      public List<Way> getGeometry() {
         this.route.mergeForwardWays();
         List<Way> ways = this.route.getForwardWays();
         LatLon startLoc = this.getStart().getLocation();
         LatLon endLoc = this.getEnd().getLocation();
         TransportRoutePlanner.TransportRouteResultSegment.SearchNodeInd startInd = new TransportRoutePlanner.TransportRouteResultSegment.SearchNodeInd();
         TransportRoutePlanner.TransportRouteResultSegment.SearchNodeInd endInd = new TransportRoutePlanner.TransportRouteResultSegment.SearchNodeInd();

         for(int i = 0; i < ways.size(); ++i) {
            List<Node> nodes = ways.get(i).getNodes();

            for(int j = 0; j < nodes.size(); ++j) {
               Node n = nodes.get(j);
               if (MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude()) < startInd.dist) {
                  startInd.dist = MapUtils.getDistance(startLoc, n.getLatitude(), n.getLongitude());
                  startInd.ind = j;
                  startInd.way = ways.get(i);
               }

               if (MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude()) < endInd.dist) {
                  endInd.dist = MapUtils.getDistance(endLoc, n.getLatitude(), n.getLongitude());
                  endInd.ind = j;
                  endInd.way = ways.get(i);
               }
            }
         }

         boolean validOneWay = startInd.way != null && startInd.way == endInd.way && startInd.ind <= endInd.ind;
         if (validOneWay) {
            Way way = new Way(-1L);

            for(int k = startInd.ind; k <= endInd.ind; ++k) {
               way.addNode(startInd.way.getNodes().get(k));
            }

            return Collections.singletonList(way);
         } else {
            boolean validContinuation = startInd.way != null && endInd.way != null && startInd.way != endInd.way;
            if (validContinuation) {
               Node ln = startInd.way.getLastNode();
               Node fn = endInd.way.getFirstNode();
               if (ln != null && fn != null && MapUtils.getDistance(ln.getLatLon(), fn.getLatLon()) < 50000.0) {
                  validContinuation = true;
               } else {
                  validContinuation = false;
               }
            }

            if (validContinuation) {
               List<Way> two = new ArrayList<>();
               Way way = new Way(-1L);

               for(int k = startInd.ind; k < startInd.way.getNodes().size(); ++k) {
                  way.addNode(startInd.way.getNodes().get(k));
               }

               two.add(way);
               way = new Way(-1L);

               for(int k = 0; k <= endInd.ind; ++k) {
                  way.addNode(endInd.way.getNodes().get(k));
               }

               two.add(way);
               return two;
            } else {
               Way way = new Way(-2L);

               for(int i = this.start; i <= this.end; ++i) {
                  LatLon l = this.getStop(i).getLocation();
                  Node n = new Node(l.getLatitude(), l.getLongitude(), -1L);
                  way.addNode(n);
               }

               return Collections.singletonList(way);
            }
         }
      }

      public double getTravelDist() {
         double d = 0.0;

         for(int k = this.start; k < this.end; ++k) {
            d += MapUtils.getDistance(this.route.getForwardStops().get(k).getLocation(), this.route.getForwardStops().get(k + 1).getLocation());
         }

         return d;
      }

      public TransportStop getStop(int i) {
         return this.route.getForwardStops().get(i);
      }

      private static class SearchNodeInd {
         int ind = -1;
         Way way = null;
         double dist = 150.0;

         private SearchNodeInd() {
         }
      }
   }

   public static class TransportRouteSegment {
      final int segStart;
      final TransportRoute road;
      final int departureTime;
      private static final int SHIFT = 10;
      private static final int SHIFT_DEPTIME = 14;
      TransportRoutePlanner.TransportRouteSegment parentRoute = null;
      int parentStop;
      double parentTravelTime;
      double parentTravelDist;
      double walkDist = 0.0;
      double distFromStart = 0.0;

      public TransportRouteSegment(TransportRoute road, int stopIndex) {
         this.road = road;
         this.segStart = (short)stopIndex;
         this.departureTime = -1;
      }

      public TransportRouteSegment(TransportRoute road, int stopIndex, int depTime) {
         this.road = road;
         this.segStart = (short)stopIndex;
         this.departureTime = depTime;
      }

      public TransportRouteSegment(TransportRoutePlanner.TransportRouteSegment c) {
         this.road = c.road;
         this.segStart = c.segStart;
         this.departureTime = c.departureTime;
      }

      public boolean wasVisited(TransportRoutePlanner.TransportRouteSegment rrs) {
         if (rrs.road.getId() == this.road.getId() && rrs.departureTime == this.departureTime) {
            return true;
         } else {
            return this.parentRoute != null ? this.parentRoute.wasVisited(rrs) : false;
         }
      }

      public TransportStop getStop(int i) {
         return this.road.getForwardStops().get(i);
      }

      public LatLon getLocation() {
         return this.road.getForwardStops().get(this.segStart).getLocation();
      }

      public int getLength() {
         return this.road.getForwardStops().size();
      }

      public long getId() {
         long l = this.road.getId();
         l <<= 14;
         if (this.departureTime >= 16384) {
            throw new IllegalStateException("too long dep time" + this.departureTime);
         } else {
            l += (long)(this.departureTime + 1);
            l <<= 10;
            if (this.segStart >= 1024) {
               throw new IllegalStateException("too many stops " + this.road.getId() + " " + this.segStart);
            } else {
               l += (long)this.segStart;
               if (l < 0L) {
                  throw new IllegalStateException("too long id " + this.road.getId());
               } else {
                  return l;
               }
            }
         }
      }

      public int getDepth() {
         return this.parentRoute != null ? this.parentRoute.getDepth() + 1 : 1;
      }

      @Override
      public String toString() {
         return String.format(
            "Route: %s, stop: %s %s",
            this.road.getName(),
            this.road.getForwardStops().get(this.segStart).getName(),
            this.departureTime == -1 ? "" : TransportRoutePlanner.formatTransportTime(this.departureTime)
         );
      }
   }
}
