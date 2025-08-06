package net.osmand.data;

import gnu.trove.list.array.TIntArrayList;

public class TransportSchedule {
   public TIntArrayList tripIntervals = new TIntArrayList();
   public TIntArrayList avgStopIntervals = new TIntArrayList();
   public TIntArrayList avgWaitIntervals = new TIntArrayList();

   public TransportSchedule() {
   }

   public TransportSchedule(TIntArrayList tripIntervals, TIntArrayList avgStopIntervals, TIntArrayList avgWaitIntervals) {
      this.tripIntervals = tripIntervals;
      this.avgStopIntervals = avgStopIntervals;
      this.avgWaitIntervals = avgWaitIntervals;
   }

   public int[] getTripIntervals() {
      return this.tripIntervals.toArray();
   }

   public int[] getAvgStopIntervals() {
      return this.avgStopIntervals.toArray();
   }

   public int[] getAvgWaitIntervals() {
      return this.avgWaitIntervals.toArray();
   }

   public boolean compareSchedule(TransportSchedule thatObj) {
      if (this == thatObj) {
         return true;
      } else {
         return this.tripIntervals.equals(thatObj.tripIntervals)
            && this.avgStopIntervals.equals(thatObj.avgStopIntervals)
            && this.avgWaitIntervals.equals(thatObj.avgWaitIntervals);
      }
   }
}
