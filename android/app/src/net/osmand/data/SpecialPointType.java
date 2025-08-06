package net.osmand.data;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.mudita.maps.R;

import net.osmand.plus.myplaces.FavoriteGroup;

public enum SpecialPointType {

	HOME("home", R.string.home_button, com.mudita.map.common.R.drawable.mx_mudita_default),
	WORK("work", R.string.work_button, com.mudita.map.common.R.drawable.mx_mudita_default),
	PARKING("parking", R.string.osmand_parking_position_name, com.mudita.map.common.R.drawable.mx_mudita_default);

	private final String typeName;
	@StringRes
	private final int resId;
	@DrawableRes
	private final int iconId;

	SpecialPointType(@NonNull String typeName, @StringRes int resId, @DrawableRes int iconId) {
		this.typeName = typeName;
		this.resId = resId;
		this.iconId = iconId;
	}

	public String getName() {
		return typeName;
	}

	public String getCategory() {
		return FavoriteGroup.PERSONAL_CATEGORY;
	}

	public int getIconId(@NonNull Context ctx) {
		return iconId;
	}

	public String getHumanString(@NonNull Context ctx) {
		return ctx.getString(resId);
	}
}
