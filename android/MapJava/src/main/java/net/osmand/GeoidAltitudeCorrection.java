package net.osmand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.commons.logging.Log;

public class GeoidAltitudeCorrection {
   private final Log log = PlatformUtil.getLog(GeoidAltitudeCorrection.class);
   private File f;
   private RandomAccessFile rf;
   private int cachedPointer = -1;
   private short cachedValue = 0;

   public GeoidAltitudeCorrection(File dir) {
      String[] fnames = dir.list();
      if (fnames != null) {
         String fn = null;

         for(String f : fnames) {
            if (f.contains("WW15MGH")) {
               fn = f;
               break;
            }
         }

         if (fn != null) {
            this.f = new File(dir, fn);
            if (this.f.exists()) {
               try {
                  this.rf = new RandomAccessFile(this.f.getPath(), "r");
               } catch (FileNotFoundException var8) {
                  this.log.error("Error", var8);
               }
            }
         }
      }
   }

   public boolean isGeoidInformationAvailable() {
      return this.rf != null;
   }

   public float getGeoidHeight(double lat, double lon) {
      if (!this.isGeoidInformationAvailable()) {
         return 0.0F;
      } else {
         int shy = (int)Math.floor((90.0 - lat) * 4.0);
         int shx = (int)Math.floor((lon >= 0.0 ? lon : lon + 360.0) * 4.0);
         int pointer = (shy * 1440 + shx) * 2;
         short res = 0;
         if (pointer != this.cachedPointer) {
            try {
               this.rf.seek((long)pointer);
               this.cachedValue = this.readShort();
               this.cachedPointer = pointer;
            } catch (IOException var10) {
               this.log.error("Geoid info error", var10);
            }
         }

         res = this.cachedValue;
         return (float)res / 100.0F;
      }
   }

   private short readShort() throws IOException {
      byte[] b = new byte[2];
      this.rf.read(b);
      int ch1 = b[0] < 0 ? b[0] + 256 : b[0];
      int ch2 = b[1] < 0 ? b[1] + 256 : b[1];
      return (short)((ch1 << 8) + ch2);
   }
}
