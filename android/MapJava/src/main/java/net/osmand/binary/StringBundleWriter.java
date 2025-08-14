package net.osmand.binary;

import java.util.Map.Entry;

public abstract class StringBundleWriter {
   private StringBundle bundle;

   public StringBundleWriter(StringBundle bundle) {
      this.bundle = bundle;
   }

   public StringBundle getBundle() {
      return this.bundle;
   }

   protected abstract void writeItem(String var1, StringBundle.Item<?> var2);

   public void writeBundle() {
      for(Entry<String, StringBundle.Item<?>> entry : this.bundle.getMap().entrySet()) {
         this.writeItem("osmand:" + (String)entry.getKey(), entry.getValue());
      }
   }
}
