package net.osmand.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.osmand.GPXUtilities;
import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.StringBundle;
import net.osmand.util.Algorithms;

public class RouteExporter {
   public static final String OSMAND_ROUTER_V2 = "OsmAndRouterV2";
   private final String name;
   private final List<RouteSegmentResult> route;
   private final List<Location> locations;
   private final List<Integer> routePointIndexes;
   private final List<GPXUtilities.WptPt> points;

   public RouteExporter(
      String name, List<RouteSegmentResult> route, List<Location> locations, List<Integer> routePointIndexes, List<GPXUtilities.WptPt> points
   ) {
      this.name = name;
      this.route = route;
      this.locations = locations;
      this.routePointIndexes = (List<Integer>)(routePointIndexes == null ? new ArrayList<>() : routePointIndexes);
      this.points = points;
   }

   public GPXUtilities.GPXFile exportRoute() {
      GPXUtilities.GPXFile gpx = new GPXUtilities.GPXFile("OsmAndRouterV2");
      GPXUtilities.Track track = new GPXUtilities.Track();
      track.name = this.name;
      gpx.tracks.add(track);
      track.segments.add(this.generateRouteSegment());
      if (this.points != null) {
         for(GPXUtilities.WptPt pt : this.points) {
            gpx.addPoint(pt);
         }
      }

      return gpx;
   }

   public static GPXUtilities.GPXFile exportRoute(
      String name, List<GPXUtilities.TrkSegment> trkSegments, List<GPXUtilities.WptPt> points, List<List<GPXUtilities.WptPt>> routePoints
   ) {
      GPXUtilities.GPXFile gpx = new GPXUtilities.GPXFile("OsmAndRouterV2");
      GPXUtilities.Track track = new GPXUtilities.Track();
      track.name = name;
      gpx.tracks.add(track);
      track.segments.addAll(trkSegments);
      if (points != null) {
         for(GPXUtilities.WptPt pt : points) {
            gpx.addPoint(pt);
         }
      }

      if (routePoints != null) {
         for(List<GPXUtilities.WptPt> wptPts : routePoints) {
            gpx.addRoutePoints(wptPts, true);
         }
      }

      return gpx;
   }

   public GPXUtilities.TrkSegment generateRouteSegment() {
      RouteDataResources resources = new RouteDataResources(this.locations, this.routePointIndexes);
      List<StringBundle> routeItems = new ArrayList<>();
      if (!Algorithms.isEmpty(this.route)) {
         for(RouteSegmentResult sr : this.route) {
            sr.collectTypes(resources);
         }

         for(RouteSegmentResult sr : this.route) {
            sr.collectNames(resources);
         }

         for(RouteSegmentResult sr : this.route) {
            RouteDataBundle itemBundle = new RouteDataBundle(resources);
            sr.writeToBundle(itemBundle);
            routeItems.add(itemBundle);
         }
      }

      List<StringBundle> typeList = new ArrayList<>();
      Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules = resources.getRules();

      for(BinaryMapRouteReaderAdapter.RouteTypeRule rule : rules.keySet()) {
         RouteDataBundle typeBundle = new RouteDataBundle(resources);
         rule.writeToBundle(typeBundle);
         typeList.add(typeBundle);
      }

      GPXUtilities.TrkSegment trkSegment = new GPXUtilities.TrkSegment();
      if (this.locations != null && !this.locations.isEmpty()) {
         for(int i = 0; i < this.locations.size(); ++i) {
            Location loc = this.locations.get(i);
            GPXUtilities.WptPt pt = new GPXUtilities.WptPt();
            pt.lat = loc.getLatitude();
            pt.lon = loc.getLongitude();
            if (loc.hasSpeed()) {
               pt.speed = (double)loc.getSpeed();
            }

            if (loc.hasAltitude()) {
               pt.ele = loc.getAltitude();
            }

            if (loc.hasAccuracy()) {
               pt.hdop = (double)loc.getAccuracy();
            }

            trkSegment.points.add(pt);
         }

         List<GPXUtilities.RouteSegment> routeSegments = new ArrayList<>();

         for(StringBundle item : routeItems) {
            routeSegments.add(GPXUtilities.RouteSegment.fromStringBundle(item));
         }

         trkSegment.routeSegments = routeSegments;
         List<GPXUtilities.RouteType> routeTypes = new ArrayList<>();

         for(StringBundle item : typeList) {
            routeTypes.add(GPXUtilities.RouteType.fromStringBundle(item));
         }

         trkSegment.routeTypes = routeTypes;
         return trkSegment;
      } else {
         return trkSegment;
      }
   }
}
