package net.osmand;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {
   private final OutputStream out;
   private final IProgress progress;
   private int bytesDivisor = 1024;
   private int counter = 0;

   public ProgressOutputStream(OutputStream out, IProgress progress) {
      this.out = out;
      this.progress = progress;
   }

   public ProgressOutputStream(OutputStream out, IProgress progress, int bytesDivisor) {
      this.out = out;
      this.progress = progress;
      this.bytesDivisor = bytesDivisor;
   }

   private void submitProgress(int length) {
      this.counter += length;
      if (this.progress != null && this.counter > this.bytesDivisor) {
         this.progress.progress(this.counter / this.bytesDivisor);
         this.counter %= this.bytesDivisor;
      }
   }

   @Override
   public void write(byte[] b, int offset, int length) throws IOException {
      this.out.write(b, offset, length);
      this.submitProgress(length);
   }

   @Override
   public void write(byte[] b) throws IOException {
      this.out.write(b);
      this.submitProgress(b.length);
   }

   @Override
   public void write(int b) throws IOException {
      this.out.write(b);
      this.submitProgress(1);
   }
}
