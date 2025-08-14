package net.osmand.search.core;

public class SearchExportSettings {
   private boolean exportEmptyCities;
   private boolean exportBuildings;
   private double maxDistance;

   public SearchExportSettings() {
      this.exportEmptyCities = true;
      this.exportBuildings = true;
      this.maxDistance = -1.0;
   }

   public SearchExportSettings(boolean exportEmptyCities, boolean exportBuildings, double maxDistance) {
      this.exportEmptyCities = exportEmptyCities;
      this.exportBuildings = exportBuildings;
      this.maxDistance = maxDistance;
   }

   public boolean isExportEmptyCities() {
      return this.exportEmptyCities;
   }

   public boolean isExportBuildings() {
      return this.exportBuildings;
   }

   public double getMaxDistance() {
      return this.maxDistance;
   }
}
