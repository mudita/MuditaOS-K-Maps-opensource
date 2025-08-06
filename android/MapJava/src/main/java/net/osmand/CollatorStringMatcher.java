package net.osmand;

import java.util.Locale;

public class CollatorStringMatcher implements StringMatcher {
   private final Collator collator = OsmAndCollator.primaryCollator();
   private final CollatorStringMatcher.StringMatcherMode mode;
   private final String part;

   public CollatorStringMatcher(String part, CollatorStringMatcher.StringMatcherMode mode) {
      part = simplifyStringAndAlignChars(part);
      if (part.length() > 0 && part.charAt(part.length() - 1) == '.') {
         part = part.substring(0, part.length() - 1);
         if (mode == CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE) {
            mode = CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
         } else if (mode == CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS) {
            mode = CollatorStringMatcher.StringMatcherMode.CHECK_ONLY_STARTS_WITH;
         }
      }

      this.part = part;
      this.mode = mode;
   }

   public Collator getCollator() {
      return this.collator;
   }

   @Override
   public boolean matches(String name) {
      return cmatches(this.collator, name, this.part, this.mode);
   }

   public static boolean cmatches(Collator collator, String fullName, String part, CollatorStringMatcher.StringMatcherMode mode) {
      switch(mode) {
         case CHECK_CONTAINS:
            return ccontains(collator, fullName, part);
         case CHECK_EQUALS_FROM_SPACE:
            return cstartsWith(collator, fullName, part, true, true, true);
         case CHECK_STARTS_FROM_SPACE:
            return cstartsWith(collator, fullName, part, true, true, false);
         case CHECK_STARTS_FROM_SPACE_NOT_BEGINNING:
            return cstartsWith(collator, fullName, part, false, true, false);
         case CHECK_ONLY_STARTS_WITH:
            return cstartsWith(collator, fullName, part, true, false, false);
         case CHECK_EQUALS:
            return cstartsWith(collator, fullName, part, false, false, true);
         default:
            return false;
      }
   }

   public static boolean ccontains(Collator collator, String base, String part) {
      if (base.length() <= part.length()) {
         return collator.equals(base, part);
      } else {
         for(int pos = 0; pos <= base.length() - part.length() + 1; ++pos) {
            String temp = base.substring(pos, Math.min(pos + part.length() * 2, base.length()));

            for(int length = temp.length(); length >= 0; --length) {
               String temp2 = temp.substring(0, length);
               if (collator.equals(temp2, part)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static int cindexOf(Collator collator, int start, String part, String base) {
      for(int pos = start; pos <= base.length() - part.length(); ++pos) {
         if (collator.equals(base.substring(pos, pos + part.length()), part)) {
            return pos;
         }
      }

      return -1;
   }

   public static boolean cstartsWith(Collator collator, String fullTextP, String theStart, boolean checkBeginning, boolean checkSpaces, boolean equals) {
      theStart = alignChars(theStart);
      String searchIn = simplifyStringAndAlignChars(fullTextP);
      int searchInLength = searchIn.length();
      int startLength = theStart.length();
      if (startLength == 0) {
         return true;
      } else if (startLength > searchInLength) {
         return false;
      } else {
         if (checkBeginning) {
            boolean starts = collator.equals(searchIn.substring(0, startLength), theStart);
            if (starts) {
               if (!equals) {
                  return true;
               }

               if (startLength == searchInLength || isSpace(searchIn.charAt(startLength))) {
                  return true;
               }
            }
         }

         if (checkSpaces) {
            for(int i = 1; i <= searchInLength - startLength; ++i) {
               if (isSpace(searchIn.charAt(i - 1)) && !isSpace(searchIn.charAt(i)) && collator.equals(searchIn.substring(i, i + startLength), theStart)) {
                  if (!equals) {
                     return true;
                  }

                  if (i + startLength == searchInLength || isSpace(searchIn.charAt(i + startLength))) {
                     return true;
                  }
               }
            }
         }

         return !checkBeginning && !checkSpaces && equals ? collator.equals(searchIn, theStart) : false;
      }
   }

   private static String simplifyStringAndAlignChars(String fullText) {
      fullText = fullText.toLowerCase(Locale.getDefault());
      return alignChars(fullText);
   }

   private static String alignChars(String fullText) {
      int i;
      while((i = fullText.indexOf(223)) != -1) {
         fullText = fullText.substring(0, i) + "ss" + fullText.substring(i + 1);
      }

      return fullText;
   }

   private static boolean isSpace(char c) {
      return !Character.isLetter(c) && !Character.isDigit(c);
   }

   public static enum StringMatcherMode {
      CHECK_ONLY_STARTS_WITH,
      CHECK_STARTS_FROM_SPACE,
      CHECK_STARTS_FROM_SPACE_NOT_BEGINNING,
      CHECK_EQUALS_FROM_SPACE,
      CHECK_CONTAINS,
      CHECK_EQUALS;
   }
}
