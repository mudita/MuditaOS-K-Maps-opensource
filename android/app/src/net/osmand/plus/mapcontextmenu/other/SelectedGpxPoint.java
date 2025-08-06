package net.osmand.plus.mapcontextmenu.other;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.track.helpers.SelectedGpxFile;

public class SelectedGpxPoint {

	private final SelectedGpxFile selectedGpxFile;
	private final WptPt selectedPoint;
	private final WptPt prevPoint;
	private final WptPt nextPoint;
	private final float bearing;
	private final boolean showTrackPointMenu;

	public SelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt selectedPoint) {
		this(selectedGpxFile, selectedPoint, null, null, Float.NaN, false);
	}

	public SelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt selectedPoint, WptPt prevPoint,
							WptPt nextPoint, float bearing, boolean showTrackPointMenu) {
		this.prevPoint = prevPoint;
		this.nextPoint = nextPoint;
		this.selectedPoint = selectedPoint;
		this.selectedGpxFile = selectedGpxFile;
		this.bearing = bearing;
		this.showTrackPointMenu = showTrackPointMenu;
	}

	public SelectedGpxFile getSelectedGpxFile() {
		return selectedGpxFile;
	}

	public WptPt getSelectedPoint() {
		return selectedPoint;
	}

	public float getBearing() {
		return bearing;
	}

	public WptPt getPrevPoint() {
		return prevPoint;
	}

	public WptPt getNextPoint() {
		return nextPoint;
	}

	public boolean shouldShowTrackPointMenu() {
		return showTrackPointMenu;
	}
}