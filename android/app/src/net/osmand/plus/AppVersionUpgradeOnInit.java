package net.osmand.plus;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

class AppVersionUpgradeOnInit {

	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN";
	private static final String VERSION_INSTALLED_NUMBER = "VERSION_INSTALLED_NUMBER";
	private static final String NUMBER_OF_STARTS = "NUMBER_OF_STARTS";
	private static final String FIRST_INSTALLED = "FIRST_INSTALLED";
	private static final String UPDATE_TIME_MS = "UPDATE_TIME_MS";

	public static final int VERSION_1_0_0 = 100;

	public static final int LAST_APP_VERSION = VERSION_1_0_0;

	private static final String VERSION_INSTALLED = "VERSION_INSTALLED";

	private final OsmandApplication app;

	private int prevAppVersion;
	private boolean appVersionChanged;
	private boolean firstTime;

	AppVersionUpgradeOnInit(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@SuppressLint("ApplySharedPref")
	void upgradeVersion(@NonNull SharedPreferences startPrefs, int lastVersion) {
		if (!startPrefs.contains(NUMBER_OF_STARTS)) {
			startPrefs.edit().putInt(NUMBER_OF_STARTS, 1).commit();
		} else {
			startPrefs.edit().putInt(NUMBER_OF_STARTS, startPrefs.getInt(NUMBER_OF_STARTS, 0) + 1).commit();
		}
		if (!startPrefs.contains(FIRST_INSTALLED)) {
			startPrefs.edit().putLong(FIRST_INSTALLED, System.currentTimeMillis()).commit();
		}
		if (!startPrefs.contains(UPDATE_TIME_MS)) {
			startPrefs.edit().putLong(UPDATE_TIME_MS, System.currentTimeMillis()).commit();
		}
		if (!startPrefs.contains(FIRST_TIME_APP_RUN)) {
			firstTime = true;
			startPrefs.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			startPrefs.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, lastVersion).commit();
		} else {
			prevAppVersion = startPrefs.getInt(VERSION_INSTALLED_NUMBER, 0);
			if (needsUpgrade(startPrefs, lastVersion)) {
				startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, lastVersion).commit();
				startPrefs.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
				startPrefs.edit().putLong(UPDATE_TIME_MS, System.currentTimeMillis()).commit();
				appVersionChanged = true;
			}
		}
	}

	private boolean needsUpgrade(SharedPreferences startPrefs, int maxVersion) {
		return !(Version.getFullVersion(app)).equals(startPrefs.getString(VERSION_INSTALLED, "")) || prevAppVersion < maxVersion;
	}

	boolean isAppVersionChanged() {
		return appVersionChanged;
	}

	int getPrevAppVersion() {
		return prevAppVersion;
	}

	public void resetFirstTimeRun(SharedPreferences startPrefs) {
		if (startPrefs != null) {
			startPrefs.edit().remove(FIRST_TIME_APP_RUN).commit();
		}
	}

	public boolean isFirstTime() {
		return firstTime;
	}
}