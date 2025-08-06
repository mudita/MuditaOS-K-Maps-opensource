package net.osmand.router;

import gnu.trove.set.hash.TIntHashSet;
import net.osmand.util.Algorithms;

public class TurnType {
   public static final int C = 1;
   public static final int TL = 2;
   public static final int TSLL = 3;
   public static final int TSHL = 4;
   public static final int TR = 5;
   public static final int TSLR = 6;
   public static final int TSHR = 7;
   public static final int KL = 8;
   public static final int KR = 9;
   public static final int TU = 10;
   public static final int TRU = 11;
   public static final int OFFR = 12;
   public static final int RNDB = 13;
   public static final int RNLB = 14;
   private final int value;
   private int exitOut;
   private float turnAngle;
   private boolean skipToSpeak;
   private int[] lanes;
   private boolean possiblyLeftTurn;
   private boolean possiblyRightTurn;

   public static TurnType straight() {
      return valueOf(1, false);
   }

   public int getActiveCommonLaneTurn() {
      if (this.lanes != null && this.lanes.length != 0) {
         for(int i = 0; i < this.lanes.length; ++i) {
            if (this.lanes[i] % 2 == 1) {
               return getPrimaryTurn(this.lanes[i]);
            }
         }

         return 1;
      } else {
         return 1;
      }
   }

   public String toXmlString() {
      switch(this.value) {
         case 1:
            return "C";
         case 2:
            return "TL";
         case 3:
            return "TSLL";
         case 4:
            return "TSHL";
         case 5:
            return "TR";
         case 6:
            return "TSLR";
         case 7:
            return "TSHR";
         case 8:
            return "KL";
         case 9:
            return "KR";
         case 10:
            return "TU";
         case 11:
            return "TRU";
         case 12:
            return "OFFR";
         case 13:
            return "RNDB" + this.exitOut;
         case 14:
            return "RNLB" + this.exitOut;
         default:
            return "C";
      }
   }

   public static TurnType fromString(String s, boolean leftSide) {
      TurnType t = null;
      if ("C".equals(s)) {
         t = valueOf(1, leftSide);
      } else if ("TL".equals(s)) {
         t = valueOf(2, leftSide);
      } else if ("TSLL".equals(s)) {
         t = valueOf(3, leftSide);
      } else if ("TSHL".equals(s)) {
         t = valueOf(4, leftSide);
      } else if ("TR".equals(s)) {
         t = valueOf(5, leftSide);
      } else if ("TSLR".equals(s)) {
         t = valueOf(6, leftSide);
      } else if ("TSHR".equals(s)) {
         t = valueOf(7, leftSide);
      } else if ("KL".equals(s)) {
         t = valueOf(8, leftSide);
      } else if ("KR".equals(s)) {
         t = valueOf(9, leftSide);
      } else if ("TU".equals(s)) {
         t = valueOf(10, leftSide);
      } else if ("TRU".equals(s)) {
         t = valueOf(11, leftSide);
      } else if ("OFFR".equals(s)) {
         t = valueOf(12, leftSide);
      } else if (s != null && (s.startsWith("EXIT") || s.startsWith("RNDB") || s.startsWith("RNLB"))) {
         try {
            int type = s.contains("RNLB") ? RNLB : RNDB;
            t = TurnType.getExitTurn(type, Integer.parseInt(s.substring(4)), 0, leftSide);
         } catch (NumberFormatException var4) {
            var4.printStackTrace();
         }
      }

      if (t == null) {
         t = straight();
      }

      return t;
   }

   public static TurnType valueOf(int vs, boolean leftSide) {
      if (vs == 10 && leftSide) {
         vs = 11;
      } else if (vs == 13 && leftSide) {
         vs = 14;
      }

      return new TurnType(vs);
   }

   public TurnType(int value, int exitOut, float turnAngle, boolean skipToSpeak, int[] lanes, boolean possiblyLeftTurn, boolean possiblyRightTurn) {
      this.value = value;
      this.exitOut = exitOut;
      this.turnAngle = turnAngle;
      this.skipToSpeak = skipToSpeak;
      this.lanes = lanes;
      this.possiblyLeftTurn = possiblyLeftTurn;
      this.possiblyRightTurn = possiblyRightTurn;
   }

   public static TurnType getExitTurn(int out, float angle, boolean leftSide) {
      TurnType r = valueOf(13, leftSide);
      r.exitOut = out;
      r.setTurnAngle(angle);
      return r;
   }

   private static TurnType getExitTurn(int type, int out, float angle, boolean leftSide) {
      if (type != RNDB && type != RNLB) {
         return getExitTurn(out, angle, leftSide);
      }
      TurnType r = valueOf(type, leftSide);
      r.exitOut = out;
      r.setTurnAngle(angle);
      return r;
   }

   private TurnType(int vl) {
      this.value = vl;
   }

   public float getTurnAngle() {
      return this.turnAngle;
   }

   public boolean isLeftSide() {
      return this.value == 14 || this.value == 11;
   }

   public void setExitOut(int exitOut) {
      this.exitOut = exitOut;
   }

   public void setTurnAngle(float turnAngle) {
      this.turnAngle = turnAngle;
   }

   public int getValue() {
      return this.value;
   }

   public int getExitOut() {
      return this.exitOut;
   }

   public boolean isRoundAbout() {
      return this.value == 13 || this.value == 14;
   }

   public void setLanes(int[] lanes) {
      this.lanes = lanes;
   }

   public static void setPrimaryTurnAndReset(int[] lanes, int lane, int turnType) {
      lanes[lane] = turnType << 1;
   }

   public static int getPrimaryTurn(int laneValue) {
      return laneValue >> 1 & 15;
   }

   public static void setSecondaryTurn(int[] lanes, int lane, int turnType) {
      lanes[lane] &= -481;
      lanes[lane] |= turnType << 5;
   }

   public static void setPrimaryTurn(int[] lanes, int lane, int turnType) {
      lanes[lane] &= -31;
      lanes[lane] |= turnType << 1;
   }

   public static int getSecondaryTurn(int laneValue) {
      return laneValue >> 5 & 31;
   }

   public static void setPrimaryTurnShiftOthers(int[] lanes, int lane, int turnType) {
      int pt = getPrimaryTurn(lanes[lane]);
      int st = getSecondaryTurn(lanes[lane]);
      setPrimaryTurnAndReset(lanes, lane, turnType);
      setSecondaryTurn(lanes, lane, pt);
      setTertiaryTurn(lanes, lane, st);
   }

   public static void setSecondaryToPrimary(int[] lanes, int lane) {
      int st = getSecondaryTurn(lanes[lane]);
      int pt = getPrimaryTurn(lanes[lane]);
      setPrimaryTurn(lanes, lane, st);
      setSecondaryTurn(lanes, lane, pt);
   }

   public static void setTertiaryToPrimary(int[] lanes, int lane) {
      int st = getSecondaryTurn(lanes[lane]);
      int pt = getPrimaryTurn(lanes[lane]);
      int tt = getTertiaryTurn(lanes[lane]);
      setPrimaryTurn(lanes, lane, tt);
      setSecondaryTurn(lanes, lane, pt);
      setTertiaryTurn(lanes, lane, st);
   }

   public static void setTertiaryTurn(int[] lanes, int lane, int turnType) {
      lanes[lane] &= -15361;
      lanes[lane] |= turnType << 10;
   }

   public static int getTertiaryTurn(int laneValue) {
      return laneValue >> 10;
   }

   public static String lanesToString(int[] lns) {
      StringBuilder s = new StringBuilder();

      for(int h = 0; h < lns.length; ++h) {
         if (h > 0) {
            s.append("|");
         }

         if (lns[h] % 2 == 1) {
            s.append("+");
         }

         int pt = getPrimaryTurn(lns[h]);
         if (pt == 0) {
            pt = 1;
         }

         s.append(valueOf(pt, false).toXmlString());
         int st = getSecondaryTurn(lns[h]);
         if (st != 0) {
            s.append(",").append(valueOf(st, false).toXmlString());
         }

         int tt = getTertiaryTurn(lns[h]);
         if (tt != 0) {
            s.append(",").append(valueOf(tt, false).toXmlString());
         }
      }

      return s.toString();
   }

   public static int[] lanesFromString(String lanesString) {
      if (Algorithms.isEmpty(lanesString)) {
         return null;
      } else {
         String[] lanesArr = lanesString.split("\\|");
         int[] lanes = new int[lanesArr.length];

         for(int l = 0; l < lanesArr.length; ++l) {
            String lane = lanesArr[l];
            String[] turns = lane.split(",");
            TurnType primaryTurn = null;
            TurnType secondaryTurn = null;
            TurnType tertiaryTurn = null;
            boolean plus = false;

            for(int i = 0; i < turns.length; ++i) {
               String turn = turns[i];
               if (i == 0) {
                  plus = turn.length() > 0 && turn.charAt(0) == '+';
                  if (plus) {
                     turn = turn.substring(1);
                  }

                  primaryTurn = fromString(turn, false);
               } else if (i == 1) {
                  secondaryTurn = fromString(turn, false);
               } else if (i == 2) {
                  tertiaryTurn = fromString(turn, false);
               }
            }

            setPrimaryTurnAndReset(lanes, l, primaryTurn.value);
            if (secondaryTurn != null) {
               setSecondaryTurn(lanes, l, secondaryTurn.value);
            }

            if (tertiaryTurn != null) {
               setTertiaryTurn(lanes, l, tertiaryTurn.value);
            }

            if (plus) {
               lanes[l] |= 1;
            }
         }

         return lanes;
      }
   }

   public int[] getLanes() {
      return this.lanes;
   }

   public void setPossibleLeftTurn(boolean possiblyLeftTurn) {
      this.possiblyLeftTurn = possiblyLeftTurn;
   }

   public void setPossibleRightTurn(boolean possiblyRightTurn) {
      this.possiblyRightTurn = possiblyRightTurn;
   }

   public boolean isPossibleLeftTurn() {
      return this.possiblyLeftTurn;
   }

   public boolean isPossibleRightTurn() {
      return this.possiblyRightTurn;
   }

   public boolean keepLeft() {
      return this.value == 8;
   }

   public boolean keepRight() {
      return this.value == 9;
   }

   public boolean goAhead() {
      return this.value == 1;
   }

   public boolean isSkipToSpeak() {
      return this.skipToSpeak;
   }

   public void setSkipToSpeak(boolean skipToSpeak) {
      this.skipToSpeak = skipToSpeak;
   }

   @Override
   public String toString() {
      String vl = null;
      if (this.isRoundAbout()) {
         vl = "Take " + this.getExitOut() + " exit";
      } else if (this.value == 1) {
         vl = "Go ahead";
      } else if (this.value == 3) {
         vl = "Turn slightly left";
      } else if (this.value == 2) {
         vl = "Turn left";
      } else if (this.value == 4) {
         vl = "Turn sharply left";
      } else if (this.value == 6) {
         vl = "Turn slightly right";
      } else if (this.value == 5) {
         vl = "Turn right";
      } else if (this.value == 7) {
         vl = "Turn sharply right";
      } else if (this.value == 10) {
         vl = "Make uturn";
      } else if (this.value == 11) {
         vl = "Make uturn";
      } else if (this.value == 8) {
         vl = "Keep left";
      } else if (this.value == 9) {
         vl = "Keep right";
      } else if (this.value == 12) {
         vl = "Off route";
      }

      if (vl != null) {
         if (this.lanes != null) {
            vl = vl + " (" + lanesToString(this.lanes) + ")";
         }

         return vl;
      } else {
         return super.toString();
      }
   }

   public static boolean isLeftTurn(int type) {
      return type == 2 || type == 4 || type == 3 || type == 10 || type == 8;
   }

   public static boolean isLeftTurnNoUTurn(int type) {
      return type == 2 || type == 4 || type == 3 || type == 8;
   }

   public static boolean isRightTurn(int type) {
      return type == 5 || type == 7 || type == 6 || type == 11 || type == 9;
   }

   public static boolean isRightTurnNoUTurn(int type) {
      return type == 5 || type == 7 || type == 6 || type == 9;
   }

   public static boolean isSlightTurn(int type) {
      return type == 3 || type == 6 || type == 1 || type == 8 || type == 9;
   }

   public static boolean isKeepDirectionTurn(int type) {
      return type == 1 || type == 8 || type == 9;
   }

   public static boolean hasAnySlightTurnLane(int type) {
      return isSlightTurn(getPrimaryTurn(type)) || isSlightTurn(getSecondaryTurn(type)) || isSlightTurn(getTertiaryTurn(type));
   }

   public static boolean hasAnyTurnLane(int type, int turn) {
      return getPrimaryTurn(type) == turn || getSecondaryTurn(type) == turn || getTertiaryTurn(type) == turn;
   }

   public static void collectTurnTypes(int lane, TIntHashSet set) {
      int pt = getPrimaryTurn(lane);
      if (pt != 0) {
         set.add(pt);
      }

      pt = getSecondaryTurn(lane);
      if (pt != 0) {
         set.add(pt);
      }

      pt = getTertiaryTurn(lane);
      if (pt != 0) {
         set.add(pt);
      }
   }

   public static int orderFromLeftToRight(int type) {
      switch(type) {
         case 2:
            return -3;
         case 3:
            return -2;
         case 4:
            return -4;
         case 5:
            return 3;
         case 6:
            return 2;
         case 7:
            return 4;
         case 8:
            return -1;
         case 9:
            return 1;
         case 10:
            return -5;
         case 11:
            return 5;
         default:
            return 0;
      }
   }

   public static int convertType(String lane) {
      int turn;
      if (lane.equals("merge_to_left")) {
         turn = 1;
      } else if (lane.equals("merge_to_right")) {
         turn = 1;
      } else if (lane.equals("none") || lane.equals("through")) {
         turn = 1;
      } else if (lane.equals("slight_right")) {
         turn = 6;
      } else if (lane.equals("slight_left")) {
         turn = 3;
      } else if (lane.equals("right")) {
         turn = 5;
      } else if (lane.equals("left")) {
         turn = 2;
      } else if (lane.equals("sharp_right")) {
         turn = 7;
      } else if (lane.equals("sharp_left")) {
         turn = 4;
      } else if (lane.equals("reverse")) {
         turn = 10;
      } else {
         turn = 1;
      }

      return turn;
   }
}
