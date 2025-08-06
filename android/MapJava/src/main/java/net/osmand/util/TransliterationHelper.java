package net.osmand.util;

import net.osmand.PlatformUtil;
import net.sf.junidecode.Junidecode;
import org.apache.commons.logging.Log;

public class TransliterationHelper {
   public static final Log LOG = PlatformUtil.getLog(TransliterationHelper.class);
   private static boolean japanese;

   private TransliterationHelper() {
   }

   public static boolean isJapanese() {
      return japanese;
   }

   public static void setJapanese(boolean japanese) {
      TransliterationHelper.japanese = japanese;
   }

   public static String transliterate(String text) {
      return japanese ? text : Junidecode.unidecode(text);
   }
}
