package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.osmand.CollatorStringMatcher;
import net.osmand.StringMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Postcode;
import net.osmand.data.Street;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;
import org.apache.commons.logging.Log;

public class BinaryMapAddressReaderAdapter {
   public static final int CITY_TOWN_TYPE = 1;
   public static final int POSTCODES_TYPE = 2;
   public static final int VILLAGES_TYPE = 3;
   public static final int STREET_TYPE = 4;
   public static final List<Integer> TYPES = Arrays.asList(1, 2, 3, 4);
   public static final int[] CITY_TYPES = new int[]{1, 2, 3};
   private CodedInputStream codedIS;
   private final BinaryMapIndexReader map;

   protected BinaryMapAddressReaderAdapter(BinaryMapIndexReader map) {
      this.codedIS = map.codedIS;
      this.map = map;
   }

   private void skipUnknownField(int t) throws IOException {
      this.map.skipUnknownField(t);
   }

   private int readInt() throws IOException {
      return this.map.readInt();
   }

   private void readBoundariesIndex(BinaryMapAddressReaderAdapter.AddressRegion region) throws IOException {
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

   protected void readAddressIndex(BinaryMapAddressReaderAdapter.AddressRegion region) throws IOException {
      label42:
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (region.enName == null || region.enName.length() == 0) {
                  region.enName = region.name == null ? "" : TransliterationHelper.transliterate(region.name);
               }

               return;
            case 1:
               region.name = this.codedIS.readString();
               break;
            case 2:
               region.enName = this.codedIS.readString();
               break;
            case 3: {
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readBoundariesIndex(region);
               this.codedIS.popLimit(oldLimit);
               region.enName = this.codedIS.readString();
               break;
            }
            case 4: {
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               region.attributeTagsTable = this.map.readStringTable();
               this.codedIS.popLimit(oldLimit);
               break;
            }
            case 5:
            default:
               this.skipUnknownField(t);
               break;
            case 6:
               BinaryMapAddressReaderAdapter.CitiesBlock block = new BinaryMapAddressReaderAdapter.CitiesBlock();
               region.cities.add(block);
               block.type = 1;
               block.length = this.readInt();
               block.filePointer = this.codedIS.getTotalBytesRead();

               while(true) {
                  int tt = this.codedIS.readTag();
                  int ttag = WireFormat.getTagFieldNumber(tt);
                  if (ttag != 0) {
                     if (ttag != 2) {
                        this.skipUnknownField(tt);
                        continue;
                     }

                     block.type = this.codedIS.readUInt32();
                  }

                  this.codedIS.seek((long)(block.filePointer + block.length));
                  continue label42;
               }
            case 7: {
               region.indexNameOffset = this.codedIS.getTotalBytesRead();
               int length = this.readInt();
               this.codedIS.seek((long)(region.indexNameOffset + length + 4));
            }
         }
      }
   }

   protected void readCities(
      List<City> cities, BinaryMapIndexReader.SearchRequest<City> resultMatcher, StringMatcher matcher, List<String> additionalTagsTable
   ) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 5:
               int fp = this.codedIS.getTotalBytesRead();
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               City c = this.readCityHeader(resultMatcher, new BinaryMapAddressReaderAdapter.DefaultCityMatcher(matcher), fp, additionalTagsTable);
               if (c != null && (resultMatcher == null || resultMatcher.publish(c))) {
                  cities.add(c);
               }

               this.codedIS.popLimit(oldLimit);
               if (resultMatcher != null && resultMatcher.isCancelled()) {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               }
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected void readCityStreets(BinaryMapIndexReader.SearchRequest<Street> resultMatcher, City city, List<String> attributeTagsTable) throws IOException {
      int x = MapUtils.get31TileNumberX(city.getLocation().getLongitude());
      int y = MapUtils.get31TileNumberY(city.getLocation().getLatitude());

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 10:
               this.skipUnknownField(t);
            default:
               this.skipUnknownField(t);
               break;
            case 12:
               Street s = new Street(city);
               s.setFileOffset(this.codedIS.getTotalBytesRead());
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               this.readStreet(s, null, false, x >> 7, y >> 7, city.isPostcode() ? city.getName() : null, attributeTagsTable);
               this.publishRawData(resultMatcher, s);
               if (resultMatcher == null || resultMatcher.publish(s)) {
                  city.registerStreet(s);
               }

               if (resultMatcher != null && resultMatcher.isCancelled()) {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               }

               this.codedIS.popLimit(oldLimit);
         }
      }
   }

   protected City readCityHeader(
      BinaryMapIndexReader.SearchRequest<? super City> resultMatcher,
      BinaryMapAddressReaderAdapter.CityMatcher matcher,
      int filePointer,
      List<String> additionalTagsTable
   ) throws IOException {
      int x = 0;
      int y = 0;
      City c = null;
      LinkedList<String> additionalTags = null;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               this.publishRawData(resultMatcher, c);
               return matcher != null && !matcher.matches(c) ? null : c;
            case 1:
               int type = this.codedIS.readUInt32();
               c = new City(City.CityType.values()[type]);
               break;
            case 2:
               String name = this.codedIS.readString();
               if (c == null) {
                  c = City.createPostcode(name);
               }

               c.setName(name);
               break;
            case 3:
               String enName = this.codedIS.readString();
               c.setEnName(enName);
               break;
            case 4:
               c.setId(Long.valueOf(this.codedIS.readUInt64()));
               break;
            case 5:
               x = this.codedIS.readUInt32();
               break;
            case 6:
               y = this.codedIS.readUInt32();
               c.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
               break;
            case 7:
               int tgid = this.codedIS.readUInt32();
               if (additionalTags == null) {
                  additionalTags = new LinkedList<>();
               }

               if (additionalTagsTable != null && tgid < additionalTagsTable.size()) {
                  additionalTags.add(additionalTagsTable.get(tgid));
               }
               break;
            case 8:
               String nm = this.codedIS.readString();
               if (additionalTags != null && additionalTags.size() > 0) {
                  String tg = additionalTags.pollFirst();
                  if (tg.startsWith("name:")) {
                     c.setName(tg.substring("name:".length()), nm);
                  }
               }
               break;
            case 9:
            default:
               this.skipUnknownField(t);
               break;
            case 10:
               int offset = this.readInt();
               offset += filePointer;
               c.setFileOffset(offset);
         }
      }
   }

   protected Street readStreet(
      Street s,
      BinaryMapIndexReader.SearchRequest<Building> buildingsMatcher,
      boolean loadBuildingsAndIntersected,
      int city24X,
      int city24Y,
      String postcodeFilter,
      List<String> additionalTagsTable
   ) throws IOException {
      int x = 0;
      int y = 0;
      LinkedList<String> additionalTags = null;
      boolean loadLocation = city24X != 0 || city24Y != 0;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (loadLocation) {
                  s.setLocation(MapUtils.getLatitudeFromTile(24.0F, (double)y), MapUtils.getLongitudeFromTile(24.0, (double)x));
               }

               return s;
            case 1:
               s.setName(this.codedIS.readString());
               break;
            case 2:
               s.setEnName(this.codedIS.readString());
               break;
            case 3:
               int sx = this.codedIS.readSInt32();
               if (loadLocation) {
                  x = sx + city24X;
               } else {
                  x = (int)MapUtils.getTileNumberX(24.0F, s.getLocation().getLongitude());
               }
               break;
            case 4:
               int sy = this.codedIS.readSInt32();
               if (loadLocation) {
                  y = sy + city24Y;
               } else {
                  y = (int)MapUtils.getTileNumberY(24.0F, s.getLocation().getLatitude());
               }
               break;
            case 5:
               int length = this.codedIS.readRawVarint32();
               if (loadBuildingsAndIntersected) {
                  int oldLimit = this.codedIS.pushLimit(length);
                  Street si = this.readIntersectedStreet(s.getCity(), x, y, additionalTagsTable);
                  s.addIntersectedStreet(si);
                  this.codedIS.popLimit(oldLimit);
               } else {
                  this.codedIS.skipRawBytes(length);
               }
               break;
            case 6:
               s.setId(Long.valueOf(this.codedIS.readUInt64()));
               break;
            case 7:
               int tgid = this.codedIS.readUInt32();
               if (additionalTags == null) {
                  additionalTags = new LinkedList<>();
               }

               if (additionalTagsTable != null && tgid < additionalTagsTable.size()) {
                  additionalTags.add(additionalTagsTable.get(tgid));
               }
               break;
            case 8:
               String nm = this.codedIS.readString();
               if (additionalTags != null && additionalTags.size() > 0) {
                  String tg = additionalTags.pollFirst();
                  if (tg.startsWith("name:")) {
                     s.setName(tg.substring("name:".length()), nm);
                  }
               }
               break;
            case 9:
            case 10:
            case 11:
            default:
               this.skipUnknownField(t);
               break;
            case 12:
               int offset = this.codedIS.getTotalBytesRead();
               int bytesLength = this.codedIS.readRawVarint32();
               if (!loadBuildingsAndIntersected) {
                  this.codedIS.skipRawBytes(bytesLength);
               } else {
                  int oldLimit = this.codedIS.pushLimit(bytesLength);
                  Building b = this.readBuilding(offset, x, y, additionalTagsTable);
                  this.publishRawData(buildingsMatcher, b);
                  if ((postcodeFilter == null || postcodeFilter.equalsIgnoreCase(b.getPostcode()))
                     && (buildingsMatcher == null || buildingsMatcher.publish(b))) {
                     s.addBuilding(b);
                  }

                  this.codedIS.popLimit(oldLimit);
               }
         }
      }
   }

   protected Street readIntersectedStreet(City c, int street24X, int street24Y, List<String> additionalTagsTable) throws IOException {
      int x = 0;
      int y = 0;
      Street s = new Street(c);
      LinkedList<String> additionalTags = null;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               s.setLocation(MapUtils.getLatitudeFromTile(24.0F, (double)y), MapUtils.getLongitudeFromTile(24.0, (double)x));
               return s;
            case 1:
            case 6:
            case 9:
            case 10:
            case 11:
            case 12:
            default:
               this.skipUnknownField(t);
               break;
            case 2:
               s.setName(this.codedIS.readString());
               break;
            case 3:
               s.setEnName(this.codedIS.readString());
               break;
            case 4:
               x = this.codedIS.readSInt32() + street24X;
               break;
            case 5:
               y = this.codedIS.readSInt32() + street24Y;
               break;
            case 7:
               int tgid = this.codedIS.readUInt32();
               if (additionalTags == null) {
                  additionalTags = new LinkedList<>();
               }

               if (additionalTagsTable != null && tgid < additionalTagsTable.size()) {
                  additionalTags.add(additionalTagsTable.get(tgid));
               }
               break;
            case 8:
               String nm = this.codedIS.readString();
               if (additionalTags != null && additionalTags.size() > 0) {
                  String tg = additionalTags.pollFirst();
                  if (tg.startsWith("name:")) {
                     s.setName(tg.substring("name:".length()), nm);
                  }
               }
               break;
            case 13:
               s.setId(Long.valueOf(this.codedIS.readUInt64()));
         }
      }
   }

   protected Building readBuilding(int fileOffset, int street24X, int street24Y, List<String> additionalTagsTable) throws IOException {
      int x = 0;
      int y = 0;
      int x2 = 0;
      int y2 = 0;
      LinkedList<String> additionalTags = null;
      Building b = new Building();
      b.setFileOffset(fileOffset);

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               b.setLocation(MapUtils.getLatitudeFromTile(24.0F, (double)y), MapUtils.getLongitudeFromTile(24.0, (double)x));
               if (x2 != 0 && y2 != 0) {
                  b.setLatLon2(new LatLon(MapUtils.getLatitudeFromTile(24.0F, (double)y2), MapUtils.getLongitudeFromTile(24.0, (double)x2)));
               }

               return b;
            case 1:
               b.setName(this.codedIS.readString());
               break;
            case 2:
               b.setEnName(this.codedIS.readString());
               break;
            case 3:
               b.setName2(this.codedIS.readString());
               break;
            case 4:
               this.codedIS.readString();
               break;
            case 5:
               int sint = this.codedIS.readSInt32();
               if (sint > 0) {
                  b.setInterpolationInterval(sint);
               } else {
                  b.setInterpolationType(Building.BuildingInterpolation.fromValue(sint));
               }
               break;
            case 6:
            case 11:
            case 12:
            default:
               this.skipUnknownField(t);
               break;
            case 7:
               x = this.codedIS.readSInt32() + street24X;
               break;
            case 8:
               y = this.codedIS.readSInt32() + street24Y;
               break;
            case 9:
               x2 = this.codedIS.readSInt32() + street24X;
               break;
            case 10:
               y2 = this.codedIS.readSInt32() + street24Y;
               break;
            case 13:
               b.setId(Long.valueOf(this.codedIS.readUInt64()));
               break;
            case 14:
               b.setPostcode(this.codedIS.readString());
               break;
            case 15:
               int tgid = this.codedIS.readUInt32();
               if (additionalTags == null) {
                  additionalTags = new LinkedList<>();
               }

               if (additionalTagsTable != null && tgid < additionalTagsTable.size()) {
                  additionalTags.add(additionalTagsTable.get(tgid));
               }
               break;
            case 16:
               String nm = this.codedIS.readString();
               if (additionalTags != null && additionalTags.size() > 0) {
                  String tg = additionalTags.pollFirst();
                  if (tg.startsWith("name:")) {
                     b.setName(tg.substring("name:".length()), nm);
                  }
               }
         }
      }
   }

   public void searchAddressDataByName(
      BinaryMapAddressReaderAdapter.AddressRegion reg, BinaryMapIndexReader.SearchRequest<MapObject> req, List<Integer> typeFilter
   ) throws IOException {
      TIntArrayList loffsets = new TIntArrayList();
      CollatorStringMatcher stringMatcher = new CollatorStringMatcher(req.nameQuery, req.matcherMode);
      String postcode = Postcode.normalize(req.nameQuery, this.map.getCountryName());
      final BinaryMapAddressReaderAdapter.CityMatcher postcodeMatcher = new BinaryMapAddressReaderAdapter.DefaultCityMatcher(
         new CollatorStringMatcher(postcode, req.matcherMode)
      );
      final BinaryMapAddressReaderAdapter.CityMatcher cityMatcher = new BinaryMapAddressReaderAdapter.DefaultCityMatcher(stringMatcher);
      BinaryMapAddressReaderAdapter.CityMatcher cityPostcodeMatcher = new BinaryMapAddressReaderAdapter.CityMatcher() {
         @Override
         public boolean matches(City city) {
            return city.isPostcode() ? postcodeMatcher.matches(city) : cityMatcher.matches(city);
         }
      };
      long time = System.currentTimeMillis();
      int indexOffset = 0;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 4:
               int length = this.readInt();
               indexOffset = this.codedIS.getTotalBytesRead();
               int oldLimit = this.codedIS.pushLimit(length);
               TIntArrayList charsList = new TIntArrayList();
               charsList.add(0);
               this.map
                  .readIndexedStringTable(
                     stringMatcher.getCollator(), Collections.singletonList(req.nameQuery), "", Collections.singletonList(loffsets), charsList
                  );
               this.codedIS.popLimit(oldLimit);
               break;
            case 7:
               loffsets.sort();
               TIntArrayList[] refs = new TIntArrayList[5];
               TIntArrayList[] refsContainer = new TIntArrayList[5];

               for(int i = 0; i < refs.length; ++i) {
                  refs[i] = new TIntArrayList();
                  refsContainer[i] = new TIntArrayList();
               }


               for(int j = 0; j < loffsets.size(); ++j) {
                  int fp = indexOffset + loffsets.get(j);
                  this.codedIS.seek((long)fp);
                  int len = this.codedIS.readRawVarint32();
                  int oldLim = this.codedIS.pushLimit(len);
                  int stag = 0;

                  do {
                     int st = this.codedIS.readTag();
                     stag = WireFormat.getTagFieldNumber(st);
                     if (stag == 4) {
                        int slen = this.codedIS.readRawVarint32();
                        int soldLim = this.codedIS.pushLimit(slen);
                        this.readAddressNameData(req, refs, refsContainer, fp);
                        this.codedIS.popLimit(soldLim);
                     } else if (stag != 0) {
                        this.skipUnknownField(st);
                     }
                  } while(stag != 0);

                  this.codedIS.popLimit(oldLim);
                  if (req.isCancelled()) {
                     return;
                  }
               }

               if (typeFilter == null) {
                  typeFilter = TYPES;
               }

               for(int i = 0; i < typeFilter.size() && !req.isCancelled(); ++i) {
                  TIntArrayList list = refs[typeFilter.get(i)];
                  TIntArrayList listContainer = refsContainer[typeFilter.get(i)];
                  if (typeFilter.get(i) == 4) {
                     TIntLongHashMap mp = new TIntLongHashMap();

                     for(int j = 0; j < list.size(); ++j) {
                        mp.put(list.get(j), (long)listContainer.get(j));
                     }

                     list.sort();

                     for(int j = 0; j < list.size() && !req.isCancelled(); ++j) {
                        int offset = list.get(j);
                        if (j <= 0 || offset != list.get(j - 1)) {
                           int contOffset = (int)mp.get(offset);
                           this.codedIS.seek((long)contOffset);
                           int len = this.codedIS.readRawVarint32();
                           int old = this.codedIS.pushLimit(len);
                           City obj = this.readCityHeader(req, null, contOffset, reg.attributeTagsTable);
                           this.codedIS.popLimit(old);
                           if (obj != null) {
                              this.codedIS.seek((long)offset);
                              contOffset = this.codedIS.readRawVarint32();
                              len = this.codedIS.pushLimit(contOffset);
                              LatLon l = obj.getLocation();
                              Street s = new Street(obj);
                              s.setFileOffset(offset);
                              this.readStreet(
                                 s,
                                 null,
                                 false,
                                 MapUtils.get31TileNumberX(l.getLongitude()) >> 7,
                                 MapUtils.get31TileNumberY(l.getLatitude()) >> 7,
                                 obj.isPostcode() ? obj.getName() : null,
                                 reg.attributeTagsTable
                              );
                              this.publishRawData(req, s);
                              boolean matches = stringMatcher.matches(s.getName());
                              if (!matches) {
                                 for(String n : s.getOtherNames()) {
                                    matches = stringMatcher.matches(n);
                                    if (matches) {
                                       break;
                                    }
                                 }
                              }

                              if (matches) {
                                 req.publish(s);
                              }

                              this.codedIS.popLimit(len);
                           }
                        }
                     }
                  } else {
                     list.sort();
                     TIntSet published = new TIntHashSet();

                     for(int j = 0; j < list.size() && !req.isCancelled(); ++j) {
                        int offset = list.get(j);
                        if (j <= 0 || offset != list.get(j - 1)) {
                           this.codedIS.seek((long)offset);
                           int len = this.codedIS.readRawVarint32();
                           int old = this.codedIS.pushLimit(len);
                           City obj = this.readCityHeader(req, cityPostcodeMatcher, list.get(j), reg.attributeTagsTable);
                           this.publishRawData(req, obj);
                           if (obj != null && !published.contains(offset)) {
                              req.publish(obj);
                              published.add(offset);
                           }

                           this.codedIS.popLimit(old);
                        }
                     }
                  }
               }

               return;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private void readAddressNameData(BinaryMapIndexReader.SearchRequest<MapObject> req, TIntArrayList[] refs, TIntArrayList[] refsContainer, int fp) throws IOException {
      TIntArrayList toAdd = null;
      TIntArrayList toAddCity = null;
      int shiftindex = 0;
      int shiftcityindex = 0;
      boolean add = true;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         if ((tag == 0 || tag == 5) && toAdd != null && add) {
            if (shiftindex != 0) {
               toAdd.add(shiftindex);
            }

            if (shiftcityindex != 0) {
               toAddCity.add(shiftcityindex);
            }
         }

         switch(tag) {
            case 0:
               return;
            case 1:
               this.codedIS.readString();
               continue;
            case 2:
               this.codedIS.readString();
               continue;
            case 3:
               int type = this.codedIS.readInt32();
               toAdd = refs[type];
               toAddCity = refsContainer[type];
               continue;
            case 4:
            default:
               this.skipUnknownField(t);
               continue;
            case 5:
               shiftindex = fp - this.codedIS.readInt32();
               continue;
            case 6:
               if (toAddCity != null) {
                  shiftcityindex = fp - this.codedIS.readInt32();
               }
               continue;
            case 7:
         }

         int in32 = this.codedIS.readInt32();
         int x16 = in32 >>> 16 << 15;
         int y16 = (in32 & 65535) << 15;
         add = !req.isBboxSpecified() || req.contains(x16, y16, x16, y16);
      }
   }

   private <T> void publishRawData(BinaryMapIndexReader.SearchRequest<T> resultMatcher, T obj) {
      if (resultMatcher != null && obj != null) {
         resultMatcher.collectRawData(obj);
      }
   }

   public static class AddressRegion extends BinaryIndexPart {
      String enName;
      int indexNameOffset = -1;
      List<String> attributeTagsTable = new ArrayList<>();
      List<BinaryMapAddressReaderAdapter.CitiesBlock> cities = new ArrayList<>();
      LatLon calculatedCenter = null;
      int bottom31;
      int top31;
      int right31;
      int left31;

      public String getEnName() {
         return this.enName;
      }

      public List<BinaryMapAddressReaderAdapter.CitiesBlock> getCities() {
         return this.cities;
      }

      public List<String> getAttributeTagsTable() {
         return this.attributeTagsTable;
      }

      public int getIndexNameOffset() {
         return this.indexNameOffset;
      }

      @Override
      public String getPartName() {
         return "Address";
      }

      @Override
      public int getFieldNumber() {
         return 7;
      }
   }

   public static class CitiesBlock extends BinaryIndexPart {
      int type;

      public int getType() {
         return this.type;
      }

      @Override
      public String getPartName() {
         return "City";
      }

      @Override
      public int getFieldNumber() {
         return 6;
      }
   }

   interface CityMatcher {
      boolean matches(City var1);
   }

   private class DefaultCityMatcher implements BinaryMapAddressReaderAdapter.CityMatcher {
      private StringMatcher stringMatcher = null;

      DefaultCityMatcher(StringMatcher stringMatcher) {
         this.stringMatcher = stringMatcher;
      }

      @Override
      public boolean matches(City city) {
         if (this.stringMatcher == null) {
            return true;
         } else {
            boolean matches = this.stringMatcher.matches(city.getName());
            if (!matches) {
               for(String n : city.getOtherNames()) {
                  matches = this.stringMatcher.matches(n);
                  if (matches) {
                     break;
                  }
               }
            }

            return matches;
         }
      }
   }
}
