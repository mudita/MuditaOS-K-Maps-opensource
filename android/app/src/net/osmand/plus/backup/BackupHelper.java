package net.osmand.plus.backup;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

public class BackupHelper {
	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		setLastModifiedTime(ctx, name, System.currentTimeMillis());
	}

	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name, long lastModifiedTime) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		new BackupDbHelper(app).setLastModifiedTime(name, lastModifiedTime);
	}

	public static long getLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		return new BackupDbHelper(app).getLastModifiedTime(name);
	}
}
