package net.osmand.binary;

import net.osmand.util.Algorithms;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class BinaryMapDataObject {
   protected int[] coordinates = null;
   protected int[][] polygonInnerCoordinates = null;
   protected boolean area = false;
   protected int[] types = null;
   protected int[] additionalTypes = null;
   protected int objectType = 1;
   protected int labelX;
   protected int labelY;
   private static final int SHIFT_ID = 7;
   protected TIntObjectHashMap<String> objectNames = null;
   protected TIntArrayList namesOrder = null;
   protected long id = 0L;
   protected BinaryMapIndexReader.MapIndex mapIndex = null;

   public BinaryMapDataObject() {
   }

   public BinaryMapDataObject(
      long id, int[] coordinates, int[][] polygonInnerCoordinates, int objectType, boolean area, int[] types, int[] additionalTypes, int labelX, int labelY
   ) {
      this.polygonInnerCoordinates = polygonInnerCoordinates;
      this.coordinates = coordinates;
      this.additionalTypes = additionalTypes;
      this.types = types;
      this.id = id;
      this.objectType = objectType;
      this.area = area;
      this.labelX = labelX;
      this.labelY = labelY;
   }

   protected void setCoordinates(int[] coordinates) {
      this.coordinates = coordinates;
   }

   public String getName() {
      if (this.objectNames == null) {
         return "";
      } else {
         String name = (String)this.objectNames.get(this.mapIndex.nameEncodingType);
         return name == null ? "" : name;
      }
   }

   public TIntObjectHashMap<String> getObjectNames() {
      return this.objectNames;
   }

   public Map<Integer, String> getOrderedObjectNames() {
      if (this.namesOrder == null) {
         return null;
      } else {
         LinkedHashMap<Integer, String> lm = new LinkedHashMap<>();

         for(int i = 0; i < this.namesOrder.size(); ++i) {
            int nm = this.namesOrder.get(i);
            lm.put(Integer.valueOf(nm), (String)this.objectNames.get(nm));
         }

         return lm;
      }
   }

   public void putObjectName(int type, String name) {
      if (this.objectNames == null) {
         this.objectNames = new TIntObjectHashMap();
         this.namesOrder = new TIntArrayList();
      }

      this.objectNames.put(type, name);
      this.namesOrder.add(type);
   }

   public int[][] getPolygonInnerCoordinates() {
      return this.polygonInnerCoordinates;
   }

   public int[] getTypes() {
      return this.types;
   }

   public boolean containsType(int cachedType) {
      if (cachedType != -1) {
         for(int i = 0; i < this.types.length; ++i) {
            if (this.types[i] == cachedType) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean containsAdditionalType(int cachedType) {
      if (cachedType != -1 && this.additionalTypes != null) {
         for(int i = 0; i < this.additionalTypes.length; ++i) {
            if (this.additionalTypes[i] == cachedType) {
               return true;
            }
         }
      }

      return false;
   }

   public String getNameByType(int type) {
      return type != -1 && this.objectNames != null ? (String)this.objectNames.get(type) : null;
   }

   public int[] getAdditionalTypes() {
      return this.additionalTypes;
   }

   public boolean isArea() {
      return this.area;
   }

   public boolean isCycle() {
      if (this.coordinates != null && this.coordinates.length >= 2) {
         return this.coordinates[0] == this.coordinates[this.coordinates.length - 2] && this.coordinates[1] == this.coordinates[this.coordinates.length - 1];
      } else {
         return false;
      }
   }

   public void setArea(boolean area) {
      this.area = area;
   }

   public long getId() {
      return this.id;
   }

   protected void setId(long id) {
      this.id = id;
   }

   protected void setTypes(int[] types) {
      this.types = types;
   }

   public int getSimpleLayer() {
      if (this.mapIndex != null && this.additionalTypes != null) {
         for(int i = 0; i < this.additionalTypes.length; ++i) {
            if (this.mapIndex.positiveLayers.contains(this.additionalTypes[i])) {
               return 1;
            }

            if (this.mapIndex.negativeLayers.contains(this.additionalTypes[i])) {
               return -1;
            }
         }
      }

      return 0;
   }

   public TIntArrayList getNamesOrder() {
      return this.namesOrder;
   }

   public BinaryMapIndexReader.MapIndex getMapIndex() {
      return this.mapIndex;
   }

   public void setMapIndex(BinaryMapIndexReader.MapIndex mapIndex) {
      this.mapIndex = mapIndex;
   }

   public int getPointsLength() {
      return this.coordinates == null ? 0 : this.coordinates.length / 2;
   }

   public int getPoint31YTile(int ind) {
      return this.coordinates[2 * ind + 1];
   }

   public int getPoint31XTile(int ind) {
      return this.coordinates[2 * ind];
   }

   public boolean compareBinary(BinaryMapDataObject thatObj, int coordinatesPrecision) {
      if (this.objectType != thatObj.objectType
         || this.id != thatObj.id
         || this.area != thatObj.area
         || !compareCoordinates(this.coordinates, thatObj.coordinates, coordinatesPrecision)) {
         return false;
      } else if (this.mapIndex == null) {
         throw new IllegalStateException("Illegal binary object: " + this.id);
      } else if (thatObj.mapIndex == null) {
         throw new IllegalStateException("Illegal binary object: " + thatObj.id);
      } else {
         boolean equals = true;
         if (equals) {
            if (this.polygonInnerCoordinates != null && thatObj.polygonInnerCoordinates != null) {
               if (this.polygonInnerCoordinates.length != thatObj.polygonInnerCoordinates.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.polygonInnerCoordinates.length && equals; ++i) {
                     if (this.polygonInnerCoordinates[i] != null && thatObj.polygonInnerCoordinates[i] != null) {
                        if (this.polygonInnerCoordinates[i].length != thatObj.polygonInnerCoordinates[i].length) {
                           equals = false;
                        } else {
                           equals = compareCoordinates(this.polygonInnerCoordinates[i], thatObj.polygonInnerCoordinates[i], coordinatesPrecision);
                        }
                     } else {
                        equals = this.polygonInnerCoordinates[i] == thatObj.polygonInnerCoordinates[i];
                     }
                  }
               }
            } else {
               equals = this.polygonInnerCoordinates == thatObj.polygonInnerCoordinates;
            }
         }

         if (equals) {
            if (this.types != null && thatObj.types != null) {
               if (this.types.length != thatObj.types.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.types.length && equals; ++i) {
                     BinaryMapIndexReader.TagValuePair o = this.mapIndex.decodeType(this.types[i]);
                     BinaryMapIndexReader.TagValuePair s = thatObj.mapIndex.decodeType(thatObj.types[i]);
                     equals = o.equals(s) && equals;
                  }
               }
            } else {
               equals = this.types == thatObj.types;
            }
         }

         if (equals) {
            if (this.additionalTypes != null && thatObj.additionalTypes != null) {
               if (this.additionalTypes.length != thatObj.additionalTypes.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.additionalTypes.length && equals; ++i) {
                     BinaryMapIndexReader.TagValuePair o = this.mapIndex.decodeType(this.additionalTypes[i]);
                     BinaryMapIndexReader.TagValuePair s = thatObj.mapIndex.decodeType(thatObj.additionalTypes[i]);
                     equals = o.equals(s);
                  }
               }
            } else {
               equals = this.additionalTypes == thatObj.additionalTypes;
            }
         }

         if (equals) {
            if (this.namesOrder != null && thatObj.namesOrder != null) {
               if (this.namesOrder.size() != thatObj.namesOrder.size()) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.namesOrder.size() && equals; ++i) {
                     BinaryMapIndexReader.TagValuePair o = this.mapIndex.decodeType(this.namesOrder.get(i));
                     BinaryMapIndexReader.TagValuePair s = thatObj.mapIndex.decodeType(thatObj.namesOrder.get(i));
                     equals = o.equals(s);
                  }
               }
            } else {
               equals = this.namesOrder == thatObj.namesOrder;
            }
         }

         if (equals) {
            if (this.objectNames != null && thatObj.objectNames != null) {
               if (this.objectNames.size() != thatObj.objectNames.size()) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.namesOrder.size() && equals; ++i) {
                     String o = (String)this.objectNames.get(this.namesOrder.get(i));
                     String s = (String)thatObj.objectNames.get(thatObj.namesOrder.get(i));
                     equals = Algorithms.objectEquals(o, s);
                  }
               }
            } else {
               equals = this.objectNames == thatObj.objectNames;
            }
         }

         return equals;
      }
   }

   private static boolean compareCoordinates(int[] coordinates, int[] coordinates2, int precision) {
      if (precision == 0) {
         return Arrays.equals(coordinates, coordinates2);
      } else {
         TIntArrayList cd = simplify(coordinates, precision);
         TIntArrayList cd2 = simplify(coordinates2, precision);
         return cd.equals(cd2);
      }
   }

   private static TIntArrayList simplify(int[] c, int precision) {
      int len = c.length / 2;
      TIntArrayList lt = new TIntArrayList(len * 3);

      for(int i = 0; i < len; ++i) {
         lt.add(0);
         lt.add(c[i * 2]);
         lt.add(c[i * 2 + 1]);
      }

      lt.set(0, 1);
      lt.set((len - 1) * 3, 1);
      simplifyLine(lt, precision, 0, len - 1);
      TIntArrayList res = new TIntArrayList(len * 2);

      for(int i = 0; i < len; ++i) {
         if (lt.get(i * 3) == 1) {
            res.add(lt.get(i * 3 + 1));
            res.add(lt.get(i * 3 + 2));
         }
      }

      return res;
   }

   private static double orthogonalDistance(int x, int y, int x1, int y1, int x2, int y2) {
      long A = (long)(x - x1);
      long B = (long)(y - y1);
      long C = (long)(x2 - x1);
      long D = (long)(y2 - y1);
      return (double)Math.abs(A * D - C * B) / Math.sqrt((double)(C * C + D * D));
   }

   private static void simplifyLine(TIntArrayList lt, int precision, int start, int end) {
      if (start != end - 1) {
         int x = lt.get(start * 3 + 1);
         int y = lt.get(start * 3 + 2);
         int ex = lt.get(end * 3 + 1);
         int ey = lt.get(end * 3 + 2);
         double max = 0.0;
         int maxK = -1;

         for(int k = start + 1; k < end; ++k) {
            double ld = orthogonalDistance(lt.get(k * 3 + 1), lt.get(k * 3 + 2), x, y, ex, ey);
            if (maxK == -1 || max < ld) {
               maxK = k;
               max = ld;
            }
         }

         if (!(max < (double)precision)) {
            lt.set(maxK * 3, 1);
            simplifyLine(lt, precision, start, maxK);
            simplifyLine(lt, precision, maxK, end);
         }
      }
   }

   public boolean isLabelSpecified() {
      return (this.labelX != 0 || this.labelY != 0) && this.coordinates.length > 0;
   }

   public int getLabelX() {
      long sum = 0L;
      int LABEL_SHIFT = 5;
      int len = this.coordinates.length / 2;

      for(int i = 0; i < len; ++i) {
         sum += this.coordinates[2 * i];
      }

      int average = (int) (sum >> 5) / len;
      return average + this.labelX << LABEL_SHIFT;
   }

   public int getLabelY() {
      long sum = 0L;
      int LABEL_SHIFT = 5;
      int len = this.coordinates.length / 2;

      for(int i = 0; i < len; ++i) {
         sum += this.coordinates[2 * i + 1];
      }

      int average = (int) (sum >> 5) / len;
      return average + this.labelY << LABEL_SHIFT;
   }

   public int[] getCoordinates() {
      return this.coordinates;
   }

   public int getObjectType() {
      return this.objectType;
   }

   public String getTagValue(String tag) {
      if (this.mapIndex == null) {
         return "";
      } else {
         TIntObjectIterator<String> it = this.objectNames.iterator();

         while(it.hasNext()) {
            it.advance();
            BinaryMapIndexReader.TagValuePair tp = this.mapIndex.decodeType(it.key());
            if (tp.tag.equals(tag)) {
               return (String)it.value();
            }
         }

         return "";
      }
   }

   public String getAdditionalTagValue(String tag) {
      if (this.mapIndex == null) {
         return "";
      } else {
         for(int type : this.additionalTypes) {
            BinaryMapIndexReader.TagValuePair tp = this.mapIndex.decodeType(type);
            if (tag.equals(tp.tag)) {
               return tp.value;
            }
         }

         return "";
      }
   }

   @Override
   public String toString() {
      String obj = "Point";
      if (this.objectType == 2) {
         obj = "Line";
      } else if (this.objectType == 3) {
         obj = "Polygon";
      }

      return obj + " " + (this.getId() >> 7);
   }
}
