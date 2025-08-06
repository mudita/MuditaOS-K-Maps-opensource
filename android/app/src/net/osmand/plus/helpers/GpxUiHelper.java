package net.osmand.plus.helpers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.binary.RouteDataObject.HEIGHT_UNDEFINED;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.track.GpxAppearanceAdapter.SHOW_START_FINISH_ATTR;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.mudita.maps.R;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.ActivityResultListener.OnActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.CtxMenuUtils;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

public class GpxUiHelper {

	private static final int OPEN_GPX_DOCUMENT_REQUEST = 1005;

	public static final long SECOND_IN_MILLIS = 1000L;


	public static String getColorValue(String clr, String value, boolean html) {
		if (!html) {
			return value;
		}
		return "<font color=\"" + clr + "\">" + value + "</font>";
	}

	public static String getColorValue(String clr, String value) {
		return getColorValue(clr, value, true);
	}

	public static String getDescription(OsmandApplication app, GPXTrackAnalysis analysis, boolean html) {
		StringBuilder description = new StringBuilder();
		String nl = html ? "<br/>" : "\n";
		String timeSpanClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_time_span_color));
		String distanceClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_distance_color));
		String speedClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_speed));
		String ascClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_asc));
		String descClr = Algorithms.colorToString(ContextCompat.getColor(app, R.color.gpx_altitude_desc));
		// OUTPUT:
		// 1. Total distance, Start time, End time
		if (analysis.totalTracks > 1) {
			description.append(nl).append(app.getString(R.string.gpx_info_subtracks, getColorValue(speedClr, analysis.totalTracks + "", html)));
		}
		if (analysis.wptPoints > 0) {
			description.append(nl).append(app.getString(R.string.gpx_info_waypoints, getColorValue(speedClr, analysis.wptPoints + "", html)));
		}
		if (analysis.isTimeSpecified()) {
			description.append(nl).append(app.getString(R.string.gpx_info_start_time, analysis.startTime));
			description.append(nl).append(app.getString(R.string.gpx_info_end_time, analysis.endTime));
		}

		// 2. Time span
		if (analysis.timeSpan > 0 && analysis.timeSpan != analysis.timeMoving) {
			String formatDuration = Algorithms.formatDuration((int) (analysis.timeSpan / 1000.0f + 0.5), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timespan,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 3. Time moving, if any
		if (analysis.isTimeMoving()) {
				//Next few lines for Issue 3222 heuristic testing only
				//final String formatDuration0 = Algorithms.formatDuration((int) (analysis.timeMoving0 / 1000.0f + 0.5), app.accessibilityEnabled());
				//description.append(nl).append(app.getString(R.string.gpx_timemoving,
				//		getColorValue(timeSpanClr, formatDuration0, html)));
				//description.append(" (" + getColorValue(distanceClr, OsmAndFormatter.getFormattedDistance(analysis.totalDistanceMoving0, app), html) + ")");
			String formatDuration = Algorithms.formatDuration((int) (analysis.timeMoving / 1000.0f + 0.5), app.accessibilityEnabled());
			description.append(nl).append(app.getString(R.string.gpx_timemoving,
					getColorValue(timeSpanClr, formatDuration, html)));
		}

		// 4. Elevation, eleUp, eleDown, if recorded
		if (analysis.isElevationSpecified()) {
			description.append(nl);
		}


		if (analysis.isSpeedSpecified()) {
		}
		return description.toString();
	}

	public static AlertDialog selectGPXFiles(List<String> selectedGpxList, Activity activity,
	                                         CallbackWithObject<GPXFile[]> callbackWithObject,
	                                         int dialogThemeRes, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		List<GPXInfo> orderedAllGpxList = listGpxInfo(app, selectedGpxList, false);
		if (orderedAllGpxList.size() < 2) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		ContextMenuAdapter adapter = createGpxContextMenuAdapter(app, orderedAllGpxList, true);
		return createDialog(activity, true, true, true, callbackWithObject, orderedAllGpxList, adapter, dialogThemeRes, nightMode);
	}

	private static List<GPXInfo> listGpxInfo(OsmandApplication app, List<String> selectedGpxFiles, boolean absolutePath) {
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> allGpxList = getSortedGPXFilesInfo(gpxDir, selectedGpxFiles, absolutePath);
		GPXInfo currentTrack = new GPXInfo(app.getString(R.string.show_current_gpx_title), 0, 0);
		currentTrack.setSelected(selectedGpxFiles.contains(""));
		allGpxList.add(0, currentTrack);
		return allGpxList;
	}

	public static AlertDialog selectGPXFile(Activity activity, boolean showCurrentGpx,
	                                        boolean multipleChoice,
	                                        CallbackWithObject<GPXFile[]> callbackWithObject,
	                                        boolean nightMode) {
		int dialogThemeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> list = getSortedGPXFilesInfo(dir, null, false);
		if (list.isEmpty()) {
			Toast.makeText(activity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
		}
		if (!list.isEmpty() || showCurrentGpx) {
			if (showCurrentGpx) {
				list.add(0, new GPXInfo(activity.getString(R.string.show_current_gpx_title), 0, 0));
			}

			ContextMenuAdapter adapter = createGpxContextMenuAdapter(app, list, false);
			return createDialog(activity, showCurrentGpx, multipleChoice, false, callbackWithObject, list, adapter, dialogThemeRes, nightMode);
		}
		return null;
	}

	private static ContextMenuAdapter createGpxContextMenuAdapter(OsmandApplication app,
	                                                              List<GPXInfo> allGpxList,
	                                                              boolean needSelectItems) {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		fillGpxContextMenuAdapter(adapter, allGpxList, needSelectItems);
		return adapter;
	}

	private static void fillGpxContextMenuAdapter(ContextMenuAdapter adapter, List<GPXInfo> allGpxFiles,
	                                              boolean needSelectItems) {
		for (GPXInfo gpxInfo : allGpxFiles) {
			adapter.addItem(new ContextMenuItem(null)
					.setTitle(getGpxTitle(gpxInfo.getFileName()))
					.setSelected(needSelectItems && gpxInfo.selected)
					.setIcon(R.drawable.ic_action_polygom_dark));
		}
	}

	@NonNull
	public static String getGpxTitle(@Nullable String name) {
		return name != null ? Algorithms.getFileNameWithoutExtension(name) : "";
	}

	@NonNull
	public static String getGpxDirTitle(@Nullable String name) {
		if (Algorithms.isEmpty(name)) {
			return "";
		}
		return Algorithms.capitalizeFirstLetter(Algorithms.getFileNameWithoutExtension(name));
	}

	private static class DialogGpxDataItemCallback implements GpxDataItemCallback {

		private static final int UPDATE_GPX_ITEM_MSG_ID = OsmAndConstants.UI_HANDLER_LOCATION_SERVICE + 6;
		private static final long MIN_UPDATE_INTERVAL = 500;

		private final OsmandApplication app;
		private long lastUpdateTime;
		private boolean updateEnable = true;
		private ArrayAdapter<String> listAdapter;

		DialogGpxDataItemCallback(OsmandApplication app) {
			this.app = app;
		}

		public boolean isUpdateEnable() {
			return updateEnable;
		}

		public void setUpdateEnable(boolean updateEnable) {
			this.updateEnable = updateEnable;
		}

		public ArrayAdapter<String> getListAdapter() {
			return listAdapter;
		}

		public void setListAdapter(ArrayAdapter<String> listAdapter) {
			this.listAdapter = listAdapter;
		}

		private final Runnable updateItemsProc = new Runnable() {
			@Override
			public void run() {
				if (updateEnable) {
					lastUpdateTime = System.currentTimeMillis();
					listAdapter.notifyDataSetChanged();
				}
			}
		};

		@Override
		public boolean isCancelled() {
			return !updateEnable;
		}

		@Override
		public void onGpxDataItemReady(GpxDataItem item) {
			if (System.currentTimeMillis() - lastUpdateTime > MIN_UPDATE_INTERVAL) {
				updateItemsProc.run();
			}
			app.runMessageInUIThreadAndCancelPrevious(UPDATE_GPX_ITEM_MSG_ID, updateItemsProc, MIN_UPDATE_INTERVAL);
		}
	}

	private static AlertDialog createDialog(Activity activity,
	                                        boolean showCurrentGpx,
	                                        boolean multipleChoice,
	                                        boolean showAppearanceSetting,
	                                        CallbackWithObject<GPXFile[]> callbackWithObject,
	                                        List<GPXInfo> gpxInfoList,
	                                        ContextMenuAdapter adapter,
	                                        int themeRes,
	                                        boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
		final int layout = R.layout.gpx_track_item;
		Map<String, String> gpxAppearanceParams = new HashMap<>();
		DialogGpxDataItemCallback gpxDataItemCallback = new DialogGpxDataItemCallback(app);

		List<String> modifiableGpxFileNames = CtxMenuUtils.getNames(adapter.getItems());
		ArrayAdapter<String> alertDialogAdapter = new ArrayAdapter<String>(activity, layout, R.id.title, modifiableGpxFileNames) {

			@Override
			public int getItemViewType(int position) {
				return showCurrentGpx && position == 0 ? 1 : 0;
			}

			@Override
			public int getViewTypeCount() {
				return 2;
			}

			private GpxDataItem getDataItem(GPXInfo info) {
				return app.getGpxDbHelper().getItem(
						new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()),
						gpxDataItemCallback);
			}

			@Override
			@NonNull
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				boolean checkLayout = getItemViewType(position) == 0;
				if (v == null) {
					v = View.inflate(new ContextThemeWrapper(activity, themeRes), layout, null);
				}

				ContextMenuItem item = adapter.getItem(position);
				GPXInfo info = gpxInfoList.get(position);
				boolean currentlyRecordingTrack = showCurrentGpx && position == 0;

				GPXTrackAnalysis analysis = null;
				if (currentlyRecordingTrack) {
					analysis = app.getSavingTrackHelper().getCurrentTrack().getTrackAnalysis(app);
				} else {
					GpxDataItem dataItem = getDataItem(info);
					if (dataItem != null) {
						analysis = dataItem.getAnalysis();
					}
				}
				updateGpxInfoView(v, item.getTitle(), info, analysis, app);

				if (item.getSelected() == null) {
					v.findViewById(R.id.check_item).setVisibility(View.GONE);
					v.findViewById(R.id.check_local_index).setVisibility(View.GONE);
				} else {
					if (checkLayout) {
						CheckBox ch = v.findViewById(R.id.check_local_index);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener((buttonView, isChecked) -> {
							item.setSelected(isChecked);
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					} else {
						SwitchCompat ch = v.findViewById(R.id.toggle_item);
						ch.setVisibility(View.VISIBLE);
						v.findViewById(R.id.toggle_checkbox_item).setVisibility(View.GONE);
						ch.setOnCheckedChangeListener(null);
						ch.setChecked(item.getSelected());
						ch.setOnCheckedChangeListener((buttonView, isChecked) -> {
							item.setSelected(isChecked);
						});
						UiUtilities.setupCompoundButton(ch, nightMode, PROFILE_DEPENDENT);
					}
					v.findViewById(R.id.check_item).setVisibility(View.VISIBLE);
				}
				return v;
			}
		};

		OnClickListener onClickListener = (dialog, position) -> { };
		gpxDataItemCallback.setListAdapter(alertDialogAdapter);
		builder.setAdapter(alertDialogAdapter, onClickListener);
		if (multipleChoice) {
			if (showAppearanceSetting) {
				RenderingRuleProperty trackWidthProp;
				RenderingRuleProperty trackColorProp;
				RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (renderer != null) {
					trackWidthProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
					trackColorProp = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
				} else {
					trackWidthProp = null;
					trackColorProp = null;
				}
				if (trackWidthProp == null || trackColorProp == null) {
					builder.setTitle(R.string.show_gpx);
				} else {
					View apprTitleView = View.inflate(new ContextThemeWrapper(activity, themeRes), R.layout.select_gpx_appearance_title, null);

					CommonPreference<String> prefWidth
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR);
					CommonPreference<String> prefColor
							= app.getSettings().getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);

					updateAppearanceTitle(activity, app, trackWidthProp, renderer, apprTitleView, prefWidth.get(), prefColor.get());

					apprTitleView.setOnClickListener(v -> {
						ListPopupWindow popup = new ListPopupWindow(new ContextThemeWrapper(activity, themeRes));
						popup.setAnchorView(apprTitleView);
						popup.setContentWidth(AndroidUtils.dpToPx(activity, 200f));
						popup.setModal(true);
						popup.setDropDownGravity(Gravity.END | Gravity.TOP);
						popup.setVerticalOffset(AndroidUtils.dpToPx(activity, -48f));
						popup.setHorizontalOffset(AndroidUtils.dpToPx(activity, -6f));
						boolean showStartFinish = Objects.equals(gpxAppearanceParams.get(SHOW_START_FINISH_ATTR), "true");
						GpxAppearanceAdapter gpxApprAdapter = new GpxAppearanceAdapter(new ContextThemeWrapper(activity, themeRes),
								gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get(),
								GpxAppearanceAdapter.GpxAppearanceAdapterType.TRACK_WIDTH_COLOR, showStartFinish, nightMode);
						popup.setAdapter(gpxApprAdapter);
						popup.setOnItemClickListener((parent, view, position, id) -> {
							AppearanceListItem item = gpxApprAdapter.getItem(position);
							if (item != null) {
								if (CURRENT_TRACK_WIDTH_ATTR.equals(item.getAttrName())) {
									gpxAppearanceParams.put(CURRENT_TRACK_WIDTH_ATTR, item.getValue());
								} else if (CURRENT_TRACK_COLOR_ATTR.equals(item.getAttrName())) {
									gpxAppearanceParams.put(CURRENT_TRACK_COLOR_ATTR, item.getValue());
								} else if (SHOW_START_FINISH_ATTR.equals(item.getAttrName())) {
									gpxAppearanceParams.put(SHOW_START_FINISH_ATTR, item.getValue());
								}
							}
							popup.dismiss();
							updateAppearanceTitle(activity, app, trackWidthProp, renderer,
									apprTitleView,
									gpxAppearanceParams.containsKey(CURRENT_TRACK_WIDTH_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_WIDTH_ATTR) : prefWidth.get(),
									gpxAppearanceParams.containsKey(CURRENT_TRACK_COLOR_ATTR) ? gpxAppearanceParams.get(CURRENT_TRACK_COLOR_ATTR) : prefColor.get());
						});
						popup.show();
					});
					builder.setCustomTitle(apprTitleView);
				}
			} else {
				builder.setTitle(R.string.show_gpx);
			}
			builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
				if (gpxAppearanceParams.size() > 0) {
					for (Map.Entry<String, String> entry : gpxAppearanceParams.entrySet()) {
						if (SHOW_START_FINISH_ATTR.equals(entry.getKey())) {
							boolean showStartFinish = Objects.equals(entry.getValue(), "true");
							app.getSettings().CURRENT_TRACK_SHOW_START_FINISH.set(showStartFinish);
						} else {
							CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(entry.getKey());
							pref.set(entry.getValue());
						}
					}
					if (activity instanceof MapActivity) {
						((MapActivity) activity).refreshMapComplete();
					}
				}
				GPXFile currentGPX = null;
				//clear all previously selected files before adding new one
				if (app.getSelectedGpxHelper() != null) {
					app.getSelectedGpxHelper().clearAllGpxFilesToShow(false);
				}
				if (showCurrentGpx && adapter.getItem(0).getSelected()) {
					currentGPX = app.getSavingTrackHelper().getCurrentGpx();
				}
				List<String> selectedGpxNames = new ArrayList<>();
				for (int i = (showCurrentGpx ? 1 : 0); i < adapter.length(); i++) {
					if (adapter.getItem(i).getSelected()) {
						selectedGpxNames.add(gpxInfoList.get(i).getFileName());
					}
				}
				dialog.dismiss();
				updateSelectedTracksAppearance(app, selectedGpxNames, gpxAppearanceParams);
				loadGPXFileInDifferentThread(activity, callbackWithObject, dir, currentGPX,
						selectedGpxNames.toArray(new String[0]));
			});
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			if (gpxInfoList.size() > 1 || !showCurrentGpx && gpxInfoList.size() > 0) {
				builder.setNeutralButton(R.string.gpx_add_track, null);
			}
		}

		AlertDialog dlg = builder.create();
		dlg.setCanceledOnTouchOutside(true);
		if (gpxInfoList.size() == 0 || showCurrentGpx && gpxInfoList.size() == 1) {
			View footerView = activity.getLayoutInflater().inflate(R.layout.no_gpx_files_list_footer, null);
			TextView descTextView = footerView.findViewById(R.id.descFolder);
			String descPrefix = app.getString(R.string.gpx_no_tracks_title_folder);
			SpannableString spannableDesc = new SpannableString(descPrefix + ": " + dir.getAbsolutePath());
			spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
					descPrefix.length() + 1, spannableDesc.length(), 0);
			descTextView.setText(spannableDesc);
			footerView.findViewById(R.id.button).setOnClickListener(v -> {
				addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
			});
			dlg.getListView().addFooterView(footerView, null, false);
		}
		dlg.getListView().setOnItemClickListener((parent, view, position, id) -> {
			if (multipleChoice) {
				ContextMenuItem item = adapter.getItem(position);
				item.setSelected(!item.getSelected());
				alertDialogAdapter.notifyDataSetInvalidated();
			} else {
				dlg.dismiss();
				if (showCurrentGpx && position == 0) {
					callbackWithObject.processResult(null);
				} else {
					String fileName = gpxInfoList.get(position).getFileName();
					SelectedGpxFile selectedGpxFile =
							app.getSelectedGpxHelper().getSelectedFileByName(fileName);
					if (selectedGpxFile != null) {
						callbackWithObject.processResult(new GPXFile[] {selectedGpxFile.getGpxFile()});
					} else {
						loadGPXFileInDifferentThread(activity, callbackWithObject, dir, null, fileName);
					}
				}
			}
		});
		dlg.setOnShowListener(dialog -> {
			Button addTrackButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
			if (addTrackButton != null) {
				addTrackButton.setOnClickListener(v -> {
					addTrack(activity, alertDialogAdapter, adapter, gpxInfoList);
				});
			}
		});
		dlg.setOnDismissListener(dialog -> gpxDataItemCallback.setUpdateEnable(false));
		dlg.show();
		try {
			dlg.getListView().setFastScrollEnabled(true);
		} catch (Exception e) {
			// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
			// Unknown reason but on some devices fail
		}
		return dlg;
	}

	private static void updateSelectedTracksAppearance(OsmandApplication app, List<String> fileNames, Map<String, String> params) {
		GpxDataItemCallback callback = new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				updateTrackAppearance(app, item, params);
			}
		};
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		for (String name : fileNames) {
			GpxDataItem item = gpxDbHelper.getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), name), callback);
			if (item != null) {
				updateTrackAppearance(app, item, params);
			}
		}
	}

	private static void updateTrackAppearance(OsmandApplication app, GpxDataItem item, Map<String, String> params) {
		OsmandSettings settings = app.getSettings();
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		if (params.containsKey(CURRENT_TRACK_COLOR_ATTR)) {
			String savedColor = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).get();
			int color = GpxAppearanceAdapter.parseTrackColor(app.getRendererRegistry().getCurrentSelectedRenderer(), savedColor);
			gpxDbHelper.updateColor(item, color);
		}
		if (params.containsKey(CURRENT_TRACK_WIDTH_ATTR)) {
			String width = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).get();
			gpxDbHelper.updateWidth(item, width);
		}
		if (params.containsKey(SHOW_START_FINISH_ATTR)) {
			boolean showStartFinish = settings.CURRENT_TRACK_SHOW_START_FINISH.get();
			gpxDbHelper.updateShowStartFinish(item, showStartFinish);
		}
	}

	public static void updateGpxInfoView(@NonNull OsmandApplication app,
	                                     @NonNull View v,
	                                     @NonNull String itemTitle,
	                                     @Nullable Drawable iconDrawable,
	                                     @NonNull GPXInfo info) {
		GpxDataItem item = getDataItem(app, info, new GpxDataItemCallback() {
			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public void onGpxDataItemReady(GpxDataItem item) {
				updateGpxInfoView(app, v, itemTitle, iconDrawable, info, item);
			}
		});
		if (item != null) {
			updateGpxInfoView(app, v, itemTitle, iconDrawable, info, item);
		}
	}

	private static void updateGpxInfoView(@NonNull OsmandApplication app,
	                                      @NonNull View v,
	                                      @NonNull String itemTitle,
	                                      @Nullable Drawable iconDrawable,
	                                      @NonNull GPXInfo info,
	                                      @NonNull GpxDataItem dataItem) {
		updateGpxInfoView(v, itemTitle, info, dataItem.getAnalysis(), app);
		if (iconDrawable != null) {
			ImageView icon = v.findViewById(R.id.icon);
			icon.setImageDrawable(iconDrawable);
			icon.setVisibility(View.VISIBLE);
		}
	}

	public static void updateGpxInfoView(View v,
	                                     String itemTitle,
	                                     GPXInfo info,
	                                     GPXTrackAnalysis analysis,
	                                     OsmandApplication app) {
		TextView viewName = v.findViewById(R.id.name);
		viewName.setText(itemTitle.replace("/", " • ").trim());
		viewName.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
		ImageView icon = v.findViewById(R.id.icon);
		icon.setVisibility(View.GONE);
		//icon.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_polygom_dark));

		boolean sectionRead = analysis == null;
		if (sectionRead) {
			v.findViewById(R.id.read_section).setVisibility(View.GONE);
			v.findViewById(R.id.unknown_section).setVisibility(View.VISIBLE);
			String date = "";
			String size = "";
			if (info.getFileSize() >= 0) {
				size = AndroidUtils.formatSize(v.getContext(), info.getFileSize());
			}
			DateFormat df = app.getResourceManager().getDateFormat();
			long fd = info.getLastModified();
			if (fd > 0) {
				date = (df.format(new Date(fd)));
			}
			TextView sizeText = v.findViewById(R.id.date_and_size_details);
			sizeText.setText(date + " \u2022 " + size);

		} else {
			v.findViewById(R.id.read_section).setVisibility(View.VISIBLE);
			v.findViewById(R.id.unknown_section).setVisibility(View.GONE);
			ImageView distanceI = v.findViewById(R.id.distance_icon);
			distanceI.setVisibility(View.VISIBLE);
			distanceI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_distance_16));
			ImageView pointsI = v.findViewById(R.id.points_icon);
			pointsI.setVisibility(View.VISIBLE);
			pointsI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_waypoint_16));
			ImageView timeI = v.findViewById(R.id.time_icon);
			timeI.setVisibility(View.VISIBLE);
			timeI.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_time_16));
			TextView time = v.findViewById(R.id.time);
			TextView distance = v.findViewById(R.id.distance);
			TextView pointsCount = v.findViewById(R.id.points_count);
			pointsCount.setText(analysis.wptPoints + "");

			if (analysis.isTimeSpecified()) {
				time.setText(Algorithms.formatDuration((int) (analysis.timeSpan / 1000.0f + 0.5), app.accessibilityEnabled()) + "");
			} else {
				time.setText("");
			}
		}

		TextView descr = v.findViewById(R.id.description);
		if (descr != null) {
			descr.setVisibility(View.GONE);
		}

		View checkbox = v.findViewById(R.id.check_item);
		if (checkbox != null) {
			checkbox.setVisibility(View.GONE);
		}
	}

	private static GpxDataItem getDataItem(@NonNull OsmandApplication app,
	                                       @NonNull GPXInfo info,
	                                       @Nullable GpxDataItemCallback callback) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		String fileName = info.getFileName();
		File file = new File(dir, fileName);
		return app.getGpxDbHelper().getItem(file, callback);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static void addTrack(Activity activity, ArrayAdapter<String> listAdapter,
	                             ContextMenuAdapter contextMenuAdapter, List<GPXInfo> allGpxFiles) {
		if (activity instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) activity;
			OnActivityResultListener listener = (resultCode, resultData) -> {
				if (resultCode != Activity.RESULT_OK || resultData == null) {
					return;
				}

				ImportHelper importHelper = mapActivity.getImportHelper();
				importHelper.setGpxImportCompleteListener(new ImportHelper.OnGpxImportCompleteListener() {
					@Override
					public void onImportComplete(boolean success) {
					}

					@Override
					public void onSaveComplete(boolean success, GPXFile result) {
						if (success) {
							OsmandApplication app = (OsmandApplication) activity.getApplication();
							GpxSelectionParams params = GpxSelectionParams.newInstance()
									.showOnMap().syncGroup().selectedByUser().addToMarkers()
									.addToHistory().saveSelection();
							app.getSelectedGpxHelper().selectGpxFile(result, params);
							updateGpxDialogAfterImport(activity, listAdapter, contextMenuAdapter, allGpxFiles, result.path);
						}
					}
				});

				Uri uri = resultData.getData();
				importHelper.handleGpxImport(uri, null, false);
			};

			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.setType("*/*");
			try {
				mapActivity.startActivityForResult(intent, OPEN_GPX_DOCUMENT_REQUEST);
				mapActivity.registerActivityResultListener(new ActivityResultListener(OPEN_GPX_DOCUMENT_REQUEST, listener));
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mapActivity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			}
		}
	}

	private static void updateGpxDialogAfterImport(Activity activity,
	                                               ArrayAdapter<String> dialogAdapter,
	                                               ContextMenuAdapter adapter, List<GPXInfo> allGpxFiles,
	                                               String importedGpx) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();

		List<String> selectedGpxFiles = new ArrayList<>();
		selectedGpxFiles.add(importedGpx);
		for (int i = 0; i < allGpxFiles.size(); i++) {
			GPXInfo gpxInfo = allGpxFiles.get(i);
			ContextMenuItem menuItem = adapter.getItem(i);
			if (menuItem.getSelected()) {
				boolean isCurrentTrack =
						gpxInfo.getFileName().equals(app.getString(R.string.show_current_gpx_title));
				selectedGpxFiles.add(isCurrentTrack ? "" : gpxInfo.getFileName());
			}
		}
		allGpxFiles.clear();
		allGpxFiles.addAll(listGpxInfo(app, selectedGpxFiles, false));
		adapter.clear();
		fillGpxContextMenuAdapter(adapter, allGpxFiles, true);
		dialogAdapter.clear();
		dialogAdapter.addAll(CtxMenuUtils.getNames(adapter.getItems()));
		dialogAdapter.notifyDataSetInvalidated();
	}

	private static void updateAppearanceTitle(Activity activity, OsmandApplication app,
											  RenderingRuleProperty trackWidthProp,
											  RenderingRulesStorage renderer,
											  View apprTitleView,
											  String prefWidthValue,
											  String prefColorValue) {
		TextView widthTextView = apprTitleView.findViewById(R.id.widthTitle);
		ImageView colorImageView = apprTitleView.findViewById(R.id.colorImage);
		if (!Algorithms.isEmpty(prefWidthValue)) {
			widthTextView.setText(AndroidUtils.getRenderingStringPropertyValue(activity, prefWidthValue));
		}
		int color = GpxAppearanceAdapter.parseTrackColor(renderer, prefColorValue);
		if (color == -1) {
			colorImageView.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle));
		} else {
			colorImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_circle, color));
		}
	}

	@NonNull
	public static List<String> getSelectedTrackPaths(OsmandApplication app) {
		List<String> trackNames = new ArrayList<>();
		for (SelectedGpxFile file : app.getSelectedGpxHelper().getSelectedGPXFiles()) {
			trackNames.add(file.getGpxFile().path);
		}
		return trackNames;
	}

	public static List<GPXInfo> getSortedGPXFilesInfoByDate(File dir, boolean absolutePath) {
		List<GPXInfo> list = new ArrayList<>();
		readGpxDirectory(dir, list, "", absolutePath);
		Collections.sort(list, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo object1, GPXInfo object2) {
				long lhs = object1.getLastModified();
				long rhs = object2.getLastModified();
				return lhs < rhs ? 1 : (lhs == rhs ? 0 : -1);
			}
		});
		return list;
	}

	@Nullable
	public static GPXInfo getGpxInfoByFileName(@NonNull OsmandApplication app, @NonNull String fileName) {
		File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		File file = new File(dir, fileName);
		if (file.exists() && file.getName().endsWith(GPX_FILE_EXT)) {
			return new GPXInfo(fileName, file.lastModified(), file.length());
		}
		return null;
	}

	@NonNull
	public static List<GPXInfo> getSortedGPXFilesInfo(File dir, List<String> selectedGpxList, boolean absolutePath) {
		List<GPXInfo> allGpxFiles = new ArrayList<>();
		readGpxDirectory(dir, allGpxFiles, "", absolutePath);
		if (selectedGpxList != null) {
			for (GPXInfo gpxInfo : allGpxFiles) {
				for (String selectedGpxName : selectedGpxList) {
					if (selectedGpxName.endsWith(gpxInfo.getFileName())) {
						gpxInfo.setSelected(true);
						break;
					}
				}
			}
		}
		Collections.sort(allGpxFiles, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				int res = i1.isSelected() == i2.isSelected() ? 0 : i1.isSelected() ? -1 : 1;
				if (res != 0) {
					return res;
				}

				String name1 = i1.getFileName();
				String name2 = i2.getFileName();
				int d1 = depth(name1);
				int d2 = depth(name2);
				if (d1 != d2) {
					return d1 - d2;
				}
				int lastSame = 0;
				for (int i = 0; i < name1.length() && i < name2.length(); i++) {
					if (name1.charAt(i) != name2.charAt(i)) {
						break;
					}
					if (name1.charAt(i) == '/') {
						lastSame = i + 1;
					}
				}

				boolean isDigitStarts1 = isLastSameStartsWithDigit(name1, lastSame);
				boolean isDigitStarts2 = isLastSameStartsWithDigit(name2, lastSame);
				res = isDigitStarts1 == isDigitStarts2 ? 0 : isDigitStarts1 ? -1 : 1;
				if (res != 0) {
					return res;
				}
				if (isDigitStarts1) {
					return -name1.compareToIgnoreCase(name2);
				}
				return name1.compareToIgnoreCase(name2);
			}

			private int depth(String name1) {
				int d = 0;
				for (int i = 0; i < name1.length(); i++) {
					if (name1.charAt(i) == '/') {
						d++;
					}
				}
				return d;
			}

			private boolean isLastSameStartsWithDigit(String name1, int lastSame) {
				if (name1.length() > lastSame) {
					return Character.isDigit(name1.charAt(lastSame));
				}

				return false;
			}
		});
		return allGpxFiles;
	}

	public static void readGpxDirectory(@Nullable File dir, @NonNull List<GPXInfo> list,
	                                    @NonNull String parent, boolean absolutePath) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					if (file.isFile() && name.toLowerCase().endsWith(GPX_FILE_EXT)) {
						String fileName = absolutePath ? file.getAbsolutePath() : parent + name;
						list.add(new GPXInfo(fileName, file.lastModified(), file.length()));
					} else if (file.isDirectory()) {
						readGpxDirectory(file, list, parent + name + "/", absolutePath);
					}
				}
			}
		}
	}

	public static void loadGPXFileInDifferentThread(Activity activity, CallbackWithObject<GPXFile[]> callbackWithObject,
	                                                File dir, GPXFile currentFile, String... filename) {
		ProgressDialog dlg = ProgressDialog.show(activity, activity.getString(R.string.loading_smth, ""),
				activity.getString(R.string.loading_data));
		new Thread(new Runnable() {
			@Override
			public void run() {
				GPXFile[] result = new GPXFile[filename.length + (currentFile == null ? 0 : 1)];
				int k = 0;
				String w = "";
				if (currentFile != null) {
					result[k++] = currentFile;
				}
				for (String fname : filename) {
					File f = new File(dir, fname);
					GPXFile res = GPXUtilities.loadGPXFile(f);
					if (res.error != null && !Algorithms.isEmpty(res.error.getMessage())) {
						w += res.error.getMessage() + "\n";
					} else {
						res.addGeneralTrack();
					}
					result[k++] = res;
				}
				dlg.dismiss();
				String warn = w;
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (warn.length() > 0) {
							Toast.makeText(activity, warn, Toast.LENGTH_LONG).show();
						} else {
							callbackWithObject.processResult(result);
						}
					}
				});
			}

		}, "Loading gpx").start(); //$NON-NLS-1$
	}

	public enum GPXDataSetType {
		ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
		SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed),
		SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

		private final int stringId;
		private final int imageId;

		GPXDataSetType(int stringId, int imageId) {
			this.stringId = stringId;
			this.imageId = imageId;
		}

		public String getName(@NonNull Context ctx) {
			return ctx.getString(stringId);
		}

		public static String getName(@NonNull Context ctx, @NonNull GPXDataSetType[] types) {
			List<String> list = new ArrayList<>();
			for (GPXDataSetType type : types) {
				list.add(type.getName(ctx));
			}
			Collections.sort(list);
			StringBuilder sb = new StringBuilder();
			for (String s : list) {
				if (sb.length() > 0) {
					sb.append("/");
				}
				sb.append(s);
			}
			return sb.toString();
		}
	}

	@NonNull
	public static GPXFile makeGpxFromLocations(List<Location> locations, OsmandApplication app) {
		double lastHeight = HEIGHT_UNDEFINED;
		double lastValidHeight = Double.NaN;
		GPXFile gpx = new GPXFile(Version.getFullVersion(app));
		if (locations != null) {
			Track track = new Track();
			TrkSegment seg = new TrkSegment();
			List<WptPt> pts = seg.points;
			for (Location l : locations) {
				WptPt point = new WptPt();
				point.lat = l.getLatitude();
				point.lon = l.getLongitude();
				if (l.hasAltitude()) {
					gpx.hasAltitude = true;
					float h = (float) l.getAltitude();
					point.ele = h;
					lastValidHeight = h;
					if (lastHeight == HEIGHT_UNDEFINED && pts.size() > 0) {
						for (WptPt pt : pts) {
							if (Double.isNaN(pt.ele)) {
								pt.ele = h;
							}
						}
					}
					lastHeight = h;
				} else {
					lastHeight = HEIGHT_UNDEFINED;
				}
				if (pts.size() == 0) {
					if (l.hasSpeed() && l.getSpeed() > 0) {
						point.speed = l.getSpeed();
					}
					point.time = System.currentTimeMillis();
				} else {
					GPXUtilities.WptPt prevPoint = pts.get(pts.size() - 1);
					if (l.hasSpeed() && l.getSpeed() > 0) {
						point.speed = l.getSpeed();
						double dist = MapUtils.getDistance(prevPoint.lat, prevPoint.lon, point.lat, point.lon);
						point.time = prevPoint.time + (long) (dist / point.speed * SECOND_IN_MILLIS);
					} else {
						point.time = prevPoint.time;
					}
				}
				pts.add(point);
			}
			if (!Double.isNaN(lastValidHeight) && lastHeight == HEIGHT_UNDEFINED) {
				for (ListIterator<WptPt> iterator = pts.listIterator(pts.size()); iterator.hasPrevious(); ) {
					WptPt point = iterator.previous();
					if (!Double.isNaN(point.ele)) {
						break;
					}
					point.ele = lastValidHeight;
				}
			}
			track.segments.add(seg);
			gpx.tracks.add(track);
		}
		return gpx;
	}

	public static void saveGpx(GPXFile gpxFile, SaveGpxListener listener) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	public static String getGpxFileRelativePath(@NonNull OsmandApplication app, @NonNull String fullPath) {
		String rootGpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath() + '/';
		return fullPath.replace(rootGpxDir, "");
	}

	public static class GPXInfo {
		private final String fileName;
		private final long lastModified;
		private final long fileSize;
		private boolean selected;

		public GPXInfo(String fileName, long lastModified, long fileSize) {
			this.fileName = fileName;
			this.lastModified = lastModified;
			this.fileSize = fileSize;
		}

		public String getFileName() {
			return fileName;
		}

		public long getLastModified() {
			return lastModified;
		}

		public long getFileSize() {
			return fileSize;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}
}
