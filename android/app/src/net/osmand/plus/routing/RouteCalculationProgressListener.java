package net.osmand.plus.routing;

import androidx.annotation.Nullable;

import java.util.List;

public interface RouteCalculationProgressListener {

	void onCalculationStart();

	void onUpdateCalculationProgress(int progress);

	void onRequestPrivateAccessRouting();

	void onUpdateMissingMaps(@Nullable List<String> missingMaps);

	void onCalculationFinish(@Nullable Exception error);
}
