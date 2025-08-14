package net.osmand.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.osmand.data.TransportSchedule;
import net.osmand.data.TransportStop;

public class TransportRouteResult {
   List<TransportRoutePlanner.TransportRouteResultSegment> segments = new ArrayList<>(4);
   double finishWalkDist;
   double routeTime;
   private final TransportRoutingConfiguration cfg;

   public TransportRouteResult(TransportRoutingContext ctx) {
      this.cfg = ctx.cfg;
   }

   public TransportRouteResult(TransportRoutingConfiguration cfg) {
      this.cfg = cfg;
   }

   public List<TransportRoutePlanner.TransportRouteResultSegment> getSegments() {
      return this.segments;
   }

   public void setFinishWalkDist(double finishWalkDist) {
      this.finishWalkDist = finishWalkDist;
   }

   public void setRouteTime(double routeTime) {
      this.routeTime = routeTime;
   }

   public void addSegment(TransportRoutePlanner.TransportRouteResultSegment seg) {
      this.segments.add(seg);
   }

   public double getWalkDist() {
      double d = this.finishWalkDist;

      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         d += s.walkDist;
      }

      return d;
   }

   public double getFinishWalkDist() {
      return this.finishWalkDist;
   }

   public double getWalkSpeed() {
      return (double)this.cfg.walkSpeed;
   }

   public double getRouteTime() {
      return this.routeTime;
   }

   public int getStops() {
      int stops = 0;

      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         stops += s.end - s.start;
      }

      return stops;
   }

   public boolean isRouteStop(TransportStop stop) {
      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         if (s.getTravelStops().contains(stop)) {
            return true;
         }
      }

      return false;
   }

   public TransportRoutePlanner.TransportRouteResultSegment getRouteStopSegment(TransportStop stop) {
      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         if (s.getTravelStops().contains(stop)) {
            return s;
         }
      }

      return null;
   }

   public double getTravelDist() {
      double d = 0.0;

      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         d += s.getTravelDist();
      }

      return d;
   }

   public double getTravelTime() {
      double t = 0.0;

      for(TransportRoutePlanner.TransportRouteResultSegment s : this.segments) {
         if (this.cfg.useSchedule) {
            TransportSchedule sts = s.route.getSchedule();

            for(int k = s.start; k < s.end; ++k) {
               t += (double)(sts.getAvgStopIntervals()[k] * 10);
            }
         } else {
            t += (double)this.cfg.getBoardingTime();
            t += s.getTravelTime();
         }
      }

      return t;
   }

   public double getWalkTime() {
      return this.getWalkDist() / (double)this.cfg.walkSpeed;
   }

   public double getChangeTime() {
      return (double)this.cfg.getChangeTime();
   }

   public double getBoardingTime() {
      return (double)this.cfg.getBoardingTime();
   }

   public int getChanges() {
      return this.segments.size() - 1;
   }

   @Override
   public String toString() {
      StringBuilder bld = new StringBuilder();
      bld.append(
         String.format(
            Locale.US,
            "Route %d stops, %d changes, %.2f min: %.2f m (%.1f min) to walk, %.2f m (%.1f min) to travel\n",
            this.getStops(),
            this.getChanges(),
            this.routeTime / 60.0,
            this.getWalkDist(),
            this.getWalkTime() / 60.0,
            this.getTravelDist(),
            this.getTravelTime() / 60.0
         )
      );

      for(int i = 0; i < this.segments.size(); ++i) {
         TransportRoutePlanner.TransportRouteResultSegment s = this.segments.get(i);
         String time = "";
         String arriveTime = "";
         if (s.depTime != -1) {
            time = String.format("at %s", TransportRoutePlanner.formatTransportTime(s.depTime));
         }

         int aTime = s.getArrivalTime();
         if (aTime != -1) {
            arriveTime = String.format("and arrive at %s", TransportRoutePlanner.formatTransportTime(aTime));
         }

         bld.append(
            String.format(
               Locale.US,
               " %d. %s [%d]: walk %.1f m to '%s' and travel %s to '%s' by %s %d stops %s\n",
               i + 1,
               s.route.getRef(),
               s.route.getId() / 2L,
               s.walkDist,
               s.getStart().getName(),
               time,
               s.getEnd().getName(),
               s.route.getName(),
               s.end - s.start,
               arriveTime
            )
         );
      }

      bld.append(String.format(" F. Walk %.1f m to reach your destination", this.finishWalkDist));
      return bld.toString();
   }
}
