package net.osmand.plus.views;

import android.graphics.PointF;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;

import org.apache.commons.logging.Log;

import java.lang.reflect.Method;


public class MultiTouchSupport {

	private static final Log log = PlatformUtil.getLog(MultiTouchSupport.class);

	public static final int ACTION_MASK = 255;
	public static final int ACTION_POINTER_DOWN = 5;
	public static final int ACTION_POINTER_UP = 6;

	public interface MultiTouchZoomListener {

		void onZoomStarted(PointF centerPoint);

		void onZoomingOrRotating(double relativeToStart);

		void onZoomOrRotationEnded(double relativeToStart);

		void onGestureInit(float x1, float y1, float x2, float y2);

		void onActionPointerUp();

		void onActionCancel();
	}

	private final OsmandApplication app;
	private final MultiTouchZoomListener listener;

	private Method getPointerCount;
	private Method getX;
	private Method getY;
	private boolean multiTouchAPISupported;


	public MultiTouchSupport(@NonNull OsmandApplication app, @NonNull MultiTouchZoomListener listener) {
		this.app = app;
		this.listener = listener;
		initMethods();
	}

	public boolean isMultiTouchSupported() {
		return multiTouchAPISupported;
	}

	public boolean isInZoomMode() {
		return inZoomMode;
	}

	public boolean isInTiltMode() {
		return inTiltMode;
	}

	private void initMethods() {
		try {
			getPointerCount = MotionEvent.class.getMethod("getPointerCount");
			getX = MotionEvent.class.getMethod("getX", Integer.TYPE);
			getY = MotionEvent.class.getMethod("getY", Integer.TYPE);
			multiTouchAPISupported = true;
		} catch (Exception e) {
			multiTouchAPISupported = false;
			log.info("Multi touch not supported", e);
		}
	}

	private boolean inZoomMode;
	private boolean inTiltMode;
	private double zoomStartedDistance = 100;
	private double zoomRelative = 1;
	private PointF centerPoint = new PointF();

	public boolean onTouchEvent(MotionEvent event) {
		if (!isMultiTouchSupported()) {
			return false;
		}
		int actionCode = event.getAction() & ACTION_MASK;
		try {
			if (actionCode == MotionEvent.ACTION_CANCEL) {
				listener.onActionCancel();
			}
			Integer pointCount = (Integer) getPointerCount.invoke(event);
			if (pointCount < 2) {
				if (inZoomMode) {
					listener.onZoomOrRotationEnded(zoomRelative);
					inZoomMode = false;
					return true;
				} else if (inTiltMode) {
					inTiltMode = false;
					return true;
				}
				return false;
			}
			Float x1 = (Float) getX.invoke(event, 0);
			Float x2 = (Float) getX.invoke(event, 1);
			Float y1 = (Float) getY.invoke(event, 0);
			Float y2 = (Float) getY.invoke(event, 1);
			float distance = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
			if (actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_POINTER_UP) {
				listener.onActionPointerUp();
			}
			if (actionCode == ACTION_POINTER_DOWN) {
				centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
				listener.onGestureInit(x1, y1, x2, y2);
				listener.onZoomStarted(centerPoint);
				zoomStartedDistance = distance;
				return true;
			} else if (actionCode == ACTION_POINTER_UP) {
				if (inZoomMode) {
					listener.onZoomOrRotationEnded(zoomRelative);
					inZoomMode = false;
				} else if (inTiltMode) {
					inTiltMode = false;
				}
				return true;
			} else if (actionCode == MotionEvent.ACTION_MOVE) {
				if (inZoomMode) {

					// Keep zoom center fixed or flexible
					centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
					zoomRelative = distance / zoomStartedDistance;
					listener.onZoomingOrRotating(zoomRelative);
				} else {
					inZoomMode = true;
				}
				return true;
			}
		} catch (Exception e) {
			log.debug("Multi touch exception", e);
		}
		return false;
	}

	public PointF getCenterPoint() {
		return centerPoint;
	}

	public static boolean isTiltSupportEnabled(@NonNull OsmandApplication app) {
		return isTiltSupported(app) && app.getSettings().ENABLE_3D_VIEW.get();
	}

	public static boolean isTiltSupported(@NonNull OsmandApplication app) {
		return app.useOpenGlRenderer();
	}
}
