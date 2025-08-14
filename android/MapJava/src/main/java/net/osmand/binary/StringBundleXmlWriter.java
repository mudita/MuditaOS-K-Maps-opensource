package net.osmand.binary;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlSerializer;

public class StringBundleXmlWriter extends StringBundleWriter {
   public static final Log log = PlatformUtil.getLog(StringBundleXmlWriter.class);
   private XmlSerializer serializer;

   public StringBundleXmlWriter(StringBundle bundle, XmlSerializer serializer) {
      super(bundle);
      this.serializer = serializer;
   }

   @Override
   protected void writeItem(String name, StringBundle.Item<?> item) {
      if (this.serializer != null) {
         try {
            this.writeItemImpl(name, item);
         } catch (Exception var4) {
            log.error("Error writing string bundle as xml", var4);
         }
      }
   }

   @Override
   public void writeBundle() {
      if (this.serializer != null) {
         super.writeBundle();

         try {
            this.serializer.flush();
         } catch (Exception var2) {
            log.error("Error writing string bundle as xml", var2);
         }
      }
   }

   private void writeItemImpl(String name, StringBundle.Item<?> item) throws IOException {
      if (this.serializer != null && item != null) {
         switch(item.getType()) {
            case STRING:
               StringBundle.StringItem stringItem = (StringBundle.StringItem)item;
               this.serializer.attribute(null, name, stringItem.getValue());
               break;
            case LIST:
               StringBundle.StringListItem listItem = (StringBundle.StringListItem)item;
               this.serializer.startTag(null, name);
               List<StringBundle.Item<?>> list = listItem.getValue();

               for(StringBundle.Item<?> i : list) {
                  if (i.getType() == StringBundle.ItemType.STRING) {
                     this.writeItemImpl(i.getName(), i);
                  }
               }

               for(StringBundle.Item<?> i : list) {
                  if (i.getType() != StringBundle.ItemType.STRING) {
                     this.writeItemImpl(i.getName(), i);
                  }
               }

               this.serializer.endTag(null, name);
               break;
            case MAP:
               StringBundle.StringMapItem mapItem = (StringBundle.StringMapItem)item;
               this.serializer.startTag(null, name);

               for(Entry<String, StringBundle.Item<?>> entry : mapItem.getValue().entrySet()) {
                  StringBundle.Item<?> i = entry.getValue();
                  if (i.getType() == StringBundle.ItemType.STRING) {
                     this.writeItemImpl(entry.getKey(), i);
                  }
               }

               for(Entry<String, StringBundle.Item<?>> entry : mapItem.getValue().entrySet()) {
                  StringBundle.Item<?> i = entry.getValue();
                  if (i.getType() != StringBundle.ItemType.STRING) {
                     this.writeItemImpl(entry.getKey(), i);
                  }
               }

               this.serializer.endTag(null, name);
         }
      }
   }
}
