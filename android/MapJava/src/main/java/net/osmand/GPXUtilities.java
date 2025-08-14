package net.osmand;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Map.Entry;
import net.osmand.binary.StringBundle;
import net.osmand.binary.StringBundleWriter;
import net.osmand.binary.StringBundleXmlReader;
import net.osmand.binary.StringBundleXmlWriter;
import net.osmand.data.QuadRect;
import net.osmand.router.RouteColorize;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class GPXUtilities {
   public static final Log log = PlatformUtil.getLog(GPXUtilities.class);
   public static final String ICON_NAME_EXTENSION = "icon";
   public static final String BACKGROUND_TYPE_EXTENSION = "background";
   public static final String COLOR_NAME_EXTENSION = "color";
   public static final String PROFILE_TYPE_EXTENSION = "profile";
   public static final String ADDRESS_EXTENSION = "address";
   public static final String OSMAND_EXTENSIONS_PREFIX = "osmand:";
   public static final String OSM_PREFIX = "osm_tag_";
   public static final String AMENITY_PREFIX = "amenity_";
   public static final String AMENITY_ORIGIN_EXTENSION = "amenity_origin";
   public static final String GAP_PROFILE_TYPE = "gap";
   public static final String TRKPT_INDEX_EXTENSION = "trkpt_idx";
   public static final String DEFAULT_ICON_NAME = "special_star";
   public static final char TRAVEL_GPX_CONVERT_FIRST_LETTER = 'A';
   public static final int TRAVEL_GPX_CONVERT_FIRST_DIST = 5000;
   public static final int TRAVEL_GPX_CONVERT_MULT_1 = 2;
   public static final int TRAVEL_GPX_CONVERT_MULT_2 = 5;
   public static boolean GPX_TIME_OLD_FORMAT = false;
   private static final String GPX_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
   private static final String GPX_TIME_PATTERN_TZ = "yyyy-MM-dd'T'HH:mm:ssXXX";
   private static final String GPX_TIME_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
   private static final String GPX_TIME_MILLIS_PATTERN_OLD = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
   private static final NumberFormat LAT_LON_FORMAT = new DecimalFormat("0.00#####", new DecimalFormatSymbols(Locale.US));
   public static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
   public static final int RADIUS_DIVIDER = 5000;
   public static final double PRIME_MERIDIAN = 179.999991234;

   public static int parseColor(String colorString, int defColor) {
      if (!Algorithms.isEmpty(colorString)) {
         if (colorString.charAt(0) == '#') {
            try {
               return Algorithms.parseColor(colorString);
            } catch (IllegalArgumentException var3) {
               return defColor;
            }
         }

         GPXUtilities.GPXColor gpxColor = GPXUtilities.GPXColor.getColorFromName(colorString);
         if (gpxColor != null) {
            return gpxColor.color;
         }
      }

      return defColor;
   }

   private static GPXUtilities.SplitMetric getDistanceMetric() {
      return new GPXUtilities.SplitMetric() {
         private final float[] calculations = new float[1];

         @Override
         public double metric(GPXUtilities.WptPt p1, GPXUtilities.WptPt p2) {
            Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, this.calculations);
            return (double)this.calculations[0];
         }
      };
   }

   private static GPXUtilities.SplitMetric getTimeSplit() {
      return new GPXUtilities.SplitMetric() {
         @Override
         public double metric(GPXUtilities.WptPt p1, GPXUtilities.WptPt p2) {
            return p1.time != 0L && p2.time != 0L ? (double)((int)Math.abs((p2.time - p1.time) / 1000L)) : 0.0;
         }
      };
   }

   private static void splitSegment(
      GPXUtilities.SplitMetric metric,
      GPXUtilities.SplitMetric secondaryMetric,
      double metricLimit,
      List<GPXUtilities.SplitSegment> splitSegments,
      GPXUtilities.TrkSegment segment,
      boolean joinSegments
   ) {
      double currentMetricEnd = metricLimit;
      double secondaryMetricEnd = 0.0;
      GPXUtilities.SplitSegment sp = new GPXUtilities.SplitSegment(segment, 0, 0.0);
      double total = 0.0;
      GPXUtilities.WptPt prev = null;

      for(int k = 0; k < segment.points.size(); ++k) {
         GPXUtilities.WptPt point = segment.points.get(k);
         if (k > 0) {
            double currentSegment = 0.0;
            if (!segment.generalSegment || joinSegments || !point.firstPoint) {
               currentSegment = metric.metric(prev, point);
               secondaryMetricEnd += secondaryMetric.metric(prev, point);
            }

            while(total + currentSegment > currentMetricEnd) {
               double p = currentMetricEnd - total;
               double cf = p / currentSegment;
               sp.setLastPoint(k - 1, cf);
               sp.metricEnd = currentMetricEnd;
               sp.secondaryMetricEnd = secondaryMetricEnd;
               splitSegments.add(sp);
               sp = new GPXUtilities.SplitSegment(segment, k - 1, cf);
               currentMetricEnd += metricLimit;
            }

            total += currentSegment;
         }

         prev = point;
      }

      if (segment.points.size() > 0 && (sp.endPointInd != segment.points.size() - 1 || sp.startCoeff != 1.0)) {
         sp.metricEnd = total;
         sp.secondaryMetricEnd = secondaryMetricEnd;
         sp.setLastPoint(segment.points.size() - 2, 1.0);
         splitSegments.add(sp);
      }
   }

   private static List<GPXUtilities.GPXTrackAnalysis> convert(List<GPXUtilities.SplitSegment> splitSegments) {
      List<GPXUtilities.GPXTrackAnalysis> ls = new ArrayList<>();

      for(GPXUtilities.SplitSegment s : splitSegments) {
         GPXUtilities.GPXTrackAnalysis a = new GPXUtilities.GPXTrackAnalysis();
         a.prepareInformation(0L, s);
         ls.add(a);
      }

      return ls;
   }

   public static QuadRect calculateBounds(List<GPXUtilities.WptPt> pts) {
      QuadRect trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
      updateBounds(trackBounds, pts, 0);
      return trackBounds;
   }

   public static QuadRect calculateTrackBounds(List<GPXUtilities.TrkSegment> segments) {
      QuadRect trackBounds = new QuadRect(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

      for(GPXUtilities.TrkSegment segment : segments) {
         updateBounds(trackBounds, segment.points, 0);
      }

      return trackBounds;
   }

   public static void updateBounds(QuadRect trackBounds, List<GPXUtilities.WptPt> pts, int startIndex) {
      for(int i = startIndex; i < pts.size(); ++i) {
         GPXUtilities.WptPt pt = pts.get(i);
         trackBounds.right = Math.max(trackBounds.right, pt.lon);
         trackBounds.left = Math.min(trackBounds.left, pt.lon);
         trackBounds.top = Math.max(trackBounds.top, pt.lat);
         trackBounds.bottom = Math.min(trackBounds.bottom, pt.lat);
      }
   }

   public static int calculateTrackPoints(List<GPXUtilities.TrkSegment> segments) {
      int result = 0;

      for(GPXUtilities.TrkSegment segment : segments) {
         result += segment.points.size();
      }

      return result;
   }

   public static void updateQR(QuadRect q, GPXUtilities.WptPt p, double defLat, double defLon) {
      if (q.left == defLon && q.top == defLat && q.right == defLon && q.bottom == defLat) {
         q.left = p.getLongitude();
         q.right = p.getLongitude();
         q.top = p.getLatitude();
         q.bottom = p.getLatitude();
      } else {
         q.left = Math.min(q.left, p.getLongitude());
         q.right = Math.max(q.right, p.getLongitude());
         q.top = Math.max(q.top, p.getLatitude());
         q.bottom = Math.min(q.bottom, p.getLatitude());
      }
   }

   public static String asString(GPXUtilities.GPXFile file) {
      Writer writer = new StringWriter();
      writeGpx(writer, file, null);
      return writer.toString();
   }

   public static Exception writeGpxFile(File fout, GPXUtilities.GPXFile file) {
      Writer output = null;

      Exception var4;
      try {
         if (fout.getParentFile() != null) {
            fout.getParentFile().mkdirs();
         }

         output = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
         if (Algorithms.isEmpty(file.path)) {
            file.path = fout.getAbsolutePath();
         }

         return writeGpx(output, file, null);
      } catch (Exception var8) {
         log.error("Error saving gpx", var8);
         var4 = var8;
      } finally {
         Algorithms.closeStream(output);
      }

      return var4;
   }

   public static Exception writeGpx(Writer output, GPXUtilities.GPXFile file, IProgress progress) {
      if (progress != null) {
         progress.startWork(file.getItemsToWriteSize());
      }

      try {
         XmlSerializer serializer = PlatformUtil.newSerializer();
         serializer.setOutput(output);
         serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
         serializer.startDocument("UTF-8", true);
         serializer.startTag(null, "gpx");
         serializer.attribute(null, "version", "1.1");
         if (file.author != null) {
            serializer.attribute(null, "creator", file.author);
         }

         serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1");
         serializer.attribute(null, "xmlns:osmand", "https://osmand.net");
         serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
         serializer.attribute(null, "xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
         assignPointsGroupsExtensionWriter(file);
         writeMetadata(serializer, file, progress);
         writePoints(serializer, file, progress);
         writeRoutes(serializer, file, progress);
         writeTracks(serializer, file, progress);
         writeExtensions(serializer, file, progress);
         writeNetworkRoute(serializer, file, progress);
         serializer.endTag(null, "gpx");
         serializer.endDocument();
         serializer.flush();
         return null;
      } catch (Exception var4) {
         log.error("Error saving gpx", var4);
         return var4;
      }
   }

   private static void writeNetworkRoute(XmlSerializer serializer, GPXUtilities.GPXFile gpxFile, IProgress progress) throws IOException {
      assignNetworkRouteExtensionWriter(gpxFile);
      writeExtensions(serializer, gpxFile, progress);
   }

   private static void assignNetworkRouteExtensionWriter(final GPXUtilities.GPXFile gpxFile) {
      if (!Algorithms.isEmpty(gpxFile.networkRouteKeyTags)) {
         gpxFile.setExtensionsWriter(new GPXUtilities.GPXExtensionsWriter() {
            @Override
            public void writeExtensions(XmlSerializer serializer) {
               StringBundle bundle = new StringBundle();
               StringBundle tagsBundle = new StringBundle();
               tagsBundle.putString("type", gpxFile.networkRouteKeyTags.get("type"));

               for(Entry<String, String> tag : gpxFile.networkRouteKeyTags.entrySet()) {
                  tagsBundle.putString(tag.getKey(), tag.getValue());
               }

               List<StringBundle> routeKeyBundle = new ArrayList<>();
               routeKeyBundle.add(tagsBundle);
               bundle.putBundleList("network_route", "osmand:route_key", routeKeyBundle);
               StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
               bundleWriter.writeBundle();
            }
         });
      } else {
         gpxFile.setExtensionsWriter(null);
      }
   }

   private static void assignPointsGroupsExtensionWriter(final GPXUtilities.GPXFile gpxFile) {
      if (!Algorithms.isEmpty(gpxFile.pointsGroups) && gpxFile.getExtensionsWriter() == null) {
         gpxFile.setExtensionsWriter(new GPXUtilities.GPXExtensionsWriter() {
            @Override
            public void writeExtensions(XmlSerializer serializer) {
               StringBundle bundle = new StringBundle();
               List<StringBundle> categoriesBundle = new ArrayList<>();

               for(GPXUtilities.PointsGroup group : gpxFile.pointsGroups.values()) {
                  categoriesBundle.add(group.toStringBundle());
               }

               bundle.putBundleList("points_groups", "group", categoriesBundle);
               StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
               bundleWriter.writeBundle();
            }
         });
      }
   }

   private static void writeMetadata(XmlSerializer serializer, GPXUtilities.GPXFile file, IProgress progress) throws IOException {
      String defName = file.metadata.name;
      String trackName = !Algorithms.isEmpty(defName) ? defName : getFilename(file.path);
      serializer.startTag(null, "metadata");
      writeNotNullText(serializer, "name", trackName);
      writeNotNullText(serializer, "desc", file.metadata.desc);
      if (file.metadata.author != null) {
         serializer.startTag(null, "author");
         writeAuthor(serializer, file.metadata.author);
         serializer.endTag(null, "author");
      }

      if (file.metadata.copyright != null) {
         serializer.startTag(null, "copyright");
         writeCopyright(serializer, file.metadata.copyright);
         serializer.endTag(null, "copyright");
      }

      writeNotNullTextWithAttribute(serializer, "link", "href", file.metadata.link);
      if (file.metadata.time != 0L) {
         writeNotNullText(serializer, "time", formatTime(file.metadata.time));
      }

      writeNotNullText(serializer, "keywords", file.metadata.keywords);
      if (file.metadata.bounds != null) {
         writeBounds(serializer, file.metadata.bounds);
      }

      writeExtensions(serializer, file.metadata, null);
      if (progress != null) {
         progress.progress(1);
      }

      serializer.endTag(null, "metadata");
   }

   private static void writePoints(XmlSerializer serializer, GPXUtilities.GPXFile file, IProgress progress) throws IOException {
      for(GPXUtilities.WptPt l : file.points) {
         serializer.startTag(null, "wpt");
         writeWpt(serializer, l, progress);
         serializer.endTag(null, "wpt");
      }
   }

   private static void writeRoutes(XmlSerializer serializer, GPXUtilities.GPXFile file, IProgress progress) throws IOException {
      for(GPXUtilities.Route route : file.routes) {
         serializer.startTag(null, "rte");
         writeNotNullText(serializer, "name", route.name);
         writeNotNullText(serializer, "desc", route.desc);

         for(GPXUtilities.WptPt p : route.points) {
            boolean artificial = Math.abs(p.lon) == 179.999991234;
            if (!artificial) {
               serializer.startTag(null, "rtept");
               writeWpt(serializer, p, progress);
               serializer.endTag(null, "rtept");
            }
         }

         writeExtensions(serializer, route, null);
         serializer.endTag(null, "rte");
      }
   }

   private static void writeTracks(XmlSerializer serializer, GPXUtilities.GPXFile file, IProgress progress) throws IOException {
      for(GPXUtilities.Track track : file.tracks) {
         if (!track.generalTrack) {
            serializer.startTag(null, "trk");
            writeNotNullText(serializer, "name", track.name);
            writeNotNullText(serializer, "desc", track.desc);

            for(GPXUtilities.TrkSegment segment : track.segments) {
               serializer.startTag(null, "trkseg");
               writeNotNullText(serializer, "name", segment.name);

               for(GPXUtilities.WptPt p : segment.points) {
                  boolean artificial = Math.abs(p.lon) == 179.999991234;
                  if (!artificial) {
                     serializer.startTag(null, "trkpt");
                     writeWpt(serializer, p, progress);
                     serializer.endTag(null, "trkpt");
                  }
               }

               assignRouteExtensionWriter(segment);
               writeExtensions(serializer, segment, null);
               serializer.endTag(null, "trkseg");
            }

            writeExtensions(serializer, track, null);
            serializer.endTag(null, "trk");
         }
      }
   }

   private static void assignRouteExtensionWriter(final GPXUtilities.TrkSegment segment) {
      if (segment.hasRoute() && segment.getExtensionsWriter() == null) {
         segment.setExtensionsWriter(new GPXUtilities.GPXExtensionsWriter() {
            @Override
            public void writeExtensions(XmlSerializer serializer) {
               StringBundle bundle = new StringBundle();
               List<StringBundle> segmentsBundle = new ArrayList<>();

               for(GPXUtilities.RouteSegment segmentx : segment.routeSegments) {
                  segmentsBundle.add(segmentx.toStringBundle());
               }

               bundle.putBundleList("route", "segment", segmentsBundle);
               List<StringBundle> typesBundle = new ArrayList<>();

               for(GPXUtilities.RouteType routeType : segment.routeTypes) {
                  typesBundle.add(routeType.toStringBundle());
               }

               bundle.putBundleList("types", "type", typesBundle);
               StringBundleWriter bundleWriter = new StringBundleXmlWriter(bundle, serializer);
               bundleWriter.writeBundle();
            }
         });
      }
   }

   private static String getFilename(String path) {
      if (path != null) {
         int i = path.lastIndexOf(47);
         if (i > 0) {
            path = path.substring(i + 1);
         }

         i = path.lastIndexOf(46);
         if (i > 0) {
            path = path.substring(0, i);
         }
      }

      return path;
   }

   private static void writeNotNullTextWithAttribute(XmlSerializer serializer, String tag, String attribute, String value) throws IOException {
      if (value != null) {
         serializer.startTag(null, tag);
         serializer.attribute(null, attribute, value);
         serializer.endTag(null, tag);
      }
   }

   public static void writeNotNullText(XmlSerializer serializer, String tag, String value) throws IOException {
      if (value != null) {
         serializer.startTag(null, tag);
         serializer.text(value);
         serializer.endTag(null, tag);
      }
   }

   private static void writeExtensions(XmlSerializer serializer, GPXUtilities.GPXExtensions p, IProgress progress) throws IOException {
      writeExtensions(serializer, p.getExtensionsToRead(), p, progress);
   }

   private static void writeExtensions(XmlSerializer serializer, Map<String, String> extensions, GPXUtilities.GPXExtensions p, IProgress progress) throws IOException {
      GPXUtilities.GPXExtensionsWriter extensionsWriter = p.getExtensionsWriter();
      if (!extensions.isEmpty() || extensionsWriter != null) {
         serializer.startTag(null, "extensions");
         if (!extensions.isEmpty()) {
            for(Entry<String, String> entry : extensions.entrySet()) {
               String key = entry.getKey().replace(":", "_-_");
               if (!key.startsWith("osmand:")) {
                  key = "osmand:" + key;
               }

               writeNotNullText(serializer, key, entry.getValue());
            }
         }

         if (extensionsWriter != null) {
            extensionsWriter.writeExtensions(serializer);
         }

         serializer.endTag(null, "extensions");
         if (progress != null) {
            progress.progress(1);
         }
      }
   }

   private static void writeWpt(XmlSerializer serializer, GPXUtilities.WptPt p, IProgress progress) throws IOException {
      serializer.attribute(null, "lat", LAT_LON_FORMAT.format(p.lat));
      serializer.attribute(null, "lon", LAT_LON_FORMAT.format(p.lon));
      if (!Double.isNaN(p.ele)) {
         writeNotNullText(serializer, "ele", DECIMAL_FORMAT.format(p.ele));
      }

      if (p.time != 0L) {
         writeNotNullText(serializer, "time", formatTime(p.time));
      }

      writeNotNullText(serializer, "name", p.name);
      writeNotNullText(serializer, "desc", p.desc);
      writeNotNullTextWithAttribute(serializer, "link", "href", p.link);
      writeNotNullText(serializer, "type", p.category);
      writeNotNullText(serializer, "cmt", p.comment);
      if (!Double.isNaN(p.hdop)) {
         writeNotNullText(serializer, "hdop", DECIMAL_FORMAT.format(p.hdop));
      }

      if (p.speed > 0.0) {
         p.getExtensionsToWrite().put("speed", DECIMAL_FORMAT.format(p.speed));
      }

      if (!Float.isNaN(p.heading)) {
         p.getExtensionsToWrite().put("heading", String.valueOf(Math.round(p.heading)));
      }

      Map<String, String> extensions = p.getExtensionsToRead();
      if (!"rtept".equals(serializer.getName())) {
         extensions.remove("profile");
         extensions.remove("trkpt_idx");
         writeExtensions(serializer, extensions, p, null);
      } else {
         String profile = extensions.get("profile");
         if ("gap".equals(profile)) {
            extensions.remove("profile");
         }

         writeExtensions(serializer, p, null);
      }

      if (progress != null) {
         progress.progress(1);
      }
   }

   private static void writeAuthor(XmlSerializer serializer, GPXUtilities.Author author) throws IOException {
      writeNotNullText(serializer, "name", author.name);
      if (author.email != null && author.email.contains("@")) {
         String[] idAndDomain = author.email.split("@");
         if (idAndDomain.length == 2 && !idAndDomain[0].isEmpty() && !idAndDomain[1].isEmpty()) {
            serializer.startTag(null, "email");
            serializer.attribute(null, "id", idAndDomain[0]);
            serializer.attribute(null, "domain", idAndDomain[1]);
            serializer.endTag(null, "email");
         }
      }

      writeNotNullTextWithAttribute(serializer, "link", "href", author.link);
   }

   private static void writeCopyright(XmlSerializer serializer, GPXUtilities.Copyright copyright) throws IOException {
      serializer.attribute(null, "author", copyright.author);
      writeNotNullText(serializer, "year", copyright.year);
      writeNotNullText(serializer, "license", copyright.license);
   }

   private static void writeBounds(XmlSerializer serializer, GPXUtilities.Bounds bounds) throws IOException {
      serializer.startTag(null, "bounds");
      serializer.attribute(null, "minlat", LAT_LON_FORMAT.format(bounds.minlat));
      serializer.attribute(null, "minlon", LAT_LON_FORMAT.format(bounds.minlon));
      serializer.attribute(null, "maxlat", LAT_LON_FORMAT.format(bounds.maxlat));
      serializer.attribute(null, "maxlon", LAT_LON_FORMAT.format(bounds.maxlon));
      serializer.endTag(null, "bounds");
   }

   public static String readText(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
      StringBuilder text = null;

      int tok;
      while((tok = parser.next()) != 1 && (tok != 1 || !parser.getName().equals(key))) {
         if (tok == 4) {
            if (text == null) {
               text = new StringBuilder(parser.getText());
            } else {
               text.append(parser.getText());
            }
         }
      }

      return text == null ? null : text.toString();
   }

   private static Map<String, String> readTextMap(XmlPullParser parser, String key) throws XmlPullParserException, IOException {
      StringBuilder text = null;
      Map<String, String> result = new HashMap<>();

      int tok;
      while((tok = parser.next()) != 1) {
         if (tok == 3) {
            String tag = parser.getName();
            if (text != null && !Algorithms.isEmpty(text.toString().trim())) {
               result.put(tag, text.toString());
            }

            if (tag.equals(key)) {
               break;
            }

            text = null;
         } else if (tok == 2) {
            text = null;
         } else if (tok == 4) {
            if (text == null) {
               text = new StringBuilder(parser.getText());
            } else {
               text.append(parser.getText());
            }
         }
      }

      return result;
   }

   public static String formatTime(long time) {
      SimpleDateFormat format = getTimeFormatter();
      return format.format(new Date(time));
   }

   public static long parseTime(String text) {
      return GPX_TIME_OLD_FORMAT
         ? parseTime(text, getTimeFormatter(), getTimeFormatterMills())
         : parseTime(text, getTimeFormatterTZ(), getTimeFormatterMills());
   }

   public static long parseTime(String text, SimpleDateFormat format, SimpleDateFormat formatMillis) {
      long time = 0L;
      if (text != null) {
         try {
            time = format.parse(text).getTime();
         } catch (ParseException var8) {
            try {
               time = formatMillis.parse(text).getTime();
            } catch (ParseException var7) {
            }
         }
      }

      return time;
   }

   private static SimpleDateFormat getTimeFormatter() {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format;
   }

   private static SimpleDateFormat getTimeFormatterTZ() {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format;
   }

   private static SimpleDateFormat getTimeFormatterMills() {
      String pattern = GPX_TIME_OLD_FORMAT ? "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" : "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
      SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("UTC"));
      return format;
   }

   public static GPXUtilities.GPXFile loadGPXFile(File file) {
      return loadGPXFile(file, null);
   }

   public static GPXUtilities.GPXFile loadGPXFile(File file, GPXUtilities.GPXExtensionsReader extensionsReader) {
      FileInputStream fis = null;

      GPXUtilities.GPXFile var5;
      try {
         fis = new FileInputStream(file);
         GPXUtilities.GPXFile gpxFile = loadGPXFile(fis, extensionsReader);
         gpxFile.path = file.getAbsolutePath();
         gpxFile.modifiedTime = file.lastModified();
         gpxFile.pointsModifiedTime = gpxFile.modifiedTime;
         Algorithms.closeStream(fis);
         return gpxFile;
      } catch (IOException var9) {
         GPXUtilities.GPXFile gpxFilex = new GPXUtilities.GPXFile(null);
         gpxFilex.path = file.getAbsolutePath();
         log.error("Error reading gpx " + gpxFilex.path, var9);
         gpxFilex.error = var9;
         var5 = gpxFilex;
      } finally {
         Algorithms.closeStream(fis);
      }

      return var5;
   }

   public static GPXUtilities.GPXFile loadGPXFile(InputStream stream) {
      return loadGPXFile(stream, null);
   }

   public static GPXUtilities.GPXFile loadGPXFile(InputStream stream, GPXUtilities.GPXExtensionsReader extensionsReader) {
      GPXUtilities.GPXFile gpxFile = new GPXUtilities.GPXFile(null);

      try {
         XmlPullParser parser = PlatformUtil.newXMLPullParser();
         parser.setInput(getUTF8Reader(stream));
         GPXUtilities.Track routeTrack = new GPXUtilities.Track();
         GPXUtilities.TrkSegment routeTrackSegment = new GPXUtilities.TrkSegment();
         routeTrack.segments.add(routeTrackSegment);
         Stack<GPXUtilities.GPXExtensions> parserState = new Stack<>();
         GPXUtilities.TrkSegment firstSegment = null;
         boolean extensionReadMode = false;
         boolean routePointExtension = false;
         List<GPXUtilities.RouteSegment> routeSegments = new ArrayList<>();
         List<GPXUtilities.RouteType> routeTypes = new ArrayList<>();
         List<GPXUtilities.PointsGroup> pointsGroups = new ArrayList<>();
         boolean routeExtension = false;
         boolean typesExtension = false;
         boolean pointsGroupsExtension = false;
         boolean networkRoute = false;
         parserState.push(gpxFile);

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               GPXUtilities.GPXExtensions parse = parserState.peek();
               String tag = parser.getName();
               if (extensionReadMode && parse != null && !routePointExtension) {
                  String tagName = tag.toLowerCase();
                  if (routeExtension && tagName.equals("segment")) {
                     GPXUtilities.RouteSegment segment = parseRouteSegmentAttributes(parser);
                     routeSegments.add(segment);
                  } else if (typesExtension && tagName.equals("type")) {
                     GPXUtilities.RouteType type = parseRouteTypeAttributes(parser);
                     routeTypes.add(type);
                  } else if (pointsGroupsExtension && tagName.equals("group")) {
                     GPXUtilities.PointsGroup pointsGroup = GPXUtilities.PointsGroup.parsePointsGroupAttributes(parser);
                     pointsGroups.add(pointsGroup);
                  } else if (networkRoute && tagName.equals("route_key")) {
                     gpxFile.networkRouteKeyTags.putAll(parseRouteKeyAttributes(parser));
                  }

                  switch(tagName) {
                     case "routepointextension":
                        routePointExtension = true;
                        if (parse instanceof GPXUtilities.WptPt) {
                           parse.getExtensionsToWrite().put("offset", routeTrackSegment.points.size() + "");
                        }
                        break;
                     case "route":
                        routeExtension = true;
                        break;
                     case "types":
                        typesExtension = true;
                        break;
                     case "points_groups":
                        pointsGroupsExtension = true;
                        break;
                     case "network_route":
                        networkRoute = true;
                        break;
                     default:
                        if (extensionsReader == null || !extensionsReader.readExtensions(gpxFile, parser)) {
                           Map<String, String> values = readTextMap(parser, tag);
                           if (values.size() > 0) {
                              for(Entry<String, String> entry : values.entrySet()) {
                                 String t = entry.getKey().toLowerCase();
                                 String value = entry.getValue();
                                 parse.getExtensionsToWrite().put(t, value);
                                 if (tag.equals("speed") && parse instanceof GPXUtilities.WptPt) {
                                    try {
                                       ((GPXUtilities.WptPt)parse).speed = (double)Float.parseFloat(value);
                                    } catch (NumberFormatException var33) {
                                       log.debug(var33.getMessage(), var33);
                                    }
                                 }
                              }
                           }
                        }
                  }
               } else if (parse != null && tag.equals("extensions")) {
                  extensionReadMode = true;
               } else if (routePointExtension) {
                  if (tag.equals("rpt")) {
                     GPXUtilities.WptPt wptPt = parseWptAttributes(parser);
                     routeTrackSegment.points.add(wptPt);
                     parserState.push(wptPt);
                  }
               } else if (parse instanceof GPXUtilities.GPXFile) {
                  if (tag.equals("gpx")) {
                     ((GPXUtilities.GPXFile)parse).author = parser.getAttributeValue("", "creator");
                  }

                  if (tag.equals("metadata")) {
                     GPXUtilities.Metadata metadata = new GPXUtilities.Metadata();
                     ((GPXUtilities.GPXFile)parse).metadata = metadata;
                     parserState.push(metadata);
                  }

                  if (tag.equals("trk")) {
                     GPXUtilities.Track track = new GPXUtilities.Track();
                     ((GPXUtilities.GPXFile)parse).tracks.add(track);
                     parserState.push(track);
                  }

                  if (tag.equals("rte")) {
                     GPXUtilities.Route route = new GPXUtilities.Route();
                     ((GPXUtilities.GPXFile)parse).routes.add(route);
                     parserState.push(route);
                  }

                  if (tag.equals("wpt")) {
                     GPXUtilities.WptPt wptPt = parseWptAttributes(parser);
                     ((GPXUtilities.GPXFile)parse).points.add(wptPt);
                     parserState.push(wptPt);
                  }
               } else if (parse instanceof GPXUtilities.Metadata) {
                  if (tag.equals("name")) {
                     ((GPXUtilities.Metadata)parse).name = readText(parser, "name");
                  }

                  if (tag.equals("desc")) {
                     ((GPXUtilities.Metadata)parse).desc = readText(parser, "desc");
                  }

                  if (tag.equals("author")) {
                     GPXUtilities.Author author = new GPXUtilities.Author();
                     author.name = parser.getText();
                     ((GPXUtilities.Metadata)parse).author = author;
                     parserState.push(author);
                  }

                  if (tag.equals("copyright")) {
                     GPXUtilities.Copyright copyright = new GPXUtilities.Copyright();
                     copyright.license = parser.getText();
                     copyright.author = parser.getAttributeValue("", "author");
                     ((GPXUtilities.Metadata)parse).copyright = copyright;
                     parserState.push(copyright);
                  }

                  if (tag.equals("link")) {
                     ((GPXUtilities.Metadata)parse).link = parser.getAttributeValue("", "href");
                  }

                  if (tag.equals("time")) {
                     String text = readText(parser, "time");
                     ((GPXUtilities.Metadata)parse).time = parseTime(text);
                  }

                  if (tag.equals("keywords")) {
                     ((GPXUtilities.Metadata)parse).keywords = readText(parser, "keywords");
                  }

                  if (tag.equals("bounds")) {
                     GPXUtilities.Bounds bounds = parseBoundsAttributes(parser);
                     ((GPXUtilities.Metadata)parse).bounds = bounds;
                     parserState.push(bounds);
                  }
               } else if (parse instanceof GPXUtilities.Author) {
                  if (tag.equals("name")) {
                     ((GPXUtilities.Author)parse).name = readText(parser, "name");
                  }

                  if (tag.equals("email")) {
                     String id = parser.getAttributeValue("", "id");
                     String domain = parser.getAttributeValue("", "domain");
                     if (!Algorithms.isEmpty(id) && !Algorithms.isEmpty(domain)) {
                        ((GPXUtilities.Author)parse).email = id + "@" + domain;
                     }
                  }

                  if (tag.equals("link")) {
                     ((GPXUtilities.Author)parse).link = parser.getAttributeValue("", "href");
                  }
               } else if (parse instanceof GPXUtilities.Copyright) {
                  if (tag.equals("year")) {
                     ((GPXUtilities.Copyright)parse).year = readText(parser, "year");
                  }

                  if (tag.equals("license")) {
                     ((GPXUtilities.Copyright)parse).license = readText(parser, "license");
                  }
               } else if (parse instanceof GPXUtilities.Route) {
                  if (tag.equals("name")) {
                     ((GPXUtilities.Route)parse).name = readText(parser, "name");
                  }

                  if (tag.equals("desc")) {
                     ((GPXUtilities.Route)parse).desc = readText(parser, "desc");
                  }

                  if (tag.equals("rtept")) {
                     GPXUtilities.WptPt wptPt = parseWptAttributes(parser);
                     ((GPXUtilities.Route)parse).points.add(wptPt);
                     parserState.push(wptPt);
                  }
               } else if (parse instanceof GPXUtilities.Track) {
                  if (tag.equals("name")) {
                     ((GPXUtilities.Track)parse).name = readText(parser, "name");
                  } else if (tag.equals("desc")) {
                     ((GPXUtilities.Track)parse).desc = readText(parser, "desc");
                  } else if (tag.equals("trkseg")) {
                     GPXUtilities.TrkSegment trkSeg = new GPXUtilities.TrkSegment();
                     ((GPXUtilities.Track)parse).segments.add(trkSeg);
                     parserState.push(trkSeg);
                  } else if (tag.equals("trkpt") || tag.equals("rpt")) {
                     GPXUtilities.WptPt wptPt = parseWptAttributes(parser);
                     int size = ((GPXUtilities.Track)parse).segments.size();
                     if (size == 0) {
                        ((GPXUtilities.Track)parse).segments.add(new GPXUtilities.TrkSegment());
                        ++size;
                     }

                     ((GPXUtilities.Track)parse).segments.get(size - 1).points.add(wptPt);
                     parserState.push(wptPt);
                  }
               } else if (!(parse instanceof GPXUtilities.TrkSegment)) {
                  if (parse instanceof GPXUtilities.WptPt) {
                     if (tag.equals("name")) {
                        ((GPXUtilities.WptPt)parse).name = readText(parser, "name");
                     } else if (tag.equals("desc")) {
                        ((GPXUtilities.WptPt)parse).desc = readText(parser, "desc");
                     } else if (tag.equals("cmt")) {
                        ((GPXUtilities.WptPt)parse).comment = readText(parser, "cmt");
                     } else if (tag.equals("speed")) {
                        try {
                           String value = readText(parser, "speed");
                           if (!Algorithms.isEmpty(value)) {
                              ((GPXUtilities.WptPt)parse).speed = (double)Float.parseFloat(value);
                              parse.getExtensionsToWrite().put("speed", value);
                           }
                        } catch (NumberFormatException var31) {
                        }
                     } else if (tag.equals("link")) {
                        ((GPXUtilities.WptPt)parse).link = parser.getAttributeValue("", "href");
                     } else if (tag.equals("category")) {
                        ((GPXUtilities.WptPt)parse).category = readText(parser, "category");
                     } else if (tag.equals("type")) {
                        if (((GPXUtilities.WptPt)parse).category == null) {
                           ((GPXUtilities.WptPt)parse).category = readText(parser, "type");
                        }
                     } else if (tag.equals("ele")) {
                        String text = readText(parser, "ele");
                        if (text != null) {
                           try {
                              ((GPXUtilities.WptPt)parse).ele = (double)Float.parseFloat(text);
                           } catch (NumberFormatException var30) {
                           }
                        }
                     } else if (tag.equals("hdop")) {
                        String text = readText(parser, "hdop");
                        if (text != null) {
                           try {
                              ((GPXUtilities.WptPt)parse).hdop = (double)Float.parseFloat(text);
                           } catch (NumberFormatException var29) {
                           }
                        }
                     } else if (tag.equals("time")) {
                        String text = readText(parser, "time");
                        ((GPXUtilities.WptPt)parse).time = parseTime(text);
                     }
                  }
               } else {
                  if (tag.equals("name")) {
                     ((GPXUtilities.TrkSegment)parse).name = readText(parser, "name");
                  } else if (tag.equals("trkpt") || tag.equals("rpt")) {
                     GPXUtilities.WptPt wptPt = parseWptAttributes(parser);
                     ((GPXUtilities.TrkSegment)parse).points.add(wptPt);
                     parserState.push(wptPt);
                  }

                  if (tag.equals("csvattributes")) {
                     String segmentPoints = readText(parser, "csvattributes");
                     String[] pointsArr = segmentPoints.split("\n");

                     for(int i = 0; i < pointsArr.length; ++i) {
                        String[] pointAttrs = pointsArr[i].split(",");

                        try {
                           int arrLength = pointsArr.length;
                           if (arrLength > 1) {
                              GPXUtilities.WptPt wptPt = new GPXUtilities.WptPt();
                              wptPt.lon = Double.parseDouble(pointAttrs[0]);
                              wptPt.lat = Double.parseDouble(pointAttrs[1]);
                              ((GPXUtilities.TrkSegment)parse).points.add(wptPt);
                              if (arrLength > 2) {
                                 wptPt.ele = Double.parseDouble(pointAttrs[2]);
                              }
                           }
                        } catch (NumberFormatException var32) {
                        }
                     }
                  }
               }
            } else if (tok == 3) {
               Object parse = parserState.peek();
               String tag = parser.getName();
               if (tag.equalsIgnoreCase("routepointextension")) {
                  routePointExtension = false;
               }

               if (parse != null && tag.equals("extensions")) {
                  extensionReadMode = false;
               }

               if (extensionReadMode && tag.equals("route")) {
                  routeExtension = false;
               } else if (extensionReadMode && tag.equals("types")) {
                  typesExtension = false;
               } else if (extensionReadMode && tag.equals("network_route")) {
                  networkRoute = false;
               } else if (tag.equals("metadata")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.Metadata;
               } else if (tag.equals("author")) {
                  if (parse instanceof GPXUtilities.Author) {
                     parserState.pop();
                  }
               } else if (tag.equals("copyright")) {
                  if (parse instanceof GPXUtilities.Copyright) {
                     parserState.pop();
                  }
               } else if (tag.equals("bounds")) {
                  if (parse instanceof GPXUtilities.Bounds) {
                     parserState.pop();
                  }
               } else if (tag.equals("trkpt")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.WptPt;
               } else if (tag.equals("wpt")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.WptPt;
               } else if (tag.equals("rtept")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.WptPt;
               } else if (tag.equals("trk")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.Track;
               } else if (tag.equals("rte")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.Route;
               } else if (tag.equals("trkseg")) {
                  Object pop = parserState.pop();
                  if (pop instanceof GPXUtilities.TrkSegment) {
                     GPXUtilities.TrkSegment segment = (GPXUtilities.TrkSegment)pop;
                     segment.routeSegments = routeSegments;
                     segment.routeTypes = routeTypes;
                     routeSegments = new ArrayList<>();
                     routeTypes = new ArrayList<>();
                     if (firstSegment == null) {
                        firstSegment = segment;
                     }
                  }

                  assert pop instanceof GPXUtilities.TrkSegment;
               } else if (tag.equals("rpt")) {
                  Object pop = parserState.pop();

                  assert pop instanceof GPXUtilities.WptPt;
               }
            }
         }

         if (!routeTrackSegment.points.isEmpty()) {
            gpxFile.tracks.add(routeTrack);
         }

         if (!routeSegments.isEmpty() && !routeTypes.isEmpty() && firstSegment != null) {
            firstSegment.routeSegments = routeSegments;
            firstSegment.routeTypes = routeTypes;
         }

         if (!pointsGroups.isEmpty() || !gpxFile.points.isEmpty()) {
            gpxFile.pointsGroups.putAll(mergePointsGroups(pointsGroups, gpxFile.points));
         }

         gpxFile.addGeneralTrack();
      } catch (Exception var34) {
         gpxFile.error = var34;
         log.error("Error reading gpx", var34);
      }

      createArtificialPrimeMeridianPoints(gpxFile);
      return gpxFile;
   }

   private static Map<String, String> parseRouteKeyAttributes(XmlPullParser parser) {
      Map<String, String> networkRouteKeyTags = new LinkedHashMap<>();
      StringBundleXmlReader reader = new StringBundleXmlReader(parser);
      reader.readBundle();
      StringBundle bundle = reader.getBundle();
      if (!bundle.isEmpty()) {
         for(StringBundle.Item<?> item : bundle.getMap().values()) {
            if (item.getType() == StringBundle.ItemType.STRING) {
               networkRouteKeyTags.put(item.getName(), (String)item.getValue());
            }
         }
      }

      return networkRouteKeyTags;
   }

   private static Map<String, GPXUtilities.PointsGroup> mergePointsGroups(List<GPXUtilities.PointsGroup> groups, List<GPXUtilities.WptPt> points) {
      Map<String, GPXUtilities.PointsGroup> pointsGroups = new LinkedHashMap<>();

      for(GPXUtilities.PointsGroup category : groups) {
         pointsGroups.put(category.name, category);
      }

      for(GPXUtilities.WptPt point : points) {
         String categoryName = point.category != null ? point.category : "";
         GPXUtilities.PointsGroup pointsGroup = pointsGroups.get(categoryName);
         if (pointsGroup == null) {
            pointsGroup = new GPXUtilities.PointsGroup(point);
            pointsGroups.put(categoryName, pointsGroup);
         }

         int color = point.getColor();
         if (pointsGroup.color == 0 && color != 0) {
            pointsGroup.color = color;
         }

         String iconName = point.getIconName();
         if (Algorithms.isEmpty(pointsGroup.iconName) && !Algorithms.isEmpty(iconName)) {
            pointsGroup.iconName = iconName;
         }

         String backgroundType = point.getBackgroundType();
         if (Algorithms.isEmpty(pointsGroup.backgroundType) && !Algorithms.isEmpty(backgroundType)) {
            pointsGroup.backgroundType = backgroundType;
         }

         pointsGroup.points.add(point);
      }

      return pointsGroups;
   }

   private static Reader getUTF8Reader(InputStream f) throws IOException {
      BufferedInputStream bis = new BufferedInputStream(f);

      assert bis.markSupported();

      bis.mark(3);
      boolean reset = true;
      byte[] t = new byte[3];
      bis.read(t);
      if (t[0] == -17 && t[1] == -69 && t[2] == -65) {
         reset = false;
      }

      if (reset) {
         bis.reset();
      }

      return new InputStreamReader(bis, "UTF-8");
   }

   private static GPXUtilities.WptPt parseWptAttributes(XmlPullParser parser) {
      GPXUtilities.WptPt wpt = new GPXUtilities.WptPt();

      try {
         wpt.lat = Double.parseDouble(parser.getAttributeValue("", "lat"));
         wpt.lon = Double.parseDouble(parser.getAttributeValue("", "lon"));
      } catch (NumberFormatException var3) {
      }

      return wpt;
   }

   private static GPXUtilities.RouteSegment parseRouteSegmentAttributes(XmlPullParser parser) {
      GPXUtilities.RouteSegment segment = new GPXUtilities.RouteSegment();
      segment.id = parser.getAttributeValue("", "id");
      segment.length = parser.getAttributeValue("", "length");
      segment.startTrackPointIndex = parser.getAttributeValue("", "startTrkptIdx");
      segment.segmentTime = parser.getAttributeValue("", "segmentTime");
      segment.speed = parser.getAttributeValue("", "speed");
      segment.turnType = parser.getAttributeValue("", "turnType");
      segment.turnLanes = parser.getAttributeValue("", "turnLanes");
      segment.turnAngle = parser.getAttributeValue("", "turnAngle");
      segment.skipTurn = parser.getAttributeValue("", "skipTurn");
      segment.types = parser.getAttributeValue("", "types");
      segment.pointTypes = parser.getAttributeValue("", "pointTypes");
      segment.names = parser.getAttributeValue("", "names");
      return segment;
   }

   private static GPXUtilities.RouteType parseRouteTypeAttributes(XmlPullParser parser) {
      GPXUtilities.RouteType type = new GPXUtilities.RouteType();
      type.tag = parser.getAttributeValue("", "t");
      type.value = parser.getAttributeValue("", "v");
      return type;
   }

   private static GPXUtilities.Bounds parseBoundsAttributes(XmlPullParser parser) {
      GPXUtilities.Bounds bounds = new GPXUtilities.Bounds();

      try {
         String minlat = parser.getAttributeValue("", "minlat");
         String minlon = parser.getAttributeValue("", "minlon");
         String maxlat = parser.getAttributeValue("", "maxlat");
         String maxlon = parser.getAttributeValue("", "maxlon");
         if (minlat == null) {
            minlat = parser.getAttributeValue("", "minLat");
         }

         if (minlon == null) {
            minlon = parser.getAttributeValue("", "minLon");
         }

         if (maxlat == null) {
            maxlat = parser.getAttributeValue("", "maxLat");
         }

         if (maxlon == null) {
            maxlon = parser.getAttributeValue("", "maxLon");
         }

         if (minlat != null) {
            bounds.minlat = Double.parseDouble(minlat);
         }

         if (minlon != null) {
            bounds.minlon = Double.parseDouble(minlon);
         }

         if (maxlat != null) {
            bounds.maxlat = Double.parseDouble(maxlat);
         }

         if (maxlon != null) {
            bounds.maxlon = Double.parseDouble(maxlon);
         }
      } catch (NumberFormatException var6) {
      }

      return bounds;
   }

   public static void mergeGPXFileInto(GPXUtilities.GPXFile to, GPXUtilities.GPXFile from) {
      if (from != null) {
         if (from.showCurrentTrack) {
            to.showCurrentTrack = true;
         }

         if (!Algorithms.isEmpty(from.points)) {
            to.addPoints(from.points);
         }

         if (from.tracks != null) {
            to.tracks.addAll(from.tracks);
         }

         if (from.routes != null) {
            to.routes.addAll(from.routes);
         }

         if (from.error != null) {
            to.error = from.error;
         }
      }
   }

   public static void createArtificialPrimeMeridianPoints(GPXUtilities.GPXFile gpxFile) {
      if (gpxFile.getNonEmptySegmentsCount() == 0) {
         for(GPXUtilities.Route route : gpxFile.routes) {
            createArtificialPrimeMeridianPoints(route.points);
         }
      } else {
         for(GPXUtilities.Track track : gpxFile.tracks) {
            for(GPXUtilities.TrkSegment segment : track.segments) {
               createArtificialPrimeMeridianPoints(segment.points);
            }
         }
      }
   }

   private static void createArtificialPrimeMeridianPoints(List<GPXUtilities.WptPt> points) {
      for(int i = 1; i < points.size(); ++i) {
         GPXUtilities.WptPt previous = points.get(i - 1);
         GPXUtilities.WptPt current = points.get(i);
         if (Math.abs(current.lon - previous.lon) >= 180.0) {
            GPXUtilities.WptPt projection = projectionOnPrimeMeridian(previous, current);
            GPXUtilities.WptPt oppositeSideProjection = new GPXUtilities.WptPt(projection);
            oppositeSideProjection.lon = -oppositeSideProjection.lon;
            points.addAll(i, Arrays.asList(projection, oppositeSideProjection));
            i += 2;
         }
      }
   }

   private static GPXUtilities.WptPt projectionOnPrimeMeridian(GPXUtilities.WptPt previous, GPXUtilities.WptPt next) {
      double lat = MapUtils.getProjection(0.0, 0.0, previous.lat, previous.lon, next.lat, next.lon).getLatitude();
      double lon = previous.lon < 0.0 ? -179.999991234 : 179.999991234;
      double projectionCoeff = MapUtils.getProjectionCoeff(0.0, 0.0, previous.lat, previous.lon, next.lat, next.lon);
      long time = (long)((double)previous.time + (double)(next.time - previous.time) * projectionCoeff);
      double ele = Double.isNaN(previous.ele + next.ele) ? Double.NaN : previous.ele + (next.ele - previous.ele) * projectionCoeff;
      double speed = previous.speed + (next.speed - previous.speed) * projectionCoeff;
      return new GPXUtilities.WptPt(lat, lon, time, ele, speed, Double.NaN);
   }

   public static class Author extends GPXUtilities.GPXExtensions {
      public String name;
      public String email;
      public String link;

      public Author() {
      }

      public Author(GPXUtilities.Author author) {
         this.name = author.name;
         this.email = author.email;
         this.link = author.link;
         this.copyExtensions(author);
      }
   }

   public static class Bounds extends GPXUtilities.GPXExtensions {
      public double minlat;
      public double minlon;
      public double maxlat;
      public double maxlon;

      public Bounds() {
      }

      public Bounds(GPXUtilities.Bounds source) {
         this.minlat = source.minlat;
         this.minlon = source.minlon;
         this.maxlat = source.maxlat;
         this.maxlon = source.maxlon;
         this.copyExtensions(source);
      }
   }

   public static class Copyright extends GPXUtilities.GPXExtensions {
      public String author;
      public String year;
      public String license;

      public Copyright() {
      }

      public Copyright(GPXUtilities.Copyright copyright) {
         this.author = copyright.author;
         this.year = copyright.year;
         this.license = copyright.license;
         this.copyExtensions(copyright);
      }
   }

   public static class Elevation {
      public float distance;
      public float timeDiff;
      public float elevation;
      public boolean firstPoint = false;
      public boolean lastPoint = false;
   }

   public static enum GPXColor {
      BLACK(-16777216),
      DARKGRAY(-12303292),
      GRAY(-7829368),
      LIGHTGRAY(-3355444),
      WHITE(-1),
      RED(-65536),
      GREEN(-16711936),
      DARKGREEN(-16751616),
      BLUE(-16776961),
      YELLOW(-256),
      CYAN(-16711681),
      MAGENTA(-65281),
      AQUA(-16711681),
      FUCHSIA(-65281),
      DARKGREY(-12303292),
      GREY(-7829368),
      LIGHTGREY(-3355444),
      LIME(-16711936),
      MAROON(-8388608),
      NAVY(-16777088),
      OLIVE(-8355840),
      PURPLE(-8388480),
      SILVER(-4144960),
      TEAL(-16744320);

      public final int color;

      private GPXColor(int color) {
         this.color = color;
      }

      public static GPXUtilities.GPXColor getColorFromName(String name) {
         for(GPXUtilities.GPXColor c : values()) {
            if (c.name().equalsIgnoreCase(name)) {
               return c;
            }
         }

         return null;
      }
   }

   public static class GPXExtensions {
      public Map<String, String> extensions = null;
      GPXUtilities.GPXExtensionsWriter extensionsWriter = null;

      public Map<String, String> getExtensionsToRead() {
         return this.extensions == null ? Collections.<String, String>emptyMap() : this.extensions;
      }

      public Map<String, String> getExtensionsToWrite() {
         if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
         }

         return this.extensions;
      }

      public void copyExtensions(GPXUtilities.GPXExtensions e) {
         Map<String, String> extensionsToRead = e.getExtensionsToRead();
         if (!extensionsToRead.isEmpty()) {
            this.getExtensionsToWrite().putAll(extensionsToRead);
         }
      }

      public GPXUtilities.GPXExtensionsWriter getExtensionsWriter() {
         return this.extensionsWriter;
      }

      public void setExtensionsWriter(GPXUtilities.GPXExtensionsWriter extensionsWriter) {
         this.extensionsWriter = extensionsWriter;
      }

      public int getColor(int defColor) {
         String clrValue = null;
         if (this.extensions != null) {
            clrValue = this.extensions.get("color");
            if (clrValue == null) {
               clrValue = this.extensions.get("colour");
            }

            if (clrValue == null) {
               clrValue = this.extensions.get("displaycolor");
            }

            if (clrValue == null) {
               clrValue = this.extensions.get("displaycolour");
            }
         }

         return GPXUtilities.parseColor(clrValue, defColor);
      }

      public void setColor(int color) {
         this.getExtensionsToWrite().put("color", Algorithms.colorToString(color));
      }

      public void removeColor() {
         this.getExtensionsToWrite().remove("color");
      }
   }

   public interface GPXExtensionsReader {
      boolean readExtensions(GPXUtilities.GPXFile var1, XmlPullParser var2) throws IOException, XmlPullParserException;
   }

   public interface GPXExtensionsWriter {
      void writeExtensions(XmlSerializer var1);
   }

   public static class GPXFile extends GPXUtilities.GPXExtensions {
      public String author;
      public GPXUtilities.Metadata metadata = new GPXUtilities.Metadata();
      public List<GPXUtilities.Track> tracks = new ArrayList<>();
      public List<GPXUtilities.Route> routes = new ArrayList<>();
      private final List<GPXUtilities.WptPt> points = new ArrayList<>();
      private Map<String, GPXUtilities.PointsGroup> pointsGroups = new LinkedHashMap<>();
      private final Map<String, String> networkRouteKeyTags = new LinkedHashMap<>();
      public Exception error = null;
      public String path = "";
      public boolean showCurrentTrack;
      public boolean hasAltitude;
      public long modifiedTime = 0L;
      public long pointsModifiedTime = 0L;
      private GPXUtilities.Track generalTrack;
      private GPXUtilities.TrkSegment generalSegment;

      public GPXFile(String author) {
         this.metadata.time = System.currentTimeMillis();
         this.author = author;
      }

      public GPXFile(String title, String lang, String description) {
         this.metadata.time = System.currentTimeMillis();
         if (description != null) {
            this.metadata.getExtensionsToWrite().put("desc", description);
         }

         if (lang != null) {
            this.metadata.getExtensionsToWrite().put("article_lang", lang);
         }

         if (title != null) {
            this.metadata.getExtensionsToWrite().put("article_title", title);
         }
      }

      public boolean hasRoute() {
         return this.getNonEmptyTrkSegments(true).size() > 0;
      }

      public List<GPXUtilities.WptPt> getPoints() {
         return Collections.unmodifiableList(this.points);
      }

      public List<GPXUtilities.WptPt> getAllSegmentsPoints() {
         List<GPXUtilities.WptPt> points = new ArrayList<>();

         for(GPXUtilities.Track track : this.tracks) {
            if (!track.generalTrack) {
               for(GPXUtilities.TrkSegment segment : track.segments) {
                  if (!segment.generalSegment) {
                     points.addAll(segment.points);
                  }
               }
            }
         }

         return points;
      }

      public boolean isPointsEmpty() {
         return this.points.isEmpty();
      }

      public int getPointsSize() {
         return this.points.size();
      }

      public boolean containsPoint(GPXUtilities.WptPt point) {
         return this.points.contains(point);
      }

      public void clearPoints() {
         this.points.clear();
         this.pointsGroups.clear();
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void addPoint(GPXUtilities.WptPt point) {
         this.points.add(point);
         this.addPointsToGroups(Collections.singleton(point));
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void addPoint(int position, GPXUtilities.WptPt point) {
         this.points.add(position, point);
         this.addPointsToGroups(Collections.singleton(point));
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void addPoints(Collection<? extends GPXUtilities.WptPt> collection) {
         this.points.addAll(collection);
         this.addPointsToGroups(collection);
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void addPointsGroup(GPXUtilities.PointsGroup group) {
         this.points.addAll(group.points);
         this.pointsGroups.put(group.name, group);
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void setPointsGroups(Map<String, GPXUtilities.PointsGroup> groups) {
         this.pointsGroups = groups;
      }

      private void addPointsToGroups(Collection<? extends GPXUtilities.WptPt> collection) {
         for(GPXUtilities.WptPt point : collection) {
            GPXUtilities.PointsGroup pointsGroup = this.getOrCreateGroup(point);
            pointsGroup.points.add(point);
         }
      }

      private GPXUtilities.PointsGroup getOrCreateGroup(GPXUtilities.WptPt point) {
         if (this.pointsGroups.containsKey(point.category)) {
            return this.pointsGroups.get(point.category);
         } else {
            GPXUtilities.PointsGroup pointsGroup = new GPXUtilities.PointsGroup(point);
            this.pointsGroups.put(pointsGroup.name, pointsGroup);
            return pointsGroup;
         }
      }

      public boolean deleteWptPt(GPXUtilities.WptPt point) {
         this.removePointFromGroup(point);
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
         return this.points.remove(point);
      }

      private void removePointFromGroup(GPXUtilities.WptPt point) {
         this.removePointFromGroup(point, point.category);
      }

      private void removePointFromGroup(GPXUtilities.WptPt point, String groupName) {
         GPXUtilities.PointsGroup group = this.pointsGroups.get(groupName);
         if (group != null) {
            group.points.remove(point);
            if (Algorithms.isEmpty(group.points)) {
               this.pointsGroups.remove(groupName);
            }
         }
      }

      public void updateWptPt(
         GPXUtilities.WptPt point, double lat, double lon, String description, String name, String category, int color, String iconName, String backgroundType
      ) {
         int index = this.points.indexOf(point);
         String prevGroupName = point.category;
         point.updatePoint(lat, lon, description, name, category, color, iconName, backgroundType);
         if (Algorithms.stringsEqual(category, prevGroupName) || Algorithms.isEmpty(category) && Algorithms.isEmpty(prevGroupName)) {
            this.removePointFromGroup(point, prevGroupName);
            GPXUtilities.PointsGroup pointsGroup = this.getOrCreateGroup(point);
            pointsGroup.points.add(point);
         }

         if (index != -1) {
            this.points.set(index, point);
         }

         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void updatePointsGroup(String prevGroupName, GPXUtilities.PointsGroup pointsGroup) {
         this.pointsGroups.remove(prevGroupName);
         this.pointsGroups.put(pointsGroup.name, pointsGroup);
         this.modifiedTime = System.currentTimeMillis();
      }

      public boolean isCloudmadeRouteFile() {
         return "cloudmade".equalsIgnoreCase(this.author);
      }

      public boolean hasGeneralTrack() {
         return this.generalTrack != null;
      }

      public void addGeneralTrack() {
         GPXUtilities.Track generalTrack = this.getGeneralTrack();
         if (generalTrack != null && !this.tracks.contains(generalTrack)) {
            this.tracks.add(0, generalTrack);
         }
      }

      public GPXUtilities.Track getGeneralTrack() {
         GPXUtilities.TrkSegment generalSegment = this.getGeneralSegment();
         if (this.generalTrack == null && generalSegment != null) {
            GPXUtilities.Track track = new GPXUtilities.Track();
            track.segments = new ArrayList<>();
            track.segments.add(generalSegment);
            this.generalTrack = track;
            track.generalTrack = true;
         }

         return this.generalTrack;
      }

      public GPXUtilities.TrkSegment getGeneralSegment() {
         if (this.generalSegment == null && this.getNonEmptySegmentsCount() > 1) {
            this.buildGeneralSegment();
         }

         return this.generalSegment;
      }

      private void buildGeneralSegment() {
         GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();

         for(GPXUtilities.Track track : this.tracks) {
            for(GPXUtilities.TrkSegment s : track.segments) {
               if (s.points.size() > 0) {
                  List<GPXUtilities.WptPt> waypoints = new ArrayList<>(s.points.size());

                  for(GPXUtilities.WptPt wptPt : s.points) {
                     waypoints.add(new GPXUtilities.WptPt(wptPt));
                  }

                  waypoints.get(0).firstPoint = true;
                  waypoints.get(waypoints.size() - 1).lastPoint = true;
                  segment.points.addAll(waypoints);
               }
            }
         }

         if (segment.points.size() > 0) {
            segment.generalSegment = true;
            this.generalSegment = segment;
         }
      }

      public GPXUtilities.GPXTrackAnalysis getAnalysis(long fileTimestamp) {
         return this.getAnalysis(fileTimestamp, null, null);
      }

      public GPXUtilities.GPXTrackAnalysis getAnalysis(long fileTimestamp, Double fromDistance, Double toDistance) {
         GPXUtilities.GPXTrackAnalysis analysis = new GPXUtilities.GPXTrackAnalysis();
         analysis.name = this.path;
         analysis.wptPoints = this.points.size();
         analysis.wptCategoryNames = this.getWaypointCategories();
         List<GPXUtilities.SplitSegment> segments = this.getSplitSegments(analysis, fromDistance, toDistance);
         analysis.prepareInformation(fileTimestamp, segments.toArray(new GPXUtilities.SplitSegment[0]));
         return analysis;
      }

      private List<GPXUtilities.SplitSegment> getSplitSegments(GPXUtilities.GPXTrackAnalysis g, Double fromDistance, Double toDistance) {
         List<GPXUtilities.SplitSegment> splitSegments = new ArrayList<>();

         for(int i = 0; i < this.tracks.size(); ++i) {
            GPXUtilities.Track subtrack = this.tracks.get(i);

            for(GPXUtilities.TrkSegment segment : subtrack.segments) {
               if (!segment.generalSegment) {
                  ++g.totalTracks;
                  if (segment.points.size() > 1) {
                     splitSegments.add(this.createSplitSegment(segment, fromDistance, toDistance));
                  }
               }
            }
         }

         return splitSegments;
      }

      private GPXUtilities.SplitSegment createSplitSegment(GPXUtilities.TrkSegment segment, Double fromDistance, Double toDistance) {
         if (fromDistance != null && toDistance != null) {
            int startInd = this.getPointIndexByDistance(segment.points, fromDistance);
            int endInd = this.getPointIndexByDistance(segment.points, toDistance);
            return new GPXUtilities.SplitSegment(startInd, endInd, segment);
         } else {
            return new GPXUtilities.SplitSegment(segment);
         }
      }

      public int getPointIndexByDistance(List<GPXUtilities.WptPt> points, double distance) {
         int index = 0;
         double minDistanceChange = Double.MAX_VALUE;

         for(int i = 0; i < points.size(); ++i) {
            GPXUtilities.WptPt point = points.get(i);
            double currentDistanceChange = Math.abs(point.distance - distance);
            if (currentDistanceChange < minDistanceChange) {
               minDistanceChange = currentDistanceChange;
               index = i;
            }
         }

         return index;
      }

      public boolean containsRoutePoint(GPXUtilities.WptPt point) {
         return this.getRoutePoints().contains(point);
      }

      public List<GPXUtilities.WptPt> getRoutePoints() {
         List<GPXUtilities.WptPt> points = new ArrayList<>();

         for(int i = 0; i < this.routes.size(); ++i) {
            GPXUtilities.Route rt = this.routes.get(i);
            points.addAll(rt.points);
         }

         return points;
      }

      public List<GPXUtilities.WptPt> getRoutePoints(int routeIndex) {
         List<GPXUtilities.WptPt> points = new ArrayList<>();
         if (this.routes.size() > routeIndex) {
            GPXUtilities.Route rt = this.routes.get(routeIndex);
            points.addAll(rt.points);
         }

         return points;
      }

      public boolean isAttachedToRoads() {
         List<GPXUtilities.WptPt> points = this.getRoutePoints();
         if (!Algorithms.isEmpty(points)) {
            for(GPXUtilities.WptPt wptPt : points) {
               if (Algorithms.isEmpty(wptPt.getProfileType())) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }

      public boolean hasRtePt() {
         for(GPXUtilities.Route r : this.routes) {
            if (r.points.size() > 0) {
               return true;
            }
         }

         return false;
      }

      public boolean hasWptPt() {
         return this.points.size() > 0;
      }

      public boolean hasTrkPt() {
         for(GPXUtilities.Track t : this.tracks) {
            for(GPXUtilities.TrkSegment ts : t.segments) {
               if (ts.points.size() > 0) {
                  return true;
               }
            }
         }

         return false;
      }

      public List<GPXUtilities.TrkSegment> getNonEmptyTrkSegments(boolean routesOnly) {
         List<GPXUtilities.TrkSegment> segments = new ArrayList<>();

         for(GPXUtilities.Track t : this.tracks) {
            for(GPXUtilities.TrkSegment s : t.segments) {
               if (!s.generalSegment && s.points.size() > 0 && (!routesOnly || s.hasRoute())) {
                  segments.add(s);
               }
            }
         }

         return segments;
      }

      public void addTrkSegment(List<GPXUtilities.WptPt> points) {
         this.removeGeneralTrackIfExists();
         GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
         segment.points.addAll(points);
         if (this.tracks.size() == 0) {
            this.tracks.add(new GPXUtilities.Track());
         }

         GPXUtilities.Track lastTrack = this.tracks.get(this.tracks.size() - 1);
         lastTrack.segments.add(segment);
         this.modifiedTime = System.currentTimeMillis();
      }

      public boolean replaceSegment(GPXUtilities.TrkSegment oldSegment, GPXUtilities.TrkSegment newSegment) {
         this.removeGeneralTrackIfExists();

         for(int i = 0; i < this.tracks.size(); ++i) {
            GPXUtilities.Track currentTrack = this.tracks.get(i);

            for(int j = 0; j < currentTrack.segments.size(); ++j) {
               int segmentIndex = currentTrack.segments.indexOf(oldSegment);
               if (segmentIndex != -1) {
                  currentTrack.segments.remove(segmentIndex);
                  currentTrack.segments.add(segmentIndex, newSegment);
                  this.addGeneralTrack();
                  this.modifiedTime = System.currentTimeMillis();
                  return true;
               }
            }
         }

         this.addGeneralTrack();
         return false;
      }

      public void addRoutePoints(List<GPXUtilities.WptPt> points, boolean addRoute) {
         if (this.routes.size() == 0 || addRoute) {
            GPXUtilities.Route route = new GPXUtilities.Route();
            this.routes.add(route);
         }

         GPXUtilities.Route lastRoute = this.routes.get(this.routes.size() - 1);
         lastRoute.points.addAll(points);
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      public void replaceRoutePoints(List<GPXUtilities.WptPt> points) {
         this.routes.clear();
         this.routes.add(new GPXUtilities.Route());
         GPXUtilities.Route currentRoute = this.routes.get(this.routes.size() - 1);
         currentRoute.points.addAll(points);
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;
      }

      private void removeGeneralTrackIfExists() {
         if (this.generalTrack != null) {
            this.tracks.remove(this.generalTrack);
            this.generalTrack = null;
            this.generalSegment = null;
         }
      }

      public boolean removeTrkSegment(GPXUtilities.TrkSegment segment) {
         this.removeGeneralTrackIfExists();

         for(int i = 0; i < this.tracks.size(); ++i) {
            GPXUtilities.Track currentTrack = this.tracks.get(i);

            for(int j = 0; j < currentTrack.segments.size(); ++j) {
               if (currentTrack.segments.remove(segment)) {
                  this.addGeneralTrack();
                  this.modifiedTime = System.currentTimeMillis();
                  return true;
               }
            }
         }

         this.addGeneralTrack();
         return false;
      }

      public boolean deleteRtePt(GPXUtilities.WptPt pt) {
         this.modifiedTime = System.currentTimeMillis();
         this.pointsModifiedTime = this.modifiedTime;

         for(GPXUtilities.Route route : this.routes) {
            if (route.points.remove(pt)) {
               return true;
            }
         }

         return false;
      }

      public List<GPXUtilities.TrkSegment> processRoutePoints() {
         List<GPXUtilities.TrkSegment> tpoints = new ArrayList<>();
         if (this.routes.size() > 0) {
            for(GPXUtilities.Route r : this.routes) {
               int routeColor = r.getColor(this.getColor(0));
               if (r.points.size() > 0) {
                  GPXUtilities.TrkSegment sgmt = new GPXUtilities.TrkSegment();
                  tpoints.add(sgmt);
                  sgmt.points.addAll(r.points);
                  sgmt.setColor(routeColor);
               }
            }
         }

         return tpoints;
      }

      public List<GPXUtilities.TrkSegment> proccessPoints() {
         List<GPXUtilities.TrkSegment> tpoints = new ArrayList<>();

         for(GPXUtilities.Track t : this.tracks) {
            int trackColor = t.getColor(this.getColor(0));

            for(GPXUtilities.TrkSegment ts : t.segments) {
               if (!ts.generalSegment && ts.points.size() > 0) {
                  GPXUtilities.TrkSegment sgmt = new GPXUtilities.TrkSegment();
                  tpoints.add(sgmt);
                  sgmt.points.addAll(ts.points);
                  sgmt.setColor(trackColor);
               }
            }
         }

         return tpoints;
      }

      public GPXUtilities.WptPt getLastPoint() {
         if (this.tracks.size() > 0) {
            GPXUtilities.Track tk = this.tracks.get(this.tracks.size() - 1);
            if (tk.segments.size() > 0) {
               GPXUtilities.TrkSegment ts = tk.segments.get(tk.segments.size() - 1);
               if (ts.points.size() > 0) {
                  return ts.points.get(ts.points.size() - 1);
               }
            }
         }

         return null;
      }

      public GPXUtilities.WptPt findPointToShow() {
         for(GPXUtilities.Track t : this.tracks) {
            for(GPXUtilities.TrkSegment s : t.segments) {
               if (s.points.size() > 0) {
                  return s.points.get(0);
               }
            }
         }

         for(GPXUtilities.Route s : this.routes) {
            if (s.points.size() > 0) {
               return s.points.get(0);
            }
         }

         return this.points.size() > 0 ? this.points.get(0) : null;
      }

      public boolean isEmpty() {
         for(GPXUtilities.Track t : this.tracks) {
            if (t.segments != null) {
               for(GPXUtilities.TrkSegment s : t.segments) {
                  boolean tracksEmpty = s.points.isEmpty();
                  if (!tracksEmpty) {
                     return false;
                  }
               }
            }
         }

         return this.points.isEmpty() && this.routes.isEmpty();
      }

      public List<GPXUtilities.Track> getTracks(boolean includeGeneralTrack) {
         List<GPXUtilities.Track> tracks = new ArrayList<>();

         for(GPXUtilities.Track track : this.tracks) {
            if (includeGeneralTrack || !track.generalTrack) {
               tracks.add(track);
            }
         }

         return tracks;
      }

      public int getTracksCount() {
         int count = 0;

         for(GPXUtilities.Track track : this.tracks) {
            if (!track.generalTrack) {
               ++count;
            }
         }

         return count;
      }

      public int getNonEmptyTracksCount() {
         int count = 0;

         for(GPXUtilities.Track track : this.tracks) {
            for(GPXUtilities.TrkSegment segment : track.segments) {
               if (segment.points.size() > 0) {
                  ++count;
                  break;
               }
            }
         }

         return count;
      }

      public int getNonEmptySegmentsCount() {
         int count = 0;

         for(GPXUtilities.Track t : this.tracks) {
            for(GPXUtilities.TrkSegment s : t.segments) {
               if (s.points.size() > 0) {
                  ++count;
               }
            }
         }

         return count;
      }

      public Set<String> getWaypointCategories() {
         return new HashSet<>(this.pointsGroups.keySet());
      }

      public Map<String, GPXUtilities.PointsGroup> getPointsGroups() {
         return this.pointsGroups;
      }

      public QuadRect getRect() {
         return this.getBounds(0.0, 0.0);
      }

      public QuadRect getBounds(double defaultMissingLat, double defaultMissingLon) {
         QuadRect qr = new QuadRect(defaultMissingLon, defaultMissingLat, defaultMissingLon, defaultMissingLat);

         for(GPXUtilities.Track track : this.tracks) {
            for(GPXUtilities.TrkSegment segment : track.segments) {
               for(GPXUtilities.WptPt p : segment.points) {
                  GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
               }
            }
         }

         for(GPXUtilities.WptPt p : this.points) {
            GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
         }

         for(GPXUtilities.Route route : this.routes) {
            for(GPXUtilities.WptPt p : route.points) {
               GPXUtilities.updateQR(qr, p, defaultMissingLat, defaultMissingLon);
            }
         }

         return qr;
      }

      public String getColoringType() {
         return this.extensions != null ? this.extensions.get("coloring_type") : null;
      }

      public String getGradientScaleType() {
         return this.extensions != null ? this.extensions.get("gradient_scale_type") : null;
      }

      public void setColoringType(String coloringType) {
         this.getExtensionsToWrite().put("coloring_type", coloringType);
      }

      public void removeGradientScaleType() {
         this.getExtensionsToWrite().remove("gradient_scale_type");
      }

      public String getSplitType() {
         return this.extensions != null ? this.extensions.get("split_type") : null;
      }

      public void setSplitType(String gpxSplitType) {
         this.getExtensionsToWrite().put("split_type", gpxSplitType);
      }

      public double getSplitInterval() {
         if (this.extensions != null) {
            String splitIntervalStr = this.extensions.get("split_interval");
            if (!Algorithms.isEmpty(splitIntervalStr)) {
               try {
                  return Double.parseDouble(splitIntervalStr);
               } catch (NumberFormatException var3) {
                  GPXUtilities.log.error("Error reading split_interval", var3);
               }
            }
         }

         return 0.0;
      }

      public void setSplitInterval(double splitInterval) {
         this.getExtensionsToWrite().put("split_interval", String.valueOf(splitInterval));
      }

      public String getWidth(String defWidth) {
         String widthValue = null;
         if (this.extensions != null) {
            widthValue = this.extensions.get("width");
         }

         return widthValue != null ? widthValue : defWidth;
      }

      public void setWidth(String width) {
         this.getExtensionsToWrite().put("width", width);
      }

      public boolean isShowArrows() {
         String showArrows = null;
         if (this.extensions != null) {
            showArrows = this.extensions.get("show_arrows");
         }

         return Boolean.parseBoolean(showArrows);
      }

      public void setShowArrows(boolean showArrows) {
         this.getExtensionsToWrite().put("show_arrows", String.valueOf(showArrows));
      }

      public boolean isShowStartFinish() {
         return this.extensions != null && this.extensions.containsKey("show_start_finish")
            ? Boolean.parseBoolean(this.extensions.get("show_start_finish"))
            : true;
      }

      public void setShowStartFinish(boolean showStartFinish) {
         this.getExtensionsToWrite().put("show_start_finish", String.valueOf(showStartFinish));
      }

      public void addRouteKeyTags(Map<String, String> routeKey) {
         this.networkRouteKeyTags.putAll(routeKey);
      }

      public Map<String, String> getRouteKeyTags() {
         return this.networkRouteKeyTags;
      }

      public void setRef(String ref) {
         this.getExtensionsToWrite().put("ref", ref);
      }

      public String getRef() {
         return this.extensions != null ? this.extensions.get("ref") : null;
      }

      public String getOuterRadius() {
         QuadRect rect = this.getRect();
         int radius = (int)MapUtils.getDistance(rect.bottom, rect.left, rect.top, rect.right);
         return MapUtils.convertDistToChar(radius, 'A', 5000, 2, 5);
      }

      public String getArticleTitle() {
         return this.metadata.getArticleTitle();
      }

      private int getItemsToWriteSize() {
         int size = this.getPointsSize();

         for(GPXUtilities.Route route : this.routes) {
            size += route.points.size();
         }

         for(GPXUtilities.TrkSegment segment : this.getNonEmptyTrkSegments(false)) {
            size += segment.points.size();
         }

         ++size;
         if (this.metadata.author != null) {
            ++size;
         }

         if (this.metadata.copyright != null) {
            ++size;
         }

         if (this.metadata.bounds != null) {
            ++size;
         }

         if (!this.getExtensionsToWrite().isEmpty() || this.getExtensionsWriter() != null) {
            ++size;
         }

         return size;
      }
   }

   public static class GPXFileResult {
      public ArrayList<List<Location>> locations = new ArrayList<>();
      public ArrayList<GPXUtilities.WptPt> wayPoints = new ArrayList<>();
      public boolean cloudMadeFile;
      public String error;

      public Location findFistLocation() {
         for(List<Location> l : this.locations) {
            for(Location ls : l) {
               if (ls != null) {
                  return ls;
               }
            }
         }

         return null;
      }
   }

   public static class GPXTrackAnalysis {
      public String name;
      public float totalDistance = 0.0F;
      public float totalDistanceWithoutGaps = 0.0F;
      public int totalTracks = 0;
      public long startTime = Long.MAX_VALUE;
      public long endTime = Long.MIN_VALUE;
      public long timeSpan = 0L;
      public long timeSpanWithoutGaps = 0L;
      public long timeMoving = 0L;
      public long timeMovingWithoutGaps = 0L;
      public float totalDistanceMoving = 0.0F;
      public float totalDistanceMovingWithoutGaps = 0.0F;
      public double diffElevationUp = 0.0;
      public double diffElevationDown = 0.0;
      public double avgElevation = 0.0;
      public double minElevation = 99999.0;
      public double maxElevation = -100.0;
      public float minSpeed = Float.MAX_VALUE;
      public float maxSpeed = 0.0F;
      public float avgSpeed;
      public double minHdop = Double.NaN;
      public double maxHdop = Double.NaN;
      public int points;
      public int wptPoints = 0;
      public Set<String> wptCategoryNames;
      public double metricEnd;
      public double secondaryMetricEnd;
      public GPXUtilities.WptPt locationStart;
      public GPXUtilities.WptPt locationEnd;
      public double left = 0.0;
      public double right = 0.0;
      public double top = 0.0;
      public double bottom = 0.0;
      public List<GPXUtilities.Elevation> elevationData;
      public List<GPXUtilities.Speed> speedData;
      public boolean hasElevationData;
      public boolean hasSpeedData;
      public boolean hasSpeedInTrack = false;

      public boolean isTimeSpecified() {
         return this.startTime != Long.MAX_VALUE && this.startTime != 0L;
      }

      public boolean isTimeMoving() {
         return this.timeMoving != 0L;
      }

      public boolean isElevationSpecified() {
         return this.maxElevation != -100.0;
      }

      public boolean hasSpeedInTrack() {
         return this.hasSpeedInTrack;
      }

      public boolean isBoundsCalculated() {
         return this.left != 0.0 && this.right != 0.0 && this.top != 0.0 && this.bottom != 0.0;
      }

      public boolean isSpeedSpecified() {
         return this.avgSpeed > 0.0F;
      }

      public boolean isHdopSpecified() {
         return this.minHdop > 0.0;
      }

      public boolean isColorizationTypeAvailable(RouteColorize.ColorizationType colorizationType) {
         if (colorizationType == RouteColorize.ColorizationType.SPEED) {
            return this.isSpeedSpecified();
         } else {
            return colorizationType != RouteColorize.ColorizationType.ELEVATION && colorizationType != RouteColorize.ColorizationType.SLOPE
               ? true
               : this.isElevationSpecified();
         }
      }

      public static GPXUtilities.GPXTrackAnalysis segment(long filetimestamp, GPXUtilities.TrkSegment segment) {
         return new GPXUtilities.GPXTrackAnalysis().prepareInformation(filetimestamp, new GPXUtilities.SplitSegment(segment));
      }

      public GPXUtilities.GPXTrackAnalysis prepareInformation(long filestamp, GPXUtilities.SplitSegment... splitSegments) {
         float[] calculations = new float[1];
         long startTimeOfSingleSegment = 0L;
         long endTimeOfSingleSegment = 0L;
         float distanceOfSingleSegment = 0.0F;
         float distanceMovingOfSingleSegment = 0.0F;
         long timeMovingOfSingleSegment = 0L;
         float totalElevation = 0.0F;
         int elevationPoints = 0;
         int speedCount = 0;
         long timeDiffMillis = 0L;
         int timeDiff = 0;
         double totalSpeedSum = 0.0;
         this.points = 0;
         this.elevationData = new ArrayList<>();
         this.speedData = new ArrayList<>();

         for(final GPXUtilities.SplitSegment s : splitSegments) {
            int numberOfPoints = s.getNumberOfPoints();
            float segmentDistance = 0.0F;
            this.metricEnd += s.metricEnd;
            this.secondaryMetricEnd += s.secondaryMetricEnd;
            this.points += numberOfPoints;

            for(int j = 0; j < numberOfPoints; ++j) {
               GPXUtilities.WptPt point = s.get(j);
               if (j == 0 && this.locationStart == null) {
                  this.locationStart = point;
               }

               if (j == numberOfPoints - 1) {
                  this.locationEnd = point;
               }

               long time = point.time;
               if (time != 0L) {
                  if (s.metricEnd == 0.0 && s.segment.generalSegment) {
                     if (point.firstPoint) {
                        startTimeOfSingleSegment = time;
                     } else if (point.lastPoint) {
                        endTimeOfSingleSegment = time;
                     }

                     if (startTimeOfSingleSegment != 0L && endTimeOfSingleSegment != 0L) {
                        this.timeSpanWithoutGaps += endTimeOfSingleSegment - startTimeOfSingleSegment;
                        startTimeOfSingleSegment = 0L;
                        endTimeOfSingleSegment = 0L;
                     }
                  }

                  this.startTime = Math.min(this.startTime, time);
                  this.endTime = Math.max(this.endTime, time);
               }

               if (this.left == 0.0 && this.right == 0.0) {
                  this.left = point.getLongitude();
                  this.right = point.getLongitude();
                  this.top = point.getLatitude();
                  this.bottom = point.getLatitude();
               } else {
                  this.left = Math.min(this.left, point.getLongitude());
                  this.right = Math.max(this.right, point.getLongitude());
                  this.top = Math.max(this.top, point.getLatitude());
                  this.bottom = Math.min(this.bottom, point.getLatitude());
               }

               double elevation = point.ele;
               GPXUtilities.Elevation elevation1 = new GPXUtilities.Elevation();
               if (!Double.isNaN(elevation)) {
                  totalElevation = (float)((double)totalElevation + elevation);
                  ++elevationPoints;
                  this.minElevation = Math.min(elevation, this.minElevation);
                  this.maxElevation = Math.max(elevation, this.maxElevation);
                  elevation1.elevation = (float)elevation;
               } else {
                  elevation1.elevation = Float.NaN;
               }

               float speed = (float)point.speed;
               if (speed > 0.0F) {
                  this.hasSpeedInTrack = true;
               }

               double hdop = point.hdop;
               if (hdop > 0.0) {
                  if (Double.isNaN(this.minHdop) || hdop < this.minHdop) {
                     this.minHdop = hdop;
                  }

                  if (Double.isNaN(this.maxHdop) || hdop > this.maxHdop) {
                     this.maxHdop = hdop;
                  }
               }

               if (j > 0) {
                  GPXUtilities.WptPt prev = s.get(j - 1);
                  Location.distanceBetween(prev.lat, prev.lon, point.lat, point.lon, calculations);
                  this.totalDistance += calculations[0];
                  segmentDistance += calculations[0];
                  point.distance = (double)segmentDistance;
                  timeDiffMillis = Math.max(0L, point.time - prev.time);
                  timeDiff = (int)(timeDiffMillis / 1000L);
                  if (!this.hasSpeedInTrack && speed == 0.0F && timeDiff > 0) {
                     speed = calculations[0] / (float)timeDiff;
                  }

                  boolean timeSpecified = point.time != 0L && prev.time != 0L;
                  if (speed > 0.0F && timeSpecified && calculations[0] > (float)timeDiffMillis / 10000.0F) {
                     this.timeMoving += timeDiffMillis;
                     this.totalDistanceMoving += calculations[0];
                     if (s.segment.generalSegment && !point.firstPoint) {
                        timeMovingOfSingleSegment += timeDiffMillis;
                        distanceMovingOfSingleSegment += calculations[0];
                     }
                  }
               }

               elevation1.timeDiff = (float)timeDiffMillis / 1000.0F;
               elevation1.distance = j > 0 ? calculations[0] : 0.0F;
               this.elevationData.add(elevation1);
               if (!this.hasElevationData && !Float.isNaN(elevation1.elevation) && this.totalDistance > 0.0F) {
                  this.hasElevationData = true;
               }

               this.minSpeed = Math.min(speed, this.minSpeed);
               if (speed > 0.0F) {
                  totalSpeedSum += (double)speed;
                  this.maxSpeed = Math.max(speed, this.maxSpeed);
                  ++speedCount;
               }

               GPXUtilities.Speed speed1 = new GPXUtilities.Speed();
               speed1.speed = speed;
               speed1.timeDiff = (float)timeDiffMillis / 1000.0F;
               speed1.distance = elevation1.distance;
               this.speedData.add(speed1);
               if (!this.hasSpeedData && speed1.speed > 0.0F && this.totalDistance > 0.0F) {
                  this.hasSpeedData = true;
               }

               if (s.segment.generalSegment) {
                  distanceOfSingleSegment += calculations[0];
                  if (point.firstPoint) {
                     distanceOfSingleSegment = 0.0F;
                     timeMovingOfSingleSegment = 0L;
                     distanceMovingOfSingleSegment = 0.0F;
                     if (j > 0) {
                        elevation1.firstPoint = true;
                        speed1.firstPoint = true;
                     }
                  }

                  if (point.lastPoint) {
                     this.totalDistanceWithoutGaps += distanceOfSingleSegment;
                     this.timeMovingWithoutGaps += timeMovingOfSingleSegment;
                     this.totalDistanceMovingWithoutGaps += distanceMovingOfSingleSegment;
                     if (j < numberOfPoints - 1) {
                        elevation1.lastPoint = true;
                        speed1.lastPoint = true;
                     }
                  }
               }
            }

            GPXUtilities.GPXTrackAnalysis.ElevationDiffsCalculator elevationDiffsCalc = new GPXUtilities.GPXTrackAnalysis.ElevationDiffsCalculator(
               0, numberOfPoints
            ) {
               @Override
               public GPXUtilities.WptPt getPoint(int index) {
                  return s.get(index);
               }
            };
            elevationDiffsCalc.calculateElevationDiffs();
            this.diffElevationUp += elevationDiffsCalc.diffElevationUp;
            this.diffElevationDown += elevationDiffsCalc.diffElevationDown;
         }

         if (this.totalDistance < 0.0F) {
            this.hasElevationData = false;
            this.hasSpeedData = false;
         }

         if (!this.isTimeSpecified()) {
            this.startTime = filestamp;
            this.endTime = filestamp;
         }

         if (this.timeSpan == 0L) {
            this.timeSpan = this.endTime - this.startTime;
         }

         if (elevationPoints > 0) {
            this.avgElevation = (double)(totalElevation / (float)elevationPoints);
         }

         if (speedCount > 0) {
            if (this.timeMoving > 0L) {
               this.avgSpeed = this.totalDistanceMoving / (float)this.timeMoving * 1000.0F;
            } else {
               this.avgSpeed = (float)totalSpeedSum / (float)speedCount;
            }
         } else {
            this.avgSpeed = -1.0F;
         }

         return this;
      }

      public abstract static class ElevationDiffsCalculator {
         public static final double CALCULATED_GPX_WINDOW_LENGTH = 10.0;
         private double windowLength;
         private final int startIndex;
         private final int numberOfPoints;
         private double diffElevationUp = 0.0;
         private double diffElevationDown = 0.0;

         public ElevationDiffsCalculator(int startIndex, int numberOfPoints) {
            this.startIndex = startIndex;
            this.numberOfPoints = numberOfPoints;
            GPXUtilities.WptPt lastPoint = this.getPoint(startIndex + numberOfPoints - 1);
            this.windowLength = lastPoint.time == 0L ? 10.0 : Math.max(20.0, lastPoint.distance / (double)numberOfPoints * 4.0);
         }

         public ElevationDiffsCalculator(double windowLength, int startIndex, int numberOfPoints) {
            this(startIndex, numberOfPoints);
            this.windowLength = windowLength;
         }

         public abstract GPXUtilities.WptPt getPoint(int var1);

         public double getDiffElevationUp() {
            return this.diffElevationUp;
         }

         public double getDiffElevationDown() {
            return this.diffElevationDown;
         }

         public void calculateElevationDiffs() {
            GPXUtilities.WptPt initialPoint = this.getPoint(this.startIndex);
            double eleSumm = initialPoint.ele;
            double prevEle = initialPoint.ele;
            int pointsCount = Double.isNaN(eleSumm) ? 0 : 1;
            double eleAvg = Double.NaN;
            double nextWindowPos = initialPoint.distance + this.windowLength;

            for(int pointIndex = this.startIndex + 1; pointIndex < this.numberOfPoints + this.startIndex; ++pointIndex) {
               GPXUtilities.WptPt point = this.getPoint(pointIndex);
               if (point.distance > nextWindowPos) {
                  eleAvg = this.calcAvg(eleSumm, pointsCount, eleAvg);
                  if (!Double.isNaN(point.ele)) {
                     eleSumm = point.ele;
                     prevEle = point.ele;
                     pointsCount = 1;
                  } else if (!Double.isNaN(prevEle)) {
                     eleSumm = prevEle;
                     pointsCount = 1;
                  } else {
                     eleSumm = Double.NaN;
                     pointsCount = 0;
                  }

                  while(nextWindowPos < point.distance) {
                     nextWindowPos += this.windowLength;
                  }
               } else if (!Double.isNaN(point.ele)) {
                  eleSumm += point.ele;
                  prevEle = point.ele;
                  ++pointsCount;
               } else if (!Double.isNaN(prevEle)) {
                  eleSumm += prevEle;
                  ++pointsCount;
               }
            }

            if (pointsCount > 1) {
               this.calcAvg(eleSumm, pointsCount, eleAvg);
            }

            this.diffElevationUp = (double)Math.round(this.diffElevationUp + 0.3F);
         }

         private double calcAvg(double eleSumm, int pointsCount, double eleAvg) {
            if (!Double.isNaN(eleSumm) && pointsCount != 0) {
               double avg = eleSumm / (double)pointsCount;
               if (!Double.isNaN(eleAvg)) {
                  double diff = avg - eleAvg;
                  if (diff > 0.0) {
                     this.diffElevationUp += diff;
                  } else {
                     this.diffElevationDown -= diff;
                  }
               }

               return avg;
            } else {
               return Double.NaN;
            }
         }
      }
   }

   public static class Metadata extends GPXUtilities.GPXExtensions {
      public String name;
      public String desc;
      public String link;
      public String keywords;
      public long time = 0L;
      public GPXUtilities.Author author = null;
      public GPXUtilities.Copyright copyright = null;
      public GPXUtilities.Bounds bounds = null;

      public Metadata() {
      }

      public Metadata(GPXUtilities.Metadata source) {
         this.name = source.name;
         this.desc = source.desc;
         this.link = source.link;
         this.keywords = source.keywords;
         this.time = source.time;
         if (source.author != null) {
            this.author = new GPXUtilities.Author(source.author);
         }

         if (source.copyright != null) {
            this.copyright = new GPXUtilities.Copyright(source.copyright);
         }

         if (source.bounds != null) {
            this.bounds = new GPXUtilities.Bounds(source.bounds);
         }

         this.copyExtensions(source);
      }

      public String getArticleTitle() {
         return this.getExtensionsToRead().get("article_title");
      }

      public String getArticleLang() {
         return this.getExtensionsToRead().get("article_lang");
      }

      public String getDescription() {
         return this.getExtensionsToRead().get("desc");
      }
   }

   public static class PointsGroup {
      public final String name;
      public String iconName;
      public String backgroundType;
      public List<GPXUtilities.WptPt> points = new ArrayList<>();
      public int color;

      public PointsGroup(String name) {
         this.name = name != null ? name : "";
      }

      public PointsGroup(String name, String iconName, String backgroundType, int color) {
         this(name);
         this.color = color;
         this.iconName = iconName;
         this.backgroundType = backgroundType;
      }

      public PointsGroup(GPXUtilities.WptPt point) {
         this(point.category);
         this.color = point.getColor();
         this.iconName = point.getIconName();
         this.backgroundType = point.getBackgroundType();
      }

      @Override
      public int hashCode() {
         return Algorithms.hash(this.name, this.iconName, this.backgroundType, this.color, this.points);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            GPXUtilities.PointsGroup that = (GPXUtilities.PointsGroup)o;
            return this.color == that.color
               && Algorithms.objectEquals(this.points, that.points)
               && Algorithms.stringsEqual(this.name, that.name)
               && Algorithms.stringsEqual(this.iconName, that.iconName)
               && Algorithms.stringsEqual(this.backgroundType, that.backgroundType);
         } else {
            return false;
         }
      }

      public StringBundle toStringBundle() {
         StringBundle bundle = new StringBundle();
         bundle.putString("name", this.name != null ? this.name : "");
         if (this.color != 0) {
            bundle.putString("color", Algorithms.colorToString(this.color));
         }

         if (!Algorithms.isEmpty(this.iconName)) {
            bundle.putString("icon", this.iconName);
         }

         if (!Algorithms.isEmpty(this.backgroundType)) {
            bundle.putString("background", this.backgroundType);
         }

         return bundle;
      }

      private static GPXUtilities.PointsGroup parsePointsGroupAttributes(XmlPullParser parser) {
         String name = parser.getAttributeValue("", "name");
         GPXUtilities.PointsGroup category = new GPXUtilities.PointsGroup(name != null ? name : "");
         category.color = GPXUtilities.parseColor(parser.getAttributeValue("", "color"), 0);
         category.iconName = parser.getAttributeValue("", "icon");
         category.backgroundType = parser.getAttributeValue("", "background");
         return category;
      }
   }

   public static class Route extends GPXUtilities.GPXExtensions {
      public String name = null;
      public String desc = null;
      public List<GPXUtilities.WptPt> points = new ArrayList<>();
   }

   public static class RouteSegment {
      public static final String START_TRKPT_IDX_ATTR = "startTrkptIdx";
      public String id;
      public String length;
      public String startTrackPointIndex;
      public String segmentTime;
      public String speed;
      public String turnType;
      public String turnLanes;
      public String turnAngle;
      public String skipTurn;
      public String types;
      public String pointTypes;
      public String names;

      public static GPXUtilities.RouteSegment fromStringBundle(StringBundle bundle) {
         GPXUtilities.RouteSegment s = new GPXUtilities.RouteSegment();
         s.id = bundle.getString("id", null);
         s.length = bundle.getString("length", null);
         s.startTrackPointIndex = bundle.getString("startTrkptIdx", null);
         s.segmentTime = bundle.getString("segmentTime", null);
         s.speed = bundle.getString("speed", null);
         s.turnType = bundle.getString("turnType", null);
         s.turnLanes = bundle.getString("turnLanes", null);
         s.turnAngle = bundle.getString("turnAngle", null);
         s.skipTurn = bundle.getString("skipTurn", null);
         s.types = bundle.getString("types", null);
         s.pointTypes = bundle.getString("pointTypes", null);
         s.names = bundle.getString("names", null);
         return s;
      }

      public StringBundle toStringBundle() {
         StringBundle bundle = new StringBundle();
         bundle.putString("id", this.id);
         bundle.putString("length", this.length);
         bundle.putString("startTrkptIdx", this.startTrackPointIndex);
         bundle.putString("segmentTime", this.segmentTime);
         bundle.putString("speed", this.speed);
         bundle.putString("turnType", this.turnType);
         bundle.putString("turnLanes", this.turnLanes);
         bundle.putString("turnAngle", this.turnAngle);
         bundle.putString("skipTurn", this.skipTurn);
         bundle.putString("types", this.types);
         bundle.putString("pointTypes", this.pointTypes);
         bundle.putString("names", this.names);
         return bundle;
      }
   }

   public static class RouteType {
      public String tag;
      public String value;

      public static GPXUtilities.RouteType fromStringBundle(StringBundle bundle) {
         GPXUtilities.RouteType t = new GPXUtilities.RouteType();
         t.tag = bundle.getString("t", null);
         t.value = bundle.getString("v", null);
         return t;
      }

      public StringBundle toStringBundle() {
         StringBundle bundle = new StringBundle();
         bundle.putString("t", this.tag);
         bundle.putString("v", this.value);
         return bundle;
      }
   }

   public static class Speed {
      public float distance;
      public float timeDiff;
      public float speed;
      public boolean firstPoint = false;
      public boolean lastPoint = false;
   }

   private abstract static class SplitMetric {
      private SplitMetric() {
      }

      public abstract double metric(GPXUtilities.WptPt var1, GPXUtilities.WptPt var2);
   }

   private static class SplitSegment {
      GPXUtilities.TrkSegment segment;
      double startCoeff = 0.0;
      int startPointInd;
      double endCoeff = 0.0;
      int endPointInd;
      double metricEnd;
      double secondaryMetricEnd;

      public SplitSegment(GPXUtilities.TrkSegment s) {
         this.startPointInd = 0;
         this.startCoeff = 0.0;
         this.endPointInd = s.points.size() - 2;
         this.endCoeff = 1.0;
         this.segment = s;
      }

      public SplitSegment(int startInd, int endInd, GPXUtilities.TrkSegment s) {
         this.startPointInd = startInd;
         this.startCoeff = 0.0;
         this.endPointInd = endInd - 2;
         this.endCoeff = 1.0;
         this.segment = s;
      }

      public SplitSegment(GPXUtilities.TrkSegment s, int pointInd, double cf) {
         this.segment = s;
         this.startPointInd = pointInd;
         this.startCoeff = cf;
      }

      public int getNumberOfPoints() {
         return this.endPointInd - this.startPointInd + 2;
      }

      public GPXUtilities.WptPt get(int j) {
         int ind = j + this.startPointInd;
         if (j == 0) {
            return this.startCoeff == 0.0
               ? this.segment.points.get(ind)
               : this.approx(this.segment.points.get(ind), this.segment.points.get(ind + 1), this.startCoeff);
         } else if (j == this.getNumberOfPoints() - 1) {
            return this.endCoeff == 1.0
               ? this.segment.points.get(ind)
               : this.approx(this.segment.points.get(ind - 1), this.segment.points.get(ind), this.endCoeff);
         } else {
            return this.segment.points.get(ind);
         }
      }

      private GPXUtilities.WptPt approx(GPXUtilities.WptPt w1, GPXUtilities.WptPt w2, double cf) {
         long time = this.value(w1.time, w2.time, 0L, cf);
         double speed = this.value(w1.speed, w2.speed, 0.0, cf);
         double ele = this.value(w1.ele, w2.ele, 0.0, cf);
         double hdop = this.value(w1.hdop, w2.hdop, 0.0, cf);
         double lat = this.value(w1.lat, w2.lat, -360.0, cf);
         double lon = this.value(w1.lon, w2.lon, -360.0, cf);
         return new GPXUtilities.WptPt(lat, lon, time, ele, speed, hdop);
      }

      private double value(double vl, double vl2, double none, double cf) {
         if (vl == none || Double.isNaN(vl)) {
            return vl2;
         } else {
            return vl2 != none && !Double.isNaN(vl2) ? vl + cf * (vl2 - vl) : vl;
         }
      }

      private long value(long vl, long vl2, long none, double cf) {
         if (vl == none) {
            return vl2;
         } else {
            return vl2 == none ? vl : vl + (long)(cf * (double)(vl2 - vl));
         }
      }

      public double setLastPoint(int pointInd, double endCf) {
         this.endCoeff = endCf;
         this.endPointInd = pointInd;
         return this.endCoeff;
      }
   }

   public static class Track extends GPXUtilities.GPXExtensions {
      public String name = null;
      public String desc = null;
      public List<GPXUtilities.TrkSegment> segments = new ArrayList<>();
      public boolean generalTrack = false;
   }

   public static class TrkSegment extends GPXUtilities.GPXExtensions {
      public String name = null;
      public boolean generalSegment = false;
      public List<GPXUtilities.WptPt> points = new ArrayList<>();
      public Object renderer;
      public List<GPXUtilities.RouteSegment> routeSegments = new ArrayList<>();
      public List<GPXUtilities.RouteType> routeTypes = new ArrayList<>();

      public boolean hasRoute() {
         return !this.routeSegments.isEmpty() && !this.routeTypes.isEmpty();
      }

      public List<GPXUtilities.GPXTrackAnalysis> splitByDistance(double meters, boolean joinSegments) {
         return this.split(GPXUtilities.getDistanceMetric(), GPXUtilities.getTimeSplit(), meters, joinSegments);
      }

      public List<GPXUtilities.GPXTrackAnalysis> splitByTime(int seconds, boolean joinSegments) {
         return this.split(GPXUtilities.getTimeSplit(), GPXUtilities.getDistanceMetric(), (double)seconds, joinSegments);
      }

      private List<GPXUtilities.GPXTrackAnalysis> split(
         GPXUtilities.SplitMetric metric, GPXUtilities.SplitMetric secondaryMetric, double metricLimit, boolean joinSegments
      ) {
         List<GPXUtilities.SplitSegment> splitSegments = new ArrayList<>();
         GPXUtilities.splitSegment(metric, secondaryMetric, metricLimit, splitSegments, this, joinSegments);
         return GPXUtilities.convert(splitSegments);
      }
   }

   public static class WptPt extends GPXUtilities.GPXExtensions {
      public boolean firstPoint = false;
      public boolean lastPoint = false;
      public double lat;
      public double lon;
      public String name = null;
      public String link = null;
      public String category = null;
      public String desc = null;
      public String comment = null;
      public long time = 0L;
      public double ele = Double.NaN;
      public double speed = 0.0;
      public double hdop = Double.NaN;
      public float heading = Float.NaN;
      public boolean deleted = false;
      public int speedColor = 0;
      public int altitudeColor = 0;
      public int slopeColor = 0;
      public int colourARGB = 0;
      public double distance = 0.0;

      public WptPt() {
      }

      public WptPt(GPXUtilities.WptPt wptPt) {
         this.lat = wptPt.lat;
         this.lon = wptPt.lon;
         this.name = wptPt.name;
         this.link = wptPt.link;
         this.category = wptPt.category;
         this.desc = wptPt.desc;
         this.comment = wptPt.comment;
         this.time = wptPt.time;
         this.ele = wptPt.ele;
         this.speed = wptPt.speed;
         this.hdop = wptPt.hdop;
         this.heading = wptPt.heading;
         this.deleted = wptPt.deleted;
         this.speedColor = wptPt.speedColor;
         this.altitudeColor = wptPt.altitudeColor;
         this.slopeColor = wptPt.slopeColor;
         this.colourARGB = wptPt.colourARGB;
         this.distance = wptPt.distance;
      }

      public void setDistance(double dist) {
         this.distance = dist;
      }

      public double getDistance() {
         return this.distance;
      }

      public int getColor() {
         return this.getColor(0);
      }

      public double getLatitude() {
         return this.lat;
      }

      public double getLongitude() {
         return this.lon;
      }

      public float getHeading() {
         return this.heading;
      }

      public WptPt(double lat, double lon, long time, double ele, double speed, double hdop) {
         this(lat, lon, time, ele, speed, hdop, Float.NaN);
      }

      public WptPt(double lat, double lon, long time, double ele, double speed, double hdop, float heading) {
         this.lat = lat;
         this.lon = lon;
         this.time = time;
         this.ele = ele;
         this.speed = speed;
         this.hdop = hdop;
         this.heading = heading;
      }

      public boolean isVisible() {
         return true;
      }

      public String getIconName() {
         return this.getExtensionsToRead().get("icon");
      }

      public String getIconNameOrDefault() {
         String iconName = this.getIconName();
         if (iconName == null) {
            iconName = "special_star";
         }

         return iconName;
      }

      public void setIconName(String iconName) {
         this.getExtensionsToWrite().put("icon", iconName);
      }

      public String getAmenityOriginName() {
         Map<String, String> extensionsToRead = this.getExtensionsToRead();
         return !extensionsToRead.isEmpty() ? extensionsToRead.get("amenity_origin") : null;
      }

      public void setAmenityOriginName(String originName) {
         this.getExtensionsToWrite().put("amenity_origin", originName);
      }

      public int getColor(RouteColorize.ColorizationType type) {
         if (type == RouteColorize.ColorizationType.SPEED) {
            return this.speedColor;
         } else {
            return type == RouteColorize.ColorizationType.ELEVATION ? this.altitudeColor : this.slopeColor;
         }
      }

      public void setColor(RouteColorize.ColorizationType type, int color) {
         if (type == RouteColorize.ColorizationType.SPEED) {
            this.speedColor = color;
         } else if (type == RouteColorize.ColorizationType.ELEVATION) {
            this.altitudeColor = color;
         } else if (type == RouteColorize.ColorizationType.SLOPE) {
            this.slopeColor = color;
         }
      }

      public String getBackgroundType() {
         return this.getExtensionsToRead().get("background");
      }

      public void setBackgroundType(String backType) {
         this.getExtensionsToWrite().put("background", backType);
      }

      public String getProfileType() {
         return this.getExtensionsToRead().get("profile");
      }

      public String getAddress() {
         return this.getExtensionsToRead().get("address");
      }

      public void setAddress(String address) {
         if (Algorithms.isBlank(address)) {
            this.getExtensionsToWrite().remove("address");
         } else {
            this.getExtensionsToWrite().put("address", address);
         }
      }

      public void setProfileType(String profileType) {
         this.getExtensionsToWrite().put("profile", profileType);
      }

      public boolean hasProfile() {
         String profileType = this.getProfileType();
         return profileType != null && !"gap".equals(profileType);
      }

      public boolean isGap() {
         String profileType = this.getProfileType();
         return "gap".equals(profileType);
      }

      public void setGap() {
         this.setProfileType("gap");
      }

      public void removeProfileType() {
         this.getExtensionsToWrite().remove("profile");
      }

      public int getTrkPtIndex() {
         try {
            return Integer.parseInt(this.getExtensionsToRead().get("trkpt_idx"));
         } catch (NumberFormatException var2) {
            return -1;
         }
      }

      public void setTrkPtIndex(int index) {
         this.getExtensionsToWrite().put("trkpt_idx", String.valueOf(index));
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
         result = 31 * result + (this.category == null ? 0 : this.category.hashCode());
         result = 31 * result + (this.desc == null ? 0 : this.desc.hashCode());
         result = 31 * result + (this.comment == null ? 0 : this.comment.hashCode());
         result = 31 * result + (this.lat == 0.0 ? 0 : Double.valueOf(this.lat).hashCode());
         return 31 * result + (this.lon == 0.0 ? 0 : Double.valueOf(this.lon).hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj != null && this.getClass() == obj.getClass()) {
            GPXUtilities.WptPt other = (GPXUtilities.WptPt)obj;
            return Algorithms.objectEquals(other.name, this.name)
               && Algorithms.objectEquals(other.category, this.category)
               && Algorithms.objectEquals(other.lat, this.lat)
               && Algorithms.objectEquals(other.lon, this.lon)
               && Algorithms.objectEquals(other.desc, this.desc);
         } else {
            return false;
         }
      }

      public boolean hasLocation() {
         return this.lat != 0.0 && this.lon != 0.0;
      }

      public static GPXUtilities.WptPt createAdjustedPoint(
         double lat,
         double lon,
         String description,
         String name,
         String category,
         int color,
         String iconName,
         String backgroundType,
         String amenityOriginName,
         Map<String, String> amenityExtensions
      ) {
         double latAdjusted = Double.parseDouble(GPXUtilities.LAT_LON_FORMAT.format(lat));
         double lonAdjusted = Double.parseDouble(GPXUtilities.LAT_LON_FORMAT.format(lon));
         GPXUtilities.WptPt point = new GPXUtilities.WptPt(latAdjusted, lonAdjusted, System.currentTimeMillis(), Double.NaN, 0.0, Double.NaN);
         point.name = name;
         point.category = category;
         point.desc = description;
         if (color != 0) {
            point.setColor(color);
         }

         if (iconName != null) {
            point.setIconName(iconName);
         }

         if (backgroundType != null) {
            point.setBackgroundType(backgroundType);
         }

         if (amenityOriginName != null) {
            point.setAmenityOriginName(amenityOriginName);
         }

         if (amenityExtensions != null) {
            point.getExtensionsToWrite().putAll(amenityExtensions);
         }

         return point;
      }

      private void updatePoint(double lat, double lon, String description, String name, String category, int color, String iconName, String backgroundType) {
         this.lat = Double.parseDouble(GPXUtilities.LAT_LON_FORMAT.format(lat));
         this.lon = Double.parseDouble(GPXUtilities.LAT_LON_FORMAT.format(lon));
         this.time = System.currentTimeMillis();
         this.desc = description;
         this.name = name;
         this.category = category;
         if (color != 0) {
            this.setColor(color);
         }

         if (iconName != null) {
            this.setIconName(iconName);
         }

         if (backgroundType != null) {
            this.setBackgroundType(backgroundType);
         }
      }
   }
}
