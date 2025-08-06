package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;
import net.osmand.data.TransportStopExit;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;
import net.osmand.util.TransliterationHelper;

public class BinaryMapTransportReaderAdapter {
   private CodedInputStream codedIS;
   private final BinaryMapIndexReader map;

   protected BinaryMapTransportReaderAdapter(BinaryMapIndexReader map) {
      this.codedIS = map.codedIS;
      this.map = map;
   }

   private void skipUnknownField(int t) throws IOException {
      this.map.skipUnknownField(t);
   }

   private int readInt() throws IOException {
      return this.map.readInt();
   }

   protected void readTransportIndex(BinaryMapTransportReaderAdapter.TransportIndex ind) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               ind.setName(this.codedIS.readString());
               break;
            case 2:
            case 4:
            case 5:
            case 7:
            default:
               this.skipUnknownField(t);
               break;
            case 3:
               this.skipUnknownField(t);
               break;
            case 6:
               ind.stopsFileLength = this.readInt();
               ind.stopsFileOffset = this.codedIS.getTotalBytesRead();
               int old = this.codedIS.pushLimit(ind.stopsFileLength);
               this.readTransportBounds(ind);
               this.codedIS.popLimit(old);
               break;
            case 8:
               ind.incompleteRoutesLength = this.codedIS.readRawVarint32();
               ind.incompleteRoutesOffset = this.codedIS.getTotalBytesRead();
               this.codedIS.seek((long)(ind.incompleteRoutesLength + ind.incompleteRoutesOffset));
               break;
            case 9:
               BinaryMapTransportReaderAdapter.IndexStringTable st = new BinaryMapTransportReaderAdapter.IndexStringTable();
               st.length = this.codedIS.readRawVarint32();
               st.fileOffset = this.codedIS.getTotalBytesRead();
               ind.stringTable = st;
               this.codedIS.seek((long)(st.length + st.fileOffset));
         }
      }
   }

   private void readTransportBounds(BinaryMapTransportReaderAdapter.TransportIndex ind) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               ind.left = this.codedIS.readSInt32();
               break;
            case 2:
               ind.right = this.codedIS.readSInt32();
               break;
            case 3:
               ind.top = this.codedIS.readSInt32();
               break;
            case 4:
               ind.bottom = this.codedIS.readSInt32();
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected void searchTransportTreeBounds(
      int pleft, int pright, int ptop, int pbottom, BinaryMapIndexReader.SearchRequest<TransportStop> req, TIntObjectHashMap<String> stringTable
   ) throws IOException {
      int init = 0;
      int lastIndexResult = -1;
      int cright = 0;
      int cleft = 0;
      int ctop = 0;
      int cbottom = 0;
      ++req.numberOfReadSubtrees;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         if (init == 15) {
            init = 0;
            if (cright < req.left || cleft > req.right || ctop > req.bottom || cbottom < req.top) {
               return;
            }

            ++req.numberOfAcceptedSubtrees;
         }

         switch(tag) {
            case 0:
               return;
            case 1:
               cleft = this.codedIS.readSInt32() + pleft;
               init |= 2;
               break;
            case 2:
               cright = this.codedIS.readSInt32() + pright;
               init |= 4;
               break;
            case 3:
               ctop = this.codedIS.readSInt32() + ptop;
               init |= 8;
               break;
            case 4:
               cbottom = this.codedIS.readSInt32() + pbottom;
               init |= 1;
               break;
            case 5:
            case 6:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            default:
               this.skipUnknownField(t);
               break;
            case 7:
               int length = this.readInt();
               int filePointer = this.codedIS.getTotalBytesRead();
               if (req.limit == -1 || req.limit >= req.getSearchResults().size()) {
                  int oldLimit = this.codedIS.pushLimit(length);
                  this.searchTransportTreeBounds(cleft, cright, ctop, cbottom, req, stringTable);
                  this.codedIS.popLimit(oldLimit);
               }

               this.codedIS.seek((long)(filePointer + length));
               if (lastIndexResult >= 0) {
                  throw new IllegalStateException();
               }
               break;
            case 8:
               int stopOffset = this.codedIS.getTotalBytesRead();
               int bytesLength = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(bytesLength);
               if (lastIndexResult == -1) {
                  lastIndexResult = req.getSearchResults().size();
               }

               ++req.numberOfVisitedObjects;
               TransportStop transportStop = this.readTransportStop(stopOffset, cleft, cright, ctop, cbottom, req, stringTable);
               if (transportStop != null) {
                  req.publish(transportStop);
               }

               this.codedIS.popLimit(oldLimit);
               break;
            case 16:
               long baseId = this.codedIS.readUInt64();
               if (lastIndexResult != -1) {
                  for(int i = lastIndexResult; i < req.getSearchResults().size(); ++i) {
                     TransportStop rs = req.getSearchResults().get(i);
                     rs.setId(Long.valueOf(rs.getId() + baseId));
                  }
               }
         }
      }
   }

   private String regStr(TIntObjectHashMap<String> stringTable) throws IOException {
      int i = this.codedIS.readUInt32();
      stringTable.putIfAbsent(i, "");
      return (char)i + "";
   }

   private String regStr(TIntObjectHashMap<String> stringTable, int i) throws IOException {
      stringTable.putIfAbsent(i, "");
      return (char)i + "";
   }

   public void readIncompleteRoutesList(TLongObjectHashMap<IncompleteTransportRoute> incompleteRoutes, int transportIndexStart) throws IOException {
      boolean end = false;

      while(!end) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               end = true;
               break;
            case 6:
               int l = this.codedIS.readRawVarint32();
               int olds = this.codedIS.pushLimit(l);
               IncompleteTransportRoute ir = this.readIncompleteRoute(transportIndexStart);
               IncompleteTransportRoute itr = (IncompleteTransportRoute)incompleteRoutes.get(ir.getRouteId());
               if (itr != null) {
                  itr.setNextLinkedRoute(ir);
               } else {
                  incompleteRoutes.put(ir.getRouteId(), ir);
               }

               this.codedIS.popLimit(olds);
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   public IncompleteTransportRoute readIncompleteRoute(int transportIndexStart) throws IOException {
      IncompleteTransportRoute dataObject = new IncompleteTransportRoute();
      boolean end = false;

      while(!end) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               end = true;
               break;
            case 1:
               dataObject.setRouteId(this.codedIS.readUInt64());
               break;
            case 2:
            case 6:
            default:
               this.skipUnknownField(t);
               break;
            case 3:
               this.skipUnknownField(t);
               break;
            case 4:
               this.skipUnknownField(t);
               break;
            case 5:
               this.skipUnknownField(t);
               break;
            case 7:
               int delta = this.codedIS.readRawVarint32();
               if (delta > transportIndexStart) {
                  dataObject.setRouteOffset(delta);
               } else {
                  dataObject.setRouteOffset(transportIndexStart + delta);
               }
               break;
            case 8:
               this.skipUnknownField(t);
         }
      }

      return dataObject;
   }

   public TransportRoute getTransportRoute(int filePointer, TIntObjectHashMap<String> stringTable, boolean onlyDescription) throws IOException {
      this.codedIS.seek((long)filePointer);
      int routeLength = this.codedIS.readRawVarint32();
      int old = this.codedIS.pushLimit(routeLength);
      TransportRoute dataObject = new TransportRoute();
      dataObject.setFileOffset(filePointer);
      boolean end = false;
      long rid = 0L;
      int[] rx = new int[]{0};
      int[] ry = new int[]{0};

      while(!end) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               end = true;
               break;
            case 1:
               dataObject.setId(Long.valueOf(this.codedIS.readUInt64()));
               break;
            case 2:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 16:
            default:
               this.skipUnknownField(t);
               break;
            case 3:
               dataObject.setType(this.regStr(stringTable));
               break;
            case 4:
               dataObject.setOperator(this.regStr(stringTable));
               break;
            case 5:
               dataObject.setRef(this.codedIS.readString());
               break;
            case 6:
               dataObject.setName(this.regStr(stringTable));
               break;
            case 7:
               dataObject.setEnName(this.regStr(stringTable));
               break;
            case 8:
               dataObject.setDistance(this.codedIS.readUInt32());
               break;
            case 9:
               dataObject.setColor(this.regStr(stringTable));
               break;
            case 15:
               if (onlyDescription) {
                  end = true;
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               } else {
                  int length = this.codedIS.readRawVarint32();
                  int olds = this.codedIS.pushLimit(length);
                  TransportStop stop = this.readTransportRouteStop(rx, ry, rid, stringTable, filePointer);
                  dataObject.getForwardStops().add(stop);
                  rid = stop.getId();
                  this.codedIS.popLimit(olds);
               }
               break;
            case 17:
               int sizeL = this.codedIS.readRawVarint32();
               int pold = this.codedIS.pushLimit(sizeL);
               int px = 0;
               int py = 0;
               Way w = new Way(-1L);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int ddx = this.codedIS.readSInt32() << 5;
                  int ddy = this.codedIS.readSInt32() << 5;
                  if (ddx == 0 && ddy == 0) {
                     if (w.getNodes().size() > 0) {
                        dataObject.addWay(w);
                     }

                     w = new Way(-1L);
                  } else {
                     int x = ddx + px;
                     int y = ddy + py;
                     w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1L));
                     px = x;
                     py = y;
                  }
               }

               if (w.getNodes().size() > 0) {
                  dataObject.addWay(w);
               }

               this.codedIS.popLimit(pold);
               break;
            case 18: {
               int sizeL18 = this.codedIS.readRawVarint32();
               int pold18 = this.codedIS.pushLimit(sizeL18);
               this.readTransportSchedule(dataObject.getOrCreateSchedule());
               this.codedIS.popLimit(pold18);
               break;
            }
            case 19:
               String str = this.regStr(stringTable);
               dataObject.addTag(str, "");
               break;
            case 20: {
               TByteArrayList buf = new TByteArrayList();
               int sizeL20 = this.codedIS.readRawVarint32();
               int olds = this.codedIS.pushLimit(sizeL20);
               String key = this.regStr(stringTable);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  buf.add(this.codedIS.readRawByte());
               }

               this.codedIS.popLimit(olds);
               dataObject.addTag(key, new String(buf.toArray(), StandardCharsets.UTF_8));
            }
         }
      }

      this.codedIS.popLimit(old);
      return dataObject;
   }

   private void readTransportSchedule(TransportSchedule schedule) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               int sizeL = this.codedIS.readRawVarint32();
               int old = this.codedIS.pushLimit(sizeL);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int interval = this.codedIS.readRawVarint32();
                  schedule.avgStopIntervals.add(interval);
               }

               this.codedIS.popLimit(old);
               break;
            case 2:
               int sizeL2 = this.codedIS.readRawVarint32();
               int old2 = this.codedIS.pushLimit(sizeL2);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int interval = this.codedIS.readRawVarint32();
                  schedule.avgWaitIntervals.add(interval);
               }

               this.codedIS.popLimit(old2);
               break;
            case 3:
               int sizeL3 = this.codedIS.readRawVarint32();
               int old3 = this.codedIS.pushLimit(sizeL3);

               while(this.codedIS.getBytesUntilLimit() > 0) {
                  int interval = this.codedIS.readRawVarint32();
                  schedule.tripIntervals.add(interval);
               }

               this.codedIS.popLimit(old3);
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected TIntObjectHashMap<String> initializeStringTable(BinaryMapTransportReaderAdapter.TransportIndex ind, TIntObjectHashMap<String> requested) throws IOException {
      if (ind.stringTable.stringTable == null) {
         ind.stringTable.stringTable = new TIntObjectHashMap();
         this.codedIS.seek((long)ind.stringTable.fileOffset);
         int oldLimit = this.codedIS.pushLimit(ind.stringTable.length);
         int current = 0;

         while(this.codedIS.getBytesUntilLimit() > 0) {
            int t = this.codedIS.readTag();
            int tag = WireFormat.getTagFieldNumber(t);
            switch(tag) {
               case 0:
                  break;
               case 1:
                  String value = this.codedIS.readString();
                  ind.stringTable.stringTable.put(current, value);
                  ++current;
                  break;
               default:
                  this.skipUnknownField(t);
            }
         }

         this.codedIS.popLimit(oldLimit);
      }

      return ind.stringTable.stringTable;
   }

   protected void initializeNames(boolean onlyDescription, TransportRoute dataObject, TIntObjectHashMap<String> stringTable) throws IOException {
      if (dataObject.getName().length() > 0) {
         dataObject.setName((String)stringTable.get(dataObject.getName().charAt(0)));
      }

      if (dataObject.getEnName(false).length() > 0) {
         dataObject.setEnName((String)stringTable.get(dataObject.getEnName(false).charAt(0)));
      }

      if (dataObject.getName().length() > 0 && dataObject.getName("en").length() == 0) {
         dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
      }

      if (dataObject.getOperator() != null && dataObject.getOperator().length() > 0) {
         dataObject.setOperator((String)stringTable.get(dataObject.getOperator().charAt(0)));
      }

      if (dataObject.getColor() != null && dataObject.getColor().length() > 0) {
         dataObject.setColor((String)stringTable.get(dataObject.getColor().charAt(0)));
      }

      if (dataObject.getType() != null && dataObject.getType().length() > 0) {
         dataObject.setType((String)stringTable.get(dataObject.getType().charAt(0)));
      }

      if (!onlyDescription) {
         for(TransportStop s : dataObject.getForwardStops()) {
            this.initializeNames(stringTable, s);
         }
      }

      if (dataObject.getTags() != null && dataObject.getTags().size() > 0) {
         dataObject.setTags(this.initializeTags(stringTable, dataObject));
      }
   }

   private Map<String, String> initializeTags(TIntObjectHashMap<String> stringTable, TransportRoute dataObject) {
      Map<String, String> newMap = new HashMap<>();

      for(Entry<String, String> entry : dataObject.getTags().entrySet()) {
         String string = (String)stringTable.get(entry.getKey().charAt(0));
         if (entry.getValue().length() > 0) {
            newMap.put(string, entry.getValue());
         } else {
            int index = string.indexOf(47);
            if (index > 0) {
               newMap.put(string.substring(0, index), string.substring(index + 1));
            }
         }
      }

      return newMap;
   }

   protected void initializeNames(TIntObjectHashMap<String> stringTable, TransportStop s) {
      for(TransportStopExit exit : s.getExits()) {
         if (exit.getRef().length() > 0) {
            exit.setRef((String)stringTable.get(exit.getRef().charAt(0)));
         }
      }

      if (s.getName().length() > 0) {
         s.setName((String)stringTable.get(s.getName().charAt(0)));
      }

      if (s.getEnName(false).length() > 0) {
         s.setEnName((String)stringTable.get(s.getEnName(false).charAt(0)));
      }

      Map<String, String> namesMap = new HashMap<>(s.getNamesMap(false));
      if (!s.getNamesMap(false).isEmpty()) {
         s.getNamesMap(false).clear();
      }

      for(Entry<String, String> e : namesMap.entrySet()) {
         s.setName((String)stringTable.get(e.getKey().charAt(0)), (String)stringTable.get(e.getValue().charAt(0)));
      }
   }

   private TransportStop readTransportRouteStop(int[] dx, int[] dy, long did, TIntObjectHashMap<String> stringTable, int filePointer) throws IOException {
      TransportStop dataObject = new TransportStop();
      dataObject.setFileOffset(this.codedIS.getTotalBytesRead());
      dataObject.setReferencesToRoutes(new int[]{filePointer});
      boolean end = false;

      while(!end) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               end = true;
               break;
            case 1:
               did += this.codedIS.readSInt64();
               break;
            case 2:
               dx[0] += this.codedIS.readSInt32();
               break;
            case 3:
               dy[0] += this.codedIS.readSInt32();
               break;
            case 4:
            case 5:
            default:
               this.skipUnknownField(t);
               break;
            case 6:
               dataObject.setName(this.regStr(stringTable));
               break;
            case 7:
               dataObject.setEnName(this.regStr(stringTable));
         }
      }

      dataObject.setId(Long.valueOf(did));
      dataObject.setLocation(24, dx[0], dy[0]);
      return dataObject;
   }

   private TransportStop readTransportStop(
      int shift, int cleft, int cright, int ctop, int cbottom, BinaryMapIndexReader.SearchRequest<TransportStop> req, TIntObjectHashMap<String> stringTable
   ) throws IOException {
      int tag = WireFormat.getTagFieldNumber(this.codedIS.readTag());
      if (1 != tag) {
         throw new IllegalArgumentException();
      } else {
         int x = this.codedIS.readSInt32() + cleft;
         tag = WireFormat.getTagFieldNumber(this.codedIS.readTag());
         if (2 != tag) {
            throw new IllegalArgumentException();
         } else {
            int y = this.codedIS.readSInt32() + ctop;
            if (req.right >= x && req.left <= x && req.top <= y && req.bottom >= y) {
               ++req.numberOfAcceptedObjects;
               req.cacheTypes.clear();
               req.cacheIdsA.clear();
               req.cacheIdsB.clear();
               TransportStop dataObject = new TransportStop();
               dataObject.setLocation(24, x, y);
               dataObject.setFileOffset(shift);

               while(true) {
                  int t = this.codedIS.readTag();
                  tag = WireFormat.getTagFieldNumber(t);
                  switch(tag) {
                     case 0:
                        dataObject.setReferencesToRoutes(req.cacheTypes.toArray());
                        dataObject.setDeletedRoutesIds(req.cacheIdsA.toArray());
                        dataObject.setRoutesIds(req.cacheIdsB.toArray());
                        if (dataObject.getName("en").length() == 0) {
                           dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
                        }

                        return dataObject;
                     case 1:
                     case 2:
                     case 3:
                     case 4:
                     case 10:
                     case 11:
                     case 12:
                     case 13:
                     case 14:
                     case 15:
                     case 17:
                     case 18:
                     case 19:
                     case 21:
                     default:
                        this.skipUnknownField(t);
                        break;
                     case 5:
                        dataObject.setId(Long.valueOf(this.codedIS.readSInt64()));
                        break;
                     case 6:
                        if (stringTable != null) {
                           dataObject.setName(this.regStr(stringTable));
                        } else {
                           this.skipUnknownField(t);
                        }
                        break;
                     case 7:
                        if (stringTable != null) {
                           dataObject.setEnName(this.regStr(stringTable));
                        } else {
                           this.skipUnknownField(t);
                        }
                        break;
                     case 8:
                        if (stringTable == null) {
                           this.skipUnknownField(t);
                           break;
                        }

                        int sizeL = this.codedIS.readRawVarint32();
                        int oldRef = this.codedIS.pushLimit(sizeL);

                        while(this.codedIS.getBytesUntilLimit() > 0) {
                           dataObject.setName(
                              this.regStr(stringTable, this.codedIS.readRawVarint32()), this.regStr(stringTable, this.codedIS.readRawVarint32())
                           );
                        }

                        this.codedIS.popLimit(oldRef);
                        break;
                     case 9:
                        int length = this.codedIS.readRawVarint32();
                        int oldLimit = this.codedIS.pushLimit(length);
                        TransportStopExit transportStopExit = this.readTransportStopExit(cleft, ctop, req, stringTable);
                        dataObject.addExit(transportStopExit);
                        this.codedIS.popLimit(oldLimit);
                        break;
                     case 16:
                        req.cacheTypes.add(shift - this.codedIS.readUInt32());
                        break;
                     case 20:
                        req.cacheIdsA.add(this.codedIS.readUInt64());
                        break;
                     case 22:
                        req.cacheIdsB.add(this.codedIS.readUInt64());
                  }
               }
            } else {
               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               return null;
            }
         }
      }
   }

   private TransportStopExit readTransportStopExit(
      int cleft, int ctop, BinaryMapIndexReader.SearchRequest<TransportStop> req, TIntObjectHashMap<String> stringTable
   ) throws IOException {
      TransportStopExit dataObject = new TransportStopExit();
      int x = 0;
      int y = 0;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (dataObject.getName("en").length() == 0) {
                  dataObject.setEnName(TransliterationHelper.transliterate(dataObject.getName()));
               }

               if (x != 0 || y != 0) {
                  dataObject.setLocation(24, x, y);
               }

               return dataObject;
            case 1:
               x = this.codedIS.readSInt32() + cleft;
               break;
            case 2:
               y = this.codedIS.readSInt32() + ctop;
               break;
            case 3:
               if (stringTable != null) {
                  dataObject.setRef(this.regStr(stringTable));
               } else {
                  this.skipUnknownField(t);
               }
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected static class IndexStringTable {
      int fileOffset = 0;
      int length = 0;
      TIntObjectHashMap<String> stringTable = null;
   }

   public static class TransportIndex extends BinaryIndexPart {
      int left = 0;
      int right = 0;
      int top = 0;
      int bottom = 0;
      int stopsFileOffset = 0;
      int stopsFileLength = 0;
      int incompleteRoutesOffset = 0;
      int incompleteRoutesLength = 0;
      BinaryMapTransportReaderAdapter.IndexStringTable stringTable = null;

      @Override
      public String getPartName() {
         return "Transport";
      }

      @Override
      public int getFieldNumber() {
         return 4;
      }

      public int getLeft() {
         return this.left;
      }

      public int getRight() {
         return this.right;
      }

      public int getTop() {
         return this.top;
      }

      public int getBottom() {
         return this.bottom;
      }
   }
}
