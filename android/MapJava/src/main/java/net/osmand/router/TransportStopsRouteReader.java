package net.osmand.router;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportStopsRouteReader {
   public static final int MISSING_STOP_SEARCH_RADIUS = 50000;
   TLongObjectHashMap<TransportRoute> combinedRoutesCache = new TLongObjectHashMap();
   Map<BinaryMapIndexReader, TIntObjectHashMap<TransportRoute>> routesFilesCache = new LinkedHashMap<>();

   public TransportStopsRouteReader(Collection<BinaryMapIndexReader> fls) {
      for(BinaryMapIndexReader r : fls) {
         this.routesFilesCache.put(r, new TIntObjectHashMap());
      }
   }

   public Collection<TransportStop> readMergedTransportStops(BinaryMapIndexReader.SearchRequest<TransportStop> sr) throws IOException {
      TLongObjectHashMap<TransportStop> loadedTransportStops = new TLongObjectHashMap();

      for(BinaryMapIndexReader r : this.routesFilesCache.keySet()) {
         sr.clearSearchResults();
         List<TransportStop> stops = r.searchTransportIndex(sr);
         TIntObjectHashMap<TransportRoute> routesToLoad = this.mergeTransportStops(r, loadedTransportStops, stops);
         this.loadRoutes(r, routesToLoad);

         for(TransportStop stop : stops) {
            if (!stop.isMissingStop()) {
               long stopId = stop.getId();
               TransportStop multifileStop = (TransportStop)loadedTransportStops.get(stopId);
               int[] rrs = stop.getReferencesToRoutes();
               stop.setReferencesToRoutes(null);
               if (rrs != null && !multifileStop.isDeleted()) {
                  for(int rr : rrs) {
                     TransportRoute route = (TransportRoute)routesToLoad.get(rr);
                     if (route == null) {
                        if (routesToLoad.containsKey(rr)) {
                           System.err.println(String.format("Something went wrong by loading combined route %d for stop %s", rr, stop));
                        }
                     } else {
                        TransportRoute combinedRoute = this.getCombinedRoute(route);
                        if (multifileStop == stop || !multifileStop.hasRoute(combinedRoute.getId()) && !multifileStop.isRouteDeleted(combinedRoute.getId())) {
                           multifileStop.addRouteId(combinedRoute.getId());
                           multifileStop.addRoute(combinedRoute);
                        }
                     }
                  }
               }
            }
         }
      }

      return loadedTransportStops.valueCollection();
   }

   public TIntObjectHashMap<TransportRoute> mergeTransportStops(
      BinaryMapIndexReader reader, TLongObjectHashMap<TransportStop> loadedTransportStops, List<TransportStop> stops
   ) throws IOException {
      Iterator<TransportStop> it = stops.iterator();
      TIntObjectHashMap<TransportRoute> routesToLoad = (TIntObjectHashMap)this.routesFilesCache.get(reader);

      while(it.hasNext()) {
         TransportStop stop = it.next();
         long stopId = stop.getId();
         TransportStop multifileStop = (TransportStop)loadedTransportStops.get(stopId);
         long[] routesIds = stop.getRoutesIds();
         long[] delRIds = stop.getDeletedRoutesIds();
         if (multifileStop == null) {
            loadedTransportStops.put(stopId, stop);
            if (!stop.isDeleted()) {
               this.putAll(routesToLoad, stop.getReferencesToRoutes());
            }
         } else if (multifileStop.isDeleted()) {
            it.remove();
         } else {
            if (delRIds != null) {
               for(long deletedRouteId : delRIds) {
                  multifileStop.addDeletedRouteId(deletedRouteId);
               }
            }

            if (routesIds != null && routesIds.length > 0) {
               int[] refs = stop.getReferencesToRoutes();

               for(int i = 0; i < routesIds.length; ++i) {
                  long routeId = routesIds[i];
                  if (!multifileStop.hasRoute(routeId) && !multifileStop.isRouteDeleted(routeId) && !routesToLoad.containsKey(refs[i])) {
                     routesToLoad.put(refs[i], null);
                  }
               }
            } else if (stop.hasReferencesToRoutes()) {
               this.putAll(routesToLoad, stop.getReferencesToRoutes());
            } else {
               it.remove();
            }
         }
      }

      return routesToLoad;
   }

   private void putAll(TIntObjectHashMap<TransportRoute> routesToLoad, int[] referencesToRoutes) {
      for(int k = 0; k < referencesToRoutes.length; ++k) {
         if (!routesToLoad.containsKey(referencesToRoutes[k])) {
            routesToLoad.put(referencesToRoutes[k], null);
         }
      }
   }

   public void loadRoutes(BinaryMapIndexReader reader, TIntObjectHashMap<TransportRoute> localFileRoutes) throws IOException {
      if (localFileRoutes.size() > 0) {
         TIntArrayList routesToLoad = new TIntArrayList(localFileRoutes.size());
         TIntObjectIterator<TransportRoute> it = localFileRoutes.iterator();

         while(it.hasNext()) {
            it.advance();
            if (it.value() == null) {
               routesToLoad.add(it.key());
            }
         }

         routesToLoad.sort();
         reader.loadTransportRoutes(routesToLoad.toArray(), localFileRoutes);
      }
   }

   private TransportRoute getCombinedRoute(TransportRoute route) throws IOException {
      if (!route.isIncomplete()) {
         return route;
      } else {
         TransportRoute c = (TransportRoute)this.combinedRoutesCache.get(route.getId());
         if (c == null) {
            c = this.combineRoute(route);
            this.combinedRoutesCache.put(route.getId(), c);
         }

         return c;
      }
   }

   private TransportRoute combineRoute(TransportRoute route) throws IOException {
      List<TransportRoute> incompleteRoutes = this.findIncompleteRouteParts(route);
      if (incompleteRoutes == null) {
         return route;
      } else {
         List<Way> allWays = this.getAllWays(incompleteRoutes);
         LinkedList<List<TransportStop>> stopSegments = this.parseRoutePartsToSegments(incompleteRoutes);
         List<List<TransportStop>> mergedSegments = this.combineSegmentsOfSameRoute(stopSegments);
         List<TransportStop> firstSegment = null;
         List<TransportStop> lastSegment = null;

         for(List<TransportStop> l : mergedSegments) {
            if (!l.get(0).isMissingStop()) {
               firstSegment = l;
            }

            if (!l.get(l.size() - 1).isMissingStop()) {
               lastSegment = l;
            }
         }

         List<List<TransportStop>> sortedSegments = new ArrayList<>();
         if (firstSegment != null) {
            sortedSegments.add(firstSegment);
            mergedSegments.remove(firstSegment);

            while(!mergedSegments.isEmpty()) {
               List<TransportStop> last = sortedSegments.get(sortedSegments.size() - 1);
               List<TransportStop> add = this.findAndDeleteMinDistance(last.get(last.size() - 1).getLocation(), mergedSegments, true);
               sortedSegments.add(add);
            }
         } else if (lastSegment != null) {
            sortedSegments.add(lastSegment);
            mergedSegments.remove(lastSegment);

            while(!mergedSegments.isEmpty()) {
               List<TransportStop> first = sortedSegments.get(0);
               List<TransportStop> add = this.findAndDeleteMinDistance(first.get(0).getLocation(), mergedSegments, false);
               sortedSegments.add(0, add);
            }
         } else {
            sortedSegments = mergedSegments;
         }

         List<TransportStop> finalList = new ArrayList<>();

         for(List<TransportStop> s : sortedSegments) {
            finalList.addAll(s);
         }

         return new TransportRoute(route, finalList, allWays);
      }
   }

   private List<TransportStop> findAndDeleteMinDistance(LatLon location, List<List<TransportStop>> mergedSegments, boolean attachToBegin) {
      int ind = attachToBegin ? 0 : mergedSegments.get(0).size() - 1;
      double minDist = MapUtils.getDistance(mergedSegments.get(0).get(ind).getLocation(), location);
      int minInd = 0;

      for(int i = 1; i < mergedSegments.size(); ++i) {
         ind = attachToBegin ? 0 : mergedSegments.get(i).size() - 1;
         double dist = MapUtils.getDistance(mergedSegments.get(i).get(ind).getLocation(), location);
         if (dist < minDist) {
            minInd = i;
         }
      }

      return mergedSegments.remove(minInd);
   }

   private List<Way> getAllWays(List<TransportRoute> parts) {
      List<Way> w = new ArrayList<>();

      for(TransportRoute t : parts) {
         w.addAll(t.getForwardWays());
      }

      return w;
   }

   private List<List<TransportStop>> combineSegmentsOfSameRoute(LinkedList<List<TransportStop>> segments) {
      LinkedList<List<TransportStop>> tempResultSegments = this.mergeSegments(segments, new LinkedList(), false);
      return this.mergeSegments(tempResultSegments, new ArrayList(), true);
   }

   private <T extends List<List<TransportStop>>> T mergeSegments(LinkedList<List<TransportStop>> segments, T resultSegments, boolean mergeMissingSegs) {
      while(!segments.isEmpty()) {
         List<TransportStop> firstSegment = segments.poll();
         boolean merged = true;

         while(merged) {
            merged = false;
            Iterator<List<TransportStop>> it = segments.iterator();

            while(it.hasNext()) {
               List<TransportStop> segmentToMerge = it.next();
               if (mergeMissingSegs) {
                  merged = this.tryToMergeMissingStops(firstSegment, segmentToMerge);
               } else {
                  merged = this.tryToMerge(firstSegment, segmentToMerge);
               }

               if (merged) {
                  it.remove();
                  break;
               }
            }
         }

         resultSegments.add(firstSegment);
      }

      return resultSegments;
   }

   private boolean tryToMerge(List<TransportStop> firstSegment, List<TransportStop> segmentToMerge) {
      if (firstSegment.size() >= 2 && segmentToMerge.size() >= 2) {
         int commonStopFirst = 0;
         int commonStopSecond = 0;

         boolean found;
         for(found = false; commonStopFirst < firstSegment.size(); ++commonStopFirst) {
            for(commonStopSecond = 0; commonStopSecond < segmentToMerge.size() && !found; ++commonStopSecond) {
               long lid1 = firstSegment.get(commonStopFirst).getId();
               long lid2 = segmentToMerge.get(commonStopSecond).getId();
               if (lid1 > 0L && lid2 == lid1) {
                  found = true;
                  break;
               }
            }

            if (found) {
               break;
            }
         }

         if (found && commonStopFirst < firstSegment.size()) {
            int leftPartFirst = firstSegment.size() - commonStopFirst;
            int leftPartSecond = segmentToMerge.size() - commonStopSecond;
            if (leftPartFirst < leftPartSecond || leftPartFirst == leftPartSecond && firstSegment.get(firstSegment.size() - 1).isMissingStop()) {
               while(firstSegment.size() > commonStopFirst) {
                  firstSegment.remove(firstSegment.size() - 1);
               }

               for(int i = commonStopSecond; i < segmentToMerge.size(); ++i) {
                  firstSegment.add(segmentToMerge.get(i));
               }
            }

            if (commonStopFirst < commonStopSecond || commonStopFirst == commonStopSecond && firstSegment.get(0).isMissingStop()) {
               firstSegment.subList(0, commonStopFirst + 1).clear();

               for(int i = commonStopSecond; i >= 0; --i) {
                  firstSegment.add(0, segmentToMerge.get(i));
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean tryToMergeMissingStops(List<TransportStop> firstSegment, List<TransportStop> segmentToMerge) {
      boolean merged = false;
      if (MapUtils.getDistance(firstSegment.get(0).getLocation(), segmentToMerge.get(segmentToMerge.size() - 1).getLocation()) < 50000.0
         && firstSegment.get(0).isMissingStop()
         && segmentToMerge.get(segmentToMerge.size() - 1).isMissingStop()) {
         firstSegment.remove(0);

         for(int i = segmentToMerge.size() - 2; i >= 0; --i) {
            firstSegment.add(0, segmentToMerge.get(i));
         }

         merged = true;
      } else if (MapUtils.getDistance(firstSegment.get(firstSegment.size() - 1).getLocation(), segmentToMerge.get(0).getLocation()) < 50000.0
         && segmentToMerge.get(0).isMissingStop()
         && firstSegment.get(firstSegment.size() - 1).isMissingStop()) {
         firstSegment.remove(firstSegment.size() - 1);

         for(int i = 1; i < segmentToMerge.size(); ++i) {
            firstSegment.add(segmentToMerge.get(i));
         }

         merged = true;
      }

      return merged;
   }

   private LinkedList<List<TransportStop>> parseRoutePartsToSegments(List<TransportRoute> routeParts) {
      LinkedList<List<TransportStop>> segs = new LinkedList<>();

      for(TransportRoute part : routeParts) {
         List<TransportStop> newSeg = new ArrayList<>();

         for(TransportStop s : part.getForwardStops()) {
            newSeg.add(s);
            if (s.isMissingStop() && newSeg.size() > 1) {
               segs.add(newSeg);
               newSeg = new ArrayList<>();
            }
         }

         if (newSeg.size() > 1) {
            segs.add(newSeg);
         }
      }

      return segs;
   }

   private List<TransportRoute> findIncompleteRouteParts(TransportRoute baseRoute) throws IOException {
      List<TransportRoute> allRoutes = null;

      for(BinaryMapIndexReader bmir : this.routesFilesCache.keySet()) {
         IncompleteTransportRoute ptr = (IncompleteTransportRoute)bmir.getIncompleteTransportRoutes().get(baseRoute.getId());
         if (ptr != null) {
            TIntArrayList lst;
            for(lst = new TIntArrayList(); ptr != null; ptr = ptr.getNextLinkedRoute()) {
               lst.add(ptr.getRouteOffset());
            }

            if (lst.size() > 0) {
               if (allRoutes == null) {
                  allRoutes = new ArrayList<>();
               }

               allRoutes.addAll(bmir.getTransportRoutes(lst.toArray()).valueCollection());
            }
         }
      }

      return allRoutes;
   }
}
