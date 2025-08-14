package net.osmand.osm.io;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetworkUtils {

   public static HttpURLConnection getHttpURLConnection(String urlString) throws MalformedURLException, IOException {
      return getHttpURLConnection(new URL(urlString));
   }

   public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
      return (HttpURLConnection)url.openConnection();
   }
}
