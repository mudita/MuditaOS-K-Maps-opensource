package net.osmand.router;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TransportRoutingConfiguration {
   public int ZOOM_TO_LOAD_TILES = 15;
   public int walkRadius = 1500;
   public int walkChangeRadius = 300;
   public int maxNumberOfChanges = 3;
   public int finishTimeSeconds = 1200;
   public int maxRouteTime = 36000;
   public int maxRouteDistance = 0;
   public int maxRouteIncreaseSpeed = 30;
   public GeneralRouter router;
   public float walkSpeed = 1.0F;
   public float defaultTravelSpeed = 16.666666F;
   public int stopTime = 30;
   public int changeTime = 180;
   public int boardingTime = 180;
   public boolean useSchedule;
   public int scheduleTimeOfDay = 4320;
   public int scheduleMaxTime = 300;
   public int scheduleDayNumber;
   private Map<String, Integer> rawTypes = new HashMap<>();
   private Map<String, Float> speed = new TreeMap<>();

   public float getSpeedByRouteType(String routeType) {
      Float sl = this.speed.get(routeType);
      if (sl == null) {
         GeneralRouter.RouteAttributeContext spds = this.router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED);
         sl = spds.evaluateFloat(this.getRawBitset("route", routeType), this.defaultTravelSpeed);
         this.speed.put(routeType, sl);
      }

      return sl;
   }

   private int getRawType(String tg, String vl) {
      String key = tg + "$" + vl;
      if (!this.rawTypes.containsKey(key)) {
         int at = this.router.registerTagValueAttribute(tg, vl);
         this.rawTypes.put(key, at);
      }

      return this.rawTypes.get(key);
   }

   private BitSet getRawBitset(String tg, String vl) {
      BitSet bs = new BitSet();
      bs.set(this.getRawType(tg, vl));
      return bs;
   }

   public int getChangeTime() {
      return this.useSchedule ? 0 : this.changeTime;
   }

   public int getBoardingTime() {
      return this.boardingTime;
   }

   public TransportRoutingConfiguration(GeneralRouter prouter, Map<String, String> params) {
      if (prouter != null) {
         this.router = prouter.build(params);
         this.walkRadius = this.router.getIntAttribute("walkRadius", this.walkRadius);
         this.walkChangeRadius = this.router.getIntAttribute("walkChangeRadius", this.walkChangeRadius);
         this.ZOOM_TO_LOAD_TILES = this.router.getIntAttribute("zoomToLoadTiles", this.ZOOM_TO_LOAD_TILES);
         this.maxNumberOfChanges = this.router.getIntAttribute("maxNumberOfChanges", this.maxNumberOfChanges);
         this.maxRouteTime = this.router.getIntAttribute("maxRouteTime", this.maxRouteTime);
         this.maxRouteIncreaseSpeed = this.router.getIntAttribute("maxRouteIncreaseSpeed", this.maxRouteIncreaseSpeed);
         this.maxRouteDistance = this.router.getIntAttribute("maxRouteDistance", this.maxRouteDistance);
         this.finishTimeSeconds = this.router.getIntAttribute("delayForAlternativesRoutes", this.finishTimeSeconds);
         String mn = params.get("max_num_changes");
         this.maxNumberOfChanges = (int)RoutingConfiguration.parseSilentFloat(mn, (float)this.maxNumberOfChanges);
         this.walkSpeed = this.router.getFloatAttribute("minDefaultSpeed", this.walkSpeed * 3.6F) / 3.6F;
         this.defaultTravelSpeed = this.router.getFloatAttribute("maxDefaultSpeed", this.defaultTravelSpeed * 3.6F) / 3.6F;
         GeneralRouter.RouteAttributeContext obstacles = this.router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROUTING_OBSTACLES);
         this.stopTime = obstacles.evaluateInt(this.getRawBitset("time", "stop"), this.stopTime);
         this.changeTime = obstacles.evaluateInt(this.getRawBitset("time", "change"), this.changeTime);
         this.boardingTime = obstacles.evaluateInt(this.getRawBitset("time", "boarding"), this.boardingTime);
         GeneralRouter.RouteAttributeContext spds = this.router.getObjContext(GeneralRouter.RouteDataObjectAttribute.ROAD_SPEED);
         this.walkSpeed = spds.evaluateFloat(this.getRawBitset("route", "walk"), this.walkSpeed);
      }
   }
}
