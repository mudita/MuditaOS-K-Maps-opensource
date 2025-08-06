package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TLongHashSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class GeneralRouter implements VehicleRouter {
   private static final float CAR_SHORTEST_DEFAULT_SPEED = 15.277779F;
   private static final float BICYCLE_SHORTEST_DEFAULT_SPEED = 4.166667F;
   public static final String USE_SHORTEST_WAY = "short_way";
   public static final String USE_HEIGHT_OBSTACLES = "height_obstacles";
   public static final String AVOID_FERRIES = "avoid_ferries";
   public static final String AVOID_TOLL = "avoid_toll";
   public static final String AVOID_MOTORWAY = "avoid_motorway";
   public static final String AVOID_UNPAVED = "avoid_unpaved";
   public static final String PREFER_MOTORWAYS = "prefer_motorway";
   public static final String ALLOW_PRIVATE = "allow_private";
   public static final String ALLOW_PRIVATE_FOR_TRUCK = "allow_private_for_truck";
   public static final String HAZMAT_CATEGORY = "hazmat_category";
   public static final String GOODS_RESTRICTIONS = "goods_restrictions";
   public static final String ALLOW_MOTORWAYS = "allow_motorway";
   public static final String DEFAULT_SPEED = "default_speed";
   public static final String MIN_SPEED = "min_speed";
   public static final String MAX_SPEED = "max_speed";
   public static final String VEHICLE_HEIGHT = "height";
   public static final String VEHICLE_WEIGHT = "weight";
   public static final String VEHICLE_WIDTH = "width";
   public static final String VEHICLE_LENGTH = "length";
   public static final String MOTOR_TYPE = "motor_type";
   public static final String CHECK_ALLOW_PRIVATE_NEEDED = "check_allow_private_needed";
   private static boolean USE_CACHE = true;
   public static long TIMER = 0L;
   private final GeneralRouter.RouteAttributeContext[] objectAttributes;
   public final Map<String, String> attributes;
   private final Map<String, GeneralRouter.RoutingParameter> parameters;
   private final Map<String, Integer> universalRules;
   private final List<String> universalRulesById;
   private final Map<String, BitSet> tagRuleMask;
   private final ArrayList<Object> ruleToValue;
   private boolean shortestRoute;
   private boolean heightObstacles;
   private boolean allowPrivate;
   private String filename = null;
   private String profileName = "";
   private Map<BinaryMapRouteReaderAdapter.RouteRegion, Map<Integer, Integer>> regionConvert = new LinkedHashMap<>();
   private boolean restrictionsAware = true;
   private float sharpTurn;
   private float roundaboutTurn;
   private float slightTurn;
   private float minSpeed = 0.28F;
   private float defaultSpeed = 1.0F;
   private float maxSpeed = 10.0F;
   private float maxVehicleSpeed;
   private TLongHashSet impassableRoads;
   private GeneralRouter.GeneralRouterProfile profile;
   Map<BinaryMapRouteReaderAdapter.RouteRegion, Map<GeneralRouter.IntHolder, Float>>[] evalCache;

   public GeneralRouter(GeneralRouter parent, Map<String, String> params) {
      this.profile = parent.profile;
      this.attributes = new LinkedHashMap<>();

      for(Entry<String, String> next : parent.attributes.entrySet()) {
         this.addAttribute(next.getKey(), next.getValue());
      }

      this.universalRules = parent.universalRules;
      this.universalRulesById = parent.universalRulesById;
      this.tagRuleMask = parent.tagRuleMask;
      this.ruleToValue = parent.ruleToValue;
      this.parameters = parent.parameters;
      this.objectAttributes = new GeneralRouter.RouteAttributeContext[GeneralRouter.RouteDataObjectAttribute.values().length];

      for(int i = 0; i < this.objectAttributes.length; ++i) {
         this.objectAttributes[i] = new GeneralRouter.RouteAttributeContext(parent.objectAttributes[i], params);
      }

      this.shortestRoute = params.containsKey("short_way") && parseSilentBoolean(params.get("short_way"), false);
      this.heightObstacles = params.containsKey("height_obstacles") && parseSilentBoolean(params.get("height_obstacles"), false);
      if (params.containsKey("profile_truck")) {
         this.allowPrivate = params.containsKey("allow_private_for_truck") && parseSilentBoolean(params.get("allow_private_for_truck"), false);
      } else {
         this.allowPrivate = params.containsKey("allow_private") && parseSilentBoolean(params.get("allow_private"), false);
      }

      if (params.containsKey("default_speed")) {
         this.defaultSpeed = parseSilentFloat(params.get("default_speed"), this.defaultSpeed);
      }

      if (params.containsKey("min_speed")) {
         this.minSpeed = parseSilentFloat(params.get("min_speed"), this.minSpeed);
      }

      if (params.containsKey("max_speed")) {
         this.maxSpeed = parseSilentFloat(params.get("max_speed"), this.maxSpeed);
      }

      this.maxVehicleSpeed = this.maxSpeed;
      if (this.shortestRoute) {
         if (this.profile == GeneralRouter.GeneralRouterProfile.BICYCLE) {
            this.maxSpeed = Math.min(4.166667F, this.maxSpeed);
         } else {
            this.maxSpeed = Math.min(15.277779F, this.maxSpeed);
         }
      }

      this.initCaches();
   }

   public GeneralRouter(GeneralRouter.GeneralRouterProfile profile, Map<String, String> attributes) {
      this.profile = profile;
      this.attributes = new LinkedHashMap<>();

      for(Entry<String, String> next : attributes.entrySet()) {
         this.addAttribute(next.getKey(), next.getValue());
      }

      this.objectAttributes = new GeneralRouter.RouteAttributeContext[GeneralRouter.RouteDataObjectAttribute.values().length];

      for(int i = 0; i < this.objectAttributes.length; ++i) {
         this.objectAttributes[i] = new GeneralRouter.RouteAttributeContext();
      }

      this.universalRules = new LinkedHashMap<>();
      this.universalRulesById = new ArrayList<>();
      this.tagRuleMask = new LinkedHashMap<>();
      this.ruleToValue = new ArrayList<>();
      this.parameters = new LinkedHashMap<>();
      this.initCaches();
   }

   private void initCaches() {
      int l = GeneralRouter.RouteDataObjectAttribute.values().length;
      this.evalCache = new Map[l];

      for(int i = 0; i < l; ++i) {
         this.evalCache[i] = new HashMap<>();
      }
   }

   public String getFilename() {
      return this.filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public String getProfileName() {
      return this.profileName;
   }

   public void setProfileName(String profileName) {
      this.profileName = profileName;
   }

   public GeneralRouter.GeneralRouterProfile getProfile() {
      return this.profile;
   }

   public boolean getHeightObstacles() {
      return this.heightObstacles;
   }

   public Map<String, GeneralRouter.RoutingParameter> getParameters() {
      return this.parameters;
   }

   public void addAttribute(String k, String v) {
      this.attributes.put(k, v);
      if (k.equals("restrictionsAware")) {
         this.restrictionsAware = parseSilentBoolean(v, this.restrictionsAware);
      } else if (k.equals("sharpTurn") || k.equals("leftTurn")) {
         this.sharpTurn = parseSilentFloat(v, this.sharpTurn);
      } else if (k.equals("slightTurn") || k.equals("rightTurn")) {
         this.slightTurn = parseSilentFloat(v, this.slightTurn);
      } else if (k.equals("roundaboutTurn")) {
         this.roundaboutTurn = parseSilentFloat(v, this.roundaboutTurn);
      } else if (k.equals("minDefaultSpeed") || k.equals("defaultSpeed")) {
         this.defaultSpeed = parseSilentFloat(v, this.defaultSpeed * 3.6F) / 3.6F;
      } else if (k.equals("minSpeed")) {
         this.minSpeed = parseSilentFloat(v, this.minSpeed * 3.6F) / 3.6F;
      } else if (k.equals("maxDefaultSpeed") || k.equals("maxSpeed")) {
         this.maxSpeed = parseSilentFloat(v, this.maxSpeed * 3.6F) / 3.6F;
      }
   }

   public GeneralRouter.RouteAttributeContext getObjContext(GeneralRouter.RouteDataObjectAttribute a) {
      return this.objectAttributes[a.ordinal()];
   }

   public void registerBooleanParameter(String id, String group, String name, String description, String[] profiles, boolean defaultValue) {
      GeneralRouter.RoutingParameter rp = new GeneralRouter.RoutingParameter();
      rp.id = id;
      rp.group = group;
      rp.name = name;
      rp.description = description;
      rp.profiles = profiles;
      rp.type = GeneralRouter.RoutingParameterType.BOOLEAN;
      rp.defaultBoolean = defaultValue;
      this.parameters.put(rp.id, rp);
   }

   public void registerNumericParameter(String id, String name, String description, String[] profiles, Double[] vls, String[] vlsDescriptions) {
      GeneralRouter.RoutingParameter rp = new GeneralRouter.RoutingParameter();
      rp.name = name;
      rp.description = description;
      rp.id = id;
      rp.profiles = profiles;
      rp.possibleValues = vls;
      rp.possibleValueDescriptions = vlsDescriptions;
      rp.type = GeneralRouter.RoutingParameterType.NUMERIC;
      this.parameters.put(rp.id, rp);
   }

   @Override
   public boolean acceptLine(RouteDataObject way) {
      Float res = this.getCache(GeneralRouter.RouteDataObjectAttribute.ACCESS, way);
      if (res == null) {
         res = (float)this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ACCESS).evaluateInt(way, 0);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.ACCESS, way, res);
      }

      if (this.impassableRoads != null && this.impassableRoads.contains(way.id)) {
         return false;
      } else {
         return res >= 0.0F;
      }
   }

   public boolean isAllowPrivate() {
      return this.allowPrivate;
   }

   public long[] getImpassableRoadIds() {
      return this.impassableRoads == null ? new long[0] : this.impassableRoads.toArray();
   }

   public int registerTagValueAttribute(String tag, String value) {
      String key = tag + "$" + value;
      if (this.universalRules.containsKey(key)) {
         return this.universalRules.get(key);
      } else {
         int id = this.universalRules.size();
         this.universalRulesById.add(key);
         this.universalRules.put(key, id);
         if (!this.tagRuleMask.containsKey(tag)) {
            this.tagRuleMask.put(tag, new BitSet());
         }

         this.tagRuleMask.get(tag).set(id);
         return id;
      }
   }

   private Object parseValue(String value, String type) {
      float vl = -1.0F;
      value = value.trim();
      if ("speed".equals(type)) {
         vl = RouteDataObject.parseSpeed(value, vl);
      } else if ("weight".equals(type)) {
         vl = RouteDataObject.parseWeightInTon(value, vl);
      } else if ("length".equals(type)) {
         vl = RouteDataObject.parseLength(value, vl);
      } else {
         int i = Algorithms.findFirstNumberEndIndex(value);
         if (i > 0) {
            return Float.parseFloat(value.substring(0, i));
         }
      }

      return vl == -1.0F ? null : vl;
   }

   private Object parseValueFromTag(int id, String type) {
      while(this.ruleToValue.size() <= id) {
         this.ruleToValue.add(null);
      }

      Object res = this.ruleToValue.get(id);
      if (res == null) {
         String v = this.universalRulesById.get(id);
         String value = v.substring(v.indexOf(36) + 1);
         res = this.parseValue(value, type);
         if (res == null) {
            res = "";
         }

         this.ruleToValue.set(id, res);
      }

      return "".equals(res) ? null : res;
   }

   public GeneralRouter build(Map<String, String> params) {
      return new GeneralRouter(this, params);
   }

   @Override
   public boolean restrictionsAware() {
      return this.restrictionsAware;
   }

   @Override
   public float defineObstacle(RouteDataObject road, int point, boolean dir) {
      int[] pointTypes = road.getPointTypes(point);
      if (pointTypes != null) {
         Float obst = this.getCache(GeneralRouter.RouteDataObjectAttribute.OBSTACLES, road.region, pointTypes, dir);
         if (obst == null) {
            int[] filteredPointTypes = this.filterDirectionTags(road, pointTypes, dir);
            obst = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.OBSTACLES).evaluateFloat(road.region, filteredPointTypes, 0.0F);
            this.putCache(GeneralRouter.RouteDataObjectAttribute.OBSTACLES, road.region, pointTypes, obst, dir);
         }

         return obst;
      } else {
         return 0.0F;
      }
   }

   @Override
   public float defineRoutingObstacle(RouteDataObject road, int point, boolean dir) {
      int[] pointTypes = road.getPointTypes(point);
      if (pointTypes != null) {
         Float obst = this.getCache(GeneralRouter.RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region, pointTypes, dir);
         if (obst == null) {
            int[] filteredPointTypes = this.filterDirectionTags(road, pointTypes, dir);
            obst = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROUTING_OBSTACLES).evaluateFloat(road.region, filteredPointTypes, 0.0F);
            this.putCache(GeneralRouter.RouteDataObjectAttribute.ROUTING_OBSTACLES, road.region, pointTypes, obst, dir);
         }

         return obst;
      } else {
         return 0.0F;
      }
   }

   private int[] filterDirectionTags(RouteDataObject road, int[] pointTypes, boolean dir) {
      int wayOppositeDirection = dir ? -1 : 1;
      int direction = 0;
      int tdirection = 0;
      int hdirection = 0;

      for(int i = 0; i < pointTypes.length; ++i) {
         if (pointTypes[i] == road.region.directionBackward) {
            direction = -1;
         } else if (pointTypes[i] == road.region.directionForward) {
            direction = 1;
         } else if (pointTypes[i] == road.region.directionTrafficSignalsBackward) {
            tdirection = -1;
         } else if (pointTypes[i] == road.region.directionTrafficSignalsForward) {
            tdirection = 1;
         } else if (pointTypes[i] == road.region.maxheightBackward) {
            hdirection = -1;
         } else if (pointTypes[i] == road.region.maxheightForward) {
            hdirection = 1;
         }
      }

      if (direction == 0 && tdirection == 0 && hdirection == 0) {
         return pointTypes;
      } else {
         TIntArrayList filteredRules = new TIntArrayList();

         for(int i = 0; i < pointTypes.length; ++i) {
            boolean skip = false;
            if ((pointTypes[i] == road.region.stopSign || pointTypes[i] == road.region.giveWaySign) && direction == wayOppositeDirection) {
               skip = true;
            } else if (pointTypes[i] == road.region.trafficSignals && tdirection == wayOppositeDirection) {
               skip = true;
            } else if (hdirection == wayOppositeDirection) {
               skip = true;
            }

            if (!skip) {
               filteredRules.add(pointTypes[i]);
            }
         }

         return filteredRules.toArray();
      }
   }

   @Override
   public double defineHeightObstacle(RouteDataObject road, short startIndex, short endIndex) {
      if (!this.heightObstacles) {
         return 0.0;
      } else {
         float[] heightArray = road.calculateHeightArray();
         if (heightArray != null && heightArray.length != 0) {
            double sum = 0.0;
            GeneralRouter.RouteAttributeContext objContext = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.OBSTACLE_SRTM_ALT_SPEED);

            int knext;
            for(int k = startIndex; k != endIndex; k = knext) {
               knext = startIndex < endIndex ? k + 1 : k - 1;
               double dist = startIndex < endIndex ? (double)heightArray[2 * knext] : (double)heightArray[2 * k];
               double diff = (double)(heightArray[2 * knext + 1] - heightArray[2 * k + 1]);
               if (diff != 0.0 && dist > 0.0) {
                  double incl = Math.abs(diff / dist);
                  int percentIncl = (int)(incl * 100.0);
                  percentIncl = (percentIncl + 2) / 3 * 3 - 2;
                  if (percentIncl >= 1) {
                     objContext.paramContext.incline = diff > 0.0 ? (double)percentIncl : (double)(-percentIncl);
                     sum += (double)objContext.evaluateFloat(road, 0.0F) * (diff > 0.0 ? diff : -diff);
                  }
               }
            }

            return sum;
         } else {
            return 0.0;
         }
      }
   }

   @Override
   public int isOneWay(RouteDataObject road) {
      Float res = this.getCache(GeneralRouter.RouteDataObjectAttribute.ONEWAY, road);
      if (res == null) {
         res = (float)this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ONEWAY).evaluateInt(road, 0);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.ONEWAY, road, res);
      }

      return res.intValue();
   }

   @Override
   public boolean isArea(RouteDataObject road) {
      return this.getObjContext(GeneralRouter.RouteDataObjectAttribute.AREA).evaluateInt(road, 0) == 1;
   }

   @Override
   public float getPenaltyTransition(RouteDataObject road) {
      Float vl = this.getCache(GeneralRouter.RouteDataObjectAttribute.PENALTY_TRANSITION, road);
      if (vl == null) {
         vl = (float)this.getObjContext(GeneralRouter.RouteDataObjectAttribute.PENALTY_TRANSITION).evaluateInt(road, 0);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.PENALTY_TRANSITION, road, vl);
      }

      return vl;
   }

   @Override
   public float defineRoutingSpeed(RouteDataObject road) {
      Float definedSpd = this.getCache(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED, road);
      if (definedSpd == null) {
         float spd = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, this.defaultSpeed);
         definedSpd = Math.max(Math.min(spd, this.maxSpeed), this.minSpeed);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED, road, definedSpd);
      }

      return definedSpd;
   }

   @Override
   public float defineVehicleSpeed(RouteDataObject road) {
      if (this.maxVehicleSpeed != this.maxSpeed) {
         float spd = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, this.defaultSpeed);
         return Math.max(Math.min(spd, this.maxVehicleSpeed), this.minSpeed);
      } else {
         Float sp = this.getCache(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED, road);
         if (sp == null) {
            float spd = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED).evaluateFloat(road, this.defaultSpeed);
            sp = Math.max(Math.min(spd, this.maxVehicleSpeed), this.minSpeed);
            this.putCache(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED, road, sp);
         }

         return sp;
      }
   }

   @Override
   public float defineSpeedPriority(RouteDataObject road) {
      Float sp = this.getCache(GeneralRouter.RouteDataObjectAttribute.ROAD_PRIORITIES, road);
      if (sp == null) {
         sp = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_PRIORITIES).evaluateFloat(road, 1.0F);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.ROAD_PRIORITIES, road, sp, false);
      }

      return sp;
   }

   @Override
   public float defineDestinationPriority(RouteDataObject road) {
      Float sp = this.getCache(GeneralRouter.RouteDataObjectAttribute.DESTINATION_PRIORITIES, road);
      if (sp == null) {
         sp = this.getObjContext(GeneralRouter.RouteDataObjectAttribute.DESTINATION_PRIORITIES).evaluateFloat(road, 1.0F);
         this.putCache(GeneralRouter.RouteDataObjectAttribute.DESTINATION_PRIORITIES, road, sp, false);
      }

      return sp;
   }

   private void putCache(GeneralRouter.RouteDataObjectAttribute attr, RouteDataObject road, Float val) {
      this.putCache(attr, road.region, road.types, val, false);
   }

   private void putCache(GeneralRouter.RouteDataObjectAttribute attr, RouteDataObject road, Float val, boolean extra) {
      this.putCache(attr, road.region, road.types, val, extra);
   }

   private void putCache(GeneralRouter.RouteDataObjectAttribute attr, BinaryMapRouteReaderAdapter.RouteRegion reg, int[] types, Float val, boolean extra) {
      Map<BinaryMapRouteReaderAdapter.RouteRegion, Map<GeneralRouter.IntHolder, Float>> ch = this.evalCache[attr.ordinal()];
      if (USE_CACHE) {
         Map<GeneralRouter.IntHolder, Float> rM = ch.get(reg);
         if (rM == null) {
            rM = new HashMap<>();
            ch.put(reg, rM);
         }

         rM.put(new GeneralRouter.IntHolder(types, extra), val);
      }
   }

   private Float getCache(GeneralRouter.RouteDataObjectAttribute attr, RouteDataObject road) {
      return this.getCache(attr, road.region, road.types, false);
   }

   private Float getCache(GeneralRouter.RouteDataObjectAttribute attr, BinaryMapRouteReaderAdapter.RouteRegion reg, int[] types, boolean extra) {
      Map<BinaryMapRouteReaderAdapter.RouteRegion, Map<GeneralRouter.IntHolder, Float>> ch = this.evalCache[attr.ordinal()];
      if (USE_CACHE) {
         Map<GeneralRouter.IntHolder, Float> rM = ch.get(reg);
         if (rM == null) {
            return null;
         }

         Float vl = rM.get(new GeneralRouter.IntHolder(types, extra));
         if (vl != null) {
            return vl;
         }
      }

      return null;
   }

   @Override
   public float getDefaultSpeed() {
      return this.defaultSpeed;
   }

   @Override
   public float getMinSpeed() {
      return this.minSpeed;
   }

   @Override
   public float getMaxSpeed() {
      return this.maxSpeed;
   }

   public double getLeftTurn() {
      return (double)this.sharpTurn;
   }

   public double getRightTurn() {
      return (double)this.slightTurn;
   }

   public double getRoundaboutTurn() {
      return (double)this.roundaboutTurn;
   }

   @Override
   public double calculateTurnTime(BinaryRoutePlanner.RouteSegment segment, int segmentEnd, BinaryRoutePlanner.RouteSegment prev, int prevSegmentEnd) {
      float ts = this.getPenaltyTransition(segment.getRoad());
      float prevTs = this.getPenaltyTransition(prev.getRoad());
      float totalPenalty = 0.0F;
      if (prevTs != ts) {
         totalPenalty += Math.abs(ts - prevTs) / 2.0F;
      }

      if (this.shortestRoute) {
         return (double)totalPenalty;
      } else {
         if (segment.getRoad().roundabout() && !prev.getRoad().roundabout()) {
            double rt = this.getRoundaboutTurn();
            if (rt > 0.0) {
               totalPenalty = (float)((double)totalPenalty + rt);
            }
         } else if (this.getLeftTurn() > 0.0 || this.getRightTurn() > 0.0) {
            double a1 = segment.getRoad().directionRoute(segment.getSegmentStart(), segment.getSegmentStart() < segmentEnd);
            double a2 = prev.getRoad().directionRoute(prevSegmentEnd, prevSegmentEnd < prev.getSegmentStart());
            double diff = Math.abs(MapUtils.alignAngleDifference(a1 - a2 - Math.PI));
            if (diff > Math.PI * 2.0 / 3.0) {
               totalPenalty = (float)((double)totalPenalty + this.getLeftTurn());
            } else if (diff > Math.PI / 3) {
               totalPenalty = (float)((double)totalPenalty + this.getRightTurn());
            }
         }

         return (double)totalPenalty;
      }
   }

   @Override
   public boolean containsAttribute(String attribute) {
      return this.attributes.containsKey(attribute);
   }

   @Override
   public String getAttribute(String attribute) {
      return this.attributes.get(attribute);
   }

   public float getFloatAttribute(String attribute, float v) {
      return parseSilentFloat(this.getAttribute(attribute), v);
   }

   public int getIntAttribute(String attribute, int v) {
      return (int)parseSilentFloat(this.getAttribute(attribute), (float)v);
   }

   private static boolean parseSilentBoolean(String t, boolean v) {
      return t != null && t.length() != 0 ? Boolean.parseBoolean(t) : v;
   }

   private static float parseSilentFloat(String t, float v) {
      return t != null && t.length() != 0 ? Float.parseFloat(t) : v;
   }

   public void printRules(PrintStream out) {
      for(int i = 0; i < GeneralRouter.RouteDataObjectAttribute.values().length; ++i) {
         out.println(GeneralRouter.RouteDataObjectAttribute.values()[i]);
         this.objectAttributes[i].printRules(out);
      }
   }

   public void setImpassableRoads(Set<Long> impassableRoads) {
      if (impassableRoads != null && !impassableRoads.isEmpty()) {
         this.impassableRoads = new TLongHashSet(impassableRoads);
      } else if (this.impassableRoads != null) {
         this.impassableRoads.clear();
      }
   }

   public static enum GeneralRouterProfile {
      CAR,
      PEDESTRIAN,
      BICYCLE,
      BOAT,
      PUBLIC_TRANSPORT,
      HORSEBACKRIDING;
   }

   class IntHolder {
      private final int[] array;
      private final boolean extra;

      IntHolder(int[] ts, boolean extra) {
         this.array = ts;
         this.extra = extra;
      }

      @Override
      public int hashCode() {
         return Arrays.hashCode(this.array) + (this.extra ? 1 : 0);
      }

      @Override
      public boolean equals(Object other) {
         if (this.array == other) {
            return true;
         } else if (!(other instanceof GeneralRouter.IntHolder)) {
            return false;
         } else {
            return ((GeneralRouter.IntHolder)other).extra != this.extra ? false : Arrays.equals(this.array, ((GeneralRouter.IntHolder)other).array);
         }
      }
   }

   private class ParameterContext {
      private Map<String, String> vars;
      private double incline = 0.0;

      private ParameterContext() {
      }
   }

   public class RouteAttributeContext {
      List<GeneralRouter.RouteAttributeEvalRule> rules = new ArrayList<>();
      GeneralRouter.ParameterContext paramContext = null;

      public RouteAttributeContext() {
      }

      public RouteAttributeContext(GeneralRouter.RouteAttributeContext original, Map<String, String> params) {
         if (params != null) {
            this.paramContext = GeneralRouter.this.new ParameterContext();
            this.paramContext.vars = params;
         }

         for(GeneralRouter.RouteAttributeEvalRule rt : original.rules) {
            if (this.checkParameter(rt)) {
               this.rules.add(rt);
            }
         }
      }

      public GeneralRouter.RouteAttributeEvalRule[] getRules() {
         return this.rules.toArray(new GeneralRouter.RouteAttributeEvalRule[0]);
      }

      public String[] getParamKeys() {
         return this.paramContext == null ? new String[0] : this.paramContext.vars.keySet().toArray(new String[0]);
      }

      public String[] getParamValues() {
         return this.paramContext == null ? new String[0] : this.paramContext.vars.values().toArray(new String[0]);
      }

      private Object evaluate(RouteDataObject ro) {
         return this.evaluate(this.convert(ro.region, ro.types));
      }

      public void printRules(PrintStream out) {
         for(GeneralRouter.RouteAttributeEvalRule r : this.rules) {
            r.printRule(out);
         }
      }

      public GeneralRouter.RouteAttributeEvalRule registerNewRule(String selectValue, String selectType) {
         GeneralRouter.RouteAttributeEvalRule ev = GeneralRouter.this.new RouteAttributeEvalRule();
         ev.registerSelectValue(selectValue, selectType);
         this.rules.add(ev);
         return ev;
      }

      public GeneralRouter.RouteAttributeEvalRule getLastRule() {
         return this.rules.get(this.rules.size() - 1);
      }

      private Object evaluate(BitSet types) {
         for(int k = 0; k < this.rules.size(); ++k) {
            GeneralRouter.RouteAttributeEvalRule r = this.rules.get(k);
            Object o = r.eval(types, this.paramContext);
            if (o != null) {
               return o;
            }
         }

         return null;
      }

      private boolean checkParameter(GeneralRouter.RouteAttributeEvalRule r) {
         if (this.paramContext != null && r.parameters.size() > 0) {
            for(String p : r.parameters) {
               boolean not = false;
               if (p.startsWith("-")) {
                  not = true;
                  p = p.substring(1);
               }

               boolean val = this.paramContext.vars.containsKey(p);
               if (not && val) {
                  return false;
               }

               if (!not && !val) {
                  return false;
               }
            }
         }

         return true;
      }

      public int evaluateInt(RouteDataObject ro, int defValue) {
         Object o = this.evaluate(ro);
         return !(o instanceof Number) ? defValue : ((Number)o).intValue();
      }

      public int evaluateInt(BinaryMapRouteReaderAdapter.RouteRegion region, int[] types, int defValue) {
         Object o = this.evaluate(this.convert(region, types));
         return !(o instanceof Number) ? defValue : ((Number)o).intValue();
      }

      public int evaluateInt(BitSet rawTypes, int defValue) {
         Object o = this.evaluate(rawTypes);
         return !(o instanceof Number) ? defValue : ((Number)o).intValue();
      }

      public float evaluateFloat(RouteDataObject ro, float defValue) {
         Object o = this.evaluate(ro);
         return !(o instanceof Number) ? defValue : ((Number)o).floatValue();
      }

      public float evaluateFloat(BinaryMapRouteReaderAdapter.RouteRegion region, int[] types, float defValue) {
         Object o = this.evaluate(this.convert(region, types));
         return !(o instanceof Number) ? defValue : ((Number)o).floatValue();
      }

      public float evaluateFloat(BitSet rawTypes, float defValue) {
         Object o = this.evaluate(rawTypes);
         return !(o instanceof Number) ? defValue : ((Number)o).floatValue();
      }

      private BitSet convert(BinaryMapRouteReaderAdapter.RouteRegion reg, int[] types) {
         BitSet b = new BitSet(GeneralRouter.this.universalRules.size());
         Map<Integer, Integer> map = GeneralRouter.this.regionConvert.get(reg);
         if (map == null) {
            map = new HashMap<>();
            GeneralRouter.this.regionConvert.put(reg, map);
         }

         for(int k = 0; k < types.length; ++k) {
            Integer nid = map.get(types[k]);
            if (nid == null) {
               BinaryMapRouteReaderAdapter.RouteTypeRule r = reg.quickGetEncodingRule(types[k]);
               nid = GeneralRouter.this.registerTagValueAttribute(r.getTag(), r.getValue());
               map.put(types[k], nid);
            }

            b.set(nid);
         }

         return b;
      }
   }

   public class RouteAttributeEvalRule {
      protected List<String> parameters = new ArrayList<>();
      protected List<String> tagValueCondDefTag = new ArrayList<>();
      protected List<String> tagValueCondDefValue = new ArrayList<>();
      protected List<Boolean> tagValueCondDefNot = new ArrayList<>();
      protected String selectValueDef = null;
      protected Object selectValue = null;
      protected String selectType = null;
      protected BitSet filterTypes = new BitSet();
      protected BitSet filterNotTypes = new BitSet();
      protected BitSet evalFilterTypes = new BitSet();
      protected Set<String> onlyTags = new LinkedHashSet<>();
      protected Set<String> onlyNotTags = new LinkedHashSet<>();
      protected List<GeneralRouter.RouteAttributeExpression> expressions = new ArrayList<>();

      public GeneralRouter.RouteAttributeExpression[] getExpressions() {
         return this.expressions.toArray(new GeneralRouter.RouteAttributeExpression[0]);
      }

      public String[] getParameters() {
         return this.parameters.toArray(new String[0]);
      }

      public String[] getTagValueCondDefTag() {
         return this.tagValueCondDefTag.toArray(new String[0]);
      }

      public String[] getTagValueCondDefValue() {
         return this.tagValueCondDefValue.toArray(new String[0]);
      }

      public boolean[] getTagValueCondDefNot() {
         boolean[] r = new boolean[this.tagValueCondDefNot.size()];

         for(int i = 0; i < r.length; ++i) {
            r[i] = this.tagValueCondDefNot.get(i);
         }

         return r;
      }

      public void registerSelectValue(String value, String type) {
         this.selectType = type;
         this.selectValueDef = value;
         if (!value.startsWith(":") && !value.startsWith("$")) {
            this.selectValue = GeneralRouter.this.parseValue(value, type);
            if (this.selectValue == null) {
               System.err.println("Routing.xml select value '" + value + "' was not registered");
            }
         } else {
            this.selectValue = value;
         }
      }

      public void printRule(PrintStream out) {
         out.print(" Select " + this.selectValue + " if ");

         for(int k = 0; k < this.filterTypes.length(); ++k) {
            if (this.filterTypes.get(k)) {
               String key = GeneralRouter.this.universalRulesById.get(k);
               out.print(key + " ");
            }
         }

         if (this.filterNotTypes.length() > 0) {
            out.print(" ifnot ");
         }

         for(int k = 0; k < this.filterNotTypes.length(); ++k) {
            if (this.filterNotTypes.get(k)) {
               String key = GeneralRouter.this.universalRulesById.get(k);
               out.print(key + " ");
            }
         }

         for(int k = 0; k < this.parameters.size(); ++k) {
            out.print(" param=" + (String)this.parameters.get(k));
         }

         if (this.onlyTags.size() > 0) {
            out.print(" match tag = " + this.onlyTags);
         }

         if (this.onlyNotTags.size() > 0) {
            out.print(" not match tag = " + this.onlyNotTags);
         }

         if (this.expressions.size() > 0) {
            out.println(" subexpressions " + this.expressions.size());
         }

         out.println();
      }

      public void registerAndTagValueCondition(String tag, String value, boolean not) {
         this.tagValueCondDefTag.add(tag);
         this.tagValueCondDefValue.add(value);
         this.tagValueCondDefNot.add(not);
         if (value == null) {
            if (not) {
               this.onlyNotTags.add(tag);
            } else {
               this.onlyTags.add(tag);
            }
         } else {
            int vtype = GeneralRouter.this.registerTagValueAttribute(tag, value);
            if (not) {
               this.filterNotTypes.set(vtype);
            } else {
               this.filterTypes.set(vtype);
            }
         }
      }

      public void registerLessCondition(String value1, String value2, String valueType) {
         this.expressions.add(GeneralRouter.this.new RouteAttributeExpression(new String[]{value1, value2}, valueType, 1));
      }

      public void registerGreatCondition(String value1, String value2, String valueType) {
         this.expressions.add(GeneralRouter.this.new RouteAttributeExpression(new String[]{value1, value2}, valueType, 2));
      }

      public void registerEqualCondition(String value1, String value2, String valueType) {
         this.expressions.add(GeneralRouter.this.new RouteAttributeExpression(new String[]{value1, value2}, valueType, 3));
      }

      public void registerAndParamCondition(String param, boolean not) {
         param = not ? "-" + param : param;
         this.parameters.add(param);
      }

      public synchronized Object eval(BitSet types, GeneralRouter.ParameterContext paramContext) {
         return this.matches(types, paramContext) ? this.calcSelectValue(types, paramContext) : null;
      }

      protected Object calcSelectValue(BitSet types, GeneralRouter.ParameterContext paramContext) {
         if (this.selectValue instanceof String && this.selectValue.toString().startsWith("$")) {
            BitSet mask = GeneralRouter.this.tagRuleMask.get(this.selectValue.toString().substring(1));
            if (mask != null && mask.intersects(types)) {
               BitSet findBit = new BitSet(mask.length());
               findBit.or(mask);
               findBit.and(types);
               int value = findBit.nextSetBit(0);
               return GeneralRouter.this.parseValueFromTag(value, this.selectType);
            }
         } else if (this.selectValue instanceof String && this.selectValue.toString().startsWith(":")) {
            String p = ((String)this.selectValue).substring(1);
            if (paramContext == null || !paramContext.vars.containsKey(p)) {
               return null;
            }

            this.selectValue = GeneralRouter.this.parseValue(paramContext.vars.get(p), this.selectType);
         }

         return this.selectValue;
      }

      public boolean matches(BitSet types, GeneralRouter.ParameterContext paramContext) {
         if (!this.checkAllTypesShouldBePresent(types)) {
            return false;
         } else if (!this.checkAllTypesShouldNotBePresent(types)) {
            return false;
         } else if (!this.checkFreeTags(types)) {
            return false;
         } else if (!this.checkNotFreeTags(types)) {
            return false;
         } else {
            return this.checkExpressions(types, paramContext);
         }
      }

      private boolean checkExpressions(BitSet types, GeneralRouter.ParameterContext paramContext) {
         for(GeneralRouter.RouteAttributeExpression e : this.expressions) {
            if (!e.matches(types, paramContext)) {
               return false;
            }
         }

         return true;
      }

      private boolean checkFreeTags(BitSet types) {
         for(String ts : this.onlyTags) {
            BitSet b = GeneralRouter.this.tagRuleMask.get(ts);
            if (b == null || !b.intersects(types)) {
               return false;
            }
         }

         return true;
      }

      private boolean checkNotFreeTags(BitSet types) {
         for(String ts : this.onlyNotTags) {
            BitSet b = GeneralRouter.this.tagRuleMask.get(ts);
            if (b != null && b.intersects(types)) {
               return false;
            }
         }

         return true;
      }

      private boolean checkAllTypesShouldNotBePresent(BitSet types) {
         return !this.filterNotTypes.intersects(types);
      }

      private boolean checkAllTypesShouldBePresent(BitSet types) {
         this.evalFilterTypes.or(this.filterTypes);
         this.evalFilterTypes.and(types);
         return this.evalFilterTypes.equals(this.filterTypes);
      }
   }

   public class RouteAttributeExpression {
      public static final int LESS_EXPRESSION = 1;
      public static final int GREAT_EXPRESSION = 2;
      public static final int EQUAL_EXPRESSION = 3;
      private String[] values;
      private int expressionType;
      private String valueType;
      private Number[] cacheValues;

      public RouteAttributeExpression(String[] vs, String valueType, int expressionId) {
         this.expressionType = expressionId;
         this.values = vs;
         if (vs.length < 2) {
            throw new IllegalStateException("Expression should have at least 2 arguments");
         } else {
            this.cacheValues = new Number[vs.length];
            this.valueType = valueType;

            for(int i = 0; i < vs.length; ++i) {
               if (!vs[i].startsWith("$") && !vs[i].startsWith(":")) {
                  Object o = GeneralRouter.this.parseValue(vs[i], valueType);
                  if (o instanceof Number) {
                     this.cacheValues[i] = (Number)o;
                  }
               }
            }
         }
      }

      public boolean matches(BitSet types, GeneralRouter.ParameterContext paramContext) {
         double f1 = this.calculateExprValue(0, types, paramContext);
         double f2 = this.calculateExprValue(1, types, paramContext);
         if (Double.isNaN(f1) || Double.isNaN(f2)) {
            return false;
         } else if (this.expressionType == 1) {
            return f1 <= f2;
         } else if (this.expressionType == 2) {
            return f1 >= f2;
         } else if (this.expressionType == 3) {
            return f1 == f2;
         } else {
            return false;
         }
      }

      private double calculateExprValue(int id, BitSet types, GeneralRouter.ParameterContext paramContext) {
         String value = this.values[id];
         Number cacheValue = this.cacheValues[id];
         if (cacheValue != null) {
            return cacheValue.doubleValue();
         } else {
            Object o = null;
            if (value instanceof String && value.toString().startsWith("$")) {
               BitSet mask = GeneralRouter.this.tagRuleMask.get(value.toString().substring(1));
               if (mask != null && mask.intersects(types)) {
                  BitSet findBit = new BitSet(mask.length());
                  findBit.or(mask);
                  findBit.and(types);
                  int v = findBit.nextSetBit(0);
                  o = GeneralRouter.this.parseValueFromTag(v, this.valueType);
               }
            } else {
               if (value instanceof String && value.equals(":incline")) {
                  return paramContext.incline;
               }

               if (value instanceof String && value.toString().startsWith(":")) {
                  String p = value.substring(1);
                  if (paramContext != null && paramContext.vars.containsKey(p)) {
                     o = GeneralRouter.this.parseValue(paramContext.vars.get(p), this.valueType);
                  }
               }
            }

            return o instanceof Number ? ((Number)o).doubleValue() : Double.NaN;
         }
      }
   }

   public static enum RouteDataObjectAttribute {
      ROAD_SPEED("speed"),
      ROAD_PRIORITIES("priority"),
      DESTINATION_PRIORITIES("destination_priority"),
      ACCESS("access"),
      OBSTACLES("obstacle_time"),
      ROUTING_OBSTACLES("obstacle"),
      ONEWAY("oneway"),
      PENALTY_TRANSITION("penalty_transition"),
      OBSTACLE_SRTM_ALT_SPEED("obstacle_srtm_alt_speed"),
      AREA("area");

      public final String nm;

      private RouteDataObjectAttribute(String name) {
         this.nm = name;
      }

      public static GeneralRouter.RouteDataObjectAttribute getValueOf(String s) {
         for(GeneralRouter.RouteDataObjectAttribute a : values()) {
            if (a.nm.equals(s)) {
               return a;
            }
         }

         return null;
      }
   }

   public static class RoutingParameter {
      private String id;
      private String group;
      private String name;
      private String description;
      private GeneralRouter.RoutingParameterType type;
      private Object[] possibleValues;
      private String[] possibleValueDescriptions;
      private String[] profiles;
      private boolean defaultBoolean;

      public String getId() {
         return this.id;
      }

      public String getGroup() {
         return this.group;
      }

      public String getName() {
         return this.name;
      }

      public String getDescription() {
         return this.description;
      }

      public GeneralRouter.RoutingParameterType getType() {
         return this.type;
      }

      public String[] getPossibleValueDescriptions() {
         return this.possibleValueDescriptions;
      }

      public Object[] getPossibleValues() {
         return this.possibleValues;
      }

      public boolean getDefaultBoolean() {
         return this.defaultBoolean;
      }

      public String getDefaultString() {
         return this.type == GeneralRouter.RoutingParameterType.NUMERIC ? "0.0" : "-";
      }

      public String[] getProfiles() {
         return this.profiles;
      }
   }

   public static enum RoutingParameterType {
      NUMERIC,
      BOOLEAN,
      SYMBOLIC;
   }
}
