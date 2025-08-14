package net.osmand;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Period {
   private static final Pattern PATTERN = Pattern.compile("^P(?:([-+]?[0-9]+)([YMWD]))?$", 2);
   private Period.PeriodUnit unit;
   private final int numberOfUnits;

   public static Period ofYears(int years) {
      return new Period(Period.PeriodUnit.YEAR, years);
   }

   public static Period ofMonths(int months) {
      return new Period(Period.PeriodUnit.MONTH, months);
   }

   public static Period ofWeeks(int weeks) {
      return new Period(Period.PeriodUnit.WEEK, weeks);
   }

   public static Period ofDays(int days) {
      return new Period(Period.PeriodUnit.DAY, days);
   }

   public Period.PeriodUnit getUnit() {
      return this.unit;
   }

   public int getNumberOfUnits() {
      return this.numberOfUnits;
   }

   public static Period parse(CharSequence text) throws ParseException {
      Matcher matcher = PATTERN.matcher(text);
      if (matcher.matches()) {
         String numberOfUnitsMatch = matcher.group(1);
         String unitMatch = matcher.group(2);
         if (numberOfUnitsMatch != null && unitMatch != null) {
            try {
               int numberOfUnits = parseNumber(numberOfUnitsMatch);
               Period.PeriodUnit unit = Period.PeriodUnit.parseUnit(unitMatch);
               return new Period(unit, numberOfUnits);
            } catch (IllegalArgumentException var6) {
               throw new ParseException("Text cannot be parsed to a Period: " + text, 0);
            }
         }
      }

      throw new ParseException("Text cannot be parsed to a Period: " + text, 0);
   }

   private static int parseNumber(String str) throws ParseException {
      return str == null ? 0 : Integer.parseInt(str);
   }

   public Period(Period.PeriodUnit unit, int numberOfUnits) {
      if (unit == null) {
         throw new IllegalArgumentException("PeriodUnit cannot be null");
      } else {
         this.unit = unit;
         this.numberOfUnits = numberOfUnits;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!(obj instanceof Period)) {
         return false;
      } else {
         Period other = (Period)obj;
         return this.unit.ordinal() == other.unit.ordinal() && this.numberOfUnits == other.numberOfUnits;
      }
   }

   @Override
   public int hashCode() {
      return this.unit.ordinal() + Integer.rotateLeft(this.numberOfUnits, 8);
   }

   @Override
   public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append('P').append(this.numberOfUnits);
      switch(this.unit) {
         case YEAR:
            buf.append('Y');
            break;
         case MONTH:
            buf.append('M');
            break;
         case WEEK:
            buf.append('W');
            break;
         case DAY:
            buf.append('D');
      }

      return buf.toString();
   }

   public static enum PeriodUnit {
      YEAR("Y", 1),
      MONTH("M", 2),
      WEEK("W", 3),
      DAY("D", 5);

      private String unitStr;
      private int calendarIdx;

      private PeriodUnit(String unitStr, int calendarIdx) {
         this.calendarIdx = calendarIdx;
         this.unitStr = unitStr;
      }

      public String getUnitStr() {
         return this.unitStr;
      }

      public int getCalendarIdx() {
         return this.calendarIdx;
      }

      public double getMonthsValue() {
         switch(this) {
            case YEAR:
               return 12.0;
            case MONTH:
               return 1.0;
            case WEEK:
               return 0.25;
            case DAY:
               return 0.03333333333333333;
            default:
               return 0.0;
         }
      }

      public static Period.PeriodUnit parseUnit(String unitStr) {
         for(Period.PeriodUnit unit : values()) {
            if (unit.unitStr.equals(unitStr)) {
               return unit;
            }
         }

         return null;
      }
   }
}
