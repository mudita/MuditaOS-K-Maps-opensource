package net.osmand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;

public class LocationsHolder {
   private static final int LOCATION_TYPE_UNKNOWN = -1;
   private static final int LOCATION_TYPE_LATLON = 0;
   private static final int LOCATION_TYPE_LOCATION = 1;
   private static final int LOCATION_TYPE_WPTPT = 2;
   private List<LatLon> latLonList;
   private List<Location> locationList;
   private List<GPXUtilities.WptPt> wptPtList;
   private int locationType;
   private int size;

   public LocationsHolder(List<?> locations) {
      this.locationType = this.resolveLocationType(locations);
      switch(this.locationType) {
         case 0:
            this.latLonList = new ArrayList<>((Collection<? extends LatLon>) locations);
            this.size = locations.size();
            break;
         case 1:
            this.locationList = new ArrayList<>((Collection<? extends Location>)locations);
            this.size = locations.size();
            break;
         case 2:
            this.wptPtList = new ArrayList<>((Collection<? extends GPXUtilities.WptPt>)locations);
            this.size = locations.size();
      }
   }

   private int resolveLocationType(List<?> locations) {
      if (!Algorithms.isEmpty(locations)) {
         Object locationObj = locations.get(0);
         if (locationObj instanceof LatLon) {
            return 0;
         } else if (locationObj instanceof GPXUtilities.WptPt) {
            return 2;
         } else if (locationObj instanceof Location) {
            return 1;
         } else {
            throw new IllegalArgumentException("Unsupported location type: " + locationObj.getClass().getSimpleName());
         }
      } else {
         return -1;
      }
   }

   public double getLatitude(int index) {
      switch(this.locationType) {
         case 0:
            return this.latLonList.get(index).getLatitude();
         case 1:
            return this.locationList.get(index).getLatitude();
         case 2:
            return this.wptPtList.get(index).getLatitude();
         default:
            return 0.0;
      }
   }

   public double getLongitude(int index) {
      switch(this.locationType) {
         case 0:
            return this.latLonList.get(index).getLongitude();
         case 1:
            return this.locationList.get(index).getLongitude();
         case 2:
            return this.wptPtList.get(index).getLongitude();
         default:
            return 0.0;
      }
   }

   public int getSize() {
      return this.size;
   }

   private <T> List<T> getList(int locationType) {
      List<T> res = new ArrayList<>();
      if (this.size > 0) {
         for(int i = 0; i < this.size; ++i) {
            switch(locationType) {
               case 0:
                  res.add((T)this.getLatLon(i));
                  break;
               case 1:
                  res.add((T)this.getLocation(i));
                  break;
               case 2:
                  res.add((T)this.getWptPt(i));
            }
         }
      }

      return res;
   }

   public List<LatLon> getLatLonList() {
      if (this.locationType == LOCATION_TYPE_LATLON) {
         return latLonList;
      } else {
         return getList(LOCATION_TYPE_LATLON);
      }
   }

   public List<GPXUtilities.WptPt> getWptPtList() {
      if (this.locationType == LOCATION_TYPE_WPTPT) {
         return wptPtList;
      } else {
         return getList(LOCATION_TYPE_WPTPT);
      }
   }

   public List<Location> getLocationsList() {
      if (this.locationType == LOCATION_TYPE_LOCATION) {
         return locationList;
      } else {
         return getList(LOCATION_TYPE_LOCATION);
      }
   }

   public LatLon getLatLon(int index) {
      return this.locationType == 0 ? this.latLonList.get(index) : new LatLon(this.getLatitude(index), this.getLongitude(index));
   }

   public GPXUtilities.WptPt getWptPt(int index) {
      if (this.locationType == 2) {
         return this.wptPtList.get(index);
      } else {
         GPXUtilities.WptPt wptPt = new GPXUtilities.WptPt();
         wptPt.lat = this.getLatitude(index);
         wptPt.lon = this.getLongitude(index);
         return wptPt;
      }
   }

   public Location getLocation(int index) {
      return this.locationType == 1 ? this.locationList.get(index) : new Location("", this.getLatitude(index), this.getLongitude(index));
   }
}
