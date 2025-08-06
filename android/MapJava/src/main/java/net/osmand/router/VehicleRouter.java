package net.osmand.router;

import java.util.Map;
import net.osmand.binary.RouteDataObject;

public interface VehicleRouter {
   boolean containsAttribute(String var1);

   String getAttribute(String var1);

   boolean acceptLine(RouteDataObject var1);

   int isOneWay(RouteDataObject var1);

   float getPenaltyTransition(RouteDataObject var1);

   float defineObstacle(RouteDataObject var1, int var2, boolean var3);

   double defineHeightObstacle(RouteDataObject var1, short var2, short var3);

   float defineRoutingObstacle(RouteDataObject var1, int var2, boolean var3);

   float defineRoutingSpeed(RouteDataObject var1);

   float defineVehicleSpeed(RouteDataObject var1);

   float defineSpeedPriority(RouteDataObject var1);

   float defineDestinationPriority(RouteDataObject var1);

   float getDefaultSpeed();

   float getMinSpeed();

   float getMaxSpeed();

   boolean restrictionsAware();

   boolean isArea(RouteDataObject var1);

   double calculateTurnTime(BinaryRoutePlanner.RouteSegment var1, int var2, BinaryRoutePlanner.RouteSegment var3, int var4);

   VehicleRouter build(Map<String, String> var1);
}
