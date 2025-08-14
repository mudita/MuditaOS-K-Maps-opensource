package net.osmand.data;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.osmand.util.MapUtils;

public class DataTileManager<T> {
   private final int zoom;
   private final TLongObjectHashMap<List<T>> objects = new TLongObjectHashMap();

   public DataTileManager() {
      this.zoom = 15;
   }

   public DataTileManager(int z) {
      this.zoom = z;
   }

   public int getZoom() {
      return this.zoom;
   }

   public boolean isEmpty() {
      return this.getObjectsCount() == 0;
   }

   public int getObjectsCount() {
      int x = 0;

      for(List<T> s : this.objects.valueCollection()) {
         x += s.size();
      }

      return x;
   }

   private void putObjects(int tx, int ty, List<T> r) {
      if (this.objects.containsKey(this.evTile(tx, ty))) {
         r.addAll((Collection<? extends T>)this.objects.get(this.evTile(tx, ty)));
      }
   }

   public List<T> getAllObjects() {
      List<T> l = new ArrayList<>();

      for(List<T> s : this.objects.valueCollection()) {
         l.addAll(s);
      }

      return l;
   }

   public List<List<T>> getAllEditObjects() {
      return new ArrayList<>(this.objects.valueCollection());
   }

   public List<T> getObjects(double latitudeUp, double longitudeUp, double latitudeDown, double longitudeDown) {
      int tileXUp = (int)MapUtils.getTileNumberX((float)this.zoom, longitudeUp);
      int tileYUp = (int)MapUtils.getTileNumberY((float)this.zoom, latitudeUp);
      int tileXDown = (int)MapUtils.getTileNumberX((float)this.zoom, longitudeDown) + 1;
      int tileYDown = (int)MapUtils.getTileNumberY((float)this.zoom, latitudeDown) + 1;
      List<T> result = new ArrayList<>();
      if (tileXUp > tileXDown) {
         tileXDown = tileXUp;
         tileXUp = 0;
      }

      if (tileYUp > tileYDown) {
         tileYDown = tileYUp;
         tileXUp = 0;
      }

      for(int i = tileXUp; i <= tileXDown; ++i) {
         for(int j = tileYUp; j <= tileYDown; ++j) {
            this.putObjects(i, j, result);
         }
      }

      return result;
   }

   public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31) {
      List<T> result = new ArrayList<>();
      return this.getObjects(leftX31, topY31, rightX31, bottomY31, result);
   }

   public List<T> getObjects(int leftX31, int topY31, int rightX31, int bottomY31, List<T> result) {
      int tileXUp = leftX31 >> 31 - this.zoom;
      int tileYUp = topY31 >> 31 - this.zoom;
      int tileXDown = (rightX31 >> 31 - this.zoom) + 1;
      int tileYDown = (bottomY31 >> 31 - this.zoom) + 1;

      for(int i = tileXUp; i <= tileXDown; ++i) {
         for(int j = tileYUp; j <= tileYDown; ++j) {
            this.putObjects(i, j, result);
         }
      }

      return result;
   }

   public List<T> getClosestObjects(double latitude, double longitude, int defaultStep) {
      if (this.isEmpty()) {
         return Collections.emptyList();
      } else {
         int dp = 0;

         List<T> l;
         for(l = null; l == null || l.isEmpty(); dp += defaultStep) {
            l = this.getClosestObjects(latitude, longitude, dp, dp + defaultStep);
         }

         return l;
      }
   }

   public List<T> getClosestObjects(double latitude, double longitude) {
      return this.getClosestObjects(latitude, longitude, 3);
   }

   public List<T> getClosestObjects(double latitude, double longitude, int startDepth, int depth) {
      int tileX = (int)MapUtils.getTileNumberX((float)this.zoom, longitude);
      int tileY = (int)MapUtils.getTileNumberY((float)this.zoom, latitude);
      List<T> result = new ArrayList<>();
      if (startDepth <= 0) {
         this.putObjects(tileX, tileY, result);
         startDepth = 1;
      }

      for(int i = startDepth; i <= depth; ++i) {
         for(int j = 0; j <= i; ++j) {
            for(int dx = j == 0 ? 0 : -1; dx < 1 || j < i && dx == 1; dx += 2) {
               this.putObjects(tileX + dx * j, tileY + i, result);
               this.putObjects(tileX + i, tileY - dx * j, result);
               this.putObjects(tileX - dx * j, tileY - i, result);
               this.putObjects(tileX - i, tileY + dx * j, result);
            }
         }
      }

      return result;
   }

   private long evTile(int tileX, int tileY) {
      return ((long)tileX << this.zoom) + (long)tileY;
   }

   public long evaluateTile(double latitude, double longitude) {
      int tileX = (int)MapUtils.getTileNumberX((float)this.zoom, longitude);
      int tileY = (int)MapUtils.getTileNumberY((float)this.zoom, latitude);
      return this.evTile(tileX, tileY);
   }

   public long evaluateTileXY(int x31, int y31) {
      return this.evTile(x31 >> 31 - this.zoom, y31 >> 31 - this.zoom);
   }

   public void unregisterObject(double latitude, double longitude, T object) {
      long tile = this.evaluateTile(latitude, longitude);
      this.removeObject(object, tile);
   }

   public void unregisterObjectXY(int x31, int y31, T object) {
      long tile = this.evaluateTileXY(x31, y31);
      this.removeObject(object, tile);
   }

   private void removeObject(T object, long tile) {
      if (this.objects.containsKey(tile)) {
         ((List)this.objects.get(tile)).remove(object);
      }
   }

   public long registerObjectXY(int x31, int y31, T object) {
      return this.addObject(object, this.evTile(x31 >> 31 - this.zoom, y31 >> 31 - this.zoom));
   }

   public long registerObject(double latitude, double longitude, T object) {
      long tile = this.evaluateTile(latitude, longitude);
      return this.addObject(object, tile);
   }

   private long addObject(T object, long tile) {
      if (!this.objects.containsKey(tile)) {
         this.objects.put(tile, new ArrayList());
      }

      ((List)this.objects.get(tile)).add(object);
      return tile;
   }

   public void clear() {
      this.objects.clear();
   }
}
