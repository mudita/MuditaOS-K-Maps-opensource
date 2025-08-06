package com.mudita.map.common.enums;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.mudita.map.common.R;

public enum DistanceByTapTextSize {

	NORMAL(R.string.shared_string_normal, 0),
	LARGE(R.string.shared_string_large, 0);

	@StringRes
	private final int key;
	@DimenRes
	private final int textSizeId;

	DistanceByTapTextSize(@StringRes int key, @DimenRes int textSizeId) {
		this.key = key;
		this.textSizeId = textSizeId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(key);
	}

	@DimenRes
	public int getTextSizeId() {
		return textSizeId;
	}
}