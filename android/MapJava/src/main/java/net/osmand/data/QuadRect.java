package net.osmand.data;

public class QuadRect {
   public double left;
   public double right;
   public double top;
   public double bottom;

   public QuadRect(double left, double top, double right, double bottom) {
      this.left = left;
      this.right = right;
      this.top = top;
      this.bottom = bottom;
   }

   public QuadRect(QuadRect a) {
      this(a.left, a.top, a.right, a.bottom);
   }

   public QuadRect() {
   }

   public double width() {
      return Math.abs(this.right - this.left);
   }

   public double height() {
      return Math.abs(this.bottom - this.top);
   }

   public boolean contains(double left, double top, double right, double bottom) {
      return Math.min(this.left, this.right) <= Math.min(left, right)
         && Math.max(this.left, this.right) >= Math.max(left, right)
         && Math.min(this.top, this.bottom) <= Math.min(top, bottom)
         && Math.max(this.top, this.bottom) >= Math.max(top, bottom);
   }

   public boolean contains(QuadRect box) {
      return this.contains(box.left, box.top, box.right, box.bottom);
   }

   public static boolean intersects(QuadRect a, QuadRect b) {
      return Math.min(a.left, a.right) <= Math.max(b.left, b.right)
         && Math.max(a.left, a.right) >= Math.min(b.left, b.right)
         && Math.min(a.bottom, a.top) <= Math.max(b.bottom, b.top)
         && Math.max(a.bottom, a.top) >= Math.min(b.bottom, b.top);
   }

   public static boolean trivialOverlap(QuadRect a, QuadRect b) {
      return intersects(a, b);
   }

   public double centerX() {
      return (this.left + this.right) / 2.0;
   }

   public double centerY() {
      return (this.top + this.bottom) / 2.0;
   }

   public void offset(double dx, double dy) {
      this.left += dx;
      this.top += dy;
      this.right += dx;
      this.bottom += dy;
   }

   public void inset(double dx, double dy) {
      this.left += dx;
      this.top += dy;
      this.right -= dx;
      this.bottom -= dy;
   }

   @Override
   public String toString() {
      return "[" + (float)this.left + "," + (float)this.top + " - " + (float)this.right + "," + (float)this.bottom + "]";
   }
}
