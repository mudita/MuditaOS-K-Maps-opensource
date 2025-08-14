package net.osmand.binary;

public interface StringExternalizable<T extends StringBundle> {
   void writeToBundle(T var1);

   void readFromBundle(T var1);
}
