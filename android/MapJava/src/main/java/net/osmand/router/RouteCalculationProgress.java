package net.osmand.router;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RouteCalculationProgress {
   public int segmentNotFound = -1;
   public float distanceFromBegin;
   public float directDistance;
   public int directSegmentQueueSize;
   public float distanceFromEnd;
   public int reverseSegmentQueueSize;
   public float reverseDistance;
   public float totalEstimatedDistance = 0.0F;
   public float totalApproximateDistance = 0.0F;
   public float approximatedDistance;
   public float routingCalculatedTime = 0.0F;
   public int visitedSegments = 0;
   public int visitedDirectSegments = 0;
   public int visitedOppositeSegments = 0;
   public int directQueueSize = 0;
   public int oppositeQueueSize = 0;
   public int totalIterations = 1;
   public int iteration = -1;
   public long timeNanoToCalcDeviation = 0L;
   public long timeToLoad = 0L;
   public long timeToLoadHeaders = 0L;
   public long timeToFindInitialSegments = 0L;
   public long timeToCalculate = 0L;
   public int distinctLoadedTiles = 0;
   public int maxLoadedTiles = 0;
   public int loadedPrevUnloadedTiles = 0;
   public int unloadedTiles = 0;
   public int loadedTiles = 0;
   public boolean isCancelled;
   public boolean requestPrivateAccessRouting;
   public long routeCalculationStartTime;
   public List<String> missingMaps;
   private static final float INITIAL_PROGRESS = 0.05F;
   private static final float FIRST_ITERATION = 0.72F;

   public static RouteCalculationProgress capture(RouteCalculationProgress cp) {
      RouteCalculationProgress p = new RouteCalculationProgress();
      p.timeNanoToCalcDeviation = cp.timeNanoToCalcDeviation;
      p.timeToCalculate = cp.timeToCalculate;
      p.timeToLoadHeaders = cp.timeToLoadHeaders;
      p.timeToFindInitialSegments = cp.timeToFindInitialSegments;
      p.timeToLoad = cp.timeToLoad;
      p.visitedSegments = cp.visitedSegments;
      p.directQueueSize = cp.directQueueSize;
      p.reverseSegmentQueueSize = cp.reverseSegmentQueueSize;
      p.visitedDirectSegments = cp.visitedDirectSegments;
      p.visitedOppositeSegments = cp.visitedOppositeSegments;
      p.loadedTiles = cp.loadedTiles;
      p.distinctLoadedTiles = cp.distinctLoadedTiles;
      p.maxLoadedTiles = cp.maxLoadedTiles;
      p.loadedPrevUnloadedTiles = cp.loadedPrevUnloadedTiles;
      cp.maxLoadedTiles = 0;
      return p;
   }

   public Map<String, Object> getInfo(RouteCalculationProgress firstPhase) {
      TreeMap<String, Object> map = new TreeMap<>();
      TreeMap<String, Object> tiles = new TreeMap<>();
      if (firstPhase == null) {
         firstPhase = new RouteCalculationProgress();
      }

      map.put("tiles", tiles);
      tiles.put("loadedTiles", this.loadedTiles - firstPhase.loadedTiles);
      tiles.put("loadedTilesDistinct", this.distinctLoadedTiles - firstPhase.distinctLoadedTiles);
      tiles.put("loadedTilesPrevUnloaded", this.loadedPrevUnloadedTiles - firstPhase.loadedPrevUnloadedTiles);
      tiles.put("loadedTilesMax", Math.max(this.maxLoadedTiles, this.distinctLoadedTiles));
      tiles.put("unloadedTiles", this.unloadedTiles - firstPhase.unloadedTiles);
      Map<String, Object> segms = new LinkedHashMap<>();
      map.put("segments", segms);
      segms.put("visited", this.visitedSegments - firstPhase.visitedSegments);
      segms.put("queueDirectSize", this.directQueueSize - firstPhase.directQueueSize);
      segms.put("queueOppositeSize", this.reverseSegmentQueueSize - firstPhase.reverseSegmentQueueSize);
      segms.put("visitedDirectPoints", this.visitedDirectSegments - firstPhase.visitedDirectSegments);
      segms.put("visitedOppositePoints", this.visitedOppositeSegments - -firstPhase.visitedOppositeSegments);
      Map<String, Object> time = new LinkedHashMap<>();
      map.put("time", time);
      float timeToCalc = (float)((double)(this.timeToCalculate - firstPhase.timeToCalculate) / 1.0E9);
      time.put("timeToCalculate", timeToCalc);
      float timeToLoad = (float)((double)(this.timeToLoad - firstPhase.timeToLoad) / 1.0E9);
      time.put("timeToLoad", timeToLoad);
      float timeToLoadHeaders = (float)((double)(this.timeToLoadHeaders - firstPhase.timeToLoadHeaders) / 1.0E9);
      time.put("timeToLoadHeaders", timeToLoadHeaders);
      float timeToFindInitialSegments = (float)((double)(this.timeToFindInitialSegments - firstPhase.timeToFindInitialSegments) / 1.0E9);
      time.put("timeToFindInitialSegments", timeToFindInitialSegments);
      float timeExtra = (float)((double)(this.timeNanoToCalcDeviation - firstPhase.timeNanoToCalcDeviation) / 1.0E9);
      time.put("timeExtra", timeExtra);
      Map<String, Object> metrics = new LinkedHashMap<>();
      map.put("metrics", metrics);
      if (timeToLoad + timeToLoadHeaders > 0.0F) {
         metrics.put("tilesPerSec", (float)(this.loadedTiles - firstPhase.loadedTiles) / (timeToLoad + timeToLoadHeaders));
      }

      float pureTime = timeToCalc - (timeToLoad + timeToLoadHeaders + timeToFindInitialSegments);
      if (pureTime > 0.0F) {
         metrics.put("segmentsPerSec", (float)(this.visitedSegments - firstPhase.visitedSegments) / pureTime);
      } else {
         metrics.put("segmentsPerSec", 0.0F);
      }

      return map;
   }

   public float getLinearProgress() {
      float p = Math.max(this.distanceFromBegin, this.distanceFromEnd);
      float all = this.totalEstimatedDistance * 1.35F;
      float pr = 0.0F;
      if (all > 0.0F) {
         pr = Math.min(p * p / (all * all), 1.0F);
      }

      float progress = 0.05F;
      if (this.totalIterations <= 1) {
         progress = 0.05F + pr * 0.95F;
      } else if (this.totalIterations <= 2) {
         if (this.iteration < 1) {
            progress = pr * 0.72F + 0.05F;
         } else {
            progress = 0.77000004F + pr * 0.22999997F;
         }
      } else {
         progress = (float)(((double)this.iteration + Math.min((double)pr, 0.7)) / (double)this.totalIterations);
      }

      return Math.min(progress * 100.0F, 99.0F);
   }

   public float getApproximationProgress() {
      float progress = 0.0F;
      if (this.totalApproximateDistance > 0.0F) {
         progress = this.approximatedDistance / this.totalApproximateDistance;
      }

      progress = 0.05F + progress * 0.95F;
      return Math.min(progress * 100.0F, 99.0F);
   }

   public void nextIteration() {
      ++this.iteration;
      this.totalEstimatedDistance = 0.0F;
   }
}
