package net.osmand.binary;

import net.osmand.CollatorStringMatcher;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GeocodingUtilities {
   private static final Log log = PlatformUtil.getLog(GeocodingUtilities.class);
   public static final float THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER = 5.0F;
   public static final float STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS = 250.0F;
   public static final float STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS = 400.0F;
   public static final int DISTANCE_STREET_NAME_PROXIMITY_BY_NAME = 45000;
   public static final float DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME = 1000.0F;
   public static final float THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER = 1.5F;
   public static final float DISTANCE_BUILDING_PROXIMITY = 100.0F;
   public static final Comparator<GeocodingUtilities.GeocodingResult> DISTANCE_COMPARATOR = new Comparator<GeocodingUtilities.GeocodingResult>() {
      public int compare(GeocodingUtilities.GeocodingResult o1, GeocodingUtilities.GeocodingResult o2) {
         return Double.compare(o1.getDistance(), o2.getDistance());
      }
   };

   public List<GeocodingUtilities.GeocodingResult> reverseGeocodingSearch(RoutingContext ctx, double lat, double lon, boolean allowEmptyNames) throws IOException {
      RoutePlannerFrontEnd rp = new RoutePlannerFrontEnd();
      List<GeocodingUtilities.GeocodingResult> lst = new ArrayList<>();
      List<BinaryRoutePlanner.RouteSegmentPoint> listR = new ArrayList<>();
      rp.findRouteSegment(lat, lon, ctx, listR, false, true);
      double distSquare = 0.0;
      Map<String, List<BinaryMapRouteReaderAdapter.RouteRegion>> streetNames = new HashMap<>();

      for(BinaryRoutePlanner.RouteSegmentPoint p : listR) {
         RouteDataObject road = p.getRoad();
         String name = Algorithms.isEmpty(road.getName()) ? road.getRef("", false, true) : road.getName();
         if (allowEmptyNames || !Algorithms.isEmpty(name)) {
            if (distSquare == 0.0 || distSquare > p.distSquare) {
               distSquare = p.distSquare;
            }

            GeocodingUtilities.GeocodingResult sr = new GeocodingUtilities.GeocodingResult();
            sr.searchPoint = new LatLon(lat, lon);
            sr.streetName = name == null ? "" : name;
            sr.point = p;
            sr.connectionPoint = new LatLon(MapUtils.get31LatitudeY(p.preciseY), MapUtils.get31LongitudeX(p.preciseX));
            sr.regionFP = road.region.getFilePointer();
            sr.regionLen = road.region.getLength();
            List<BinaryMapRouteReaderAdapter.RouteRegion> plst = streetNames.get(sr.streetName);
            if (plst == null) {
               plst = new ArrayList<>();
               streetNames.put(sr.streetName, plst);
            }

            if (!plst.contains(road.region)) {
               plst.add(road.region);
               lst.add(sr);
            }
         }

         if (p.distSquare > 62500.0 && distSquare != 0.0 && p.distSquare > 5.0 * distSquare || p.distSquare > 160000.0) {
            break;
         }
      }

      Collections.sort(lst, DISTANCE_COMPARATOR);
      return lst;
   }

   public List<String> prepareStreetName(String s, boolean addCommonWords) {
      List<String> ls = new ArrayList<>();
      int beginning = 0;

      for(int i = 1; i < s.length(); ++i) {
         if (s.charAt(i) == ' ') {
            this.addWord(ls, s.substring(beginning, i), addCommonWords);
            beginning = i;
         } else if (s.charAt(i) == '(') {
            this.addWord(ls, s.substring(beginning, i), addCommonWords);

            while(i < s.length()) {
               char c = s.charAt(i);
               beginning = ++i;
               if (c == ')') {
                  break;
               }
            }
         }
      }

      if (beginning < s.length()) {
         String lastWord = s.substring(beginning, s.length());
         this.addWord(ls, lastWord, addCommonWords);
      }

      Collections.sort(ls, Collator.getInstance());
      return ls;
   }

   private void addWord(List<String> ls, String word, boolean addCommonWords) {
      String w = word.trim().toLowerCase();
      if (!Algorithms.isEmpty(w)) {
         if (!addCommonWords && CommonWords.getCommonGeocoding(w) != -1) {
            return;
         }

         ls.add(w);
      }
   }

   public List<GeocodingUtilities.GeocodingResult> justifyReverseGeocodingSearch(
      final GeocodingUtilities.GeocodingResult road,
      BinaryMapIndexReader reader,
      double knownMinBuildingDistance,
      final ResultMatcher<GeocodingUtilities.GeocodingResult> result
   ) throws IOException {
      final List<GeocodingUtilities.GeocodingResult> streetsList = new ArrayList<>();
      boolean addCommonWords = false;
      List<String> streetNamesUsed = this.prepareStreetName(road.streetName, addCommonWords);
      if (streetNamesUsed.isEmpty()) {
         addCommonWords = true;
         streetNamesUsed = this.prepareStreetName(road.streetName, addCommonWords);
      }

      final List<String> streetNamesUsedFinal = streetNamesUsed;
      if (!streetNamesUsed.isEmpty()) {
         if (road.building != null) {
            log.info("Search street by name " + road.streetName + road.building.getName2() + " " + road.building.getName());
         } else {
            log.info("Search street by name " + road.streetName);
         }
         String mainWord = "";

         for(int i = 0; i < streetNamesUsedFinal.size(); ++i) {
            String s = streetNamesUsedFinal.get(i);
            if (s.length() > mainWord.length()) {
               mainWord = s;
            }
         }

         final boolean finalAddCommonWords = addCommonWords;
         BinaryMapIndexReader.SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(new ResultMatcher<MapObject>() {
            public boolean publish(MapObject object) {
               if (object instanceof Street && GeocodingUtilities.this.prepareStreetName(object.getName(), finalAddCommonWords).equals(streetNamesUsedFinal)) {
                  double d = MapUtils.getDistance(object.getLocation(), road.searchPoint.getLatitude(), road.searchPoint.getLongitude());
                  if (d < 45000.0) {
                     GeocodingUtilities.GeocodingResult rs = new GeocodingUtilities.GeocodingResult(road);
                     rs.street = (Street)object;
                     rs.connectionPoint = rs.street.getLocation();
                     rs.city = rs.street.getCity();
                     streetsList.add(rs);
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            }

            @Override
            public boolean isCancelled() {
               return result != null && result.isCancelled();
            }
         }, mainWord, CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
         req.setBBoxRadius(road.getLocation().getLatitude(), road.getLocation().getLongitude(), 45000);
         reader.searchAddressDataByName(req);
      }

      List<GeocodingUtilities.GeocodingResult> res = new ArrayList<>();
      if (streetsList.isEmpty()) {
         res.add(road);
      } else {
         streetsList.sort(DISTANCE_COMPARATOR);
         double streetDistance = 0.0;
         boolean isBuildingFound = knownMinBuildingDistance > 0.0;
         Iterator var15 = streetsList.iterator();

         label64:
         while(true) {
            GeocodingUtilities.GeocodingResult street;
            do {
               if (!var15.hasNext()) {
                  break label64;
               }

               street = (GeocodingUtilities.GeocodingResult)var15.next();
               if (streetDistance == 0.0) {
                  streetDistance = street.getDistance();
                  break;
               }
            } while(streetDistance > 0.0 && street.getDistance() > streetDistance + 1000.0 && isBuildingFound);

            street.connectionPoint = road.connectionPoint;
            List<GeocodingUtilities.GeocodingResult> streetBuildings = this.loadStreetBuildings(road, reader, street);
            streetBuildings.sort(DISTANCE_COMPARATOR);
            if (!streetBuildings.isEmpty()) {
               Iterator<GeocodingUtilities.GeocodingResult> it = streetBuildings.iterator();
               if (knownMinBuildingDistance == 0.0) {
                  GeocodingUtilities.GeocodingResult firstBld = it.next();
                  knownMinBuildingDistance = firstBld.getDistance();
                  isBuildingFound = true;
                  res.add(firstBld);
               }

               while(it.hasNext()) {
                  GeocodingUtilities.GeocodingResult nextBld = it.next();
                  if (nextBld.getDistance() > knownMinBuildingDistance * 1.5) {
                     break;
                  }

                  res.add(nextBld);
               }
            }

            res.add(street);
         }
      }

      res.sort(DISTANCE_COMPARATOR);
      return res;
   }

   public void filterDuplicateRegionResults(List<GeocodingUtilities.GeocodingResult> res) {
      res.sort(DISTANCE_COMPARATOR);
      int i = 0;

      while(i < res.size() - 1) {
         int cmp = this.cmpResult(res.get(i), res.get(i + 1));
         if (cmp > 0) {
            res.remove(i);
         } else if (cmp < 0) {
            res.remove(i + 1);
         } else {
            ++i;
         }
      }
   }

   private int cmpResult(GeocodingUtilities.GeocodingResult gr1, GeocodingUtilities.GeocodingResult gr2) {
      boolean eqStreet = Algorithms.stringsEqual(gr1.streetName, gr2.streetName);
      if (eqStreet) {
         boolean sameObj = false;
         if (gr1.city != null && gr2.city != null) {
            if (gr1.building != null && gr2.building != null) {
               if (Algorithms.stringsEqual(gr1.building.getName(), gr2.building.getName())) {
                  sameObj = true;
               }
            } else if (gr1.building == null && gr2.building == null) {
               sameObj = true;
            }
         }

         if (sameObj) {
            double cityDist1 = MapUtils.getDistance(gr1.searchPoint, gr1.city.getLocation());
            double cityDist2 = MapUtils.getDistance(gr2.searchPoint, gr2.city.getLocation());
            if (cityDist1 < cityDist2) {
               return -1;
            }

            return 1;
         }
      }

      return 0;
   }

   private List<GeocodingUtilities.GeocodingResult> loadStreetBuildings(
      GeocodingUtilities.GeocodingResult road, BinaryMapIndexReader reader, GeocodingUtilities.GeocodingResult street
   ) throws IOException {
      List<GeocodingUtilities.GeocodingResult> streetBuildings = new ArrayList<>();
      reader.preloadBuildings(street.street, null);

      for(Building b : street.street.getBuildings()) {
         if (b.getLatLon2() != null) {
            double slat = b.getLocation().getLatitude();
            double slon = b.getLocation().getLongitude();
            double tolat = b.getLatLon2().getLatitude();
            double tolon = b.getLatLon2().getLongitude();
            double coeff = MapUtils.getProjectionCoeff(road.searchPoint.getLatitude(), road.searchPoint.getLongitude(), slat, slon, tolat, tolon);
            double plat = slat + (tolat - slat) * coeff;
            double plon = slon + (tolon - slon) * coeff;
            if (MapUtils.getDistance(road.searchPoint, plat, plon) < 100.0) {
               GeocodingUtilities.GeocodingResult bld = new GeocodingUtilities.GeocodingResult(street);
               bld.building = b;
               bld.connectionPoint = new LatLon(plat, plon);
               streetBuildings.add(bld);
               String nm = b.getInterpolationName(coeff);
               if (!Algorithms.isEmpty(nm)) {
                  bld.buildingInterpolation = nm;
               }
            }
         } else if (MapUtils.getDistance(b.getLocation(), road.searchPoint) < 100.0) {
            GeocodingUtilities.GeocodingResult bld = new GeocodingUtilities.GeocodingResult(street);
            bld.building = b;
            bld.connectionPoint = b.getLocation();
            streetBuildings.add(bld);
         }
      }

      return streetBuildings;
   }

   public List<GeocodingUtilities.GeocodingResult> sortGeocodingResults(List<BinaryMapIndexReader> list, List<GeocodingUtilities.GeocodingResult> res) throws IOException {
      List<GeocodingUtilities.GeocodingResult> complete = new ArrayList<>();
      double minBuildingDistance = 0.0;

      for(GeocodingUtilities.GeocodingResult r : res) {
         BinaryMapIndexReader reader = null;

         for(BinaryMapIndexReader b : list) {
            for(BinaryMapRouteReaderAdapter.RouteRegion rb : b.getRoutingIndexes()) {
               if (r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
                  reader = b;
                  break;
               }
            }

            if (reader != null) {
               break;
            }
         }

         if (reader != null) {
            List<GeocodingUtilities.GeocodingResult> justified = this.justifyReverseGeocodingSearch(r, reader, minBuildingDistance, null);
            if (!justified.isEmpty()) {
               double md = justified.get(0).getDistance();
               if (minBuildingDistance == 0.0) {
                  minBuildingDistance = md;
               } else {
                  minBuildingDistance = Math.min(md, minBuildingDistance);
               }

               justified.get(0).dist = -1.0;
               complete.addAll(justified);
            }
         } else {
            complete.add(r);
         }
      }

      this.filterDuplicateRegionResults(complete);
      Iterator<GeocodingUtilities.GeocodingResult> it = complete.iterator();

      while(it.hasNext()) {
         GeocodingUtilities.GeocodingResult r = it.next();
         if (r.building != null && r.getDistance() > minBuildingDistance * 1.5) {
            it.remove();
         }
      }

      Collections.sort(complete, new Comparator<GeocodingUtilities.GeocodingResult>() {
         public int compare(GeocodingUtilities.GeocodingResult o1, GeocodingUtilities.GeocodingResult o2) {
            return Double.compare(o1.getDistance(), o2.getDistance());
         }
      });
      return complete;
   }

   public static class GeocodingResult {
      public LatLon searchPoint;
      public LatLon connectionPoint;
      public int regionFP;
      public int regionLen;
      public BinaryRoutePlanner.RouteSegmentPoint point;
      public String streetName;
      public Building building;
      public String buildingInterpolation;
      public Street street;
      public City city;
      private double dist = -1.0;

      public GeocodingResult() {
      }

      public GeocodingResult(GeocodingUtilities.GeocodingResult r) {
         this.searchPoint = r.searchPoint;
         this.regionFP = r.regionFP;
         this.regionLen = r.regionLen;
         this.connectionPoint = r.connectionPoint;
         this.streetName = r.streetName;
         this.point = r.point;
         this.building = r.building;
         this.city = r.city;
         this.street = r.street;
      }

      public LatLon getLocation() {
         return this.connectionPoint;
      }

      public double getSortDistance() {
         double dist = this.getDistance();
         return dist > 0.0 && this.building == null ? dist + 50.0 : dist;
      }

      public double getDistance() {
         if (this.dist == -1.0 && this.searchPoint != null) {
            if (this.building == null && this.point != null) {
               this.dist = Math.sqrt(this.point.distSquare);
            } else if (this.connectionPoint != null) {
               this.dist = MapUtils.getDistance(this.connectionPoint, this.searchPoint);
            }
         }

         return this.dist;
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();
         if (this.building != null) {
            if (this.buildingInterpolation != null) {
               bld.append(this.buildingInterpolation);
            } else {
               bld.append(this.building.getName());
            }
         }

         if (this.street != null) {
            bld.append(" str. ").append(this.street.getName()).append(" city ").append(this.city.getName());
         } else if (this.streetName != null) {
            bld.append(" str. ").append(this.streetName);
         } else if (this.city != null) {
            bld.append(" city ").append(this.city.getName());
         }

         if (this.getDistance() > 0.0) {
            bld.append(" dist=").append((int)this.getDistance());
         }

         return bld.toString();
      }
   }
}
