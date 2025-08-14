package net.osmand.binary;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.osmand.ResultMatcher;
import net.osmand.util.MapUtils;

public class BinaryMapIndexFilter {
   private final BinaryMapIndexReader reader;

   public BinaryMapIndexFilter(File file) throws IOException {
      this.reader = new BinaryMapIndexReader(new RandomAccessFile(file.getPath(), "r"), file);
   }

   private double calculateArea(BinaryMapDataObject o, int zoom) {
      double sum = 0.0;

      for(int i = 0; i < o.getPointsLength(); ++i) {
         double x = MapUtils.getTileNumberX((float)(zoom + 8), MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
         int prev = i == 0 ? o.getPointsLength() - 1 : i - 1;
         int next = i == o.getPointsLength() - 1 ? 0 : i + 1;
         double y1 = MapUtils.getTileNumberY((float)(zoom + 8), MapUtils.get31LatitudeY(o.getPoint31YTile(prev)));
         double y2 = MapUtils.getTileNumberY((float)(zoom + 8), MapUtils.get31LatitudeY(o.getPoint31YTile(next)));
         sum += x * (y1 - y2);
      }

      return Math.abs(sum);
   }

   private double calculateLength(BinaryMapDataObject o, int zoom) {
      double sum = 0.0;

      for(int i = 1; i < o.getPointsLength(); ++i) {
         double x = MapUtils.getTileNumberX((float)(zoom + 8), MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
         double y = MapUtils.getTileNumberY((float)(zoom + 8), MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
         double x2 = MapUtils.getTileNumberX((float)(zoom + 8), MapUtils.get31LongitudeX(o.getPoint31XTile(i - 1)));
         double y2 = MapUtils.getTileNumberY((float)(zoom + 8), MapUtils.get31LatitudeY(o.getPoint31YTile(i - 1)));
         sum += Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
      }

      return Math.abs(sum);
   }

   private int tilesCovers(BinaryMapDataObject o, int zoom, TIntHashSet set) {
      set.clear();

      for(int i = 0; i < o.getPointsLength(); ++i) {
         int x = (int)MapUtils.getTileNumberX((float)zoom, MapUtils.get31LongitudeX(o.getPoint31XTile(i)));
         int y = (int)MapUtils.getTileNumberY((float)zoom, MapUtils.get31LatitudeY(o.getPoint31YTile(i)));
         int val = x << 16 | y;
         set.add(val);
      }

      return set.size();
   }

   private BinaryMapIndexFilter.Stat process(final int zoom) throws IOException {
      final BinaryMapIndexFilter.Stat stat = new BinaryMapIndexFilter.Stat();
      final Map<BinaryMapIndexReader.TagValuePair, Integer> map = new HashMap<>();
      BinaryMapIndexReader.SearchFilter sf = new BinaryMapIndexReader.SearchFilter() {
         @Override
         public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
            boolean polygon = false;
            boolean polyline = false;

            for(int j = 0; j < types.size(); ++j) {
               int wholeType = types.get(j);
               BinaryMapIndexReader.TagValuePair pair = index.decodeType(wholeType);
               if (pair != null) {
                  int t = wholeType & 3;
                  if (t == 1) {
                     ++stat.pointCount;
                  } else if (t == 2) {
                     ++stat.wayCount;
                     polyline = true;
                  } else {
                     polygon = true;
                     ++stat.polygonCount;
                     if (!map.containsKey(pair)) {
                        map.put(pair, 0);
                     }

                     map.put(pair, map.get(pair) + 1);
                  }
               }
            }

            ++stat.totalCount;
            return polyline;
         }
      };
      ResultMatcher<BinaryMapDataObject> matcher = new ResultMatcher<BinaryMapDataObject>() {
         TIntHashSet set = new TIntHashSet();

         @Override
         public boolean isCancelled() {
            return false;
         }

         public boolean publish(BinaryMapDataObject object) {
            double len = BinaryMapIndexFilter.this.calculateLength(object, zoom);
            if (len > 100.0) {
               ++stat.polygonBigSize;
               if (stat.polygonBigSize % 10000 == 0) {
                  return true;
               }
            }

            return false;
         }
      };
      BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(
         0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, zoom, sf, matcher
      );
      List<BinaryMapDataObject> result = this.reader.searchMapIndex(req);
      ArrayList<BinaryMapIndexReader.TagValuePair> list = new ArrayList<>(map.keySet());
      Collections.sort(list, new Comparator<BinaryMapIndexReader.TagValuePair>() {
         public int compare(BinaryMapIndexReader.TagValuePair o1, BinaryMapIndexReader.TagValuePair o2) {
            return -map.get(o1) + map.get(o2);
         }
      });

      for(BinaryMapIndexReader.TagValuePair tp : list) {
         Integer i = map.get(tp);
         if (i > 10) {
         }
      }

      for(BinaryMapDataObject obj : result) {
         System.out.println("id " + (obj.getId() >> 3) + " " + this.calculateArea(obj, zoom));
      }

      return stat;
   }

   public static void main(String[] iargs) throws IOException {
      BinaryMapIndexFilter filter = new BinaryMapIndexFilter(new File(""));

      for(int i = 10; i <= 14; ++i) {
         BinaryMapIndexFilter.Stat st = filter.process(i);
         System.out.println(i + " zoom -> " + st);
      }
   }

   private static class Stat {
      int pointCount = 0;
      int totalCount = 0;
      int wayCount = 0;
      int polygonCount = 0;
      int polygonBigSize = 0;

      private Stat() {
      }

      @Override
      public String toString() {
         return " ways "
            + this.wayCount
            + " polygons "
            + this.polygonCount
            + " points "
            + this.pointCount
            + " total "
            + this.totalCount
            + "\n polygons big size "
            + this.polygonBigSize;
      }
   }
}
