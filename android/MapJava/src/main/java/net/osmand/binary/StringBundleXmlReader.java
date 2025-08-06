package net.osmand.binary;

import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

public class StringBundleXmlReader extends StringBundleReader {
   public static final Log log = PlatformUtil.getLog(StringBundleXmlReader.class);
   private XmlPullParser parser;

   public StringBundleXmlReader(XmlPullParser parser) {
      this.parser = parser;
   }

   @Override
   public void readBundle() {
      StringBundle bundle = this.getBundle();

      for(int i = 0; i < this.parser.getAttributeCount(); ++i) {
         String name = this.parser.getAttributeName(i);
         String value = this.parser.getAttributeValue(i);
         bundle.putString(name, value);
      }
   }
}
