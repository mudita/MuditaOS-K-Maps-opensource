package net.osmand.map;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TileSourceManager {
   private static final Log log = PlatformUtil.getLog(TileSourceManager.class);
   public static final String MAPILLARY_VECTOR_TILE_EXT = ".pbf";
   private static final TileSourceManager.TileSourceTemplate MAPNIK_SOURCE = new TileSourceManager.TileSourceTemplate(
      "OsmAnd (online tiles)", "https://tile.osmand.net/hd/{0}/{1}/{2}.png", ".png", 19, 1, 512, 8, 18000
   );
   private static final TileSourceManager.TileSourceTemplate CYCLE_MAP_SOURCE = new TileSourceManager.TileSourceTemplate(
      "CycleMap", "https://b.tile.thunderforest.com/cycle/{0}/{1}/{2}.png?apikey=a778ae1a212641d38f46dc11f20ac116", ".png", 16, 1, 256, 32, 18000
   );
   private static final TileSourceManager.TileSourceTemplate MAPILLARY_VECTOR_SOURCE = new TileSourceManager.TileSourceTemplate(
      "Mapillary (vector tiles)",
      "https://tiles.mapillary.com/maps/vtp/mly1_public/2/{0}/{1}/{2}/?access_token=MLY|4444816185556934|29475a355616c979409a5adc377a00fa",
      ".pbf",
      21,
      13,
      256,
      16,
      3200
   );
   private static final TileSourceManager.TileSourceTemplate MAPILLARY_CACHE_SOURCE = new TileSourceManager.TileSourceTemplate(
      "Mapillary (raster tiles)", "", ".png", 21, 13, 256, 32, 18000
   );
   private static final String PARAM_BING_QUAD_KEY = "{q}";
   private static final String PARAM_RND = "{rnd}";
   private static final String PARAM_BOUNDING_BOX = "{bbox}";
   public static final String PARAMETER_NAME = "{PARAM}";

   private static int parseInt(Map<String, String> attributes, String value, int def) {
      String val = attributes.get(value);
      if (val == null) {
         return def;
      } else {
         try {
            return Integer.parseInt(val);
         } catch (NumberFormatException var5) {
            return def;
         }
      }
   }

   public static void createMetaInfoFile(File dir, TileSourceManager.TileSourceTemplate tm, boolean override) throws IOException {
      File metainfo = new File(dir, ".metainfo");
      Map<String, String> properties = new LinkedHashMap<>();
      if (!Algorithms.isEmpty(tm.getRule())) {
         properties.put("rule", tm.getRule());
      }

      if (tm.getUrlTemplate() != null) {
         properties.put("url_template", tm.getUrlTemplate());
      }

      if (!Algorithms.isEmpty(tm.getReferer())) {
         properties.put("referer", tm.getReferer());
      }

      if (!Algorithms.isEmpty(tm.getUserAgent())) {
         properties.put("user_agent", tm.getUserAgent());
      }

      properties.put("ext", tm.getTileFormat());
      properties.put("min_zoom", tm.getMinimumZoomSupported() + "");
      properties.put("max_zoom", tm.getMaximumZoomSupported() + "");
      properties.put("tile_size", tm.getTileSize() + "");
      properties.put("img_density", tm.getBitDensity() + "");
      properties.put("avg_img_size", tm.getAverageSize() + "");
      if (tm.isEllipticYTile()) {
         properties.put("ellipsoid", tm.isEllipticYTile() + "");
      }

      if (tm.isInvertedYTile()) {
         properties.put("inverted_y", tm.isInvertedYTile() + "");
      }

      if (tm.getRandoms() != null) {
         properties.put("randoms", tm.getRandoms());
      }

      if (tm.getExpirationTimeMinutes() != -1) {
         properties.put("expiration_time_minutes", tm.getExpirationTimeMinutes() + "");
      }

      if (tm.paramType != ParameterType.UNDEFINED) {
         properties.put("param_type", tm.paramType.getParamName());
         properties.put("param_min", tm.paramMin + "");
         properties.put("param_step", tm.paramStep + "");
         properties.put("param_max", tm.paramMax + "");
      }

      if (override || !metainfo.exists()) {
         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(metainfo)));

         for(Entry<String, String> entry : properties.entrySet()) {
            writer.write("[" + (String)entry.getKey() + "]\n" + (String)entry.getValue() + "\n");
         }

         writer.close();
      }
   }

   public static boolean isTileSourceMetaInfoExist(File dir) {
      return new File(dir, ".metainfo").exists() || new File(dir, "url").exists();
   }

   public static TileSourceManager.TileSourceTemplate createTileSourceTemplate(File dir) {
      Map<String, String> metaInfo = readMetaInfoFile(dir);
      boolean ruleAcceptable = true;
      if (!metaInfo.isEmpty()) {
         metaInfo.put("name", dir.getName());
         TileSourceManager.TileSourceTemplate template = createTileSourceTemplate(metaInfo);
         if (template != null) {
            return template;
         }

         ruleAcceptable = false;
      }

      String ext = findOneTile(dir);
      ext = ext == null ? ".jpg" : ext;
      String url = null;
      File readUrl = new File(dir, "url");

      try {
         if (readUrl.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(readUrl), "UTF-8"));
            url = reader.readLine();
            url = TileSourceManager.TileSourceTemplate.normalizeUrl(url);
            reader.close();
         }
      } catch (IOException var7) {
         log.debug("Error reading url " + dir.getName(), var7);
      }

      TileSourceManager.TileSourceTemplate template = new TileSourceManager.TileSourceTemplate(dir.getName(), url, ext, 18, 1, 256, 16, 20000);
      template.setRuleAcceptable(ruleAcceptable);
      return template;
   }

   private static Map<String, String> readMetaInfoFile(File dir) {
      Map<String, String> keyValueMap = new LinkedHashMap<>();

      try {
         File metainfo = new File(dir, ".metainfo");
         if (metainfo.exists()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(metainfo), StandardCharsets.UTF_8));
            String key = null;

            String line;
            while((line = reader.readLine()) != null) {
               line = line.trim();
               if (line.startsWith("[")) {
                  key = line.substring(1, line.length() - 1).toLowerCase();
               } else if (key != null && line.length() > 0) {
                  keyValueMap.put(key, line);
                  key = null;
               }
            }

            reader.close();
         }
      } catch (IOException var6) {
         log.error("Error reading metainfo file " + dir.getAbsolutePath(), var6);
      }

      return keyValueMap;
   }

   private static String findOneTile(File dir) {
      if (dir.isDirectory()) {
         File[] files = dir.listFiles();
         if (files == null) {
            return null;
         }

         for(File file : files) {
            if (file.isDirectory()) {
               String ext = findOneTile(file);
               if (ext != null) {
                  return ext;
               }
            } else {
               String fileName = file.getName();
               if (fileName.endsWith(".tile")) {
                  String substring = fileName.substring(0, fileName.length() - ".tile".length());
                  int extInt = substring.lastIndexOf(46);
                  if (extInt != -1) {
                     return substring.substring(extInt, substring.length());
                  }
               }
            }
         }
      }

      return null;
   }

   public static List<TileSourceManager.TileSourceTemplate> getKnownSourceTemplates() {
      List<TileSourceManager.TileSourceTemplate> list = new ArrayList<>();
      list.add(getMapnikSource());
      list.add(getMapillaryVectorSource());
      list.add(getMapillaryCacheSource());
      return list;
   }

   public static TileSourceManager.TileSourceTemplate getMapnikSource() {
      return MAPNIK_SOURCE;
   }

   public static TileSourceManager.TileSourceTemplate getMapillaryVectorSource() {
      return MAPILLARY_VECTOR_SOURCE;
   }

   public static TileSourceManager.TileSourceTemplate getMapillaryCacheSource() {
      return MAPILLARY_CACHE_SOURCE;
   }

   public static List<TileSourceManager.TileSourceTemplate> downloadTileSourceTemplates(String versionAsUrl, boolean https) {
      List<TileSourceManager.TileSourceTemplate> templates = new ArrayList<>();

      try {
         URLConnection connection = NetworkUtils.getHttpURLConnection((https ? "https" : "http") + "://download.osmand.net/tile_sources?" + versionAsUrl);
         XmlPullParser parser = PlatformUtil.newXMLPullParser();
         parser.setInput(connection.getInputStream(), "UTF-8");

         int tok;
         while((tok = parser.next()) != 1) {
            if (tok == 2) {
               String name = parser.getName();
               if (name.equals("tile_source")) {
                  Map<String, String> attrs = new LinkedHashMap<>();

                  for(int i = 0; i < parser.getAttributeCount(); ++i) {
                     attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
                  }

                  TileSourceManager.TileSourceTemplate template = createTileSourceTemplate(attrs);
                  if (template != null) {
                     templates.add(template);
                  }
               }
            }
         }

         return templates;
      } catch (XmlPullParserException | IOException var9) {
         log.error("Exception while downloading tile sources", var9);
         return null;
      }
   }

   public static TileSourceManager.TileSourceTemplate createTileSourceTemplate(Map<String, String> attrs) {
      String rule = attrs.get("rule");
      TileSourceManager.TileSourceTemplate template;
      if (rule == null) {
         template = createSimpleTileSourceTemplate(attrs, false);
      } else if ("template:1".equalsIgnoreCase(rule)) {
         template = createSimpleTileSourceTemplate(attrs, false);
      } else if ("wms_tile".equalsIgnoreCase(rule)) {
         template = createWmsTileSourceTemplate(attrs);
      } else {
         if (!"yandex_traffic".equalsIgnoreCase(rule)) {
            return null;
         }

         template = createSimpleTileSourceTemplate(attrs, true);
      }

      if (template != null) {
         template.setRule(rule);
      }

      return template;
   }

   private static TileSourceManager.TileSourceTemplate createWmsTileSourceTemplate(Map<String, String> attributes) {
      String name = attributes.get("name");
      String layer = attributes.get("layer");
      String urlTemplate = attributes.get("url_template");
      if (name != null && urlTemplate != null && layer != null) {
         int maxZoom = parseInt(attributes, "max_zoom", 18);
         int minZoom = parseInt(attributes, "min_zoom", 5);
         int tileSize = parseInt(attributes, "tile_size", 256);
         String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
         int bitDensity = parseInt(attributes, "img_density", 16);
         int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
         String randoms = attributes.get("randoms");
         urlTemplate = "http://whoots.mapwarper.net/tms/{0}/{1}/{2}/" + layer + "/" + urlTemplate;
         TileSourceManager.TileSourceTemplate templ = new TileSourceManager.TileSourceTemplate(
            name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize
         );
         templ.setRandoms(randoms);
         return templ;
      } else {
         return null;
      }
   }

   private static TileSourceManager.TileSourceTemplate createSimpleTileSourceTemplate(Map<String, String> attributes, boolean ignoreTemplate) {
      String name = attributes.get("name");
      String urlTemplate = attributes.get("url_template");
      if (name != null && (urlTemplate != null || ignoreTemplate)) {
         urlTemplate = TileSourceManager.TileSourceTemplate.normalizeUrl(urlTemplate);
         int maxZoom = parseInt(attributes, "max_zoom", 18);
         int minZoom = parseInt(attributes, "min_zoom", 5);
         int tileSize = parseInt(attributes, "tile_size", 256);
         int expirationTime = parseInt(attributes, "expiration_time_minutes", -1);
         String ext = attributes.get("ext") == null ? ".jpg" : attributes.get("ext");
         int bitDensity = parseInt(attributes, "img_density", 16);
         int avgTileSize = parseInt(attributes, "avg_img_size", 18000);
         boolean ellipsoid = false;
         if (Boolean.parseBoolean(attributes.get("ellipsoid"))) {
            ellipsoid = true;
         }

         boolean invertedY = false;
         if (Boolean.parseBoolean(attributes.get("inverted_y"))) {
            invertedY = true;
         }

         String randoms = attributes.get("randoms");
         TileSourceManager.TileSourceTemplate templ = new TileSourceManager.TileSourceTemplate(
            name, urlTemplate, ext, maxZoom, minZoom, tileSize, bitDensity, avgTileSize
         );
         if (attributes.get("referer") != null) {
            templ.setReferer(attributes.get("referer"));
         }

         if (attributes.get("user_agent") != null) {
            templ.setUserAgent(attributes.get("user_agent"));
         }

         if (expirationTime >= 0) {
            templ.setExpirationTimeMinutes(expirationTime);
         }

         templ.setEllipticYTile(ellipsoid);
         templ.setInvertedYTile(invertedY);
         templ.setRandoms(randoms);
         if (attributes.get("param_type") != null
            && attributes.get("param_min") != null
            && attributes.get("param_step") != null
            && attributes.get("param_max") != null) {
            templ.setParamType(ParameterType.fromName(attributes.get("param_type")));

            try {
               templ.setParamMin(Long.parseLong(attributes.get("param_min")));
            } catch (NumberFormatException var18) {
            }

            try {
               templ.setParamStep(Long.parseLong(attributes.get("param_step")));
            } catch (NumberFormatException var17) {
            }

            try {
               templ.setParamMax(Long.parseLong(attributes.get("param_max")));
            } catch (NumberFormatException var16) {
            }
         }

         return templ;
      } else {
         return null;
      }
   }

   static {
      MAPILLARY_VECTOR_SOURCE.setExpirationTimeMinutes(1440);
      MAPILLARY_VECTOR_SOURCE.setHidden(true);
   }

   public static class TileSourceTemplate implements ITileSource, Cloneable {
      private int maxZoom;
      private int minZoom;
      private String name;
      protected int tileSize;
      protected String urlToLoad;
      private final Map<String, String> urlParameters = new ConcurrentHashMap<>();
      protected String ext;
      private int avgSize;
      private int bitDensity;
      private long expirationTimeMillis = -1L;
      private boolean ellipticYTile;
      private boolean invertedYTile;
      private String randoms;
      private String[] randomsArray;
      private String rule;
      private String referer;
      private String userAgent;
      private boolean hidden;
      private ParameterType paramType = ParameterType.UNDEFINED;
      private long paramMin;
      private long paramStep;
      private long paramMax;
      private boolean isRuleAcceptable = true;

      public TileSourceTemplate(String name, String urlToLoad, String ext, int maxZoom, int minZoom, int tileSize, int bitDensity, int avgSize) {
         this.maxZoom = maxZoom;
         this.minZoom = minZoom;
         this.name = name;
         this.tileSize = tileSize;
         this.urlToLoad = urlToLoad;
         this.ext = ext;
         this.avgSize = avgSize;
         this.bitDensity = bitDensity;
      }

      public TileSourceTemplate(
         String name,
         String urlToLoad,
         String ext,
         int maxZoom,
         int minZoom,
         int tileSize,
         int bitDensity,
         int avgSize,
         ParameterType paramType,
         long paramMin,
         long paramStep,
         long paramMax
      ) {
         this(name, urlToLoad, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
         this.paramType = paramType;
         this.paramMin = paramMin;
         this.paramStep = paramStep;
         this.paramMax = paramMax;
      }

      public static String normalizeUrl(String url) {
         if (url != null) {
            url = url.replaceAll("\\{\\$z\\}", "{0}");
            url = url.replaceAll("\\{\\$x\\}", "{1}");
            url = url.replaceAll("\\{\\$y\\}", "{2}");
            url = url.replaceAll("\\{z\\}", "{0}");
            url = url.replaceAll("\\{x\\}", "{1}");
            url = url.replaceAll("\\{y\\}", "{2}");
         }

         return url;
      }

      public static String[] buildRandomsArray(String randomsStr) {
         List<String> randoms = new ArrayList<>();
         if (!Algorithms.isEmpty(randomsStr)) {
            if (randomsStr.equals("wikimapia")) {
               return new String[]{"wikimapia"};
            }

            String[] valuesArray = randomsStr.split(",");

            for(String s : valuesArray) {
               String[] rangeArray = s.split("-");
               if (rangeArray.length == 2) {
                  String s1 = rangeArray[0];
                  String s2 = rangeArray[1];
                  boolean rangeValid = false;

                  try {
                     int a = Integer.parseInt(s1);
                     int b = Integer.parseInt(s2);
                     if (b > a) {
                        for(int i = a; i <= b; ++i) {
                           randoms.add(String.valueOf(i));
                        }

                        rangeValid = true;
                     }
                  } catch (NumberFormatException var15) {
                     if (s1.length() == 1 && s2.length() == 1) {
                        char a = s1.charAt(0);
                        char b = s2.charAt(0);
                        if (b > a) {
                           for(char i = a; i <= b; ++i) {
                              randoms.add(String.valueOf(i));
                           }

                           rangeValid = true;
                        }
                     }
                  }

                  if (!rangeValid) {
                     randoms.add(s1);
                     randoms.add(s2);
                  }
               } else {
                  randoms.add(s);
               }
            }
         }

         return randoms.toArray(new String[0]);
      }

      public void setMinZoom(int minZoom) {
         this.minZoom = minZoom;
      }

      public void setMaxZoom(int maxZoom) {
         this.maxZoom = maxZoom;
      }

      public boolean isHidden() {
         return this.hidden;
      }

      public void setHidden(boolean hidden) {
         this.hidden = hidden;
      }

      public void setName(String name) {
         this.name = name;
      }

      public void setEllipticYTile(boolean ellipticYTile) {
         this.ellipticYTile = ellipticYTile;
      }

      @Override
      public boolean isEllipticYTile() {
         return this.ellipticYTile;
      }

      @Override
      public boolean isInvertedYTile() {
         return this.invertedYTile;
      }

      @Override
      public boolean isTimeSupported() {
         return this.expirationTimeMillis != -1L;
      }

      @Override
      public boolean getInversiveZoom() {
         return false;
      }

      public void setInvertedYTile(boolean invertedYTile) {
         this.invertedYTile = invertedYTile;
      }

      @Override
      public String getRandoms() {
         return this.randoms;
      }

      public void setRandoms(String randoms) {
         this.randoms = randoms;
         this.randomsArray = buildRandomsArray(randoms);
      }

      public String[] getRandomsArray() {
         return this.randomsArray;
      }

      public void setRandomsArray(String[] randomsArray) {
         this.randomsArray = randomsArray;
      }

      @Override
      public int getBitDensity() {
         return this.bitDensity;
      }

      public int getAverageSize() {
         return this.avgSize;
      }

      @Override
      public int getMaximumZoomSupported() {
         return this.maxZoom;
      }

      @Override
      public int getMinimumZoomSupported() {
         return this.minZoom;
      }

      @Override
      public String getName() {
         return this.name;
      }

      public void setExpirationTimeMillis(long timeMillis) {
         this.expirationTimeMillis = timeMillis;
      }

      public void setExpirationTimeMinutes(int minutes) {
         if (minutes < 0) {
            this.expirationTimeMillis = -1L;
         } else {
            this.expirationTimeMillis = (long)(minutes * 60) * 1000L;
         }
      }

      @Override
      public int getExpirationTimeMinutes() {
         return this.expirationTimeMillis < 0L ? -1 : (int)(this.expirationTimeMillis / 60000L);
      }

      @Override
      public long getExpirationTimeMillis() {
         return this.expirationTimeMillis;
      }

      @Override
      public String getReferer() {
         return this.referer;
      }

      public void setReferer(String referer) {
         this.referer = referer;
      }

      @Override
      public String getUserAgent() {
         return this.userAgent;
      }

      public void setUserAgent(String userAgent) {
         this.userAgent = userAgent;
      }

      @Override
      public int getTileSize() {
         return this.tileSize;
      }

      @Override
      public String getTileFormat() {
         return this.ext;
      }

      public void setTileFormat(String ext) {
         this.ext = ext;
      }

      public void setUrlToLoad(String urlToLoad) {
         this.urlToLoad = urlToLoad;
      }

      public boolean isRuleAcceptable() {
         return this.isRuleAcceptable;
      }

      public void setRuleAcceptable(boolean isRuleAcceptable) {
         this.isRuleAcceptable = isRuleAcceptable;
      }

      @Override
      public ParameterType getParamType() {
         return this.paramType;
      }

      @Override
      public long getParamMin() {
         return this.paramMin;
      }

      @Override
      public long getParamStep() {
         return this.paramStep;
      }

      @Override
      public long getParamMax() {
         return this.paramMax;
      }

      public void setParamType(ParameterType paramType) {
         this.paramType = paramType;
      }

      public void setParamMin(long paramMin) {
         this.paramMin = paramMin;
      }

      public void setParamStep(long paramStep) {
         this.paramStep = paramStep;
      }

      public void setParamMax(long paramMax) {
         this.paramMax = paramMax;
      }

      @Override
      public Map<String, String> getUrlParameters() {
         return Collections.unmodifiableMap(this.urlParameters);
      }

      @Override
      public String getUrlParameter(String name) {
         return this.urlParameters.get(name);
      }

      @Override
      public void setUrlParameter(String name, String value) {
         this.urlParameters.put(name, value);
      }

      @Override
      public void resetUrlParameter(String name) {
         this.urlParameters.remove(name);
      }

      @Override
      public void resetUrlParameters() {
         this.urlParameters.clear();
      }

      public TileSourceManager.TileSourceTemplate copy() {
         try {
            return (TileSourceManager.TileSourceTemplate)this.clone();
         } catch (CloneNotSupportedException var2) {
            return this;
         }
      }

      @Override
      public String getUrlToLoad(int x, int y, int zoom) {
         if (this.urlToLoad == null) {
            return null;
         } else {
            if (this.isInvertedYTile()) {
               y = (1 << zoom) - 1 - y;
            }

            return buildUrlToLoad(this.urlToLoad, this.randomsArray, x, y, zoom, this.urlParameters);
         }
      }

      private static String eqtBingQuadKey(int z, int x, int y) {
         char[] NUM_CHAR = new char[]{'0', '1', '2', '3'};
         char[] tn = new char[z];

         for(int i = z - 1; i >= 0; --i) {
            int num = x % 2 | y % 2 << 1;
            tn[i] = NUM_CHAR[num];
            x >>= 1;
            y >>= 1;
         }

         return new String(tn);
      }

      private static String calcBoundingBoxForTile(int zoom, int x, int y) {
         double xmin = MapUtils.getLongitudeFromTile((double)zoom, (double)x);
         double xmax = MapUtils.getLongitudeFromTile((double)zoom, (double)(x + 1));
         double ymin = MapUtils.getLatitudeFromTile((float)zoom, (double)(y + 1));
         double ymax = MapUtils.getLatitudeFromTile((float)zoom, (double)y);
         return String.format("%.8f,%.8f,%.8f,%.8f", xmin, ymin, xmax, ymax);
      }

      public static String buildUrlToLoad(String urlTemplate, String[] randomsArray, int x, int y, int zoom, Map<String, String> params) {
         try {
            if (randomsArray != null && randomsArray.length > 0) {
               String rand;
               if ("wikimapia".equals(randomsArray[0])) {
                  rand = String.valueOf(x % 4 + y % 4 * 4);
               } else {
                  rand = randomsArray[(x + y) % randomsArray.length];
               }

               urlTemplate = urlTemplate.replace("{rnd}", rand);
            } else if (urlTemplate.contains("{rnd}")) {
               TileSourceManager.log.error("Cannot resolve randoms for template: " + urlTemplate);
               return null;
            }

            int bingQuadKeyParamIndex = urlTemplate.indexOf("{q}");
            if (bingQuadKeyParamIndex != -1) {
               return urlTemplate.replace("{q}", eqtBingQuadKey(zoom, x, y));
            } else {
               int bbKeyParamIndex = urlTemplate.indexOf("{bbox}");
               if (bbKeyParamIndex != -1) {
                  return urlTemplate.replace("{bbox}", calcBoundingBoxForTile(zoom, x, y));
               } else {
                  if (!Algorithms.isEmpty(params)) {
                     for(Entry<String, String> pv : params.entrySet()) {
                        String name = pv.getKey();
                        int paramIndex = urlTemplate.indexOf(name);
                        if (paramIndex != -1) {
                           urlTemplate = urlTemplate.replace(name, pv.getValue());
                        }
                     }
                  }

                  return MessageFormat.format(urlTemplate, zoom + "", x + "", y + "");
               }
            }
         } catch (IllegalArgumentException var12) {
            TileSourceManager.log.error("Cannot build url for template: " + urlTemplate, var12);
            return null;
         }
      }

      @Override
      public String getUrlTemplate() {
         return this.urlToLoad;
      }

      @Override
      public boolean couldBeDownloadedFromInternet() {
         return this.urlToLoad != null;
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         return 31 * result + (this.name == null ? 0 : this.name.hashCode());
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
            TileSourceManager.TileSourceTemplate other = (TileSourceManager.TileSourceTemplate)obj;
            if (this.name == null) {
               if (other.name != null) {
                  return false;
               }
            } else if (!this.name.equals(other.name)) {
               return false;
            }

            return true;
         }
      }

      public void setRule(String rule) {
         this.rule = rule;
      }

      @Override
      public String getRule() {
         return this.rule;
      }

      public String calculateTileId(int x, int y, int zoom) {
         StringBuilder builder = new StringBuilder(this.getName());
         builder.append('/');
         builder.append(zoom).append('/').append(x).append('/').append(y).append(this.getTileFormat()).append(".tile");
         return builder.toString();
      }

      @Override
      public long getTileModifyTime(int x, int y, int zoom, String dirWithTiles) {
         File en = new File(dirWithTiles, this.calculateTileId(x, y, zoom));
         return en.exists() ? en.lastModified() : System.currentTimeMillis();
      }

      @Override
      public byte[] getBytes(int x, int y, int zoom, String dirWithTiles) throws IOException {
         File f = new File(dirWithTiles, this.calculateTileId(x, y, zoom));
         if (!f.exists()) {
            return null;
         } else {
            ByteArrayOutputStream bous = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(f);
            Algorithms.streamCopy(fis, bous);
            fis.close();
            bous.close();
            return bous.toByteArray();
         }
      }

      @Override
      public void deleteTiles(String path) {
         File pf = new File(path);
         File[] list = pf.listFiles();
         if (list != null) {
            for(File l : list) {
               if (l.isDirectory()) {
                  Algorithms.removeAllFiles(l);
               }
            }
         }
      }

      @Override
      public int getAvgSize() {
         return this.avgSize;
      }
   }
}
