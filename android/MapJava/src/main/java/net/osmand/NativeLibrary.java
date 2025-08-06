package net.osmand;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.NativeTransportRoutingResult;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.TransportRoutingConfiguration;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

public class NativeLibrary {
   private static final Log log = PlatformUtil.getLog(NativeLibrary.class);

   public NativeLibrary.NativeSearchResult searchObjectsForRendering(
      int sleft,
      int sright,
      int stop,
      int sbottom,
      int zoom,
      RenderingRuleSearchRequest request,
      boolean skipDuplicates,
      Object objectWithInterruptedField,
      String msgIfNothingFound
   ) {
      int renderRouteDataFile = 0;
      if (request.searchRenderingAttribute("showRoadMapsAttribute")) {
         renderRouteDataFile = request.getIntPropertyValue(request.ALL.R_ATTR_INT_VALUE);
      }

      return new NativeLibrary.NativeSearchResult(
         searchNativeObjectsForRendering(
            sleft, sright, stop, sbottom, zoom, request, skipDuplicates, renderRouteDataFile, objectWithInterruptedField, msgIfNothingFound
         )
      );
   }

   public RouteDataObject[] getDataObjects(NativeLibrary.NativeRouteSearchResult rs, int x31, int y31) {
      return rs.nativeHandler == 0L ? new RouteDataObject[0] : getRouteDataObjects(rs.region.routeReg, rs.nativeHandler, x31, y31);
   }

   public boolean initMapFile(String filePath, boolean useLive) {
      return initBinaryMapFile(filePath, useLive, false);
   }

   public boolean initCacheMapFile(String filePath) {
      return initCacheMapFiles(filePath);
   }

   public boolean closeMapFile(String filePath) {
      return closeBinaryMapFile(filePath);
   }

   public NativeTransportRoutingResult[] runNativePTRouting(
      int sx31, int sy31, int ex31, int ey31, TransportRoutingConfiguration cfg, RouteCalculationProgress progress
   ) {
      return nativeTransportRouting(new int[]{sx31, sy31, ex31, ey31}, cfg, progress);
   }

   public RouteSegmentResult[] runNativeRouting(RoutingContext c, BinaryMapRouteReaderAdapter.RouteRegion[] regions, boolean basemap) {
      return nativeRouting(c, c.config.initialDirection == null ? -360.0F : c.config.initialDirection.floatValue(), regions, basemap);
   }

   public RoutePlannerFrontEnd.GpxRouteApproximation runNativeSearchGpxRoute(
      RoutePlannerFrontEnd.GpxRouteApproximation gCtx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints
   ) {
      BinaryMapRouteReaderAdapter.RouteRegion[] regions = gCtx.ctx.reverseMap.keySet().toArray(new BinaryMapRouteReaderAdapter.RouteRegion[0]);
      int pointsSize = gpxPoints.size();
      NativeLibrary.NativeGpxPointApproximation[] nativePoints = new NativeLibrary.NativeGpxPointApproximation[pointsSize];

      for(int i = 0; i < pointsSize; ++i) {
         nativePoints[i] = new NativeLibrary.NativeGpxPointApproximation(gpxPoints.get(i));
      }

      NativeLibrary.NativeGpxRouteApproximationResult nativeResult = nativeSearchGpxRoute(gCtx.ctx, nativePoints, regions);

      for(NativeLibrary.NativeGpxPointApproximation point : nativeResult.finalPoints) {
         gCtx.finalPoints.add(point.convertToGpxPoint());
      }

      List<RouteSegmentResult> results = nativeResult.result;

      for(RouteSegmentResult rsr : results) {
         this.initRouteRegion(gCtx, rsr);
      }

      gCtx.result.addAll(results);
      return gCtx;
   }

   private void initRouteRegion(RoutePlannerFrontEnd.GpxRouteApproximation gCtx, RouteSegmentResult rsr) {
      BinaryMapRouteReaderAdapter.RouteRegion region = rsr.getObject().region;
      BinaryMapIndexReader reader = gCtx.ctx.reverseMap.get(region);
      if (reader != null) {
         try {
            reader.initRouteRegion(region);
         } catch (IOException var6) {
            var6.printStackTrace();
         }
      }
   }

   public NativeLibrary.NativeRouteSearchResult loadRouteRegion(BinaryMapRouteReaderAdapter.RouteSubregion sub, boolean loadObjects) {
      NativeLibrary.NativeRouteSearchResult lr = loadRoutingData(sub.routeReg, sub.routeReg.getName(), sub.routeReg.getFilePointer(), sub, loadObjects);
      if (lr != null && lr.nativeHandler != 0L) {
         lr.region = sub;
      }

      return lr;
   }

   public void clearCachedRenderingRulesStorage() {
      clearRenderingRulesStorage();
   }

   protected static native NativeLibrary.NativeGpxRouteApproximationResult nativeSearchGpxRoute(
      RoutingContext var0, NativeLibrary.NativeGpxPointApproximation[] var1, BinaryMapRouteReaderAdapter.RouteRegion[] var2
   );

   protected static native NativeLibrary.NativeRouteSearchResult loadRoutingData(
      BinaryMapRouteReaderAdapter.RouteRegion var0, String var1, int var2, BinaryMapRouteReaderAdapter.RouteSubregion var3, boolean var4
   );

   public static native void deleteNativeRoutingContext(long var0);

   protected static native void deleteRenderingContextHandle(long var0);

   protected static native void deleteRouteSearchResult(long var0);

   protected static native RouteDataObject[] getRouteDataObjects(BinaryMapRouteReaderAdapter.RouteRegion var0, long var1, int var3, int var4);

   protected static native RouteSegmentResult[] nativeRouting(RoutingContext var0, float var1, BinaryMapRouteReaderAdapter.RouteRegion[] var2, boolean var3);

   protected static native NativeTransportRoutingResult[] nativeTransportRouting(int[] var0, TransportRoutingConfiguration var1, RouteCalculationProgress var2);

   protected static native void deleteSearchResult(long var0);

   protected static native boolean initBinaryMapFile(String var0, boolean var1, boolean var2);

   protected static native boolean initCacheMapFiles(String var0);

   protected static native boolean closeBinaryMapFile(String var0);

   protected static native void initRenderingRulesStorage(RenderingRulesStorage var0);

   protected static native void clearRenderingRulesStorage();

   protected static native NativeLibrary.RenderingGenerationResult generateRenderingIndirect(
      RenderingContext var0, long var1, boolean var3, RenderingRuleSearchRequest var4, boolean var5
   );

   protected static native long searchNativeObjectsForRendering(
      int var0, int var1, int var2, int var3, int var4, RenderingRuleSearchRequest var5, boolean var6, int var7, Object var8, String var9
   );

   protected static native boolean initFontType(String var0, String var1, boolean var2, boolean var3);

   protected static native NativeLibrary.RenderedObject[] searchRenderedObjects(RenderingContext var0, int var1, int var2, boolean var3);

   public NativeLibrary.RenderedObject[] searchRenderedObjectsFromContext(RenderingContext context, int x, int y) {
      return searchRenderedObjects(context, x, y, false);
   }

   public NativeLibrary.RenderedObject[] searchRenderedObjectsFromContext(RenderingContext context, int x, int y, boolean notvisible) {
      return searchRenderedObjects(context, x, y, notvisible);
   }

   public boolean needRequestPrivateAccessRouting(RoutingContext ctx, int[] x31Coordinates, int[] y31Coordinates) {
      return nativeNeedRequestPrivateAccessRouting(ctx, x31Coordinates, y31Coordinates);
   }

   protected static native boolean nativeNeedRequestPrivateAccessRouting(RoutingContext var0, int[] var1, int[] var2);

   public static boolean loadNewLib(String path) {
      return load("OsmAndJNI", path);
   }

   public static boolean loadOldLib(String path) {
      boolean b = true;
      return b & load("osmand", path);
   }

   public static boolean load(String libBaseName, String path) {
      if (path != null && path.length() > 0) {
         try {
            System.load(path + "/" + System.mapLibraryName(libBaseName));
            return true;
         } catch (UnsatisfiedLinkError var13) {
            log.error(var13);
         }
      }

      String osname = System.getProperty("os.name").toLowerCase();
      String osarch = System.getProperty("os.arch");
      if (osname.startsWith("mac os")) {
         osname = "mac";
         osarch = "universal";
      }

      if (osname.startsWith("windows")) {
         osname = "win";
      }

      if (osname.startsWith("sunos")) {
         osname = "solaris";
      }

      if (osarch.startsWith("i") && osarch.endsWith("86")) {
         osarch = "x86";
      }

      String libname = libBaseName + "-" + osname + '-' + osarch + ".lib";

      try {
         ClassLoader cl = NativeLibrary.class.getClassLoader();
         InputStream in = cl.getResourceAsStream(libname);
         if (in == null) {
            log.error("libname: " + libname + " not found");
            return false;
         }

         File tmplib = File.createTempFile(libBaseName + "-", ".lib");
         tmplib.deleteOnExit();
         OutputStream out = new FileOutputStream(tmplib);
         byte[] buf = new byte[1024];

         int len;
         while((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
         }

         in.close();
         out.close();
         System.load(tmplib.getAbsolutePath());
         return true;
      } catch (Exception var11) {
         var11.printStackTrace();
         log.error(var11.getMessage(), var11);
      } catch (UnsatisfiedLinkError var12) {
         log.error(var12.getMessage(), var12);
      }

      return false;
   }

   public static int ccmp(int lhs, int rhs) {
      return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
   }

   public void loadFontData(File dr) {
      File[] lf = dr.listFiles();
      if (lf == null) {
         System.err.println("No fonts loaded from " + dr.getAbsolutePath());
      } else {
         ArrayList<File> lst = new ArrayList<>(Arrays.asList(lf));
         Collections.sort(lst, new Comparator<File>() {
            public int compare(File arg0, File arg1) {
               return NativeLibrary.ccmp(this.order(arg0), this.order(arg1));
            }

            private int order(File a) {
               String nm = a.getName().toLowerCase();
               boolean hasNumber = Character.isDigit(nm.charAt(0)) && Character.isDigit(nm.charAt(1));
               if (hasNumber) {
                  return Integer.parseInt(nm.substring(0, 2));
               } else {
                  return nm.contains("NotoSans".toLowerCase()) ? 100 : 101;
               }
            }
         });

         for(File f : lst) {
            String name = f.getName();
            if ((name.endsWith(".ttf") || name.endsWith(".otf")) && !name.contains("Roboto".toLowerCase())) {
               initFontType(
                  f.getAbsolutePath(), name.substring(0, name.length() - 4), name.toLowerCase().contains("bold"), name.toLowerCase().contains("italic")
               );
            }
         }
      }
   }

   public static class NativeDirectionPoint {
      public int x31;
      public int y31;
      public String[][] tags;

      public NativeDirectionPoint(double lat, double lon, Map<String, String> tags) {
         this.x31 = MapUtils.get31TileNumberX(lon);
         this.y31 = MapUtils.get31TileNumberY(lat);
         this.tags = new String[tags.size()][2];
         int i = 0;

         for(Entry<String, String> e : tags.entrySet()) {
            this.tags[i][0] = e.getKey();
            this.tags[i][1] = e.getValue();
            ++i;
         }
      }
   }

   public static class NativeGpxPointApproximation {
      public int ind;
      public double lat;
      public double lon;
      public double cumDist;
      public List<RouteSegmentResult> routeToTarget;

      NativeGpxPointApproximation(RoutePlannerFrontEnd.GpxPoint gpxPoint) {
         this.lat = gpxPoint.loc.getLatitude();
         this.lon = gpxPoint.loc.getLongitude();
         this.cumDist = gpxPoint.cumDist;
      }

      public NativeGpxPointApproximation(int ind, double lat, double lon, double cumDist) {
         this.ind = ind;
         this.lat = lat;
         this.lon = lon;
         this.cumDist = cumDist;
         this.routeToTarget = new ArrayList<>();
      }

      public void addRouteToTarget(RouteSegmentResult routeSegmentResult) {
         this.routeToTarget.add(routeSegmentResult);
      }

      public RoutePlannerFrontEnd.GpxPoint convertToGpxPoint() {
         RoutePlannerFrontEnd.GpxPoint point = new RoutePlannerFrontEnd.GpxPoint();
         point.ind = this.ind;
         point.loc = new LatLon(this.lat, this.lon);
         point.cumDist = this.cumDist;
         if (this.routeToTarget.size() > 0 && this.routeToTarget.get(0).getObject().region == null) {
            this.fixStraightLineRegion();
         }

         point.routeToTarget = new ArrayList<>(this.routeToTarget);
         return point;
      }

      private void fixStraightLineRegion() {
         BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
         reg.initRouteEncodingRule(0, "highway", "unmatched");

         for(int i = 0; i < this.routeToTarget.size(); ++i) {
            RouteDataObject newRdo = new RouteDataObject(reg);
            RouteDataObject rdo = this.routeToTarget.get(i).getObject();
            newRdo.pointsX = rdo.pointsX;
            newRdo.pointsY = rdo.pointsY;
            newRdo.types = rdo.getTypes();
            newRdo.id = -1L;
            this.routeToTarget.get(i).setObject(newRdo);
         }
      }
   }

   public static class NativeGpxRouteApproximationResult {
      public List<NativeLibrary.NativeGpxPointApproximation> finalPoints = new ArrayList<>();
      public List<RouteSegmentResult> result = new ArrayList<>();

      public void addFinalPoint(NativeLibrary.NativeGpxPointApproximation finalPoint) {
         this.finalPoints.add(finalPoint);
      }

      public void addResultSegment(RouteSegmentResult routeSegmentResult) {
         this.result.add(routeSegmentResult);
      }
   }

   public static class NativeRouteSearchResult {
      public long nativeHandler;
      public RouteDataObject[] objects;
      public BinaryMapRouteReaderAdapter.RouteSubregion region;

      public NativeRouteSearchResult(long nativeHandler, RouteDataObject[] objects) {
         this.nativeHandler = nativeHandler;
         this.objects = objects;
      }

      @Override
      protected void finalize() throws Throwable {
         this.deleteNativeResult();
         super.finalize();
      }

      public void deleteNativeResult() {
         if (this.nativeHandler != 0L) {
            NativeLibrary.deleteRouteSearchResult(this.nativeHandler);
            this.nativeHandler = 0L;
         }
      }
   }

   public static class NativeSearchResult {
      public long nativeHandler;

      private NativeSearchResult(long nativeHandler) {
         this.nativeHandler = nativeHandler;
      }

      @Override
      protected void finalize() throws Throwable {
         this.deleteNativeResult();
         super.finalize();
      }

      public void deleteNativeResult() {
         if (this.nativeHandler != 0L) {
            NativeLibrary.deleteSearchResult(this.nativeHandler);
            this.nativeHandler = 0L;
         }
      }
   }

   public static class RenderedObject extends MapObject {
      private Map<String, String> tags = new LinkedHashMap<>();
      private QuadRect bbox = new QuadRect();
      private TIntArrayList x = new TIntArrayList();
      private TIntArrayList y = new TIntArrayList();
      private String iconRes;
      private int order;
      private boolean visible;
      private boolean drawOnPath;
      private LatLon labelLatLon;
      private int labelX = 0;
      private int labelY = 0;

      public Map<String, String> getTags() {
         return this.tags;
      }

      public String getTagValue(String tag) {
         return this.getTags().get(tag);
      }

      public boolean isText() {
         return !this.getName().isEmpty();
      }

      public int getOrder() {
         return this.order;
      }

      public void setLabelLatLon(LatLon labelLatLon) {
         this.labelLatLon = labelLatLon;
      }

      public LatLon getLabelLatLon() {
         return this.labelLatLon;
      }

      public void setOrder(int order) {
         this.order = order;
      }

      public void addLocation(int x, int y) {
         this.x.add(x);
         this.y.add(y);
      }

      public TIntArrayList getX() {
         return this.x;
      }

      public String getIconRes() {
         return this.iconRes;
      }

      public void setIconRes(String iconRes) {
         this.iconRes = iconRes;
      }

      public void setVisible(boolean visible) {
         this.visible = visible;
      }

      public boolean isVisible() {
         return this.visible;
      }

      public void setDrawOnPath(boolean drawOnPath) {
         this.drawOnPath = drawOnPath;
      }

      public boolean isDrawOnPath() {
         return this.drawOnPath;
      }

      public TIntArrayList getY() {
         return this.y;
      }

      public void setBbox(int left, int top, int right, int bottom) {
         this.bbox = new QuadRect((double)left, (double)top, (double)right, (double)bottom);
      }

      public QuadRect getBbox() {
         return this.bbox;
      }

      public void setNativeId(long id) {
         this.setId(Long.valueOf(id));
      }

      public void putTag(String t, String v) {
         this.tags.put(t, v);
      }

      public int getLabelX() {
         return this.labelX;
      }

      public int getLabelY() {
         return this.labelY;
      }

      public void setLabelX(int labelX) {
         this.labelX = labelX;
      }

      public void setLabelY(int labelY) {
         this.labelY = labelY;
      }

      public List<String> getOriginalNames() {
         List<String> names = new ArrayList<>();
         if (!Algorithms.isEmpty(this.name)) {
            names.add(this.name);
         }

         for(Entry<String, String> entry : this.tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ((key.startsWith("name:") || key.equals("name")) && !value.isEmpty()) {
               names.add(value);
            }
         }

         return names;
      }

      public String getRouteID() {
         for(Entry<String, String> entry : this.getTags().entrySet()) {
            if ("route_id".equals(entry.getKey())) {
               return entry.getValue();
            }
         }

         return null;
      }

      public String getGpxFileName() {
         for(String name : this.getOriginalNames()) {
            if (name.endsWith(".gpx") || name.endsWith(".gpx.gz")) {
               return name;
            }
         }

         return null;
      }
   }

   public static class RenderingGenerationResult {
      public final ByteBuffer bitmapBuffer;

      public RenderingGenerationResult(ByteBuffer bitmap) {
         this.bitmapBuffer = bitmap;
      }
   }
}
