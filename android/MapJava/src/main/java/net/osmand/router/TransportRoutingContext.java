package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.util.MapUtils;

public class TransportRoutingContext {
   public NativeLibrary library;
   public RouteCalculationProgress calculationProgress;
   public TLongObjectHashMap<TransportRoutePlanner.TransportRouteSegment> visitedSegments = new TLongObjectHashMap();
   public TransportRoutingConfiguration cfg;
   public TLongObjectHashMap<TransportRoute> combinedRoutesCache = new TLongObjectHashMap();
   public Map<TransportStop, List<TransportRoute>> missingStopsCache = new HashMap<>();
   public TLongObjectHashMap<List<TransportRoutePlanner.TransportRouteSegment>> quadTree;
   final TransportStopsRouteReader transportStopsReader;
   public int finishTimeSeconds;
   public long startCalcTime;
   public int visitedRoutesCount;
   public int visitedStops;
   public int wrongLoadedWays;
   public int loadedWays;
   public long loadTime;
   public long readTime;
   private final int walkRadiusIn31;
   private final int walkChangeRadiusIn31;

   public TransportRoutingContext(TransportRoutingConfiguration cfg, NativeLibrary library, BinaryMapIndexReader... readers) {
      this.cfg = cfg;
      this.walkRadiusIn31 = (int)((double)cfg.walkRadius / MapUtils.getTileDistanceWidth(31.0F));
      this.walkChangeRadiusIn31 = (int)((double)cfg.walkChangeRadius / MapUtils.getTileDistanceWidth(31.0F));
      this.quadTree = new TLongObjectHashMap();
      this.library = library;
      this.transportStopsReader = new TransportStopsRouteReader(Arrays.asList(readers));
   }

   public List<TransportRoutePlanner.TransportRouteSegment> getTransportStops(LatLon loc) throws IOException {
      int y = MapUtils.get31TileNumberY(loc.getLatitude());
      int x = MapUtils.get31TileNumberX(loc.getLongitude());
      return this.getTransportStops(x, y, false, new ArrayList());
   }

   public List<TransportRoutePlanner.TransportRouteSegment> getTransportStops(
      int x, int y, boolean change, List<TransportRoutePlanner.TransportRouteSegment> res
   ) throws IOException {
      return this.loadNativeTransportStops(x, y, change, res);
   }

   private List<TransportRoutePlanner.TransportRouteSegment> loadNativeTransportStops(
      int sx, int sy, boolean change, List<TransportRoutePlanner.TransportRouteSegment> res
   ) throws IOException {
      long nanoTime = System.nanoTime();
      int d = change ? this.walkChangeRadiusIn31 : this.walkRadiusIn31;
      int lx = sx - d >> 31 - this.cfg.ZOOM_TO_LOAD_TILES;
      int rx = sx + d >> 31 - this.cfg.ZOOM_TO_LOAD_TILES;
      int ty = sy - d >> 31 - this.cfg.ZOOM_TO_LOAD_TILES;
      int by = sy + d >> 31 - this.cfg.ZOOM_TO_LOAD_TILES;

      for(int x = lx; x <= rx; ++x) {
         for(int y = ty; y <= by; ++y) {
            long tileId = ((long)x << this.cfg.ZOOM_TO_LOAD_TILES + 1) + (long)y;
            List<TransportRoutePlanner.TransportRouteSegment> list = (List)this.quadTree.get(tileId);
            if (list == null) {
               list = this.loadTile(x, y);
               this.quadTree.put(tileId, list);
            }

            for(TransportRoutePlanner.TransportRouteSegment r : list) {
               TransportStop st = r.getStop(r.segStart);
               if (Math.abs(st.x31 - sx) <= this.walkRadiusIn31 && Math.abs(st.y31 - sy) <= this.walkRadiusIn31) {
                  ++this.loadedWays;
                  res.add(r);
               } else {
                  ++this.wrongLoadedWays;
               }
            }
         }
      }

      this.loadTime += System.nanoTime() - nanoTime;
      return res;
   }

   private List<TransportRoutePlanner.TransportRouteSegment> loadTile(int x, int y) throws IOException {
      long nanoTime = System.nanoTime();
      List<TransportRoutePlanner.TransportRouteSegment> lst = new ArrayList<>();
      int pz = 31 - this.cfg.ZOOM_TO_LOAD_TILES;
      BinaryMapIndexReader.SearchRequest<TransportStop> sr = BinaryMapIndexReader.buildSearchTransportRequest(
         x << pz, x + 1 << pz, y << pz, y + 1 << pz, -1, null
      );
      Collection<TransportStop> stops = this.transportStopsReader.readMergedTransportStops(sr);
      this.loadTransportSegments(stops, lst);
      this.readTime += System.nanoTime() - nanoTime;
      return lst;
   }

   private void loadTransportSegments(Collection<TransportStop> stops, List<TransportRoutePlanner.TransportRouteSegment> lst) throws IOException {
      for(TransportStop s : stops) {
         if (!s.isDeleted() && s.getRoutes() != null) {
            for(TransportRoute route : s.getRoutes()) {
               int stopIndex = -1;
               double dist = 40.0;

               for(int k = 0; k < route.getForwardStops().size(); ++k) {
                  TransportStop st = route.getForwardStops().get(k);
                  if (st.getId() == s.getId()) {
                     stopIndex = k;
                     break;
                  }

                  double d = MapUtils.getDistance(st.getLocation(), s.getLocation());
                  if (d < dist) {
                     stopIndex = k;
                     dist = d;
                  }
               }

               if (stopIndex != -1) {
                  if (this.cfg != null && this.cfg.useSchedule) {
                     this.loadScheduleRouteSegment(lst, route, stopIndex);
                  } else {
                     TransportRoutePlanner.TransportRouteSegment segment = new TransportRoutePlanner.TransportRouteSegment(route, stopIndex);
                     lst.add(segment);
                  }
               } else {
                  System.err
                     .println(
                        String.format(Locale.US, "Routing error: missing stop '%s' in route '%s' id: %d", s.toString(), route.getRef(), route.getId() / 2L)
                     );
               }
            }
         }
      }
   }

   private void loadScheduleRouteSegment(List<TransportRoutePlanner.TransportRouteSegment> lst, TransportRoute route, int stopIndex) {
      if (route.getSchedule() != null) {
         TIntArrayList ti = route.getSchedule().tripIntervals;
         int cnt = ti.size();
         int t = 0;
         int stopTravelTime = 0;
         TIntArrayList avgStopIntervals = route.getSchedule().avgStopIntervals;

         for(int i = 0; i < stopIndex; ++i) {
            if (avgStopIntervals.size() > i) {
               stopTravelTime += avgStopIntervals.getQuick(i);
            }
         }

         for(int i = 0; i < cnt; ++i) {
            t += ti.getQuick(i);
            int startTime = t + stopTravelTime;
            if (startTime >= this.cfg.scheduleTimeOfDay && startTime <= this.cfg.scheduleTimeOfDay + this.cfg.scheduleMaxTime) {
               TransportRoutePlanner.TransportRouteSegment segment = new TransportRoutePlanner.TransportRouteSegment(route, stopIndex, startTime);
               lst.add(segment);
            }
         }
      }
   }
}
