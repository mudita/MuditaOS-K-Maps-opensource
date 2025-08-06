package net.osmand.router;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class PrecalculatedRouteDirection {
   private int[] pointsX;
   private int[] pointsY;
   private float minSpeed;
   private float maxSpeed;
   private float[] tms;
   private boolean followNext;
   private static final int SHIFT = 16384;
   private static final int[] SHIFTS = new int[]{65536, 262144, 524288, 1048576, 16777216};
   private List<Integer> cachedS = new ArrayList<>();
   private long startPoint = 0L;
   private long endPoint = 0L;
   QuadTree<Integer> quadTree = new QuadTree<>(new QuadRect(0.0, 0.0, 2.147483647E9, 2.147483647E9), 8, 0.55F);
   private float startFinishTime;
   private float endFinishTime;

   public PrecalculatedRouteDirection(TIntArrayList px, TIntArrayList py, List<Float> speedSegments, float maxSpeed) {
      this.maxSpeed = maxSpeed;
      this.init(px, py, speedSegments);
   }

   private PrecalculatedRouteDirection(List<RouteSegmentResult> ls, float maxSpeed) {
      this.maxSpeed = maxSpeed;
      this.init(ls);
   }

   private PrecalculatedRouteDirection(LatLon[] ls, float maxSpeed) {
      this.maxSpeed = maxSpeed;
      this.init(ls);
   }

   private PrecalculatedRouteDirection(PrecalculatedRouteDirection parent, int s1, int s2) {
      this.minSpeed = parent.minSpeed;
      this.maxSpeed = parent.maxSpeed;
      boolean inverse = false;
      if (s1 > s2) {
         int tmp = s1;
         s1 = s2;
         s2 = tmp;
         inverse = true;
      }

      this.tms = new float[s2 - s1 + 1];
      this.pointsX = new int[s2 - s1 + 1];
      this.pointsY = new int[s2 - s1 + 1];

      for(int i = s1; i <= s2; ++i) {
         int shiftInd = i - s1;
         this.pointsX[shiftInd] = parent.pointsX[i];
         this.pointsY[shiftInd] = parent.pointsY[i];
         this.quadTree.insert(shiftInd, (float)parent.pointsX[i], (float)parent.pointsY[i]);
         this.tms[shiftInd] = parent.tms[i] - parent.tms[inverse ? s1 : s2];
      }
   }

   public static PrecalculatedRouteDirection build(List<RouteSegmentResult> ls, float cutoffDistance, float maxSpeed) {
      int begi = 0;

      for(float d = cutoffDistance; begi < ls.size(); ++begi) {
         d -= ls.get(begi).getDistance();
         if (d < 0.0F) {
            break;
         }
      }

      int endi = ls.size();

      for(float var6 = cutoffDistance; endi > 0; --endi) {
         var6 -= ls.get(endi - 1).getDistance();
         if (var6 < 0.0F) {
            break;
         }
      }

      return begi < endi ? new PrecalculatedRouteDirection(ls.subList(begi, endi), maxSpeed) : null;
   }

   public static PrecalculatedRouteDirection build(LatLon[] ls, float maxSpeed) {
      return new PrecalculatedRouteDirection(ls, maxSpeed);
   }

   private void init(List<RouteSegmentResult> ls) {
      TIntArrayList px = new TIntArrayList();
      TIntArrayList py = new TIntArrayList();
      List<Float> speedSegments = new ArrayList<>();

      for(RouteSegmentResult s : ls) {
         boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
         int i = s.getStartPointIndex();
         RouteDataObject obj = s.getObject();
         float routeSpd = s.getRoutingTime() != 0.0F && s.getDistance() != 0.0F ? s.getDistance() / s.getRoutingTime() : this.maxSpeed;

         do {
            i = plus ? i + 1 : i - 1;
            if (i < 0 || i >= obj.pointsX.length) break;
            px.add(obj.getPoint31XTile(i));
            py.add(obj.getPoint31YTile(i));
            speedSegments.add(routeSpd);
         } while(i == s.getEndPointIndex());
      }

      this.init(px, py, speedSegments);
   }

   private void init(LatLon[] ls) {
      TIntArrayList px = new TIntArrayList();
      TIntArrayList py = new TIntArrayList();
      List<Float> speedSegments = new ArrayList<>();

      for(LatLon s : ls) {
         float routeSpd = this.maxSpeed;
         px.add(MapUtils.get31TileNumberX(s.getLongitude()));
         py.add(MapUtils.get31TileNumberY(s.getLatitude()));
         speedSegments.add(routeSpd);
      }

      this.init(px, py, speedSegments);
   }

   private void init(TIntArrayList px, TIntArrayList py, List<Float> speedSegments) {
      float totaltm = 0.0F;
      List<Float> times = new ArrayList<>();

      for(int i = 0; i < px.size(); ++i) {
         int ip = i == 0 ? 0 : i - 1;
         float dist = (float)MapUtils.measuredDist31(px.get(ip), py.get(ip), px.get(i), py.get(i));
         float tm = dist / speedSegments.get(i);
         times.add(tm);
         this.quadTree.insert(i, (float)px.get(i), (float)py.get(i));
         totaltm += tm;
      }

      this.pointsX = px.toArray();
      this.pointsY = py.toArray();
      this.tms = new float[times.size()];
      float totDec = totaltm;

      for(int i = 0; i < times.size(); ++i) {
         totDec -= times.get(i);
         this.tms[i] = totDec;
      }
   }

   public float timeEstimate(int sx31, int sy31, int ex31, int ey31) {
      long l1 = this.calc(sx31, sy31);
      long l2 = this.calc(ex31, ey31);
      int x31;
      int y31;
      boolean start;
      if (l1 != this.startPoint && l1 != this.endPoint) {
         if (l2 != this.startPoint && l2 != this.endPoint) {
            throw new UnsupportedOperationException();
         }

         start = l2 == this.startPoint;
         x31 = sx31;
         y31 = sy31;
      } else {
         start = l1 == this.startPoint;
         x31 = ex31;
         y31 = ey31;
      }

      int ind = this.getIndex(x31, y31);
      if (ind == -1) {
         return -1.0F;
      } else if ((ind != 0 || !start) && (ind != this.pointsX.length - 1 || start)) {
         float distToPoint = this.getDeviationDistance(x31, y31, ind);
         float deviationPenalty = distToPoint / this.minSpeed;
         float finishTime = start ? this.startFinishTime : this.endFinishTime;
         return start ? this.tms[0] - this.tms[ind] + deviationPenalty + finishTime : this.tms[ind] + deviationPenalty + finishTime;
      } else {
         return -1.0F;
      }
   }

   public float getDeviationDistance(int x31, int y31) {
      int ind = this.getIndex(x31, y31);
      return ind == -1 ? 0.0F : this.getDeviationDistance(x31, y31, ind);
   }

   public float getDeviationDistance(int x31, int y31, int ind) {
      float distToPoint = 0.0F;
      if (ind < this.pointsX.length - 1 && ind != 0) {
         double nx = BinaryRoutePlanner.squareRootDist(x31, y31, this.pointsX[ind + 1], this.pointsY[ind + 1]);
         double pr = BinaryRoutePlanner.squareRootDist(x31, y31, this.pointsX[ind - 1], this.pointsY[ind - 1]);
         int nind = nx > pr ? ind - 1 : ind + 1;
         QuadPoint proj = MapUtils.getProjectionPoint31(x31, y31, this.pointsX[ind], this.pointsY[ind], this.pointsX[nind], this.pointsX[nind]);
         distToPoint = (float)BinaryRoutePlanner.squareRootDist(x31, y31, (int)proj.x, (int)proj.y);
      }

      return distToPoint;
   }

   public int getIndex(int x31, int y31) {
      int ind = -1;
      this.cachedS.clear();
      this.quadTree.queryInBox(new QuadRect((double)(x31 - 16384), (double)(y31 - 16384), (double)(x31 + 16384), (double)(y31 + 16384)), this.cachedS);
      if (this.cachedS.size() == 0) {
         for(int k = 0; k < SHIFTS.length; ++k) {
            this.quadTree
               .queryInBox(
                  new QuadRect((double)(x31 - SHIFTS[k]), (double)(y31 - SHIFTS[k]), (double)(x31 + SHIFTS[k]), (double)(y31 + SHIFTS[k])), this.cachedS
               );
            if (this.cachedS.size() != 0) {
               break;
            }
         }

         if (this.cachedS.size() == 0) {
            return -1;
         }
      }

      double minDist = 0.0;

      for(int i = 0; i < this.cachedS.size(); ++i) {
         Integer n = this.cachedS.get(i);
         double ds = BinaryRoutePlanner.squareRootDist(x31, y31, this.pointsX[n], this.pointsY[n]);
         if (ds < minDist || i == 0) {
            ind = n;
            minDist = ds;
         }
      }

      return ind;
   }

   private long calc(int x31, int y31) {
      return (long)x31 << (int)(32L + (long)y31);
   }

   public void setFollowNext(boolean followNext) {
      this.followNext = followNext;
   }

   public boolean isFollowNext() {
      return this.followNext;
   }

   public PrecalculatedRouteDirection adopt(RoutingContext ctx) {
      int ind1 = this.getIndex(ctx.startX, ctx.startY);
      int ind2 = this.getIndex(ctx.targetX, ctx.targetY);
      this.minSpeed = ctx.getRouter().getDefaultSpeed();
      this.maxSpeed = ctx.getRouter().getMaxSpeed();
      if (ind1 == -1) {
         return null;
      } else if (ind2 == -1) {
         return null;
      } else {
         PrecalculatedRouteDirection routeDirection = new PrecalculatedRouteDirection(this, ind1, ind2);
         routeDirection.startPoint = this.calc(ctx.startX, ctx.startY);
         routeDirection.startFinishTime = (float)BinaryRoutePlanner.squareRootDist(this.pointsX[ind1], this.pointsY[ind1], ctx.startX, ctx.startY)
            / this.maxSpeed;
         routeDirection.endPoint = this.calc(ctx.targetX, ctx.targetY);
         routeDirection.endFinishTime = (float)BinaryRoutePlanner.squareRootDist(this.pointsX[ind2], this.pointsY[ind2], ctx.targetX, ctx.targetY)
            / this.maxSpeed;
         routeDirection.followNext = this.followNext;
         return routeDirection;
      }
   }
}
