package net.osmand;

import java.util.Locale;

public class OsmAndCollator {
   public static Collator primaryCollator() {
      java.text.Collator instance = !Locale.getDefault().getLanguage().equals("ro")
            && !Locale.getDefault().getLanguage().equals("cs")
            && !Locale.getDefault().getLanguage().equals("sk")
         ? java.text.Collator.getInstance()
         : java.text.Collator.getInstance(Locale.US);
      instance.setStrength(0);
      return wrapCollator(instance);
   }

   public static Collator wrapCollator(final java.text.Collator instance) {
      return new Collator() {
         @Override
         public int compare(Object o1, Object o2) {
            return instance.compare(o1, o2);
         }

         @Override
         public boolean equals(Object obj) {
            return instance.equals(obj);
         }

         @Override
         public boolean equals(String source, String target) {
            return instance.equals(source, target);
         }

         @Override
         public int compare(String source, String target) {
            return instance.compare(source, target);
         }
      };
   }
}
