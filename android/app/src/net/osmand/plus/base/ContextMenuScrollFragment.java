package net.osmand.plus.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mudita.maps.R;

import net.osmand.plus.LockableScrollView;
import net.osmand.plus.base.ContextMenuFragment.ContextMenuFragmentListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.MapLayers;

public abstract class ContextMenuScrollFragment extends ContextMenuFragment implements ContextMenuFragmentListener {

	public static final String TAG = ContextMenuScrollFragment.class.getSimpleName();

	@Nullable
	private View mapBottomHudButtons;

	public float getMiddleStateKoef() {
		return 0.5f;
	}

	public boolean isShowMapBottomHudButtons() {
		return true;
	}

	@Override
	public int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setListener(this);

			View bottomScrollView = getBottomScrollView();
			if (bottomScrollView instanceof LockableScrollView) {
				((LockableScrollView) bottomScrollView).setScrollingEnabled(true);
			}

			mapBottomHudButtons = view.findViewById(R.id.map_controls_container);
			if (mapBottomHudButtons != null) {
				if (isShowMapBottomHudButtons()) {
					setupControlButtons(mapBottomHudButtons);
				} else {
					AndroidUiHelper.updateVisibility(mapBottomHudButtons, false);
				}
			}
		}
		return view;
	}

	@Override
	public void onContextMenuYPosChanged(@NonNull ContextMenuFragment fragment, int y, boolean needMapAdjust, boolean animated) {
		updateMapControlsPos(fragment, y, animated);
	}

	@Override
	public void onContextMenuStateChanged(@NonNull ContextMenuFragment fragment, int menuState, int previousMenuState) {
		updateMapControlsVisibility(menuState);
	}

	@Override
	public void onContextMenuDismiss(@NonNull ContextMenuFragment fragment) {

	}


	@Nullable
	protected View getMapBottomHudButtons() {
		return mapBottomHudButtons;
	}

	protected void setupControlButtons(@NonNull View view) {
	}

	protected void setupMapRulerWidget(@NonNull View view, @NonNull MapLayers mapLayers) {}

	public void updateMapControlsPos(@NonNull ContextMenuFragment fragment, int y, boolean animated) {
		View mapControlsView = this.mapBottomHudButtons;
		if (mapControlsView != null) {
			int zoomY = y - getMapControlsHeight();
			if (animated) {
				fragment.animateView(mapControlsView, zoomY, null);
			} else {
				mapControlsView.setY(zoomY);
			}
		}
	}

	private int getMapControlsHeight() {
		View mapControlsContainer = this.mapBottomHudButtons;
		return mapControlsContainer != null ? mapControlsContainer.getHeight() : 0;
	}

	public boolean shouldShowMapControls(int menuState) {
		return menuState == MenuState.HEADER_ONLY;
	}

	private void updateMapControlsVisibility(int menuState) {
		if (mapBottomHudButtons != null) {
			if (shouldShowMapControls(menuState)) {
				if (mapBottomHudButtons.getVisibility() != View.VISIBLE) {
					mapBottomHudButtons.setVisibility(View.VISIBLE);
				}
			} else {
				if (mapBottomHudButtons.getVisibility() == View.VISIBLE) {
					mapBottomHudButtons.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
}