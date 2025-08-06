package net.osmand.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

public class CachedOsmandIndexes {
   private OsmandIndex.OsmAndStoredIndex storedIndex;
   private OsmandIndex.OsmAndStoredIndex.Builder storedIndexBuilder;
   private Log log = PlatformUtil.getLog(CachedOsmandIndexes.class);
   private boolean hasChanged = false;
   public static final String INDEXES_DEFAULT_FILENAME = "indexes.cache";
   public static final int VERSION = 2;

   public OsmandIndex.FileIndex addToCache(BinaryMapIndexReader reader, File f) {
      this.hasChanged = true;
      if (this.storedIndexBuilder == null) {
         this.storedIndexBuilder = OsmandIndex.OsmAndStoredIndex.newBuilder();
         this.storedIndexBuilder.setVersion(2);
         this.storedIndexBuilder.setDateCreated(System.currentTimeMillis());
         if (this.storedIndex != null) {
            for(OsmandIndex.FileIndex ex : this.storedIndex.getFileIndexList()) {
               if (!ex.getFileName().equals(f.getName())) {
                  this.storedIndexBuilder.addFileIndex(ex);
               }
            }
         }
      }

      OsmandIndex.FileIndex.Builder fileIndex = OsmandIndex.FileIndex.newBuilder();
      long d = reader.getDateCreated();
      fileIndex.setDateModified(d == 0L ? f.lastModified() : d);
      fileIndex.setSize(f.length());
      fileIndex.setVersion(reader.getVersion());
      fileIndex.setFileName(f.getName());

      for(BinaryMapIndexReader.MapIndex index : reader.getMapIndexes()) {
         OsmandIndex.MapPart.Builder map = OsmandIndex.MapPart.newBuilder();
         map.setSize((long)index.getLength());
         map.setOffset((long)index.getFilePointer());
         if (index.getName() != null) {
            map.setName(index.getName());
         }

         for(BinaryMapIndexReader.MapRoot mr : index.getRoots()) {
            OsmandIndex.MapLevel.Builder lev = OsmandIndex.MapLevel.newBuilder();
            lev.setSize((long)mr.length);
            lev.setOffset((long)mr.filePointer);
            lev.setLeft(mr.left);
            lev.setRight(mr.right);
            lev.setTop(mr.top);
            lev.setBottom(mr.bottom);
            lev.setMinzoom(mr.minZoom);
            lev.setMaxzoom(mr.maxZoom);
            map.addLevels(lev);
         }

         fileIndex.addMapIndex(map);
      }

      for(BinaryMapAddressReaderAdapter.AddressRegion index : reader.getAddressIndexes()) {
         OsmandIndex.AddressPart.Builder addr = OsmandIndex.AddressPart.newBuilder();
         addr.setSize((long)index.getLength());
         addr.setOffset((long)index.getFilePointer());
         if (index.getName() != null) {
            addr.setName(index.getName());
         }

         if (index.getEnName() != null) {
            addr.setNameEn(index.getEnName());
         }

         addr.setIndexNameOffset(index.getIndexNameOffset());

         for(BinaryMapAddressReaderAdapter.CitiesBlock mr : index.getCities()) {
            OsmandIndex.CityBlock.Builder cblock = OsmandIndex.CityBlock.newBuilder();
            cblock.setSize((long)mr.length);
            cblock.setOffset((long)mr.filePointer);
            cblock.setType(mr.type);
            addr.addCities(cblock);
         }

         for(String s : index.getAttributeTagsTable()) {
            addr.addAdditionalTags(s);
         }

         fileIndex.addAddressIndex(addr);
      }

      for(BinaryMapPoiReaderAdapter.PoiRegion index : reader.getPoiIndexes()) {
         OsmandIndex.PoiPart.Builder poi = OsmandIndex.PoiPart.newBuilder();
         poi.setSize((long)index.getLength());
         poi.setOffset((long)index.getFilePointer());
         if (index.getName() != null) {
            poi.setName(index.getName());
         }

         poi.setLeft(index.left31);
         poi.setRight(index.right31);
         poi.setTop(index.top31);
         poi.setBottom(index.bottom31);
         fileIndex.addPoiIndex(poi.build());
      }

      for(BinaryMapTransportReaderAdapter.TransportIndex index : reader.getTransportIndexes()) {
         OsmandIndex.TransportPart.Builder transport = OsmandIndex.TransportPart.newBuilder();
         transport.setSize((long)index.getLength());
         transport.setOffset((long)index.getFilePointer());
         if (index.getName() != null) {
            transport.setName(index.getName());
         }

         transport.setLeft(index.getLeft());
         transport.setRight(index.getRight());
         transport.setTop(index.getTop());
         transport.setBottom(index.getBottom());
         transport.setStopsTableLength(index.stopsFileLength);
         transport.setStopsTableOffset(index.stopsFileOffset);
         transport.setIncompleteRoutesLength(index.incompleteRoutesLength);
         transport.setIncompleteRoutesOffset(index.incompleteRoutesOffset);
         transport.setStringTableLength(index.stringTable.length);
         transport.setStringTableOffset(index.stringTable.fileOffset);
         fileIndex.addTransportIndex(transport);
      }

      for(BinaryMapRouteReaderAdapter.RouteRegion index : reader.getRoutingIndexes()) {
         OsmandIndex.RoutingPart.Builder routing = OsmandIndex.RoutingPart.newBuilder();
         routing.setSize((long)index.getLength());
         routing.setOffset((long)index.getFilePointer());
         if (index.getName() != null) {
            routing.setName(index.getName());
         }

         for(BinaryMapRouteReaderAdapter.RouteSubregion sub : index.getSubregions()) {
            this.addRouteSubregion(routing, sub, false);
         }

         for(BinaryMapRouteReaderAdapter.RouteSubregion sub : index.getBaseSubregions()) {
            this.addRouteSubregion(routing, sub, true);
         }

         fileIndex.addRoutingIndex(routing);
      }

      OsmandIndex.FileIndex fi = fileIndex.build();
      this.storedIndexBuilder.addFileIndex(fi);
      return fi;
   }

   private void addRouteSubregion(OsmandIndex.RoutingPart.Builder routing, BinaryMapRouteReaderAdapter.RouteSubregion sub, boolean base) {
      OsmandIndex.RoutingSubregion.Builder rpart = OsmandIndex.RoutingSubregion.newBuilder();
      rpart.setSize((long)sub.length);
      rpart.setOffset((long)sub.filePointer);
      rpart.setLeft(sub.left);
      rpart.setRight(sub.right);
      rpart.setTop(sub.top);
      rpart.setBasemap(base);
      rpart.setBottom(sub.bottom);
      rpart.setShifToData(sub.shiftToData);
      routing.addSubregions(rpart);
   }

   public BinaryMapIndexReader getReader(File f, boolean useStoredIndex) throws IOException {
      OsmandIndex.FileIndex found = useStoredIndex ? this.getFileIndex(f, false) : null;
      BinaryMapIndexReader reader = null;
      RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
      if (found == null) {
         long val = System.currentTimeMillis();
         reader = new BinaryMapIndexReader(mf, f);
         found = this.addToCache(reader, f);
         if (this.log.isDebugEnabled()) {
            this.log.debug("Initializing db " + f.getAbsolutePath() + " " + (System.currentTimeMillis() - val) + "ms");
         }
      } else {
         reader = this.initReaderFromFileIndex(found, mf, f);
      }

      return reader;
   }

   public OsmandIndex.FileIndex getFileIndex(File f, boolean init) throws IOException {
      OsmandIndex.FileIndex found = null;
      if (this.storedIndex != null) {
         for(int i = 0; i < this.storedIndex.getFileIndexCount(); ++i) {
            OsmandIndex.FileIndex fi = this.storedIndex.getFileIndex(i);
            if (f.length() == fi.getSize() && f.getName().equals(fi.getFileName())) {
               found = fi;
               break;
            }
         }
      }

      if (found == null && init) {
         RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
         long val = System.currentTimeMillis();
         BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, f);
         found = this.addToCache(reader, f);
         if (this.log.isDebugEnabled()) {
            this.log.debug("Initializing db " + f.getAbsolutePath() + " " + (System.currentTimeMillis() - val) + "ms");
         }

         reader.close();
         mf.close();
      }

      return found;
   }

   public BinaryMapIndexReader initReaderFromFileIndex(OsmandIndex.FileIndex found, RandomAccessFile mf, File f) throws IOException {
      BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, f, false);
      reader.version = found.getVersion();
      reader.dateCreated = found.getDateModified();

      for(OsmandIndex.MapPart index : found.getMapIndexList()) {
         BinaryMapIndexReader.MapIndex mi = new BinaryMapIndexReader.MapIndex();
         mi.length = (int)index.getSize();
         mi.filePointer = (int)index.getOffset();
         mi.name = index.getName();

         for(OsmandIndex.MapLevel mr : index.getLevelsList()) {
            BinaryMapIndexReader.MapRoot root = new BinaryMapIndexReader.MapRoot();
            root.length = (int)mr.getSize();
            root.filePointer = (int)mr.getOffset();
            root.left = mr.getLeft();
            root.right = mr.getRight();
            root.top = mr.getTop();
            root.bottom = mr.getBottom();
            root.minZoom = mr.getMinzoom();
            root.maxZoom = mr.getMaxzoom();
            mi.roots.add(root);
         }

         reader.mapIndexes.add(mi);
         reader.indexes.add(mi);
         reader.basemap = reader.basemap || mi.isBaseMap();
      }

      for(OsmandIndex.AddressPart index : found.getAddressIndexList()) {
         BinaryMapAddressReaderAdapter.AddressRegion mi = new BinaryMapAddressReaderAdapter.AddressRegion();
         mi.length = (int)index.getSize();
         mi.filePointer = (int)index.getOffset();
         mi.name = index.getName();
         mi.enName = index.getNameEn();
         mi.indexNameOffset = index.getIndexNameOffset();

         for(OsmandIndex.CityBlock mr : index.getCitiesList()) {
            BinaryMapAddressReaderAdapter.CitiesBlock cblock = new BinaryMapAddressReaderAdapter.CitiesBlock();
            cblock.length = (int)mr.getSize();
            cblock.filePointer = (int)mr.getOffset();
            cblock.type = mr.getType();
            mi.cities.add(cblock);
         }

         mi.attributeTagsTable.addAll(index.getAdditionalTagsList());
         reader.addressIndexes.add(mi);
         reader.indexes.add(mi);
      }

      for(OsmandIndex.PoiPart index : found.getPoiIndexList()) {
         BinaryMapPoiReaderAdapter.PoiRegion mi = new BinaryMapPoiReaderAdapter.PoiRegion();
         mi.length = (int)index.getSize();
         mi.filePointer = (int)index.getOffset();
         mi.name = index.getName();
         mi.left31 = index.getLeft();
         mi.right31 = index.getRight();
         mi.top31 = index.getTop();
         mi.bottom31 = index.getBottom();
         reader.poiIndexes.add(mi);
         reader.indexes.add(mi);
      }

      for(OsmandIndex.TransportPart index : found.getTransportIndexList()) {
         BinaryMapTransportReaderAdapter.TransportIndex mi = new BinaryMapTransportReaderAdapter.TransportIndex();
         mi.length = (int)index.getSize();
         mi.filePointer = (int)index.getOffset();
         mi.name = index.getName();
         mi.left = index.getLeft();
         mi.right = index.getRight();
         mi.top = index.getTop();
         mi.bottom = index.getBottom();
         mi.stopsFileLength = index.getStopsTableLength();
         mi.stopsFileOffset = index.getStopsTableOffset();
         mi.incompleteRoutesLength = index.getIncompleteRoutesLength();
         mi.incompleteRoutesOffset = index.getIncompleteRoutesOffset();
         mi.stringTable = new BinaryMapTransportReaderAdapter.IndexStringTable();
         mi.stringTable.fileOffset = index.getStringTableOffset();
         mi.stringTable.length = index.getStringTableLength();
         reader.transportIndexes.add(mi);
         reader.indexes.add(mi);
      }

      for(OsmandIndex.RoutingPart index : found.getRoutingIndexList()) {
         BinaryMapRouteReaderAdapter.RouteRegion mi = new BinaryMapRouteReaderAdapter.RouteRegion();
         mi.length = (int)index.getSize();
         mi.filePointer = (int)index.getOffset();
         mi.name = index.getName();

         for(OsmandIndex.RoutingSubregion mr : index.getSubregionsList()) {
            BinaryMapRouteReaderAdapter.RouteSubregion sub = new BinaryMapRouteReaderAdapter.RouteSubregion(mi);
            sub.length = (int)mr.getSize();
            sub.filePointer = (int)mr.getOffset();
            sub.left = mr.getLeft();
            sub.right = mr.getRight();
            sub.top = mr.getTop();
            sub.bottom = mr.getBottom();
            sub.shiftToData = mr.getShifToData();
            if (mr.getBasemap()) {
               mi.basesubregions.add(sub);
            } else {
               mi.subregions.add(sub);
            }
         }

         reader.routingIndexes.add(mi);
         reader.indexes.add(mi);
      }

      return reader;
   }

   public void readFromFile(File f, int version) throws IOException {
      long time = System.currentTimeMillis();
      FileInputStream is = new FileInputStream(f);

      try {
         this.storedIndex = OsmandIndex.OsmAndStoredIndex.newBuilder().mergeFrom(is).build();
         this.hasChanged = false;
         if (this.storedIndex.getVersion() != version) {
            this.storedIndex = null;
         }
      } finally {
         is.close();
      }

      this.log.info("Initialize cache " + (System.currentTimeMillis() - time));
   }

   public void writeToFile(File f) throws IOException {
      if (this.hasChanged) {
         FileOutputStream outputStream = new FileOutputStream(f);

         try {
            this.storedIndexBuilder.build().writeTo(outputStream);
         } finally {
            outputStream.close();
         }
      }
   }
}
