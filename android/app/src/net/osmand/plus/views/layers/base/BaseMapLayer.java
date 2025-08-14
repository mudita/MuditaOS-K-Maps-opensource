package net.osmand.plus.views.layers.base;

import android.content.Context;

import androidx.annotation.NonNull;

public abstract class BaseMapLayer extends OsmandMapLayer {

	private int alpha = 255;
	protected int warningToSwitchMapShown;
	
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}
	
	public int getAlpha() {
		return alpha;
	}

	public BaseMapLayer(@NonNull Context ctx) {
		super(ctx);
	}
}
