package net.osmand.router.network;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteContext {
   public static final int ZOOM_TO_LOAD_TILES = 15;
   public static final int ZOOM_TO_LOAD_TILES_SHIFT_L = 16;
   public static final int ZOOM_TO_LOAD_TILES_SHIFT_R = 16;
   private final TLongObjectHashMap<NetworkRouteContext.NetworkRoutesTile> indexedTiles = new TLongObjectHashMap();
   private final NetworkRouteSelector.NetworkRouteSelectorFilter filter;
   private final Map<BinaryMapIndexReader, List<BinaryMapRouteReaderAdapter.RouteSubregion>> readers = new LinkedHashMap<>();
   private final Map<BinaryMapRouteReaderAdapter.RouteSubregion, List<RouteDataObject>> loadedSubregions = new HashMap<>();
   private final boolean routing;
   private NetworkRouteContext.NetworkRouteContextStats stats;

   public NetworkRouteContext(BinaryMapIndexReader[] readers, NetworkRouteSelector.NetworkRouteSelectorFilter filter, boolean routing) {
      this.filter = filter;
      this.routing = routing;
      this.stats = new NetworkRouteContext.NetworkRouteContextStats();

      for(BinaryMapIndexReader r : readers) {
         if (!routing) {
            this.readers.put(r, null);
         } else {
            List<BinaryMapRouteReaderAdapter.RouteSubregion> subregions = new ArrayList<>();

            for(BinaryMapRouteReaderAdapter.RouteRegion rInd : r.getRoutingIndexes()) {
               for(BinaryMapRouteReaderAdapter.RouteSubregion rs : rInd.getSubregions()) {
                  subregions.add(new BinaryMapRouteReaderAdapter.RouteSubregion(rs));
               }
            }

            this.readers.put(r, subregions);
         }
      }
   }

   public static long convertPointToLong(int x31, int y31) {
      return ((long)x31 << 32) + (long)y31;
   }

   public static int getXFromLong(long l) {
      return (int)(l >> 32);
   }

   public static int getYFromLong(long l) {
      return (int)(l - (l >> 32 << 32));
   }

   Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> loadRouteSegmentsBbox(
      int x31L, int y31T, int x31R, int y31B, NetworkRouteSelector.RouteKey rKey
   ) throws IOException {
      Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> map = new LinkedHashMap<>();
      int left = x31L >> 16;
      int right = x31R >> 16;
      int top = y31T >> 16;
      int bottom = y31B >> 16;

      for(int x = left; x <= right; ++x) {
         for(int y = top; y <= bottom; ++y) {
            this.loadRouteSegmentTile(x, y, rKey, map);
         }
      }

      return map;
   }

   Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> loadRouteSegmentTile(
      int x, int y, NetworkRouteSelector.RouteKey routeKey, Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> map
   ) throws IOException {
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = this.getMapRouteTile(x << 16, y << 16);

      for(NetworkRouteContext.NetworkRoutePoint pnt : osmcRoutesTile.getRoutes().valueCollection()) {
         for(NetworkRouteContext.NetworkRouteSegment segment : pnt.objects) {
            if (!this.loadOnlyRouteWithKey(routeKey) || segment.routeKey.equals(routeKey)) {
               List<NetworkRouteContext.NetworkRouteSegment> routeSegments = map.get(segment.routeKey);
               if (routeSegments == null) {
                  routeSegments = new ArrayList<>();
                  map.put(segment.routeKey, routeSegments);
               }

               if (segment.start == 0 || !this.loadOnlyRouteWithKey(routeKey)) {
                  routeSegments.add(segment);
               }
            }
         }
      }

      return map;
   }

   boolean loadOnlyRouteWithKey(NetworkRouteSelector.RouteKey rKey) {
      return rKey != null;
   }

   public List<NetworkRouteContext.NetworkRouteSegment> loadNearRouteSegment(int x31, int y31, double radius) throws IOException {
      List<NetworkRouteContext.NetworkRoutePoint> nearPoints = new ArrayList<>();
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = this.getMapRouteTile(x31, y31);
      double sqrRadius = radius * radius;

      for(NetworkRouteContext.NetworkRoutePoint pnt : osmcRoutesTile.getRoutes().valueCollection()) {
         double dist = MapUtils.squareDist31TileMetric(pnt.x31, pnt.y31, x31, y31);
         if (dist < sqrRadius) {
            pnt.localVar = dist;
            nearPoints.add(pnt);
         }
      }

      Collections.sort(nearPoints, new Comparator<NetworkRouteContext.NetworkRoutePoint>() {
         public int compare(NetworkRouteContext.NetworkRoutePoint o1, NetworkRouteContext.NetworkRoutePoint o2) {
            return Double.compare(o1.localVar, o2.localVar);
         }
      });
      if (nearPoints.size() == 0) {
         return Collections.emptyList();
      } else {
         List<NetworkRouteContext.NetworkRouteSegment> objs = new ArrayList<>();

         for(NetworkRouteContext.NetworkRoutePoint pnt : nearPoints) {
            objs.addAll(pnt.objects);
         }

         return objs;
      }
   }

   public List<NetworkRouteContext.NetworkRouteSegment> loadRouteSegment(int x31, int y31) throws IOException {
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = this.getMapRouteTile(x31, y31);
      NetworkRouteContext.NetworkRoutePoint point = osmcRoutesTile.getRouteSegment(x31, y31);
      return point == null ? Collections.<NetworkRouteSegment>emptyList() : point.objects;
   }

   public NetworkRouteContext.NetworkRoutePoint getClosestNetworkRoutePoint(int sx31, int sy31) throws IOException {
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = this.getMapRouteTile(sx31, sy31);
      double minDistance = Double.MAX_VALUE;
      NetworkRouteContext.NetworkRoutePoint nearPoint = null;

      for(NetworkRouteContext.NetworkRoutePoint pt : osmcRoutesTile.routes.valueCollection()) {
         double distance = MapUtils.squareRootDist31(sx31, sy31, pt.x31, pt.y31);
         if (distance < minDistance) {
            nearPoint = pt;
            minDistance = distance;
         }
      }

      return nearPoint;
   }

   private NetworkRouteContext.NetworkRoutesTile getMapRouteTile(int x31, int y31) throws IOException {
      long tileId = getTileId(x31, y31);
      NetworkRouteContext.NetworkRoutesTile tile = (NetworkRouteContext.NetworkRoutesTile)this.indexedTiles.get(tileId);
      if (tile == null) {
         tile = this.loadTile(x31 >> 16, y31 >> 16, tileId);
         this.indexedTiles.put(tileId, tile);
      }

      return tile;
   }

   public boolean isRouting() {
      return this.routing;
   }

   private NetworkRouteContext.NetworkRoutesTile loadTile(int x, int y, long tileId) throws IOException {
      ++this.stats.loadedTiles;
      if (this.routing) {
         BinaryMapIndexReader.SearchRequest<RouteDataObject> req = BinaryMapIndexReader.buildSearchRouteRequest(
            x << 16, x + 1 << 16, y << 16, y + 1 << 16, null
         );
         req.log = false;
         return this.loadRoutingDataTile(req, tileId);
      } else {
         BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
            x << 16, x + 1 << 16, y << 16, y + 1 << 16, 15, new BinaryMapIndexReader.SearchFilter() {
               @Override
               public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
                  return true;
               }
            }, null
         );
         req.log = false;
         return this.loadMapDataTile(req, tileId);
      }
   }

   private NetworkRouteContext.NetworkRoutesTile loadRoutingDataTile(BinaryMapIndexReader.SearchRequest<RouteDataObject> req, long tileId) throws IOException {
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = new NetworkRouteContext.NetworkRoutesTile(tileId);

      for(Entry<BinaryMapIndexReader, List<BinaryMapRouteReaderAdapter.RouteSubregion>> reader : this.readers.entrySet()) {
         req.clearSearchResults();
         long nt = System.nanoTime();
         List<BinaryMapRouteReaderAdapter.RouteSubregion> subregions = reader.getKey().searchRouteIndexTree(req, reader.getValue());
         this.stats.loadTimeNs += System.nanoTime() - nt;

         for(BinaryMapRouteReaderAdapter.RouteSubregion sub : subregions) {
            List<RouteDataObject> objects = this.loadedSubregions.get(sub);
            if (objects == null) {
               nt = System.nanoTime();
               objects = reader.getKey().loadRouteIndexData(sub);
               this.loadedSubregions.put(sub, objects);
               this.stats.loadTimeNs += System.nanoTime() - nt;
            }

            for(RouteDataObject obj : objects) {
               if (obj != null) {
                  ++this.stats.loadedObjects;

                  for(NetworkRouteSelector.RouteKey rk : this.filter.convert(obj)) {
                     ++this.stats.loadedRoutes;
                     osmcRoutesTile.add(obj, rk);
                  }
               }
            }
         }
      }

      return osmcRoutesTile;
   }

   private NetworkRouteContext.NetworkRoutesTile loadMapDataTile(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, long tileId) throws IOException {
      NetworkRouteContext.NetworkRoutesTile osmcRoutesTile = new NetworkRouteContext.NetworkRoutesTile(tileId);

      for(BinaryMapIndexReader reader : this.readers.keySet()) {
         req.clearSearchResults();
         long nt = System.nanoTime();
         List<BinaryMapDataObject> objects = reader.searchMapIndex(req);
         this.stats.loadTimeNs += System.nanoTime() - nt;

         for(BinaryMapDataObject obj : objects) {
            ++this.stats.loadedObjects;

            for(NetworkRouteSelector.RouteKey rk : this.filter.convert(obj)) {
               ++this.stats.loadedRoutes;
               osmcRoutesTile.add(obj, rk);
            }
         }
      }

      return osmcRoutesTile;
   }

   public static int getXFromTileId(long tileId) {
      return (int)(tileId >> 16);
   }

   public static int getYFromTileId(long tileId) {
      long xShifted = tileId >> 16;
      return (int)(tileId - (xShifted << 16));
   }

   public static long getTileId(int x31, int y31) {
      return getTileId(x31, y31, 16);
   }

   public static long getTileId(int x, int y, int shiftR) {
      return ((long)x >> shiftR << 16) + (long)(y >> shiftR);
   }

   public NetworkRouteContext.NetworkRouteContextStats getStats() {
      return this.stats;
   }

   public void clearData() {
      this.indexedTiles.clear();
      this.loadedSubregions.clear();
      this.stats = new NetworkRouteContext.NetworkRouteContextStats();
   }

   public void clearStats() {
      this.stats = new NetworkRouteContext.NetworkRouteContextStats();
   }

   public static class NetworkRouteContextStats {
      public int loadedTiles;
      public int loadedObjects;
      public int loadedRoutes;
      public long loadTimeNs;
   }

   public static class NetworkRoutePoint {
      public final int x31;
      public final int y31;
      public final long id;
      public final List<NetworkRouteContext.NetworkRouteSegment> objects = new ArrayList<>();
      public double localVar;

      public NetworkRoutePoint(int x31, int y31, long id) {
         this.x31 = x31;
         this.y31 = y31;
         this.id = id;
      }

      public void addObject(NetworkRouteContext.NetworkRouteSegment obj) {
         if (obj.getId() > 0L) {
            for(NetworkRouteContext.NetworkRouteSegment obj2 : this.objects) {
               if (obj.getId() == obj2.getId() && obj.direction() == obj2.direction() && Algorithms.objectEquals(obj.routeKey, obj2.routeKey)) {
                  return;
               }
            }
         }

         this.objects.add(obj);
      }
   }

   public static class NetworkRouteSegment {
      public final int start;
      public final int end;
      public final BinaryMapDataObject obj;
      public final RouteDataObject robj;
      public final NetworkRouteSelector.RouteKey routeKey;

      public NetworkRouteSegment(BinaryMapDataObject obj, NetworkRouteSelector.RouteKey routeKey, int start, int end) {
         this.robj = null;
         this.obj = obj;
         this.start = start;
         this.end = end;
         this.routeKey = routeKey;
      }

      public NetworkRouteSegment(NetworkRouteContext.NetworkRouteSegment segment, int start, int end) {
         this.robj = segment.robj;
         this.obj = segment.obj;
         this.start = start;
         this.end = end;
         this.routeKey = segment.routeKey;
      }

      public NetworkRouteSegment(RouteDataObject obj, NetworkRouteSelector.RouteKey routeKey, int start, int end) {
         this.robj = obj;
         this.obj = null;
         this.start = start;
         this.end = end;
         this.routeKey = routeKey;
      }

      public boolean direction() {
         return this.end > this.start;
      }

      public long getId() {
         return this.robj != null ? this.robj.getId() : this.obj.getId();
      }

      public int getPointsLength() {
         return this.robj != null ? this.robj.getPointsLength() : this.obj.getPointsLength();
      }

      public int getPoint31XTile(int i) {
         return this.robj != null ? this.robj.getPoint31XTile(i) : this.obj.getPoint31XTile(i);
      }

      public int getPoint31YTile(int i) {
         return this.robj != null ? this.robj.getPoint31YTile(i) : this.obj.getPoint31YTile(i);
      }

      public String getRouteName() {
         String name = this.routeKey.getValue("name");
         if (name.isEmpty()) {
            name = this.routeKey.getValue("ref");
         }

         if (!name.isEmpty()) {
            return name;
         } else {
            return this.robj != null ? this.robj.getName() : this.obj.getName();
         }
      }

      public int getStartPointX() {
         return this.getPoint31XTile(this.start);
      }

      public int getStartPointY() {
         return this.getPoint31YTile(this.start);
      }

      public int getEndPointX() {
         return this.getPoint31XTile(this.end);
      }

      public int getEndPointY() {
         return this.getPoint31YTile(this.end);
      }

      @Override
      public String toString() {
         return "NetworkRouteObject [start="
            + this.start
            + ", end="
            + this.end
            + ", obj="
            + (this.robj != null ? this.robj : this.obj)
            + ", routeKey="
            + this.routeKey
            + "]";
      }

      public NetworkRouteContext.NetworkRouteSegment inverse() {
         return new NetworkRouteContext.NetworkRouteSegment(this, this.end, this.start);
      }
   }

   private static class NetworkRoutesTile {
      private final TLongObjectMap<NetworkRouteContext.NetworkRoutePoint> routes = new TLongObjectHashMap();
      private final long tileId;

      public NetworkRoutesTile(long tileId) {
         this.tileId = tileId;
      }

      public void add(BinaryMapDataObject obj, NetworkRouteSelector.RouteKey rk) {
         int len = obj.getPointsLength();

         for(int i = 0; i < len; ++i) {
            int x31 = obj.getPoint31XTile(i);
            int y31 = obj.getPoint31YTile(i);
            if (NetworkRouteContext.getTileId(x31, y31) == this.tileId) {
               long id = NetworkRouteContext.convertPointToLong(x31, y31);
               NetworkRouteContext.NetworkRoutePoint point = (NetworkRouteContext.NetworkRoutePoint)this.routes.get(id);
               if (point == null) {
                  point = new NetworkRouteContext.NetworkRoutePoint(x31, y31, id);
                  this.routes.put(id, point);
               }

               if (i > 0) {
                  point.addObject(new NetworkRouteContext.NetworkRouteSegment(obj, rk, i, 0));
               }

               if (i < len - 1) {
                  point.addObject(new NetworkRouteContext.NetworkRouteSegment(obj, rk, i, len - 1));
               }
            }
         }
      }

      public void add(RouteDataObject obj, NetworkRouteSelector.RouteKey rk) {
         int len = obj.getPointsLength();

         for(int i = 0; i < len; ++i) {
            int x31 = obj.getPoint31XTile(i);
            int y31 = obj.getPoint31YTile(i);
            if (NetworkRouteContext.getTileId(x31, y31) == this.tileId) {
               long id = NetworkRouteContext.convertPointToLong(x31, y31);
               NetworkRouteContext.NetworkRoutePoint point = (NetworkRouteContext.NetworkRoutePoint)this.routes.get(id);
               if (point == null) {
                  point = new NetworkRouteContext.NetworkRoutePoint(x31, y31, id);
                  this.routes.put(id, point);
               }

               if (i > 0) {
                  point.addObject(new NetworkRouteContext.NetworkRouteSegment(obj, rk, i, 0));
               }

               if (i < len - 1) {
                  point.addObject(new NetworkRouteContext.NetworkRouteSegment(obj, rk, i, len - 1));
               }
            }
         }
      }

      public TLongObjectMap<NetworkRouteContext.NetworkRoutePoint> getRoutes() {
         return this.routes;
      }

      public NetworkRouteContext.NetworkRoutePoint getRouteSegment(int x31, int y31) {
         if (NetworkRouteContext.getTileId(x31, y31) != this.tileId) {
            System.err.println(String.format("Wrong tile id !!! %d != %d", NetworkRouteContext.getTileId(x31, y31), this.tileId));
         }

         return (NetworkRouteContext.NetworkRoutePoint)this.routes.get(NetworkRouteContext.convertPointToLong(x31, y31));
      }
   }
}
