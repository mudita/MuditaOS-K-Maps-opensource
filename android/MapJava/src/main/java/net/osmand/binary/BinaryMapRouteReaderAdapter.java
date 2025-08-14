package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.OpeningHoursParser;
import org.apache.commons.logging.Log;

public class BinaryMapRouteReaderAdapter {
   protected static final Log LOG = PlatformUtil.getLog(BinaryMapRouteReaderAdapter.class);
   private static final int SHIFT_COORDINATES = 4;
   private CodedInputStream codedIS;
   private final BinaryMapIndexReader map;

   protected BinaryMapRouteReaderAdapter(BinaryMapIndexReader map) {
      this.codedIS = map.codedIS;
      this.map = map;
   }

   private void skipUnknownField(int t) throws IOException {
      this.map.skipUnknownField(t);
   }

   private int readInt() throws IOException {
      return this.map.readInt();
   }

   protected void readRouteIndex(BinaryMapRouteReaderAdapter.RouteRegion region) throws IOException {
      int routeEncodingRule = 1;
      int routeEncodingRulesSize = 0;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               region.completeRouteEncodingRules();
               return;
            case 1:
               region.name = this.codedIS.readString();
               break;
            case 2:
               int len = this.codedIS.readInt32();
               if (routeEncodingRulesSize == 0) {
                  routeEncodingRulesSize = this.codedIS.getTotalBytesRead();
               }

               int oldLimit = this.codedIS.pushLimit(len);
               this.readRouteEncodingRule(region, routeEncodingRule++);
               this.codedIS.popLimit(oldLimit);
               region.routeEncodingRulesBytes = this.codedIS.getTotalBytesRead() - routeEncodingRulesSize;
               break;
            case 3:
            case 4:
               BinaryMapRouteReaderAdapter.RouteSubregion subregion = new BinaryMapRouteReaderAdapter.RouteSubregion(region);
               subregion.length = this.readInt();
               subregion.filePointer = this.codedIS.getTotalBytesRead();
               int oldLimit4 = this.codedIS.pushLimit(subregion.length);
               this.readRouteTree(subregion, null, 0, true);
               if (tag == 3) {
                  boolean exist = false;

                  for(BinaryMapRouteReaderAdapter.RouteSubregion s : region.subregions) {
                     if (s.filePointer == subregion.filePointer) {
                        exist = true;
                        break;
                     }
                  }

                  if (!exist) {
                     region.subregions.add(subregion);
                  }
               } else {
                  boolean exist = false;

                  for(BinaryMapRouteReaderAdapter.RouteSubregion s : region.basesubregions) {
                     if (s.filePointer == subregion.filePointer) {
                        exist = true;
                        break;
                     }
                  }

                  if (!exist) {
                     region.basesubregions.add(subregion);
                  }
               }

               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               this.codedIS.popLimit(oldLimit4);
               break;
            case 5:
               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private RouteDataObject readRouteDataObject(BinaryMapRouteReaderAdapter.RouteRegion reg, int pleftx, int ptopy) throws IOException {
      RouteDataObject o = new RouteDataObject(reg);
      TIntArrayList pointsX = new TIntArrayList();
      TIntArrayList pointsY = new TIntArrayList();
      TIntArrayList types = new TIntArrayList();
      List<TIntArrayList> globalpointTypes = new ArrayList();
      List<TIntArrayList> globalpointNames = new ArrayList();

      while(true) {
         int ts = this.codedIS.readTag();
         int tags = WireFormat.getTagFieldNumber(ts);
         switch(tags) {
            case 0:
               o.pointsX = pointsX.toArray();
               o.pointsY = pointsY.toArray();
               o.types = types.toArray();
               if (globalpointTypes.size() > 0) {
                  o.pointTypes = new int[globalpointTypes.size()][];

                  for(int k = 0; k < o.pointTypes.length; ++k) {
                     TIntArrayList l = (TIntArrayList)globalpointTypes.get(k);
                     if (l != null) {
                        o.pointTypes[k] = l.toArray();
                     }
                  }
               }

               if (globalpointNames.size() > 0) {
                  o.pointNames = new String[globalpointNames.size()][];
                  o.pointNameTypes = new int[globalpointNames.size()][];

                  for(int k = 0; k < o.pointNames.length; ++k) {
                     TIntArrayList l = (TIntArrayList)globalpointNames.get(k);
                     if (l != null) {
                        o.pointNameTypes[k] = new int[l.size() / 2];
                        o.pointNames[k] = new String[l.size() / 2];

                        for(int ik = 0; ik < l.size(); ik += 2) {
                           o.pointNameTypes[k][ik / 2] = l.get(ik);
                           o.pointNames[k][ik / 2] = (char)l.get(ik + 1) + "";
                        }
                     }
                  }
               }

               return o;
            case 1:
               int len = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(len);
               int px = pleftx >> 4;

               int y;
               for(int py = ptopy >> 4; this.codedIS.getBytesUntilLimit() > 0; py = y) {
                  int x = this.codedIS.readSInt32() + px;
                  y = this.codedIS.readSInt32() + py;
                  pointsX.add(x << 4);
                  pointsY.add(y << 4);
                  px = x;
               }

               this.codedIS.popLimit(oldLimit);
               break;
            case 2:
            case 3:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 13:
            default:
               this.skipUnknownField(ts);
               break;
            case 4:
               int len4 = this.codedIS.readRawVarint32();
               int oldLimit4 = this.codedIS.pushLimit(len4);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int pointInd = this.codedIS.readRawVarint32();
                  TIntArrayList pointTypes = new TIntArrayList();
                  int lens = this.codedIS.readRawVarint32();
                  int oldLimits = this.codedIS.pushLimit(lens);

                  while(this.codedIS.getBytesUntilLimit() > 0) {
                     pointTypes.add(this.codedIS.readRawVarint32());
                  }

                  this.codedIS.popLimit(oldLimits);

                  while(pointInd >= globalpointTypes.size()) {
                     globalpointTypes.add(null);
                  }

                  globalpointTypes.set(pointInd, pointTypes);
               }

               this.codedIS.popLimit(oldLimit4);
               break;
            case 5:
               int len5 = this.codedIS.readRawVarint32();
               int oldLimit5 = this.codedIS.pushLimit(len5);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int pointInd = this.codedIS.readRawVarint32();
                  int pointNameType = this.codedIS.readRawVarint32();
                  int nameInd = this.codedIS.readRawVarint32();

                  while(pointInd >= globalpointNames.size()) {
                     globalpointNames.add(null);
                  }

                  if (globalpointNames.get(pointInd) == null) {
                     TIntArrayList pointTypes = new TIntArrayList();
                     globalpointNames.set(pointInd, pointTypes);
                  }

                  ((TIntArrayList)globalpointNames.get(pointInd)).add(pointNameType);
                  ((TIntArrayList)globalpointNames.get(pointInd)).add(nameInd);
               }

               this.codedIS.popLimit(oldLimit5);
               break;
            case 7:
               int len7 = this.codedIS.readRawVarint32();
               int oldLimit7 = this.codedIS.pushLimit(len7);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  types.add(this.codedIS.readRawVarint32());
               }

               this.codedIS.popLimit(oldLimit7);
               break;
            case 12:
               o.id = (long)this.codedIS.readInt32();
               break;
            case 14:
               o.names = new TIntObjectHashMap();
               int sizeL = this.codedIS.readRawVarint32();
               int old = this.codedIS.pushLimit(sizeL);
               TIntArrayList list = new TIntArrayList();

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int stag = this.codedIS.readRawVarint32();
                  int pId = this.codedIS.readRawVarint32();
                  o.names.put(stag, (char)pId + "");
                  list.add(stag);
               }

               o.nameIds = list.toArray();
               this.codedIS.popLimit(old);
         }
      }
   }

   private void readRouteTreeData(
      BinaryMapRouteReaderAdapter.RouteSubregion routeTree, TLongArrayList idTables, TLongObjectHashMap<RouteDataObject.RestrictionInfo> restrictions
   ) throws IOException {
      routeTree.dataObjects = new ArrayList<>();
      idTables.clear();
      restrictions.clear();
      List<String> stringTable = null;

      label135:
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               TLongObjectIterator<RouteDataObject.RestrictionInfo> it = restrictions.iterator();

               while(it.hasNext()) {
                  it.advance();
                  int from = (int)it.key();
                  RouteDataObject fromr = routeTree.dataObjects.get(from);
                  fromr.restrictions = new long[((RouteDataObject.RestrictionInfo)it.value()).length()];
                  RouteDataObject.RestrictionInfo val = (RouteDataObject.RestrictionInfo)it.value();

                  for(int k = 0; k < fromr.restrictions.length; ++k) {
                     if (val != null) {
                        long via = 0L;
                        if (val.viaWay != 0L) {
                           via = idTables.get((int)val.viaWay);
                        }

                        fromr.setRestriction(k, idTables.get((int)val.toWay), val.type, via);
                     }

                     val = val.next;
                  }
               }

               for(RouteDataObject o : routeTree.dataObjects) {
                  if (o != null) {
                     if (o.id < (long)idTables.size()) {
                        o.id = idTables.get((int)o.id);
                     }

                     if (o.names != null && stringTable != null) {
                        int[] keys = o.names.keys();

                        for(int j = 0; j < keys.length; ++j) {
                           o.names.put(keys[j], stringTable.get(((String)o.names.get(keys[j])).charAt(0)));
                        }
                     }

                     if (o.pointNames != null && stringTable != null) {
                        for(String[] ar : o.pointNames) {
                           if (ar != null) {
                              for(int j = 0; j < ar.length; ++j) {
                                 ar[j] = stringTable.get(ar[j].charAt(0));
                              }
                           }
                        }
                     }
                  }
               }

               return;
            case 1:
            case 2:
            case 3:
            case 4:
            default:
               this.skipUnknownField(t);
               break;
            case 5:
               long routeId = 0L;
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);

               while(true) {
                  int ts = this.codedIS.readTag();
                  int tags = WireFormat.getTagFieldNumber(ts);
                  switch(tags) {
                     case 0:
                        this.codedIS.popLimit(oldLimit);
                        continue label135;
                     case 1:
                        routeId += this.codedIS.readSInt64();
                        idTables.add(routeId);
                        break;
                     default:
                        this.skipUnknownField(ts);
                  }
               }
            case 6: {
               int length6 = this.codedIS.readRawVarint32();
               int oldLimit6 = this.codedIS.pushLimit(length6);
               RouteDataObject obj = this.readRouteDataObject(routeTree.routeReg, routeTree.left, routeTree.top);

               while(obj.id >= (long)routeTree.dataObjects.size()) {
                  routeTree.dataObjects.add(null);
               }

               routeTree.dataObjects.set((int)obj.id, obj);
               this.codedIS.popLimit(oldLimit6);
               break;
            }
            case 7: {
               int length7 = this.codedIS.readRawVarint32();
               int oldLimit7 = this.codedIS.pushLimit(length7);
               RouteDataObject.RestrictionInfo ri = new RouteDataObject.RestrictionInfo();
               long from = 0L;

               while(true) {
                  int ts = this.codedIS.readTag();
                  int tags = WireFormat.getTagFieldNumber(ts);
                  switch(tags) {
                     case 0:
                        RouteDataObject.RestrictionInfo prev = (RouteDataObject.RestrictionInfo)restrictions.get(from);
                        if (prev != null) {
                           prev.next = ri;
                        } else {
                           restrictions.put(from, ri);
                        }

                        this.codedIS.popLimit(oldLimit7);
                        continue label135;
                     case 1:
                        ri.type = this.codedIS.readInt32();
                        break;
                     case 2:
                        from = (long)this.codedIS.readInt32();
                        break;
                     case 3:
                        ri.toWay = (long)this.codedIS.readInt32();
                        break;
                     case 4:
                        ri.viaWay = (long)this.codedIS.readInt32();
                        break;
                     default:
                        this.skipUnknownField(ts);
                  }
               }
            }
            case 8: {
               int length8 = this.codedIS.readRawVarint32();
               int oldLimit8 = this.codedIS.pushLimit(length8);
               stringTable = this.map.readStringTable();
               this.codedIS.popLimit(oldLimit8);
            }
         }
      }
   }

   private void readRouteEncodingRule(BinaryMapRouteReaderAdapter.RouteRegion index, int id) throws IOException {
      String tags = null;
      String val = null;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               index.initRouteEncodingRule(id, tags, val);
               return;
            case 1:
            case 2:
            case 4:
            case 6:
            default:
               this.skipUnknownField(t);
               break;
            case 3:
               tags = this.codedIS.readString().intern();
               break;
            case 5:
               val = this.codedIS.readString().intern();
               break;
            case 7:
               id = this.codedIS.readUInt32();
         }
      }
   }

   private BinaryMapRouteReaderAdapter.RouteSubregion readRouteTree(
      BinaryMapRouteReaderAdapter.RouteSubregion thisTree, BinaryMapRouteReaderAdapter.RouteSubregion parentTree, int depth, boolean readCoordinates
   ) throws IOException {
      boolean readChildren = depth != 0;
      if (readChildren) {
         thisTree.subregions = new ArrayList<>();
      }

      ++thisTree.routeReg.regionsRead;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return thisTree;
            case 1:
               int i = this.codedIS.readSInt32();
               if (readCoordinates) {
                  thisTree.left = i + (parentTree != null ? parentTree.left : 0);
               }
               break;
            case 2:
               int i2 = this.codedIS.readSInt32();
               if (readCoordinates) {
                  thisTree.right = i2 + (parentTree != null ? parentTree.right : 0);
               }
               break;
            case 3:
               int i3 = this.codedIS.readSInt32();
               if (readCoordinates) {
                  thisTree.top = i3 + (parentTree != null ? parentTree.top : 0);
               }
               break;
            case 4:
               int i4 = this.codedIS.readSInt32();
               if (readCoordinates) {
                  thisTree.bottom = i4 + (parentTree != null ? parentTree.bottom : 0);
               }
               break;
            case 5:
               thisTree.shiftToData = this.readInt();
               if (!readChildren) {
                  thisTree.subregions = new ArrayList<>();
                  readChildren = true;
               }
               break;
            case 6:
            default:
               this.skipUnknownField(t);
               break;
            case 7:
               if (readChildren) {
                  BinaryMapRouteReaderAdapter.RouteSubregion subregion = new BinaryMapRouteReaderAdapter.RouteSubregion(thisTree.routeReg);
                  subregion.length = this.readInt();
                  subregion.filePointer = this.codedIS.getTotalBytesRead();
                  int oldLimit = this.codedIS.pushLimit(subregion.length);
                  this.readRouteTree(subregion, thisTree, depth - 1, true);
                  thisTree.subregions.add(subregion);
                  this.codedIS.popLimit(oldLimit);
                  this.codedIS.seek((long)(subregion.filePointer + subregion.length));
               } else {
                  this.codedIS.seek((long)(thisTree.filePointer + thisTree.length));
               }
         }
      }
   }

   public void initRouteTypesIfNeeded(BinaryMapIndexReader.SearchRequest<?> req, List<BinaryMapRouteReaderAdapter.RouteSubregion> list) throws IOException {
      for(BinaryMapRouteReaderAdapter.RouteSubregion rs : list) {
         if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
            this.initRouteRegion(rs.routeReg);
         }
      }
   }

   public void initRouteRegion(BinaryMapRouteReaderAdapter.RouteRegion routeReg) throws IOException, InvalidProtocolBufferException {
      if (routeReg.routeEncodingRules.isEmpty()) {
         this.codedIS.seek((long)routeReg.filePointer);
         int oldLimit = this.codedIS.pushLimit(routeReg.length);
         this.readRouteIndex(routeReg);
         this.codedIS.popLimit(oldLimit);
      }
   }

   public List<RouteDataObject> loadRouteRegionData(BinaryMapRouteReaderAdapter.RouteSubregion rs) throws IOException {
      TLongArrayList idMap = new TLongArrayList();
      TLongObjectHashMap<RouteDataObject.RestrictionInfo> restrictionMap = new TLongObjectHashMap();
      if (rs.dataObjects == null) {
         this.codedIS.seek((long)(rs.filePointer + rs.shiftToData));
         int limit = this.codedIS.readRawVarint32();
         int oldLimit = this.codedIS.pushLimit(limit);
         this.readRouteTreeData(rs, idMap, restrictionMap);
         this.codedIS.popLimit(oldLimit);
      }

      List<RouteDataObject> res = rs.dataObjects;
      rs.dataObjects = null;
      return res;
   }

   public void loadRouteRegionData(List<BinaryMapRouteReaderAdapter.RouteSubregion> toLoad, ResultMatcher<RouteDataObject> matcher) throws IOException {
      Collections.sort(toLoad, new Comparator<BinaryMapRouteReaderAdapter.RouteSubregion>() {
         public int compare(BinaryMapRouteReaderAdapter.RouteSubregion o1, BinaryMapRouteReaderAdapter.RouteSubregion o2) {
            int p1 = o1.filePointer + o1.shiftToData;
            int p2 = o2.filePointer + o2.shiftToData;
            return p1 == p2 ? 0 : (p1 < p2 ? -1 : 1);
         }
      });
      TLongArrayList idMap = new TLongArrayList();
      TLongObjectHashMap<RouteDataObject.RestrictionInfo> restrictionMap = new TLongObjectHashMap();

      for(BinaryMapRouteReaderAdapter.RouteSubregion rs : toLoad) {
         if (rs.dataObjects == null) {
            this.codedIS.seek((long)(rs.filePointer + rs.shiftToData));
            int limit = this.codedIS.readRawVarint32();
            int oldLimit = this.codedIS.pushLimit(limit);
            this.readRouteTreeData(rs, idMap, restrictionMap);
            this.codedIS.popLimit(oldLimit);
         }

         for(RouteDataObject ro : rs.dataObjects) {
            if (ro != null) {
               matcher.publish(ro);
            }
         }

         rs.dataObjects = null;
      }
   }

   public List<BinaryMapRouteReaderAdapter.RouteSubregion> searchRouteRegionTree(
      BinaryMapIndexReader.SearchRequest<?> req,
      List<BinaryMapRouteReaderAdapter.RouteSubregion> list,
      List<BinaryMapRouteReaderAdapter.RouteSubregion> toLoad
   ) throws IOException {
      for(BinaryMapRouteReaderAdapter.RouteSubregion rs : list) {
         if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
            if (rs.subregions == null) {
               this.codedIS.seek((long)rs.filePointer);
               int old = this.codedIS.pushLimit(rs.length);
               this.readRouteTree(rs, null, req.contains(rs.left, rs.top, rs.right, rs.bottom) ? -1 : 1, false);
               this.codedIS.popLimit(old);
            }

            this.searchRouteRegionTree(req, rs.subregions, toLoad);
            if (rs.shiftToData != 0) {
               toLoad.add(rs);
            }
         }
      }

      return toLoad;
   }

   public List<BinaryMapRouteReaderAdapter.RouteSubregion> loadInteresectedPoints(
      BinaryMapIndexReader.SearchRequest<RouteDataObject> req,
      List<BinaryMapRouteReaderAdapter.RouteSubregion> list,
      List<BinaryMapRouteReaderAdapter.RouteSubregion> toLoad
   ) throws IOException {
      for(BinaryMapRouteReaderAdapter.RouteSubregion rs : list) {
         if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
            if (rs.subregions == null) {
               this.codedIS.seek((long)rs.filePointer);
               int old = this.codedIS.pushLimit(rs.length);
               this.readRouteTree(rs, null, req.contains(rs.left, rs.top, rs.right, rs.bottom) ? -1 : 1, false);
               this.codedIS.popLimit(old);
            }

            this.searchRouteRegionTree(req, rs.subregions, toLoad);
            if (rs.shiftToData != 0) {
               toLoad.add(rs);
            }
         }
      }

      return toLoad;
   }

   public static class RouteRegion extends BinaryIndexPart {
      public int regionsRead;
      public List<BinaryMapRouteReaderAdapter.RouteTypeRule> routeEncodingRules = new ArrayList<>();
      public int routeEncodingRulesBytes = 0;
      public Map<String, Integer> decodingRules = null;
      List<BinaryMapRouteReaderAdapter.RouteSubregion> subregions = new ArrayList<>();
      List<BinaryMapRouteReaderAdapter.RouteSubregion> basesubregions = new ArrayList<>();
      public int directionForward = -1;
      public int directionBackward = -1;
      public int maxheightForward = -1;
      public int maxheightBackward = -1;
      public int directionTrafficSignalsForward = -1;
      public int directionTrafficSignalsBackward = -1;
      public int trafficSignals = -1;
      public int stopSign = -1;
      public int stopMinor = -1;
      public int giveWaySign = -1;
      int nameTypeRule = -1;
      int refTypeRule = -1;
      int destinationTypeRule = -1;
      int destinationRefTypeRule = -1;
      private BinaryMapRouteReaderAdapter.RouteRegion referenceRouteRegion;

      @Override
      public String getPartName() {
         return "Routing";
      }

      @Override
      public int getFieldNumber() {
         return 9;
      }

      public int searchRouteEncodingRule(String tag, String value) {
         if (this.decodingRules == null) {
            this.decodingRules = new LinkedHashMap<>();

            for(int i = 1; i < this.routeEncodingRules.size(); ++i) {
               BinaryMapRouteReaderAdapter.RouteTypeRule rt = this.routeEncodingRules.get(i);
               String ks = rt.getTag() + "#" + (rt.getValue() == null ? "" : rt.getValue());
               this.decodingRules.put(ks, i);
            }
         }

         String k = tag + "#" + (value == null ? "" : value);
         return this.decodingRules.containsKey(k) ? this.decodingRules.get(k) : -1;
      }

      public int getNameTypeRule() {
         return this.nameTypeRule;
      }

      public int getRefTypeRule() {
         return this.refTypeRule;
      }

      public BinaryMapRouteReaderAdapter.RouteTypeRule quickGetEncodingRule(int id) {
         return this.routeEncodingRules.get(id);
      }

      public void initRouteEncodingRule(int id, String tags, String val) {
         this.decodingRules = null;

         while(this.routeEncodingRules.size() <= id) {
            this.routeEncodingRules.add(null);
         }

         this.routeEncodingRules.set(id, new BinaryMapRouteReaderAdapter.RouteTypeRule(tags, val));
         if (tags.equals("name")) {
            this.nameTypeRule = id;
         } else if (tags.equals("ref")) {
            this.refTypeRule = id;
         } else if (tags.equals("destination")
            || tags.equals("destination:forward")
            || tags.equals("destination:backward")
            || tags.startsWith("destination:lang:")) {
            this.destinationTypeRule = id;
         } else if (tags.equals("destination:ref") || tags.equals("destination:ref:forward") || tags.equals("destination:ref:backward")) {
            this.destinationRefTypeRule = id;
         } else if (tags.equals("highway") && val.equals("traffic_signals")) {
            this.trafficSignals = id;
         } else if (tags.equals("stop") && val.equals("minor")) {
            this.stopMinor = id;
         } else if (tags.equals("highway") && val.equals("stop")) {
            this.stopSign = id;
         } else if (tags.equals("highway") && val.equals("give_way")) {
            this.giveWaySign = id;
         } else if (tags.equals("traffic_signals:direction") && val != null) {
            if (val.equals("forward")) {
               this.directionTrafficSignalsForward = id;
            } else if (val.equals("backward")) {
               this.directionTrafficSignalsBackward = id;
            }
         } else if (tags.equals("direction") && val != null) {
            if (val.equals("forward")) {
               this.directionForward = id;
            } else if (val.equals("backward")) {
               this.directionBackward = id;
            }
         } else if (tags.equals("maxheight:forward") && val != null) {
            this.maxheightForward = id;
         } else if (tags.equals("maxheight:backward") && val != null) {
            this.maxheightBackward = id;
         }
      }

      public void completeRouteEncodingRules() {
         for(int i = 0; i < this.routeEncodingRules.size(); ++i) {
            BinaryMapRouteReaderAdapter.RouteTypeRule rtr = this.routeEncodingRules.get(i);
            if (rtr != null && rtr.conditional()) {
               String tag = rtr.getNonConditionalTag();

               for(BinaryMapRouteReaderAdapter.RouteTypeCondition c : rtr.conditions) {
                  if (tag != null && c.value != null) {
                     c.ruleid = this.findOrCreateRouteType(tag, c.value);
                  }
               }
            }
         }
      }

      public List<BinaryMapRouteReaderAdapter.RouteSubregion> getSubregions() {
         return this.subregions;
      }

      public List<BinaryMapRouteReaderAdapter.RouteSubregion> getBaseSubregions() {
         return this.basesubregions;
      }

      public double getLeftLongitude() {
         double l = 180.0;

         for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
            l = Math.min(l, MapUtils.get31LongitudeX(s.left));
         }

         return l;
      }

      public double getRightLongitude() {
         double l = -180.0;

         for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
            l = Math.max(l, MapUtils.get31LongitudeX(s.right));
         }

         return l;
      }

      public double getBottomLatitude() {
         double l = 90.0;

         for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
            l = Math.min(l, MapUtils.get31LatitudeY(s.bottom));
         }

         return l;
      }

      public double getTopLatitude() {
         double l = -90.0;

         for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
            l = Math.max(l, MapUtils.get31LatitudeY(s.top));
         }

         return l;
      }

      public boolean contains(int x31, int y31) {
         for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
            if (s.left <= x31 && s.right >= x31 && s.top <= y31 && s.bottom >= y31) {
               return true;
            }
         }

         return false;
      }

      public RouteDataObject adopt(RouteDataObject o) {
         if (o.region == this || o.region == this.referenceRouteRegion) {
            return o;
         } else if (this.routeEncodingRules.isEmpty()) {
            this.routeEncodingRules.addAll(o.region.routeEncodingRules);
            this.referenceRouteRegion = o.region;
            return o;
         } else {
            RouteDataObject rdo = new RouteDataObject(this);
            rdo.pointsX = o.pointsX;
            rdo.pointsY = o.pointsY;
            rdo.id = o.id;
            rdo.restrictions = o.restrictions;
            rdo.restrictionsVia = o.restrictionsVia;
            if (o.types != null) {
               rdo.types = new int[o.types.length];

               for(int i = 0; i < o.types.length; ++i) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule tp = o.region.routeEncodingRules.get(o.types[i]);
                  int ruleId = this.findOrCreateRouteType(tp.getTag(), tp.getValue());
                  rdo.types[i] = ruleId;
               }
            }

            if (o.pointTypes != null) {
               rdo.pointTypes = new int[o.pointTypes.length][];

               for(int i = 0; i < o.pointTypes.length; ++i) {
                  if (o.pointTypes[i] != null) {
                     rdo.pointTypes[i] = new int[o.pointTypes[i].length];

                     for(int j = 0; j < o.pointTypes[i].length; ++j) {
                        BinaryMapRouteReaderAdapter.RouteTypeRule tp = o.region.routeEncodingRules.get(o.pointTypes[i][j]);
                        int ruleId = this.searchRouteEncodingRule(tp.getTag(), tp.getValue());
                        if (ruleId != -1) {
                           rdo.pointTypes[i][j] = ruleId;
                        } else {
                           ruleId = this.routeEncodingRules.size();
                           this.initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue());
                           rdo.pointTypes[i][j] = ruleId;
                        }
                     }
                  }
               }
            }

            if (o.nameIds != null) {
               rdo.nameIds = new int[o.nameIds.length];
               rdo.names = new TIntObjectHashMap();

               for(int i = 0; i < o.nameIds.length; ++i) {
                  BinaryMapRouteReaderAdapter.RouteTypeRule tp = o.region.routeEncodingRules.get(o.nameIds[i]);
                  int ruleId = this.searchRouteEncodingRule(tp.getTag(), null);
                  if (ruleId != -1) {
                     rdo.nameIds[i] = ruleId;
                  } else {
                     ruleId = this.routeEncodingRules.size();
                     this.initRouteEncodingRule(ruleId, tp.getTag(), null);
                     rdo.nameIds[i] = ruleId;
                  }

                  rdo.names.put(ruleId, o.names.get(o.nameIds[i]));
               }
            }

            rdo.pointNames = o.pointNames;
            if (o.pointNameTypes != null) {
               rdo.pointNameTypes = new int[o.pointNameTypes.length][];

               for(int i = 0; i < o.pointNameTypes.length; ++i) {
                  if (o.pointNameTypes[i] != null) {
                     rdo.pointNameTypes[i] = new int[o.pointNameTypes[i].length];

                     for(int j = 0; j < o.pointNameTypes[i].length; ++j) {
                        BinaryMapRouteReaderAdapter.RouteTypeRule tp = o.region.routeEncodingRules.get(o.pointNameTypes[i][j]);
                        int ruleId = this.searchRouteEncodingRule(tp.getTag(), null);
                        if (ruleId != -1) {
                           rdo.pointNameTypes[i][j] = ruleId;
                        } else {
                           ruleId = this.routeEncodingRules.size();
                           this.initRouteEncodingRule(ruleId, tp.getTag(), tp.getValue());
                           rdo.pointNameTypes[i][j] = ruleId;
                        }
                     }
                  }
               }
            }

            return rdo;
         }
      }

      public int findOrCreateRouteType(String tag, String value) {
         int ruleId = this.searchRouteEncodingRule(tag, value);
         if (ruleId == -1) {
            ruleId = this.routeEncodingRules.size();
            this.initRouteEncodingRule(ruleId, tag, value);
         }

         return ruleId;
      }
   }

   public static class RouteSubregion {
      private static final int INT_SIZE = 4;
      public final BinaryMapRouteReaderAdapter.RouteRegion routeReg;
      public int length;
      public int filePointer;
      public int left;
      public int right;
      public int top;
      public int bottom;
      public int shiftToData;
      public List<BinaryMapRouteReaderAdapter.RouteSubregion> subregions = null;
      public List<RouteDataObject> dataObjects = null;

      public RouteSubregion(BinaryMapRouteReaderAdapter.RouteSubregion copy) {
         this.routeReg = copy.routeReg;
         this.left = copy.left;
         this.right = copy.right;
         this.top = copy.top;
         this.bottom = copy.bottom;
         this.filePointer = copy.filePointer;
         this.length = copy.length;
      }

      public RouteSubregion(BinaryMapRouteReaderAdapter.RouteRegion routeReg) {
         this.routeReg = routeReg;
      }

      public int getEstimatedSize() {
         int shallow = 40;
         if (this.subregions != null) {
            shallow += 8;

            for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
               shallow += s.getEstimatedSize();
            }
         }

         return shallow;
      }

      public int countSubregions() {
         int cnt = 1;
         if (this.subregions != null) {
            for(BinaryMapRouteReaderAdapter.RouteSubregion s : this.subregions) {
               cnt += s.countSubregions();
            }
         }

         return cnt;
      }
   }

   private static class RouteTypeCondition implements StringExternalizable<RouteDataBundle> {
      String condition = "";
      OpeningHoursParser.OpeningHours hours = null;
      String value;
      int ruleid;

      private RouteTypeCondition() {
      }

      public void writeToBundle(RouteDataBundle bundle) {
         bundle.putString("c", this.condition);
         bundle.putString("v", this.value);
         bundle.putInt("id", this.ruleid);
      }

      public void readFromBundle(RouteDataBundle bundle) {
      }
   }

   public static class RouteTypeRule implements StringExternalizable<RouteDataBundle> {
      private static final int ACCESS = 1;
      private static final int ONEWAY = 2;
      private static final int HIGHWAY_TYPE = 3;
      private static final int MAXSPEED = 4;
      private static final int ROUNDABOUT = 5;
      public static final int TRAFFIC_SIGNALS = 6;
      public static final int RAILWAY_CROSSING = 7;
      private static final int LANES = 8;
      private String t;
      private String v;
      private int intValue;
      private float floatValue;
      private int type;
      private List<BinaryMapRouteReaderAdapter.RouteTypeCondition> conditions = null;
      private int forward;

      public RouteTypeRule() {
      }

      public RouteTypeRule(String t, String v) {
         this.t = t.intern();
         if ("true".equals(v)) {
            v = "yes";
         }

         if ("false".equals(v)) {
            v = "no";
         }

         this.v = v == null ? null : v.intern();

         try {
            this.analyze();
         } catch (RuntimeException var4) {
            System.err.println("Error analyzing tag/value = " + t + "/" + v);
            throw var4;
         }
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + (this.t == null ? 0 : this.t.hashCode());
         return 31 * result + (this.v == null ? 0 : this.v.hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj != null && this.getClass() == obj.getClass()) {
            BinaryMapRouteReaderAdapter.RouteTypeRule other = (BinaryMapRouteReaderAdapter.RouteTypeRule)obj;
            return Algorithms.objectEquals(other.t, this.t) && Algorithms.objectEquals(other.v, this.v);
         } else {
            return false;
         }
      }

      public void writeToBundle(RouteDataBundle bundle) {
         bundle.putString("t", this.t);
         if (this.v != null) {
            bundle.putString("v", this.v);
         }
      }

      public void readFromBundle(RouteDataBundle bundle) {
         this.t = bundle.getString("t", null);
         this.v = bundle.getString("v", null);

         try {
            this.analyze();
         } catch (RuntimeException var3) {
            System.err.println("Error analyzing tag/value = " + this.t + "/" + this.v);
            throw var3;
         }
      }

      @Override
      public String toString() {
         return this.t + "=" + this.v;
      }

      public int isForward() {
         return this.forward;
      }

      public String getTag() {
         return this.t;
      }

      public String getValue() {
         return this.v;
      }

      public boolean roundabout() {
         return this.type == 5;
      }

      public int getType() {
         return this.type;
      }

      public boolean conditional() {
         return this.conditions != null;
      }

      public String getNonConditionalTag() {
         String tag = this.getTag();
         if (tag != null && tag.endsWith(":conditional")) {
            tag = tag.substring(0, tag.length() - ":conditional".length());
         }

         return tag;
      }

      public int onewayDirection() {
         return this.type == 2 ? this.intValue : 0;
      }

      public int conditionalValue(long time) {
         if (this.conditional()) {
            Calendar i = Calendar.getInstance();
            i.setTimeInMillis(time);

            for(BinaryMapRouteReaderAdapter.RouteTypeCondition c : this.conditions) {
               if (c.hours != null && c.hours.isOpenedForTime(i)) {
                  return c.ruleid;
               }
            }
         }

         return 0;
      }

      public float maxSpeed() {
         return this.type == 4 ? this.floatValue : -1.0F;
      }

      public int lanes() {
         return this.type == 8 ? this.intValue : -1;
      }

      public String highwayRoad() {
         return this.type == 3 ? this.v : null;
      }

      private void analyze() {
         if (this.t.equalsIgnoreCase("oneway")) {
            this.type = 2;
            if ("-1".equals(this.v) || "reverse".equals(this.v)) {
               this.intValue = -1;
            } else if (!"1".equals(this.v) && !"yes".equals(this.v)) {
               this.intValue = 0;
            } else {
               this.intValue = 1;
            }
         } else if (this.t.equalsIgnoreCase("highway") && "traffic_signals".equals(this.v)) {
            this.type = 6;
         } else if (!this.t.equalsIgnoreCase("railway") || !"crossing".equals(this.v) && !"level_crossing".equals(this.v)) {
            if (this.t.equalsIgnoreCase("roundabout") && this.v != null) {
               this.type = 5;
            } else if (this.t.equalsIgnoreCase("junction") && "roundabout".equalsIgnoreCase(this.v)) {
               this.type = 5;
            } else if (this.t.equalsIgnoreCase("highway") && this.v != null) {
               this.type = 3;
            } else if (this.t.endsWith(":conditional") && this.v != null) {
               this.conditions = new ArrayList<>();
               String[] cts = this.v.split("\\);");

               for(String c : cts) {
                  int ch = c.indexOf(64);
                  if (ch > 0) {
                     BinaryMapRouteReaderAdapter.RouteTypeCondition cond = new BinaryMapRouteReaderAdapter.RouteTypeCondition();
                     cond.value = c.substring(0, ch).trim();
                     cond.condition = c.substring(ch + 1).trim();
                     if (cond.condition.startsWith("(")) {
                        cond.condition = cond.condition.substring(1, cond.condition.length()).trim();
                     }

                     if (cond.condition.endsWith(")")) {
                        cond.condition = cond.condition.substring(0, cond.condition.length() - 1).trim();
                     }

                     cond.hours = OpeningHoursParser.parseOpenedHours(cond.condition);
                     this.conditions.add(cond);
                  }
               }
            } else if (this.t.startsWith("access") && this.v != null) {
               this.type = 1;
            } else if (this.t.equalsIgnoreCase("maxspeed") && this.v != null) {
               this.type = 4;
               this.floatValue = RouteDataObject.parseSpeed(this.v, 0.0F);
            } else if (this.t.equalsIgnoreCase("maxspeed:forward") && this.v != null) {
               this.type = 4;
               this.forward = 1;
               this.floatValue = RouteDataObject.parseSpeed(this.v, 0.0F);
            } else if (this.t.equalsIgnoreCase("maxspeed:backward") && this.v != null) {
               this.type = 4;
               this.forward = -1;
               this.floatValue = RouteDataObject.parseSpeed(this.v, 0.0F);
            } else if (this.t.equalsIgnoreCase("lanes") && this.v != null) {
               this.intValue = -1;
               int i = 0;
               this.type = 8;

               while(i < this.v.length() && Character.isDigit(this.v.charAt(i))) {
                  ++i;
               }

               if (i > 0) {
                  this.intValue = Integer.parseInt(this.v.substring(0, i));
               }
            }
         } else {
            this.type = 7;
         }
      }
   }
}
