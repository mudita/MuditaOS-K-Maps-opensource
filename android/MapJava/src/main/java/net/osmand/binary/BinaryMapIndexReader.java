package net.osmand.binary;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.IncompleteTransportRoute;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.Way;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BinaryMapIndexReader {
   public static final int DETAILED_MAP_MIN_ZOOM = 9;
   public static final int TRANSPORT_STOP_ZOOM = 24;
   public static final int SHIFT_COORDINATES = 5;
   public static final int LABEL_ZOOM_ENCODE = 26;
   private static final Log log = PlatformUtil.getLog(BinaryMapIndexReader.class);
   public static boolean READ_STATS = false;
   public static final BinaryMapIndexReader.SearchPoiTypeFilter ACCEPT_ALL_POI_TYPE_FILTER = new BinaryMapIndexReader.SearchPoiTypeFilter() {
      @Override
      public boolean isEmpty() {
         return false;
      }

      @Override
      public boolean accept(PoiCategory type, String subcategory) {
         return true;
      }
   };
   private final RandomAccessFile raf;
   protected final File file;
   int version;
   long dateCreated;
   boolean basemap = false;
   List<BinaryMapIndexReader.MapIndex> mapIndexes = new ArrayList<>();
   List<BinaryMapPoiReaderAdapter.PoiRegion> poiIndexes = new ArrayList<>();
   List<BinaryMapAddressReaderAdapter.AddressRegion> addressIndexes = new ArrayList<>();
   List<BinaryMapTransportReaderAdapter.TransportIndex> transportIndexes = new ArrayList<>();
   List<BinaryMapRouteReaderAdapter.RouteRegion> routingIndexes = new ArrayList<>();
   List<BinaryIndexPart> indexes = new ArrayList<>();
   TLongObjectHashMap<IncompleteTransportRoute> incompleteTransportRoutes = null;
   protected CodedInputStream codedIS;
   private final BinaryMapTransportReaderAdapter transportAdapter;
   private final BinaryMapPoiReaderAdapter poiAdapter;
   private final BinaryMapAddressReaderAdapter addressAdapter;
   private final BinaryMapRouteReaderAdapter routeAdapter;
   private static final String BASEMAP_NAME = "basemap";
   private int MASK_TO_READ = -32;
   private static boolean testMapSearch = false;
   private static boolean testAddressSearch = false;
   private static boolean testAddressSearchName = false;
   private static boolean testAddressJustifySearch = false;
   private static boolean testPoiSearch = true;
   private static boolean testPoiSearchOnPath = false;
   private static boolean testTransportSearch = false;
   private static int sleft = MapUtils.get31TileNumberX(27.55079);
   private static int sright = MapUtils.get31TileNumberX(27.55317);
   private static int stop = MapUtils.get31TileNumberY(53.89378);
   private static int sbottom = MapUtils.get31TileNumberY(53.89276);
   private static int szoom = 15;

   public BinaryMapIndexReader(RandomAccessFile raf, File file) throws IOException {
      this.raf = raf;
      this.file = file;
      this.codedIS = CodedInputStream.newInstance(raf);
      this.codedIS.setSizeLimit(Integer.MAX_VALUE);
      this.transportAdapter = new BinaryMapTransportReaderAdapter(this);
      this.addressAdapter = new BinaryMapAddressReaderAdapter(this);
      this.poiAdapter = new BinaryMapPoiReaderAdapter(this);
      this.routeAdapter = new BinaryMapRouteReaderAdapter(this);
      this.init();
   }

   public BinaryMapIndexReader(RandomAccessFile raf, File file, boolean init) throws IOException {
      this.raf = raf;
      this.file = file;
      this.codedIS = CodedInputStream.newInstance(raf);
      this.codedIS.setSizeLimit(Integer.MAX_VALUE);
      this.transportAdapter = new BinaryMapTransportReaderAdapter(this);
      this.addressAdapter = new BinaryMapAddressReaderAdapter(this);
      this.poiAdapter = new BinaryMapPoiReaderAdapter(this);
      this.routeAdapter = new BinaryMapRouteReaderAdapter(this);
      if (init) {
         this.init();
      }
   }

   public BinaryMapIndexReader(RandomAccessFile raf, BinaryMapIndexReader referenceToSameFile) throws IOException {
      this.raf = raf;
      this.file = referenceToSameFile.file;
      this.codedIS = CodedInputStream.newInstance(raf);
      this.codedIS.setSizeLimit(Integer.MAX_VALUE);
      this.version = referenceToSameFile.version;
      this.dateCreated = referenceToSameFile.dateCreated;
      this.transportAdapter = new BinaryMapTransportReaderAdapter(this);
      this.addressAdapter = new BinaryMapAddressReaderAdapter(this);
      this.poiAdapter = new BinaryMapPoiReaderAdapter(this);
      this.routeAdapter = new BinaryMapRouteReaderAdapter(this);
      this.mapIndexes = new ArrayList<>(referenceToSameFile.mapIndexes);
      this.poiIndexes = new ArrayList<>(referenceToSameFile.poiIndexes);
      this.addressIndexes = new ArrayList<>(referenceToSameFile.addressIndexes);
      this.transportIndexes = new ArrayList<>(referenceToSameFile.transportIndexes);
      this.routingIndexes = new ArrayList<>(referenceToSameFile.routingIndexes);
      this.indexes = new ArrayList<>(referenceToSameFile.indexes);
      this.basemap = referenceToSameFile.basemap;
      this.calculateCenterPointForRegions();
   }

   public long getDateCreated() {
      return this.dateCreated;
   }

   private void init() throws IOException {
      boolean initCorrectly = false;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (!initCorrectly) {
                  throw new IOException("Corrupt file, it should have ended as it starts with version: " + this.file.getName());
               } else {
                  return;
               }
            case 1:
               this.version = this.codedIS.readUInt32();
               break;
            case 2:
            case 3:
            case 5:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            default:
               this.skipUnknownField(t);
               break;
            case 4:
               BinaryMapTransportReaderAdapter.TransportIndex ind = new BinaryMapTransportReaderAdapter.TransportIndex();
               ind.length = this.readInt();
               ind.filePointer = this.codedIS.getTotalBytesRead();
               if (this.transportAdapter != null) {
                  int oldLimit = this.codedIS.pushLimit(ind.length);
                  this.transportAdapter.readTransportIndex(ind);
                  this.codedIS.popLimit(oldLimit);
                  this.transportIndexes.add(ind);
                  this.indexes.add(ind);
               }

               this.codedIS.seek((long)(ind.filePointer + ind.length));
               break;
            case 6:
               BinaryMapIndexReader.MapIndex mapIndex = new BinaryMapIndexReader.MapIndex();
               mapIndex.length = this.readInt();
               mapIndex.filePointer = this.codedIS.getTotalBytesRead();
               int oldLimit = this.codedIS.pushLimit(mapIndex.length);
               this.readMapIndex(mapIndex, false);
               this.basemap = this.basemap || mapIndex.isBaseMap();
               this.codedIS.popLimit(oldLimit);
               this.codedIS.seek((long)(mapIndex.filePointer + mapIndex.length));
               this.mapIndexes.add(mapIndex);
               this.indexes.add(mapIndex);
               break;
            case 7:
               BinaryMapAddressReaderAdapter.AddressRegion region = new BinaryMapAddressReaderAdapter.AddressRegion();
               region.length = this.readInt();
               region.filePointer = this.codedIS.getTotalBytesRead();
               if (this.addressAdapter != null) {
                  int limit = this.codedIS.pushLimit(region.length);
                  this.addressAdapter.readAddressIndex(region);
                  if (region.name != null) {
                     this.addressIndexes.add(region);
                     this.indexes.add(region);
                  }

                  this.codedIS.popLimit(limit);
               }

               this.codedIS.seek((long)(region.filePointer + region.length));
               break;
            case 8:
               BinaryMapPoiReaderAdapter.PoiRegion poiInd = new BinaryMapPoiReaderAdapter.PoiRegion();
               poiInd.length = this.readInt();
               poiInd.filePointer = this.codedIS.getTotalBytesRead();
               if (this.poiAdapter != null) {
                  int limit = this.codedIS.pushLimit(poiInd.length);
                  this.poiAdapter.readPoiIndex(poiInd, false);
                  this.codedIS.popLimit(limit);
                  this.poiIndexes.add(poiInd);
                  this.indexes.add(poiInd);
               }

               this.codedIS.seek((long)(poiInd.filePointer + poiInd.length));
               break;
            case 9:
               BinaryMapRouteReaderAdapter.RouteRegion routeReg = new BinaryMapRouteReaderAdapter.RouteRegion();
               routeReg.length = this.readInt();
               routeReg.filePointer = this.codedIS.getTotalBytesRead();
               if (this.routeAdapter != null) {
                  int limit = this.codedIS.pushLimit(routeReg.length);
                  this.routeAdapter.readRouteIndex(routeReg);
                  this.codedIS.popLimit(limit);
                  this.routingIndexes.add(routeReg);
                  this.indexes.add(routeReg);
               }

               this.codedIS.seek((long)(routeReg.filePointer + routeReg.length));
               break;
            case 18:
               this.dateCreated = this.codedIS.readInt64();
               break;
            case 32:
               int cversion = this.codedIS.readUInt32();
               this.calculateCenterPointForRegions();
               initCorrectly = cversion == this.version;
         }
      }
   }

   private void calculateCenterPointForRegions() {
      for(BinaryMapAddressReaderAdapter.AddressRegion reg : this.addressIndexes) {
         for(BinaryMapIndexReader.MapIndex map : this.mapIndexes) {
            if (Algorithms.objectEquals(reg.name, map.name) && map.getRoots().size() > 0) {
               reg.calculatedCenter = map.getCenterLatLon();
               break;
            }
         }

         if (reg.calculatedCenter == null) {
            for(BinaryMapRouteReaderAdapter.RouteRegion map : this.routingIndexes) {
               if (Algorithms.objectEquals(reg.name, map.name)) {
                  reg.calculatedCenter = new LatLon(
                     map.getTopLatitude() / 2.0 + map.getBottomLatitude() / 2.0, map.getLeftLongitude() / 2.0 + map.getRightLongitude() / 2.0
                  );
                  break;
               }
            }
         }
      }
   }

   public List<BinaryIndexPart> getIndexes() {
      return this.indexes;
   }

   public List<BinaryMapIndexReader.MapIndex> getMapIndexes() {
      return this.mapIndexes;
   }

   public List<BinaryMapRouteReaderAdapter.RouteRegion> getRoutingIndexes() {
      return this.routingIndexes;
   }

   public boolean isBasemap() {
      return this.basemap;
   }

   public boolean containsMapData() {
      return this.mapIndexes.size() > 0;
   }

   public boolean containsPoiData() {
      return this.poiIndexes.size() > 0;
   }

   public boolean containsRouteData() {
      return this.routingIndexes.size() > 0;
   }

   public boolean containsRouteData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
      for(BinaryMapRouteReaderAdapter.RouteRegion ri : this.routingIndexes) {
         for(BinaryMapRouteReaderAdapter.RouteSubregion r : ri.getSubregions()) {
            if (right31x >= r.left && left31x <= r.right && r.top <= bottom31y && r.bottom >= top31y) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
      for(BinaryMapPoiReaderAdapter.PoiRegion index : this.poiIndexes) {
         if (right31x >= index.left31 && left31x <= index.right31 && index.top31 <= bottom31y && index.bottom31 >= top31y) {
            return true;
         }
      }

      return false;
   }

   public boolean containsAddressData(int left31x, int top31y, int right31x, int bottom31y) {
      for(BinaryMapAddressReaderAdapter.AddressRegion index : this.addressIndexes) {
         if (right31x >= index.left31 && left31x <= index.right31 && index.top31 <= bottom31y && index.bottom31 >= top31y) {
            return true;
         }
      }

      return false;
   }

   public boolean containsMapData(int tile31x, int tile31y, int zoom) {
      for(BinaryMapIndexReader.MapIndex mapIndex : this.mapIndexes) {
         for(BinaryMapIndexReader.MapRoot root : mapIndex.getRoots()) {
            if (root.minZoom <= zoom && root.maxZoom >= zoom && tile31x >= root.left && tile31x <= root.right && root.top <= tile31y && root.bottom >= tile31y
               )
             {
               return true;
            }
         }
      }

      return false;
   }

   public boolean containsMapData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
      for(BinaryMapIndexReader.MapIndex mapIndex : this.mapIndexes) {
         for(BinaryMapIndexReader.MapRoot root : mapIndex.getRoots()) {
            if (root.minZoom <= zoom
               && root.maxZoom >= zoom
               && right31x >= root.left
               && left31x <= root.right
               && root.top <= bottom31y
               && root.bottom >= top31y) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean containsAddressData() {
      return this.addressIndexes.size() > 0;
   }

   public boolean hasTransportData() {
      return this.transportIndexes.size() > 0;
   }

   public RandomAccessFile getRaf() {
      return this.raf;
   }

   public File getFile() {
      return this.file;
   }

   public String getCountryName() {
      List<String> rg = this.getRegionNames();
      return rg.size() > 0 ? rg.get(0).split("_")[0] : "";
   }

   public String getRegionName() {
      List<String> rg = this.getRegionNames();
      if (rg.size() == 0) {
         rg.add(this.file.getName());
      }

      String ls = rg.get(0);
      if (ls.lastIndexOf(95) != -1) {
         if (!ls.matches("([a-zA-Z-]+_)+([0-9]+_){2}[0-9]+\\.obf")) {
            if (ls.contains(".")) {
               ls = ls.substring(0, ls.indexOf("."));
            }

            if (ls.endsWith("_2")) {
               ls = ls.substring(0, ls.length() - "_2".length());
            }

            if (ls.lastIndexOf(95) != -1) {
               ls = ls.substring(0, ls.lastIndexOf(95)).replace('_', ' ');
            }

            return ls;
         }

         Pattern osmDiffDateEnding = Pattern.compile("_([0-9]+_){2}[0-9]+\\.obf");
         Matcher m = osmDiffDateEnding.matcher(ls);
         if (m.find()) {
            ls = ls.substring(0, m.start());
            if (ls.lastIndexOf(95) != -1) {
               return ls.substring(0, ls.lastIndexOf(95)).replace('_', ' ');
            }

            return ls;
         }
      }

      return ls;
   }

   public int readByte() throws IOException {
      byte b = this.codedIS.readRawByte();
      return b < 0 ? b + 256 : b;
   }

   public final int readInt() throws IOException {
      int ch1 = this.readByte();
      int ch2 = this.readByte();
      int ch3 = this.readByte();
      int ch4 = this.readByte();
      return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
   }

   public int getVersion() {
      return this.version;
   }

   protected void skipUnknownField(int tag) throws IOException {
      int wireType = WireFormat.getTagWireType(tag);
      if (wireType == 6) {
         int length = this.readInt();
         this.codedIS.skipRawBytes(length);
      } else {
         this.codedIS.skipField(tag);
      }
   }

   public TIntObjectHashMap<TransportRoute> getTransportRoutes(int[] filePointers) throws IOException {
      TIntObjectHashMap<TransportRoute> result = new TIntObjectHashMap();
      this.loadTransportRoutes(filePointers, result);
      return result;
   }

   public void loadTransportRoutes(int[] filePointers, TIntObjectHashMap<TransportRoute> result) throws IOException {
      Map<BinaryMapTransportReaderAdapter.TransportIndex, TIntArrayList> groupPoints = new HashMap<>();

      for(int filePointer : filePointers) {
         BinaryMapTransportReaderAdapter.TransportIndex ind = this.getTransportIndex(filePointer);
         if (ind != null) {
            if (!groupPoints.containsKey(ind)) {
               groupPoints.put(ind, new TIntArrayList());
            }

            ((TIntArrayList)groupPoints.get(ind)).add(filePointer);
         }
      }

      for(Entry<BinaryMapTransportReaderAdapter.TransportIndex, TIntArrayList> e : groupPoints.entrySet()) {
         BinaryMapTransportReaderAdapter.TransportIndex ind = e.getKey();
         TIntArrayList pointers = (TIntArrayList)e.getValue();
         pointers.sort();
         TIntObjectHashMap<String> stringTable = new TIntObjectHashMap();
         List<TransportRoute> finishInit = new ArrayList<>();

         for(int i = 0; i < pointers.size(); ++i) {
            int filePointer = pointers.get(i);
            TransportRoute transportRoute = this.transportAdapter.getTransportRoute(filePointer, stringTable, false);
            result.put(filePointer, transportRoute);
            finishInit.add(transportRoute);
         }

         TIntObjectHashMap<String> indexedStringTable = this.transportAdapter.initializeStringTable(ind, stringTable);

         for(TransportRoute transportRoute : finishInit) {
            this.transportAdapter.initializeNames(false, transportRoute, indexedStringTable);
         }
      }
   }

   public boolean transportStopBelongsTo(TransportStop s) {
      return this.getTransportIndex(s.getFileOffset()) != null;
   }

   public List<BinaryMapTransportReaderAdapter.TransportIndex> getTransportIndexes() {
      return this.transportIndexes;
   }

   private BinaryMapTransportReaderAdapter.TransportIndex getTransportIndex(int filePointer) {
      BinaryMapTransportReaderAdapter.TransportIndex ind = null;

      for(BinaryMapTransportReaderAdapter.TransportIndex i : this.transportIndexes) {
         if (i.filePointer <= filePointer && filePointer - i.filePointer < i.length) {
            ind = i;
            break;
         }
      }

      return ind;
   }

   public boolean containTransportData(double latitude, double longitude) {
      double x = MapUtils.getTileNumberX(24.0F, longitude);
      double y = MapUtils.getTileNumberY(24.0F, latitude);

      for(BinaryMapTransportReaderAdapter.TransportIndex index : this.transportIndexes) {
         if ((double)index.right >= x && (double)index.left <= x && (double)index.top <= y && (double)index.bottom >= y) {
            return true;
         }
      }

      return false;
   }

   public boolean containTransportData(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
      double leftX = MapUtils.getTileNumberX(24.0F, leftLongitude);
      double topY = MapUtils.getTileNumberY(24.0F, topLatitude);
      double rightX = MapUtils.getTileNumberX(24.0F, rightLongitude);
      double bottomY = MapUtils.getTileNumberY(24.0F, bottomLatitude);

      for(BinaryMapTransportReaderAdapter.TransportIndex index : this.transportIndexes) {
         if ((double)index.right >= leftX && (double)index.left <= rightX && (double)index.top <= bottomY && (double)index.bottom >= topY) {
            return true;
         }
      }

      return false;
   }

   public List<TransportStop> searchTransportIndex(BinaryMapTransportReaderAdapter.TransportIndex index, BinaryMapIndexReader.SearchRequest<TransportStop> req) throws IOException {
      if (index.stopsFileLength != 0 && index.right >= req.left && index.left <= req.right && index.top <= req.bottom && index.bottom >= req.top) {
         this.codedIS.seek((long)index.stopsFileOffset);
         int oldLimit = this.codedIS.pushLimit(index.stopsFileLength);
         int offset = req.searchResults.size();
         TIntObjectHashMap<String> stringTable = new TIntObjectHashMap();
         this.transportAdapter.searchTransportTreeBounds(0, 0, 0, 0, req, stringTable);
         this.codedIS.popLimit(oldLimit);
         TIntObjectHashMap<String> indexedStringTable = this.transportAdapter.initializeStringTable(index, stringTable);

         for(int i = offset; i < req.searchResults.size(); ++i) {
            TransportStop st = req.searchResults.get(i);
            this.transportAdapter.initializeNames(indexedStringTable, st);
         }

         return req.getSearchResults();
      } else {
         return req.getSearchResults();
      }
   }

   public List<TransportStop> searchTransportIndex(BinaryMapIndexReader.SearchRequest<TransportStop> req) throws IOException {
      for(BinaryMapTransportReaderAdapter.TransportIndex index : this.transportIndexes) {
         this.searchTransportIndex(index, req);
      }

      if (req.numberOfVisitedObjects > 0 && req.log) {
         log.debug("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects.");
         log.debug("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");
      }

      return req.getSearchResults();
   }

   public List<String> getRegionNames() {
      List<String> names = new ArrayList<>();

      for(BinaryMapAddressReaderAdapter.AddressRegion r : this.addressIndexes) {
         names.add(r.name);
      }

      return names;
   }

   public LatLon getRegionCenter() {
      for(BinaryMapAddressReaderAdapter.AddressRegion r : this.addressIndexes) {
         if (r.calculatedCenter != null) {
            return r.calculatedCenter;
         }
      }

      return null;
   }

   public List<City> getCities(BinaryMapIndexReader.SearchRequest<City> resultMatcher, int cityType) throws IOException {
      return this.getCities(resultMatcher, null, null, cityType);
   }

   public List<City> getCities(BinaryMapIndexReader.SearchRequest<City> resultMatcher, StringMatcher matcher, String lang, int cityType) throws IOException {
      List<City> cities = new ArrayList<>();

      for(BinaryMapAddressReaderAdapter.AddressRegion r : this.addressIndexes) {
         for(BinaryMapAddressReaderAdapter.CitiesBlock block : r.cities) {
            if (block.type == cityType) {
               this.codedIS.seek((long)block.filePointer);
               int old = this.codedIS.pushLimit(block.length);
               this.addressAdapter.readCities(cities, resultMatcher, matcher, r.attributeTagsTable);
               this.codedIS.popLimit(old);
            }
         }
      }

      return cities;
   }

   public List<City> getCities(BinaryMapAddressReaderAdapter.AddressRegion region, BinaryMapIndexReader.SearchRequest<City> resultMatcher, int cityType) throws IOException {
      return this.getCities(region, resultMatcher, null, cityType);
   }

   public List<City> getCities(
      BinaryMapAddressReaderAdapter.AddressRegion region, BinaryMapIndexReader.SearchRequest<City> resultMatcher, StringMatcher matcher, int cityType
   ) throws IOException {
      List<City> cities = new ArrayList<>();

      for(BinaryMapAddressReaderAdapter.CitiesBlock block : region.cities) {
         if (block.type == cityType) {
            this.codedIS.seek((long)block.filePointer);
            int old = this.codedIS.pushLimit(block.length);
            this.addressAdapter.readCities(cities, resultMatcher, matcher, region.attributeTagsTable);
            this.codedIS.popLimit(old);
         }
      }

      return cities;
   }

   public int preloadStreets(City c, BinaryMapIndexReader.SearchRequest<Street> resultMatcher) throws IOException {
      BinaryMapAddressReaderAdapter.AddressRegion reg;
      try {
         reg = this.checkAddressIndex(c.getFileOffset());
      } catch (IllegalArgumentException var6) {
         throw new IOException(var6.getMessage() + " while reading " + c + " (id: " + c.getId() + ")");
      }

      this.codedIS.seek((long)c.getFileOffset());
      int size = this.codedIS.readRawVarint32();
      int old = this.codedIS.pushLimit(size);
      this.addressAdapter.readCityStreets(resultMatcher, c, reg.attributeTagsTable);
      this.codedIS.popLimit(old);
      return size;
   }

   private BinaryMapAddressReaderAdapter.AddressRegion checkAddressIndex(int offset) {
      for(BinaryMapAddressReaderAdapter.AddressRegion r : this.addressIndexes) {
         if (offset >= r.filePointer && offset <= r.length + r.filePointer) {
            return r;
         }
      }

      throw new IllegalArgumentException("Illegal offset " + offset);
   }

   public void preloadBuildings(Street s, BinaryMapIndexReader.SearchRequest<Building> resultMatcher) throws IOException {
      BinaryMapAddressReaderAdapter.AddressRegion reg = this.checkAddressIndex(s.getFileOffset());
      this.codedIS.seek((long)s.getFileOffset());
      int size = this.codedIS.readRawVarint32();
      int old = this.codedIS.pushLimit(size);
      City city = s.getCity();
      this.addressAdapter.readStreet(s, resultMatcher, true, 0, 0, city != null && city.isPostcode() ? city.getName() : null, reg.attributeTagsTable);
      this.codedIS.popLimit(old);
   }

   private void readMapIndex(BinaryMapIndexReader.MapIndex index, boolean onlyInitEncodingRules) throws IOException {
      int defaultId = 1;
      int encodingRulesSize = 0;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (onlyInitEncodingRules) {
                  index.finishInitializingTags();
               }

               return;
            case 1:
            case 3:
            default:
               this.skipUnknownField(t);
               break;
            case 2:
               index.setName(this.codedIS.readString());
               break;
            case 4:
               if (onlyInitEncodingRules) {
                  if (encodingRulesSize == 0) {
                     encodingRulesSize = this.codedIS.getTotalBytesRead();
                  }

                  int len = this.codedIS.readInt32();
                  int oldLimit = this.codedIS.pushLimit(len);
                  this.readMapEncodingRule(index, defaultId++);
                  this.codedIS.popLimit(oldLimit);
                  index.encodingRulesSizeBytes = this.codedIS.getTotalBytesRead() - encodingRulesSize;
               } else {
                  this.skipUnknownField(t);
               }
               break;
            case 5:
               int length = this.readInt();
               int filePointer = this.codedIS.getTotalBytesRead();
               if (!onlyInitEncodingRules) {
                  int oldLimit = this.codedIS.pushLimit(length);
                  BinaryMapIndexReader.MapRoot mapRoot = this.readMapLevel(new BinaryMapIndexReader.MapRoot());
                  mapRoot.length = length;
                  mapRoot.filePointer = filePointer;
                  index.getRoots().add(mapRoot);
                  this.codedIS.popLimit(oldLimit);
               }

               this.codedIS.seek((long)(filePointer + length));
         }
      }
   }

   private void readMapEncodingRule(BinaryMapIndexReader.MapIndex index, int id) throws IOException {
      int type = 0;
      String tags = null;
      String val = null;

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               index.initMapEncodingRule(type, id, tags, val);
               return;
            case 1:
            case 2:
            case 4:
            case 6:
            case 8:
            case 9:
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
               break;
            case 10:
               type = this.codedIS.readUInt32();
         }
      }
   }

   private BinaryMapIndexReader.MapRoot readMapLevel(BinaryMapIndexReader.MapRoot root) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return root;
            case 1:
               root.maxZoom = this.codedIS.readInt32();
               break;
            case 2:
               root.minZoom = this.codedIS.readInt32();
               break;
            case 3:
               root.left = this.codedIS.readInt32();
               break;
            case 4:
               root.right = this.codedIS.readInt32();
               break;
            case 5:
               root.top = this.codedIS.readInt32();
               break;
            case 6:
               root.bottom = this.codedIS.readInt32();
               break;
            case 7:
               int length = this.readInt();
               int filePointer = this.codedIS.getTotalBytesRead();
               if (root.trees != null) {
                  BinaryMapIndexReader.MapTree r = new BinaryMapIndexReader.MapTree();
                  r.length = length;
                  r.filePointer = filePointer;
                  int oldLimit = this.codedIS.pushLimit(r.length);
                  this.readMapTreeBounds(r, root.left, root.right, root.top, root.bottom);
                  root.trees.add(r);
                  this.codedIS.popLimit(oldLimit);
               }

               this.codedIS.seek((long)(filePointer + length));
               break;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            default:
               this.skipUnknownField(t);
               break;
            case 15:
               this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
         }
      }
   }

   private void readMapTreeBounds(BinaryMapIndexReader.MapTree tree, int aleft, int aright, int atop, int abottom) throws IOException {
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
               tree.left = this.codedIS.readSInt32() + aleft;
               break;
            case 2:
               tree.right = this.codedIS.readSInt32() + aright;
               break;
            case 3:
               tree.top = this.codedIS.readSInt32() + atop;
               break;
            case 4:
               tree.bottom = this.codedIS.readSInt32() + abottom;
               break;
            case 5:
               tree.mapDataBlock = (long)(this.readInt() + tree.filePointer);
               break;
            case 6:
               if (this.codedIS.readBool()) {
                  tree.ocean = Boolean.TRUE;
                  break;
               }

               tree.ocean = Boolean.FALSE;
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   public List<BinaryMapDataObject> searchMapIndex(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req) throws IOException {
      return this.searchMapIndex(req, null);
   }

   public List<BinaryMapDataObject> searchMapIndex(BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, BinaryMapIndexReader.MapIndex filterMapIndex) throws IOException {
      req.numberOfVisitedObjects = 0;
      req.numberOfAcceptedObjects = 0;
      req.numberOfAcceptedSubtrees = 0;
      req.numberOfReadSubtrees = 0;
      List<BinaryMapIndexReader.MapTree> foundSubtrees = new ArrayList<>();

      for(BinaryMapIndexReader.MapIndex mapIndex : this.mapIndexes) {
         if (filterMapIndex == null || mapIndex == filterMapIndex) {
            if (mapIndex.encodingRules.isEmpty()) {
               this.codedIS.seek((long)mapIndex.filePointer);
               int oldLimit = this.codedIS.pushLimit(mapIndex.length);
               this.readMapIndex(mapIndex, true);
               this.codedIS.popLimit(oldLimit);
            }

            for(BinaryMapIndexReader.MapRoot index : mapIndex.getRoots()) {
               if (index.minZoom <= req.zoom
                  && index.maxZoom >= req.zoom
                  && index.right >= req.left
                  && index.left <= req.right
                  && index.top <= req.bottom
                  && index.bottom >= req.top) {
                  if (index.trees == null) {
                     index.trees = new ArrayList<>();
                     this.codedIS.seek((long)index.filePointer);
                     int oldLimit = this.codedIS.pushLimit(index.length);
                     this.readMapLevel(index);
                     this.codedIS.popLimit(oldLimit);
                  }

                  for(BinaryMapIndexReader.MapTree tree : index.trees) {
                     if (tree.right >= req.left && tree.left <= req.right && tree.top <= req.bottom && tree.bottom >= req.top) {
                        this.codedIS.seek((long)tree.filePointer);
                        int oldLimit = this.codedIS.pushLimit(tree.length);
                        this.searchMapTreeBounds(tree, index, req, foundSubtrees);
                        this.codedIS.popLimit(oldLimit);
                     }
                  }

                  Collections.sort(foundSubtrees, new Comparator<BinaryMapIndexReader.MapTree>() {
                     public int compare(BinaryMapIndexReader.MapTree o1, BinaryMapIndexReader.MapTree o2) {
                        return o1.mapDataBlock < o2.mapDataBlock ? -1 : (o1.mapDataBlock == o2.mapDataBlock ? 0 : 1);
                     }
                  });

                  for(BinaryMapIndexReader.MapTree tree : foundSubtrees) {
                     if (!req.isCancelled()) {
                        this.codedIS.seek(tree.mapDataBlock);
                        int length = this.codedIS.readRawVarint32();
                        int oldLimit = this.codedIS.pushLimit(length);
                        this.readMapDataBlocks(req, tree, mapIndex);
                        this.codedIS.popLimit(oldLimit);
                     }
                  }

                  foundSubtrees.clear();
               }
            }
         }
      }

      if (req.numberOfVisitedObjects > 0 && req.log) {
         log.info("Search is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects.");
         log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");
      }

      return req.getSearchResults();
   }

   protected void readMapDataBlocks(
      BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, BinaryMapIndexReader.MapTree tree, BinaryMapIndexReader.MapIndex root
   ) throws IOException {
      List<BinaryMapDataObject> tempResults = null;
      long baseId = 0L;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               if (tempResults != null) {
                  for(BinaryMapDataObject obj : tempResults) {
                     req.publish(obj);
                  }
               }

               return;
            case 10:
               baseId = this.codedIS.readUInt64();
               if (READ_STATS) {
                  req.stat.addBlockHeader(10, 0);
               }
               break;
            case 12:
               int length = this.codedIS.readRawVarint32();
               int oldLimit = this.codedIS.pushLimit(length);
               if (READ_STATS) {
                  req.stat.lastObjectSize += length;
                  req.stat.addBlockHeader(12, length);
               }

               BinaryMapDataObject mapObject = this.readMapDataObject(tree, req, root);
               if (mapObject != null) {
                  mapObject.setId(mapObject.getId() + baseId);
                  if (READ_STATS) {
                     req.publish(mapObject);
                  }

                  if (tempResults == null) {
                     tempResults = new ArrayList<>();
                  }

                  tempResults.add(mapObject);
               }

               this.codedIS.popLimit(oldLimit);
               break;
            case 15:
               int bytesLength = this.codedIS.readRawVarint32();
               int limit = this.codedIS.pushLimit(bytesLength);
               if (READ_STATS) {
                  req.stat.addBlockHeader(15, bytesLength);
                  req.stat.lastBlockStringTableSize += bytesLength;
               }

               if (tempResults != null) {
                  List<String> stringTable = this.readStringTable();

                  for(int i = 0; i < tempResults.size(); ++i) {
                     BinaryMapDataObject rs = tempResults.get(i);
                     if (rs.objectNames != null) {
                        int[] keys = rs.objectNames.keys();

                        for(int j = 0; j < keys.length; ++j) {
                           rs.objectNames.put(keys[j], stringTable.get(((String)rs.objectNames.get(keys[j])).charAt(0)));
                        }
                     }
                  }
               } else {
                  this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
               }

               this.codedIS.popLimit(limit);
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected void searchMapTreeBounds(
      BinaryMapIndexReader.MapTree current,
      BinaryMapIndexReader.MapTree parent,
      BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req,
      List<BinaryMapIndexReader.MapTree> foundSubtrees
   ) throws IOException {
      int init = 0;
      ++req.numberOfReadSubtrees;

      while(!req.isCancelled()) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         if (init == 15) {
            init = 0;
            if (current.right < req.left || current.left > req.right || current.top > req.bottom || current.bottom < req.top) {
               return;
            }

            ++req.numberOfAcceptedSubtrees;
         }

         switch(tag) {
            case 0:
               return;
            case 1:
               current.left = this.codedIS.readSInt32() + parent.left;
               init |= 2;
               break;
            case 2:
               current.right = this.codedIS.readSInt32() + parent.right;
               init |= 4;
               break;
            case 3:
               current.top = this.codedIS.readSInt32() + parent.top;
               init |= 8;
               break;
            case 4:
               current.bottom = this.codedIS.readSInt32() + parent.bottom;
               init |= 1;
               break;
            case 5:
               ++req.numberOfAcceptedSubtrees;
               current.mapDataBlock = (long)(this.readInt() + current.filePointer);
               foundSubtrees.add(current);
               break;
            case 6:
               if (this.codedIS.readBool()) {
                  current.ocean = Boolean.TRUE;
               } else {
                  current.ocean = Boolean.FALSE;
               }

               req.publishOceanTile(current.ocean);
               break;
            case 7:
               BinaryMapIndexReader.MapTree child = new BinaryMapIndexReader.MapTree();
               child.length = this.readInt();
               child.filePointer = this.codedIS.getTotalBytesRead();
               int oldLimit = this.codedIS.pushLimit(child.length);
               if (current.ocean != null) {
                  child.ocean = current.ocean;
               }

               this.searchMapTreeBounds(child, current, req, foundSubtrees);
               this.codedIS.popLimit(oldLimit);
               this.codedIS.seek((long)(child.filePointer + child.length));
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   private BinaryMapDataObject readMapDataObject(
      BinaryMapIndexReader.MapTree tree, BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> req, BinaryMapIndexReader.MapIndex root
   ) throws IOException {
      int tag = WireFormat.getTagFieldNumber(this.codedIS.readTag());
      boolean area = 2 == tag;
      if (!area && 1 != tag) {
         throw new IllegalArgumentException();
      } else {
         req.cacheCoordinates.clear();
         int size = this.codedIS.readRawVarint32();
         if (READ_STATS) {
            req.stat.lastObjectCoordinates += size;
            req.stat.addTagHeader(1, size);
         }

         int old = this.codedIS.pushLimit(size);
         int px = tree.left & this.MASK_TO_READ;
         int py = tree.top & this.MASK_TO_READ;
         boolean contains = false;
         int minX = Integer.MAX_VALUE;
         int maxX = 0;
         int minY = Integer.MAX_VALUE;
         int maxY = 0;
         ++req.numberOfVisitedObjects;

         while(this.codedIS.getBytesUntilLimit() > 0) {
            int x = (this.codedIS.readSInt32() << 5) + px;
            int y = (this.codedIS.readSInt32() << 5) + py;
            req.cacheCoordinates.add(x);
            req.cacheCoordinates.add(y);
            px = x;
            py = y;
            if (!contains && req.left <= x && req.right >= x && req.top <= y && req.bottom >= y) {
               contains = true;
            }

            if (!contains) {
               minX = Math.min(minX, x);
               maxX = Math.max(maxX, x);
               minY = Math.min(minY, y);
               maxY = Math.max(maxY, y);
            }
         }

         if (!contains && maxX >= req.left && minX <= req.right && minY <= req.bottom && maxY >= req.top) {
            contains = true;
         }

         this.codedIS.popLimit(old);
         if (!contains) {
            this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
            return null;
         } else {
            List<TIntArrayList> innercoordinates = null;
            TIntArrayList additionalTypes = null;
            TIntObjectHashMap<String> stringNames = null;
            TIntArrayList stringOrder = null;
            long id = 0L;
            int labelX = 0;
            int labelY = 0;
            boolean loop = true;

            while(loop) {
               int t = this.codedIS.readTag();
               tag = WireFormat.getTagFieldNumber(t);
               switch(tag) {
                  case 0:
                     loop = false;
                     break;
                  case 1:
                  case 2:
                  case 3:
                  case 5:
                  case 9:
                  case 11:
                  default:
                     this.skipUnknownField(t);
                     break;
                  case 4:
                     if (innercoordinates == null) {
                        innercoordinates = new ArrayList();
                     }

                     TIntArrayList polygon = new TIntArrayList();
                     innercoordinates.add(polygon);
                     px = tree.left & this.MASK_TO_READ;
                     py = tree.top & this.MASK_TO_READ;
                     size = this.codedIS.readRawVarint32();
                     if (READ_STATS) {
                        req.stat.lastObjectCoordinates += size;
                        req.stat.addTagHeader(4, size);
                     }

                     int y;
                     for(old = this.codedIS.pushLimit(size); this.codedIS.getBytesUntilLimit() > 0; py = y) {
                        int x = (this.codedIS.readSInt32() << 5) + px;
                        y = (this.codedIS.readSInt32() << 5) + py;
                        polygon.add(x);
                        polygon.add(y);
                        px = x;
                     }

                     this.codedIS.popLimit(old);
                     break;
                  case 6:
                     additionalTypes = new TIntArrayList();
                     int sizeL = this.codedIS.readRawVarint32();
                     old = this.codedIS.pushLimit(sizeL);
                     if (READ_STATS) {
                        req.stat.lastObjectAdditionalTypes += sizeL;
                        req.stat.addTagHeader(6, sizeL);
                     }

                     while(this.codedIS.getBytesUntilLimit() > 0) {
                        additionalTypes.add(this.codedIS.readRawVarint32());
                     }

                     this.codedIS.popLimit(old);
                     break;
                  case 7:
                     req.cacheTypes.clear();
                     int bytesLength = this.codedIS.readRawVarint32();
                     old = this.codedIS.pushLimit(bytesLength);
                     if (READ_STATS) {
                        req.stat.addTagHeader(7, bytesLength);
                        req.stat.lastObjectTypes += bytesLength;
                     }

                     while(this.codedIS.getBytesUntilLimit() > 0) {
                        req.cacheTypes.add(this.codedIS.readRawVarint32());
                     }

                     this.codedIS.popLimit(old);
                     boolean accept = true;
                     if (req.searchFilter != null) {
                        accept = req.searchFilter.accept(req.cacheTypes, root);
                     }

                     if (!accept) {
                        this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
                        return null;
                     }

                     ++req.numberOfAcceptedObjects;
                     break;
                  case 8:
                     int bytesLength8 = this.codedIS.readRawVarint32();
                     old = this.codedIS.pushLimit(bytesLength8);

                     for(int i = 0; this.codedIS.getBytesUntilLimit() > 0; ++i) {
                        if (i == 0) {
                           labelX = this.codedIS.readSInt32();
                        } else if (i == 1) {
                           labelY = this.codedIS.readSInt32();
                        } else {
                           this.codedIS.readRawVarint32();
                        }
                     }

                     this.codedIS.popLimit(old);
                     if (READ_STATS) {
                        req.stat.addTagHeader(8, bytesLength8);
                        req.stat.lastObjectLabelCoordinates += bytesLength8;
                     }
                     break;
                  case 10:
                     stringNames = new TIntObjectHashMap();
                     stringOrder = new TIntArrayList();
                     int bytesLength9 = this.codedIS.readRawVarint32();
                     old = this.codedIS.pushLimit(bytesLength9);

                     while(this.codedIS.getBytesUntilLimit() > 0) {
                        int stag = this.codedIS.readRawVarint32();
                        int pId = this.codedIS.readRawVarint32();
                        stringNames.put(stag, (char)pId + "");
                        stringOrder.add(stag);
                     }

                     this.codedIS.popLimit(old);
                     if (READ_STATS) {
                        req.stat.addTagHeader(10, bytesLength9);
                        req.stat.lastStringNamesSize += bytesLength9;
                     }
                     break;
                  case 12:
                     id = this.codedIS.readSInt64();
                     if (READ_STATS) {
                        req.stat.addTagHeader(12, 0);
                        --req.stat.lastObjectHeaderInfo;
                        req.stat.lastObjectIdSize += CodedOutputStream.computeSInt64SizeNoTag(id);
                     }
               }
            }

            BinaryMapDataObject dataObject = new BinaryMapDataObject();
            dataObject.area = area;
            dataObject.coordinates = req.cacheCoordinates.toArray();
            dataObject.objectNames = stringNames;
            dataObject.namesOrder = stringOrder;
            if (innercoordinates == null) {
               dataObject.polygonInnerCoordinates = new int[0][0];
            } else {
               dataObject.polygonInnerCoordinates = new int[innercoordinates.size()][];

               for(int i = 0; i < innercoordinates.size(); ++i) {
                  dataObject.polygonInnerCoordinates[i] = ((TIntArrayList)innercoordinates.get(i)).toArray();
               }
            }

            dataObject.types = req.cacheTypes.toArray();
            if (additionalTypes != null) {
               dataObject.additionalTypes = additionalTypes.toArray();
            } else {
               dataObject.additionalTypes = new int[0];
            }

            dataObject.id = id;
            dataObject.area = area;
            dataObject.mapIndex = root;
            dataObject.labelX = labelX;
            dataObject.labelY = labelY;
            return dataObject;
         }
      }
   }

   public List<MapObject> searchAddressDataByName(BinaryMapIndexReader.SearchRequest<MapObject> req, List<Integer> typeFilter) throws IOException {
      for(BinaryMapAddressReaderAdapter.AddressRegion reg : this.addressIndexes) {
         if (reg.indexNameOffset != -1) {
            this.codedIS.seek((long)reg.indexNameOffset);
            int len = this.readInt();
            int old = this.codedIS.pushLimit(len);
            this.addressAdapter.searchAddressDataByName(reg, req, typeFilter);
            this.codedIS.popLimit(old);
         }
      }

      return req.getSearchResults();
   }

   public List<MapObject> searchAddressDataByName(BinaryMapIndexReader.SearchRequest<MapObject> req) throws IOException {
      return this.searchAddressDataByName(req, null);
   }

   public void initCategories(BinaryMapPoiReaderAdapter.PoiRegion poiIndex) throws IOException {
      this.poiAdapter.initCategories(poiIndex);
   }

   public void initCategories() throws IOException {
      for(BinaryMapPoiReaderAdapter.PoiRegion poiIndex : this.poiIndexes) {
         this.poiAdapter.initCategories(poiIndex);
      }
   }

   public List<Amenity> searchPoiByName(BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      if (req.nameQuery != null && req.nameQuery.length() != 0) {
         for(BinaryMapPoiReaderAdapter.PoiRegion poiIndex : this.poiIndexes) {
            this.poiAdapter.initCategories(poiIndex);
            this.codedIS.seek((long)poiIndex.filePointer);
            int old = this.codedIS.pushLimit(poiIndex.length);
            this.poiAdapter.searchPoiByName(poiIndex, req);
            this.codedIS.popLimit(old);
         }

         return req.getSearchResults();
      } else {
         throw new IllegalArgumentException();
      }
   }

   public Map<PoiCategory, List<String>> searchPoiCategoriesByName(String query, Map<PoiCategory, List<String>> map) throws IOException {
      if (query != null && query.length() != 0) {
         Collator collator = OsmAndCollator.primaryCollator();

         for(BinaryMapPoiReaderAdapter.PoiRegion poiIndex : this.poiIndexes) {
            this.poiAdapter.initCategories(poiIndex);

            for(int i = 0; i < poiIndex.categories.size(); ++i) {
               String cat = poiIndex.categories.get(i);
               PoiCategory catType = poiIndex.categoriesType.get(i);
               if (CollatorStringMatcher.cmatches(collator, cat, query, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
                  map.put(catType, null);
               } else {
                  List<String> subcats = poiIndex.subcategories.get(i);

                  for(int j = 0; j < subcats.size(); ++j) {
                     if (CollatorStringMatcher.cmatches(collator, subcats.get(j), query, CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
                        if (!map.containsKey(catType)) {
                           map.put(catType, new ArrayList<String>());
                        }

                        List<String> list = map.get(catType);
                        if (list != null) {
                           list.add(subcats.get(j));
                        }
                     }
                  }
               }
            }
         }

         return map;
      } else {
         throw new IllegalArgumentException();
      }
   }

   public List<BinaryMapPoiReaderAdapter.PoiSubType> searchPoiSubTypesByPrefix(String query) throws IOException {
      if (query != null && query.length() != 0) {
         List<BinaryMapPoiReaderAdapter.PoiSubType> list = new ArrayList<>();

         for(BinaryMapPoiReaderAdapter.PoiRegion poiIndex : this.poiIndexes) {
            this.poiAdapter.initCategories(poiIndex);

            for(int i = 0; i < poiIndex.subTypes.size(); ++i) {
               BinaryMapPoiReaderAdapter.PoiSubType subType = poiIndex.subTypes.get(i);
               if (subType.name.startsWith(query)) {
                  list.add(subType);
               }
            }
         }

         return list;
      } else {
         throw new IllegalArgumentException();
      }
   }

   public List<BinaryMapPoiReaderAdapter.PoiSubType> getTopIndexSubTypes() throws IOException {
      List<BinaryMapPoiReaderAdapter.PoiSubType> list = new ArrayList<>();
      for (BinaryMapPoiReaderAdapter.PoiRegion poiIndex : poiIndexes) {
         poiAdapter.initCategories(poiIndex);
         list.addAll(poiIndex.topIndexSubTypes);
      }
      return list;
   }

   public List<Amenity> searchPoi(BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      req.numberOfVisitedObjects = 0;
      req.numberOfAcceptedObjects = 0;
      req.numberOfAcceptedSubtrees = 0;
      req.numberOfReadSubtrees = 0;

      for(BinaryMapPoiReaderAdapter.PoiRegion poiIndex : this.poiIndexes) {
         this.poiAdapter.initCategories(poiIndex);
         this.codedIS.seek((long)poiIndex.filePointer);
         int old = this.codedIS.pushLimit(poiIndex.length);
         this.poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
         this.codedIS.popLimit(old);
      }

      log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");
      log.info("Search poi is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects.");
      return req.getSearchResults();
   }

   public List<Amenity> searchPoi(BinaryMapPoiReaderAdapter.PoiRegion poiIndex, BinaryMapIndexReader.SearchRequest<Amenity> req) throws IOException {
      req.numberOfVisitedObjects = 0;
      req.numberOfAcceptedObjects = 0;
      req.numberOfAcceptedSubtrees = 0;
      req.numberOfReadSubtrees = 0;
      this.poiAdapter.initCategories(poiIndex);
      this.codedIS.seek((long)poiIndex.filePointer);
      int old = this.codedIS.pushLimit(poiIndex.length);
      this.poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex);
      this.codedIS.popLimit(old);
      log.info("Search poi is done. Visit " + req.numberOfVisitedObjects + " objects. Read " + req.numberOfAcceptedObjects + " objects.");
      log.info("Read " + req.numberOfReadSubtrees + " subtrees. Go through " + req.numberOfAcceptedSubtrees + " subtrees.");
      return req.getSearchResults();
   }

   protected List<String> readStringTable() throws IOException {
      List<String> list = new ArrayList<>();

      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return list;
            case 1:
               list.add(this.codedIS.readString());
               break;
            default:
               this.skipUnknownField(t);
         }
      }
   }

   protected List<BinaryMapAddressReaderAdapter.AddressRegion> getAddressIndexes() {
      return this.addressIndexes;
   }

   public List<BinaryMapPoiReaderAdapter.PoiRegion> getPoiIndexes() {
      return this.poiIndexes;
   }

   public static BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildSearchRequest(
      int sleft, int sright, int stop, int sbottom, int zoom, BinaryMapIndexReader.SearchFilter searchFilter
   ) {
      return buildSearchRequest(sleft, sright, stop, sbottom, zoom, searchFilter, null);
   }

   public static BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> buildSearchRequest(
      int sleft, int sright, int stop, int sbottom, int zoom, BinaryMapIndexReader.SearchFilter searchFilter, ResultMatcher<BinaryMapDataObject> resultMatcher
   ) {
      BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> request = new BinaryMapIndexReader.SearchRequest<>();
      request.left = sleft;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.zoom = zoom;
      request.searchFilter = searchFilter;
      request.resultMatcher = resultMatcher;
      return request;
   }

   public static <T> BinaryMapIndexReader.SearchRequest<T> buildAddressRequest(ResultMatcher<T> resultMatcher) {
      BinaryMapIndexReader.SearchRequest<T> request = new BinaryMapIndexReader.SearchRequest<>();
      request.resultMatcher = resultMatcher;
      return request;
   }

   public static <T> BinaryMapIndexReader.SearchRequest<T> buildAddressByNameRequest(
      ResultMatcher<T> resultMatcher, String nameRequest, CollatorStringMatcher.StringMatcherMode matcherMode
   ) {
      return buildAddressByNameRequest(resultMatcher, null, nameRequest, matcherMode);
   }

   public static <T> BinaryMapIndexReader.SearchRequest<T> buildAddressByNameRequest(
      ResultMatcher<T> resultMatcher, ResultMatcher<T> rawDataCollector, String nameRequest, CollatorStringMatcher.StringMatcherMode matcherMode
   ) {
      BinaryMapIndexReader.SearchRequest<T> request = new BinaryMapIndexReader.SearchRequest<>();
      request.resultMatcher = resultMatcher;
      request.rawDataCollector = rawDataCollector;
      request.nameQuery = nameRequest.trim();
      request.matcherMode = matcherMode;
      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      List<Location> route, double radius, BinaryMapIndexReader.SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> resultMatcher
   ) {
      BinaryMapIndexReader.SearchRequest<Amenity> request = new BinaryMapIndexReader.SearchRequest<>();
      float coeff = (float)(radius / MapUtils.getTileDistanceWidth(16.0F));
      TLongObjectHashMap<List<Location>> zooms = new TLongObjectHashMap();

      for(int i = 1; i < route.size(); ++i) {
         Location cr = route.get(i);
         Location pr = route.get(i - 1);
         double tx = MapUtils.getTileNumberX(16.0F, cr.getLongitude());
         double ty = MapUtils.getTileNumberY(16.0F, cr.getLatitude());
         double px = MapUtils.getTileNumberX(16.0F, pr.getLongitude());
         double py = MapUtils.getTileNumberY(16.0F, pr.getLatitude());
         double topLeftX = Math.min(tx, px) - (double)coeff;
         double topLeftY = Math.min(ty, py) - (double)coeff;
         double bottomRightX = Math.max(tx, px) + (double)coeff;
         double bottomRightY = Math.max(ty, py) + (double)coeff;

         for(int x = (int)topLeftX; (double)x <= bottomRightX; ++x) {
            for(int y = (int)topLeftY; (double)y <= bottomRightY; ++y) {
               long hash = ((long)x << 16) + (long)y;
               if (!zooms.containsKey(hash)) {
                  zooms.put(hash, new LinkedList());
               }

               List<Location> ll = (List)zooms.get(hash);
               ll.add(pr);
               ll.add(cr);
            }
         }
      }

      int sleft = Integer.MAX_VALUE;
      int sright = 0;
      int stop = Integer.MAX_VALUE;
      int sbottom = 0;

      for(long vl : zooms.keys()) {
         long x = vl >> 16 << 15;
         long y = (vl & 65535L) << 15;
         sleft = (int)Math.min(x, (long)sleft);
         stop = (int)Math.min(y, (long)stop);
         sbottom = (int)Math.max(y, (long)sbottom);
         sright = (int)Math.max(x, (long)sright);
      }

      request.radius = radius;
      request.left = sleft;
      request.zoom = -1;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.tiles = zooms;
      request.poiTypeFilter = poiTypeFilter;
      request.resultMatcher = resultMatcher;
      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      int sleft, int sright, int stop, int sbottom, int zoom, BinaryMapIndexReader.SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> matcher
   ) {
      BinaryMapIndexReader.SearchRequest<Amenity> request = new BinaryMapIndexReader.SearchRequest<>();
      request.left = sleft;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.zoom = zoom;
      request.poiTypeFilter = poiTypeFilter;
      request.resultMatcher = matcher;
      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      LatLon latLon, int radius, int zoom, BinaryMapIndexReader.SearchPoiTypeFilter poiTypeFilter, ResultMatcher<Amenity> matcher
   ) {
      BinaryMapIndexReader.SearchRequest<Amenity> request = new BinaryMapIndexReader.SearchRequest<>();
      request.setBBoxRadius(latLon.getLatitude(), latLon.getLongitude(), radius);
      request.zoom = zoom;
      request.poiTypeFilter = poiTypeFilter;
      request.resultMatcher = matcher;
      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<RouteDataObject> buildSearchRouteRequest(
      int sleft, int sright, int stop, int sbottom, ResultMatcher<RouteDataObject> matcher
   ) {
      BinaryMapIndexReader.SearchRequest<RouteDataObject> request = new BinaryMapIndexReader.SearchRequest<>();
      request.left = sleft;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.resultMatcher = matcher;
      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      int x, int y, String nameFilter, int sleft, int sright, int stop, int sbottom, ResultMatcher<Amenity> resultMatcher
   ) {
      return buildSearchPoiRequest(x, y, nameFilter, sleft, sright, stop, sbottom, resultMatcher, null);
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      int x,
      int y,
      String nameFilter,
      int sleft,
      int sright,
      int stop,
      int sbottom,
      ResultMatcher<Amenity> resultMatcher,
      ResultMatcher<Amenity> rawDataCollector
   ) {
      return buildSearchPoiRequest(x, y, nameFilter, sleft, sright, stop, sbottom, null, resultMatcher, null);
   }

   public static BinaryMapIndexReader.SearchRequest<Amenity> buildSearchPoiRequest(
      int x,
      int y,
      String nameFilter,
      int sleft,
      int sright,
      int stop,
      int sbottom,
      BinaryMapIndexReader.SearchPoiTypeFilter poiTypeFilter,
      ResultMatcher<Amenity> resultMatcher,
      ResultMatcher<Amenity> rawDataCollector
   ) {
      BinaryMapIndexReader.SearchRequest<Amenity> request = new BinaryMapIndexReader.SearchRequest<>();
      request.x = x;
      request.y = y;
      request.left = sleft;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.poiTypeFilter = poiTypeFilter;
      request.resultMatcher = resultMatcher;
      request.rawDataCollector = rawDataCollector;
      request.nameQuery = nameFilter.trim();
      return request;
   }

   public static SearchRequest<Amenity> buildSearchPoiRequest(int sleft, int sright, int stop, int sbottom, int zoom,
                                                              SearchPoiTypeFilter poiTypeFilter, SearchPoiAdditionalFilter poiTopIndexAdditionalFilter, ResultMatcher<Amenity> matcher){
      SearchRequest<Amenity> request = new SearchRequest<Amenity>();
      request.left = sleft;
      request.right = sright;
      request.top = stop;
      request.bottom = sbottom;
      request.zoom = zoom;
      request.poiTypeFilter = poiTypeFilter;
      // request.poiAdditionalFilter = poiTopIndexAdditionalFilter;
      request.resultMatcher = matcher;

      return request;
   }

   public static BinaryMapIndexReader.SearchRequest<TransportStop> buildSearchTransportRequest(
      int sleft, int sright, int stop, int sbottom, int limit, List<TransportStop> stops
   ) {
      BinaryMapIndexReader.SearchRequest<TransportStop> request = new BinaryMapIndexReader.SearchRequest<>();
      if (stops != null) {
         request.searchResults = stops;
      }

      request.left = sleft >> 7;
      request.right = sright >> 7;
      request.top = stop >> 7;
      request.bottom = sbottom >> 7;
      request.limit = limit;
      return request;
   }

   public void close() throws IOException {
      if (this.codedIS != null) {
         this.raf.close();
         this.codedIS = null;
         this.mapIndexes.clear();
         this.addressIndexes.clear();
         this.transportIndexes.clear();
      }
   }

   private static void println(String s) {
      System.out.println(s);
   }

   public static void main(String[] args) throws IOException {
      new File(System.getProperty("maps") + "/Synthetic_test_rendering.obf");
      File fl = new File(System.getProperty("maps") + "/Wikivoyage.obf__");
      RandomAccessFile raf = new RandomAccessFile(fl, "r");
      BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
      println("VERSION " + reader.getVersion());
      long time = System.currentTimeMillis();
      if (testMapSearch) {
         testMapSearch(reader);
      }

      if (testAddressSearchName) {
         testAddressSearchByName(reader);
      }

      if (testAddressSearch) {
         testAddressSearch(reader);
      }

      if (testAddressJustifySearch) {
         testAddressJustifySearch(reader);
      }

      if (testTransportSearch) {
         testTransportSearch(reader);
      }

      if (testPoiSearch || testPoiSearchOnPath) {
         BinaryMapPoiReaderAdapter.PoiRegion poiRegion = reader.getPoiIndexes().get(0);
         if (testPoiSearch) {
            testPoiSearch(reader, poiRegion);
            testPoiSearchByName(reader);
         }

         if (testPoiSearchOnPath) {
            testSearchOnthePath(reader);
         }
      }

      println("MEMORY " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
      println("Time " + (System.currentTimeMillis() - time));
   }

   private static void testSearchOnthePath(BinaryMapIndexReader reader) throws IOException {
      float radius = 1000.0F;
      final MapPoiTypes poiTypes = MapPoiTypes.getDefault();
      long now = System.currentTimeMillis();
      println("Searching poi on the path...");
      List<Location> locations = readGPX(new File("/Users/victorshcherb/osmand/maps/2015-03-07_19-07_Sat.gpx"));
      BinaryMapIndexReader.SearchRequest<Amenity> req = buildSearchPoiRequest(locations, (double)radius, new BinaryMapIndexReader.SearchPoiTypeFilter() {
         @Override
         public boolean accept(PoiCategory type, String subcategory) {
            return type == poiTypes.getPoiCategoryByName("shop") && subcategory.contains("super");
         }

         @Override
         public boolean isEmpty() {
            return false;
         }
      }, null);
      req.zoom = -1;
      List<Amenity> results = reader.searchPoi(req);
      int k = 0;
      println("Search done in " + (System.currentTimeMillis() - now) + " ms ");
      now = System.currentTimeMillis();

      for(Amenity a : results) {
         float dds = dist(a.getLocation(), locations);
         if (dds <= radius) {
            println(
               "+ "
                  + a.getType()
                  + " "
                  + a.getSubType()
                  + " Dist "
                  + dds
                  + " (="
                  + (float)a.getRoutePoint().deviateDistance
                  + ") "
                  + a.getName()
                  + " "
                  + a.getLocation()
            );
            ++k;
         } else {
            println(a.getType() + " " + a.getSubType() + " Dist " + dds + " " + a.getName() + " " + a.getLocation());
         }
      }

      println("Filtered in " + (System.currentTimeMillis() - now) + "ms " + k + " of " + results.size());
   }

   private static float dist(LatLon l, List<Location> locations) {
      float dist = Float.POSITIVE_INFINITY;

      for(int i = 1; i < locations.size(); ++i) {
         dist = Math.min(
            dist,
            (float)MapUtils.getOrthogonalDistance(
               l.getLatitude(),
               l.getLongitude(),
               locations.get(i - 1).getLatitude(),
               locations.get(i - 1).getLongitude(),
               locations.get(i).getLatitude(),
               locations.get(i).getLongitude()
            )
         );
      }

      return dist;
   }

   private static Reader getUTF8Reader(InputStream f) throws IOException {
      BufferedInputStream bis = new BufferedInputStream(f);

      assert bis.markSupported();

      bis.mark(3);
      boolean reset = true;
      byte[] t = new byte[3];
      bis.read(t);
      if (t[0] == -17 && t[1] == -69 && t[2] == -65) {
         reset = false;
      }

      if (reset) {
         bis.reset();
      }

      return new InputStreamReader(bis, "UTF-8");
   }

   private static List<Location> readGPX(File f) {
      List<Location> res = new ArrayList<>();

      try {
         BufferedReader reader = new BufferedReader(getUTF8Reader(new FileInputStream(f)));
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder dom = factory.newDocumentBuilder();
         Document doc = dom.parse(new InputSource(reader));
         NodeList list = doc.getElementsByTagName("trkpt");

         for(int i = 0; i < list.getLength(); ++i) {
            Element item = (Element)list.item(i);

            try {
               double lon = Double.parseDouble(item.getAttribute("lon"));
               double lat = Double.parseDouble(item.getAttribute("lat"));
               Location o = new Location("");
               o.setLatitude(lat);
               o.setLongitude(lon);
               res.add(o);
            } catch (NumberFormatException var14) {
            }
         }

         return res;
      } catch (IOException var15) {
         throw new RuntimeException(var15);
      } catch (ParserConfigurationException var16) {
         throw new RuntimeException(var16);
      } catch (SAXException var17) {
         throw new RuntimeException(var17);
      }
   }

   private static void testPoiSearchByName(BinaryMapIndexReader reader) throws IOException {
      println("Searching by name...");
      BinaryMapIndexReader.SearchRequest<Amenity> req = buildSearchPoiRequest(0, 0, "central ukraine", 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, null);
      reader.searchPoiByName(req);

      for(Amenity a : req.getSearchResults()) {
         println(a.getType().getTranslation() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
      }
   }

   private static void testPoiSearch(BinaryMapIndexReader reader, BinaryMapPoiReaderAdapter.PoiRegion poiRegion) throws IOException {
      println(
         MapUtils.get31LongitudeX(poiRegion.left31)
            + " "
            + MapUtils.get31LongitudeX(poiRegion.right31)
            + " "
            + MapUtils.get31LatitudeY(poiRegion.bottom31)
            + " "
            + MapUtils.get31LatitudeY(poiRegion.top31)
      );

      for(int i = 0; i < poiRegion.categories.size(); ++i) {
         println(poiRegion.categories.get(i));
         println(" " + poiRegion.subcategories.get(i));
      }

      BinaryMapIndexReader.SearchRequest<Amenity> req = buildSearchPoiRequest(sleft, sright, stop, sbottom, -1, ACCEPT_ALL_POI_TYPE_FILTER, null);

      for(Amenity a : reader.searchPoi(req)) {
         println(a.getType() + " " + a.getSubType() + " " + a.getName() + " " + a.getLocation());
      }
   }

   private static void testTransportSearch(BinaryMapIndexReader reader) throws IOException {
      for(BinaryMapTransportReaderAdapter.TransportIndex i : reader.transportIndexes) {
         println("Transport bounds : " + i.left + " " + i.right + " " + i.top + " " + i.bottom);
      }

      for(TransportStop s : reader.searchTransportIndex(buildSearchTransportRequest(sleft, sright, stop, sbottom, -1, null))) {
         println(s.getName());
         TIntObjectHashMap<TransportRoute> routes = reader.getTransportRoutes(s.getReferencesToRoutes());

         for(TransportRoute route : routes.valueCollection()) {
            println(" " + route.getRef() + " " + route.getName() + " " + route.getDistance() + " " + route.getAvgBothDistance());
            StringBuilder b = new StringBuilder();
            if (route.getForwardWays() != null) {
               for(Way w : route.getForwardWays()) {
                  b.append(w.getNodes()).append(" ");
               }

               println("  forward ways: " + b.toString());
            }
         }
      }
   }

   private static void updateFrequence(Map<String, Integer> street, String key) {
      if (!street.containsKey(key)) {
         street.put(key, 1);
      } else {
         street.put(key, street.get(key) + 1);
      }
   }

   void readIndexedStringTable(Collator instance, List<String> queries, String prefix, List<TIntArrayList> listOffsets, TIntArrayList matchedCharacters) throws IOException {
      String key = null;
      boolean[] matched = new boolean[matchedCharacters.size()];
      boolean shouldWeReadSubtable = false;

      label84:
      while(true) {
         int t = this.codedIS.readTag();
         int tag = WireFormat.getTagFieldNumber(t);
         switch(tag) {
            case 0:
               return;
            case 1:
            case 2:
            default:
               this.skipUnknownField(t);
               continue;
            case 3:
               key = this.codedIS.readString();
               if (prefix.length() > 0) {
                  key = prefix + key;
               }

               shouldWeReadSubtable = false;
               int i = 0;

               while(true) {
                  if (i >= queries.size()) {
                     continue label84;
                  }

                  int charMatches = matchedCharacters.get(i);
                  String query = queries.get(i);
                  matched[i] = false;
                  if (query != null) {
                     if (CollatorStringMatcher.cmatches(instance, key, query, CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH)) {
                        if (query.length() >= charMatches) {
                           if (query.length() > charMatches) {
                              matchedCharacters.set(i, query.length());
                              ((TIntArrayList)listOffsets.get(i)).clear();
                           }

                           matched[i] = true;
                        }
                     } else if (CollatorStringMatcher.cmatches(instance, query, key, CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH)
                        && key.length() >= charMatches) {
                        if (key.length() > charMatches) {
                           matchedCharacters.set(i, key.length());
                           ((TIntArrayList)listOffsets.get(i)).clear();
                        }

                        matched[i] = true;
                     }

                     shouldWeReadSubtable |= matched[i];
                  }

                  ++i;
               }
            case 4:
               int val = this.readInt();
               int index = 0;

               while(true) {
                  if (index >= queries.size()) {
                     continue label84;
                  }

                  if (matched[index]) {
                     ((TIntArrayList)listOffsets.get(index)).add(val);
                  }

                  ++index;
               }
            case 5:
         }

         int len = this.codedIS.readRawVarint32();
         int oldLim = this.codedIS.pushLimit(len);
         if (shouldWeReadSubtable && key != null) {
            List<String> subqueries = new ArrayList<>(queries);

            for(int i = 0; i < queries.size(); ++i) {
               if (!matched[i]) {
                  subqueries.set(i, null);
               }
            }

            this.readIndexedStringTable(instance, subqueries, key, listOffsets, matchedCharacters);
         } else {
            this.codedIS.skipRawBytes(this.codedIS.getBytesUntilLimit());
         }

         this.codedIS.popLimit(oldLim);
      }
   }

   private static void testAddressSearchByName(BinaryMapIndexReader reader) throws IOException {
      BinaryMapIndexReader.SearchRequest<MapObject> req = buildAddressByNameRequest(new ResultMatcher<MapObject>() {
         public boolean publish(MapObject object) {
            if (object instanceof Street) {
               System.out.println(object + " " + ((Street)object).getCity());
            } else {
               System.out.println(object + " " + object.getId());
            }

            return false;
         }

         @Override
         public boolean isCancelled() {
            return false;
         }
      }, "Guy'", CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH);
      reader.searchAddressDataByName(req);
   }

   private static void testAddressJustifySearch(BinaryMapIndexReader reader) throws IOException {
      String streetName = "Logger";
      double lat = 52.28212;
      double lon = 4.86269;
      final List<Street> streetsList = new ArrayList<>();
      BinaryMapIndexReader.SearchRequest<MapObject> req = buildAddressByNameRequest(new ResultMatcher<MapObject>() {
         public boolean publish(MapObject object) {
            if (!(object instanceof Street) || !object.getName().equalsIgnoreCase("Logger")) {
               return false;
            } else if (MapUtils.getDistance(object.getLocation(), 52.28212, 4.86269) < 20000.0) {
               streetsList.add((Street)object);
               return true;
            } else {
               return false;
            }
         }

         @Override
         public boolean isCancelled() {
            return false;
         }
      }, "Logger", CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
      reader.searchAddressDataByName(req);
      TreeMap<MapObject, Street> resMap = new TreeMap<>(new Comparator<MapObject>() {
         public int compare(MapObject o1, MapObject o2) {
            LatLon l1 = o1.getLocation();
            LatLon l2 = o2.getLocation();
            if (l1 != null && l2 != null) {
               return Double.compare(MapUtils.getDistance(l1, 52.28212, 4.86269), MapUtils.getDistance(l2, 52.28212, 4.86269));
            } else {
               return l2 == l1 ? 0 : (l1 == null ? -1 : 1);
            }
         }
      });

      for(Street s : streetsList) {
         resMap.put(s, s);
         reader.preloadBuildings(s, null);

         for(Building b : s.getBuildings()) {
            if (MapUtils.getDistance(b.getLocation(), 52.28212, 4.86269) < 100.0) {
               resMap.put(b, s);
            }
         }
      }

      for(Entry<MapObject, Street> entry : resMap.entrySet()) {
         MapObject e = entry.getKey();
         Street s = entry.getValue();
         if (e instanceof Building && MapUtils.getDistance(e.getLocation(), 52.28212, 4.86269) < 40.0) {
            Building b = (Building)e;
            System.out.println(b.getName() + "   " + s);
         } else if (e instanceof Street) {
            System.out.println(s + "   " + s.getCity());
         }
      }
   }

   private static void testAddressSearch(BinaryMapIndexReader reader) throws IOException {
      final Map<String, Integer> streetFreq = new HashMap<>();

      for(City c : reader.getCities(null, 1)) {
         int buildings = 0;
         reader.preloadStreets(c, null);

         for(Street s : c.getStreets()) {
            updateFrequence(streetFreq, s.getName());
            reader.preloadBuildings(s, buildAddressRequest((ResultMatcher<Building>)null));
            buildings += s.getBuildings().size();
            println(s.getName() + " " + s.getName("ru"));
         }

         println(c.getName() + " " + c.getLocation() + " " + c.getStreets().size() + " " + buildings + " " + c.getEnName(true) + " " + c.getName("ru"));
      }

      List<City> villages = reader.getCities(buildAddressRequest((ResultMatcher<City>)null), 3);

      for(City v : villages) {
         reader.preloadStreets(v, null);

         for(Street s : v.getStreets()) {
            updateFrequence(streetFreq, s.getName());
         }
      }

      System.out.println("Villages " + villages.size());
      List<String> sorted = new ArrayList<>(streetFreq.keySet());
      Collections.sort(sorted, new Comparator<String>() {
         public int compare(String o1, String o2) {
            return -streetFreq.get(o1) + streetFreq.get(o2);
         }
      });
      System.out.println(streetFreq.size());

      for(String s : sorted) {
         System.out.println(s + "   " + streetFreq.get(s));
         if (streetFreq.get(s) < 10) {
            break;
         }
      }
   }

   private static void testMapSearch(BinaryMapIndexReader reader) throws IOException {
      println(reader.mapIndexes.get(0).encodingRules + "");
      println("SEARCH " + sleft + " " + sright + " " + stop + " " + sbottom);
      reader.searchMapIndex(buildSearchRequest(sleft, sright, stop, sbottom, szoom, null, new ResultMatcher<BinaryMapDataObject>() {
         public boolean publish(BinaryMapDataObject obj) {
            StringBuilder b = new StringBuilder();
            b.append(obj.area ? "Area" : (obj.getPointsLength() > 1 ? "Way" : "Point"));
            int[] types = obj.getTypes();
            b.append(" types [");

            for(int j = 0; j < types.length; ++j) {
               if (j > 0) {
                  b.append(", ");
               }

               BinaryMapIndexReader.TagValuePair pair = obj.getMapIndex().decodeType(types[j]);
               if (pair == null) {
                  throw new NullPointerException("Type " + types[j] + "was not found");
               }

               b.append(pair.toSimpleString()).append("(").append(types[j]).append(")");
            }

            b.append("]");
            if (obj.getAdditionalTypes() != null && obj.getAdditionalTypes().length > 0) {
               b.append(" add_types [");

               for(int j = 0; j < obj.getAdditionalTypes().length; ++j) {
                  if (j > 0) {
                     b.append(", ");
                  }

                  BinaryMapIndexReader.TagValuePair pair = obj.getMapIndex().decodeType(obj.getAdditionalTypes()[j]);
                  if (pair == null) {
                     throw new NullPointerException("Type " + obj.getAdditionalTypes()[j] + "was not found");
                  }

                  b.append(pair.toSimpleString()).append("(").append(obj.getAdditionalTypes()[j]).append(")");
               }

               b.append("]");
            }

            TIntObjectHashMap<String> names = obj.getObjectNames();
            if (names != null && !names.isEmpty()) {
               b.append(" Names [");
               int[] keys = names.keys();

               for(int j = 0; j < keys.length; ++j) {
                  if (j > 0) {
                     b.append(", ");
                  }

                  BinaryMapIndexReader.TagValuePair pair = obj.getMapIndex().decodeType(keys[j]);
                  if (pair == null) {
                     throw new NullPointerException("Type " + keys[j] + "was not found");
                  }

                  b.append(pair.toSimpleString()).append("(").append(keys[j]).append(")");
                  b.append(" - ").append((String)names.get(keys[j]));
               }

               b.append("]");
            }

            b.append(" id ").append(obj.getId() >> 1);
            b.append(" lat/lon : ");

            for(int i = 0; i < obj.getPointsLength(); ++i) {
               float x = (float)MapUtils.get31LongitudeX(obj.getPoint31XTile(i));
               float y = (float)MapUtils.get31LatitudeY(obj.getPoint31YTile(i));
               b.append(x).append(" / ").append(y).append(" , ");
            }

            BinaryMapIndexReader.println(b.toString());
            return false;
         }

         @Override
         public boolean isCancelled() {
            return false;
         }
      }));
   }

   public List<BinaryMapRouteReaderAdapter.RouteSubregion> searchRouteIndexTree(
      BinaryMapIndexReader.SearchRequest<?> req, List<BinaryMapRouteReaderAdapter.RouteSubregion> list
   ) throws IOException {
      req.numberOfVisitedObjects = 0;
      req.numberOfAcceptedObjects = 0;
      req.numberOfAcceptedSubtrees = 0;
      req.numberOfReadSubtrees = 0;
      if (this.routeAdapter != null) {
         this.routeAdapter.initRouteTypesIfNeeded(req, list);
         return this.routeAdapter.searchRouteRegionTree(req, list, new ArrayList());
      } else {
         return Collections.emptyList();
      }
   }

   public void loadRouteIndexData(List<BinaryMapRouteReaderAdapter.RouteSubregion> toLoad, ResultMatcher<RouteDataObject> matcher) throws IOException {
      if (this.routeAdapter != null) {
         this.routeAdapter.loadRouteRegionData(toLoad, matcher);
      }
   }

   public List<RouteDataObject> loadRouteIndexData(BinaryMapRouteReaderAdapter.RouteSubregion rs) throws IOException {
      return this.routeAdapter != null ? this.routeAdapter.loadRouteRegionData(rs) : Collections.<RouteDataObject>emptyList();
   }

   public void initRouteRegion(BinaryMapRouteReaderAdapter.RouteRegion routeReg) throws IOException {
      if (this.routeAdapter != null) {
         this.routeAdapter.initRouteRegion(routeReg);
      }
   }

   public TLongObjectHashMap<IncompleteTransportRoute> getIncompleteTransportRoutes() throws InvalidProtocolBufferException, IOException {
      if (this.incompleteTransportRoutes == null) {
         this.incompleteTransportRoutes = new TLongObjectHashMap();

         for(BinaryMapTransportReaderAdapter.TransportIndex ti : this.transportIndexes) {
            if (ti.incompleteRoutesLength > 0) {
               this.codedIS.seek((long)ti.incompleteRoutesOffset);
               int oldLimit = this.codedIS.pushLimit(ti.incompleteRoutesLength);
               this.transportAdapter.readIncompleteRoutesList(this.incompleteTransportRoutes, ti.filePointer);
               this.codedIS.popLimit(oldLimit);
            }
         }
      }

      return this.incompleteTransportRoutes;
   }

   public static class MapIndex extends BinaryIndexPart {
      List<BinaryMapIndexReader.MapRoot> roots = new ArrayList<>();
      Map<String, Map<String, Integer>> encodingRules = new HashMap<>();
      public TIntObjectMap<BinaryMapIndexReader.TagValuePair> decodingRules = new TIntObjectHashMap();
      public int nameEncodingType = 0;
      public int nameEnEncodingType = -1;
      public int refEncodingType = -1;
      public int coastlineEncodingType = -1;
      public int coastlineBrokenEncodingType = -1;
      public int landEncodingType = -1;
      public int onewayAttribute = -1;
      public int onewayReverseAttribute = -1;
      public TIntHashSet positiveLayers = new TIntHashSet(2);
      public TIntHashSet negativeLayers = new TIntHashSet(2);
      public int encodingRulesSizeBytes;

      public Integer getRule(String t, String v) {
         Map<String, Integer> m = this.encodingRules.get(t);
         return m != null ? m.get(v) : null;
      }

      public LatLon getCenterLatLon() {
         if (this.roots.size() == 0) {
            return null;
         } else {
            BinaryMapIndexReader.MapRoot mapRoot = this.roots.get(this.roots.size() - 1);
            double cy = (MapUtils.get31LatitudeY(mapRoot.getBottom()) + MapUtils.get31LatitudeY(mapRoot.getTop())) / 2.0;
            double cx = (MapUtils.get31LongitudeX(mapRoot.getLeft()) + MapUtils.get31LongitudeX(mapRoot.getRight())) / 2.0;
            return new LatLon(cy, cx);
         }
      }

      public List<BinaryMapIndexReader.MapRoot> getRoots() {
         return this.roots;
      }

      public BinaryMapIndexReader.TagValuePair decodeType(int type) {
         return (BinaryMapIndexReader.TagValuePair)this.decodingRules.get(type);
      }

      public Integer getRule(BinaryMapIndexReader.TagValuePair tv) {
         Map<String, Integer> m = this.encodingRules.get(tv.tag);
         return m != null ? m.get(tv.value) : null;
      }

      public void finishInitializingTags() {
         int free = this.decodingRules.size();
         this.coastlineBrokenEncodingType = free++;
         this.initMapEncodingRule(0, this.coastlineBrokenEncodingType, "natural", "coastline_broken");
         if (this.landEncodingType == -1) {
            this.landEncodingType = free++;
            this.initMapEncodingRule(0, this.landEncodingType, "natural", "land");
         }
      }

      public boolean isRegisteredRule(int id) {
         return this.decodingRules.containsKey(id);
      }

      public void initMapEncodingRule(int type, int id, String tag, String val) {
         if (!this.encodingRules.containsKey(tag)) {
            this.encodingRules.put(tag, new HashMap());
         }

         this.encodingRules.get(tag).put(val, id);
         if (!this.decodingRules.containsKey(id)) {
            this.decodingRules.put(id, new BinaryMapIndexReader.TagValuePair(tag, val, type));
         }

         if ("name".equals(tag)) {
            this.nameEncodingType = id;
         } else if ("natural".equals(tag) && "coastline".equals(val)) {
            this.coastlineEncodingType = id;
         } else if ("natural".equals(tag) && "land".equals(val)) {
            this.landEncodingType = id;
         } else if ("oneway".equals(tag) && "yes".equals(val)) {
            this.onewayAttribute = id;
         } else if ("oneway".equals(tag) && "-1".equals(val)) {
            this.onewayReverseAttribute = id;
         } else if ("ref".equals(tag)) {
            this.refEncodingType = id;
         } else if ("name:en".equals(tag)) {
            this.nameEnEncodingType = id;
         } else if ("tunnel".equals(tag)) {
            this.negativeLayers.add(id);
         } else if ("bridge".equals(tag)) {
            this.positiveLayers.add(id);
         } else if ("layer".equals(tag) && val != null && !val.equals("0") && val.length() > 0) {
            if (val.startsWith("-")) {
               this.negativeLayers.add(id);
            } else {
               this.positiveLayers.add(id);
            }
         }
      }

      public boolean isBaseMap() {
         return this.name != null && this.name.toLowerCase().contains("basemap");
      }

      @Override
      public String getPartName() {
         return "Map";
      }

      @Override
      public int getFieldNumber() {
         return 6;
      }

   }

   public static class MapObjectStat {
      public int lastStringNamesSize;
      public int lastObjectIdSize;
      public int lastObjectHeaderInfo;
      public int lastObjectAdditionalTypes;
      public int lastObjectTypes;
      public int lastObjectCoordinates;
      public int lastObjectLabelCoordinates;
      public int lastObjectSize;
      public int lastBlockStringTableSize;
      public int lastBlockHeaderInfo;

      public void addBlockHeader(int typesFieldNumber, int sizeL) {
         this.lastBlockHeaderInfo += CodedOutputStream.computeTagSize(typesFieldNumber) + CodedOutputStream.computeRawVarint32Size(sizeL);
      }

      public void addTagHeader(int typesFieldNumber, int sizeL) {
         this.lastObjectHeaderInfo += CodedOutputStream.computeTagSize(typesFieldNumber) + CodedOutputStream.computeRawVarint32Size(sizeL);
      }

      public void clearObjectStats() {
         this.lastStringNamesSize = 0;
         this.lastObjectIdSize = 0;
         this.lastObjectHeaderInfo = 0;
         this.lastObjectAdditionalTypes = 0;
         this.lastObjectTypes = 0;
         this.lastObjectCoordinates = 0;
         this.lastObjectLabelCoordinates = 0;
      }
   }

   public static class MapRoot extends BinaryMapIndexReader.MapTree {
      int minZoom = 0;
      int maxZoom = 0;
      private List<BinaryMapIndexReader.MapTree> trees = null;

      public int getMinZoom() {
         return this.minZoom;
      }

      public int getMaxZoom() {
         return this.maxZoom;
      }
   }

   private static class MapTree {
      int filePointer = 0;
      int length = 0;
      long mapDataBlock = 0L;
      Boolean ocean = null;
      int left = 0;
      int right = 0;
      int top = 0;
      int bottom = 0;

      private MapTree() {
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

      public int getLength() {
         return this.length;
      }

      public int getFilePointer() {
         return this.filePointer;
      }

      @Override
      public String toString() {
         return "Top Lat "
            + (float)MapUtils.get31LatitudeY(this.top)
            + " lon "
            + (float)MapUtils.get31LongitudeX(this.left)
            + " Bottom lat "
            + (float)MapUtils.get31LatitudeY(this.bottom)
            + " lon "
            + (float)MapUtils.get31LongitudeX(this.right);
      }
   }

   public interface SearchFilter {
      boolean accept(TIntArrayList var1, BinaryMapIndexReader.MapIndex var2);
   }

   public interface SearchPoiTypeFilter {
      boolean accept(PoiCategory var1, String var2);

      boolean isEmpty();
   }

   public static interface SearchPoiAdditionalFilter {
      public boolean accept(BinaryMapPoiReaderAdapter.PoiSubType poiSubType, String value);
      String getName();
      String getIconResource();
   }

   public static class SearchRequest<T> {
      public static final int ZOOM_TO_SEARCH_POI = 16;
      private List<T> searchResults = new ArrayList<>();
      private boolean land = false;
      private boolean ocean = false;
      private ResultMatcher<T> resultMatcher;
      private ResultMatcher<T> rawDataCollector;
      int x = 0;
      int y = 0;
      int left = 0;
      int right = 0;
      int top = 0;
      int bottom = 0;
      int zoom = 15;
      int limit = -1;
      TLongObjectHashMap<List<Location>> tiles = null;
      double radius = -1.0;
      String nameQuery = null;
      CollatorStringMatcher.StringMatcherMode matcherMode = CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
      BinaryMapIndexReader.SearchFilter searchFilter = null;
      BinaryMapIndexReader.SearchPoiTypeFilter poiTypeFilter = null;
      TIntArrayList cacheCoordinates = new TIntArrayList();
      TIntArrayList cacheTypes = new TIntArrayList();
      TLongArrayList cacheIdsA = new TLongArrayList();
      TLongArrayList cacheIdsB = new TLongArrayList();
      BinaryMapIndexReader.MapObjectStat stat = new BinaryMapIndexReader.MapObjectStat();
      public boolean log = true;
      int numberOfVisitedObjects = 0;
      int numberOfAcceptedObjects = 0;
      int numberOfReadSubtrees = 0;
      int numberOfAcceptedSubtrees = 0;
      boolean interrupted = false;

      public BinaryMapIndexReader.MapObjectStat getStat() {
         return this.stat;
      }

      protected SearchRequest() {
      }

      public long getTileHashOnPath(double lat, double lon) {
         long x = (long)((int)MapUtils.getTileNumberX(16.0F, lon));
         long y = (long)((int)MapUtils.getTileNumberY(16.0F, lat));
         return x << 16 | y;
      }

      public void setBBoxRadius(double lat, double lon, int radiusMeters) {
         System.out.println("setBBoxRadius radiusInMeters " + radiusMeters);
         double dx = MapUtils.getTileNumberX(16.0F, lon);
         double half16t = MapUtils.getDistance(
            lat, MapUtils.getLongitudeFromTile(16.0, (double)((int)dx) + 0.5), lat, MapUtils.getLongitudeFromTile(16.0, (double)((int)dx))
         );
         double cf31 = (double)radiusMeters / (half16t * 2.0) * 32768.0;
         this.y = MapUtils.get31TileNumberY(lat);
         this.x = MapUtils.get31TileNumberX(lon);
         this.left = (int)((double)this.x - cf31);
         this.right = (int)((double)this.x + cf31);
         this.top = (int)((double)this.y - cf31);
         this.bottom = (int)((double)this.y + cf31);
      }

      public boolean publish(T obj) {
         if (this.resultMatcher != null && !this.resultMatcher.publish(obj)) {
            return false;
         } else {
            this.searchResults.add(obj);
            return true;
         }
      }

      public void collectRawData(T obj) {
         if (this.rawDataCollector != null) {
            this.rawDataCollector.publish(obj);
         }
      }

      protected void publishOceanTile(boolean ocean) {
         if (ocean) {
            this.ocean = true;
         } else {
            this.land = true;
         }
      }

      public List<T> getSearchResults() {
         return this.searchResults;
      }

      public void setInterrupted(boolean interrupted) {
         this.interrupted = interrupted;
      }

      public boolean limitExceeded() {
         return this.limit != -1 && this.searchResults.size() > this.limit;
      }

      public void setLimit(int limit) {
         this.limit = limit;
      }

      public boolean isCancelled() {
         if (this.interrupted) {
            return this.interrupted;
         } else {
            return this.resultMatcher != null ? this.resultMatcher.isCancelled() : false;
         }
      }

      public boolean isOcean() {
         return this.ocean;
      }

      public boolean isLand() {
         return this.land;
      }

      public boolean intersects(int l, int t, int r, int b) {
         return r >= this.left && l <= this.right && t <= this.bottom && b >= this.top;
      }

      public boolean contains(int l, int t, int r, int b) {
         return r <= this.right && l >= this.left && b <= this.bottom && t >= this.top;
      }

      public int getLeft() {
         return this.left;
      }

      public int getRight() {
         return this.right;
      }

      public int getBottom() {
         return this.bottom;
      }

      public int getTop() {
         return this.top;
      }

      public int getZoom() {
         return this.zoom;
      }

      public void clearSearchResults() {
         this.searchResults = new ArrayList<>();
         this.cacheCoordinates.clear();
         this.cacheTypes.clear();
         this.land = false;
         this.ocean = false;
         this.numberOfVisitedObjects = 0;
         this.numberOfAcceptedObjects = 0;
         this.numberOfReadSubtrees = 0;
         this.numberOfAcceptedSubtrees = 0;
      }

      public boolean isBboxSpecified() {
         return this.left != 0 || this.right != 0;
      }
   }

   public static class TagValuePair {
      public String tag;
      public String value;
      public int additionalAttribute;

      public TagValuePair(String tag, String value, int additionalAttribute) {
         this.tag = tag;
         this.value = value;
         this.additionalAttribute = additionalAttribute;
      }

      public boolean isAdditional() {
         return this.additionalAttribute % 2 == 1;
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + this.additionalAttribute;
         result = 31 * result + (this.tag == null ? 0 : this.tag.hashCode());
         return 31 * result + (this.value == null ? 0 : this.value.hashCode());
      }

      public String toSimpleString() {
         return this.value == null ? this.tag : this.tag + "-" + this.value;
      }

      @Override
      public String toString() {
         return "TagValuePair : " + this.tag + " - " + this.value;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            BinaryMapIndexReader.TagValuePair other = (BinaryMapIndexReader.TagValuePair)obj;
            if (this.additionalAttribute != other.additionalAttribute) {
               return false;
            } else {
               if (this.tag == null) {
                  if (other.tag != null) {
                     return false;
                  }
               } else if (!this.tag.equals(other.tag)) {
                  return false;
               }

               if (this.value == null) {
                  if (other.value != null) {
                     return false;
                  }
               } else if (!this.value.equals(other.value)) {
                  return false;
               }

               return true;
            }
         }
      }
   }
}
