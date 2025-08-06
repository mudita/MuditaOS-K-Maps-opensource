package net.osmand.plus.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

public class OsmandBaseActivity extends AppCompatActivity {

	private static final Log LOG = PlatformUtil.getLog(OsmandBaseActivity.class);

	@NonNull
	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

}
