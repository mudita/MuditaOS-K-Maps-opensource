package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

public class BinaryMapPoiReaderAdapter {
   private static final Log LOG = PlatformUtil.getLog(BinaryMapPoiReaderAdapter.class);
   public static final int SHIFT_BITS_CATEGORY = 7;
   private static final int CATEGORY_MASK = 127;
   private static final int ZOOM_TO_SKIP_FILTER_READ = 6;
   private static final int ZOOM_TO_SKIP_FILTER = 3;
   private static final int BUCKET_SEARCH_BY_NAME = 15;
   private static final int BASE_POI_SHIFT = 7;
   private static final int FINAL_POI_SHIFT = 5;
   private static final int BASE_POI_ZOOM = 24;
   private static final int FINAL_POI_ZOOM = 26;
   private final CodedInputStream codedIS;
   private final BinaryMapIndexReader map;
   private final MapPoiTypes poiTypes;

   protected BinaryMapPoiReaderAdapter(BinaryMapIndexReader map) {
      this.codedIS = map.codedIS;
      this.map = map;
      this.poiTypes = MapPoiTypes.getDefault();
   }

   private void skipUnknownField(int t) throws IOException {
      this.map.skipUnknownField(t);
   }

   private int readInt() throws IOException {
      return this.map.readInt();
   }

   private void readPoiBoundariesIndex(BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               region.left31 = this.codedIS.readUInt32();
               break;
            case 2:
               region.right31 = this.codedIS.readUInt32();
               break;
            case 3:
               region.top31 = this.codedIS.readUInt32();
               break;
            case 4:
               region.bottom31 = this.codedIS.readUInt32();
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected void readPoiIndex(BinaryMapPoiReaderAdapter.PoiRegion region, boolean readCategories) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               region.name = this.codedIS.readString();
               break;
            case 2: {
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readPoiBoundariesIndex(region);
               this.codedIS.popLimit(oldLimit);
               break;
            }
            case 3: {
               if (!readCategories) {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                  return;
               }

               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readCategory(region);
               this.codedIS.popLimit(oldLimit);
               break;
            }
            case 4:
            default:
               this.skipUnknownField(t);
               break;
            case 5: {
               if (!readCategories) {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                  return;
               }

               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readSubtypes(region);
               this.codedIS.popLimit(oldLimit);
               break;
            }
            case 6:
               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return;
         }
      }
   }

   private void readCategory(BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               String cat = this.codedIS.readString().intern();
               region.categories.add(cat);
               region.categoriesType.add(this.poiTypes.getPoiCategoryByName(cat.toLowerCase(), true));
               region.subcategories.add(new ArrayList());
               break;
            case 2:
            default:
               this.skipUnknownField(t);
               break;
            case 3:
               region.subcategories.get(region.subcategories.size() - 1).add(this.codedIS.readString().intern());
         }
      }
   }

   private void readSubtypes(BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      label25:
      while(true) {
         int outT = this.codedIS.readTag();
         int outTag = WireFormat.getTagFieldNumber(outT);
         switch(outTag) {
            case 0:
               return;
            case 4:
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               BinaryMapPoiReaderAdapter.PoiSubType st = new BinaryMapPoiReaderAdapter.PoiSubType();

               while(true) {
                  int inT = this.codedIS.readTag();
                  int inTag = WireFormat.getTagFieldNumber(inT);
                  switch(inTag) {
                     case 0:
                        region.subTypes.add(st);
                        this.codedIS.popLimit(oldLimit);
                        continue label25;
                     case 1:
                        st.name = this.codedIS.readString().intern();
                        break;
                     case 2:
                     case 4:
                     case 5:
                     case 6:
                     case 7:
                     default:
                        this.skipUnknownField(inT);
                        break;
                     case 3:
                        st.text = this.codedIS.readBool();
                        break;
                     case 8:
                        if (st.possibleValues == null) {
                           st.possibleValues = new ArrayList<>();
                        }

                        st.possibleValues.add(this.codedIS.readString().intern());
                  }
               }
            default:
               this.skipUnknownField(outT);
         }
      }
   }

   public void initCategories(BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      if (region.categories.isEmpty()) {
         this.codedIS.seek((long)region.filePointer);
         int oldLimit = this.codedIS.pushLimit(region.length);
         this.readPoiIndex(region, true);
         this.codedIS.popLimit(oldLimit);
      }
   }

   private String normalizeSearchPoiByNameQuery(String query) {
      return query.replace("\"", "").toLowerCase();
   }

   protected void searchPoiByName(BinaryMapPoiReaderAdapter.PoiRegion region, BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      TIntLongHashMap offsets = new TIntLongHashMap();
      String query = this.normalizeSearchPoiByNameQuery(req.nameQuery);
      CollatorStringMatcher matcher = new CollatorStringMatcher(query, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE);
      long time = System.currentTimeMillis();
      int indexOffset = this.codedIS.getTotalBytesRead();

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 4:
               int length = this.readInt();
               int oldLimit = this.codedIS.pushLimit(length);
               offsets = this.readPoiNameIndex(matcher.getCollator(), query, req);
               this.codedIS.popLimit(oldLimit);
               break;
            case 9:
               Integer[] offKeys = new Integer[offsets.size()];
               if (offsets.size() > 0) {
                  int[] keys = offsets.keys();

                  for(int i = 0; i < keys.length; ++i) {
                     offKeys[i] = keys[i];
                  }

                  final TIntLongHashMap foffsets = offsets;
                  Arrays.sort(offKeys, new Comparator<Integer>() {
                     public int compare(Integer object1, Integer object2) {
                        return Double.compare((double)foffsets.get(object1), (double)foffsets.get(object2));
                     }
                  });
                  int p = 45;
                  if (p < offKeys.length) {
                     for(int i = p + 15; i <= offKeys.length; i += 15) {
                        Arrays.sort((Object[])offKeys, p, i);
                        p = i;
                     }

                     Arrays.sort((Object[])offKeys, p, offKeys.length);
                  }
               }

               LOG.info("Searched poi structure in " + (System.currentTimeMillis() - time) + "ms. Found " + offKeys.length + " subtrees");

               for(int j = 0; j < offKeys.length; ++j) {
                  this.codedIS.seek((long)(offKeys[j] + indexOffset));
                  int len = this.readInt();
                  int oldLim = this.codedIS.pushLimit(len);
                  this.readPoiData(matcher, req, region);
                  this.codedIS.popLimit(oldLim);
                  if (req.isCancelled() || req.limitExceeded()) {
                     return;
                  }
               }

               LOG.info("Whole poi by name search is done in " + (System.currentTimeMillis() - time) + "ms. Found " + req.getSearchResults().size());
               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private TIntLongHashMap readPoiNameIndex(Collator instance, String query, BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      TIntLongHashMap offsets = new TIntLongHashMap();
      List<TIntArrayList> listOffsets = null;
      List<TIntLongHashMap> listOfSepOffsets = new ArrayList();
      int offset = 0;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return offsets;
            case 3:
               int length = this.readInt();
               int oldLimit = this.codedIS.pushLimit(length);
               offset = this.codedIS.getTotalBytesRead();
               List<String> queries = Algorithms.splitByWordsLowercase(query);
               TIntArrayList charsList = new TIntArrayList(queries.size());
               listOffsets = new ArrayList(queries.size());

               while(listOffsets.size() < queries.size()) {
                  charsList.add(0);
                  listOffsets.add(new TIntArrayList());
               }

               this.map.readIndexedStringTable(instance, queries, "", listOffsets, charsList);
               this.codedIS.popLimit(oldLimit);
               break;
            case 5:
               if (listOffsets != null) {
                  for(TIntArrayList dataOffsets : listOffsets) {
                     TIntLongHashMap offsetMap = new TIntLongHashMap();
                     listOfSepOffsets.add(offsetMap);
                     dataOffsets.sort();

                     for(int i = 0; i < dataOffsets.size(); ++i) {
                        this.codedIS.seek((long)(dataOffsets.get(i) + offset));
                        int len = this.codedIS.readRawVarint32();
                        int oldLim = this.codedIS.pushLimit(len);
                        this.readPoiNameIndexData(offsetMap, req);
                        this.codedIS.popLimit(oldLim);
                        if (req.isCancelled()) {
                           this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                           return offsets;
                        }
                     }
                  }
               }

               if (listOfSepOffsets.size() > 0) {
                  offsets.putAll((TIntLongMap)listOfSepOffsets.get(0));

                  for(int j = 1; j < listOfSepOffsets.size(); ++j) {
                     TIntLongHashMap mp = (TIntLongHashMap)listOfSepOffsets.get(j);

                     for(int chKey : offsets.keys()) {
                        if (!mp.containsKey(chKey)) {
                           offsets.remove(chKey);
                        }
                     }
                  }
               }

               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return offsets;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private void readPoiNameIndexData(TIntLongHashMap offsets, BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 3:
               int len = this.codedIS.readRawVarint32();
               int oldLim = this.codedIS.pushLimit(len);
               this.readPoiNameIndexDataAtom(offsets, req);
               this.codedIS.popLimit(oldLim);
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private void readPoiNameIndexDataAtom(TIntLongHashMap offsets, BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      int x = 0;
      int y = 0;
      int zoom = 15;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            default:
               this.skipUnknownField(t);
               break;
            case 2:
               zoom = this.codedIS.readUInt32();
               break;
            case 3:
               x = this.codedIS.readUInt32();
               break;
            case 4:
               y = this.codedIS.readUInt32();
               break;
            case 14:
               int x31 = x << 31 - zoom;
               int y31 = y << 31 - zoom;
               int shift = this.readInt();
               if (req.contains(x31, y31, x31, y31)) {
                  long d = (long)(Math.abs(req.x - x31) + Math.abs(req.y - y31));
                  offsets.put(shift, d);
               }
         }
      }
   }

   protected void searchPoiIndex(
      int left31, int right31, int top31, int bottom31, BinaryMapIndexReader.SearchRequest<Amenity> req, BinaryMapPoiReaderAdapter.PoiRegion region
   ) throws IOException {
      int indexOffset = this.codedIS.getTotalBytesRead();
      long time = System.currentTimeMillis();
      TLongHashSet skipTiles = null;
      if (req.zoom >= 0 && req.zoom < 16) {
         skipTiles = new TLongHashSet();
      }

      TIntLongHashMap offsetsMap = new TIntLongHashMap();

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 6:
               int length = this.readInt();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readBoxField(left31, right31, top31, bottom31, 0, 0, 0, offsetsMap, skipTiles, req, region);
               this.codedIS.popLimit(oldLimit);
               break;
            case 9:
               int[] offsets = offsetsMap.keys();
               Arrays.sort(offsets);
               if (skipTiles != null) {
                  skipTiles.clear();
               }

               LOG.info("Searched poi structure in " + (System.currentTimeMillis() - time) + " ms. Found " + offsets.length + " subtrees");

               for(int j = 0; j < offsets.length; ++j) {
                  long skipVal = offsetsMap.get(offsets[j]);
                  if (skipTiles != null && skipVal != -1L) {
                     int dzoom = 3;
                     long dx = skipVal >> 6;
                     long dy = skipVal - (dx << 6);
                     skipVal = dx >> dzoom << 3 | dy >> dzoom;
                     if (skipVal != -1L && skipTiles.contains(skipVal)) {
                        continue;
                     }
                  }

                  this.codedIS.seek((long)(offsets[j] + indexOffset));
                  int len = this.readInt();
                  int oldLim = this.codedIS.pushLimit(len);
                  boolean read = this.readPoiData(left31, right31, top31, bottom31, req, region, skipTiles, req.zoom == -1 ? 31 : req.zoom + 3);
                  if (read && skipVal != -1L && skipTiles != null) {
                     skipTiles.add(skipVal);
                  }

                  this.codedIS.popLimit(oldLim);
                  if (req.isCancelled()) {
                     return;
                  }
               }

               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private void readPoiData(CollatorStringMatcher matcher, BinaryMapIndexReader.SearchRequest<Amenity> req, BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      int x = 0;
      int y = 0;
      int zoom = 0;

      while(!req.isCancelled() && !req.limitExceeded()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               zoom = this.codedIS.readUInt32();
               break;
            case 2:
               x = this.codedIS.readUInt32();
               break;
            case 3:
               y = this.codedIS.readUInt32();
               break;
            case 4:
            default:
               this.skipUnknownField(t);
               break;
            case 5:
               int len = this.codedIS.readRawVarint32();
               int oldLim = this.codedIS.pushLimit(len);
               Amenity am = this.readPoiPoint(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, x, y, zoom, req, region, false);
               this.codedIS.popLimit(oldLim);
               if (am != null) {
                  boolean matches = matcher.matches(am.getName().toLowerCase()) || matcher.matches(am.getEnName(true).toLowerCase());
                  if (!matches) {
                     for(String s : am.getOtherNames()) {
                        matches = matcher.matches(s.toLowerCase());
                        if (matches) {
                           break;
                        }
                     }

                     if (!matches) {
                        for(String key : am.getAdditionalInfoKeys()) {
                           if (key.contains("_name") || key.equals("brand")) {
                              matches = matcher.matches(am.getAdditionalInfo(key));
                              if (matches) {
                                 break;
                              }
                           }
                        }
                     }
                  }

                  if (matches) {
                     req.collectRawData(am);
                     req.publish(am);
                  }
               }
         }
      }
   }

   private boolean readPoiData(
      int left31,
      int right31,
      int top31,
      int bottom31,
      BinaryMapIndexReader.SearchRequest<Amenity> req,
      BinaryMapPoiReaderAdapter.PoiRegion region,
      TLongHashSet toSkip,
      int zSkip
   ) throws IOException {
      int x = 0;
      int y = 0;
      int zoom = 0;
      boolean read = false;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return read;
            case 1:
               zoom = this.codedIS.readUInt32();
               break;
            case 2:
               x = this.codedIS.readUInt32();
               break;
            case 3:
               y = this.codedIS.readUInt32();
               break;
            case 4:
            default:
               this.skipUnknownField(t);
               break;
            case 5:
               int len = this.codedIS.readRawVarint32();
               int oldLim = this.codedIS.pushLimit(len);
               Amenity am = this.readPoiPoint(left31, right31, top31, bottom31, x, y, zoom, req, region, true);
               this.codedIS.popLimit(oldLim);
               if (am != null) {
                  if (toSkip != null) {
                     int xp = (int)MapUtils.getTileNumberX((float)zSkip, am.getLocation().getLongitude());
                     int yp = (int)MapUtils.getTileNumberY((float)zSkip, am.getLocation().getLatitude());
                     long valSkip = (long)xp << zSkip | (long)yp;
                     if (!toSkip.contains(valSkip)) {
                        req.collectRawData(am);
                        boolean publish = req.publish(am);
                        if (publish) {
                           read = true;
                           toSkip.add(valSkip);
                        }
                     } else if (zSkip <= zoom) {
                        this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                        return read;
                     }
                  } else {
                     req.collectRawData(am);
                     if (req.publish(am)) {
                        read = true;
                     }
                  }
               }
         }
      }

      return read;
   }

   private Amenity.AmenityRoutePoint dist(LatLon l, List<Location> locations, double radius) {
      float dist = (float)(radius + 0.1);
      Amenity.AmenityRoutePoint arp = null;

      for(int i = 1; i < locations.size(); i += 2) {
         float d = (float)MapUtils.getOrthogonalDistance(
            l.getLatitude(),
            l.getLongitude(),
            locations.get(i - 1).getLatitude(),
            locations.get(i - 1).getLongitude(),
            locations.get(i).getLatitude(),
            locations.get(i).getLongitude()
         );
         if (d < dist) {
            arp = new Amenity.AmenityRoutePoint();
            dist = d;
            arp.deviateDistance = (double)d;
            arp.pointA = locations.get(i - 1);
            arp.pointB = locations.get(i);
         }
      }

      if (arp != null && arp.deviateDistance != 0.0 && arp.pointA != null && arp.pointB != null) {
         arp.deviationDirectionRight = MapUtils.rightSide(
            l.getLatitude(), l.getLongitude(), arp.pointA.getLatitude(), arp.pointA.getLongitude(), arp.pointB.getLatitude(), arp.pointB.getLongitude()
         );
      }

      return arp;
   }

   private Amenity readPoiPoint(
      int left31,
      int right31,
      int top31,
      int bottom31,
      int px,
      int py,
      int zoom,
      BinaryMapIndexReader.SearchRequest<Amenity> req,
      BinaryMapPoiReaderAdapter.PoiRegion region,
      boolean checkBounds
   ) throws IOException {
      Amenity am = null;
      int x = 0;
      int y = 0;
      int precisionXY = 0;
      boolean hasLocation = false;
      StringBuilder retValue = new StringBuilder();
      PoiCategory amenityType = null;
      LinkedList<String> textTags = null;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         if (amenityType == null && (tag > 4 || tag == 0)) {
            this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
            return null;
         }

         switch(tag) {
            case 0:
               ++req.numberOfAcceptedObjects;
               if (hasLocation) {
                  if (precisionXY != 0) {
                     int[] xy = MapUtils.calculateFinalXYFromBaseAndPrecisionXY(24, 26, precisionXY, x >> 7, y >> 7, true);
                     int x31 = xy[0] << 5;
                     int y31 = xy[1] << 5;
                     am.setLocation(MapUtils.get31LatitudeY(y31), MapUtils.get31LongitudeX(x31));
                  } else {
                     am.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
                  }

                  if (req.radius > 0.0) {
                     LatLon loc = am.getLocation();
                     List<Location> locs = (List)req.tiles.get(req.getTileHashOnPath(loc.getLatitude(), loc.getLongitude()));
                     if (locs == null) {
                        return null;
                     }

                     Amenity.AmenityRoutePoint arp = this.dist(am.getLocation(), locs, req.radius);
                     if (arp == null) {
                        return null;
                     }

                     am.setRoutePoint(arp);
                  }

                  return am;
               }

               return null;
            case 1:
            case 9:
            default:
               this.skipUnknownField(t);
               break;
            case 2:
               x = this.codedIS.readSInt32() + (px << 24 - zoom) << 7;
               break;
            case 3:
               y = this.codedIS.readSInt32() + (py << 24 - zoom) << 7;
               ++req.numberOfVisitedObjects;
               if (checkBounds && (left31 > x || right31 < x || top31 > y || bottom31 < y)) {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                  return null;
               }

               am = new Amenity();
               hasLocation = true;
               break;
            case 4:
               int cat = this.codedIS.readUInt32();
               int subcatId = cat >> 7;
               int catId = cat & 127;
               PoiCategory type = this.poiTypes.getOtherPoiCategory();
               String subtype = "";
               if (catId < region.categoriesType.size()) {
                  type = region.categoriesType.get(catId);
                  List<String> subcats = region.subcategories.get(catId);
                  if (subcatId < subcats.size()) {
                     subtype = subcats.get(subcatId);
                  }
               }

               subtype = this.poiTypes.replaceDeprecatedSubtype(type, subtype);
               boolean isForbidden = this.poiTypes.isTypeForbidden(subtype);
               if (!isForbidden && (req.poiTypeFilter == null || req.poiTypeFilter.accept(type, subtype))) {
                  if (amenityType == null) {
                     amenityType = type;
                     am.setSubType(subtype);
                     am.setType(type);
                  } else {
                     am.setSubType(am.getSubType() + ";" + subtype);
                  }
               }
               break;
            case 5:
               int subtypev = this.codedIS.readUInt32();
               retValue.setLength(0);
               BinaryMapPoiReaderAdapter.PoiSubType st = region.getSubtypeFromId(subtypev, retValue);
               if (st != null) {
                  am.setAdditionalInfo(st.name, retValue.toString());
               }
               break;
            case 6:
               am.setName(this.codedIS.readString());
               break;
            case 7:
               am.setEnName(this.codedIS.readString());
               break;
            case 8:
               am.setId(Long.valueOf(this.codedIS.readUInt64()));
               break;
            case 10:
               am.setOpeningHours(this.codedIS.readString());
               break;
            case 11:
               am.setSite(this.codedIS.readString());
               break;
            case 12:
               am.setPhone(this.codedIS.readString());
               break;
            case 13:
               am.setDescription(this.codedIS.readString());
               break;
            case 14:
               int texttypev = this.codedIS.readUInt32();
               retValue.setLength(0);
               BinaryMapPoiReaderAdapter.PoiSubType textt = region.getSubtypeFromId(texttypev, retValue);
               if (textt != null && textt.text) {
                  if (textTags == null) {
                     textTags = new LinkedList<>();
                  }

                  textTags.add(textt.name);
               }
               break;
            case 15:
               String str = this.codedIS.readString();
               if (textTags != null && !textTags.isEmpty()) {
                  am.setAdditionalInfo(textTags.poll(), str);
               }
               break;
            case 16:
               if (hasLocation) {
                  precisionXY = this.codedIS.readInt32();
               }
         }
      }
   }

   private boolean checkCategories(BinaryMapIndexReader.SearchRequest<Amenity> req, BinaryMapPoiReaderAdapter.PoiRegion region) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return false;
            case 3:
               PoiCategory type = this.poiTypes.getOtherPoiCategory();
               String subtype = "";
               int cat = this.codedIS.readUInt32();
               int subcatId = cat >> 7;
               int catId = cat & 127;
               if (catId < region.categoriesType.size()) {
                  type = region.categoriesType.get(catId);
                  List<String> subcats = region.subcategories.get(catId);
                  if (subcatId < subcats.size()) {
                     subtype = subcats.get(subcatId);
                  }
               }

               subtype = this.poiTypes.replaceDeprecatedSubtype(type, subtype);
               if (!req.poiTypeFilter.accept(type, subtype)) {
                  break;
               }

               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return true;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private boolean readBoxField(
      int left31,
      int right31,
      int top31,
      int bottom31,
      int px,
      int py,
      int pzoom,
      TIntLongHashMap offsetsMap,
      TLongHashSet skipTiles,
      BinaryMapIndexReader.SearchRequest<Amenity> req,
      BinaryMapPoiReaderAdapter.PoiRegion region
   ) throws IOException {
      ++req.numberOfReadSubtrees;
      int zoomToSkip = req.zoom == -1 ? 31 : req.zoom + 6;
      boolean checkBox = true;
      boolean existsCategories = false;
      int zoom = pzoom;
      int dy = py;
      int dx = px;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return existsCategories;
            case 1:
               zoom = this.codedIS.readUInt32() + pzoom;
               break;
            case 2:
               dx = this.codedIS.readSInt32();
               break;
            case 3:
               dy = this.codedIS.readSInt32();
               break;
            case 4:
               if (req.poiTypeFilter == null) {
                  this.skipUnknownField(t);
               } else {
                  int length = this.codedIS.readRawVarint32();
                  int oldLimit = this.codedIS.pushLimit(length);
                  boolean check = this.checkCategories(req, region);
                  this.codedIS.popLimit(oldLimit);
                  if (!check) {
                     this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                     return false;
                  }

                  existsCategories = true;
               }
               break;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
            case 13:
            default:
               this.skipUnknownField(t);
               break;
            case 10:
               int x = dx + (px << zoom - pzoom);
               int y = dy + (py << zoom - pzoom);
               if (checkBox) {
                  int xL = x << 31 - zoom;
                  int xR = (x + 1 << 31 - zoom) - 1;
                  int yT = y << 31 - zoom;
                  int yB = (y + 1 << 31 - zoom) - 1;
                  if (left31 > xR || xL > right31 || bottom31 < yT || yB < top31) {
                     this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                     return false;
                  }

                  ++req.numberOfAcceptedSubtrees;
                  checkBox = false;
               }

               int length = this.readInt();
               int oldLimit = this.codedIS.pushLimit(length);
               boolean exists = this.readBoxField(left31, right31, top31, bottom31, x, y, zoom, offsetsMap, skipTiles, req, region);
               this.codedIS.popLimit(oldLimit);
               if (skipTiles != null && zoom >= zoomToSkip && exists) {
                  long val = (long)x >> zoom - zoomToSkip << zoomToSkip | (long)y >> zoom - zoomToSkip;
                  if (skipTiles.contains(val)) {
                     this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                     return true;
                  }
               }
               break;
            case 14:
               int x14 = dx + (px << zoom - pzoom);
               int y14 = dy + (py << zoom - pzoom);
               boolean read = true;
               if (req.tiles != null) {
                  long zx = (long)((long) x14 << 16 - zoom);
                  long zy = (long)((long) y14 << 16 - zoom);
                  read = req.tiles.contains((zx << 16) + zy);
               }

               int offset = this.readInt();
               if (read) {
                  if (skipTiles != null && zoom >= zoomToSkip) {
                     long valSkip = (long)x14 >> zoom - zoomToSkip << zoomToSkip | (long)y14 >> zoom - zoomToSkip;
                     offsetsMap.put(offset, valSkip);
                     skipTiles.add(valSkip);
                  } else {
                     offsetsMap.put(offset, -1L);
                  }
               }
         }
      }

      return false;
   }

   public static class PoiRegion extends BinaryIndexPart {
      List<String> categories = new ArrayList<>();
      List<PoiCategory> categoriesType = new ArrayList<>();
      List<List<String>> subcategories = new ArrayList<>();
      List<BinaryMapPoiReaderAdapter.PoiSubType> subTypes = new ArrayList<>();
      List<PoiSubType> topIndexSubTypes = new ArrayList<PoiSubType>();
      int left31;
      int right31;
      int top31;
      int bottom31;

      public int getLeft31() {
         return this.left31;
      }

      public int getRight31() {
         return this.right31;
      }

      public int getTop31() {
         return this.top31;
      }

      public int getBottom31() {
         return this.bottom31;
      }

      @Override
      public String getPartName() {
         return "POI";
      }

      public List<String> getCategories() {
         return this.categories;
      }

      public List<List<String>> getSubcategories() {
         return this.subcategories;
      }

      public List<BinaryMapPoiReaderAdapter.PoiSubType> getSubTypes() {
         return this.subTypes;
      }

      public List<PoiSubType> getTopIndexSubTypes() {
         return topIndexSubTypes;
      }

      @Override
      public int getFieldNumber() {
         return 8;
      }

      public BinaryMapPoiReaderAdapter.PoiSubType getSubtypeFromId(int id, StringBuilder returnValue) {
         int tl;
         int sl;
         if (id % 2 == 0) {
            tl = id >> 1 & 31;
            sl = id >> 6;
         } else {
            tl = id >> 1 & 65535;
            sl = id >> 16;
         }

         if (this.subTypes.size() > tl) {
            BinaryMapPoiReaderAdapter.PoiSubType st = this.subTypes.get(tl);
            if (st.text) {
               return st;
            }

            if (st.possibleValues != null && st.possibleValues.size() > sl) {
               returnValue.append(st.possibleValues.get(sl));
               return st;
            }
         }

         return null;
      }
   }

   public static class PoiSubType {
      public boolean text;
      public String name;
      public List<String> possibleValues = null;
   }
}
