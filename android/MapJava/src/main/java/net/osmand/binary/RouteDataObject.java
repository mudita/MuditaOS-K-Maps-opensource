package net.osmand.binary;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Arrays;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;
import org.apache.commons.logging.Log;

public class RouteDataObject {
   static final int RESTRICTION_SHIFT = 3;
   static final int RESTRICTION_MASK = 7;
   public static int HEIGHT_UNDEFINED = -80000;
   public final BinaryMapRouteReaderAdapter.RouteRegion region;
   public int[] types;
   public int[] pointsX;
   public int[] pointsY;
   public long[] restrictions;
   public long[] restrictionsVia;
   public int[][] pointTypes;
   public String[][] pointNames;
   public int[][] pointNameTypes;
   public long id;
   public TIntObjectHashMap<String> names;
   public static final float NONE_MAX_SPEED = 40.0F;
   public int[] nameIds;
   public float[] heightDistanceArray = null;
   public float heightByCurrentLocation = Float.NaN;
   private static final Log LOG = PlatformUtil.getLog(RouteDataObject.class);

   public RouteDataObject(BinaryMapRouteReaderAdapter.RouteRegion region) {
      this.region = region;
   }

   public RouteDataObject(BinaryMapRouteReaderAdapter.RouteRegion region, int[] nameIds, String[] nameValues) {
      this.region = region;
      this.nameIds = nameIds;
      if (nameIds.length > 0) {
         this.names = new TIntObjectHashMap();
      }

      for(int i = 0; i < nameIds.length; ++i) {
         this.names.put(nameIds[i], nameValues[i]);
      }
   }

   public RouteDataObject(RouteDataObject copy) {
      this.region = copy.region;
      this.pointsX = copy.pointsX;
      this.pointsY = copy.pointsY;
      this.types = copy.types;
      this.names = copy.names;
      this.nameIds = copy.nameIds;
      this.restrictions = copy.restrictions;
      this.restrictionsVia = copy.restrictionsVia;
      this.pointTypes = copy.pointTypes;
      this.pointNames = copy.pointNames;
      this.pointNameTypes = copy.pointNameTypes;
      this.id = copy.id;
   }

   public boolean compareRoute(RouteDataObject thatObj) {
      if (this.id != thatObj.id || !Arrays.equals(this.pointsX, thatObj.pointsX) || !Arrays.equals(this.pointsY, thatObj.pointsY)) {
         return false;
      } else if (this.region == null) {
         throw new IllegalStateException("Illegal routing object: " + this.id);
      } else if (thatObj.region == null) {
         throw new IllegalStateException("Illegal routing object: " + thatObj.id);
      } else {
         boolean equals = true;
         equals = equals && Arrays.equals(this.restrictions, thatObj.restrictions);
         equals = equals && Arrays.equals(this.restrictionsVia, thatObj.restrictionsVia);
         if (equals) {
            if (this.types != null && thatObj.types != null) {
               if (this.types.length != thatObj.types.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.types.length && equals; ++i) {
                     String thisTag = this.region.routeEncodingRules.get(this.types[i]).getTag();
                     String thisValue = this.region.routeEncodingRules.get(this.types[i]).getValue();
                     String thatTag = thatObj.region.routeEncodingRules.get(thatObj.types[i]).getTag();
                     String thatValue = thatObj.region.routeEncodingRules.get(thatObj.types[i]).getValue();
                     equals = thisTag.equals(thatTag) && thisValue.equals(thatValue);
                  }
               }
            } else {
               equals = this.types == thatObj.types;
            }
         }

         if (equals) {
            if (this.nameIds != null && thatObj.nameIds != null) {
               if (this.nameIds.length != thatObj.nameIds.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.nameIds.length && equals; ++i) {
                     String thisTag = this.region.routeEncodingRules.get(this.nameIds[i]).getTag();
                     String thisValue = (String)this.names.get(this.nameIds[i]);
                     String thatTag = thatObj.region.routeEncodingRules.get(thatObj.nameIds[i]).getTag();
                     String thatValue = (String)thatObj.names.get(thatObj.nameIds[i]);
                     equals = Algorithms.objectEquals(thisTag, thatTag) && Algorithms.objectEquals(thisValue, thatValue);
                  }
               }
            } else {
               equals = this.nameIds == thatObj.nameIds;
            }
         }

         if (equals) {
            if (this.pointTypes != null && thatObj.pointTypes != null) {
               if (this.pointTypes.length != thatObj.pointTypes.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.pointTypes.length && equals; ++i) {
                     if (this.pointTypes[i] != null && thatObj.pointTypes[i] != null) {
                        if (this.pointTypes[i].length != thatObj.pointTypes[i].length) {
                           equals = false;
                        } else {
                           for(int j = 0; j < this.pointTypes[i].length && equals; ++j) {
                              String thisTag = this.region.routeEncodingRules.get(this.pointTypes[i][j]).getTag();
                              String thisValue = this.region.routeEncodingRules.get(this.pointTypes[i][j]).getValue();
                              String thatTag = thatObj.region.routeEncodingRules.get(thatObj.pointTypes[i][j]).getTag();
                              String thatValue = thatObj.region.routeEncodingRules.get(thatObj.pointTypes[i][j]).getValue();
                              equals = Algorithms.objectEquals(thisTag, thatTag) && Algorithms.objectEquals(thisValue, thatValue);
                           }
                        }
                     } else {
                        equals = this.pointTypes[i] == thatObj.pointTypes[i];
                     }
                  }
               }
            } else {
               equals = this.pointTypes == thatObj.pointTypes;
            }
         }

         if (equals) {
            if (this.pointNameTypes != null && thatObj.pointNameTypes != null) {
               if (this.pointNameTypes.length != thatObj.pointNameTypes.length) {
                  equals = false;
               } else {
                  for(int i = 0; i < this.pointNameTypes.length && equals; ++i) {
                     if (this.pointNameTypes[i] != null && thatObj.pointNameTypes[i] != null) {
                        if (this.pointNameTypes[i].length != thatObj.pointNameTypes[i].length) {
                           equals = false;
                        } else {
                           for(int j = 0; j < this.pointNameTypes[i].length && equals; ++j) {
                              String thisTag = this.region.routeEncodingRules.get(this.pointNameTypes[i][j]).getTag();
                              String thisValue = this.pointNames[i][j];
                              String thatTag = thatObj.region.routeEncodingRules.get(thatObj.pointNameTypes[i][j]).getTag();
                              String thatValue = thatObj.pointNames[i][j];
                              equals = Algorithms.objectEquals(thisTag, thatTag) && Algorithms.objectEquals(thisValue, thatValue);
                           }
                        }
                     } else {
                        equals = this.pointNameTypes[i] == thatObj.pointNameTypes[i];
                     }
                  }
               }
            } else {
               equals = this.pointNameTypes == thatObj.pointNameTypes;
            }
         }

         return equals;
      }
   }

   public float[] calculateHeightArray() {
      return this.calculateHeightArray(null);
   }

   public float[] calculateHeightArray(LatLon currentLocation) {
      if (this.heightDistanceArray != null) {
         return this.heightDistanceArray;
      } else {
         int startHeight = Algorithms.parseIntSilently(this.getValue("osmand_ele_start"), HEIGHT_UNDEFINED);
         int endHeight = Algorithms.parseIntSilently(this.getValue("osmand_ele_end"), startHeight);
         if (startHeight == HEIGHT_UNDEFINED) {
            this.heightDistanceArray = new float[0];
            return this.heightDistanceArray;
         } else {
            this.heightDistanceArray = new float[2 * this.getPointsLength()];
            double plon = 0.0;
            double plat = 0.0;
            float prevHeight = (float)startHeight;
            this.heightByCurrentLocation = Float.NaN;
            double prevDistance = 0.0;

            for(int k = 0; k < this.getPointsLength(); ++k) {
               double lon = MapUtils.get31LongitudeX(this.getPoint31XTile(k));
               double lat = MapUtils.get31LatitudeY(this.getPoint31YTile(k));
               if (k <= 0) {
                  this.heightDistanceArray[0] = 0.0F;
                  this.heightDistanceArray[1] = (float)startHeight;
               } else {
                  double dd = MapUtils.getDistance(plat, plon, lat, lon);
                  float height = (float)HEIGHT_UNDEFINED;
                  if (k == this.getPointsLength() - 1) {
                     height = (float)endHeight;
                  } else {
                     String asc = this.getValue(k, "osmand_ele_asc");
                     if (asc != null && asc.length() > 0) {
                        height = prevHeight + Float.parseFloat(asc);
                     } else {
                        String desc = this.getValue(k, "osmand_ele_desc");
                        if (desc != null && desc.length() > 0) {
                           height = prevHeight - Float.parseFloat(desc);
                        }
                     }
                  }

                  this.heightDistanceArray[2 * k] = (float)dd;
                  this.heightDistanceArray[2 * k + 1] = height;
                  if (currentLocation != null) {
                     double distance = MapUtils.getDistance(currentLocation, lat, lon);
                     if (height != (float)HEIGHT_UNDEFINED && distance < prevDistance) {
                        prevDistance = distance;
                        this.heightByCurrentLocation = height;
                     }
                  }

                  if (height != (float)HEIGHT_UNDEFINED) {
                     double totalDistance = dd;
                     int startUndefined = k;

                     while(startUndefined - 1 >= 0 && this.heightDistanceArray[2 * (startUndefined - 1) + 1] == (float)HEIGHT_UNDEFINED) {
                        totalDistance += (double)this.heightDistanceArray[2 * --startUndefined];
                     }

                     if (totalDistance > 0.0) {
                        double angle = (double)(height - prevHeight) / totalDistance;

                        for(int j = startUndefined; j < k; ++j) {
                           this.heightDistanceArray[2 * j + 1] = (float)(
                              (double)this.heightDistanceArray[2 * j] * angle + (double)this.heightDistanceArray[2 * j - 1]
                           );
                        }
                     }

                     prevHeight = height;
                  }
               }

               plat = lat;
               plon = lon;
               if (currentLocation != null) {
                  prevDistance = MapUtils.getDistance(currentLocation, lat, lon);
               }
            }

            return this.heightDistanceArray;
         }
      }
   }

   public long getId() {
      return this.id;
   }

   public String getName() {
      return this.names != null ? (String)this.names.get(this.region.nameTypeRule) : null;
   }

   public String getName(String lang) {
      return this.getName(lang, false);
   }

   public String getName(String lang, boolean transliterate) {
      if (this.names != null) {
         if (Algorithms.isEmpty(lang)) {
            return (String)this.names.get(this.region.nameTypeRule);
         } else {
            int[] kt = this.names.keys();

            for(int i = 0; i < kt.length; ++i) {
               int k = kt[i];
               if (this.region.routeEncodingRules.size() > k && ("name:" + lang).equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return (String)this.names.get(k);
               }
            }

            String nmDef = (String)this.names.get(this.region.nameTypeRule);
            return transliterate && nmDef != null && nmDef.length() > 0 ? TransliterationHelper.transliterate(nmDef) : nmDef;
         }
      } else {
         return null;
      }
   }

   public int[] getNameIds() {
      return this.nameIds;
   }

   public TIntObjectHashMap<String> getNames() {
      return this.names;
   }

   public String getRef(String lang, boolean transliterate, boolean direction) {
      if (this.names != null) {
         if (Algorithms.isEmpty(lang)) {
            return (String)this.names.get(this.region.refTypeRule);
         } else {
            int[] kt = this.names.keys();

            for(int i = 0; i < kt.length; ++i) {
               int k = kt[i];
               if (this.region.routeEncodingRules.size() > k && ("ref:" + lang).equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return (String)this.names.get(k);
               }
            }

            String refDefault = (String)this.names.get(this.region.refTypeRule);
            return transliterate && refDefault != null && refDefault.length() > 0 ? TransliterationHelper.transliterate(refDefault) : refDefault;
         }
      } else {
         return null;
      }
   }

   public String getDestinationRef(String lang, boolean transliterate, boolean direction) {
      if (this.names != null) {
         int[] kt = this.names.keys();
         String refTag = direction ? "destination:ref:forward" : "destination:ref:backward";
         String refTagDefault = "destination:ref";
         String refDefault = null;

         for(int i = 0; i < kt.length; ++i) {
            int k = kt[i];
            if (this.region.routeEncodingRules.size() > k) {
               if (refTag.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return (String)this.names.get(k);
               }

               if (refTagDefault.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  refDefault = (String)this.names.get(k);
               }
            }
         }

         if (refDefault != null) {
            return refDefault;
         }
      }

      return null;
   }

   public String getDestinationName(String lang, boolean transliterate, boolean direction) {
      if (this.names != null) {
         int[] kt = this.names.keys();
         String destinationTagLangFB = "destination:lang:XX";
         if (!Algorithms.isEmpty(lang)) {
            destinationTagLangFB = direction ? "destination:lang:" + lang + ":forward" : "destination:lang:" + lang + ":backward";
         }

         String destinationTagFB = direction ? "destination:forward" : "destination:backward";
         String destinationTagLang = "destination:lang:XX";
         if (!Algorithms.isEmpty(lang)) {
            destinationTagLang = "destination:lang:" + lang;
         }

         String destinationTagDefault = "destination";
         String destinationDefault = null;

         for(int i = 0; i < kt.length; ++i) {
            int k = kt[i];
            if (this.region.routeEncodingRules.size() > k) {
               if (!Algorithms.isEmpty(lang) && destinationTagLangFB.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return transliterate ? TransliterationHelper.transliterate((String)this.names.get(k)) : (String)this.names.get(k);
               }

               if (destinationTagFB.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return transliterate ? TransliterationHelper.transliterate((String)this.names.get(k)) : (String)this.names.get(k);
               }

               if (!Algorithms.isEmpty(lang) && destinationTagLang.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  return transliterate ? TransliterationHelper.transliterate((String)this.names.get(k)) : (String)this.names.get(k);
               }

               if (destinationTagDefault.equals(this.region.routeEncodingRules.get(k).getTag())) {
                  destinationDefault = (String)this.names.get(k);
               }
            }
         }

         if (destinationDefault != null) {
            return transliterate ? TransliterationHelper.transliterate(destinationDefault) : destinationDefault;
         }
      }

      return "";
   }

   public int getPoint31XTile(int i) {
      return this.pointsX[i];
   }

   public int getPoint31YTile(int i) {
      return this.pointsY[i];
   }

   public int getPointsLength() {
      return this.pointsX.length;
   }

   public int getRestrictionLength() {
      return this.restrictions == null ? 0 : this.restrictions.length;
   }

   public int getRestrictionType(int i) {
      return (int)(this.restrictions[i] & 7L);
   }

   public RouteDataObject.RestrictionInfo getRestrictionInfo(int k) {
      RouteDataObject.RestrictionInfo ri = new RouteDataObject.RestrictionInfo();
      ri.toWay = this.getRestrictionId(k);
      ri.type = this.getRestrictionType(k);
      if (this.restrictionsVia != null && k < this.restrictionsVia.length) {
         ri.viaWay = this.restrictionsVia[k];
      }

      return ri;
   }

   public long getRestrictionVia(int i) {
      return this.restrictionsVia != null && this.restrictionsVia.length > i ? this.restrictionsVia[i] : 0L;
   }

   public long getRestrictionId(int i) {
      return this.restrictions[i] >> 3;
   }

   public boolean hasPointTypes() {
      return this.pointTypes != null;
   }

   public boolean hasPointNames() {
      return this.pointNames != null;
   }

   public void insert(int pos, int x31, int y31) {
      int[] opointsX = this.pointsX;
      int[] opointsY = this.pointsY;
      int[][] opointTypes = this.pointTypes;
      String[][] opointNames = this.pointNames;
      int[][] opointNameTypes = this.pointNameTypes;
      this.pointsX = new int[this.pointsX.length + 1];
      this.pointsY = new int[this.pointsY.length + 1];
      boolean insTypes = this.pointTypes != null && this.pointTypes.length > pos;
      boolean insNames = this.pointNames != null && this.pointNames.length > pos;
      if (insTypes) {
         this.pointTypes = new int[opointTypes.length + 1][];
      }

      if (insNames) {
         this.pointNames = new String[opointNames.length + 1][];
         this.pointNameTypes = new int[opointNameTypes.length + 1][];
      }

      int i;
      for(i = 0; i < pos; ++i) {
         this.pointsX[i] = opointsX[i];
         this.pointsY[i] = opointsY[i];
         if (insTypes) {
            this.pointTypes[i] = opointTypes[i];
         }

         if (insNames) {
            this.pointNames[i] = opointNames[i];
            this.pointNameTypes[i] = opointNameTypes[i];
         }
      }

      this.pointsX[i] = x31;
      this.pointsY[i] = y31;
      if (insTypes) {
         this.pointTypes[i] = null;
      }

      if (insNames) {
         this.pointNames[i] = null;
         this.pointNameTypes[i] = null;
      }

      ++i;

      for(; i < this.pointsX.length; ++i) {
         this.pointsX[i] = opointsX[i - 1];
         this.pointsY[i] = opointsY[i - 1];
         if (insTypes && i < this.pointTypes.length) {
            this.pointTypes[i] = opointTypes[i - 1];
         }

         if (insNames && i < this.pointNames.length) {
            this.pointNames[i] = opointNames[i - 1];
         }

         if (insNames && i < this.pointNameTypes.length) {
            this.pointNameTypes[i] = opointNameTypes[i - 1];
         }
      }
   }

   public String[] getPointNames(int ind) {
      return this.pointNames != null && ind < this.pointNames.length ? this.pointNames[ind] : null;
   }

   public int[] getPointNameTypes(int ind) {
      return this.pointNameTypes != null && ind < this.pointNameTypes.length ? this.pointNameTypes[ind] : null;
   }

   public int[] getPointTypes(int ind) {
      return this.pointTypes != null && ind < this.pointTypes.length ? this.pointTypes[ind] : null;
   }

   public void removePointType(int ind, int type) {
      if (this.pointTypes != null || ind < this.pointTypes.length) {
         int[] typesArr = this.pointTypes[ind];

         for(int i = 0; i < typesArr.length; ++i) {
            if (typesArr[i] == type) {
               int[] result = new int[typesArr.length - 1];
               System.arraycopy(typesArr, 0, result, 0, i);
               if (typesArr.length != i) {
                  System.arraycopy(typesArr, i + 1, result, i, typesArr.length - 1 - i);
                  this.pointTypes[ind] = result;
                  break;
               }
            }
         }
      }
   }

   public int[] getTypes() {
      return this.types;
   }

   public void processConditionalTags(long conditionalTime) {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r != null && r.conditional()) {
            int vl = r.conditionalValue(conditionalTime);
            if (vl != 0) {
               BinaryMapRouteReaderAdapter.RouteTypeRule rtr = this.region.quickGetEncodingRule(vl);
               String nonCondTag = rtr.getTag();

               int ks;
               for(ks = 0; ks < this.types.length; ++ks) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule toReplace = this.region.quickGetEncodingRule(this.types[ks]);
                  if (toReplace != null && toReplace.getTag().equals(nonCondTag)) {
                     break;
                  }
               }

               if (ks == this.types.length) {
                  int[] ntypes = new int[this.types.length + 1];
                  System.arraycopy(this.types, 0, ntypes, 0, this.types.length);
                  this.types = ntypes;
               }

               this.types[ks] = vl;
            }
         }
      }

      if (this.pointTypes != null) {
         for(int i = 0; i < this.pointTypes.length; ++i) {
            if (this.pointTypes[i] != null) {
               int[] pTypes = this.pointTypes[i];
               int pSz = pTypes.length;
               if (pSz > 0) {
                  for(int j = 0; j < pSz; ++j) {
                     BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(pTypes[j]);
                     if (r != null && r.conditional()) {
                        int vl = r.conditionalValue(conditionalTime);
                        if (vl != 0) {
                           BinaryMapRouteReaderAdapter.RouteTypeRule rtr = this.region.quickGetEncodingRule(vl);
                           String nonCondTag = rtr.getTag();

                           int ks;
                           for(ks = 0; ks < this.pointTypes[i].length; ++ks) {
                              BinaryMapRouteReaderAdapter.RouteTypeRule toReplace = this.region.quickGetEncodingRule(this.pointTypes[i][ks]);
                              if (toReplace != null && toReplace.getTag().contentEquals(nonCondTag)) {
                                 break;
                              }
                           }

                           if (ks == pTypes.length) {
                              int[] ntypes = new int[pTypes.length + 1];
                              System.arraycopy(pTypes, 0, ntypes, 0, pTypes.length);
                              pTypes = ntypes;
                           }

                           pTypes[ks] = vl;
                        }
                     }
                  }
               }

               this.pointTypes[i] = pTypes;
            }
         }
      }
   }

   public float getMaximumSpeed(boolean direction) {
      int sz = this.types.length;
      float maxSpeed = 0.0F;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         float mx = r.maxSpeed();
         if (mx > 0.0F) {
            if (r.isForward() != 0) {
               if (r.isForward() == 1 == direction) {
                  maxSpeed = mx;
                  break;
               }
            } else {
               maxSpeed = mx;
            }
         }
      }

      return maxSpeed;
   }

   public static float parseSpeed(String v, float def) {
      if (v.equals("none")) {
         return 40.0F;
      } else {
         int i = Algorithms.findFirstNumberEndIndex(v);
         if (i > 0) {
            float f = Float.parseFloat(v.substring(0, i));
            f = (float)((double)f / 3.6);
            if (v.contains("mph")) {
               f = (float)((double)f * 1.6);
            }

            return f;
         } else {
            return def;
         }
      }
   }

   public static float parseLength(String v, float def) {
      float f = 0.0F;
      int i = Algorithms.findFirstNumberEndIndex(v);
      if (i <= 0) {
         return def;
      } else {
         f += Float.parseFloat(v.substring(0, i));
         String pref = v.substring(i, v.length()).trim();
         float add = 0.0F;

         for(int ik = 0; ik < pref.length(); ++ik) {
            if (Algorithms.isDigit(pref.charAt(ik)) || pref.charAt(ik) == '.' || pref.charAt(ik) == '-') {
               int first = Algorithms.findFirstNumberEndIndex(pref.substring(ik));
               if (first != -1) {
                  add = parseLength(pref.substring(ik), 0.0F);
                  pref = pref.substring(0, ik);
               }
               break;
            }
         }

         if (pref.contains("km")) {
            f *= 1000.0F;
         }

         if (pref.contains("\"") || pref.contains("in")) {
            f = (float)((double)f * 0.0254);
         } else if (pref.contains("'") || pref.contains("ft") || pref.contains("feet")) {
            f = (float)((double)f * 0.3048);
         } else if (pref.contains("cm")) {
            f = (float)((double)f * 0.01);
         } else if (pref.contains("mile")) {
            f *= 1609.34F;
         }

         return f + add;
      }
   }

   public static float parseWeightInTon(String v, float def) {
      int i = Algorithms.findFirstNumberEndIndex(v);
      if (i <= 0) {
         return def;
      } else {
         float f = Float.parseFloat(v.substring(0, i));
         if (v.contains("\"") || v.contains("lbs")) {
            f = f * 0.4535F / 1000.0F;
         }

         return f;
      }
   }

   public boolean loop() {
      return this.pointsX[0] == this.pointsX[this.pointsX.length - 1] && this.pointsY[0] == this.pointsY[this.pointsY.length - 1];
   }

   public boolean platform() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r.getTag().equals("railway") && r.getValue().equals("platform")) {
            return true;
         }

         if (r.getTag().equals("public_transport") && r.getValue().equals("platform")) {
            return true;
         }
      }

      return false;
   }

   public boolean roundabout() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r.roundabout()) {
            return true;
         }

         if (r.onewayDirection() != 0 && this.loop()) {
            return true;
         }
      }

      return false;
   }

   public boolean tunnel() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r.getTag().equals("tunnel") && r.getValue().equals("yes")) {
            return true;
         }

         if (r.getTag().equals("layer") && r.getValue().equals("-1")) {
            return true;
         }
      }

      return false;
   }

   public boolean isExitPoint() {
      if (this.pointTypes != null) {
         int ptSz = this.pointTypes.length;

         for(int i = 0; i < ptSz; ++i) {
            int[] point = this.pointTypes[i];
            if (point != null) {
               int pSz = point.length;

               for(int j = 0; j < pSz; ++j) {
                  if (this.region.routeEncodingRules.get(point[j]).getValue().equals("motorway_junction")) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean hasTrafficLightAt(int i) {
      int[] pointTypes = this.getPointTypes(i);
      if (pointTypes != null) {
         for(int pointType : pointTypes) {
            if (this.region.routeEncodingRules.get(pointType).getValue().startsWith("traffic_signals")) {
               return true;
            }
         }
      }

      return false;
   }

   public String getExitName() {
      if (this.pointNames != null && this.pointNameTypes != null) {
         int pnSz = this.pointNames.length;

         for(int i = 0; i < pnSz; ++i) {
            String[] point = this.pointNames[i];
            if (point != null) {
               int pSz = point.length;

               for(int j = 0; j < pSz; ++j) {
                  if (this.pointNameTypes[i][j] == this.region.nameTypeRule) {
                     return point[j];
                  }
               }
            }
         }
      }

      return null;
   }

   public String getExitRef() {
      if (this.pointNames != null && this.pointNameTypes != null) {
         int pnSz = this.pointNames.length;

         for(int i = 0; i < pnSz; ++i) {
            String[] point = this.pointNames[i];
            if (point != null) {
               int pSz = point.length;

               for(int j = 0; j < pSz; ++j) {
                  if (this.pointNameTypes[i][j] == this.region.refTypeRule) {
                     return point[j];
                  }
               }
            }
         }
      }

      return null;
   }

   public int getOneway() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r.onewayDirection() != 0) {
            return r.onewayDirection();
         }

         if (r.roundabout()) {
            return 1;
         }
      }

      return 0;
   }

   public String getRoute() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if ("route".equals(r.getTag())) {
            return r.getValue();
         }
      }

      return null;
   }

   public String getHighway() {
      return getHighway(this.types, this.region);
   }

   public boolean hasPrivateAccess() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (("motorcar".equals(r.getTag()) || "motor_vehicle".equals(r.getTag()) || "vehicle".equals(r.getTag()) || "access".equals(r.getTag()))
            && r.getValue().equals("private")) {
            return true;
         }
      }

      return false;
   }

   public String getValue(String tag) {
      for(int i = 0; i < this.types.length; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         if (r.getTag().equals(tag)) {
            return r.getValue();
         }
      }

      if (this.nameIds != null) {
         for(int i = 0; i < this.nameIds.length; ++i) {
            BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.nameIds[i]);
            if (r.getTag().equals(tag)) {
               return (String)this.names.get(this.nameIds[i]);
            }
         }
      }

      return null;
   }

   public String getValue(int pnt, String tag) {
      if (this.pointTypes != null && pnt < this.pointTypes.length && this.pointTypes[pnt] != null) {
         for(int i = 0; i < this.pointTypes[pnt].length; ++i) {
            BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.pointTypes[pnt][i]);
            if (r.getTag().equals(tag)) {
               return r.getValue();
            }
         }
      }

      if (this.pointNameTypes != null && pnt < this.pointNameTypes.length && this.pointNameTypes[pnt] != null) {
         for(int i = 0; i < this.pointNameTypes[pnt].length; ++i) {
            BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.pointNameTypes[pnt][i]);
            if (r.getTag().equals(tag)) {
               return this.pointNames[pnt][i];
            }
         }
      }

      return null;
   }

   public static String getHighway(int[] types, BinaryMapRouteReaderAdapter.RouteRegion region) {
      String highway = null;
      int sz = types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = region.quickGetEncodingRule(types[i]);
         highway = r.highwayRoad();
         if (highway != null) {
            break;
         }
      }

      return highway;
   }

   public int getLanes() {
      int sz = this.types.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(this.types[i]);
         int ln = r.lanes();
         if (ln > 0) {
            return ln;
         }
      }

      return -1;
   }

   public double directionRoute(int startPoint, boolean plus) {
      return this.directionRoute(startPoint, plus, 5.0F);
   }

   public boolean bearingVsRouteDirection(Location loc) {
      boolean direction = true;
      if (loc != null && loc.hasBearing()) {
         double diff = MapUtils.alignAngleDifference(this.directionRoute(0, true) - (double)(loc.getBearing() / 180.0F) * Math.PI);
         direction = Math.abs(diff) < Math.PI / 2;
      }

      return direction;
   }

   public boolean isRoadDeleted() {
      int[] pt = this.getTypes();
      int sz = pt.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(pt[i]);
         if ("osmand_change".equals(r.getTag()) && "delete".equals(r.getValue())) {
            return true;
         }
      }

      return false;
   }

   public boolean isStopApplicable(boolean direction, int intId, int startPointInd, int endPointInd) {
      int[] pt = this.getPointTypes(intId);
      int sz = pt.length;

      for(int i = 0; i < sz; ++i) {
         BinaryMapRouteReaderAdapter.RouteTypeRule r = this.region.quickGetEncodingRule(pt[i]);
         if (r.getTag().equals("direction")) {
            String dv = r.getValue();
            if (dv.equals("forward") && direction || dv.equals("backward") && !direction) {
               return true;
            }

            if (dv.equals("forward") && !direction || dv.equals("backward") && direction) {
               return false;
            }
         }
      }

      double d2Start = this.distance(startPointInd, intId);
      double d2End = this.distance(intId, endPointInd);
      return !(d2Start < d2End) || d2Start == 0.0 || d2End == 0.0 || !(d2Start < 50.0);
   }

   public double distance(int startPoint, int endPoint) {
      if (startPoint > endPoint) {
         int k = endPoint;
         endPoint = startPoint;
         startPoint = k;
      }

      double d = 0.0;

      for(int k = startPoint; k < endPoint && k < this.getPointsLength() - 1; ++k) {
         int x = this.getPoint31XTile(k);
         int y = this.getPoint31YTile(k);
         int kx = this.getPoint31XTile(k + 1);
         int ky = this.getPoint31YTile(k + 1);
         d += this.simplifyDistance(kx, ky, x, y);
      }

      return d;
   }

   public double directionRoute(int startPoint, boolean plus, float dist) {
      int x = this.getPoint31XTile(startPoint);
      int y = this.getPoint31YTile(startPoint);
      int nx = startPoint;
      int px = x;
      int py = y;
      double total = 0.0;

      while(plus ? ++nx < this.getPointsLength() : --nx >= 0) {
         px = this.getPoint31XTile(nx);
         py = this.getPoint31YTile(nx);
         total += this.simplifyDistance(x, y, px, py);
         if (!(total < (double)dist)) {
            break;
         }
      }

      return -Math.atan2((double)(x - px), (double)(y - py));
   }

   private double simplifyDistance(int x, int y, int px, int py) {
      return (double)Math.abs(px - x) * 0.011 + (double)Math.abs(py - y) * 0.01863;
   }

   private static void assertTrueLength(String vl, float exp) {
      float dest = parseLength(vl, 0.0F);
      if (exp != dest) {
         System.err.println("FAIL " + vl + " " + dest);
      } else {
         System.out.println("OK " + vl);
      }
   }

   public static void main(String[] args) {
      assertTrueLength("10 km", 10000.0F);
      assertTrueLength("0.01 km", 10.0F);
      assertTrueLength("0.01 km 10 m", 20.0F);
      assertTrueLength("10 m", 10.0F);
      assertTrueLength("10m", 10.0F);
      assertTrueLength("3.4 m", 3.4F);
      assertTrueLength("3.40 m", 3.4F);
      assertTrueLength("10 m 10m", 20.0F);
      assertTrueLength("14'10\"", 4.5212F);
      assertTrueLength("14.5'", 4.4196F);
      assertTrueLength("14.5 ft", 4.4196F);
      assertTrueLength("14'0\"", 4.2672F);
      assertTrueLength("15ft", 4.572F);
      assertTrueLength("15 ft 1 in", 4.5974F);
      assertTrueLength("4.1 metres", 4.1F);
      assertTrueLength("14'0''", 4.2672F);
      assertTrueLength("14 feet", 4.2672F);
      assertTrueLength("14 mile", 22530.76F);
      assertTrueLength("14 cm", 0.14F);
   }

   public String coordinates() {
      StringBuilder b = new StringBuilder();
      b.append(" lat/lon : ");

      for(int i = 0; i < this.getPointsLength(); ++i) {
         float x = (float)MapUtils.get31LongitudeX(this.getPoint31XTile(i));
         float y = (float)MapUtils.get31LatitudeY(this.getPoint31YTile(i));
         b.append(y).append(" / ").append(x).append(" , ");
      }

      return b.toString();
   }

   @Override
   public String toString() {
      String str = String.format("Road (%d)", this.id / 64L);
      String rf = this.getRef("", false, true);
      if (!Algorithms.isEmpty(rf)) {
         str = str + ", ref ('" + rf + "')";
      }

      String name = this.getName();
      if (!Algorithms.isEmpty(name)) {
         str = str + ", name ('" + name + "')";
      }

      return str;
   }

   public boolean hasNameTagStartsWith(String tagStartsWith) {
      for(int nm = 0; this.nameIds != null && nm < this.nameIds.length; ++nm) {
         BinaryMapRouteReaderAdapter.RouteTypeRule rtr = this.region.quickGetEncodingRule(this.nameIds[nm]);
         if (rtr != null && rtr.getTag().startsWith(tagStartsWith)) {
            return true;
         }
      }

      return false;
   }

   public void setRestriction(int k, long to, int type, long viaWay) {
      long valto = to << 3 | (long)type & 7L;
      this.restrictions[k] = valto;
      if (viaWay != 0L) {
         this.setRestrictionVia(k, viaWay);
      }
   }

   public void setRestrictionVia(int k, long viaWay) {
      if (this.restrictionsVia != null) {
         long[] nrestrictionsVia = new long[Math.max(k + 1, this.restrictions.length)];
         System.arraycopy(this.restrictions, 0, nrestrictionsVia, 0, this.restrictions.length);
         this.restrictionsVia = nrestrictionsVia;
      } else {
         this.restrictionsVia = new long[k + 1];
      }

      this.restrictionsVia[k] = viaWay;
   }

   public void setPointNames(int pntInd, int[] array, String[] nms) {
      if (this.pointNameTypes == null || this.pointNameTypes.length <= pntInd) {
         int[][] npointTypes = new int[pntInd + 1][];
         String[][] npointNames = new String[pntInd + 1][];

         for(int k = 0; this.pointNameTypes != null && k < this.pointNameTypes.length; ++k) {
            npointTypes[k] = this.pointNameTypes[k];
            npointNames[k] = this.pointNames[k];
         }

         this.pointNameTypes = npointTypes;
         this.pointNames = npointNames;
      }

      this.pointNameTypes[pntInd] = array;
      this.pointNames[pntInd] = nms;
   }

   public void setPointTypes(int pntInd, int[] array) {
      if (this.pointTypes == null || this.pointTypes.length <= pntInd) {
         int[][] npointTypes = new int[pntInd + 1][];

         for(int k = 0; this.pointTypes != null && k < this.pointTypes.length; ++k) {
            npointTypes[k] = this.pointTypes[k];
         }

         this.pointTypes = npointTypes;
      }

      this.pointTypes[pntInd] = array;
   }

   public boolean hasPointType(int pntId, int type) {
      for(int k = 0; this.pointTypes != null && this.pointTypes[pntId] != null && k < this.pointTypes[pntId].length; ++k) {
         if (this.pointTypes[pntId][k] == type) {
            return true;
         }
      }

      return false;
   }

   public static class RestrictionInfo {
      public int type;
      public long toWay;
      public long viaWay;
      public RouteDataObject.RestrictionInfo next;

      public int length() {
         return this.next == null ? 1 : this.next.length() + 1;
      }
   }
}
