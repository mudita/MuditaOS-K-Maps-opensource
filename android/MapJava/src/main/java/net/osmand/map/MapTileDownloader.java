package net.osmand.map;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.LIFOBlockingDeque;
import org.apache.commons.logging.Log;

public class MapTileDownloader {
   private static final Log log = PlatformUtil.getLog(MapTileDownloader.class);
   public static int TILE_DOWNLOAD_THREADS = 8;
   public static int TILE_DOWNLOAD_SECONDS_TO_WORK = 25;
   public static final long TIMEOUT_AFTER_EXCEEDING_LIMIT_ERRORS = 15000L;
   public static final int TILE_DOWNLOAD_MAX_ERRORS_PER_TIMEOUT = 50;
   private static final int CONNECTION_TIMEOUT = 30000;
   private static MapTileDownloader downloader = null;
   public static String USER_AGENT = "OsmAnd~";
   private final ThreadPoolExecutor threadPoolExecutor;
   private List<WeakReference<MapTileDownloader.IMapDownloaderCallback>> callbacks = new LinkedList<>();
   private final Map<File, MapTileDownloader.DownloadRequest> pendingToDownload = new ConcurrentHashMap<>();
   private final Map<File, MapTileDownloader.DownloadRequest> currentlyDownloaded = new ConcurrentHashMap<>();
   private int currentErrors = 0;
   private long timeForErrorCounter = 0L;
   private boolean noHttps;

   public static MapTileDownloader getInstance(String userAgent) {
      if (downloader == null) {
         downloader = new MapTileDownloader(TILE_DOWNLOAD_THREADS);
         if (userAgent != null) {
            USER_AGENT = userAgent;
         }
      }

      return downloader;
   }

   public MapTileDownloader(int numberOfThreads) {
      this.threadPoolExecutor = new ThreadPoolExecutor(
         numberOfThreads, numberOfThreads, (long)TILE_DOWNLOAD_SECONDS_TO_WORK, TimeUnit.SECONDS, new LIFOBlockingDeque()
      );
   }

   public void setNoHttps(boolean noHttps) {
      this.noHttps = noHttps;
   }

   public void addDownloaderCallback(MapTileDownloader.IMapDownloaderCallback callback) {
      LinkedList<WeakReference<MapTileDownloader.IMapDownloaderCallback>> ncall = new LinkedList<>(this.callbacks);
      ncall.add(new WeakReference<>(callback));
      this.callbacks = ncall;
   }

   public void removeDownloaderCallback(MapTileDownloader.IMapDownloaderCallback callback) {
      LinkedList<WeakReference<MapTileDownloader.IMapDownloaderCallback>> ncall = new LinkedList<>(this.callbacks);
      Iterator<WeakReference<MapTileDownloader.IMapDownloaderCallback>> it = ncall.iterator();

      while(it.hasNext()) {
         MapTileDownloader.IMapDownloaderCallback c = it.next().get();
         if (c == callback) {
            it.remove();
         }
      }

      this.callbacks = ncall;
   }

   public void clearCallbacks() {
      this.callbacks = new LinkedList<>();
   }

   public List<MapTileDownloader.IMapDownloaderCallback> getDownloaderCallbacks() {
      ArrayList<MapTileDownloader.IMapDownloaderCallback> lst = new ArrayList<>();

      for(WeakReference<MapTileDownloader.IMapDownloaderCallback> c : this.callbacks) {
         MapTileDownloader.IMapDownloaderCallback ct = c.get();
         if (ct != null) {
            lst.add(ct);
         }
      }

      return lst;
   }

   public boolean isFilePendingToDownload(File f) {
      return f != null && this.pendingToDownload.containsKey(f);
   }

   public boolean isFileCurrentlyDownloaded(File f) {
      return f != null && this.currentlyDownloaded.containsKey(f);
   }

   public boolean isSomethingBeingDownloaded() {
      return !this.currentlyDownloaded.isEmpty();
   }

   public int getRemainingWorkers() {
      return (int)this.threadPoolExecutor.getTaskCount();
   }

   public void refuseAllPreviousRequests() {
      while(!this.threadPoolExecutor.getQueue().isEmpty()) {
         this.threadPoolExecutor.getQueue().poll();
      }

      this.pendingToDownload.clear();
   }

   public void requestToDownload(MapTileDownloader.DownloadRequest request) {
      long now = System.currentTimeMillis();
      if ((long)((int)(now - this.timeForErrorCounter)) > 15000L) {
         this.timeForErrorCounter = now;
         this.currentErrors = 0;
      } else if (this.shouldSkipRequests()) {
         return;
      }

      if (request.url != null) {
         if (this.noHttps) {
            request.url = request.url.replace("https://", "http://");
         }

         if (!this.isFileCurrentlyDownloaded(request.fileToSave) && !this.isFilePendingToDownload(request.fileToSave)) {
            this.pendingToDownload.put(request.fileToSave, request);
            this.threadPoolExecutor.execute(new MapTileDownloader.DownloadMapWorker(request));
         }
      }
   }

   public boolean shouldSkipRequests() {
      return this.currentErrors > 50;
   }

   public void fireLoadCallback(MapTileDownloader.DownloadRequest request) {
      for(WeakReference<MapTileDownloader.IMapDownloaderCallback> callback : this.callbacks) {
         MapTileDownloader.IMapDownloaderCallback c = callback.get();
         if (c != null) {
            c.tileDownloaded(request);
         }
      }
   }

   private class DownloadMapWorker implements Runnable, Comparable<MapTileDownloader.DownloadMapWorker> {
      private final MapTileDownloader.DownloadRequest request;

      private DownloadMapWorker(MapTileDownloader.DownloadRequest request) {
         this.request = request;
      }

      @Override
      public void run() {
         if (this.request != null && this.request.fileToSave != null && this.request.url != null) {
            MapTileDownloader.this.pendingToDownload.remove(this.request.fileToSave);
            if (MapTileDownloader.this.currentlyDownloaded.containsKey(this.request.fileToSave)) {
               return;
            }

            MapTileDownloader.this.currentlyDownloaded.put(this.request.fileToSave, this.request);
            if (MapTileDownloader.log.isDebugEnabled()) {
               MapTileDownloader.log.debug("Start downloading tile : " + this.request.url);
            }

            long time = System.currentTimeMillis();
            this.request.setError(false);
            HttpURLConnection connection = null;

            try {
               connection = NetworkUtils.getHttpURLConnection(this.request.url);
               connection.setRequestProperty("User-Agent", Algorithms.isEmpty(this.request.userAgent) ? MapTileDownloader.USER_AGENT : this.request.userAgent);
               if (this.request.referer != null) {
                  connection.setRequestProperty("Referer", this.request.referer);
               }

               connection.setConnectTimeout(30000);
               connection.setReadTimeout(30000);
               BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream(), 8192);
               this.request.saveTile(inputStream);
               if (MapTileDownloader.log.isDebugEnabled()) {
                  MapTileDownloader.log.debug("Downloading tile : " + this.request.url + " successfull " + (System.currentTimeMillis() - time) + " ms");
               }
            } catch (UnknownHostException var9) {
               MapTileDownloader.this.currentErrors++;
               MapTileDownloader.this.timeForErrorCounter = System.currentTimeMillis();
               this.request.setError(true);
               MapTileDownloader.log.error("UnknownHostException, cannot download tile " + this.request.url + " " + var9.getMessage());
            } catch (Exception var10) {
               MapTileDownloader.this.currentErrors++;
               MapTileDownloader.this.timeForErrorCounter = System.currentTimeMillis();
               this.request.setError(true);
               MapTileDownloader.log.warn("Cannot download tile : " + this.request.url, var10);
            } finally {
               MapTileDownloader.this.currentlyDownloaded.remove(this.request.fileToSave);
               if (connection != null) {
                  connection.disconnect();
               }
            }

            if (!this.request.error) {
               MapTileDownloader.this.fireLoadCallback(this.request);
            }
         }
      }

      public int compareTo(MapTileDownloader.DownloadMapWorker o) {
         return 0;
      }
   }

   public static class DownloadRequest {
      public final File fileToSave;
      public final String tileId;
      public final int zoom;
      public final int xTile;
      public final int yTile;
      public String url;
      public String referer = null;
      public String userAgent = null;
      public boolean error;

      public DownloadRequest(String url, File fileToSave, String tileId, int xTile, int yTile, int zoom) {
         this.url = url;
         this.fileToSave = fileToSave;
         this.tileId = tileId;
         this.xTile = xTile;
         this.yTile = yTile;
         this.zoom = zoom;
      }

      public void setError(boolean error) {
         this.error = error;
      }

      public void saveTile(InputStream inputStream) throws IOException {
         this.fileToSave.getParentFile().mkdirs();
         OutputStream stream = null;

         try {
            stream = new FileOutputStream(this.fileToSave);
            Algorithms.streamCopy(inputStream, stream);
            stream.flush();
         } finally {
            Algorithms.closeStream(inputStream);
            Algorithms.closeStream(stream);
         }
      }
   }

   public interface IMapDownloaderCallback {
      void tileDownloaded(MapTileDownloader.DownloadRequest var1);
   }
}
