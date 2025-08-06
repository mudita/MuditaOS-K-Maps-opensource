package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.edit.Node;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RoutingConfiguration {
   public static final int DEFAULT_MEMORY_LIMIT = 30;
   public static final int DEFAULT_NATIVE_MEMORY_LIMIT = 256;
   public static final float DEVIATION_RADIUS = 3000.0F;
   public Map<String, String> attributes = new LinkedHashMap<>();
   public float heuristicCoefficient = 1.0F;
   public int ZOOM_TO_LOAD_TILES = 16;
   public long memoryLimitation;
   public long nativeMemoryLimitation;
   public int planRoadDirection = 0;
   public GeneralRouter router = new GeneralRouter(GeneralRouter.GeneralRouterProfile.CAR, new LinkedHashMap());
   public String routerName = "";
   public Double initialDirection;
   public float recalculateDistance = 20000.0F;
   public long routeCalculationTime = 0L;
   private QuadTree<RoutingConfiguration.DirectionPoint> directionPoints;
   public int directionPointsRadius = 30;
   public float minPointApproximation = 50.0F;
   public float minStepApproximation = 100.0F;
   public float maxStepApproximation = 3000.0F;
   public float smoothenPointsNoRoute = 5.0F;
   private static RoutingConfiguration.Builder DEFAULT;

   public QuadTree<RoutingConfiguration.DirectionPoint> getDirectionPoints() {
      return this.directionPoints;
   }

   public NativeLibrary.NativeDirectionPoint[] getNativeDirectionPoints() {
      if (this.directionPoints == null) {
         return new NativeLibrary.NativeDirectionPoint[0];
      } else {
         QuadRect rect = new QuadRect(0.0, 0.0, 2.147483647E9, 2.147483647E9);
         List<RoutingConfiguration.DirectionPoint> points = this.directionPoints.queryInBox(rect, new ArrayList());
         NativeLibrary.NativeDirectionPoint[] result = new NativeLibrary.NativeDirectionPoint[points.size()];

         for(int i = 0; i < points.size(); ++i) {
            RoutingConfiguration.DirectionPoint point = points.get(i);
            result[i] = new NativeLibrary.NativeDirectionPoint(point.getLatitude(), point.getLongitude(), point.getTags());
         }

         return result;
      }
   }

   public static int parseSilentInt(String t, int v) {
      return t != null && t.length() != 0 ? Integer.parseInt(t) : v;
   }

   public static float parseSilentFloat(String t, float v) {
      return t != null && t.length() != 0 ? Float.parseFloat(t) : v;
   }

   public static RoutingConfiguration.Builder getDefault() {
      if (DEFAULT == null) {
         try {
            DEFAULT = parseFromInputStream(RoutingConfiguration.class.getResourceAsStream("routing.xml"));
         } catch (Exception var1) {
            throw new IllegalStateException(var1);
         }
      }

      return DEFAULT;
   }

   public static RoutingConfiguration.Builder parseFromInputStream(InputStream is) throws IOException, XmlPullParserException {
      return parseFromInputStream(is, null, new RoutingConfiguration.Builder());
   }

   public static RoutingConfiguration.Builder parseFromInputStream(InputStream is, String filename, RoutingConfiguration.Builder config) throws IOException, XmlPullParserException {
      XmlPullParser parser = PlatformUtil.newXMLPullParser();
      GeneralRouter currentRouter = null;
      GeneralRouter.RouteDataObjectAttribute currentAttribute = null;
      String preType = null;
      Stack<RoutingConfiguration.RoutingRule> rulesStck = new Stack<>();
      parser.setInput(is, "UTF-8");

      int tok;
      while((tok = parser.next()) != 1) {
         if (tok == 2) {
            String name = parser.getName();
            if ("osmand_routing_config".equals(name)) {
               config.defaultRouter = parser.getAttributeValue("", "defaultProfile");
            } else if ("routingProfile".equals(name)) {
               currentRouter = parseRoutingProfile(parser, config, filename);
            } else if ("attribute".equals(name)) {
               parseAttribute(parser, config, currentRouter);
            } else if ("parameter".equals(name)) {
               parseRoutingParameter(parser, currentRouter);
            } else if (!"point".equals(name) && !"way".equals(name)) {
               parseRoutingRule(parser, currentRouter, currentAttribute, preType, rulesStck);
            } else {
               String attribute = parser.getAttributeValue("", "attribute");
               currentAttribute = GeneralRouter.RouteDataObjectAttribute.getValueOf(attribute);
               preType = parser.getAttributeValue("", "type");
            }
         } else if (tok == 3) {
            String pname = parser.getName();
            if (checkTag(pname)) {
               rulesStck.pop();
            }
         }
      }

      is.close();
      return config;
   }

   private static void parseRoutingParameter(XmlPullParser parser, GeneralRouter currentRouter) {
      String description = parser.getAttributeValue("", "description");
      String group = parser.getAttributeValue("", "group");
      String name = parser.getAttributeValue("", "name");
      String id = parser.getAttributeValue("", "id");
      String type = parser.getAttributeValue("", "type");
      String profilesList = parser.getAttributeValue("", "profiles");
      String[] profiles = Algorithms.isEmpty(profilesList) ? null : profilesList.split(",");
      boolean defaultValue = Boolean.parseBoolean(parser.getAttributeValue("", "default"));
      if ("boolean".equalsIgnoreCase(type)) {
         currentRouter.registerBooleanParameter(id, Algorithms.isEmpty(group) ? null : group, name, description, profiles, defaultValue);
      } else {
         if (!"numeric".equalsIgnoreCase(type)) {
            throw new UnsupportedOperationException("Unsupported routing parameter type - " + type);
         }

         String values = parser.getAttributeValue("", "values");
         String valueDescriptions = parser.getAttributeValue("", "valueDescriptions");
         String[] vlsDesc = valueDescriptions.split(",");
         String[] strValues = values.split(",");
         Double[] vls = new Double[strValues.length];

         for(int i = 0; i < vls.length; ++i) {
            vls[i] = Double.parseDouble(strValues[i].trim());
         }

         currentRouter.registerNumericParameter(id, name, description, profiles, vls, vlsDesc);
      }
   }

   private static void parseRoutingRule(
      XmlPullParser parser,
      GeneralRouter currentRouter,
      GeneralRouter.RouteDataObjectAttribute attr,
      String parentType,
      Stack<RoutingConfiguration.RoutingRule> stack
   ) {
      String pname = parser.getName();
      if (checkTag(pname)) {
         if (attr == null) {
            throw new NullPointerException("Select tag filter outside road attribute < " + pname + " > : " + parser.getLineNumber());
         }

         RoutingConfiguration.RoutingRule rr = new RoutingConfiguration.RoutingRule();
         rr.tagName = pname;
         rr.t = parser.getAttributeValue("", "t");
         rr.v = parser.getAttributeValue("", "v");
         rr.param = parser.getAttributeValue("", "param");
         rr.value1 = parser.getAttributeValue("", "value1");
         rr.value2 = parser.getAttributeValue("", "value2");
         rr.type = parser.getAttributeValue("", "type");
         if ((rr.type == null || rr.type.length() == 0) && parentType != null && parentType.length() > 0) {
            rr.type = parentType;
         }

         GeneralRouter.RouteAttributeContext ctx = currentRouter.getObjContext(attr);
         if ("select".equals(rr.tagName)) {
            String val = parser.getAttributeValue("", "value");
            String type = rr.type;
            ctx.registerNewRule(val, type);
            addSubclause(rr, ctx);

            for(int i = 0; i < stack.size(); ++i) {
               addSubclause(stack.get(i), ctx);
            }
         } else if (stack.size() > 0 && "select".equals(stack.peek().tagName)) {
            addSubclause(rr, ctx);
         }

         stack.push(rr);
      }
   }

   private static boolean checkTag(String pname) {
      return "select".equals(pname) || "if".equals(pname) || "ifnot".equals(pname) || "gt".equals(pname) || "le".equals(pname) || "eq".equals(pname);
   }

   private static void addSubclause(RoutingConfiguration.RoutingRule rr, GeneralRouter.RouteAttributeContext ctx) {
      boolean not = "ifnot".equals(rr.tagName);
      if (!Algorithms.isEmpty(rr.param)) {
         ctx.getLastRule().registerAndParamCondition(rr.param, not);
      }

      if (!Algorithms.isEmpty(rr.t)) {
         ctx.getLastRule().registerAndTagValueCondition(rr.t, Algorithms.isEmpty(rr.v) ? null : rr.v, not);
      }

      if ("gt".equals(rr.tagName)) {
         ctx.getLastRule().registerGreatCondition(rr.value1, rr.value2, rr.type);
      } else if ("le".equals(rr.tagName)) {
         ctx.getLastRule().registerLessCondition(rr.value1, rr.value2, rr.type);
      } else if ("eq".equals(rr.tagName)) {
         ctx.getLastRule().registerEqualCondition(rr.value1, rr.value2, rr.type);
      }
   }

   private static GeneralRouter parseRoutingProfile(XmlPullParser parser, RoutingConfiguration.Builder config, String filename) {
      String currentSelectedRouterName = parser.getAttributeValue("", "name");
      Map<String, String> attrs = new LinkedHashMap<>();

      for(int i = 0; i < parser.getAttributeCount(); ++i) {
         attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
      }

      GeneralRouter.GeneralRouterProfile c = Algorithms.parseEnumValue(
         GeneralRouter.GeneralRouterProfile.values(), parser.getAttributeValue("", "baseProfile"), GeneralRouter.GeneralRouterProfile.CAR
      );
      GeneralRouter currentRouter = new GeneralRouter(c, attrs);
      currentRouter.setProfileName(currentSelectedRouterName);
      if (filename != null) {
         currentRouter.setFilename(filename);
         currentSelectedRouterName = filename + "/" + currentSelectedRouterName;
      }

      config.routers.put(currentSelectedRouterName, currentRouter);
      return currentRouter;
   }

   private static void parseAttribute(XmlPullParser parser, RoutingConfiguration.Builder config, GeneralRouter currentRouter) {
      if (currentRouter != null) {
         currentRouter.addAttribute(parser.getAttributeValue("", "name"), parser.getAttributeValue("", "value"));
      } else {
         config.attributes.put(parser.getAttributeValue("", "name"), parser.getAttributeValue("", "value"));
      }
   }

   public static class Builder {
      private String defaultRouter = "";
      private Map<String, GeneralRouter> routers = new LinkedHashMap<>();
      private Map<String, String> attributes = new LinkedHashMap<>();
      private Set<Long> impassableRoadLocations = new HashSet<>();
      private QuadTree<Node> directionPointsBuilder;

      public Builder() {
      }

      public Builder(Map<String, String> defaultAttributes) {
         this.attributes.putAll(defaultAttributes);
      }

      public RoutingConfiguration build(String router, RoutingConfiguration.RoutingMemoryLimits memoryLimits) {
         return this.build(router, null, memoryLimits, new LinkedHashMap());
      }

      public RoutingConfiguration build(String router, RoutingConfiguration.RoutingMemoryLimits memoryLimits, Map<String, String> params) {
         return this.build(router, null, memoryLimits, params);
      }

      public RoutingConfiguration build(String router, Double direction, RoutingConfiguration.RoutingMemoryLimits memoryLimits, Map<String, String> params) {
         String derivedProfile = null;
         if (!this.routers.containsKey(router)) {
            for(Entry<String, GeneralRouter> r : this.routers.entrySet()) {
               String derivedProfiles = r.getValue().getAttribute("derivedProfiles");
               if (derivedProfiles != null && derivedProfiles.contains(router)) {
                  derivedProfile = router;
                  router = r.getKey();
                  break;
               }
            }

            if (derivedProfile == null) {
               router = this.defaultRouter;
            }
         }

         if (derivedProfile != null) {
            params.put("profile_" + derivedProfile, String.valueOf(true));
         }

         RoutingConfiguration i = new RoutingConfiguration();
         if (this.routers.containsKey(router)) {
            i.router = this.routers.get(router);
            if (params != null) {
               i.router = i.router.build(params);
            }

            i.routerName = router;
         }

         this.attributes.put("routerName", router);
         i.attributes.putAll(this.attributes);
         i.initialDirection = direction;
         i.recalculateDistance = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "recalculateDistanceHelp"), i.recalculateDistance);
         i.heuristicCoefficient = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "heuristicCoefficient"), i.heuristicCoefficient);
         i.minPointApproximation = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "minPointApproximation"), i.minPointApproximation);
         i.minStepApproximation = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "minStepApproximation"), i.minStepApproximation);
         i.maxStepApproximation = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "maxStepApproximation"), i.maxStepApproximation);
         i.smoothenPointsNoRoute = RoutingConfiguration.parseSilentFloat(this.getAttribute(i.router, "smoothenPointsNoRoute"), i.smoothenPointsNoRoute);
         i.router.setImpassableRoads(new HashSet<>(this.impassableRoadLocations));
         i.ZOOM_TO_LOAD_TILES = RoutingConfiguration.parseSilentInt(this.getAttribute(i.router, "zoomToLoadTiles"), i.ZOOM_TO_LOAD_TILES);
         int memoryLimitMB = memoryLimits.memoryLimitMb;
         int desirable = RoutingConfiguration.parseSilentInt(this.getAttribute(i.router, "memoryLimitInMB"), 0);
         if (desirable != 0) {
            i.memoryLimitation = (long)desirable * 1048576L;
         } else {
            if (memoryLimitMB == 0) {
               memoryLimitMB = 30;
            }

            i.memoryLimitation = (long)memoryLimitMB * 1048576L;
         }

         int desirableNativeLimit = RoutingConfiguration.parseSilentInt(this.getAttribute(i.router, "nativeMemoryLimitInMB"), 0);
         if (desirableNativeLimit != 0) {
            i.nativeMemoryLimitation = (long)desirableNativeLimit * 1048576L;
         } else {
            i.nativeMemoryLimitation = (long)memoryLimits.nativeMemoryLimitMb * 1048576L;
         }

         i.planRoadDirection = RoutingConfiguration.parseSilentInt(this.getAttribute(i.router, "planRoadDirection"), i.planRoadDirection);
         if (this.directionPointsBuilder != null) {
            QuadRect rect = new QuadRect(0.0, 0.0, 2.147483647E9, 2.147483647E9);
            List<Node> lst = this.directionPointsBuilder.queryInBox(rect, new ArrayList());
            i.directionPoints = new QuadTree<>(rect, 14, 0.5F);

            for(Node n : lst) {
               RoutingConfiguration.DirectionPoint dp = new RoutingConfiguration.DirectionPoint(n);
               int x = MapUtils.get31TileNumberX(dp.getLongitude());
               int y = MapUtils.get31TileNumberY(dp.getLatitude());
               i.directionPoints.insert(dp, new QuadRect((double)x, (double)y, (double)x, (double)y));
            }
         }

         return i;
      }

      public RoutingConfiguration.Builder setDirectionPoints(QuadTree<Node> directionPoints) {
         this.directionPointsBuilder = directionPoints;
         return this;
      }

      public void clearImpassableRoadLocations() {
         this.impassableRoadLocations.clear();
      }

      public Set<Long> getImpassableRoadLocations() {
         return this.impassableRoadLocations;
      }

      public RoutingConfiguration.Builder addImpassableRoad(long routeId) {
         this.impassableRoadLocations.add(routeId);
         return this;
      }

      public Map<String, String> getAttributes() {
         return this.attributes;
      }

      private String getAttribute(VehicleRouter router, String propertyName) {
         return router.containsAttribute(propertyName) ? router.getAttribute(propertyName) : this.attributes.get(propertyName);
      }

      public String getDefaultRouter() {
         return this.defaultRouter;
      }

      public GeneralRouter getRouter(String routingProfileName) {
         return this.routers.get(routingProfileName);
      }

      public String getRoutingProfileKeyByFileName(String fileName) {
         if (fileName != null && this.routers != null) {
            for(Entry<String, GeneralRouter> router : this.routers.entrySet()) {
               if (fileName.equals(router.getValue().getFilename())) {
                  return router.getKey();
               }
            }
         }

         return null;
      }

      public Map<String, GeneralRouter> getAllRouters() {
         return this.routers;
      }

      public void removeImpassableRoad(long routeId) {
         this.impassableRoadLocations.remove(routeId);
      }
   }

   public static class DirectionPoint extends Node {
      private static final long serialVersionUID = -7496599771204656505L;
      public double distance = Double.MAX_VALUE;
      public RouteDataObject connected;
      public TIntArrayList types = new TIntArrayList();
      public int connectedx;
      public int connectedy;
      public static final String TAG = "osmand_dp";
      public static final String DELETE_TYPE = "osmand_delete_point";
      public static final String CREATE_TYPE = "osmand_add_point";
      public static final String ANGLE_TAG = "apply_direction_angle";
      public static final double MAX_ANGLE_DIFF = 45.0;

      public DirectionPoint(Node n) {
         super(n, n.getId());
      }

      public double getAngle() {
         String angle = this.getTag("apply_direction_angle");
         if (angle != null) {
            try {
               return Double.parseDouble(angle);
            } catch (NumberFormatException var3) {
               throw new RuntimeException(var3);
            }
         } else {
            return Double.NaN;
         }
      }
   }

   public static class RoutingMemoryLimits {
      public int memoryLimitMb;
      public int nativeMemoryLimitMb;

      public RoutingMemoryLimits(int memoryLimitMb, int nativeMemoryLimitMb) {
         this.memoryLimitMb = memoryLimitMb;
         this.nativeMemoryLimitMb = nativeMemoryLimitMb;
      }
   }

   private static class RoutingRule {
      String tagName;
      String t;
      String v;
      String param;
      String value1;
      String value2;
      String type;

      private RoutingRule() {
      }
   }
}
