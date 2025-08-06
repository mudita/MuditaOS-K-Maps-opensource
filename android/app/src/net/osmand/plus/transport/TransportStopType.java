package net.osmand.plus.transport;

import com.mudita.maps.R;

public enum TransportStopType {
	BUS(com.mudita.map.common.R.drawable.mx_route_bus_ref, com.mudita.map.common.R.drawable.mx_route_bus_ref, "routeBusColor", "route_bus_ref"),
	FERRY(com.mudita.map.common.R.drawable.mx_route_ferry_ref, com.mudita.map.common.R.drawable.mx_route_ferry_ref, "routeFerryColor", "route_ferry_ref"),
	FUNICULAR(com.mudita.map.common.R.drawable.mx_route_funicular_ref, com.mudita.map.common.R.drawable.mx_route_funicular_ref, "routeFunicularColor", "route_funicular_ref"),
	LIGHT_RAIL(com.mudita.map.common.R.drawable.mx_route_light_rail_ref, com.mudita.map.common.R.drawable.mx_route_light_rail_ref, "routeLightrailColor", "route_light_rail_ref"),
	MONORAIL(com.mudita.map.common.R.drawable.mx_route_monorail_ref, com.mudita.map.common.R.drawable.mx_route_monorail_ref, "routeLightrailColor", "route_monorail_ref"),
	RAILWAY(com.mudita.map.common.R.drawable.mx_route_railway_ref, com.mudita.map.common.R.drawable.mx_route_railway_ref, "routeTrainColor", "route_railway_ref"),
	SHARE_TAXI(com.mudita.map.common.R.drawable.mx_route_share_taxi_ref, com.mudita.map.common.R.drawable.mx_route_share_taxi_ref, "routeShareTaxiColor", "route_share_taxi_ref"),
	TRAIN(com.mudita.map.common.R.drawable.mx_railway_station, com.mudita.map.common.R.drawable.mx_railway_station, "routeTrainColor", "route_train_ref"),
	TRAM(com.mudita.map.common.R.drawable.mx_route_tram_ref, com.mudita.map.common.R.drawable.mx_railway_tram_stop, "routeTramColor", "route_tram_ref"),
	TROLLEYBUS(com.mudita.map.common.R.drawable.mx_route_trolleybus_ref, com.mudita.map.common.R.drawable.mx_route_trolleybus_ref, "routeTrolleybusColor", "route_trolleybus_ref"),
	SUBWAY(com.mudita.map.common.R.drawable.mx_subway_station, com.mudita.map.common.R.drawable.mx_subway_station, "routeTrainColor", "subway_station");

	final int resId;
	final int topResId;
	final String renderAttr;
	final String resName;

	TransportStopType(int resId, int topResId, String renderAttr, String resName) {
		this.resId = resId;
		this.topResId = topResId;
		this.renderAttr = renderAttr;
		this.resName = resName;
	}

	public int getResourceId() {
		return resId;
	}

	public int getTopResourceId() {
		return topResId;
	}

	public String getResName() {
		return resName;
	}

	public String getRendeAttr() {
		return renderAttr;
	}

	public boolean isTopType() {
		return this == TRAM || this == SUBWAY;
	}

	public static TransportStopType findType(String typeName) {
		String tName = typeName.toUpperCase();
		for (TransportStopType t : values()) {
			if (t.name().equals(tName)) {
				return t;
			}
		}
		return null;
	}

}
