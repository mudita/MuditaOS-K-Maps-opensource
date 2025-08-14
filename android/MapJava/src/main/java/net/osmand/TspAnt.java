package net.osmand;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

public class TspAnt {
   private double c = 1.0;
   private double alpha = 1.0;
   private double beta = 5.0;
   private double evaporation = 0.5;
   private double Q = 500.0;
   private double numAntFactor = 0.8;
   private double pr = 0.01;
   private int maxIterations = 2000;
   public int n = 0;
   public int m = 0;
   private double[][] graph = null;
   private double[][] trails = null;
   private TspAnt.Ant[] ants = null;
   private Random rand = new Random();
   private double[] probs = null;
   private int currentIndex = 0;
   public int[] bestTour;
   public double bestTourLength;

   public TspAnt readGraph(List<LatLon> intermediates, LatLon start, LatLon end) {
      boolean keepEndPoint = end != null;
      List<LatLon> l = new ArrayList<>();
      if (start != null) {
         l.add(start);
      }

      l.addAll(intermediates);
      if (keepEndPoint) {
         l.add(end);
      }

      this.n = l.size();
      this.graph = new double[this.n][this.n];
      double maxSum = 0.0;

      for(int i = 0; i < this.n; ++i) {
         double maxIWeight = 0.0;

         for(int j = 1; j < this.n; ++j) {
            double d = Math.rint(MapUtils.getDistance(l.get(i), l.get(j))) + 0.1;
            maxIWeight = Math.max(d, maxIWeight);
            this.graph[i][j] = d;
         }

         maxSum += maxIWeight;
      }

      maxSum = Math.rint(maxSum) + 1.0;

      for(int i = 0; i < this.n; ++i) {
         if (keepEndPoint && i == this.n - 1) {
            this.graph[i][0] = 0.1;
         } else {
            this.graph[i][0] = maxSum;
         }
      }

      this.m = (int)((double)this.n * this.numAntFactor);
      this.trails = new double[this.n][this.n];
      this.probs = new double[this.n];
      this.ants = new TspAnt.Ant[this.m];

      for(int j = 0; j < this.m; ++j) {
         this.ants[j] = new TspAnt.Ant();
      }

      return this;
   }

   public static double pow(double a, double b) {
      int x = (int)(Double.doubleToLongBits(a) >> 32);
      int y = (int)(b * (double)(x - 1072632447) + 1.072632447E9);
      return Double.longBitsToDouble((long)y << 32);
   }

   private void probTo(TspAnt.Ant ant) {
      int i = ant.tour[this.currentIndex];
      double denom = 0.0;

      for(int l = 0; l < this.n; ++l) {
         if (!ant.visited(l)) {
            denom += pow(this.trails[i][l], this.alpha) * pow(1.0 / this.graph[i][l], this.beta);
         }
      }

      for(int j = 0; j < this.n; ++j) {
         if (ant.visited(j)) {
            this.probs[j] = 0.0;
         } else {
            double numerator = pow(this.trails[i][j], this.alpha) * pow(1.0 / this.graph[i][j], this.beta);
            this.probs[j] = numerator / denom;
         }
      }
   }

   private int selectNextTown(TspAnt.Ant ant) {
      if (this.rand.nextDouble() < this.pr) {
         int t = this.rand.nextInt(this.n - this.currentIndex);
         int j = -1;

         for(int i = 0; i < this.n; ++i) {
            if (!ant.visited(i)) {
               ++j;
            }

            if (j == t) {
               return i;
            }
         }
      }

      this.probTo(ant);
      double r = this.rand.nextDouble();
      double tot = 0.0;

      for(int i = 0; i < this.n; ++i) {
         tot += this.probs[i];
         if (tot >= r) {
            return i;
         }
      }

      throw new RuntimeException("Not supposed to get here.");
   }

   private void updateTrails() {
      for(int i = 0; i < this.n; ++i) {
         for(int j = 0; j < this.n; ++j) {
            this.trails[i][j] *= this.evaporation;
         }
      }

      for(TspAnt.Ant a : this.ants) {
         double contribution = this.Q / a.tourLength();

         for(int i = 0; i < this.n - 1; ++i) {
            this.trails[a.tour[i]][a.tour[i + 1]] += contribution;
         }

         this.trails[a.tour[this.n - 1]][a.tour[0]] += contribution;
      }
   }

   private void moveAnts() {
      while(this.currentIndex < this.n - 1) {
         for(TspAnt.Ant a : this.ants) {
            a.visitTown(this.selectNextTown(a));
         }

         ++this.currentIndex;
      }
   }

   private void setupAnts() {
      this.currentIndex = -1;

      for(int i = 0; i < this.m; ++i) {
         this.ants[i].clear();
         this.ants[i].visitTown(this.rand.nextInt(this.n));
      }

      ++this.currentIndex;
   }

   private void updateBest() {
      if (this.bestTour == null) {
         this.bestTour = this.ants[0].tour;
         this.bestTourLength = this.ants[0].tourLength();
      }

      for(TspAnt.Ant a : this.ants) {
         if (a.tourLength() < this.bestTourLength) {
            this.bestTourLength = a.tourLength();
            this.bestTour = (int[])a.tour.clone();
         }
      }
   }

   public static String tourToString(int[] tour) {
      String t = "";

      for(int i : tour) {
         t = t + " " + i;
      }

      return t;
   }

   public int[] solve() {
      for(int i = 0; i < this.n; ++i) {
         for(int j = 0; j < this.n; ++j) {
            this.trails[i][j] = this.c;
         }
      }

      for(int iteration = 0; iteration < this.maxIterations; ++iteration) {
         this.setupAnts();
         this.moveAnts();
         this.updateTrails();
         this.updateBest();
      }

      System.out.println("Best tour length: " + (this.bestTourLength - (double)this.n * 0.1));
      System.out.println("Best tour:" + tourToString(this.bestTour));
      return alignAnswer((int[])this.bestTour.clone());
   }

   private static int[] alignAnswer(int[] ans) {
      int[] alignAns = new int[ans.length];
      int shift = 0;

      for(int j = 0; j < ans.length; ++j) {
         if (ans[j] == 0) {
            shift = j;
            break;
         }
      }

      for(int j = 0; j < ans.length; ++j) {
         alignAns[(j - shift + ans.length) % ans.length] = ans[j];
      }

      return alignAns;
   }

   public static void main(String[] args) {
      if (args.length >= 1) {
         TspAnt anttsp = new TspAnt();

         while(true) {
            anttsp.solve();
         }
      }

      System.err.println("Please specify a TSP data file.");
   }

   private class Ant {
      public int[] tour = new int[TspAnt.this.graph.length];
      public boolean[] visited = new boolean[TspAnt.this.graph.length];

      private Ant() {
      }

      public void visitTown(int town) {
         this.tour[TspAnt.this.currentIndex + 1] = town;
         this.visited[town] = true;
      }

      public boolean visited(int i) {
         return this.visited[i];
      }

      public double tourLength() {
         double length = TspAnt.this.graph[this.tour[TspAnt.this.n - 1]][this.tour[0]];

         for(int i = 0; i < TspAnt.this.n - 1; ++i) {
            length += TspAnt.this.graph[this.tour[i]][this.tour[i + 1]];
         }

         return length;
      }

      public void clear() {
         for(int i = 0; i < TspAnt.this.n; ++i) {
            this.visited[i] = false;
         }
      }
   }
}
