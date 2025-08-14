package net.osmand.plus.track.helpers;

import android.graphics.Bitmap;

import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.WptPt;

public class GpxDisplayItem {

	public GPXTrackAnalysis analysis;
	public GpxDisplayGroup group;

	public WptPt locationStart;
	public WptPt locationEnd;

	public double splitMetric = -1;
	public double secondarySplitMetric = -1;

	public String trackSegmentName;
	public String splitName;
	public String name;
	public String description;
	public String url;
	public Bitmap image;

	public boolean expanded;

	public WptPt locationOnMap;

	public boolean isGeneralTrack() {
		return group != null && group.isGeneralTrack();
	}
}
