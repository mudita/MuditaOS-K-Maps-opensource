package net.osmand.router;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.osmand.GPXUtilities;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringBundle;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;

public class RouteImporter {
   public static final Log log = PlatformUtil.getLog(RouteImporter.class);
   private File file;
   private GPXUtilities.GPXFile gpxFile;
   private GPXUtilities.TrkSegment segment;
   private List<GPXUtilities.WptPt> segmentRoutePoints;
   private final List<RouteSegmentResult> route = new ArrayList<>();

   public RouteImporter(File file) {
      this.file = file;
   }

   public RouteImporter(GPXUtilities.GPXFile gpxFile) {
      this.gpxFile = gpxFile;
   }

   public RouteImporter(GPXUtilities.TrkSegment segment, List<GPXUtilities.WptPt> segmentRoutePoints) {
      this.segment = segment;
      this.segmentRoutePoints = segmentRoutePoints;
   }

   public List<RouteSegmentResult> importRoute() {
      if (this.gpxFile == null && this.segment == null) {
         if (this.file != null) {
            FileInputStream fis = null;

            Object var3;
            try {
               fis = new FileInputStream(this.file);
               this.gpxFile = GPXUtilities.loadGPXFile(fis);
               this.parseRoute();
               this.gpxFile.path = this.file.getAbsolutePath();
               this.gpxFile.modifiedTime = this.file.lastModified();
               this.gpxFile.pointsModifiedTime = this.gpxFile.modifiedTime;
               return this.route;
            } catch (IOException var13) {
               log.error("Error importing route " + this.file.getAbsolutePath(), var13);
               var3 = null;
            } finally {
               try {
                  if (fis != null) {
                     fis.close();
                  }
               } catch (IOException var12) {
               }
            }

            return (List<RouteSegmentResult>)var3;
         }
      } else {
         this.parseRoute();
      }

      return this.route;
   }

   private void parseRoute() {
      if (this.segment != null) {
         this.parseRoute(this.segment, this.segmentRoutePoints);
      } else if (this.gpxFile != null) {
         List<GPXUtilities.TrkSegment> segments = this.gpxFile.getNonEmptyTrkSegments(true);

         for(int i = 0; i < segments.size(); ++i) {
            GPXUtilities.TrkSegment segment = segments.get(i);
            this.parseRoute(segment, this.gpxFile.getRoutePoints(i));
         }
      }
   }

   private void parseRoute(GPXUtilities.TrkSegment segment, List<GPXUtilities.WptPt> segmentRoutePoints) {
      BinaryMapRouteReaderAdapter.RouteRegion region = new BinaryMapRouteReaderAdapter.RouteRegion();
      RouteDataResources resources = new RouteDataResources();
      this.collectLocations(resources, segment);
      this.collectRoutePointIndexes(resources, segmentRoutePoints);
      List<RouteSegmentResult> route = this.collectRouteSegments(region, resources, segment);
      this.collectRouteTypes(region, segment);

      for(RouteSegmentResult routeSegment : route) {
         routeSegment.fillNames(resources);
      }

      this.route.addAll(route);
   }

   private void collectLocations(RouteDataResources resources, GPXUtilities.TrkSegment segment) {
      List<Location> locations = resources.getLocations();
      double lastElevation = (double)RouteDataObject.HEIGHT_UNDEFINED;
      if (segment.hasRoute()) {
         for(GPXUtilities.WptPt point : segment.points) {
            Location loc = new Location("", point.getLatitude(), point.getLongitude());
            if (!Double.isNaN(point.ele)) {
               loc.setAltitude(point.ele);
               lastElevation = point.ele;
            } else if (lastElevation != (double)RouteDataObject.HEIGHT_UNDEFINED) {
               loc.setAltitude(lastElevation);
            }

            locations.add(loc);
         }
      }
   }

   private void collectRoutePointIndexes(RouteDataResources resources, List<GPXUtilities.WptPt> segmentRoutePoints) {
      List<Integer> routePointIndexes = resources.getRoutePointIndexes();
      if (!Algorithms.isEmpty(segmentRoutePoints)) {
         for(GPXUtilities.WptPt routePoint : segmentRoutePoints) {
            routePointIndexes.add(routePoint.getTrkPtIndex());
         }
      }
   }

   private List<RouteSegmentResult> collectRouteSegments(
      BinaryMapRouteReaderAdapter.RouteRegion region, RouteDataResources resources, GPXUtilities.TrkSegment segment
   ) {
      List<RouteSegmentResult> route = new ArrayList<>();

      for(GPXUtilities.RouteSegment routeSegment : segment.routeSegments) {
         RouteDataObject object = new RouteDataObject(region);
         RouteSegmentResult segmentResult = new RouteSegmentResult(object);

         try {
            segmentResult.readFromBundle(new RouteDataBundle(resources, routeSegment.toStringBundle()));
            route.add(segmentResult);
         } catch (IllegalStateException var10) {
            log.error(var10.getMessage());
            break;
         }
      }

      return route;
   }

   private void collectRouteTypes(BinaryMapRouteReaderAdapter.RouteRegion region, GPXUtilities.TrkSegment segment) {
      int i = 0;

      for(GPXUtilities.RouteType routeType : segment.routeTypes) {
         StringBundle bundle = routeType.toStringBundle();
         String t = bundle.getString("t", null);
         String v = bundle.getString("v", null);
         region.initRouteEncodingRule(i++, t, v);
      }
   }
}
