package net.osmand.map;

import java.io.IOException;
import java.util.Map;

public interface ITileSource {
   int getMaximumZoomSupported();

   String getName();

   int getTileSize();

   String getUrlToLoad(int var1, int var2, int var3);

   String getUrlTemplate();

   byte[] getBytes(int var1, int var2, int var3, String var4) throws IOException;

   int getMinimumZoomSupported();

   String getTileFormat();

   int getBitDensity();

   boolean isEllipticYTile();

   boolean couldBeDownloadedFromInternet();

   long getExpirationTimeMillis();

   int getExpirationTimeMinutes();

   long getTileModifyTime(int var1, int var2, int var3, String var4);

   String getReferer();

   String getUserAgent();

   void deleteTiles(String var1);

   int getAvgSize();

   String getRule();

   String getRandoms();

   boolean isInvertedYTile();

   boolean isTimeSupported();

   boolean getInversiveZoom();

   ParameterType getParamType();

   long getParamMin();

   long getParamStep();

   long getParamMax();

   Map<String, String> getUrlParameters();

   String getUrlParameter(String var1);

   void setUrlParameter(String var1, String var2);

   void resetUrlParameter(String var1);

   void resetUrlParameters();
}
