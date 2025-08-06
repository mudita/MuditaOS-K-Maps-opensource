package net.osmand.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SunriseSunset {
   private double dfLat;
   private double dfLon;
   private Date dateInput;
   private double dfTimeZone;
   private Date dateSunrise;
   private Date dateSunset;
   private boolean bSunriseToday = false;
   private boolean bSunsetToday = false;
   private boolean bSunUpAllDay = false;
   private boolean bSunDownAllDay = false;
   private boolean bDaytime = false;
   private boolean bSunrise = false;
   private boolean bSunset = false;
   private boolean bGregorian = false;
   private int iJulian;
   private int iYear;
   private int iMonth;
   private int iDay;
   private int iCount;
   private int iSign;
   private int dfHourRise;
   private int dfHourSet;
   private int dfMinRise;
   private int dfMinSet;
   private double dfSinLat;
   private double dfCosLat;
   private double dfZenith;
   private double dfAA1 = 0.0;
   private double dfAA2 = 0.0;
   private double dfDD1 = 0.0;
   private double dfDD2 = 0.0;
   private double dfC0;
   private double dfK1;
   private double dfP;
   private double dfJ;
   private double dfJ3;
   private double dfA;
   private double dfA0;
   private double dfA2;
   private double dfA5;
   private double dfD0;
   private double dfD1;
   private double dfD2;
   private double dfD5;
   private double dfDA;
   private double dfDD;
   private double dfH0;
   private double dfH1;
   private double dfH2;
   private double dfL0;
   private double dfL2;
   private double dfT;
   private double dfT0;
   private double dfTT;
   private double dfV0;
   private double dfV1;
   private double dfV2;

   public SunriseSunset(double dfLatIn, double dfLonIn, Date dateInputIn, TimeZone tzIn) {
      double dfTimeZoneIn = 1.0 * (double)tzIn.getOffset(dateInputIn.getTime()) / 3600000.0;
      this.dfLat = dfLatIn;
      this.dfLon = dfLonIn;
      this.dateInput = dateInputIn;
      this.dfTimeZone = dfTimeZoneIn;
      this.doCalculations();
   }

   private void doCalculations() {
      Calendar cin = Calendar.getInstance();
      cin.setTime(this.dateInput);
      this.iYear = cin.get(1);
      this.iMonth = cin.get(2) + 1;
      this.iDay = cin.get(5);
      this.dfTimeZone /= 24.0;
      this.dfTimeZone = -this.dfTimeZone;
      this.dfLon /= 360.0;
      if (this.iYear >= 1583) {
         this.bGregorian = true;
      }

      this.dfJ = -Math.floor(7.0 * (Math.floor(((double)this.iMonth + 9.0) / 12.0) + (double)this.iYear) / 4.0)
         + Math.floor((double)this.iMonth * 275.0 / 9.0)
         + (double)this.iDay
         + 1721027.0
         + (double)this.iYear * 367.0;
      if (this.bGregorian) {
         if ((double)this.iMonth - 9.0 < 0.0) {
            this.iSign = -1;
         } else {
            this.iSign = 1;
         }

         this.dfA = Math.abs((double)this.iMonth - 9.0);
         this.dfJ3 = -Math.floor((Math.floor(Math.floor((double)this.iYear + (double)this.iSign * Math.floor(this.dfA / 7.0)) / 100.0) + 1.0) * 0.75);
         this.dfJ = this.dfJ + this.dfJ3 + 2.0;
      }

      this.iJulian = (int)this.dfJ - 1;
      this.dfT = (double)this.iJulian - 2451545.0 + 0.5;
      this.dfTT = this.dfT / 36525.0 + 1.0;
      this.dfT0 = (this.dfT * 8640184.813 / 36525.0 + 24110.5 + this.dfTimeZone * 86636.6 + this.dfLon * 86400.0) / 86400.0;
      this.dfT0 -= Math.floor(this.dfT0);
      this.dfT0 = this.dfT0 * 2.0 * Math.PI;
      this.dfT += this.dfTimeZone;

      for(this.iCount = 0; this.iCount <= 1; ++this.iCount) {
         double dfLL = 0.779072 + 0.00273790931 * this.dfT;
         dfLL -= Math.floor(dfLL);
         dfLL = dfLL * 2.0 * Math.PI;
         double dfGG = 0.993126 + 0.0027377785 * this.dfT;
         dfGG -= Math.floor(dfGG);
         dfGG = dfGG * 2.0 * Math.PI;
         double dfVV = 0.39785 * Math.sin(dfLL) - 0.01 * Math.sin(dfLL - dfGG) + 0.00333 * Math.sin(dfLL + dfGG) - 2.1E-4 * Math.sin(dfLL) * this.dfTT;
         double dfUU = 1.0 - 0.03349 * Math.cos(dfGG) - 1.4E-4 * Math.cos(dfLL * 2.0) + 8.0E-5 * Math.cos(dfLL);
         double dfWW = -1.0E-4
            - 0.04129 * Math.sin(dfLL * 2.0)
            + 0.03211 * Math.sin(dfGG)
            - 0.00104 * Math.sin(2.0 * dfLL - dfGG)
            - 3.5E-4 * Math.sin(2.0 * dfLL + dfGG)
            - 8.0E-5 * Math.sin(dfGG) * this.dfTT;
         double dfSS = dfWW / Math.sqrt(dfUU - dfVV * dfVV);
         this.dfA5 = dfLL + Math.atan(dfSS / Math.sqrt(1.0 - dfSS * dfSS));
         dfSS = dfVV / Math.sqrt(dfUU);
         this.dfD5 = Math.atan(dfSS / Math.sqrt(1.0 - dfSS * dfSS));
         if (this.iCount == 0) {
            this.dfAA1 = this.dfA5;
            this.dfDD1 = this.dfD5;
         } else {
            this.dfAA2 = this.dfA5;
            this.dfDD2 = this.dfD5;
         }

         ++this.dfT;
      }

      if (this.dfAA2 < this.dfAA1) {
         this.dfAA2 += Math.PI * 2;
      }

      this.dfZenith = 1.5853349194640092;
      this.dfSinLat = Math.sin(this.dfLat * Math.PI / 180.0);
      this.dfCosLat = Math.cos(this.dfLat * Math.PI / 180.0);
      this.dfA0 = this.dfAA1;
      this.dfD0 = this.dfDD1;
      this.dfDA = this.dfAA2 - this.dfAA1;
      this.dfDD = this.dfDD2 - this.dfDD1;
      this.dfK1 = 0.26251616834300473;
      this.dfHourRise = 99;
      this.dfMinRise = 99;
      this.dfHourSet = 99;
      this.dfMinSet = 99;
      this.dfV0 = 0.0;
      this.dfV2 = 0.0;

      for(this.iCount = 0; this.iCount < 24; ++this.iCount) {
         this.dfC0 = (double)this.iCount;
         this.dfP = (this.dfC0 + 1.0) / 24.0;
         this.dfA2 = this.dfAA1 + this.dfP * this.dfDA;
         this.dfD2 = this.dfDD1 + this.dfP * this.dfDD;
         this.dfL0 = this.dfT0 + this.dfC0 * this.dfK1;
         this.dfL2 = this.dfL0 + this.dfK1;
         this.dfH0 = this.dfL0 - this.dfA0;
         this.dfH2 = this.dfL2 - this.dfA2;
         this.dfH1 = (this.dfH2 + this.dfH0) / 2.0;
         this.dfD1 = (this.dfD2 + this.dfD0) / 2.0;
         if (this.iCount == 0) {
            this.dfV0 = this.dfSinLat * Math.sin(this.dfD0) + this.dfCosLat * Math.cos(this.dfD0) * Math.cos(this.dfH0) - Math.cos(this.dfZenith);
         } else {
            this.dfV0 = this.dfV2;
         }

         this.dfV2 = this.dfSinLat * Math.sin(this.dfD2) + this.dfCosLat * Math.cos(this.dfD2) * Math.cos(this.dfH2) - Math.cos(this.dfZenith);
         if ((!(this.dfV0 >= 0.0) || !(this.dfV2 >= 0.0)) && (!(this.dfV0 < 0.0) || !(this.dfV2 < 0.0))) {
            this.dfV1 = this.dfSinLat * Math.sin(this.dfD1) + this.dfCosLat * Math.cos(this.dfD1) * Math.cos(this.dfH1) - Math.cos(this.dfZenith);
            double tempA = 2.0 * this.dfV2 - 4.0 * this.dfV1 + 2.0 * this.dfV0;
            double tempB = 4.0 * this.dfV1 - 3.0 * this.dfV0 - this.dfV2;
            double tempD = tempB * tempB - 4.0 * tempA * this.dfV0;
            if (tempD < 0.0) {
               this.dfA0 = this.dfA2;
               this.dfD0 = this.dfD2;
            } else {
               tempD = Math.sqrt(tempD);
               this.bSunrise = false;
               this.bSunset = false;
               if (this.dfV0 < 0.0 && this.dfV2 > 0.0) {
                  this.bSunrise = true;
                  this.bSunriseToday = true;
               }

               if (this.dfV0 > 0.0 && this.dfV2 < 0.0) {
                  this.bSunset = true;
                  this.bSunsetToday = true;
               }

               double tempE = (tempD - tempB) / (2.0 * tempA);
               if (tempE > 1.0 || tempE < 0.0) {
                  tempE = (-tempD - tempB) / (2.0 * tempA);
               }

               if (this.bSunrise) {
                  this.dfHourRise = (int)(this.dfC0 + tempE + 0.008333333333333333);
                  this.dfMinRise = (int)((this.dfC0 + tempE + 0.008333333333333333 - (double)this.dfHourRise) * 60.0);
               }

               if (this.bSunset) {
                  this.dfHourSet = (int)(this.dfC0 + tempE + 0.008333333333333333);
                  this.dfMinSet = (int)((this.dfC0 + tempE + 0.008333333333333333 - (double)this.dfHourSet) * 60.0);
               }

               this.dfA0 = this.dfA2;
               this.dfD0 = this.dfD2;
            }
         } else {
            this.dfA0 = this.dfA2;
            this.dfD0 = this.dfD2;
         }
      }

      if (!this.bSunriseToday && !this.bSunsetToday) {
         if (this.dfV2 < 0.0) {
            this.bSunDownAllDay = true;
         } else {
            this.bSunUpAllDay = true;
         }
      }

      if (this.bSunriseToday) {
         Calendar c = Calendar.getInstance();
         c.set(1, this.iYear);
         c.set(2, this.iMonth - 1);
         c.set(5, this.iDay);
         c.set(11, this.dfHourRise);
         c.set(12, this.dfMinRise);
         this.dateSunrise = c.getTime();
      }

      if (this.bSunsetToday) {
         Calendar c = Calendar.getInstance();
         c.set(1, this.iYear);
         c.set(2, this.iMonth - 1);
         c.set(5, this.iDay);
         c.set(11, this.dfHourSet);
         c.set(12, this.dfMinSet);
         this.dateSunset = c.getTime();
      }
   }

   public Date getSunrise() {
      return this.bSunriseToday ? this.dateSunrise : null;
   }

   public Date getSunset() {
      return this.bSunsetToday ? this.dateSunset : null;
   }

   public boolean isSunrise() {
      return this.bSunriseToday;
   }

   public boolean isSunset() {
      return this.bSunsetToday;
   }

   public boolean isSunUp() {
      return this.bSunUpAllDay;
   }

   public boolean isSunDown() {
      return this.bSunDownAllDay;
   }

   public boolean isDaytime() {
      if (this.bSunriseToday && this.bSunsetToday) {
         if (this.dateSunrise.before(this.dateSunset)) {
            if ((this.dateInput.after(this.dateSunrise) || this.dateInput.equals(this.dateSunrise)) && this.dateInput.before(this.dateSunset)) {
               this.bDaytime = true;
            } else {
               this.bDaytime = false;
            }
         } else if (!this.dateInput.after(this.dateSunrise) && !this.dateInput.equals(this.dateSunrise) && !this.dateInput.before(this.dateSunset)) {
            this.bDaytime = false;
         } else {
            this.bDaytime = true;
         }
      } else if (this.bSunUpAllDay) {
         this.bDaytime = true;
      } else if (this.bSunDownAllDay) {
         this.bDaytime = false;
      } else if (this.bSunriseToday) {
         if (this.dateInput.before(this.dateSunrise)) {
            this.bDaytime = false;
         } else {
            this.bDaytime = true;
         }
      } else if (this.bSunsetToday) {
         if (this.dateInput.before(this.dateSunset)) {
            this.bDaytime = true;
         } else {
            this.bDaytime = false;
         }
      } else {
         this.bDaytime = false;
      }

      return this.bDaytime;
   }
}
