package net.osmand.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

public class RouteStatisticsHelper {
   public static final String UNDEFINED_ATTR = "undefined";
   public static final String ROUTE_INFO_PREFIX = "routeInfo_";
   private static final double H_STEP = 5.0;
   private static final double H_SLOPE_APPROX = 100.0;
   private static final int MIN_INCLINE = -101;
   private static final int MIN_DIVIDED_INCLINE = -20;
   private static final int MAX_INCLINE = 100;
   private static final int MAX_DIVIDED_INCLINE = 20;
   private static final int STEP = 4;
   private static final int[] BOUNDARIES_ARRAY;
   private static final String[] BOUNDARIES_CLASS;
   private static final String ROUTE_INFO_STEEPNESS = "routeInfo_steepness";

   public static List<RouteStatisticsHelper.RouteStatistics> calculateRouteStatistic(
      List<RouteSegmentResult> route,
      RenderingRulesStorage currentRenderer,
      RenderingRulesStorage defaultRenderer,
      RenderingRuleSearchRequest currentSearchRequest,
      RenderingRuleSearchRequest defaultSearchRequest
   ) {
      return calculateRouteStatistic(route, null, currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest);
   }

   public static List<RouteStatisticsHelper.RouteStatistics> calculateRouteStatistic(
      List<RouteSegmentResult> route,
      List<String> attributesNames,
      RenderingRulesStorage currentRenderer,
      RenderingRulesStorage defaultRenderer,
      RenderingRuleSearchRequest currentSearchRequest,
      RenderingRuleSearchRequest defaultSearchRequest
   ) {
      if (route == null) {
         return Collections.emptyList();
      } else {
         List<RouteStatisticsHelper.RouteSegmentWithIncline> routeSegmentWithInclines = calculateInclineRouteSegments(route);
         List<RouteStatisticsHelper.RouteStatistics> result = new ArrayList<>();
         if (Algorithms.isEmpty(attributesNames)) {
            attributesNames = getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, false);
         }

         for(String attr : attributesNames) {
            RouteStatisticsHelper.RouteStatisticComputer statisticComputer = new RouteStatisticsHelper.RouteStatisticComputer(
               currentRenderer, defaultRenderer, currentSearchRequest, defaultSearchRequest
            );
            RouteStatisticsHelper.RouteStatistics routeStatistics = statisticComputer.computeStatistic(routeSegmentWithInclines, attr);
            Map<String, RouteStatisticsHelper.RouteSegmentAttribute> partitions = routeStatistics.partition;
            if (!partitions.isEmpty() && (partitions.size() != 1 || !routeStatistics.partition.containsKey("undefined"))) {
               result.add(routeStatistics);
            }
         }

         return result;
      }
   }

   public static List<String> getRouteStatisticAttrsNames(
      RenderingRulesStorage currentRenderer, RenderingRulesStorage defaultRenderer, boolean excludeSteepness
   ) {
      List<String> attributeNames = new ArrayList<>();
      if (currentRenderer != null) {
         for(String s : currentRenderer.getRenderingAttributeNames()) {
            if (s.startsWith("routeInfo_")) {
               attributeNames.add(s);
            }
         }
      }

      if (attributeNames.isEmpty()) {
         for(String s : defaultRenderer.getRenderingAttributeNames()) {
            if (s.startsWith("routeInfo_")) {
               attributeNames.add(s);
            }
         }
      }

      if (excludeSteepness) {
         attributeNames.remove("routeInfo_steepness");
      }

      return attributeNames;
   }

   private static List<RouteStatisticsHelper.RouteSegmentWithIncline> calculateInclineRouteSegments(List<RouteSegmentResult> route) {
      List<RouteStatisticsHelper.RouteSegmentWithIncline> input = new ArrayList<>();
      float prevHeight = 0.0F;
      int totalArrayHeightsLength = 0;

      for(RouteSegmentResult r : route) {
         float[] heightValues = r.getHeightValues();
         RouteStatisticsHelper.RouteSegmentWithIncline incl = new RouteStatisticsHelper.RouteSegmentWithIncline();
         incl.dist = r.getDistance();
         incl.obj = r.getObject();
         input.add(incl);
         float prevH = prevHeight;
         int indStep = 0;
         if ((double)incl.dist > 5.0) {
            incl.interpolatedHeightByStep = new float[(int)((double)incl.dist / 5.0) + 1];
            totalArrayHeightsLength += incl.interpolatedHeightByStep.length;
         }

         if (heightValues != null && heightValues.length > 0) {
            int indH = 2;
            float distCum = 0.0F;
            prevH = heightValues[1];
            incl.h = prevH;
            if (incl.interpolatedHeightByStep != null && incl.interpolatedHeightByStep.length > indStep) {
               incl.interpolatedHeightByStep[indStep++] = prevH;
            }

            while(incl.interpolatedHeightByStep != null && indStep < incl.interpolatedHeightByStep.length && indH < heightValues.length) {
               float dist = heightValues[indH] + distCum;
               if ((double)dist > (double)indStep * 5.0) {
                  if (dist == distCum) {
                     incl.interpolatedHeightByStep[indStep] = prevH;
                  } else {
                     incl.interpolatedHeightByStep[indStep] = (float)(
                        (double)prevH + ((double)indStep * 5.0 - (double)distCum) * (double)(heightValues[indH + 1] - prevH) / (double)(dist - distCum)
                     );
                  }

                  ++indStep;
               } else {
                  distCum = dist;
                  prevH = heightValues[indH + 1];
                  indH += 2;
               }
            }
         } else {
            incl.h = prevHeight;
         }

         while(incl.interpolatedHeightByStep != null && indStep < incl.interpolatedHeightByStep.length) {
            incl.interpolatedHeightByStep[indStep++] = prevH;
         }

         prevHeight = prevH;
      }

      int slopeSmoothShift = 10;
      float[] heightArray = new float[totalArrayHeightsLength];
      int iter = 0;

      for(int i = 0; i < input.size(); ++i) {
         RouteStatisticsHelper.RouteSegmentWithIncline rswi = input.get(i);

         for(int k = 0; rswi.interpolatedHeightByStep != null && k < rswi.interpolatedHeightByStep.length; ++k) {
            heightArray[iter++] = rswi.interpolatedHeightByStep[k];
         }
      }

      iter = 0;
      int minSlope = Integer.MAX_VALUE;
      int maxSlope = Integer.MIN_VALUE;

      for(int i = 0; i < input.size(); ++i) {
         RouteStatisticsHelper.RouteSegmentWithIncline rswi = input.get(i);
         if (rswi.interpolatedHeightByStep != null) {
            rswi.slopeByStep = new float[rswi.interpolatedHeightByStep.length];

            for(int k = 0; k < rswi.slopeByStep.length; ++k) {
               if (iter > slopeSmoothShift && iter + slopeSmoothShift < heightArray.length) {
                  double slope = (double)((heightArray[iter + slopeSmoothShift] - heightArray[iter - slopeSmoothShift]) * 100.0F) / 100.0;
                  rswi.slopeByStep[k] = (float)slope;
                  minSlope = Math.min((int)slope, minSlope);
                  maxSlope = Math.max((int)slope, maxSlope);
               }

               ++iter;
            }
         }
      }

      String[] classFormattedStrings = new String[BOUNDARIES_ARRAY.length];
      classFormattedStrings[0] = formatSlopeString(minSlope, -20);
      classFormattedStrings[1] = formatSlopeString(minSlope, -20);
      classFormattedStrings[BOUNDARIES_ARRAY.length - 1] = formatSlopeString(20, maxSlope);

      for(int k = 2; k < BOUNDARIES_ARRAY.length - 1; ++k) {
         classFormattedStrings[k] = formatSlopeString(BOUNDARIES_ARRAY[k - 1], BOUNDARIES_ARRAY[k]);
      }

      for(int i = 0; i < input.size(); ++i) {
         RouteStatisticsHelper.RouteSegmentWithIncline rswi = input.get(i);
         if (rswi.slopeByStep != null) {
            rswi.slopeClass = new int[rswi.slopeByStep.length];
            rswi.slopeClassUserString = new String[rswi.slopeByStep.length];

            for(int t = 0; t < rswi.slopeClass.length; ++t) {
               for(int k = 0; k < BOUNDARIES_ARRAY.length; ++k) {
                  if (rswi.slopeByStep[t] <= (float)BOUNDARIES_ARRAY[k] || k == BOUNDARIES_ARRAY.length - 1) {
                     rswi.slopeClass[t] = k;
                     rswi.slopeClassUserString[t] = classFormattedStrings[k];
                     break;
                  }
               }
            }
         }
      }

      return input;
   }

   private static String formatSlopeString(int slope, int next) {
      return String.format("%d%% .. %d%%", slope, next);
   }

   static {
      int NUM = 13;
      BOUNDARIES_ARRAY = new int[NUM];
      BOUNDARIES_CLASS = new String[NUM];
      BOUNDARIES_ARRAY[0] = -101;
      BOUNDARIES_CLASS[0] = "steepness=-100_-20";

      for(int i = 1; i < NUM - 1; ++i) {
         BOUNDARIES_ARRAY[i] = -20 + (i - 1) * 4;
         BOUNDARIES_CLASS[i] = "steepness=" + (BOUNDARIES_ARRAY[i - 1] + 1) + "_" + BOUNDARIES_ARRAY[i];
      }

      BOUNDARIES_ARRAY[NUM - 1] = 100;
      BOUNDARIES_CLASS[NUM - 1] = "steepness=20_100";
   }

   public static class RouteSegmentAttribute {
      private final int color;
      private final String propertyName;
      private final int slopeIndex;
      private float distance;
      private String userPropertyName;

      RouteSegmentAttribute(String propertyName, int color, int slopeIndex) {
         this.propertyName = propertyName == null ? "undefined" : propertyName;
         this.slopeIndex = slopeIndex >= 0 && RouteStatisticsHelper.BOUNDARIES_CLASS[slopeIndex].endsWith(this.propertyName) ? slopeIndex : -1;
         this.color = color;
      }

      RouteSegmentAttribute(RouteStatisticsHelper.RouteSegmentAttribute segmentAttribute) {
         this.propertyName = segmentAttribute.getPropertyName();
         this.color = segmentAttribute.getColor();
         this.slopeIndex = segmentAttribute.slopeIndex;
         this.userPropertyName = segmentAttribute.userPropertyName;
      }

      public String getUserPropertyName() {
         return this.userPropertyName == null ? this.propertyName : this.userPropertyName;
      }

      public void setUserPropertyName(String userPropertyName) {
         this.userPropertyName = userPropertyName;
      }

      public float getDistance() {
         return this.distance;
      }

      public void incrementDistanceBy(float distance) {
         this.distance += distance;
      }

      public String getPropertyName() {
         return this.propertyName;
      }

      public int getColor() {
         return this.color;
      }

      @Override
      public String toString() {
         return String.format("%s - %.0f m %d", this.getUserPropertyName(), this.getDistance(), this.getColor());
      }
   }

   private static class RouteSegmentWithIncline {
      RouteDataObject obj;
      float dist;
      float h;
      float[] interpolatedHeightByStep;
      float[] slopeByStep;
      String[] slopeClassUserString;
      int[] slopeClass;

      private RouteSegmentWithIncline() {
      }
   }

   public static class RouteStatisticComputer {
      final RenderingRulesStorage currentRenderer;
      final RenderingRulesStorage defaultRenderer;
      final RenderingRuleSearchRequest currentRenderingRuleSearchRequest;
      final RenderingRuleSearchRequest defaultRenderingRuleSearchRequest;

      public RouteStatisticComputer(
         RenderingRulesStorage currentRenderer,
         RenderingRulesStorage defaultRenderer,
         RenderingRuleSearchRequest currentRenderingRuleSearchRequest,
         RenderingRuleSearchRequest defaultRenderingRuleSearchRequest
      ) {
         this.currentRenderer = currentRenderer;
         this.defaultRenderer = defaultRenderer;
         this.currentRenderingRuleSearchRequest = currentRenderingRuleSearchRequest;
         this.defaultRenderingRuleSearchRequest = defaultRenderingRuleSearchRequest;
      }

      public RouteStatisticsHelper.RouteStatistics computeStatistic(List<RouteStatisticsHelper.RouteSegmentWithIncline> route, String attribute) {
         List<RouteStatisticsHelper.RouteSegmentAttribute> routeAttributes = this.processRoute(route, attribute);
         Map<String, RouteStatisticsHelper.RouteSegmentAttribute> partition = this.makePartition(routeAttributes);
         float totalDistance = this.computeTotalDistance(routeAttributes);
         return new RouteStatisticsHelper.RouteStatistics(attribute, routeAttributes, partition, totalDistance);
      }

      Map<String, RouteStatisticsHelper.RouteSegmentAttribute> makePartition(List<RouteStatisticsHelper.RouteSegmentAttribute> routeAttributes) {
         final Map<String, RouteStatisticsHelper.RouteSegmentAttribute> partition = new TreeMap<>();

         for(RouteStatisticsHelper.RouteSegmentAttribute attribute : routeAttributes) {
            RouteStatisticsHelper.RouteSegmentAttribute attr = partition.get(attribute.getUserPropertyName());
            if (attr == null) {
               attr = new RouteStatisticsHelper.RouteSegmentAttribute(attribute);
               partition.put(attribute.getUserPropertyName(), attr);
            }

            attr.incrementDistanceBy(attribute.getDistance());
         }

         List<String> keys = new ArrayList<>(partition.keySet());
         Collections.sort(keys, new Comparator<String>() {
            public int compare(String o1, String o2) {
               if (o1.equalsIgnoreCase("undefined")) {
                  return 1;
               } else if (o2.equalsIgnoreCase("undefined")) {
                  return -1;
               } else {
                  int cmp = Integer.compare(partition.get(o1).slopeIndex, partition.get(o2).slopeIndex);
                  return cmp != 0 ? cmp : -Float.compare(partition.get(o1).getDistance(), partition.get(o2).getDistance());
               }
            }
         });
         Map<String, RouteStatisticsHelper.RouteSegmentAttribute> sorted = new LinkedHashMap<>();

         for(String k : keys) {
            sorted.put(k, partition.get(k));
         }

         return sorted;
      }

      private float computeTotalDistance(List<RouteStatisticsHelper.RouteSegmentAttribute> attributes) {
         float distance = 0.0F;

         for(RouteStatisticsHelper.RouteSegmentAttribute attribute : attributes) {
            distance += attribute.getDistance();
         }

         return distance;
      }

      protected List<RouteStatisticsHelper.RouteSegmentAttribute> processRoute(List<RouteStatisticsHelper.RouteSegmentWithIncline> route, String attribute) {
         List<RouteStatisticsHelper.RouteSegmentAttribute> routes = new ArrayList<>();
         RouteStatisticsHelper.RouteSegmentAttribute prev = null;

         for(RouteStatisticsHelper.RouteSegmentWithIncline segment : route) {
            if (segment.slopeClass != null && segment.slopeClass.length != 0) {
               for(int i = 0; i < segment.slopeClass.length; ++i) {
                  float d = (float)(i == 0 ? (double)segment.dist - 5.0 * (double)(segment.slopeClass.length - 1) : 5.0);
                  if (i > 0 && segment.slopeClass[i] == segment.slopeClass[i - 1]) {
                     prev.incrementDistanceBy(d);
                  } else {
                     RouteStatisticsHelper.RouteSegmentAttribute current = this.classifySegment(attribute, segment.slopeClass[i], segment.obj);
                     current.distance = d;
                     if (prev != null && prev.getPropertyName() != null && prev.getPropertyName().equals(current.getPropertyName())) {
                        prev.incrementDistanceBy(current.distance);
                     } else {
                        if (current.slopeIndex == segment.slopeClass[i]) {
                           current.setUserPropertyName(segment.slopeClassUserString[i]);
                        }

                        routes.add(current);
                        prev = current;
                     }
                  }
               }
            } else {
               RouteStatisticsHelper.RouteSegmentAttribute current = this.classifySegment(attribute, -1, segment.obj);
               current.distance = segment.dist;
               if (prev != null && prev.getPropertyName() != null && prev.getPropertyName().equals(current.getPropertyName())) {
                  prev.incrementDistanceBy(current.distance);
               } else {
                  routes.add(current);
                  prev = current;
               }
            }
         }

         return routes;
      }

      public RouteStatisticsHelper.RouteSegmentAttribute classifySegment(String attribute, int slopeClass, RouteDataObject routeObject) {
         RouteStatisticsHelper.RouteSegmentAttribute res = new RouteStatisticsHelper.RouteSegmentAttribute("undefined", 0, -1);
         RenderingRuleSearchRequest currentRequest = this.currentRenderer == null
            ? null
            : new RenderingRuleSearchRequest(this.currentRenderingRuleSearchRequest);
         if (this.currentRenderer != null && this.searchRenderingAttribute(attribute, this.currentRenderer, currentRequest, routeObject, slopeClass)) {
            res = new RouteStatisticsHelper.RouteSegmentAttribute(
               currentRequest.getStringPropertyValue(this.currentRenderer.PROPS.R_ATTR_STRING_VALUE),
               currentRequest.getIntPropertyValue(this.currentRenderer.PROPS.R_ATTR_COLOR_VALUE),
               slopeClass
            );
         } else {
            RenderingRuleSearchRequest defaultRequest = new RenderingRuleSearchRequest(this.defaultRenderingRuleSearchRequest);
            if (this.searchRenderingAttribute(attribute, this.defaultRenderer, defaultRequest, routeObject, slopeClass)) {
               res = new RouteStatisticsHelper.RouteSegmentAttribute(
                  defaultRequest.getStringPropertyValue(this.defaultRenderer.PROPS.R_ATTR_STRING_VALUE),
                  defaultRequest.getIntPropertyValue(this.defaultRenderer.PROPS.R_ATTR_COLOR_VALUE),
                  slopeClass
               );
            }
         }

         return res;
      }

      protected boolean searchRenderingAttribute(
         String attribute, RenderingRulesStorage rrs, RenderingRuleSearchRequest req, RouteDataObject routeObject, int slopeClass
      ) {
         boolean mainTagAdded = false;
         StringBuilder additional = new StringBuilder(slopeClass >= 0 ? RouteStatisticsHelper.BOUNDARIES_CLASS[slopeClass] + ";" : "");

         for(int type : routeObject.getTypes()) {
            BinaryMapRouteReaderAdapter.RouteTypeRule tp = routeObject.region.quickGetEncodingRule(type);
            if (!tp.getTag().equals("highway")
               && !tp.getTag().equals("route")
               && !tp.getTag().equals("railway")
               && !tp.getTag().equals("aeroway")
               && !tp.getTag().equals("aerialway")
               && !tp.getTag().equals("piste:type")) {
               additional.append(tp.getTag()).append("=").append(tp.getValue()).append(";");
            } else if (!mainTagAdded) {
               req.setStringFilter(rrs.PROPS.R_TAG, tp.getTag());
               req.setStringFilter(rrs.PROPS.R_VALUE, tp.getValue());
               mainTagAdded = true;
            }
         }

         req.setStringFilter(rrs.PROPS.R_ADDITIONAL, additional.toString());
         return req.searchRenderingAttribute(attribute);
      }
   }

   public static class RouteStatistics {
      public final List<RouteStatisticsHelper.RouteSegmentAttribute> elements;
      public final Map<String, RouteStatisticsHelper.RouteSegmentAttribute> partition;
      public final float totalDistance;
      public final String name;

      private RouteStatistics(
         String name,
         List<RouteStatisticsHelper.RouteSegmentAttribute> elements,
         Map<String, RouteStatisticsHelper.RouteSegmentAttribute> partition,
         float totalDistance
      ) {
         this.name = name.startsWith("routeInfo_") ? name.substring("routeInfo_".length()) : name;
         this.elements = elements;
         this.partition = partition;
         this.totalDistance = totalDistance;
      }

      @Override
      public String toString() {
         StringBuilder s = new StringBuilder("Statistic '").append(this.name).append("':");

         for(RouteStatisticsHelper.RouteSegmentAttribute a : this.elements) {
            s.append(String.format(" %.0fm %s,", a.distance, a.getUserPropertyName()));
         }

         s.append("\n  Partition: ").append(this.partition);
         return s.toString();
      }
   }
}
