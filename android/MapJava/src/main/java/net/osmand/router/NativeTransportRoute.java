package net.osmand.router;

public class NativeTransportRoute {
   public long id = -1L;
   public double routeLat = -1.0;
   public double routeLon = -1.0;
   public String name = "";
   public String enName = "";
   public String[] namesLng;
   public String[] namesNames;
   public int fileOffset;
   public NativeTransportStop[] forwardStops;
   public String ref = "";
   public String routeOperator = "";
   public String type = "";
   public int dist = -1;
   public String color = "";
   public int[] intervals;
   public int[] avgStopIntervals;
   public int[] avgWaitIntervals;
   public long[] waysIds;
   public long[][] waysNodesIds;
   public double[][] waysNodesLats;
   public double[][] waysNodesLons;
}
