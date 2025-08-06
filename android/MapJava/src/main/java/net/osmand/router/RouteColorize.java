package net.osmand.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

public class RouteColorize {
   public int zoom;
   public double[] latitudes;
   public double[] longitudes;
   public double[] values;
   public double minValue;
   public double maxValue;
   public double[][] palette;
   private List<RouteColorize.RouteColorizationPoint> dataList;
   public static final int DARK_GREY = rgbaToDecimal(92, 92, 92, 255);
   public static final int LIGHT_GREY = rgbaToDecimal(200, 200, 200, 255);
   public static final int GREEN = rgbaToDecimal(90, 220, 95, 255);
   public static final int YELLOW = rgbaToDecimal(212, 239, 50, 255);
   public static final int RED = rgbaToDecimal(243, 55, 77, 255);
   public static final int GREEN_SLOPE = rgbaToDecimal(46, 185, 0, 255);
   public static final int WHITE = rgbaToDecimal(255, 255, 255, 255);
   public static final int YELLOW_SLOPE = rgbaToDecimal(255, 222, 2, 255);
   public static final int RED_SLOPE = rgbaToDecimal(255, 1, 1, 255);
   public static final int PURPLE_SLOPE = rgbaToDecimal(130, 1, 255, 255);
   public static final int[] COLORS = new int[]{GREEN, YELLOW, RED};
   public static final int[] SLOPE_COLORS = new int[]{GREEN_SLOPE, WHITE, YELLOW_SLOPE, RED_SLOPE, PURPLE_SLOPE};
   public static final double SLOPE_MIN_VALUE = -0.25;
   public static final double SLOPE_MAX_VALUE = 1.0;
   public static final double[][] SLOPE_PALETTE = new double[][]{
      {-0.25, (double)GREEN_SLOPE}, {0.0, (double)WHITE}, {0.125, (double)YELLOW_SLOPE}, {0.25, (double)RED_SLOPE}, {1.0, (double)PURPLE_SLOPE}
   };
   private static final float DEFAULT_BASE = 17.2F;
   public static double MAX_CORRECT_ELEVATION_DISTANCE = 100.0;
   private final int VALUE_INDEX = 0;
   private final int DECIMAL_COLOR_INDEX = 1;
   private final int RED_COLOR_INDEX = 1;
   private final int GREEN_COLOR_INDEX = 2;
   private final int BLUE_COLOR_INDEX = 3;
   private final int ALPHA_COLOR_INDEX = 4;
   private RouteColorize.ColorizationType colorizationType;
   public static int SLOPE_RANGE = 150;
   private static final double MIN_DIFFERENCE_SLOPE = 0.05;
   private static final Log LOG = PlatformUtil.getLog(RouteColorize.class);

   public RouteColorize(int zoom, double[] latitudes, double[] longitudes, double[] values, double minValue, double maxValue, double[][] palette) {
      this.zoom = zoom;
      this.latitudes = latitudes;
      this.longitudes = longitudes;
      this.values = values;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.palette = palette;
      if (Double.isNaN(minValue) || Double.isNaN(maxValue)) {
         this.calculateMinMaxValue();
      }

      this.checkPalette();
      this.sortPalette();
   }

   public RouteColorize(int zoom, GPXUtilities.GPXFile gpxFile, RouteColorize.ColorizationType type) {
      this(zoom, gpxFile, null, type, 0.0F);
   }

   public RouteColorize(
      int zoom, GPXUtilities.GPXFile gpxFile, GPXUtilities.GPXTrackAnalysis analysis, RouteColorize.ColorizationType type, float maxProfileSpeed
   ) {
      if (!gpxFile.hasTrkPt()) {
         LOG.warn("GPX file is not consist of track points");
      } else {
         List<Double> latList = new ArrayList<>();
         List<Double> lonList = new ArrayList<>();
         List<Double> valList = new ArrayList<>();
         int wptIdx = 0;
         if (analysis == null) {
            analysis = Algorithms.isEmpty(gpxFile.path) ? gpxFile.getAnalysis(System.currentTimeMillis()) : gpxFile.getAnalysis(gpxFile.modifiedTime);
         }

         for(GPXUtilities.Track t : gpxFile.tracks) {
            for(GPXUtilities.TrkSegment ts : t.segments) {
               if (!ts.generalSegment && ts.points.size() >= 2) {
                  for(GPXUtilities.WptPt p : ts.points) {
                     latList.add(p.lat);
                     lonList.add(p.lon);
                     if (type == RouteColorize.ColorizationType.SPEED) {
                        valList.add((double)analysis.speedData.get(wptIdx).speed);
                     } else {
                        valList.add((double)analysis.elevationData.get(wptIdx).elevation);
                     }

                     ++wptIdx;
                  }
               }
            }
         }

         this.zoom = zoom;
         this.colorizationType = type;
         this.latitudes = this.listToArray(latList);
         this.longitudes = this.listToArray(lonList);
         if (type == RouteColorize.ColorizationType.SLOPE) {
            this.values = this.calculateSlopesByElevations(this.latitudes, this.longitudes, this.listToArray(valList), (double)SLOPE_RANGE);
         } else {
            this.values = this.listToArray(valList);
         }

         this.calculateMinMaxValue(analysis, maxProfileSpeed);
         this.checkPalette();
         this.sortPalette();
      }
   }

   public int getZoom() {
      return this.zoom;
   }

   public void setZoom(int zoom) {
      this.zoom = zoom;
   }

   public double[] calculateSlopesByElevations(double[] latitudes, double[] longitudes, double[] elevations, double slopeRange) {
      this.correctElevations(latitudes, longitudes, elevations);
      double[] newElevations = elevations;

      for(int i = 2; i < elevations.length - 2; ++i) {
         newElevations[i] = elevations[i - 2] + elevations[i - 1] + elevations[i] + elevations[i + 1] + elevations[i + 2];
         newElevations[i] /= 5.0;
      }

      elevations = newElevations;
      double[] slopes = new double[newElevations.length];
      if (latitudes.length == longitudes.length && latitudes.length == newElevations.length) {
         double[] distances = new double[newElevations.length];
         double totalDistance = 0.0;
         distances[0] = totalDistance;

         for(int i = 0; i < elevations.length - 1; ++i) {
            totalDistance += MapUtils.getDistance(latitudes[i], longitudes[i], latitudes[i + 1], longitudes[i + 1]);
            distances[i + 1] = totalDistance;
         }

         for(int i = 0; i < elevations.length; ++i) {
            if (!(distances[i] < slopeRange / 2.0) && !(distances[i] > totalDistance - slopeRange / 2.0)) {
               double[] arg = this.findDerivativeArguments(distances, elevations, i, slopeRange);
               slopes[i] = (arg[1] - arg[0]) / (arg[3] - arg[2]);
            } else {
               slopes[i] = Double.NaN;
            }
         }

         return slopes;
      } else {
         LOG.warn("Sizes of arrays latitudes, longitudes and values are not match");
         return slopes;
      }
   }

   private void correctElevations(double[] latitudes, double[] longitudes, double[] elevations) {
      for(int i = 0; i < elevations.length; ++i) {
         if (Double.isNaN(elevations[i])) {
            double leftDist = MAX_CORRECT_ELEVATION_DISTANCE;
            double rightDist = MAX_CORRECT_ELEVATION_DISTANCE;
            double leftElevation = Double.NaN;
            double rightElevation = Double.NaN;

            for(int left = i - 1; left > 0 && leftDist <= MAX_CORRECT_ELEVATION_DISTANCE; --left) {
               if (!Double.isNaN(elevations[left])) {
                  double dist = MapUtils.getDistance(latitudes[left], longitudes[left], latitudes[i], longitudes[i]);
                  if (!(dist < leftDist)) {
                     break;
                  }

                  leftDist = dist;
                  leftElevation = elevations[left];
               }
            }

            for(int right = i + 1; right < elevations.length && rightDist <= MAX_CORRECT_ELEVATION_DISTANCE; ++right) {
               if (!Double.isNaN(elevations[right])) {
                  double dist = MapUtils.getDistance(latitudes[right], longitudes[right], latitudes[i], longitudes[i]);
                  if (!(dist < rightDist)) {
                     break;
                  }

                  rightElevation = elevations[right];
                  rightDist = dist;
               }
            }

            if (!Double.isNaN(leftElevation) && !Double.isNaN(rightElevation)) {
               elevations[i] = (leftElevation + rightElevation) / 2.0;
            } else if (Double.isNaN(leftElevation) && !Double.isNaN(rightElevation)) {
               elevations[i] = rightElevation;
            } else if (!Double.isNaN(leftElevation) && Double.isNaN(rightElevation)) {
               elevations[i] = leftElevation;
            } else {
               for(int right = i + 1; right < elevations.length; ++right) {
                  if (!Double.isNaN(elevations[right])) {
                     elevations[i] = elevations[right];
                     break;
                  }
               }
            }
         }
      }
   }

   public List<RouteColorize.RouteColorizationPoint> getResult(boolean simplify) {
      List<RouteColorize.RouteColorizationPoint> result = new ArrayList<>();
      if (simplify) {
         result = this.simplify();
      } else {
         for(int i = 0; i < this.latitudes.length; ++i) {
            result.add(new RouteColorize.RouteColorizationPoint(i, this.latitudes[i], this.longitudes[i], this.values[i]));
         }
      }

      for(RouteColorize.RouteColorizationPoint data : result) {
         data.color = this.getColorByValue(data.val);
      }

      return result;
   }

   public int getColorByValue(double value) {
      if (Double.isNaN(value)) {
         return LIGHT_GREY;
      } else {
         for(int i = 0; i < this.palette.length - 1; ++i) {
            if (value == this.palette[i][0]) {
               return (int)this.palette[i][1];
            }

            if (value >= this.palette[i][0] && value <= this.palette[i + 1][0]) {
               int minPaletteColor = (int)this.palette[i][1];
               int maxPaletteColor = (int)this.palette[i + 1][1];
               double minPaletteValue = this.palette[i][0];
               double maxPaletteValue = this.palette[i + 1][0];
               double percent = (value - minPaletteValue) / (maxPaletteValue - minPaletteValue);
               return getIntermediateColor(minPaletteColor, maxPaletteColor, percent);
            }
         }

         if (value <= this.palette[0][0]) {
            return (int)this.palette[0][1];
         } else {
            return value >= this.palette[this.palette.length - 1][0] ? (int)this.palette[this.palette.length - 1][1] : this.getTransparentColor();
         }
      }
   }

   public void setPalette(double[][] palette) {
      this.palette = palette;
      this.checkPalette();
      this.sortPalette();
   }

   public void setPalette(int[] gradientPalette) {
      if (gradientPalette != null && gradientPalette.length == 3) {
         this.setPalette(
            new double[][]{
               {this.minValue, (double)gradientPalette[0]},
               {(this.minValue + this.maxValue) / 2.0, (double)gradientPalette[1]},
               {this.maxValue, (double)gradientPalette[2]}
            }
         );
      }
   }

   private int getTransparentColor() {
      return rgbaToDecimal(0, 0, 0, 0);
   }

   public List<RouteColorize.RouteColorizationPoint> simplify() {
      if (this.dataList == null) {
         this.dataList = new ArrayList<>();

         for(int i = 0; i < this.latitudes.length; ++i) {
            this.dataList.add(new RouteColorize.RouteColorizationPoint(i, this.latitudes[i], this.longitudes[i], this.values[i]));
         }
      }

      List<Node> nodes = new ArrayList<>();
      List<Node> result = new ArrayList<>();

      for(RouteColorize.RouteColorizationPoint data : this.dataList) {
         nodes.add(new Node(data.lat, data.lon, (long)data.id));
      }

      double epsilon = Math.pow(2.0, (double)(17.2F - (float)this.zoom));
      result.add(nodes.get(0));
      OsmMapUtils.simplifyDouglasPeucker(nodes, 0, nodes.size() - 1, result, epsilon);
      List<RouteColorize.RouteColorizationPoint> simplified = new ArrayList<>();

      for(int i = 1; i < result.size(); ++i) {
         int prevId = (int)result.get(i - 1).getId();
         int currentId = (int)result.get(i).getId();
         List<RouteColorize.RouteColorizationPoint> sublist = this.dataList.subList(prevId, currentId);
         simplified.addAll(this.getExtremums(sublist));
      }

      Node lastSurvivedPoint = result.get(result.size() - 1);
      simplified.add(this.dataList.get((int)lastSurvivedPoint.getId()));
      return simplified;
   }

   private List<RouteColorize.RouteColorizationPoint> getExtremums(List<RouteColorize.RouteColorizationPoint> subDataList) {
      if (subDataList.size() <= 2) {
         return subDataList;
      } else {
         List<RouteColorize.RouteColorizationPoint> result = new ArrayList<>();
         double max;
         double min = max = subDataList.get(0).val;

         for(RouteColorize.RouteColorizationPoint pt : subDataList) {
            if (min > pt.val) {
               min = pt.val;
            }

            if (max < pt.val) {
               max = pt.val;
            }
         }

         double diff = max - min;
         result.add(subDataList.get(0));

         for(int i = 1; i < subDataList.size() - 1; ++i) {
            double prev = subDataList.get(i - 1).val;
            double current = subDataList.get(i).val;
            double next = subDataList.get(i + 1).val;
            RouteColorize.RouteColorizationPoint currentData = subDataList.get(i);
            if (current > prev && current > next
               || current < prev && current < next
               || current < prev && current == next
               || current == prev && current < next
               || current > prev && current == next
               || current == prev && current > next) {
               if (result.size() > 0) {
                  RouteColorize.RouteColorizationPoint prevInResult = result.get(0);
                  if (prevInResult.val / diff > 0.05) {
                     result.add(currentData);
                  }
               } else {
                  result.add(currentData);
               }
            }
         }

         result.add(subDataList.get(subDataList.size() - 1));
         return result;
      }
   }

   private void checkPalette() {
      if (this.palette == null || this.palette.length < 2 || this.palette[0].length < 2 || this.palette[1].length < 2) {
         LOG.info("Will use default palette");
         this.palette = this.getDefaultPalette(this.colorizationType);
      }

      double min;
      double max = min = this.palette[0][0];
      int minIndex = 0;
      int maxIndex = 0;
      double[][] sRGBPalette = new double[this.palette.length][2];

      for(int i = 0; i < this.palette.length; ++i) {
         double[] p = this.palette[i];
         if (p.length == 2) {
            sRGBPalette[i] = p;
         } else if (p.length == 4) {
            int color = rgbaToDecimal((int)p[1], (int)p[2], (int)p[3], 255);
            sRGBPalette[i] = new double[]{p[0], (double)color};
         } else if (p.length >= 5) {
            int color = rgbaToDecimal((int)p[1], (int)p[2], (int)p[3], (int)p[4]);
            sRGBPalette[i] = new double[]{p[0], (double)color};
         }

         if (p[0] > max) {
            max = p[0];
            maxIndex = i;
         }

         if (p[0] < min) {
            min = p[0];
            minIndex = i;
         }
      }

      this.palette = sRGBPalette;
      if (this.minValue < min) {
         this.palette[minIndex][0] = this.minValue;
      }

      if (this.maxValue > max) {
         this.palette[maxIndex][0] = this.maxValue;
      }
   }

   private void sortPalette() {
      Arrays.sort(this.palette, new Comparator<double[]>() {
         public int compare(double[] a, double[] b) {
            return Double.compare(a[0], b[0]);
         }
      });
   }

   private double[] findDerivativeArguments(double[] distances, double[] elevations, int index, double slopeRange) {
      double[] result = new double[4];
      double minDist = distances[index] - slopeRange / 2.0;
      double maxDist = distances[index] + slopeRange / 2.0;
      result[0] = Double.NaN;
      result[1] = Double.NaN;
      result[2] = minDist;
      result[3] = maxDist;
      int closestMaxIndex = -1;
      int closestMinIndex = -1;

      for(int i = index; i < distances.length; ++i) {
         if (distances[i] == maxDist) {
            result[1] = elevations[i];
            break;
         }

         if (distances[i] > maxDist) {
            closestMaxIndex = i;
            break;
         }
      }

      for(int i = index; i >= 0; --i) {
         if (distances[i] == minDist) {
            result[0] = elevations[i];
            break;
         }

         if (distances[i] < minDist) {
            closestMinIndex = i;
            break;
         }
      }

      if (closestMaxIndex > 0) {
         double diff = distances[closestMaxIndex] - distances[closestMaxIndex - 1];
         double coef = (maxDist - distances[closestMaxIndex - 1]) / diff;
         if (coef > 1.0 || coef < 0.0) {
            LOG.warn("Coefficient fo max must be 0..1 , coef=" + coef);
         }

         result[1] = (1.0 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex];
      }

      if (closestMinIndex >= 0) {
         double diff = distances[closestMinIndex + 1] - distances[closestMinIndex];
         double coef = (minDist - distances[closestMinIndex]) / diff;
         if (coef > 1.0 || coef < 0.0) {
            LOG.warn("Coefficient for min must be 0..1 , coef=" + coef);
         }

         result[0] = (1.0 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1];
      }

      if (Double.isNaN(result[0]) || Double.isNaN(result[1])) {
         LOG.warn("Elevations wasn't calculated");
      }

      return result;
   }

   public static double getMinValue(RouteColorize.ColorizationType type, GPXUtilities.GPXTrackAnalysis analysis) {
      switch(type) {
         case SPEED:
            return 0.0;
         case ELEVATION:
            return analysis.minElevation;
         case SLOPE:
            return -0.25;
         default:
            return -1.0;
      }
   }

   public static double getMaxValue(RouteColorize.ColorizationType type, GPXUtilities.GPXTrackAnalysis analysis, double minValue, double maxProfileSpeed) {
      switch(type) {
         case SPEED:
            return Math.max((double)analysis.maxSpeed, maxProfileSpeed);
         case ELEVATION:
            return Math.max(analysis.maxElevation, minValue + 50.0);
         case SLOPE:
            return 1.0;
         default:
            return -1.0;
      }
   }

   public static int getIntermediateColor(int minPaletteColor, int maxPaletteColor, double percent) {
      double resultRed = (double)getRed(minPaletteColor) + percent * (double)(getRed(maxPaletteColor) - getRed(minPaletteColor));
      double resultGreen = (double)getGreen(minPaletteColor) + percent * (double)(getGreen(maxPaletteColor) - getGreen(minPaletteColor));
      double resultBlue = (double)getBlue(minPaletteColor) + percent * (double)(getBlue(maxPaletteColor) - getBlue(minPaletteColor));
      double resultAlpha = (double)getAlpha(minPaletteColor) + percent * (double)(getAlpha(maxPaletteColor) - getAlpha(minPaletteColor));
      return rgbaToDecimal((int)resultRed, (int)resultGreen, (int)resultBlue, (int)resultAlpha);
   }

   private void calculateMinMaxValue() {
      if (this.values.length != 0) {
         this.minValue = this.maxValue = Double.NaN;

         for(double value : this.values) {
            if ((Double.isNaN(this.maxValue) || Double.isNaN(this.minValue)) && !Double.isNaN(value)) {
               this.maxValue = this.minValue = value;
            }

            if (this.minValue > value) {
               this.minValue = value;
            }

            if (this.maxValue < value) {
               this.maxValue = value;
            }
         }
      }
   }

   private void calculateMinMaxValue(GPXUtilities.GPXTrackAnalysis analysis, float maxProfileSpeed) {
      this.calculateMinMaxValue();
      this.maxValue = getMaxValue(this.colorizationType, analysis, this.minValue, (double)maxProfileSpeed);
   }

   private double[] listToArray(List<Double> doubleList) {
      double[] result = new double[doubleList.size()];

      for(int i = 0; i < doubleList.size(); ++i) {
         result[i] = doubleList.get(i);
      }

      return result;
   }

   private double[][] getDefaultPalette(RouteColorize.ColorizationType colorizationType) {
      return colorizationType == RouteColorize.ColorizationType.SLOPE
         ? SLOPE_PALETTE
         : new double[][]{{this.minValue, (double)GREEN}, {(this.minValue + this.maxValue) / 2.0, (double)YELLOW}, {this.maxValue, (double)RED}};
   }

   private static int rgbaToDecimal(int r, int g, int b, int a) {
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF) << 0;
   }

   private static int getRed(int value) {
      return value >> 16 & 0xFF;
   }

   private static int getGreen(int value) {
      return value >> 8 & 0xFF;
   }

   private static int getBlue(int value) {
      return value >> 0 & 0xFF;
   }

   private static int getAlpha(int value) {
      return value >> 24 & 0xFF;
   }

   public static enum ColorizationType {
      ELEVATION,
      SPEED,
      SLOPE,
      NONE;
   }

   public static class RouteColorizationPoint {
      public int id;
      public double lat;
      public double lon;
      public double val;
      public int color;

      public RouteColorizationPoint(int id, double lat, double lon, double val) {
         this.id = id;
         this.lat = lat;
         this.lon = lon;
         this.val = val;
      }
   }
}
