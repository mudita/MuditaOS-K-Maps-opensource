package net.osmand.data;

import net.osmand.util.MapUtils;

public class RotatedTileBox {
   private double lat;
   private double lon;
   private float rotate;
   private float density;
   private int zoom;
   private double mapDensity = 1.0;
   private double zoomAnimation;
   private double zoomFloatPart;
   private int cx;
   private int cy;
   private int pixWidth;
   private int pixHeight;
   private float ratiocx;
   private float ratiocy;
   private double zoomFactor;
   private double rotateCos;
   private double rotateSin;
   private double oxTile;
   private double oyTile;
   private QuadRect tileBounds;
   private QuadRect latLonBounds;
   private QuadPointDouble tileLT;
   private QuadPointDouble tileRT;
   private QuadPointDouble tileRB;
   private QuadPointDouble tileLB;

   private RotatedTileBox() {
   }

   public RotatedTileBox(RotatedTileBox r) {
      this.pixWidth = r.pixWidth;
      this.pixHeight = r.pixHeight;
      this.lat = r.lat;
      this.lon = r.lon;
      this.zoom = r.zoom;
      this.mapDensity = r.mapDensity;
      this.zoomFloatPart = r.zoomFloatPart;
      this.zoomAnimation = r.zoomAnimation;
      this.rotate = r.rotate;
      this.density = r.density;
      this.cx = r.cx;
      this.cy = r.cy;
      this.ratiocx = r.ratiocx;
      this.ratiocy = r.ratiocy;
      this.copyDerivedFields(r);
   }

   private void copyDerivedFields(RotatedTileBox r) {
      this.zoomFactor = r.zoomFactor;
      this.rotateCos = r.rotateCos;
      this.rotateSin = r.rotateSin;
      this.oxTile = r.oxTile;
      this.oyTile = r.oyTile;
      if (r.tileBounds != null && r.latLonBounds != null) {
         this.tileBounds = new QuadRect(r.tileBounds);
         this.latLonBounds = new QuadRect(r.latLonBounds);
         this.tileLT = new QuadPointDouble(r.tileLT);
         this.tileRT = new QuadPointDouble(r.tileRT);
         this.tileRB = new QuadPointDouble(r.tileRB);
         this.tileLB = new QuadPointDouble(r.tileLB);
      }
   }

   public void calculateDerivedFields() {
      this.zoomFactor = Math.pow(2.0, this.zoomAnimation + this.zoomFloatPart) * 256.0 * this.mapDensity;
      double rad = Math.toRadians((double)this.rotate);
      this.rotateCos = Math.cos(rad);
      this.rotateSin = Math.sin(rad);
      this.oxTile = MapUtils.getTileNumberX((float)this.zoom, this.lon);
      this.oyTile = MapUtils.getTileNumberY((float)this.zoom, this.lat);

      while(this.rotate < 0.0F) {
         this.rotate += 360.0F;
      }

      while(this.rotate > 360.0F) {
         this.rotate -= 360.0F;
      }

      this.tileBounds = null;
   }

   public double getLatFromPixel(float x, float y) {
      return MapUtils.getLatitudeFromTile((float)this.zoom, this.getTileYFromPixel(x, y));
   }

   public double getLonFromPixel(float x, float y) {
      return MapUtils.getLongitudeFromTile((double)this.zoom, this.getTileXFromPixel(x, y));
   }

   public LatLon getLatLonFromPixel(float x, float y) {
      return new LatLon(this.getLatFromPixel(x, y), this.getLonFromPixel(x, y));
   }

   public LatLon getCenterLatLon() {
      return new LatLon(this.lat, this.lon);
   }

   public QuadPoint getCenterPixelPoint() {
      return new QuadPoint((float)this.cx, (float)this.cy);
   }

   public int getCenterPixelX() {
      return this.cx;
   }

   public int getCenterPixelY() {
      return this.cy;
   }

   public void setDensity(float density) {
      this.density = density;
   }

   public double getCenterTileX() {
      return this.oxTile;
   }

   public int getCenter31X() {
      return MapUtils.get31TileNumberX(this.lon);
   }

   public int getCenter31Y() {
      return MapUtils.get31TileNumberY(this.lat);
   }

   public double getCenterTileY() {
      return this.oyTile;
   }

   protected double getTileXFromPixel(float x, float y) {
      float dx = x - (float)this.cx;
      float dy = y - (float)this.cy;
      double dtilex;
      if (this.isMapRotateEnabled()) {
         dtilex = this.rotateCos * (double)dx + this.rotateSin * (double)dy;
      } else {
         dtilex = (double)dx;
      }

      return dtilex / this.zoomFactor + this.oxTile;
   }

   protected double getTileYFromPixel(float x, float y) {
      float dx = x - (float)this.cx;
      float dy = y - (float)this.cy;
      double dtiley;
      if (this.isMapRotateEnabled()) {
         dtiley = -this.rotateSin * (double)dx + this.rotateCos * (double)dy;
      } else {
         dtiley = (double)dy;
      }

      return dtiley / this.zoomFactor + this.oyTile;
   }

   public QuadRect getTileBounds() {
      this.checkTileRectangleCalculated();
      return this.tileBounds;
   }

   public void calculateTileRectangle() {
      double x1 = this.getTileXFromPixel(0.0F, 0.0F);
      double x2 = this.getTileXFromPixel((float)this.pixWidth, 0.0F);
      double x3 = this.getTileXFromPixel((float)this.pixWidth, (float)this.pixHeight);
      double x4 = this.getTileXFromPixel(0.0F, (float)this.pixHeight);
      double y1 = this.getTileYFromPixel(0.0F, 0.0F);
      double y2 = this.getTileYFromPixel((float)this.pixWidth, 0.0F);
      double y3 = this.getTileYFromPixel((float)this.pixWidth, (float)this.pixHeight);
      double y4 = this.getTileYFromPixel(0.0F, (float)this.pixHeight);
      this.tileLT = new QuadPointDouble(x1, y1);
      this.tileRT = new QuadPointDouble(x2, y2);
      this.tileRB = new QuadPointDouble(x3, y3);
      this.tileLB = new QuadPointDouble(x4, y4);
      double l = Math.min(Math.min(x1, x2), Math.min(x3, x4));
      double r = Math.max(Math.max(x1, x2), Math.max(x3, x4));
      double t = Math.min(Math.min(y1, y2), Math.min(y3, y4));
      double b = Math.max(Math.max(y1, y2), Math.max(y3, y4));
      QuadRect bounds = new QuadRect((double)((float)l), (double)((float)t), (double)((float)r), (double)((float)b));
      float top = (float)MapUtils.getLatitudeFromTile((float)this.zoom, this.alignTile(bounds.top));
      float left = (float)MapUtils.getLongitudeFromTile((double)this.zoom, this.alignTile(bounds.left));
      float bottom = (float)MapUtils.getLatitudeFromTile((float)this.zoom, this.alignTile(bounds.bottom));
      float right = (float)MapUtils.getLongitudeFromTile((double)this.zoom, this.alignTile(bounds.right));
      this.tileBounds = bounds;
      this.latLonBounds = new QuadRect((double)left, (double)top, (double)right, (double)bottom);
   }

   private double alignTile(double tile) {
      if (tile < 0.0) {
         return 0.0;
      } else {
         return tile >= MapUtils.getPowZoom((double)this.zoom) ? MapUtils.getPowZoom((double)this.zoom) - 1.0E-6 : tile;
      }
   }

   public double getPixDensity() {
      double dist = this.getDistance(0, this.getPixHeight() / 2, this.getPixWidth(), this.getPixHeight() / 2);
      return (double)this.getPixWidth() / dist;
   }

   public int getPixWidth() {
      return this.pixWidth;
   }

   public int getPixHeight() {
      return this.pixHeight;
   }

   public float getPixXFrom31(int x31, int y31) {
      double zm = MapUtils.getPowZoom((double)(31 - this.zoom));
      double xTile = (double)x31 / zm;
      double yTile = (double)y31 / zm;
      return this.getPixXFromTile(xTile, yTile);
   }

   public float getPixYFrom31(int x31, int y31) {
      double zm = MapUtils.getPowZoom((double)(31 - this.zoom));
      double xTile = (double)x31 / zm;
      double yTile = (double)y31 / zm;
      return this.getPixYFromTile(xTile, yTile);
   }

   public float getPixXFromLatLon(double latitude, double longitude) {
      double xTile = MapUtils.getTileNumberX((float)this.zoom, longitude);
      double yTile = MapUtils.getTileNumberY((float)this.zoom, latitude);
      return this.getPixXFromTile(xTile, yTile);
   }

   public float getPixXFromTile(double tileX, double tileY, float zoom) {
      double pw = MapUtils.getPowZoom((double)(zoom - (float)this.zoom));
      double xTile = tileX / pw;
      double yTile = tileY / pw;
      return this.getPixXFromTile(xTile, yTile);
   }

   protected float getPixXFromTile(double xTile, double yTile) {
      double dTileX = xTile - this.oxTile;
      double dTileY = yTile - this.oyTile;
      double rotX;
      if (this.isMapRotateEnabled()) {
         rotX = this.rotateCos * dTileX - this.rotateSin * dTileY;
      } else {
         rotX = dTileX;
      }

      double dx = rotX * this.zoomFactor;
      return (float)(dx + (double)this.cx);
   }

   public float getPixYFromLatLon(double latitude, double longitude) {
      double xTile = MapUtils.getTileNumberX((float)this.zoom, longitude);
      double yTile = MapUtils.getTileNumberY((float)this.zoom, latitude);
      return this.getPixYFromTile(xTile, yTile);
   }

   public float getPixYFromTile(double tileX, double tileY, float zoom) {
      double pw = MapUtils.getPowZoom((double)(zoom - (float)this.zoom));
      double xTile = tileX / pw;
      double yTile = tileY / pw;
      return this.getPixYFromTile(xTile, yTile);
   }

   protected float getPixYFromTile(double xTile, double yTile) {
      double dTileX = xTile - this.oxTile;
      double dTileY = yTile - this.oyTile;
      double rotY;
      if (this.isMapRotateEnabled()) {
         rotY = this.rotateSin * dTileX + this.rotateCos * dTileY;
      } else {
         rotY = dTileY;
      }

      double dy = rotY * this.zoomFactor;
      return (float)(dy + (double)this.cy);
   }

   public int getPixXFromLonNoRot(double longitude) {
      double dTilex = MapUtils.getTileNumberX((float)this.zoom, longitude) - this.oxTile;
      return (int)(dTilex * this.zoomFactor + (double)this.cx);
   }

   public int getPixXFromTileXNoRot(double tileX) {
      double dTilex = tileX - this.oxTile;
      return (int)(dTilex * this.zoomFactor + (double)this.cx);
   }

   public int getPixYFromLatNoRot(double latitude) {
      double dTileY = MapUtils.getTileNumberY((float)this.zoom, latitude) - this.oyTile;
      return (int)(dTileY * this.zoomFactor + (double)this.cy);
   }

   public int getPixYFromTileYNoRot(double tileY) {
      double dTileY = tileY - this.oyTile;
      return (int)(dTileY * this.zoomFactor + (double)this.cy);
   }

   private boolean isMapRotateEnabled() {
      return this.rotate != 0.0F;
   }

   public QuadRect getLatLonBounds() {
      this.checkTileRectangleCalculated();
      return this.latLonBounds;
   }

   public double getRotateCos() {
      return this.rotateCos;
   }

   public double getRotateSin() {
      return this.rotateSin;
   }

   public int getZoom() {
      return this.zoom;
   }

   public int getDefaultRadiusPoi() {
      double zoom = (double)this.getZoom();
      int radius;
      if (zoom <= 15.0) {
         radius = 10;
      } else if (zoom <= 16.0) {
         radius = 14;
      } else if (zoom <= 17.0) {
         radius = 16;
      } else {
         radius = 18;
      }

      return (int)((float)radius * this.getDensity());
   }

   public void setLatLonCenter(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
      this.calculateDerivedFields();
   }

   public void setRotate(float rotate) {
      this.rotate = rotate;
      this.calculateDerivedFields();
   }

   public void increasePixelDimensions(int dwidth, int dheight) {
      this.pixWidth += 2 * dwidth;
      this.pixHeight += 2 * dheight;
      this.cx += dwidth;
      this.cy += dheight;
      this.calculateDerivedFields();
   }

   public void setPixelDimensions(int width, int height) {
      this.setPixelDimensions(width, height, 0.5F, 0.5F);
   }

   public void setPixelDimensions(int width, int height, float ratiocx, float ratiocy) {
      this.pixHeight = height;
      this.pixWidth = width;
      this.cx = (int)((float)this.pixWidth * ratiocx);
      this.cy = (int)((float)this.pixHeight * ratiocy);
      this.ratiocx = ratiocx;
      this.ratiocy = ratiocy;
      this.calculateDerivedFields();
   }

   public boolean isZoomAnimated() {
      return this.zoomAnimation != 0.0;
   }

   public double getZoomAnimation() {
      return this.zoomAnimation;
   }

   public double getZoomFloatPart() {
      return this.zoomFloatPart;
   }

   public void setZoomAndAnimation(int zoom, double zoomAnimation, double zoomFloatPart) {
      this.zoomAnimation = zoomAnimation;
      this.zoomFloatPart = zoomFloatPart;
      this.zoom = zoom;
      this.calculateDerivedFields();
   }

   public void setZoomAndAnimation(int zoom, double zoomAnimation) {
      this.zoomAnimation = zoomAnimation;
      this.zoom = zoom;
      this.calculateDerivedFields();
   }

   public void setCenterLocation(float ratiocx, float ratiocy) {
      this.cx = (int)((float)this.pixWidth * ratiocx);
      this.cy = (int)((float)this.pixHeight * ratiocy);
      this.ratiocx = ratiocx;
      this.ratiocy = ratiocy;
      this.calculateDerivedFields();
   }

   public boolean isCenterShifted() {
      return this.ratiocx != 0.5F || this.ratiocy != 0.5F;
   }

   public LatLon getLeftTopLatLon() {
      this.checkTileRectangleCalculated();
      return new LatLon(
         MapUtils.getLatitudeFromTile((float)this.zoom, this.alignTile(this.tileLT.y)),
         MapUtils.getLongitudeFromTile((double)this.zoom, this.alignTile(this.tileLT.x))
      );
   }

   public QuadPointDouble getLeftTopTile(double zoom) {
      this.checkTileRectangleCalculated();
      return new QuadPointDouble(this.tileLT.x * MapUtils.getPowZoom(zoom - (double)this.zoom), this.tileLT.y * MapUtils.getPowZoom(zoom - (double)this.zoom));
   }

   public QuadPointDouble getRightBottomTile(float zoom) {
      this.checkTileRectangleCalculated();
      return new QuadPointDouble(
         this.tileRB.x * MapUtils.getPowZoom((double)(zoom - (float)this.zoom)), this.tileRB.y * MapUtils.getPowZoom((double)(zoom - (float)this.zoom))
      );
   }

   private void checkTileRectangleCalculated() {
      if (this.tileBounds == null) {
         this.calculateTileRectangle();
      }
   }

   public LatLon getRightBottomLatLon() {
      this.checkTileRectangleCalculated();
      return new LatLon(
         MapUtils.getLatitudeFromTile((float)this.zoom, this.alignTile(this.tileRB.y)),
         MapUtils.getLongitudeFromTile((double)this.zoom, this.alignTile(this.tileRB.x))
      );
   }

   public void setMapDensity(double mapDensity) {
      this.mapDensity = mapDensity;
      this.calculateDerivedFields();
   }

   public double getMapDensity() {
      return this.mapDensity;
   }

   public void setZoom(int zoom) {
      this.zoom = zoom;
      this.calculateDerivedFields();
   }

   public float getRotate() {
      return this.rotate;
   }

   public float getDensity() {
      return this.density;
   }

   public RotatedTileBox copy() {
      return new RotatedTileBox(this);
   }

   public boolean containsTileBox(RotatedTileBox box) {
      this.checkTileRectangleCalculated();
      box = box.copy();
      box.checkTileRectangleCalculated();
      if (!this.containsTilePoint(box.tileLB)) {
         return false;
      } else if (!this.containsTilePoint(box.tileLT)) {
         return false;
      } else if (!this.containsTilePoint(box.tileRB)) {
         return false;
      } else {
         return this.containsTilePoint(box.tileRT);
      }
   }

   public boolean containsTilePoint(QuadPoint qp) {
      double tx = (double)this.getPixXFromTile((double)qp.x, (double)qp.y);
      double ty = (double)this.getPixYFromTile((double)qp.x, (double)qp.y);
      return tx >= 0.0 && tx <= (double)this.pixWidth && ty >= 0.0 && ty <= (double)this.pixHeight;
   }

   public boolean containsTilePoint(QuadPointDouble qp) {
      double tx = (double)this.getPixXFromTile(qp.x, qp.y);
      double ty = (double)this.getPixYFromTile(qp.x, qp.y);
      return tx >= 0.0 && tx <= (double)this.pixWidth && ty >= 0.0 && ty <= (double)this.pixHeight;
   }

   public boolean containsLatLon(double lat, double lon) {
      double tx = (double)this.getPixXFromLatLon(lat, lon);
      double ty = (double)this.getPixYFromLatLon(lat, lon);
      return tx >= 0.0 && tx <= (double)this.pixWidth && ty >= 0.0 && ty <= (double)this.pixHeight;
   }

   public boolean containsLatLon(LatLon latLon) {
      double tx = (double)this.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
      double ty = (double)this.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
      return tx >= 0.0 && tx <= (double)this.pixWidth && ty >= 0.0 && ty <= (double)this.pixHeight;
   }

   public boolean containsPoint(float tx, float ty, float outMargin) {
      return tx >= -outMargin && tx <= (float)this.pixWidth + outMargin && ty >= -outMargin && ty <= (float)this.pixHeight + outMargin;
   }

   public double getDistance(int pixX, int pixY, int pixX2, int pixY2) {
      double lat1 = this.getLatFromPixel((float)pixX, (float)pixY);
      double lon1 = this.getLonFromPixel((float)pixX, (float)pixY);
      double lat2 = this.getLatFromPixel((float)pixX2, (float)pixY2);
      double lon2 = this.getLonFromPixel((float)pixX2, (float)pixY2);
      return MapUtils.getDistance(lat1, lon1, lat2, lon2);
   }

   public double getLongitude() {
      return this.lon;
   }

   public double getLatitude() {
      return this.lat;
   }

   @Override
   public String toString() {
      return "RotatedTileBox [lat="
         + this.lat
         + ", lon="
         + this.lon
         + ", rotate="
         + this.rotate
         + ", density="
         + this.density
         + ", zoom="
         + this.zoom
         + ", mapDensity="
         + this.mapDensity
         + ", zoomAnimation="
         + this.zoomAnimation
         + ", zoomFloatPart="
         + this.zoomFloatPart
         + ", cx="
         + this.cx
         + ", cy="
         + this.cy
         + ", pixWidth="
         + this.pixWidth
         + ", pixHeight="
         + this.pixHeight
         + "]";
   }

   public static class RotatedTileBoxBuilder {
      private RotatedTileBox tb;
      private boolean pixelDimensionsSet = false;
      private boolean locationSet = false;
      private boolean zoomSet = false;

      public RotatedTileBoxBuilder() {
         this.tb = new RotatedTileBox();
         this.tb.density = 1.0F;
         this.tb.rotate = 0.0F;
      }

      public RotatedTileBox.RotatedTileBoxBuilder density(float d) {
         this.tb.density = d;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setMapDensity(double mapDensity) {
         this.tb.mapDensity = mapDensity;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setZoom(int zoom) {
         this.tb.zoom = zoom;
         this.zoomSet = true;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setLocation(double lat, double lon) {
         this.tb.lat = lat;
         this.tb.lon = lon;
         this.locationSet = true;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setRotate(float degrees) {
         this.tb.rotate = degrees;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setPixelDimensions(int pixWidth, int pixHeight, float centerX, float centerY) {
         this.tb.pixWidth = pixWidth;
         this.tb.pixHeight = pixHeight;
         this.tb.cx = (int)((float)pixWidth * centerX);
         this.tb.cy = (int)((float)pixHeight * centerY);
         this.tb.ratiocx = centerX;
         this.tb.ratiocy = centerY;
         this.pixelDimensionsSet = true;
         return this;
      }

      public RotatedTileBox.RotatedTileBoxBuilder setPixelDimensions(int pixWidth, int pixHeight) {
         return this.setPixelDimensions(pixWidth, pixHeight, 0.5F, 0.5F);
      }

      public RotatedTileBox build() {
         if (!this.pixelDimensionsSet) {
            throw new IllegalArgumentException("Please specify pixel dimensions");
         } else if (!this.zoomSet) {
            throw new IllegalArgumentException("Please specify zoom");
         } else if (!this.locationSet) {
            throw new IllegalArgumentException("Please specify location");
         } else {
            RotatedTileBox local = this.tb;
            local.calculateDerivedFields();
            this.tb = null;
            return local;
         }
      }
   }
}
