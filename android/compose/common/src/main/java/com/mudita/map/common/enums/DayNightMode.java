package com.mudita.map.common.enums;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.mudita.map.common.R;

public enum DayNightMode {
	AUTO(R.string.daynight_mode_auto, R.drawable.icon_search),
	DAY(R.string.daynight_mode_day, R.drawable.icon_search),
	NIGHT(R.string.daynight_mode_night, R.drawable.icon_search),
	SENSOR(R.string.daynight_mode_sensor, R.drawable.icon_search);

	private final int key;
	@DrawableRes
	private final int drawableRes;

	DayNightMode(@StringRes int key, @DrawableRes int drawableRes) {
		this.key = key;
		this.drawableRes = drawableRes;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	@DrawableRes
	public int getIconRes() {
		return drawableRes;
	}

	public boolean isSensor() {
		return this == SENSOR;
	}

	public boolean isAuto() {
		return this == AUTO;
	}

	public boolean isDay() {
		return this == DAY;
	}

	public boolean isNight() {
		return this == NIGHT;
	}

	public static DayNightMode[] possibleValues(Context context) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		boolean isLightSensorEnabled = mLight != null;
		if (isLightSensorEnabled) {
			return values();
		} else {
			return new DayNightMode[]{AUTO, DAY, NIGHT};
		}
	}
}