package net.osmand;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import java.io.UnsupportedEncodingException;
import org.apache.commons.logging.Log;

public class Reshaper {
   private static final Log LOG = PlatformUtil.getLog(Reshaper.class);

   public static String reshape(byte[] bytes) {
      try {
         return reshape(new String(bytes, "UTF-8"), false);
      } catch (UnsupportedEncodingException var2) {
         return "";
      }
   }

   public static String reshape(String s) {
      return reshape(s, true);
   }

   public static String reshape(String s, boolean reshape) {
      if (reshape) {
         ArabicShaping as = new ArabicShaping(8);

         try {
            s = as.shape(s);
         } catch (ArabicShapingException var4) {
            LOG.error(var4.getMessage(), var4);
         }
      }

      return bidiShape(s, reshape);
   }

   public static String bidiShape(String s, boolean mirror) {
      try {
         Bidi line = new Bidi(s.length(), s.length());
         line.setPara(s, (byte)126, null);
         byte direction = line.getDirection();
         if (direction != 2) {
            if (!line.isLeftToRight() && mirror) {
               char[] chs = new char[s.length()];

               for(int i = 0; i < chs.length; ++i) {
                  chs[i] = mirror(s.charAt(chs.length - i - 1));
               }

               return new String(chs);
            } else {
               return s;
            }
         } else {
            int count = line.countRuns();
            StringBuilder res = new StringBuilder();

            for(int i = 0; i < count; ++i) {
               StringBuilder runs = new StringBuilder();
               BidiRun run = line.getVisualRun(i);
               boolean ltr = run.getDirection() == 0;
               int start = run.getStart();
               int limit = run.getLimit();
               int begin = ltr ? start : limit - 1;
               int end = ltr ? limit : start - 1;
               int ind = begin;

               while(ind != end) {
                  char ch = s.charAt(ind);
                  if (!ltr && mirror) {
                     ch = mirror(ch);
                  }

                  res.append(ch);
                  runs.append(ch);
                  if (ltr) {
                     ++ind;
                  } else {
                     --ind;
                  }
               }
            }

            if (!mirror) {
               res.reverse();
            }

            return res.toString();
         }
      } catch (RuntimeException var17) {
         LOG.error(var17.getMessage(), var17);
         return s;
      }
   }

   private static char mirror(char ch) {
      switch(ch) {
         case '(':
            ch = ')';
            break;
         case ')':
            ch = '(';
            break;
         case '[':
            ch = ']';
            break;
         case ']':
            ch = '[';
      }

      return ch;
   }

   public static void main(String[] args) {
      test2();
      test3();
      test4();
      test5();
   }

   public static void test3() {
      String s = "מרכז מסחרי/השלום (40050)";
      String reshape = reshape(s);
      String expected = "(40050) םולשה/ירחסמ זכרמ";
      check(s, reshape, expected);
   }

   public static void test5() {
      String s = "מרכז מסחרי/השלום (מרז)";
      String reshape = reshape(s);
      String expected = "(זרמ) םולשה/ירחסמ זכרמ";
      check(s, reshape, expected);
   }

   public static void check(String source, String reshape, String expected) {
      printSplit("Source  ", source);
      printSplit("Expected", expected);
      printSplit("Reshaped", reshape);
      System.out.println(reshape);
      if (!reshape.equals(expected)) {
         throw new IllegalArgumentException(String.format("Bug: expected '%s', reshaped '%s'", expected, reshape));
      }
   }

   static void printSplit(String p, String source) {
      printSplit(p, source, true);
      printSplit(p, source, false);
   }

   static void printSplit(String p, String source, boolean f) {
      System.out.print(p);
      System.out.print(": \u2066");

      for(int i = 0; i < source.length(); ++i) {
         if (f) {
            System.out.print(source.charAt(i));
            System.out.print(" \u200e");
         } else {
            System.out.print(String.format("%04x ", Integer.valueOf(source.charAt(i))));
         }
      }

      System.out.println();
      System.out.flush();
   }

   public static void test2() {
      String s = "گچ پژ نمکی باللغة العربي";
      String reshape = reshape(s);
      String expected1 = "ﻲﺑﺮﻌﻟﺍ ﺔﻐﻠﻟﺎﺑ ﯽﮑﻤﻧ ﮋﭘ ﭻﮔ";
      String expected2 = "ﻲﺑﺮﻌﻟﺍ ﺔﻐﻠﻟﺎﺑ یکﻤﻧ ژپ چگ";
      check(s, reshape, expected1);
   }

   public static void test4() {
      String s = "Abc (123)";
      check(s, reshape(s), s);
   }
}
