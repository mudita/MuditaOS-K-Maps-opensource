package net.osmand.router;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

public class RoutingContext {
   public static boolean SHOW_GC_SIZE = false;
   public static boolean PRINT_ROUTING_ALERTS = false;
   private static final Log log = PlatformUtil.getLog(RoutingContext.class);
   public final RoutingConfiguration config;
   public final RoutePlannerFrontEnd.RouteCalculationMode calculationMode;
   public final NativeLibrary nativeLib;
   public final Map<BinaryMapIndexReader, List<BinaryMapRouteReaderAdapter.RouteSubregion>> map = new LinkedHashMap<>();
   public final Map<BinaryMapRouteReaderAdapter.RouteRegion, BinaryMapIndexReader> reverseMap = new LinkedHashMap<>();
   public long nativeRoutingContext;
   public boolean keepNativeRoutingContext;
   public int startX;
   public int startY;
   public long startRoadId;
   public int startSegmentInd;
   public boolean startTransportStop;
   public int targetX;
   public int targetY;
   public long targetRoadId;
   public int targetSegmentInd;
   public boolean targetTransportStop;
   public boolean publicTransport;
   public RouteCalculationProgress calculationProgress;
   public RouteCalculationProgress calculationProgressFirstPhase;
   public boolean leftSideNavigation;
   public List<RouteSegmentResult> previouslyCalculatedRoute;
   public PrecalculatedRouteDirection precalculatedRouteDirection;
   TLongObjectHashMap<List<RoutingContext.RoutingSubregionTile>> indexedSubregions = new TLongObjectHashMap();
   List<RoutingContext.RoutingSubregionTile> subregionTiles = new ArrayList<>();
   ArrayList<BinaryRoutePlanner.RouteSegment> segmentsToVisitPrescripted = new ArrayList<>(5);
   ArrayList<BinaryRoutePlanner.RouteSegment> segmentsToVisitNotForbidden = new ArrayList<>(5);
   public RoutingContext.TileStatistics global = new RoutingContext.TileStatistics();
   public int memoryOverhead = 0;
   public float routingTime = 0.0F;
   BinaryRoutePlanner.RouteSegmentVisitor visitor = null;
   public int alertFasterRoadToVisitedSegments;
   public int alertSlowerSegmentedWasVisitedEarlier;
   public BinaryRoutePlanner.FinalRouteSegment finalRouteSegment;

   RoutingContext(RoutingContext cp) {
      this.config = cp.config;
      this.map.putAll(cp.map);
      this.calculationMode = cp.calculationMode;
      this.leftSideNavigation = cp.leftSideNavigation;
      this.reverseMap.putAll(cp.reverseMap);
      this.nativeLib = cp.nativeLib;
      this.visitor = cp.visitor;
      this.calculationProgress = cp.calculationProgress;
   }

   RoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RoutePlannerFrontEnd.RouteCalculationMode calcMode) {
      this.calculationMode = calcMode;

      for(BinaryMapIndexReader mr : map) {
         List<BinaryMapRouteReaderAdapter.RouteRegion> rr = mr.getRoutingIndexes();
         List<BinaryMapRouteReaderAdapter.RouteSubregion> subregions = new ArrayList<>();

         for(BinaryMapRouteReaderAdapter.RouteRegion r : rr) {
            for(BinaryMapRouteReaderAdapter.RouteSubregion rs : calcMode == RoutePlannerFrontEnd.RouteCalculationMode.BASE
               ? r.getBaseSubregions()
               : r.getSubregions()) {
               subregions.add(new BinaryMapRouteReaderAdapter.RouteSubregion(rs));
            }

            this.reverseMap.put(r, mr);
         }

         this.map.put(mr, subregions);
      }

      this.config = config;
      this.nativeLib = nativeLibrary;
   }

   public BinaryRoutePlanner.RouteSegmentVisitor getVisitor() {
      return this.visitor;
   }

   public int getCurrentlyLoadedTiles() {
      int cnt = 0;

      for(RoutingContext.RoutingSubregionTile t : this.subregionTiles) {
         if (t.isLoaded()) {
            ++cnt;
         }
      }

      return cnt;
   }

   public int getCurrentEstimatedSize() {
      return this.global.size;
   }

   public void setVisitor(BinaryRoutePlanner.RouteSegmentVisitor visitor) {
      this.visitor = visitor;
   }

   public void setRouter(GeneralRouter router) {
      this.config.router = router;
   }

   public void setHeuristicCoefficient(float heuristicCoefficient) {
      this.config.heuristicCoefficient = heuristicCoefficient;
   }

   public VehicleRouter getRouter() {
      return this.config.router;
   }

   public boolean planRouteIn2Directions() {
      return this.config.planRoadDirection == 0;
   }

   public int getPlanRoadDirection() {
      return this.config.planRoadDirection;
   }

   public int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, double o2DistanceFromStart, double o2DistanceToEnd) {
      return BinaryRoutePlanner.roadPriorityComparator(
         o1DistanceFromStart, o1DistanceToEnd, o2DistanceFromStart, o2DistanceToEnd, (double)this.config.heuristicCoefficient
      );
   }

   public void initStartAndTargetPoints(BinaryRoutePlanner.RouteSegmentPoint start, BinaryRoutePlanner.RouteSegmentPoint end) {
      this.initTargetPoint(end);
      this.startX = start.preciseX;
      this.startY = start.preciseY;
      this.startRoadId = start.road.getId();
      this.startSegmentInd = start.getSegmentStart();
   }

   public void initTargetPoint(BinaryRoutePlanner.RouteSegmentPoint end) {
      this.targetX = end.preciseX;
      this.targetY = end.preciseY;
      this.targetRoadId = end.road.getId();
      this.targetSegmentInd = end.getSegmentStart();
   }

   public void unloadAllData() {
      this.unloadAllData(null);
   }

   public void unloadAllData(RoutingContext except) {
      for(RoutingContext.RoutingSubregionTile tl : this.subregionTiles) {
         if (tl.isLoaded() && (except == null || except.searchSubregionTile(tl.subregion) < 0)) {
            tl.unload();
            if (this.calculationProgress != null) {
               ++this.calculationProgress.unloadedTiles;
            }

            this.global.size -= tl.tileStatistics.size;
         }
      }

      this.subregionTiles.clear();
      this.indexedSubregions.clear();
   }

   private int searchSubregionTile(BinaryMapRouteReaderAdapter.RouteSubregion subregion) {
      RoutingContext.RoutingSubregionTile key = new RoutingContext.RoutingSubregionTile(subregion);
      int ind = Collections.binarySearch(this.subregionTiles, key, new Comparator<RoutingContext.RoutingSubregionTile>() {
         public int compare(RoutingContext.RoutingSubregionTile o1, RoutingContext.RoutingSubregionTile o2) {
            if (o1.subregion.left == o2.subregion.left) {
               return 0;
            } else {
               return o1.subregion.left < o2.subregion.left ? 1 : -1;
            }
         }
      });
      if (ind >= 0) {
         for(int i = ind; i <= this.subregionTiles.size(); ++i) {
            if (i == this.subregionTiles.size() || this.subregionTiles.get(i).subregion.left > subregion.left) {
               return -i - 1;
            }

            if (this.subregionTiles.get(i).subregion == subregion) {
               return i;
            }
         }
      }

      return ind;
   }

   public BinaryRoutePlanner.RouteSegment loadRouteSegment(int x31, int y31, long memoryLimit) {
      return this.loadRouteSegment(x31, y31, memoryLimit, false);
   }

   public BinaryRoutePlanner.RouteSegment loadRouteSegment(int x31, int y31, long memoryLimit, boolean reverseWaySearch) {
      long tileId = this.getRoutingTile(x31, y31, memoryLimit);
      TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap();
      BinaryRoutePlanner.RouteSegment original = null;
      List<RoutingContext.RoutingSubregionTile> subregions = (List)this.indexedSubregions.get(tileId);
      if (subregions != null) {
         for(int j = 0; j < subregions.size(); ++j) {
            original = subregions.get(j).loadRouteSegment(x31, y31, this, excludeDuplications, original, subregions, j, reverseWaySearch);
         }
      }

      return original;
   }

   public void loadSubregionTile(
      RoutingContext.RoutingSubregionTile ts, boolean loadObjectsInMemory, List<RouteDataObject> toLoad, TLongHashSet excludeNotAllowed
   ) {
      long now = System.nanoTime();
      boolean wasUnloaded = ts.isUnloaded();
      int ucount = ts.getUnloadCont();
      if (this.nativeLib == null) {
         List<RoutingConfiguration.DirectionPoint> points = Collections.emptyList();
         if (this.config.getDirectionPoints() != null) {
            points = this.config
               .getDirectionPoints()
               .queryInBox(
                  new QuadRect((double)ts.subregion.left, (double)ts.subregion.top, (double)ts.subregion.right, (double)ts.subregion.bottom),
                  new ArrayList()
               );
            int createType = ts.subregion.routeReg.findOrCreateRouteType("osmand_dp", "osmand_add_point");

            for(RoutingConfiguration.DirectionPoint d : points) {
               d.types.clear();

               for(Entry<String, String> e : d.getTags().entrySet()) {
                  int type = ts.subregion.routeReg.searchRouteEncodingRule(e.getKey(), e.getValue());
                  if (type != -1) {
                     d.types.add(type);
                  }
               }

               d.types.add(createType);
            }
         }

         try {
            BinaryMapIndexReader reader = this.reverseMap.get(ts.subregion.routeReg);
            ts.setLoadedNonNative();
            List<RouteDataObject> res = reader.loadRouteIndexData(ts.subregion);
            if (toLoad != null) {
               toLoad.addAll(res);
            } else {
               for(RouteDataObject ro : res) {
                  if (ro != null) {
                     if (this.config.routeCalculationTime != 0L) {
                        ro.processConditionalTags(this.config.routeCalculationTime);
                     }

                     if (this.config.router.acceptLine(ro) && excludeNotAllowed != null && !excludeNotAllowed.contains(ro.getId())) {
                        if (!this.config.router.attributes.containsKey("check_allow_private_needed")) {
                           this.connectPoint(ts, ro, points);
                        }

                        ts.add(ro);
                     }

                     if (excludeNotAllowed != null && ro.getId() > 0L) {
                        excludeNotAllowed.add(ro.getId());
                        if (ts.excludedIds == null) {
                           ts.excludedIds = new TLongHashSet();
                        }

                        ts.excludedIds.add(ro.getId());
                     }
                  }
               }
            }
         } catch (IOException var16) {
            throw new RuntimeException("Loading data exception", var16);
         }
      } else {
         NativeLibrary.NativeRouteSearchResult ns = this.nativeLib.loadRouteRegion(ts.subregion, loadObjectsInMemory);
         ts.setLoadedNative(ns, this);
      }

      if (this.calculationProgress != null) {
         ++this.calculationProgress.loadedTiles;
      }

      if (wasUnloaded) {
         if (ucount == 1 && this.calculationProgress != null) {
            ++this.calculationProgress.loadedPrevUnloadedTiles;
         }
      } else {
         if (this.global != null) {
            this.global.allRoutes += ts.tileStatistics.allRoutes;
            this.global.coordinates += ts.tileStatistics.coordinates;
         }

         if (this.calculationProgress != null) {
            ++this.calculationProgress.distinctLoadedTiles;
         }
      }

      this.global.size += ts.tileStatistics.size;
      if (this.calculationProgress != null) {
         this.calculationProgress.timeToLoad += System.nanoTime() - now;
      }
   }

   public List<RoutingContext.RoutingSubregionTile> loadAllSubregionTiles(BinaryMapIndexReader reader, BinaryMapRouteReaderAdapter.RouteSubregion reg) throws IOException {
      List<RoutingContext.RoutingSubregionTile> list = new ArrayList<>();
      BinaryMapIndexReader.SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(
         0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null
      );

      for(BinaryMapRouteReaderAdapter.RouteSubregion s : reader.searchRouteIndexTree(request, Collections.singletonList(reg))) {
         list.add(new RoutingContext.RoutingSubregionTile(s));
      }

      return list;
   }

   public List<RoutingContext.RoutingSubregionTile> loadTileHeaders(int x31, int y31) {
      int zoomToLoad = 31 - this.config.ZOOM_TO_LOAD_TILES;
      int tileX = x31 >> zoomToLoad;
      int tileY = y31 >> zoomToLoad;
      long now = System.nanoTime();
      BinaryMapIndexReader.SearchRequest<RouteDataObject> request = BinaryMapIndexReader.buildSearchRouteRequest(
         tileX << zoomToLoad, tileX + 1 << zoomToLoad, tileY << zoomToLoad, tileY + 1 << zoomToLoad, null
      );
      List<RoutingContext.RoutingSubregionTile> collection = null;

      for(Entry<BinaryMapIndexReader, List<BinaryMapRouteReaderAdapter.RouteSubregion>> r : this.map.entrySet()) {
         try {
            boolean intersect = false;

            for(BinaryMapRouteReaderAdapter.RouteSubregion rs : r.getValue()) {
               if (request.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
                  intersect = true;
                  break;
               }
            }

            if (intersect) {
               for(BinaryMapRouteReaderAdapter.RouteSubregion sr : r.getKey().searchRouteIndexTree(request, r.getValue())) {
                  int ind = this.searchSubregionTile(sr);
                  RoutingContext.RoutingSubregionTile found;
                  if (ind < 0) {
                     found = new RoutingContext.RoutingSubregionTile(sr);
                     this.subregionTiles.add(-(ind + 1), found);
                  } else {
                     found = this.subregionTiles.get(ind);
                  }

                  if (collection == null) {
                     collection = new ArrayList<>(4);
                  }

                  collection.add(found);
               }
            }
         } catch (IOException var18) {
            throw new RuntimeException("Loading data exception", var18);
         }
      }

      if (this.calculationProgress != null) {
         this.calculationProgress.timeToLoadHeaders += System.nanoTime() - now;
      }

      return collection;
   }

   public void loadTileData(int x31, int y31, int zoomAround, List<RouteDataObject> toFillIn) {
      this.loadTileData(x31, y31, zoomAround, toFillIn, false);
   }

   public void loadTileData(int x31, int y31, int zoomAround, List<RouteDataObject> toFillIn, boolean allowDuplications) {
      int t = this.config.ZOOM_TO_LOAD_TILES - zoomAround;
      int coordinatesShift = 1 << 31 - this.config.ZOOM_TO_LOAD_TILES;
      if (t <= 0) {
         t = 1;
         coordinatesShift = 1 << 31 - zoomAround;
      } else {
         t = 1 << t;
      }

      TLongHashSet ts = new TLongHashSet();

      for(int i = -t; i <= t; ++i) {
         for(int j = -t; j <= t; ++j) {
            ts.add(this.getRoutingTile(x31 + i * coordinatesShift, y31 + j * coordinatesShift, 0L));
         }
      }

      TLongIterator it = ts.iterator();
      TLongObjectHashMap<RouteDataObject> excludeDuplications = new TLongObjectHashMap();

      while(it.hasNext()) {
         this.getAllObjects(it.next(), toFillIn, excludeDuplications);
         if (allowDuplications) {
            excludeDuplications.clear();
         }
      }
   }

   private long getRoutingTile(int x31, int y31, long memoryLimit) {
      int zmShift = 31 - this.config.ZOOM_TO_LOAD_TILES;
      long xloc = (long)(x31 >> zmShift);
      long yloc = (long)(y31 >> zmShift);
      long tileId = (xloc << this.config.ZOOM_TO_LOAD_TILES) + yloc;
      if (memoryLimit == 0L) {
         memoryLimit = this.config.memoryLimitation;
      }

      if ((double)this.getCurrentEstimatedSize() > 0.9 * (double)memoryLimit) {
         int sz1 = this.getCurrentEstimatedSize();
         long h1 = 0L;
         if (SHOW_GC_SIZE && (double)sz1 > 0.7 * (double)memoryLimit) {
            runGCUsedMemory();
            h1 = runGCUsedMemory();
         }

         int clt = this.getCurrentlyLoadedTiles();
         long us1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
         this.unloadUnusedTiles(memoryLimit);
         if (h1 != 0L && this.getCurrentlyLoadedTiles() != clt) {
            int sz2 = this.getCurrentEstimatedSize();
            runGCUsedMemory();
            long h2 = runGCUsedMemory();
            float mb = 1048576.0F;
            log.warn("Unload tiles :  estimated " + (float)(sz1 - sz2) / mb + " ?= " + (float)(h1 - h2) / mb + " actual");
            log.warn("Used after " + (float)h2 / mb + " of " + (float)Runtime.getRuntime().totalMemory() / mb);
         } else {
            float mb = 1048576.0F;
            int sz2 = this.getCurrentEstimatedSize();
            log.warn(
               "Unload tiles :  occupied before "
                  + (float)sz1 / mb
                  + " Mb - now  "
                  + (float)sz2 / mb
                  + "MB "
                  + (float)memoryLimit / mb
                  + " limit MB "
                  + (float)this.config.memoryLimitation / mb
            );
            long us2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            log.warn("Used memory before " + (float)us1 / mb + "after " + (float)us1 / mb);
         }
      }

      if (!this.indexedSubregions.containsKey(tileId)) {
         List<RoutingContext.RoutingSubregionTile> collection = this.loadTileHeaders(x31, y31);
         this.indexedSubregions.put(tileId, collection);
      }

      List<RoutingContext.RoutingSubregionTile> subregions = (List)this.indexedSubregions.get(tileId);
      if (subregions != null) {
         boolean load = false;

         for(RoutingContext.RoutingSubregionTile ts : subregions) {
            if (!ts.isLoaded()) {
               load = true;
            }
         }

         if (load) {
            TLongHashSet excludeIds = new TLongHashSet();

            for(RoutingContext.RoutingSubregionTile ts : subregions) {
               if (!ts.isLoaded()) {
                  this.loadSubregionTile(ts, true, null, excludeIds);
               } else if (ts.excludedIds != null) {
                  excludeIds.addAll(ts.excludedIds);
               }
            }
         }
      }

      return tileId;
   }

   private void connectPoint(RoutingContext.RoutingSubregionTile ts, RouteDataObject ro, List<RoutingConfiguration.DirectionPoint> points) {
      int createType = ro.region.findOrCreateRouteType("osmand_dp", "osmand_add_point");
      int deleteType = ro.region.findOrCreateRouteType("osmand_dp", "osmand_delete_point");

      for(RoutingConfiguration.DirectionPoint np : points) {
         if (np.types.size() != 0) {
            int wptX = MapUtils.get31TileNumberX(np.getLongitude());
            int wptY = MapUtils.get31TileNumberY(np.getLatitude());
            int x = ro.getPoint31XTile(0);
            int y = ro.getPoint31YTile(0);
            double mindist = (double)(this.config.directionPointsRadius * 2);
            int indexToInsert = 0;
            int mprojx = 0;
            int mprojy = 0;

            for(int i = 1; i < ro.getPointsLength(); ++i) {
               int nx = ro.getPoint31XTile(i);
               int ny = ro.getPoint31YTile(i);
               boolean sgnx = nx - wptX > 0;
               boolean sgx = x - wptX > 0;
               boolean sgny = ny - wptY > 0;
               boolean sgy = y - wptY > 0;
               boolean checkPreciseProjection = true;
               if (sgny == sgy && sgx == sgnx) {
                  double dist = MapUtils.squareRootDist31(
                     wptX, wptY, Math.abs(nx - wptX) < Math.abs(x - wptX) ? nx : x, Math.abs(ny - wptY) < Math.abs(y - wptY) ? ny : y
                  );
                  checkPreciseProjection = dist < (double)this.config.directionPointsRadius;
               }

               if (checkPreciseProjection) {
                  QuadPoint pnt = MapUtils.getProjectionPoint31(wptX, wptY, x, y, nx, ny);
                  int projx = (int)pnt.x;
                  int projy = (int)pnt.y;
                  double dist = MapUtils.squareRootDist31(wptX, wptY, projx, projy);
                  if (dist < mindist) {
                     indexToInsert = i;
                     mindist = dist;
                     mprojx = projx;
                     mprojy = projy;
                  }
               }

               x = nx;
               y = ny;
            }

            boolean sameRoadId = np.connected != null && np.connected.getId() == ro.getId();
            boolean pointShouldBeAttachedByDist = mindist < (double)this.config.directionPointsRadius && mindist < np.distance;
            double npAngle = np.getAngle();
            boolean restrictionByAngle = !Double.isNaN(npAngle);
            if (pointShouldBeAttachedByDist) {
               if (restrictionByAngle) {
                  int oneWay = ro.getOneway();
                  double forwardAngle = Math.toDegrees(ro.directionRoute(indexToInsert, true));
                  if (oneWay == 1 || oneWay == 0) {
                     double diff = Math.abs(MapUtils.degreesDiff(npAngle, forwardAngle));
                     if (diff <= 45.0) {
                        restrictionByAngle = false;
                     }
                  }

                  if (restrictionByAngle && (oneWay == -1 || oneWay == 0)) {
                     double diff = Math.abs(MapUtils.degreesDiff(npAngle, forwardAngle + 180.0));
                     if (diff <= 45.0) {
                        restrictionByAngle = false;
                     }
                  }
               }

               if (!restrictionByAngle) {
                  if (!sameRoadId) {
                     if (np.connected != null) {
                        int pointIndex = this.findPointIndex(np, createType);
                        if (pointIndex == -1) {
                           throw new RuntimeException();
                        }

                        np.connected.setPointTypes(pointIndex, new int[]{deleteType});
                     }
                  } else {
                     int sameRoadPointIndex = this.findPointIndex(np, createType);
                     if (sameRoadPointIndex != -1 && np.connected != null) {
                        if (mprojx == np.connectedx && mprojy == np.connectedy) {
                           continue;
                        }

                        np.connected.setPointTypes(sameRoadPointIndex, new int[]{deleteType});
                     }
                  }

                  np.connectedx = mprojx;
                  np.connectedy = mprojy;
                  ro.insert(indexToInsert, mprojx, mprojy);
                  ro.setPointTypes(indexToInsert, np.types.toArray());
                  np.distance = mindist;
                  np.connected = ro;
               }
            }
         }
      }
   }

   private int findPointIndex(RoutingConfiguration.DirectionPoint np, int createType) {
      int samePointIndex = -1;

      for(int i = 0; np.connected != null && i < np.connected.getPointsLength(); ++i) {
         int tx = np.connected.getPoint31XTile(i);
         int ty = np.connected.getPoint31YTile(i);
         if (tx == np.connectedx && ty == np.connectedy && np.connected.hasPointType(i, createType)) {
            samePointIndex = i;
            break;
         }
      }

      return samePointIndex;
   }

   public boolean checkIfMemoryLimitCritical(long memoryLimit) {
      return (double)this.getCurrentEstimatedSize() > 0.9 * (double)memoryLimit;
   }

   public void unloadUnusedTiles(long memoryLimit) {
      float desirableSize = (float)memoryLimit * 0.7F;
      List<RoutingContext.RoutingSubregionTile> list = new ArrayList<>(this.subregionTiles.size() / 2);
      int loaded = 0;

      for(RoutingContext.RoutingSubregionTile t : this.subregionTiles) {
         if (t.isLoaded()) {
            list.add(t);
            ++loaded;
         }
      }

      if (this.calculationProgress != null) {
         this.calculationProgress.maxLoadedTiles = Math.max(this.calculationProgress.maxLoadedTiles, this.getCurrentlyLoadedTiles());
      }

      Collections.sort(list, new Comparator<RoutingContext.RoutingSubregionTile>() {
         private int pow(int base, int pw) {
            int r = 1;

            for(int i = 0; i < pw; ++i) {
               r *= base;
            }

            return r;
         }

         public int compare(RoutingContext.RoutingSubregionTile o1, RoutingContext.RoutingSubregionTile o2) {
            int v1 = (o1.access + 1) * this.pow(10, o1.getUnloadCont() - 1);
            int v2 = (o2.access + 1) * this.pow(10, o2.getUnloadCont() - 1);
            return v1 < v2 ? -1 : (v1 == v2 ? 0 : 1);
         }
      });

      RoutingContext.RoutingSubregionTile unload;
      for(int i = 0;
         (float)this.getCurrentEstimatedSize() >= desirableSize && list.size() - i > loaded / 5 && i < list.size();
         this.global.size -= unload.tileStatistics.size
      ) {
         unload = list.get(i);
         ++i;
         unload.unload();
         if (this.calculationProgress != null) {
            ++this.calculationProgress.unloadedTiles;
         }
      }

      for(RoutingContext.RoutingSubregionTile t : this.subregionTiles) {
         t.access /= 3;
      }
   }

   private void getAllObjects(long tileId, List<RouteDataObject> toFillIn, TLongObjectHashMap<RouteDataObject> excludeDuplications) {
      List<RoutingContext.RoutingSubregionTile> subregions = (List)this.indexedSubregions.get(tileId);
      if (subregions != null) {
         for(RoutingContext.RoutingSubregionTile rs : subregions) {
            rs.loadAllObjects(toFillIn, this, excludeDuplications);
         }
      }
   }

   protected static long runGCUsedMemory() {
      Runtime runtime = Runtime.getRuntime();
      long usedMem1 = runtime.totalMemory() - runtime.freeMemory();
      long usedMem2 = Long.MAX_VALUE;
      int cnt = 4;

      while(cnt-- >= 0) {
         for(int i = 0; usedMem1 < usedMem2 && i < 1000; ++i) {
            runtime.runFinalization();
            runtime.gc();
            Thread.yield();
            usedMem2 = usedMem1;
            usedMem1 = runtime.totalMemory() - runtime.freeMemory();
         }
      }

      return usedMem1;
   }

   private static long calcRouteId(RouteDataObject o, int ind) {
      return (o.getId() << 10) + (long)ind;
   }

   static int getEstimatedSize(RouteDataObject o) {
      int sz = 0;
      sz += 12;
      if (o.names != null) {
         sz += 12;

         String vl;
         for(TIntObjectIterator<String> it = o.names.iterator(); it.hasNext(); sz += 12 + vl.length()) {
            it.advance();
            vl = (String)it.value();
         }

         sz += 12 + o.names.size() * 25;
      }

      sz += 8;
      sz += (12 + 4 * o.getPointsLength()) * 4;
      sz += o.types == null ? 4 : 12 + 4 * o.types.length;
      sz += o.restrictions == null ? 4 : 12 + 8 * o.restrictions.length;
      sz += 4;
      if (o.pointTypes != null) {
         sz += 8 + 4 * o.pointTypes.length;

         for(int i = 0; i < o.pointTypes.length; ++i) {
            sz += 4;
            if (o.pointTypes[i] != null) {
               sz += 8 + 8 * o.pointTypes[i].length;
            }
         }
      }

      return (int)((double)sz * 3.5);
   }

   public BinaryMapIndexReader[] getMaps() {
      return this.map.keySet().toArray(new BinaryMapIndexReader[0]);
   }

   public int getVisitedSegments() {
      return this.calculationProgress != null ? this.calculationProgress.visitedSegments : 0;
   }

   public int getLoadedTiles() {
      return this.calculationProgress != null ? this.calculationProgress.loadedTiles : 0;
   }

   public synchronized void deleteNativeRoutingContext() {
      if (this.nativeRoutingContext != 0L) {
         NativeLibrary.deleteNativeRoutingContext(this.nativeRoutingContext);
      }

      this.nativeRoutingContext = 0L;
   }

   @Override
   protected void finalize() throws Throwable {
      this.deleteNativeRoutingContext();
      super.finalize();
   }

   public static class RoutingSubregionTile {
      public final BinaryMapRouteReaderAdapter.RouteSubregion subregion;
      public int access;
      public RoutingContext.TileStatistics tileStatistics = new RoutingContext.TileStatistics();
      private NativeLibrary.NativeRouteSearchResult searchResult = null;
      private int isLoaded = 0;
      private TLongObjectMap<BinaryRoutePlanner.RouteSegment> routes = null;
      private TLongHashSet excludedIds = null;

      public RoutingSubregionTile(BinaryMapRouteReaderAdapter.RouteSubregion subregion) {
         this.subregion = subregion;
      }

      public TLongObjectMap<BinaryRoutePlanner.RouteSegment> getRoutes() {
         return this.routes;
      }

      public void loadAllObjects(List<RouteDataObject> toFillIn, RoutingContext ctx, TLongObjectHashMap<RouteDataObject> excludeDuplications) {
         if (this.routes != null) {
            Iterator<BinaryRoutePlanner.RouteSegment> it = this.routes.valueCollection().iterator();

            while(it.hasNext()) {
               for(BinaryRoutePlanner.RouteSegment rs = it.next(); rs != null; rs = rs.nextLoaded) {
                  RouteDataObject ro = rs.road;
                  if (!excludeDuplications.contains(ro.id)) {
                     excludeDuplications.put(ro.id, ro);
                     toFillIn.add(ro);
                  }
               }
            }
         } else if (this.searchResult != null) {
            RouteDataObject[] objects = this.searchResult.objects;
            if (objects != null) {
               for(RouteDataObject ro : objects) {
                  if (ro != null && !excludeDuplications.contains(ro.id)) {
                     excludeDuplications.put(ro.id, ro);
                     toFillIn.add(ro);
                  }
               }
            }
         }
      }

      private BinaryRoutePlanner.RouteSegment loadRouteSegment(
         int x31,
         int y31,
         RoutingContext ctx,
         TLongObjectHashMap<RouteDataObject> excludeDuplications,
         BinaryRoutePlanner.RouteSegment original,
         List<RoutingContext.RoutingSubregionTile> subregions,
         int subregionIndex,
         boolean reverseWaySearch
      ) {
         ++this.access;
         if (this.routes == null) {
            throw new UnsupportedOperationException("Not clear how it could be used with native");
         } else {
            long l = ((long)x31 << 31) + (long)y31;

            for(BinaryRoutePlanner.RouteSegment segment = (BinaryRoutePlanner.RouteSegment)this.routes.get(l); segment != null; segment = segment.nextLoaded) {
               RouteDataObject ro = segment.road;
               RouteDataObject toCmp = (RouteDataObject)excludeDuplications.get(RoutingContext.calcRouteId(ro, segment.getSegmentStart()));
               if (!isExcluded(ro.id, subregions, subregionIndex) && (toCmp == null || toCmp.getPointsLength() < ro.getPointsLength())) {
                  excludeDuplications.put(RoutingContext.calcRouteId(ro, segment.getSegmentStart()), ro);
                  if (reverseWaySearch) {
                     if (segment.reverseSearch == null) {
                        segment.reverseSearch = new BinaryRoutePlanner.RouteSegment(ro, segment.getSegmentStart());
                        segment.reverseSearch.reverseSearch = segment;
                        segment.reverseSearch.nextLoaded = segment.nextLoaded;
                     }

                     segment = segment.reverseSearch;
                  }

                  segment.next = original;
                  original = segment;
               }
            }

            return original;
         }
      }

      private static boolean isExcluded(long id, List<RoutingContext.RoutingSubregionTile> subregions, int subregionIndex) {
         for(int i = 0; i < subregionIndex; ++i) {
            if (subregions.get(i).excludedIds != null && subregions.get(i).excludedIds.contains(id)) {
               return true;
            }
         }

         return false;
      }

      public boolean isLoaded() {
         return this.isLoaded > 0;
      }

      public int getUnloadCont() {
         return Math.abs(this.isLoaded);
      }

      public boolean isUnloaded() {
         return this.isLoaded < 0;
      }

      public void unload() {
         if (this.isLoaded == 0) {
            this.isLoaded = -1;
         } else {
            this.isLoaded = -Math.abs(this.isLoaded);
         }

         if (this.searchResult != null) {
            this.searchResult.deleteNativeResult();
         }

         this.searchResult = null;
         this.routes = null;
         this.excludedIds = null;
      }

      public void setLoadedNonNative() {
         this.isLoaded = Math.abs(this.isLoaded) + 1;
         this.routes = new TLongObjectHashMap();
         this.tileStatistics = new RoutingContext.TileStatistics();
      }

      public void add(RouteDataObject ro) {
         this.tileStatistics.addObject(ro);

         for(int i = 0; i < ro.pointsX.length; ++i) {
            int x31 = ro.getPoint31XTile(i);
            int y31 = ro.getPoint31YTile(i);
            long l = ((long)x31 << 31) + (long)y31;
            BinaryRoutePlanner.RouteSegment segment = new BinaryRoutePlanner.RouteSegment(ro, i);
            if (!this.routes.containsKey(l)) {
               this.routes.put(l, segment);
            } else {
               BinaryRoutePlanner.RouteSegment orig = (BinaryRoutePlanner.RouteSegment)this.routes.get(l);

               while(orig.nextLoaded != null) {
                  orig = orig.nextLoaded;
               }

               orig.nextLoaded = segment;
            }
         }
      }

      public void setLoadedNative(NativeLibrary.NativeRouteSearchResult r, RoutingContext ctx) {
         this.isLoaded = Math.abs(this.isLoaded) + 1;
         this.tileStatistics = new RoutingContext.TileStatistics();
         if (r.objects != null) {
            this.searchResult = null;
            this.routes = new TLongObjectHashMap();

            for(RouteDataObject ro : r.objects) {
               if (ro != null && ctx.config.router.acceptLine(ro)) {
                  this.add(ro);
               }
            }
         } else {
            this.searchResult = r;
            this.tileStatistics.size += 100;
         }
      }
   }

   protected static class TileStatistics {
      public int size = 0;
      public int allRoutes = 0;
      public int coordinates = 0;

      @Override
      public String toString() {
         return "All routes "
            + this.allRoutes
            + " size "
            + (float)this.size / 1024.0F
            + " KB coordinates "
            + this.coordinates
            + " ratio coord "
            + (float)this.size / (float)this.coordinates
            + " ratio routes "
            + (float)this.size / (float)this.allRoutes;
      }

      public void addObject(RouteDataObject o) {
         ++this.allRoutes;
         this.coordinates += o.getPointsLength() * 2;
         this.size += RoutingContext.getEstimatedSize(o);
      }
   }
}
