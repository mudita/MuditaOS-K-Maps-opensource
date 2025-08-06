package net.osmand.plus.views.layers;

import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;
import static net.osmand.router.RouteResultPreparation.SHIFT_ID;

import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MapSelectionHelper {
	private static final int CLOSEST_AMENITY_SEARCH_RADIUS = 2;
	public static final int SHIFT_MULTIPOLYGON_IDS = 43;
	public static final int SHIFT_NON_SPLIT_EXISTING_IDS = 41;
	public static final long RELATION_BIT = 1L << SHIFT_MULTIPOLYGON_IDS - 1; //According IndexPoiCreator SHIFT_MULTIPOLYGON_IDS
	public static final long SPLIT_BIT = 1L << SHIFT_NON_SPLIT_EXISTING_IDS - 1; //According IndexVectorMapCreator
	public static final int DUPLICATE_SPLIT = 5; //According IndexPoiCreator DUPLICATE_SPLIT

	public MapSelectionHelper() {}

	@Nullable
	public static Amenity findClosestAmenity(@NonNull ResourceManager resourceManager,
											 @NonNull LatLon latLon, RotatedTileBox tileBox) {
		SearchPoiTypeFilter filter = new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				switch (subcategory) {
					case "junction":
					case "bench":
					case "waste_basket":
					case "highway_crossing":
					case "fire_hydrant":
					case "tunnel":
						return false;
				}
				return true;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};

		Map<LatLon, Integer> displayedAmenities = resourceManager.getRenderer().getDisplayedAmenities();
		if (displayedAmenities.isEmpty()) return null;
		LatLon closestAmenityLatLon = Collections.min(
                displayedAmenities.keySet(),
                Comparator.comparingDouble(amenityLatLon -> MapUtils.getDistance(latLon, amenityLatLon))
        );
		PointF iconCenterPoint = getPixFromLatLon(tileBox, closestAmenityLatLon);
		PointF clickCenterPoint = getPixFromLatLon(tileBox, latLon);
		Integer iconRadius = displayedAmenities.get(closestAmenityLatLon);
		RectF iconBoundingBox = getIconBoundingBox(iconCenterPoint, iconRadius);

		if (!iconBoundingBox.contains(clickCenterPoint.x, clickCenterPoint.y)) return null;
		QuadRect rect = getAmenityLatLonBox(
				iconCenterPoint,
				(float) (CLOSEST_AMENITY_SEARCH_RADIUS * tileBox.getPixDensity()),
				tileBox
		);
		List<Amenity> amenities = resourceManager.searchAmenities(filter, rect);

		return amenities.isEmpty() ? null : Collections.min(amenities, Comparator.comparingDouble(a -> MapUtils.getDistance(latLon, a.getLocation())));
	}

	private static PointF getPixFromLatLon(RotatedTileBox tileBox, LatLon latLon) {
		return new PointF(
				tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude()),
				tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude())
		);
	}

	private static RectF getIconBoundingBox(PointF center, Integer radius) {
		return new RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius);
	}

	private static QuadRect getAmenityLatLonBox(PointF center, float radius, RotatedTileBox tileBox) {
		LatLon topLeft = tileBox.getLatLonFromPixel(center.x - radius, center.y - radius);
		LatLon bottomRight = tileBox.getLatLonFromPixel(center.x + radius, center.y + radius);
		return new QuadRect(topLeft.getLongitude(), topLeft.getLatitude(), bottomRight.getLongitude(), bottomRight.getLatitude());
	}

	@Nullable
	public static Amenity findAmenity(@NonNull OsmandApplication app, @NonNull LatLon latLon,
	                                  @Nullable List<String> names, long id, int radius) {
		id = getOsmId(id >> 1);
		SearchPoiTypeFilter filter = new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return true;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(filter, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		Amenity res = null;
		for (Amenity amenity : amenities) {
			Long initAmenityId = amenity.getId();
			if (initAmenityId != null) {
				long amenityId;
				if (isShiftedID(initAmenityId)) {
					amenityId = getOsmId(initAmenityId);
				} else {
					amenityId = initAmenityId >> AMENITY_ID_RIGHT_SHIFT;
				}
				if (amenityId == id && !amenity.isClosed()) {
					res = amenity;
					break;
				}
			}
		}
		if (res == null && !Algorithms.isEmpty(names)) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName()) && !amenity.isClosed()) {
						res = amenity;
						break;
					}
				}
				if (res != null) {
					break;
				}
			}
		}
		return res;
	}

	public static boolean isIdFromRelation(long id) {
		return id > 0 && (id & RELATION_BIT) == RELATION_BIT;
	}

	public static boolean isIdFromSplit(long id) {
		return id > 0 && (id & SPLIT_BIT) == SPLIT_BIT;
	}

	public static long getOsmId(long id) {
		//According methods assignIdForMultipolygon and genId in IndexPoiCreator
		long clearBits = RELATION_BIT | SPLIT_BIT;
		id = isShiftedID(id) ? (id & ~clearBits) >> DUPLICATE_SPLIT : id;
		return id >> SHIFT_ID;
	}

	public static boolean isShiftedID(long id) {
		return isIdFromRelation(id) || isIdFromSplit(id);
	}
}