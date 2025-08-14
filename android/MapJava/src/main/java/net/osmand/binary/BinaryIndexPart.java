package net.osmand.binary;

public abstract class BinaryIndexPart {
   String name;
   int length;
   int filePointer;

   public abstract String getPartName();

   public abstract int getFieldNumber();

   public int getLength() {
      return this.length;
   }

   public void setLength(int length) {
      this.length = length;
   }

   public int getFilePointer() {
      return this.filePointer;
   }

   public void setFilePointer(int filePointer) {
      this.filePointer = filePointer;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
