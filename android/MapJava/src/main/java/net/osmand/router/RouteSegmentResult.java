package net.osmand.router;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringExternalizable;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TIntObjectHashMap;

public class RouteSegmentResult implements StringExternalizable<RouteDataBundle> {
   public static final float DIST_BEARING_DETECT = 15.0F;
   public static final float DIST_BEARING_DETECT_UNMATCHED = 50.0F;
   private RouteDataObject object;
   private int startPointIndex;
   private int endPointIndex;
   private List<RouteSegmentResult>[] attachedRoutes;
   private RouteSegmentResult[][] preAttachedRoutes;
   private float segmentTime;
   private float routingTime;
   private float speed;
   private float distance;
   private String description = "";
   private boolean isIntermediatePoint = false;
   private TurnType turnType;
   private static final float DIST_TO_SEEK_STREET_NAME = 150.0F;
   private static final float DIST_TO_SEEK_DEST = 1000.0F;

   public RouteSegmentResult(RouteDataObject object) {
      this.object = object;
   }

   public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex) {
      this.object = object;
      this.startPointIndex = startPointIndex;
      this.endPointIndex = endPointIndex;
      this.updateCapacity();
   }

   public RouteSegmentResult(
      RouteDataObject object,
      int startPointIndex,
      int endPointIndex,
      RouteSegmentResult[][] preAttachedRoutes,
      float segmentTime,
      float routingTime,
      float speed,
      float distance,
      TurnType turnType
   ) {
      this.object = object;
      this.startPointIndex = startPointIndex;
      this.endPointIndex = endPointIndex;
      this.preAttachedRoutes = preAttachedRoutes;
      this.segmentTime = segmentTime;
      this.routingTime = routingTime;
      this.speed = speed;
      this.distance = distance;
      this.turnType = turnType;
      this.updateCapacity();
   }

   public void collectTypes(RouteDataResources resources) {
      Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules = resources.getRules();
      if (this.object.types != null) {
         this.collectRules(rules, this.object.types);
      }

      if (this.object.pointTypes != null) {
         int start = Math.min(this.startPointIndex, this.endPointIndex);
         int end = Math.max(this.startPointIndex, this.endPointIndex);

         for(int i = start; i <= end && i < this.object.pointTypes.length; ++i) {
            int[] types = this.object.pointTypes[i];
            if (types != null) {
               this.collectRules(rules, types);
            }
         }
      }
   }

   public void collectNames(RouteDataResources resources) {
      Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules = resources.getRules();
      BinaryMapRouteReaderAdapter.RouteRegion region = this.object.region;
      if (region.getNameTypeRule() != -1) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = region.quickGetEncodingRule(region.getNameTypeRule());
         if (!rules.containsKey(r)) {
            rules.put(r, rules.size());
         }
      }

      if (region.getRefTypeRule() != -1) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = region.quickGetEncodingRule(region.getRefTypeRule());
         if (!rules.containsKey(r)) {
            rules.put(r, rules.size());
         }
      }

      if (this.object.nameIds != null) {
         for(int nameId : this.object.nameIds) {
            String name = (String)this.object.names.get(nameId);
            String tag = region.quickGetEncodingRule(nameId).getTag();
            BinaryMapRouteReaderAdapter.RouteTypeRule r = new BinaryMapRouteReaderAdapter.RouteTypeRule(tag, name);
            if (!rules.containsKey(r)) {
               rules.put(r, rules.size());
            }
         }
      }

      if (this.object.pointNameTypes != null) {
         int start = Math.min(this.startPointIndex, this.endPointIndex);
         int end = Math.min(Math.max(this.startPointIndex, this.endPointIndex) + 1, this.object.pointNameTypes.length);

         for(int i = start; i < end; ++i) {
            int[] types = this.object.pointNameTypes[i];
            if (types != null) {
               for(int type : types) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule r = region.quickGetEncodingRule(type);
                  if (!rules.containsKey(r)) {
                     rules.put(r, rules.size());
                  }
               }
            }
         }
      }
   }

   private void collectRules(Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules, int[] types) {
      BinaryMapRouteReaderAdapter.RouteRegion region = this.object.region;

      for(int type : types) {
         BinaryMapRouteReaderAdapter.RouteTypeRule rule = region.quickGetEncodingRule(type);
         String tag = rule.getTag();
         if (!tag.equals("osmand_ele_start")
            && !tag.equals("osmand_ele_end")
            && !tag.equals("osmand_ele_asc")
            && !tag.equals("osmand_ele_desc")
            && !rules.containsKey(rule)) {
            rules.put(rule, rules.size());
         }
      }
   }

   private int[] convertTypes(int[] types, Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules) {
      if (types != null && types.length != 0) {
         List<Integer> arr = new ArrayList<>();

         for(int i = 0; i < types.length; ++i) {
            int type = types[i];
            BinaryMapRouteReaderAdapter.RouteTypeRule rule = this.object.region.quickGetEncodingRule(type);
            Integer ruleId = rules.get(rule);
            if (ruleId != null) {
               arr.add(ruleId);
            }
         }

         int[] res = new int[arr.size()];

         for(int i = 0; i < arr.size(); ++i) {
            res[i] = arr.get(i);
         }

         return res;
      } else {
         return null;
      }
   }

   private int[][] convertTypes(int[][] types, Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules) {
      if (types != null && types.length != 0) {
         int[][] res = new int[types.length][];

         for(int i = 0; i < types.length; ++i) {
            int[] typesArr = types[i];
            if (typesArr != null) {
               res[i] = this.convertTypes(typesArr, rules);
            }
         }

         return res;
      } else {
         return null;
      }
   }

   private int[] convertNameIds(int[] nameIds, Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules) {
      if (nameIds != null && nameIds.length != 0) {
         int[] res = new int[nameIds.length];

         for(int i = 0; i < nameIds.length; ++i) {
            int nameId = nameIds[i];
            String name = (String)this.object.names.get(nameId);
            String tag = this.object.region.quickGetEncodingRule(nameId).getTag();
            BinaryMapRouteReaderAdapter.RouteTypeRule rule = new BinaryMapRouteReaderAdapter.RouteTypeRule(tag, name);
            Integer ruleId = rules.get(rule);
            if (ruleId == null) {
               throw new IllegalArgumentException("Cannot find collected rule: " + rule.toString());
            }

            res[i] = ruleId;
         }

         return res;
      } else {
         return null;
      }
   }

   private int[][] convertPointNames(int[][] nameTypes, String[][] pointNames, Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules) {
      if (nameTypes != null && nameTypes.length != 0) {
         int[][] res = new int[nameTypes.length][];

         for(int i = 0; i < nameTypes.length; ++i) {
            int[] types = nameTypes[i];
            if (types != null) {
               int[] arr = new int[types.length];

               for(int k = 0; k < types.length; ++k) {
                  int type = types[k];
                  String tag = this.object.region.quickGetEncodingRule(type).getTag();
                  String name = pointNames[i][k];
                  BinaryMapRouteReaderAdapter.RouteTypeRule rule = new BinaryMapRouteReaderAdapter.RouteTypeRule(tag, name);
                  Integer ruleId = rules.get(rule);
                  if (ruleId == null) {
                     ruleId = rules.size();
                     rules.put(rule, ruleId);
                  }

                  arr[k] = ruleId;
               }

               res[i] = arr;
            }
         }

         return res;
      } else {
         return null;
      }
   }

   public void fillNames(RouteDataResources resources) {
      if (this.object.nameIds != null && this.object.nameIds.length > 0) {
         BinaryMapRouteReaderAdapter.RouteRegion region = this.object.region;
         int nameTypeRule = region.getNameTypeRule();
         int refTypeRule = region.getRefTypeRule();
         this.object.names = new TIntObjectHashMap();

         for(int nameId : this.object.nameIds) {
            BinaryMapRouteReaderAdapter.RouteTypeRule rule = region.quickGetEncodingRule(nameId);
            if (rule != null) {
               if (nameTypeRule != -1 && "name".equals(rule.getTag())) {
                  nameId = nameTypeRule;
               } else if (refTypeRule != -1 && "ref".equals(rule.getTag())) {
                  nameId = refTypeRule;
               }

               this.object.names.put(nameId, rule.getValue());
            }
         }
      }

      String[][] pointNames = null;
      int[][] pointNameTypes = null;
      int[][] pointNamesArr = (int[][])resources.getPointNamesMap().get(this.object);
      if (pointNamesArr != null) {
         pointNames = new String[pointNamesArr.length][];
         pointNameTypes = new int[pointNamesArr.length][];

         for(int i = 0; i < pointNamesArr.length; ++i) {
            int[] namesIds = pointNamesArr[i];
            if (namesIds != null) {
               pointNames[i] = new String[namesIds.length];
               pointNameTypes[i] = new int[namesIds.length];

               for(int k = 0; k < namesIds.length; ++k) {
                  int id = namesIds[k];
                  BinaryMapRouteReaderAdapter.RouteTypeRule r = this.object.region.quickGetEncodingRule(id);
                  if (r != null) {
                     pointNames[i][k] = r.getValue();
                     int nameType = this.object.region.searchRouteEncodingRule(r.getTag(), null);
                     if (nameType != -1) {
                        pointNameTypes[i][k] = nameType;
                     }
                  }
               }
            }
         }
      }

      this.object.pointNames = pointNames;
      this.object.pointNameTypes = pointNameTypes;
   }

   public void writeToBundle(RouteDataBundle bundle) {
      RouteDataResources resources = bundle.getResources();
      Map<BinaryMapRouteReaderAdapter.RouteTypeRule, Integer> rules = resources.getRules();
      boolean reversed = this.endPointIndex < this.startPointIndex;
      int length = Math.abs(this.endPointIndex - this.startPointIndex) + 1;
      bundle.putInt("length", length);
      bundle.putInt("startTrkptIdx", resources.getCurrentSegmentStartLocationIndex());
      bundle.putFloat("segmentTime", this.segmentTime, 2);
      bundle.putFloat("speed", this.speed, 2);
      if (this.turnType != null) {
         bundle.putString("turnType", this.turnType.toXmlString());
         if (this.turnType.isSkipToSpeak()) {
            bundle.putBoolean("skipTurn", this.turnType.isSkipToSpeak());
         }

         if (this.turnType.getTurnAngle() != 0.0F) {
            bundle.putFloat("turnAngle", this.turnType.getTurnAngle(), 2);
         }

         int[] turnLanes = this.turnType.getLanes();
         if (turnLanes != null && turnLanes.length > 0) {
            bundle.putString("turnLanes", TurnType.lanesToString(turnLanes));
         }
      }

      bundle.putLong("id", this.object.id >> 6);
      bundle.putArray("types", this.convertTypes(this.object.types, rules));
      int start = Math.min(this.startPointIndex, this.endPointIndex);
      int end = Math.max(this.startPointIndex, this.endPointIndex) + 1;
      if (this.object.pointTypes != null && start < this.object.pointTypes.length) {
         int[][] types = (int[][])Arrays.copyOfRange((int[][])this.object.pointTypes, start, Math.min(end, this.object.pointTypes.length));
         if (reversed) {
            Algorithms.reverseArray(types);
         }

         bundle.putArray("pointTypes", this.convertTypes(types, rules));
      }

      if (this.object.nameIds != null) {
         bundle.putArray("names", this.convertNameIds(this.object.nameIds, rules));
      }

      if (this.object.pointNameTypes != null && start < this.object.pointNameTypes.length && this.object.pointNames != null) {
         int[][] types = (int[][])Arrays.copyOfRange((int[][])this.object.pointNameTypes, start, Math.min(end, this.object.pointNameTypes.length));
         String[][] names = (String[][])Arrays.copyOfRange(this.object.pointNames, start, Math.min(end, this.object.pointNames.length));
         if (reversed) {
            Algorithms.reverseArray(types);
            Algorithms.reverseArray(names);
         }

         bundle.putArray("pointNames", this.convertPointNames(types, names, rules));
      }

      resources.updateNextSegmentStartLocation(length);
   }

   public void readFromBundle(RouteDataBundle bundle) {
      int length = bundle.getInt("length", 0);
      boolean plus = length >= 0;
      length = Math.abs(length);
      this.startPointIndex = plus ? 0 : length - 1;
      this.endPointIndex = plus ? length - 1 : 0;
      this.segmentTime = bundle.getFloat("segmentTime", this.segmentTime);
      this.speed = bundle.getFloat("speed", this.speed);
      String turnTypeStr = bundle.getString("turnType", null);
      if (!Algorithms.isEmpty(turnTypeStr)) {
         this.turnType = TurnType.fromString(turnTypeStr, false);
         this.turnType.setSkipToSpeak(bundle.getBoolean("skipTurn", this.turnType.isSkipToSpeak()));
         this.turnType.setTurnAngle(bundle.getFloat("turnAngle", this.turnType.getTurnAngle()));
         int[] turnLanes = TurnType.lanesFromString(bundle.getString("turnLanes", null));
         this.turnType.setLanes(turnLanes);
      }

      this.object.id = bundle.getLong("id", this.object.id) << 6;
      this.object.types = bundle.getIntArray("types", null);
      this.object.pointTypes = bundle.getIntIntArray("pointTypes", null);
      this.object.nameIds = bundle.getIntArray("names", null);
      int[][] pointNames = bundle.getIntIntArray("pointNames", null);
      if (pointNames != null) {
         bundle.getResources().getPointNamesMap().put(this.object, pointNames);
      }

      RouteDataResources resources = bundle.getResources();
      this.object.pointsX = new int[length];
      this.object.pointsY = new int[length];
      this.object.heightDistanceArray = new float[length * 2];
      int index = plus ? 0 : length - 1;
      float distance = 0.0F;
      Location prevLocation = null;

      for(int i = 0; i < length; ++i) {
         Location location = resources.getCurrentSegmentLocation(index);
         double dist = 0.0;
         if (prevLocation != null) {
            dist = MapUtils.getDistance(prevLocation.getLatitude(), prevLocation.getLongitude(), location.getLatitude(), location.getLongitude());
            distance = (float)((double)distance + dist);
         }

         prevLocation = location;
         this.object.pointsX[i] = MapUtils.get31TileNumberX(location.getLongitude());
         this.object.pointsY[i] = MapUtils.get31TileNumberY(location.getLatitude());
         if (location.hasAltitude() && this.object.heightDistanceArray.length > 0) {
            this.object.heightDistanceArray[i * 2] = (float)dist;
            this.object.heightDistanceArray[i * 2 + 1] = (float)location.getAltitude();
         } else {
            this.object.heightDistanceArray = new float[0];
         }

         if (plus) {
            ++index;
         } else {
            --index;
         }
      }

      this.distance = distance;
      resources.updateNextSegmentStartLocation(length);
   }

   public float[] getHeightValues() {
      float[] pf = this.object.calculateHeightArray();
      if (pf != null && pf.length != 0) {
         boolean reverse = this.startPointIndex > this.endPointIndex;
         int st = Math.min(this.startPointIndex, this.endPointIndex);
         int end = Math.max(this.startPointIndex, this.endPointIndex);
         float[] res = new float[(end - st + 1) * 2];
         if (reverse) {
            for(int k = 1; k <= res.length / 2; ++k) {
               int ind = 2 * end--;
               if (ind < pf.length && k < res.length / 2) {
                  res[2 * k] = pf[ind];
               }

               if (ind < pf.length) {
                  res[2 * (k - 1) + 1] = pf[ind + 1];
               }
            }
         } else {
            for(int k = 0; k < res.length / 2; ++k) {
               int ind = 2 * (st + k);
               if (k > 0 && ind < pf.length) {
                  res[2 * k] = pf[ind];
               }

               if (ind < pf.length) {
                  res[2 * k + 1] = pf[ind + 1];
               }
            }
         }

         return res;
      } else {
         return new float[0];
      }
   }

   private void updateCapacity() {
      int capacity = Math.abs(this.endPointIndex - this.startPointIndex) + 1;
      List<RouteSegmentResult>[] old = this.attachedRoutes;
      this.attachedRoutes = new List[capacity];
      if (old != null) {
         System.arraycopy(old, 0, this.attachedRoutes, 0, Math.min(old.length, this.attachedRoutes.length));
      }
   }

   public void attachRoute(int roadIndex, RouteSegmentResult r) {
      if (!r.getObject().isRoadDeleted()) {
         int st = Math.abs(roadIndex - this.startPointIndex);
         if (this.attachedRoutes[st] == null) {
            this.attachedRoutes[st] = new ArrayList<>();
         }

         this.attachedRoutes[st].add(r);
      }
   }

   public void copyPreattachedRoutes(RouteSegmentResult toCopy, int shift) {
      if (toCopy.preAttachedRoutes != null) {
         int l = toCopy.preAttachedRoutes.length - shift;
         this.preAttachedRoutes = new RouteSegmentResult[l][];
         System.arraycopy(toCopy.preAttachedRoutes, shift, this.preAttachedRoutes, 0, l);
      }
   }

   public RouteSegmentResult[] getPreAttachedRoutes(int routeInd) {
      int st = Math.abs(routeInd - this.startPointIndex);
      return this.preAttachedRoutes != null && st < this.preAttachedRoutes.length ? this.preAttachedRoutes[st] : null;
   }

   public List<RouteSegmentResult> getAttachedRoutes(int routeInd) {
      int st = Math.abs(routeInd - this.startPointIndex);
      List<RouteSegmentResult> list = this.attachedRoutes[st];
      return list == null ? Collections.<RouteSegmentResult>emptyList() : list;
   }

   public TurnType getTurnType() {
      return this.turnType;
   }

   public void setTurnType(TurnType turnType) {
      this.turnType = turnType;
   }

   public RouteDataObject getObject() {
      return this.object;
   }

   public float getSegmentTime() {
      return this.segmentTime;
   }

   public float getBearingBegin() {
      return this.getBearingBegin(this.startPointIndex, this.distance > 0.0F && this.distance < 15.0F ? this.distance : 15.0F);
   }

   public float getBearingBegin(int point, float dist) {
      return this.getBearing(point, true, dist);
   }

   public float getBearingEnd() {
      return this.getBearingEnd(this.endPointIndex, this.distance > 0.0F && this.distance < 15.0F ? this.distance : 15.0F);
   }

   public float getBearingEnd(int point, float dist) {
      return this.getBearing(point, false, dist);
   }

   public float getBearing(int point, boolean begin, float dist) {
      if (begin) {
         return (float)(this.object.directionRoute(point, this.startPointIndex < this.endPointIndex, dist) / Math.PI * 180.0);
      } else {
         double dr = this.object.directionRoute(point, this.startPointIndex > this.endPointIndex, dist);
         return (float)(MapUtils.alignAngleDifference(dr - Math.PI) / Math.PI * 180.0);
      }
   }

   public float getDistance(int point, boolean plus) {
      return (float)(plus ? this.object.distance(point, this.endPointIndex) : this.object.distance(this.startPointIndex, point));
   }

   public void setSegmentTime(float segmentTime) {
      this.segmentTime = segmentTime;
   }

   public void setRoutingTime(float routingTime) {
      this.routingTime = routingTime;
   }

   public float getRoutingTime() {
      return this.routingTime;
   }

   public LatLon getStartPoint() {
      return this.convertPoint(this.object, this.startPointIndex);
   }

   public int getStartPointIndex() {
      return this.startPointIndex;
   }

   public int getEndPointIndex() {
      return this.endPointIndex;
   }

   public LatLon getPoint(int i) {
      return this.convertPoint(this.object, i);
   }

   public LatLon getEndPoint() {
      return this.convertPoint(this.object, this.endPointIndex);
   }

   public boolean continuesBeyondRouteSegment(RouteSegmentResult segment) {
      boolean commonX = this.object.pointsX[this.startPointIndex] == segment.object.pointsX[segment.endPointIndex];
      boolean commonY = this.object.pointsY[this.startPointIndex] == segment.object.pointsY[segment.endPointIndex];
      return commonX && commonY;
   }

   public boolean isForwardDirection() {
      return this.endPointIndex - this.startPointIndex > 0;
   }

   private LatLon convertPoint(RouteDataObject o, int ind) {
      return new LatLon(MapUtils.get31LatitudeY(o.getPoint31YTile(ind)), MapUtils.get31LongitudeX(o.getPoint31XTile(ind)));
   }

   public void setSegmentSpeed(float speed) {
      this.speed = speed;
   }

   public void setEndPointIndex(int endPointIndex) {
      this.endPointIndex = endPointIndex;
      this.updateCapacity();
   }

   public void setStartPointIndex(int startPointIndex) {
      this.startPointIndex = startPointIndex;
      this.updateCapacity();
   }

   public float getSegmentSpeed() {
      return this.speed;
   }

   public float getDistance() {
      return this.distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public boolean isIntermediatePoint() {
      return isIntermediatePoint;
   }

   public void setIntermediatePoint(boolean isIntermediatePoint) {
      this.isIntermediatePoint = isIntermediatePoint;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setObject(RouteDataObject r) {
      this.object = r;
   }

   @Override
   public String toString() {
      return this.object.toString() + ": " + this.startPointIndex + "-" + this.endPointIndex;
   }

   public String getDestinationName(String lang, boolean transliterate, List<RouteSegmentResult> list, int routeInd) {
      String dnRef = this.getObject().getDestinationRef(lang, transliterate, this.isForwardDirection());
      String destinationName = this.getObject().getDestinationName(lang, transliterate, this.isForwardDirection());
      if (Algorithms.isEmpty(destinationName)) {
         float distanceFromTurn = this.getDistance();

         for(int n = routeInd + 1; n + 1 < list.size(); ++n) {
            RouteSegmentResult s1 = list.get(n);
            String s1DnRef = s1.getObject().getDestinationRef(lang, transliterate, this.isForwardDirection());
            boolean dnRefIsEqual = !Algorithms.isEmpty(s1DnRef) && !Algorithms.isEmpty(dnRef) && s1DnRef.equals(dnRef);
            boolean isMotorwayLink = "motorway_link".equals(s1.getObject().getHighway());
            if (distanceFromTurn < 1000.0F && (isMotorwayLink || dnRefIsEqual) && Algorithms.isEmpty(destinationName)) {
               destinationName = s1.getObject().getDestinationName(lang, transliterate, s1.isForwardDirection());
            }

            distanceFromTurn += s1.getDistance();
            if (distanceFromTurn > 1000.0F || !Algorithms.isEmpty(destinationName)) {
               break;
            }
         }
      }

      if (!Algorithms.isEmpty(dnRef) && !Algorithms.isEmpty(destinationName)) {
         destinationName = dnRef + ", " + destinationName;
      } else if (!Algorithms.isEmpty(dnRef) && Algorithms.isEmpty(destinationName)) {
         destinationName = dnRef;
      }

      return destinationName;
   }

   public String getStreetName(String lang, boolean transliterate, List<RouteSegmentResult> list, int routeInd) {
      String streetName = this.getObject().getName(lang, transliterate);
      if (Algorithms.isEmpty(streetName)) {
         float distanceFromTurn = this.getDistance();
         boolean hasNewTurn = false;

         for(int n = routeInd + 1; n + 1 < list.size(); ++n) {
            RouteSegmentResult s1 = list.get(n);
            if (s1.getTurnType() != null) {
               hasNewTurn = true;
            }

            if (!hasNewTurn && distanceFromTurn < 150.0F && Algorithms.isEmpty(streetName)) {
               streetName = s1.getObject().getName(lang, transliterate);
            }

            distanceFromTurn += s1.getDistance();
            if (distanceFromTurn > 150.0F || !Algorithms.isEmpty(streetName)) {
               break;
            }
         }
      }

      return streetName;
   }

   public String getRef(String lang, boolean transliterate) {
      return this.getObject().getRef(lang, transliterate, this.isForwardDirection());
   }

   public RouteDataObject getObjectWithShield(List<RouteSegmentResult> list, int routeInd) {
      RouteDataObject rdo = null;
      boolean isNextShieldFound = this.getObject().hasNameTagStartsWith("road_ref");

      for(int ind = routeInd; ind < list.size() && !isNextShieldFound; ++ind) {
         if (list.get(ind).getTurnType() != null) {
            isNextShieldFound = true;
         } else {
            RouteDataObject obj = list.get(ind).getObject();
            if (obj.hasNameTagStartsWith("road_ref")) {
               rdo = obj;
               isNextShieldFound = true;
            }
         }
      }

      return rdo;
   }
}
