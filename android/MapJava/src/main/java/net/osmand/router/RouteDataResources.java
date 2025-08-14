package net.osmand.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;

public class RouteDataResources {
   private final Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules = new LinkedHashMap<>();
   private final List<Location> locations;
   private int currentSegmentStartLocationIndex;
   private final List<Integer> routePointIndexes;
   private final Map<RouteDataObject, int[][]> pointNamesMap = new HashMap<>();

   public RouteDataResources() {
      this.locations = new ArrayList<>();
      this.routePointIndexes = new ArrayList<>();
   }

   public RouteDataResources(List<Location> locations, List<Integer> routePointIndexes) {
      this.locations = locations;
      this.routePointIndexes = routePointIndexes;
   }

   public Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> getRules() {
      return this.rules;
   }

   public List<Location> getLocations() {
      return this.locations;
   }

   public List<Integer> getRoutePointIndexes() {
      return this.routePointIndexes;
   }

   public Location getCurrentSegmentLocation(int offset) {
      int locationIndex = this.currentSegmentStartLocationIndex + offset;
      if (locationIndex >= this.locations.size()) {
         throw new IllegalStateException("Locations index: " + locationIndex + " out of bounds");
      } else {
         return this.locations.get(locationIndex);
      }
   }

   public int getCurrentSegmentStartLocationIndex() {
      return this.currentSegmentStartLocationIndex;
   }

   public void updateNextSegmentStartLocation(int currentSegmentLength) {
      int routePointIndex = this.routePointIndexes.indexOf(this.currentSegmentStartLocationIndex + currentSegmentLength);
      boolean overlappingNextRouteSegment = routePointIndex <= 0 || routePointIndex >= this.routePointIndexes.size() - 1;
      this.currentSegmentStartLocationIndex += overlappingNextRouteSegment ? currentSegmentLength - 1 : currentSegmentLength;
   }

   public Map<RouteDataObject, int[][]> getPointNamesMap() {
      return this.pointNamesMap;
   }
}
