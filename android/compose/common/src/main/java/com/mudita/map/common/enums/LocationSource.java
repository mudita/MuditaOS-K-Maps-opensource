package com.mudita.map.common.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.mudita.map.common.R;

public enum LocationSource {

	ANDROID_API(R.string.m);

	@StringRes
	public final int nameId;

	LocationSource(@StringRes int nameId) {
		this.nameId = nameId;
	}

	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(nameId);
	}
}
