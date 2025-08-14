package net.osmand.router.network;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.osmand.GPXUtilities;
import net.osmand.NativeLibrary;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class NetworkRouteSelector {
   public static final String ROUTE_KEY_VALUE_SEPARATOR = "__";
   public static final String NETWORK_ROUTE_TYPE = "type";
   private static final boolean GROW_ALGORITHM = false;
   private static final int MAX_ITERATIONS = 16000;
   private static final double MAX_RADIUS_HOLE = 30.0;
   private static final int CONNECT_POINTS_DISTANCE_STEP = 50;
   private static final int CONNECT_POINTS_DISTANCE_MAX = 1000;
   private final NetworkRouteContext rCtx;
   private final NetworkRouteSelector.INetworkRouteSelection callback;

   public NetworkRouteSelector(
      BinaryMapIndexReader[] files, NetworkRouteSelector.NetworkRouteSelectorFilter filter, NetworkRouteSelector.INetworkRouteSelection callback
   ) {
      this(files, filter, callback, true);
   }

   public NetworkRouteSelector(
      BinaryMapIndexReader[] files,
      NetworkRouteSelector.NetworkRouteSelectorFilter filter,
      NetworkRouteSelector.INetworkRouteSelection callback,
      boolean routing
   ) {
      if (filter == null) {
         filter = new NetworkRouteSelector.NetworkRouteSelectorFilter();
      }

      this.rCtx = new NetworkRouteContext(files, filter, routing);
      this.callback = callback;
   }

   public NetworkRouteContext getNetworkRouteContext() {
      return this.rCtx;
   }

   public boolean isCancelled() {
      return this.callback != null && this.callback.isCancelled();
   }

   public Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> getRoutes(NativeLibrary.RenderedObject renderedObject) throws IOException {
      int x = renderedObject.getX().get(0);
      int y = renderedObject.getY().get(0);
      return this.getRoutes(x, y, true);
   }

   public Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> getRoutes(NativeLibrary.RenderedObject renderedObject, boolean loadRoutes) throws IOException {
      int x = renderedObject.getX().get(0);
      int y = renderedObject.getY().get(0);
      return this.getRoutes(x, y, loadRoutes);
   }

   public Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> getRoutes(int x, int y, boolean loadRoutes) throws IOException {
      Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> res = new LinkedHashMap<>();

      for(NetworkRouteContext.NetworkRouteSegment segment : this.rCtx.loadRouteSegment(x, y)) {
         if (!res.containsKey(segment.routeKey)) {
            if (loadRoutes) {
               this.connectAlgorithm(segment, res);
            } else {
               res.put(segment.routeKey, null);
            }
         }
      }

      return res;
   }

   public Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> getRoutes(QuadRect bBox, boolean loadRoutes, NetworkRouteSelector.RouteKey selected) throws IOException {
      int y31T = MapUtils.get31TileNumberY(Math.max(bBox.bottom, bBox.top));
      int y31B = MapUtils.get31TileNumberY(Math.min(bBox.bottom, bBox.top));
      int x31L = MapUtils.get31TileNumberX(bBox.left);
      int x31R = MapUtils.get31TileNumberX(bBox.right);
      Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> routeSegmentTile = this.rCtx
         .loadRouteSegmentsBbox(x31L, y31T, x31R, y31B, null);
      Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> gpxFileMap = new LinkedHashMap<>();

      for(NetworkRouteSelector.RouteKey routeKey : routeSegmentTile.keySet()) {
         if (selected == null || selected.equals(routeKey)) {
            List<NetworkRouteContext.NetworkRouteSegment> routeSegments = routeSegmentTile.get(routeKey);
            if (routeSegments.size() > 0) {
               if (!loadRoutes) {
                  gpxFileMap.put(routeKey, null);
               } else {
                  NetworkRouteContext.NetworkRouteSegment firstSegment = routeSegments.get(0);
                  this.connectAlgorithm(firstSegment, gpxFileMap);
               }
            }
         }
      }

      return gpxFileMap;
   }

   private List<NetworkRouteSelector.NetworkRouteSegmentChain> getByPoint(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains, long pnt, int radius, NetworkRouteSelector.NetworkRouteSegmentChain exclude
   ) {
      List<NetworkRouteSelector.NetworkRouteSegmentChain> list = null;
      if (radius == 0) {
         list = chains.get(pnt);
         if (list != null) {
            if (!list.contains(exclude)) {
               return new ArrayList<>(list);
            }

            if (list.size() == 1) {
               list = null;
            } else {
               list = new ArrayList<>(list);
               list.remove(exclude);
            }
         }
      } else {
         int x = NetworkRouteContext.getXFromLong(pnt);
         int y = NetworkRouteContext.getYFromLong(pnt);

         for(Entry<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> e : chains.entrySet()) {
            int x2 = NetworkRouteContext.getXFromLong(e.getKey());
            int y2 = NetworkRouteContext.getYFromLong(e.getKey());
            if (MapUtils.squareRootDist31(x, y, x2, y2) < (double)radius) {
               if (list == null) {
                  list = new ArrayList<>();
               }

               for(NetworkRouteSelector.NetworkRouteSegmentChain c : e.getValue()) {
                  if (c != exclude) {
                     list.add(c);
                  }
               }
            }
         }
      }

      return list == null ? Collections.<NetworkRouteSegmentChain>emptyList() : list;
   }

   private void connectAlgorithm(NetworkRouteContext.NetworkRouteSegment segment, Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> res) throws IOException {
      NetworkRouteSelector.RouteKey rkey = segment.routeKey;
      List<NetworkRouteContext.NetworkRouteSegment> loaded = new ArrayList<>();
      this.debug("START ", null, segment);
      this.loadData(segment, rkey, loaded);
      List<NetworkRouteSelector.NetworkRouteSegmentChain> lst = this.getNetworkRouteSegmentChains(segment.routeKey, res, loaded);
      this.debug("FINISH " + lst.size(), null, segment);
   }

   List<NetworkRouteSelector.NetworkRouteSegmentChain> getNetworkRouteSegmentChains(
      NetworkRouteSelector.RouteKey routeKey,
      Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> res,
      List<NetworkRouteContext.NetworkRouteSegment> loaded
   ) {
      System.out.println("About to merge: " + loaded.size());
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains = this.createChainStructure(loaded);
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains = this.prepareEndChain(chains);
      this.connectSimpleMerge(chains, endChains, 0, 0);
      this.connectSimpleMerge(chains, endChains, 0, 50);

      for(int s = 0; s < 1000; s += 50) {
         this.connectSimpleMerge(chains, endChains, s, s + 50);
      }

      this.connectToLongestChain(chains, endChains, 50);
      this.connectSimpleMerge(chains, endChains, 0, 50);
      this.connectSimpleMerge(chains, endChains, 500, 1000);
      List<NetworkRouteSelector.NetworkRouteSegmentChain> lst = this.flattenChainStructure(chains);
      GPXUtilities.GPXFile gpxFile = this.createGpxFile(lst, routeKey);
      res.put(routeKey, gpxFile);
      return lst;
   }

   private int connectToLongestChain(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains, Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains, int rad
   ) {
      List<NetworkRouteSelector.NetworkRouteSegmentChain> chainsFlat = new ArrayList<>();

      for(List<NetworkRouteSelector.NetworkRouteSegmentChain> ch : chains.values()) {
         chainsFlat.addAll(ch);
      }

      Collections.sort(chainsFlat, new Comparator<NetworkRouteSelector.NetworkRouteSegmentChain>() {
         public int compare(NetworkRouteSelector.NetworkRouteSegmentChain o1, NetworkRouteSelector.NetworkRouteSegmentChain o2) {
            return -Integer.compare(o1.getSize(), o2.getSize());
         }
      });
      int mergedCount = 0;
      int i = 0;

      while(i < chainsFlat.size()) {
         NetworkRouteSelector.NetworkRouteSegmentChain first = chainsFlat.get(i);
         boolean merged = false;

         for(int j = i + 1; j < chainsFlat.size() && !merged && !this.isCancelled(); ++j) {
            NetworkRouteSelector.NetworkRouteSegmentChain second = chainsFlat.get(j);
            if (MapUtils.squareRootDist31(first.getEndPointX(), first.getEndPointY(), second.getEndPointX(), second.getEndPointY()) < (double)rad) {
               NetworkRouteSelector.NetworkRouteSegmentChain secondReversed = this.chainReverse(chains, endChains, second);
               this.chainAdd(chains, endChains, first, secondReversed);
               chainsFlat.remove(j);
               merged = true;
            } else if (MapUtils.squareRootDist31(
                  first.start.getStartPointX(), first.start.getStartPointY(), second.start.getStartPointX(), second.start.getStartPointY()
               )
               < (double)rad) {
               NetworkRouteSelector.NetworkRouteSegmentChain firstReversed = this.chainReverse(chains, endChains, first);
               this.chainAdd(chains, endChains, firstReversed, second);
               chainsFlat.remove(j);
               chainsFlat.set(i, firstReversed);
               merged = true;
            } else if (MapUtils.squareRootDist31(first.getEndPointX(), first.getEndPointY(), second.start.getStartPointX(), second.start.getStartPointY())
               < (double)rad) {
               this.chainAdd(chains, endChains, first, second);
               chainsFlat.remove(j);
               merged = true;
            } else if (MapUtils.squareRootDist31(second.getEndPointX(), second.getEndPointY(), first.start.getStartPointX(), first.start.getStartPointY())
               < (double)rad) {
               this.chainAdd(chains, endChains, second, first);
               chainsFlat.remove(i);
               merged = true;
            }
         }

         if (!merged) {
            ++i;
         } else {
            i = 0;
            ++mergedCount;
         }
      }

      System.out.println(String.format("Connect longest alternative chains: %d (radius %d)", mergedCount, rad));
      return mergedCount;
   }

   private int connectSimpleMerge(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains,
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains,
      int rad,
      int radE
   ) {
      int merged = 1;

      while(merged > 0 && !this.isCancelled()) {
         int rs = this.reverseToConnectMore(chains, endChains, rad, radE);
         merged = this.connectSimpleStraight(chains, endChains, rad, radE);
         System.out.println(String.format("Simple merged: %d, reversed: %d (radius %d %d)", merged, rs, rad, radE));
      }

      return merged;
   }

   private int reverseToConnectMore(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains,
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains,
      int rad,
      int radE
   ) {
      int reversed = 0;

      for(Long startPnt : new ArrayList<>(chains.keySet())) {
         List<NetworkRouteSelector.NetworkRouteSegmentChain> vls = chains.get(startPnt);

         for(int i = 0; vls != null && i < vls.size(); ++i) {
            NetworkRouteSelector.NetworkRouteSegmentChain it = vls.get(i);
            long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
            List<NetworkRouteSelector.NetworkRouteSegmentChain> startLst = this.getByPoint(chains, pnt, radE, null);
            boolean noStartFromEnd = this.filterChains(startLst, it, rad, true).size() == 0;
            boolean reverse = noStartFromEnd && vls.size() > 0;
            List<NetworkRouteSelector.NetworkRouteSegmentChain> endLst = this.getByPoint(endChains, pnt, radE, null);
            reverse |= i == 0 && this.filterChains(endLst, it, rad, false).size() > 1 && noStartFromEnd;
            if (reverse) {
               this.chainReverse(chains, endChains, it);
               ++reversed;
               break;
            }
         }
      }

      return reversed;
   }

   private List<NetworkRouteSelector.NetworkRouteSegmentChain> filterChains(
      List<NetworkRouteSelector.NetworkRouteSegmentChain> lst, NetworkRouteSelector.NetworkRouteSegmentChain ch, int rad, boolean start
   ) {
      if (lst.size() == 0) {
         return lst;
      } else {
         Iterator<NetworkRouteSelector.NetworkRouteSegmentChain> it = lst.iterator();

         while(it.hasNext()) {
            NetworkRouteSelector.NetworkRouteSegmentChain chain = it.next();
            double min = (double)(rad + 1);
            NetworkRouteContext.NetworkRouteSegment s = start ? chain.start : chain.getLast();
            NetworkRouteContext.NetworkRouteSegment last = ch.getLast();

            for(int i = 0; i < s.getPointsLength(); ++i) {
               for(int j = 0; j < last.getPointsLength(); ++j) {
                  double m = MapUtils.squareRootDist31(last.getPoint31XTile(j), last.getPoint31YTile(j), s.getPoint31XTile(i), s.getPoint31YTile(i));
                  if (m < min) {
                     min = m;
                  }
               }
            }

            if (min > (double)rad) {
               it.remove();
            }
         }

         return lst;
      }
   }

   private int connectSimpleStraight(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains,
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains,
      int rad,
      int radE
   ) {
      int merged = 0;
      boolean changed = true;

      label38:
      while(changed && !this.isCancelled()) {
         changed = false;

         for(List<NetworkRouteSelector.NetworkRouteSegmentChain> lst : chains.values()) {
            for(NetworkRouteSelector.NetworkRouteSegmentChain it : lst) {
               long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
               List<NetworkRouteSelector.NetworkRouteSegmentChain> connectNextLst = this.getByPoint(chains, pnt, radE, it);
               connectNextLst = this.filterChains(connectNextLst, it, rad, true);
               List<NetworkRouteSelector.NetworkRouteSegmentChain> connectToEndLst = this.getByPoint(endChains, pnt, radE, it);
               connectToEndLst = this.filterChains(connectToEndLst, it, rad, false);
               if (connectToEndLst.size() > 0) {
                  connectToEndLst.removeAll(connectNextLst);
               }

               if (connectNextLst.size() == 1 && connectToEndLst.size() == 0) {
                  this.chainAdd(chains, endChains, it, connectNextLst.get(0));
                  changed = true;
                  ++merged;
                  continue label38;
               }
            }
         }
      }

      return merged;
   }

   private NetworkRouteSelector.NetworkRouteSegmentChain chainReverse(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains,
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains,
      NetworkRouteSelector.NetworkRouteSegmentChain it
   ) {
      long startPnt = NetworkRouteContext.convertPointToLong(it.start.getStartPointX(), it.start.getStartPointY());
      long pnt = NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY());
      List<NetworkRouteContext.NetworkRouteSegment> lst = new ArrayList<>();
      lst.add(0, it.start.inverse());
      if (it.connected != null) {
         for(NetworkRouteContext.NetworkRouteSegment s : it.connected) {
            lst.add(0, s.inverse());
         }
      }

      this.remove(chains, startPnt, it);
      this.remove(endChains, pnt, it);
      NetworkRouteSelector.NetworkRouteSegmentChain newChain = new NetworkRouteSelector.NetworkRouteSegmentChain();
      newChain.start = lst.remove(0);
      newChain.connected = lst;
      this.add(chains, NetworkRouteContext.convertPointToLong(newChain.start.getStartPointX(), newChain.start.getStartPointY()), newChain);
      this.add(endChains, NetworkRouteContext.convertPointToLong(newChain.getEndPointX(), newChain.getEndPointY()), newChain);
      return newChain;
   }

   private void chainAdd(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains,
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains,
      NetworkRouteSelector.NetworkRouteSegmentChain it,
      NetworkRouteSelector.NetworkRouteSegmentChain toAdd
   ) {
      if (it == toAdd) {
         throw new IllegalStateException();
      } else {
         this.remove(chains, NetworkRouteContext.convertPointToLong(toAdd.start.getStartPointX(), toAdd.start.getStartPointY()), toAdd);
         this.remove(endChains, NetworkRouteContext.convertPointToLong(toAdd.getEndPointX(), toAdd.getEndPointY()), toAdd);
         this.remove(endChains, NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY()), it);
         double minStartDist = MapUtils.squareRootDist31(it.getEndPointX(), it.getEndPointY(), toAdd.start.getStartPointX(), toAdd.start.getStartPointY());
         double minLastDist = minStartDist;
         int minStartInd = toAdd.start.start;

         for(int i = 0; i < toAdd.start.getPointsLength(); ++i) {
            double m = MapUtils.squareRootDist31(it.getEndPointX(), it.getEndPointY(), toAdd.start.getPoint31XTile(i), toAdd.start.getPoint31YTile(i));
            if (m < minStartDist && minStartInd != i) {
               minStartInd = i;
               minStartDist = m;
            }
         }

         NetworkRouteContext.NetworkRouteSegment lastIt = it.getLast();
         int minLastInd = lastIt.end;

         for(int i = 0; i < lastIt.getPointsLength(); ++i) {
            double m = MapUtils.squareRootDist31(
               lastIt.getPoint31XTile(i), lastIt.getPoint31YTile(i), toAdd.start.getStartPointX(), toAdd.start.getStartPointY()
            );
            if (m < minLastDist && minLastInd != i) {
               minLastInd = i;
               minLastDist = m;
            }
         }

         if (minLastDist > minStartDist) {
            if (minStartInd != toAdd.start.start) {
               toAdd.setStart(new NetworkRouteContext.NetworkRouteSegment(toAdd.start, minStartInd, toAdd.start.end));
            }
         } else if (minLastInd != lastIt.end) {
            it.setEnd(new NetworkRouteContext.NetworkRouteSegment(lastIt, lastIt.start, minLastInd));
         }

         it.addChain(toAdd);
         this.add(endChains, NetworkRouteContext.convertPointToLong(it.getEndPointX(), it.getEndPointY()), it);
      }
   }

   private void add(Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains, long pnt, NetworkRouteSelector.NetworkRouteSegmentChain chain) {
      List<NetworkRouteSelector.NetworkRouteSegmentChain> lst = chains.get(pnt);
      if (lst == null) {
         lst = new ArrayList<>();
         chains.put(pnt, lst);
      }

      lst.add(chain);
   }

   private void remove(Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains, long pnt, NetworkRouteSelector.NetworkRouteSegmentChain toRemove) {
      List<NetworkRouteSelector.NetworkRouteSegmentChain> lch = chains.get(pnt);
      if (lch == null) {
         throw new IllegalStateException();
      } else if (!lch.remove(toRemove)) {
         throw new IllegalStateException();
      } else {
         if (lch.isEmpty()) {
            chains.remove(pnt);
         }
      }
   }

   private List<NetworkRouteSelector.NetworkRouteSegmentChain> flattenChainStructure(Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains) {
      List<NetworkRouteSelector.NetworkRouteSegmentChain> chainsFlat = new ArrayList<>();

      for(List<NetworkRouteSelector.NetworkRouteSegmentChain> ch : chains.values()) {
         chainsFlat.addAll(ch);
      }

      Collections.sort(chainsFlat, new Comparator<NetworkRouteSelector.NetworkRouteSegmentChain>() {
         public int compare(NetworkRouteSelector.NetworkRouteSegmentChain o1, NetworkRouteSelector.NetworkRouteSegmentChain o2) {
            return -Integer.compare(o1.getSize(), o2.getSize());
         }
      });
      return chainsFlat;
   }

   private Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> prepareEndChain(
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains
   ) {
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> endChains = new LinkedHashMap<>();

      for(List<NetworkRouteSelector.NetworkRouteSegmentChain> ch : chains.values()) {
         for(NetworkRouteSelector.NetworkRouteSegmentChain chain : ch) {
            this.add(endChains, NetworkRouteContext.convertPointToLong(chain.getEndPointX(), chain.getEndPointY()), chain);
         }
      }

      return endChains;
   }

   private Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> createChainStructure(List<NetworkRouteContext.NetworkRouteSegment> lst) {
      Map<Long, List<NetworkRouteSelector.NetworkRouteSegmentChain>> chains = new LinkedHashMap<>();

      for(NetworkRouteContext.NetworkRouteSegment s : lst) {
         NetworkRouteSelector.NetworkRouteSegmentChain chain = new NetworkRouteSelector.NetworkRouteSegmentChain();
         chain.start = s;
         long pnt = NetworkRouteContext.convertPointToLong(s.getStartPointX(), s.getStartPointY());
         this.add(chains, pnt, chain);
      }

      return chains;
   }

   private void loadData(
      NetworkRouteContext.NetworkRouteSegment segment, NetworkRouteSelector.RouteKey rkey, List<NetworkRouteContext.NetworkRouteSegment> lst
   ) throws IOException {
      TLongArrayList queue = new TLongArrayList();
      Set<Long> visitedTiles = new HashSet<>();
      Set<Long> objIds = new HashSet<>();
      long start = NetworkRouteContext.getTileId(segment.getStartPointX(), segment.getStartPointY());
      long end = NetworkRouteContext.getTileId(segment.getEndPointX(), segment.getEndPointY());
      queue.add(start);
      queue.add(end);

      while(!queue.isEmpty() && !this.isCancelled()) {
         long tileID = queue.get(queue.size() - 1);
         queue.remove(queue.size() - 1, 1);
         if (visitedTiles.add(tileID)) {
            Map<NetworkRouteSelector.RouteKey, List<NetworkRouteContext.NetworkRouteSegment>> tiles = this.rCtx
               .loadRouteSegmentTile(NetworkRouteContext.getXFromTileId(tileID), NetworkRouteContext.getYFromTileId(tileID), rkey, new HashMap());
            List<NetworkRouteContext.NetworkRouteSegment> loaded = tiles.get(rkey);
            if (loaded != null) {
               for(NetworkRouteContext.NetworkRouteSegment s : loaded) {
                  if (objIds.add(s.getId())) {
                     lst.add(s);
                  }
               }

               this.addEnclosedTiles(queue, tileID);
            }
         }
      }
   }

   private void addEnclosedTiles(TLongArrayList queue, long tile) {
      int x = NetworkRouteContext.getXFromTileId(tile);
      int y = NetworkRouteContext.getYFromTileId(tile);

      for(int dx = -1; dx <= 1; ++dx) {
         for(int dy = -1; dy <= 1; ++dy) {
            if (dy != 0 || dx != 0) {
               queue.add(NetworkRouteContext.getTileId(x + dx, y + dy, 0));
            }
         }
      }
   }

   private void growAlgorithm(NetworkRouteContext.NetworkRouteSegment segment, Map<NetworkRouteSelector.RouteKey, GPXUtilities.GPXFile> res) throws IOException {
      List<NetworkRouteContext.NetworkRouteSegment> lst = new ArrayList<>();
      TLongHashSet visitedIds = new TLongHashSet();
      visitedIds.add(segment.getId());
      lst.add(segment.inverse());
      this.debug("START ", null, segment);
      int it = 0;

      while(it++ < 16000) {
         if (!this.grow(lst, visitedIds, true, false) && !this.grow(lst, visitedIds, true, true)) {
            it = 0;
            break;
         }
      }

      Collections.reverse(lst);

      for(int i = 0; i < lst.size(); ++i) {
         lst.set(i, lst.get(i).inverse());
      }

      while(it++ < 16000) {
         if (!this.grow(lst, visitedIds, false, false) && !this.grow(lst, visitedIds, false, true)) {
            it = 0;
            break;
         }
      }

      NetworkRouteSelector.RouteKey routeKey = segment.routeKey;
      if (it != 0) {
         TIntArrayList ids = new TIntArrayList();

         for(int i = lst.size() - 1; i > 0 && i > lst.size() - 50; --i) {
            ids.add((int)(lst.get(i).getId() >> 7));
         }

         String msg = "Route likely has a loop: " + routeKey + " iterations " + it + " ids " + ids;
         System.err.println(msg);
      }

      NetworkRouteSelector.NetworkRouteSegmentChain ch = new NetworkRouteSelector.NetworkRouteSegmentChain();
      ch.start = lst.get(0);
      ch.connected = lst.subList(1, lst.size());
      res.put(routeKey, this.createGpxFile(Collections.singletonList(ch), routeKey));
      this.debug("FINISH " + lst.size(), null, segment);
   }

   private void debug(String msg, Boolean reverse, NetworkRouteContext.NetworkRouteSegment ld) {
      System.out.println(msg + (reverse == null ? "" : Character.valueOf((char)(reverse ? '-' : '+'))) + " " + ld);
   }

   private boolean grow(List<NetworkRouteContext.NetworkRouteSegment> lst, TLongHashSet visitedIds, boolean reverse, boolean approximate) throws IOException {
      int lastInd = lst.size() - 1;
      NetworkRouteContext.NetworkRouteSegment obj = lst.get(lastInd);

      for(NetworkRouteContext.NetworkRouteSegment ld : approximate
         ? this.rCtx.loadNearRouteSegment(obj.getEndPointX(), obj.getEndPointY(), 30.0)
         : this.rCtx.loadRouteSegment(obj.getEndPointX(), obj.getEndPointY())) {
         this.debug("  CHECK", reverse, ld);
         if (ld.routeKey.equals(obj.routeKey) && !visitedIds.contains(ld.getId())) {
            if (visitedIds.add(ld.getId())) {
               this.debug(">ACCEPT", reverse, ld);
               lst.add(ld);
               return true;
            }

            return false;
         }
      }

      return false;
   }

   private GPXUtilities.GPXFile createGpxFile(List<NetworkRouteSelector.NetworkRouteSegmentChain> chains, NetworkRouteSelector.RouteKey routeKey) {
      GPXUtilities.GPXFile gpxFile = new GPXUtilities.GPXFile(null, null, null);
      GPXUtilities.Track track = new GPXUtilities.Track();
      List<Integer> sizes = new ArrayList<>();

      for(NetworkRouteSelector.NetworkRouteSegmentChain c : chains) {
         List<NetworkRouteContext.NetworkRouteSegment> segmentList = new ArrayList<>();
         segmentList.add(c.start);
         if (c.connected != null) {
            segmentList.addAll(c.connected);
         }

         GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
         track.segments.add(trkSegment);
         int l = 0;
         GPXUtilities.WptPt prev = null;

         for(NetworkRouteContext.NetworkRouteSegment segment : segmentList) {
            float[] heightArray = null;
            if (segment.robj != null) {
               heightArray = segment.robj.calculateHeightArray();
            }

            int inc = segment.start < segment.end ? 1 : -1;
            int i = segment.start;

            while(true) {
               GPXUtilities.WptPt point = new GPXUtilities.WptPt();
               point.lat = MapUtils.get31LatitudeY(segment.getPoint31YTile(i));
               point.lon = MapUtils.get31LongitudeX(segment.getPoint31XTile(i));
               if (heightArray != null && heightArray.length > i * 2 + 1) {
                  point.ele = (double)heightArray[i * 2 + 1];
               }

               trkSegment.points.add(point);
               if (prev != null) {
                  l = (int)((double)l + MapUtils.getDistance(prev.lat, prev.lon, point.lat, point.lon));
               }

               prev = point;
               if (i == segment.end) {
                  break;
               }

               i += inc;
            }
         }

         sizes.add(l);
      }

      System.out.println(String.format("Segments size %d: %s", track.segments.size(), sizes.toString()));
      gpxFile.tracks.add(track);
      gpxFile.addRouteKeyTags(routeKey.tagsToGpx());
      return gpxFile;
   }

   public interface INetworkRouteSelection {
      boolean isCancelled();
   }

   public static class NetworkRouteSegmentChain {
      NetworkRouteContext.NetworkRouteSegment start;
      List<NetworkRouteContext.NetworkRouteSegment> connected;

      public int getSize() {
         return 1 + (this.connected == null ? 0 : this.connected.size());
      }

      public NetworkRouteContext.NetworkRouteSegment getLast() {
         return this.connected != null && this.connected.size() > 0 ? this.connected.get(this.connected.size() - 1) : this.start;
      }

      public int getEndPointX() {
         return this.getLast().getEndPointX();
      }

      public int getEndPointY() {
         return this.getLast().getEndPointY();
      }

      public void addChain(NetworkRouteSelector.NetworkRouteSegmentChain toAdd) {
         if (this.connected == null) {
            this.connected = new ArrayList<>();
         }

         this.connected.add(toAdd.start);
         if (toAdd.connected != null) {
            this.connected.addAll(toAdd.connected);
         }
      }

      public void setStart(NetworkRouteContext.NetworkRouteSegment newStart) {
         this.start = newStart;
      }

      public void setEnd(NetworkRouteContext.NetworkRouteSegment newEnd) {
         if (this.connected != null && this.connected.size() > 0) {
            this.connected.remove(this.connected.size() - 1);
            this.connected.add(newEnd);
         } else {
            this.start = newEnd;
         }
      }
   }

   public static class NetworkRouteSelectorFilter {
      public Set<NetworkRouteSelector.RouteKey> keyFilter = null;
      public Set<NetworkRouteSelector.RouteType> typeFilter = null;

      public List<NetworkRouteSelector.RouteKey> convert(BinaryMapDataObject obj) {
         return this.filterKeys(NetworkRouteSelector.RouteType.getRouteKeys(obj));
      }

      public List<NetworkRouteSelector.RouteKey> convert(RouteDataObject obj) {
         return this.filterKeys(NetworkRouteSelector.RouteType.getRouteKeys(obj));
      }

      private List<NetworkRouteSelector.RouteKey> filterKeys(List<NetworkRouteSelector.RouteKey> keys) {
         if (this.keyFilter == null && this.typeFilter == null) {
            return keys;
         } else {
            Iterator<NetworkRouteSelector.RouteKey> it = keys.iterator();

            while(it.hasNext()) {
               NetworkRouteSelector.RouteKey key = it.next();
               if (this.keyFilter != null && !this.keyFilter.contains(key)) {
                  it.remove();
               } else if (this.typeFilter != null && !this.typeFilter.contains(key.type)) {
                  it.remove();
               }
            }

            return keys;
         }
      }
   }

   public static class RouteKey {
      public final NetworkRouteSelector.RouteType type;
      public final Set<String> tags = new TreeSet<>();

      public RouteKey(NetworkRouteSelector.RouteType routeType) {
         this.type = routeType;
      }

      public String getValue(String key) {
         key = "__" + key + "__";

         for(String tag : this.tags) {
            int i = tag.indexOf(key);
            if (i > 0) {
               return tag.substring(i + key.length());
            }
         }

         return "";
      }

      public String getKeyFromTag(String tag) {
         String prefix = "route_" + this.type.tag + "__";
         if (tag.startsWith(prefix) && tag.length() > prefix.length()) {
            int endIdx = tag.indexOf("__", prefix.length());
            return tag.substring(prefix.length(), endIdx);
         } else {
            return "";
         }
      }

      public void addTag(String key, String value) {
         value = Algorithms.isEmpty(value) ? "" : "__" + value;
         this.tags.add("route_" + this.type.tag + "__" + key + value);
      }

      public String getRouteName() {
         String name = this.getValue("name");
         if (name.isEmpty()) {
            name = this.getValue("ref");
         }

         return name;
      }

      public String getNetwork() {
         return this.getValue("network");
      }

      public String getOperator() {
         return this.getValue("operator");
      }

      public String getSymbol() {
         return this.getValue("symbol");
      }

      public String getWebsite() {
         return this.getValue("website");
      }

      public String getWikipedia() {
         return this.getValue("wikipedia");
      }

      public static NetworkRouteSelector.RouteKey fromGpx(Map<String, String> networkRouteKeyTags) {
         String type = networkRouteKeyTags.get("type");
         if (!Algorithms.isEmpty(type)) {
            NetworkRouteSelector.RouteType routeType = NetworkRouteSelector.RouteType.getByTag(type);
            if (routeType != null) {
               NetworkRouteSelector.RouteKey routeKey = new NetworkRouteSelector.RouteKey(routeType);

               for(Entry<String, String> tag : networkRouteKeyTags.entrySet()) {
                  routeKey.addTag(tag.getKey(), tag.getValue());
               }

               return routeKey;
            }
         }

         return null;
      }

      public Map<String, String> tagsToGpx() {
         Map<String, String> networkRouteKey = new HashMap<>();
         networkRouteKey.put("type", this.type.tag);

         for(String tag : this.tags) {
            String key = this.getKeyFromTag(tag);
            String value = this.getValue(key);
            if (!Algorithms.isEmpty(value)) {
               networkRouteKey.put(key, value);
            }
         }

         return networkRouteKey;
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + this.tags.hashCode();
         return 31 * result + (this.type == null ? 0 : this.type.hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            NetworkRouteSelector.RouteKey other = (NetworkRouteSelector.RouteKey)obj;
            if (!this.tags.equals(other.tags)) {
               return false;
            } else {
               return this.type == other.type;
            }
         }
      }

      @Override
      public String toString() {
         return "Route [type=" + this.type + ", set=" + this.tags + "]";
      }
   }

   public static enum RouteType {
      HIKING("hiking"),
      BICYCLE("bicycle"),
      MTB("mtb"),
      HORSE("horse");

      private final String tag;
      private final String tagPrefix;

      private RouteType(String tag) {
         this.tag = tag;
         this.tagPrefix = "route_" + tag + "_";
      }

      public String getTag() {
         return this.tag;
      }

      public static NetworkRouteSelector.RouteType getByTag(String tag) {
         for(NetworkRouteSelector.RouteType routeType : values()) {
            if (routeType.tag.equals(tag)) {
               return routeType;
            }
         }

         return null;
      }

      public static List<NetworkRouteSelector.RouteKey> getRouteKeys(RouteDataObject obj) {
         Map<String, String> tags = new TreeMap<>();

         for(int i = 0; obj.nameIds != null && i < obj.nameIds.length; ++i) {
            int nameId = obj.nameIds[i];
            String value = (String)obj.names.get(nameId);
            BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(nameId);
            if (rt != null) {
               tags.put(rt.getTag(), value);
            }
         }

         for(int i = 0; obj.types != null && i < obj.types.length; ++i) {
            BinaryMapRouteReaderAdapter.RouteTypeRule rt = obj.region.quickGetEncodingRule(obj.types[i]);
            if (rt != null) {
               tags.put(rt.getTag(), rt.getValue());
            }
         }

         return getRouteKeys(tags);
      }

      public static List<NetworkRouteSelector.RouteKey> getRouteKeys(NativeLibrary.RenderedObject renderedObject) {
         return getRouteKeys(renderedObject.getTags());
      }

      public static List<NetworkRouteSelector.RouteKey> getRouteKeys(BinaryMapDataObject bMdo) {
         Map<String, String> tags = new TreeMap<>();

         for(int i = 0; i < bMdo.getObjectNames().keys().length; ++i) {
            int keyInd = bMdo.getObjectNames().keys()[i];
            BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(keyInd);
            String value = (String)bMdo.getObjectNames().get(keyInd);
            if (tp != null) {
               tags.put(tp.tag, value);
            }
         }

         int[] tps = bMdo.getAdditionalTypes();

         for(int i = 0; i < tps.length; ++i) {
            BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
            if (tp != null) {
               tags.put(tp.tag, tp.value);
            }
         }

         tps = bMdo.getTypes();

         for(int i = 0; i < tps.length; ++i) {
            BinaryMapIndexReader.TagValuePair tp = bMdo.getMapIndex().decodeType(tps[i]);
            if (tp != null) {
               tags.put(tp.tag, tp.value);
            }
         }

         return getRouteKeys(tags);
      }

      private static int getRouteQuantity(Map<String, String> tags, NetworkRouteSelector.RouteType rType) {
         int q = 0;

         for(String tag : tags.keySet()) {
            if (tag.startsWith(rType.tagPrefix)) {
               int num = Algorithms.extractIntegerNumber(tag);
               if (num > 0 && tag.equals(rType.tagPrefix + num)) {
                  q = Math.max(q, num);
               }
            }
         }

         return q;
      }

      public static List<NetworkRouteSelector.RouteKey> getRouteKeys(Map<String, String> tags) {
         List<NetworkRouteSelector.RouteKey> lst = new ArrayList<>();

         for(NetworkRouteSelector.RouteType routeType : values()) {
            int rq = getRouteQuantity(tags, routeType);

            for(int routeIdx = 1; routeIdx <= rq; ++routeIdx) {
               String prefix = routeType.tagPrefix + routeIdx;
               NetworkRouteSelector.RouteKey routeKey = new NetworkRouteSelector.RouteKey(routeType);

               for(Entry<String, String> e : tags.entrySet()) {
                  String tag = e.getKey();
                  if (tag.startsWith(prefix) && tag.length() > prefix.length()) {
                     String key = tag.substring(prefix.length() + 1);
                     routeKey.addTag(key, e.getValue());
                  }
               }

               lst.add(routeKey);
            }
         }

         return lst;
      }
   }
}
