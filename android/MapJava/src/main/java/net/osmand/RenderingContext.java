package net.osmand;

import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;

public class RenderingContext {
   public int renderedState = 0;
   public boolean interrupted = false;
   public boolean nightMode = false;
   public String preferredLocale = "";
   public boolean transliterate = false;
   public int defaultColor = 15855336;
   public double leftX;
   public double topY;
   public int width;
   public int height;
   public int zoom;
   public double tileDivisor;
   public float rotate;
   public int pointCount = 0;
   public int pointInsideCount = 0;
   public int visible = 0;
   public int allObjects = 0;
   public int textRenderingTime = 0;
   public int lastRenderedKey = 0;
   public float screenDensityRatio = 1.0F;
   public float textScale = 1.0F;
   public int shadowRenderingMode = RenderingContext.ShadowRenderingMode.SOLID_SHADOW.value;
   public int shadowRenderingColor = -6908266;
   public String renderingDebugInfo;
   public double polygonMinSizeToDisplay;
   public long renderingContextHandle;
   private float density = 1.0F;

   public void setDensityValue(float density) {
      this.density = density;
   }

   public float getDensityValue(float val) {
      return val * this.density;
   }

   public float getComplexValue(RenderingRuleSearchRequest req, RenderingRuleProperty prop, int defVal) {
      return req.getFloatPropertyValue(prop, 0.0F) * this.density + (float)req.getIntPropertyValue(prop, defVal);
   }

   public float getComplexValue(RenderingRuleSearchRequest req, RenderingRuleProperty prop) {
      return req.getFloatPropertyValue(prop, 0.0F) * this.density + (float)req.getIntPropertyValue(prop, 0);
   }

   protected byte[] getIconRawData(String data) {
      return null;
   }

   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      if (this.renderingContextHandle != 0L) {
         NativeLibrary.deleteRenderingContextHandle(this.renderingContextHandle);
         this.renderingContextHandle = 0L;
      }
   }

   public static enum ShadowRenderingMode {
      NO_SHADOW(0),
      ONE_STEP(1),
      BLUR_SHADOW(2),
      SOLID_SHADOW(3);

      public final int value;

      private ShadowRenderingMode(int v) {
         this.value = v;
      }
   }
}
