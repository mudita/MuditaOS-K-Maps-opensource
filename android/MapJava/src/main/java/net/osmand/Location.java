package net.osmand;

public class Location {
   private String mProvider;
   private long mTime = 0L;
   private double mLatitude = 0.0;
   private double mLongitude = 0.0;
   private boolean mHasAltitude = false;
   private double mAltitude = 0.0;
   private boolean mHasSpeed = false;
   private float mSpeed = 0.0F;
   private boolean mHasBearing = false;
   private float mBearing = 0.0F;
   private boolean mHasAccuracy = false;
   private float mAccuracy = 0.0F;
   private boolean mHasVerticalAccuracy = false;
   private float mVerticalAccuracy = 0.0F;
   private double mLat1 = 0.0;
   private double mLon1 = 0.0;
   private double mLat2 = 0.0;
   private double mLon2 = 0.0;
   private float mDistance = 0.0F;
   private float mInitialBearing = 0.0F;
   private float[] mResults = new float[2];

   public Location(String provider) {
      this.mProvider = provider;
   }

   public Location(String provider, double lat, double lon) {
      this.mProvider = provider;
      this.setLatitude(lat);
      this.setLongitude(lon);
   }

   public Location(Location l) {
      this.set(l);
   }

   public void set(Location l) {
      this.mProvider = l.mProvider;
      this.mTime = l.mTime;
      this.mLatitude = l.mLatitude;
      this.mLongitude = l.mLongitude;
      this.mHasAltitude = l.mHasAltitude;
      this.mAltitude = l.mAltitude;
      this.mHasSpeed = l.mHasSpeed;
      this.mSpeed = l.mSpeed;
      this.mHasBearing = l.mHasBearing;
      this.mBearing = l.mBearing;
      this.mHasAccuracy = l.mHasAccuracy;
      this.mAccuracy = l.mAccuracy;
      this.mHasVerticalAccuracy = l.mHasVerticalAccuracy;
      this.mVerticalAccuracy = l.mVerticalAccuracy;
   }

   public void reset() {
      this.mProvider = null;
      this.mTime = 0L;
      this.mLatitude = 0.0;
      this.mLongitude = 0.0;
      this.mHasAltitude = false;
      this.mAltitude = 0.0;
      this.mHasSpeed = false;
      this.mSpeed = 0.0F;
      this.mHasBearing = false;
      this.mBearing = 0.0F;
      this.mHasAccuracy = false;
      this.mAccuracy = 0.0F;
   }

   private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, float[] results) {
      int MAXITERS = 20;
      lat1 *= Math.PI / 180.0;
      lat2 *= Math.PI / 180.0;
      lon1 *= Math.PI / 180.0;
      lon2 *= Math.PI / 180.0;
      double a = 6378137.0;
      double b = 6356752.3142;
      double f = (a - b) / a;
      double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);
      double L = lon2 - lon1;
      double A = 0.0;
      double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
      double U2 = Math.atan((1.0 - f) * Math.tan(lat2));
      double cosU1 = Math.cos(U1);
      double cosU2 = Math.cos(U2);
      double sinU1 = Math.sin(U1);
      double sinU2 = Math.sin(U2);
      double cosU1cosU2 = cosU1 * cosU2;
      double sinU1sinU2 = sinU1 * sinU2;
      double sigma = 0.0;
      double deltaSigma = 0.0;
      double cosSqAlpha = 0.0;
      double cos2SM = 0.0;
      double cosSigma = 0.0;
      double sinSigma = 0.0;
      double cosLambda = 0.0;
      double sinLambda = 0.0;
      double lambda = L;

      for(int iter = 0; iter < MAXITERS; ++iter) {
         double lambdaOrig = lambda;
         cosLambda = Math.cos(lambda);
         sinLambda = Math.sin(lambda);
         double t1 = cosU2 * sinLambda;
         double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
         double sinSqSigma = t1 * t1 + t2 * t2;
         sinSigma = Math.sqrt(sinSqSigma);
         cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda;
         sigma = Math.atan2(sinSigma, cosSigma);
         double sinAlpha = sinSigma == 0.0 ? 0.0 : cosU1cosU2 * sinLambda / sinSigma;
         cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
         cos2SM = cosSqAlpha == 0.0 ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha;
         double uSquared = cosSqAlpha * aSqMinusBSqOverBSq;
         A = 1.0 + uSquared / 16384.0 * (4096.0 + uSquared * (-768.0 + uSquared * (320.0 - 175.0 * uSquared)));
         double B = uSquared / 1024.0 * (256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
         double C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha));
         double cos2SMSq = cos2SM * cos2SM;
         deltaSigma = B
            * sinSigma
            * (cos2SM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SMSq) - B / 6.0 * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)));
         lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM)));
         double delta = (lambda - lambdaOrig) / lambda;
         if (Math.abs(delta) < 1.0E-12) {
            break;
         }
      }

      float distance = (float)(b * A * (sigma - deltaSigma));
      results[0] = distance;
      if (results.length > 1) {
         float initialBearing = (float)Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
         initialBearing = (float)((double)initialBearing * (180.0 / Math.PI));
         results[1] = initialBearing;
         if (results.length > 2) {
            float finalBearing = (float)Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
            finalBearing = (float)((double)finalBearing * (180.0 / Math.PI));
            results[2] = finalBearing;
         }
      }
   }

   public static void distanceBetween(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float[] results) {
      if (results != null && results.length >= 1) {
         computeDistanceAndBearing(startLatitude, startLongitude, endLatitude, endLongitude, results);
      } else {
         throw new IllegalArgumentException("results is null or has length < 1");
      }
   }

   public float distanceTo(Location dest) {
      synchronized(this.mResults) {
         if (this.mLatitude != this.mLat1 || this.mLongitude != this.mLon1 || dest.mLatitude != this.mLat2 || dest.mLongitude != this.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, this.mResults);
            this.mLat1 = this.mLatitude;
            this.mLon1 = this.mLongitude;
            this.mLat2 = dest.mLatitude;
            this.mLon2 = dest.mLongitude;
            this.mDistance = this.mResults[0];
            this.mInitialBearing = this.mResults[1];
         }

         return this.mDistance;
      }
   }

   public float bearingTo(Location dest) {
      synchronized(this.mResults) {
         if (this.mLatitude != this.mLat1 || this.mLongitude != this.mLon1 || dest.mLatitude != this.mLat2 || dest.mLongitude != this.mLon2) {
            computeDistanceAndBearing(this.mLatitude, this.mLongitude, dest.mLatitude, dest.mLongitude, this.mResults);
            this.mLat1 = this.mLatitude;
            this.mLon1 = this.mLongitude;
            this.mLat2 = dest.mLatitude;
            this.mLon2 = dest.mLongitude;
            this.mDistance = this.mResults[0];
            this.mInitialBearing = this.mResults[1];
         }

         return this.mInitialBearing;
      }
   }

   public String getProvider() {
      return this.mProvider;
   }

   public void setProvider(String provider) {
      this.mProvider = provider;
   }

   public long getTime() {
      return this.mTime;
   }

   public void setTime(long time) {
      this.mTime = time;
   }

   public double getLatitude() {
      return this.mLatitude;
   }

   public void setLatitude(double latitude) {
      this.mLatitude = latitude;
   }

   public double getLongitude() {
      return this.mLongitude;
   }

   public void setLongitude(double longitude) {
      this.mLongitude = longitude;
   }

   public boolean hasAltitude() {
      return this.mHasAltitude;
   }

   public double getAltitude() {
      return this.mAltitude;
   }

   public void setAltitude(double altitude) {
      this.mAltitude = altitude;
      this.mHasAltitude = true;
   }

   public void removeAltitude() {
      this.mAltitude = 0.0;
      this.mHasAltitude = false;
   }

   public boolean hasSpeed() {
      return this.mHasSpeed;
   }

   public float getSpeed() {
      return this.mSpeed;
   }

   public void setSpeed(float speed) {
      this.mSpeed = speed;
      this.mHasSpeed = true;
   }

   public void removeSpeed() {
      this.mSpeed = 0.0F;
      this.mHasSpeed = false;
   }

   public boolean hasBearing() {
      return this.mHasBearing;
   }

   public float getBearing() {
      return this.mBearing;
   }

   public void setBearing(float bearing) {
      while(bearing < 0.0F) {
         bearing += 360.0F;
      }

      while(bearing >= 360.0F) {
         bearing -= 360.0F;
      }

      this.mBearing = bearing;
      this.mHasBearing = true;
   }

   public void removeBearing() {
      this.mBearing = 0.0F;
      this.mHasBearing = false;
   }

   public boolean hasAccuracy() {
      return this.mHasAccuracy;
   }

   public float getAccuracy() {
      return this.mAccuracy;
   }

   public void setAccuracy(float accuracy) {
      this.mAccuracy = accuracy;
      this.mHasAccuracy = true;
   }

   public void removeAccuracy() {
      this.mAccuracy = 0.0F;
      this.mHasAccuracy = false;
   }

   public boolean hasVerticalAccuracy() {
      return this.mHasVerticalAccuracy;
   }

   public float getVerticalAccuracy() {
      return this.mVerticalAccuracy;
   }

   public void setVerticalAccuracy(float verticalAccuracy) {
      this.mVerticalAccuracy = verticalAccuracy;
      this.mHasVerticalAccuracy = true;
   }

   public void removeVerticalAccuracy() {
      this.mVerticalAccuracy = 0.0F;
      this.mHasVerticalAccuracy = false;
   }

   public boolean isTheSameLatLon(Location other) {
      return getLatitude() == other.getLatitude() && getLongitude() == other.getLongitude();
   }

   @Override
   public String toString() {
      return "Location[mProvider="
         + this.mProvider
         + ",mTime="
         + this.mTime
         + ",mLatitude="
         + this.mLatitude
         + ",mLongitude="
         + this.mLongitude
         + ",mHasAltitude="
         + this.mHasAltitude
         + ",mAltitude="
         + this.mAltitude
         + ",mHasSpeed="
         + this.mHasSpeed
         + ",mSpeed="
         + this.mSpeed
         + ",mHasBearing="
         + this.mHasBearing
         + ",mBearing="
         + this.mBearing
         + ",mHasAccuracy="
         + this.mHasAccuracy
         + ",mAccuracy="
         + this.mAccuracy
         + ",mHasVerticalAccuracy="
         + this.mHasVerticalAccuracy
         + ",mVerticalAccuracy="
         + this.mVerticalAccuracy;
   }
}
