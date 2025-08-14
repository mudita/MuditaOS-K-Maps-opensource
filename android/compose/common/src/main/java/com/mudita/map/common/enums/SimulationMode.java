package com.mudita.map.common.enums;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.mudita.map.common.R;

public enum SimulationMode {
	PREVIEW("preview_mode", R.string.simulation_preview_mode_title, R.string.simulation_preview_mode_desc, 0),
	CONSTANT("const_mode", R.string.simulation_constant_mode_title, R.string.simulation_constant_mode_desc, 0),
	REALISTIC("real_mode", R.string.simulation_real_mode_title, R.string.simulation_real_mode_desc, 0);

	String key;
	int title;
	int description;
	int layout;

	SimulationMode(String key, @StringRes int title, @StringRes int description, @LayoutRes int layout) {
		this.key = key;
		this.title = title;
		this.description = description;
		this.layout = layout;
	}

	@Nullable
	public static SimulationMode getMode(String key) {
		for (SimulationMode mode : values()) {
			if (mode.key.equals(key)) {
				return mode;
			}
		}
		return null;
	}

	public String getKey() {
		return key;
	}

	public int getTitle() {
		return title;
	}

	public int getDescription() {
		return description;
	}

	public int getLayout() {
		return layout;
	}
}
