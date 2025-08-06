package net.osmand.plus.profiles;

import androidx.annotation.DrawableRes;

import com.mudita.maps.R;

public enum LocationIcon {
	DEFAULT(com.mudita.map.common.R.drawable.icon_car_marker, R.drawable.map_location_default_view_angle),
	CAR(com.mudita.map.common.R.drawable.icon_car_marker, R.drawable.map_location_car_view_angle),
	BICYCLE(com.mudita.map.common.R.drawable.icon_car_marker, R.drawable.map_location_bicycle_view_angle);

	LocationIcon(@DrawableRes int iconId, @DrawableRes int headingIconId) {
		this.iconId = iconId;
		this.headingIconId = headingIconId;
	}

	@DrawableRes
	private final int iconId;
	@DrawableRes
	private final int headingIconId;

	public int getIconId() {
		return iconId;
	}

	public int getHeadingIconId() {
		return headingIconId;
	}
}