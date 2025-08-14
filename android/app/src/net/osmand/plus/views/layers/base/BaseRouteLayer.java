package net.osmand.plus.views.layers.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mudita.maps.R;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

public abstract class BaseRouteLayer extends OsmandMapLayer {

	private static final Log log = PlatformUtil.getLog(BaseRouteLayer.class);

	protected boolean nightMode;
	private boolean carView;

	protected PreviewRouteLineInfo previewRouteLineInfo;
	protected ColoringType routeColoringType = ColoringType.DEFAULT;
	protected String routeInfoAttribute;

	protected RenderingLineAttributes attrs;
	protected int routeLineColor;
	protected Integer directionArrowsColor;
	protected int customTurnArrowColor;

    protected Paint paintIconAction;
	private Bitmap actionArrow;

	public BaseRouteLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		init();
	}

	private void init() {
		float density = view.getDensity();
		initAttrs(density);
		initGeometries(density);
		initPaints();
		initIcons();
		routeLineColor = Color.BLACK;
	}

	protected void initAttrs(float density) {
		attrs = new RenderingLineAttributes("route");
		attrs.defaultWidth = (int) (12 * density);
		attrs.defaultWidth3 = (int) (7 * density);
		attrs.defaultColor = ContextCompat.getColor(getContext(), R.color.nav_track);
		attrs.paint3.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint3.setColor(Color.WHITE);
		attrs.paint2.setStrokeCap(Paint.Cap.BUTT);
		attrs.paint2.setColor(Color.BLACK);
	}

	protected void initPaints() {
		paintIconAction = new Paint();
		paintIconAction.setFilterBitmap(true);
		paintIconAction.setAntiAlias(true);
	}

	protected void initIcons() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);
	}

	protected void updateRouteColoringType() {
		if (previewRouteLineInfo != null) {
			routeColoringType = previewRouteLineInfo.getRouteColoringType();
			routeInfoAttribute = previewRouteLineInfo.getRouteInfoAttribute();
		} else {
			ApplicationMode mode = view.getApplication().getRoutingHelper().getAppMode();
			OsmandSettings settings = view.getSettings();
			routeColoringType = settings.ROUTE_COLORING_TYPE.getModeValue(mode);
			routeInfoAttribute = settings.ROUTE_INFO_ATTRIBUTE.getModeValue(mode);
		}
	}

	@ColorInt
	public int getRouteLineColor() {
		return routeLineColor;
	}

	protected float getRouteLineWidth() {
		return getContext().getResources().getDimensionPixelSize(R.dimen.route_line_width);
	}

	protected boolean shouldShowTurnArrows() {
		return false;
	}

	protected void drawTurnArrow(Canvas canvas, Matrix matrix, float x, float y, float px, float py) {
		double angleRad = Math.atan2(y - py, x - px);
		double angle = (angleRad * 180 / Math.PI) + 90f;
		double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
		if (distSegment == 0) {
			return;
		}

		float pdx = x - px;
		float pdy = y - py;
		float scale = attrs.paint3.getStrokeWidth() / (actionArrow.getWidth() / 2.25f);
		float scaledWidth = actionArrow.getWidth();
		matrix.reset();
		matrix.postTranslate(0, -actionArrow.getHeight() / 2f);
		matrix.postRotate((float) angle, actionArrow.getWidth() / 2f, 0);
		if (scale > 0) {
			matrix.postScale(scale, scale);
			scaledWidth *= scale;
		}
		matrix.postTranslate(px + pdx - scaledWidth / 2f, py + pdy);
		canvas.drawBitmap(actionArrow, matrix, paintIconAction);
	}

	protected void drawIcon(Canvas canvas, Drawable drawable, int locationX, int locationY) {
		drawable.setBounds(locationX - drawable.getIntrinsicWidth() / 2,
				locationY - drawable.getIntrinsicHeight() / 2,
				locationX + drawable.getIntrinsicWidth() / 2,
				locationY + drawable.getIntrinsicHeight() / 2);
		drawable.draw(canvas);
	}

	public boolean isPreviewRouteLineVisible() {
		return previewRouteLineInfo != null;
	}

	public void setPreviewRouteLineInfo(PreviewRouteLineInfo previewInfo) {
		this.previewRouteLineInfo = previewInfo;
	}

	protected ApplicationMode getAppMode() {
		return view.getApplication().getRoutingHelper().getAppMode();
	}

	protected abstract void initGeometries(float density);

	protected abstract void updateAttrs(DrawSettings settings, RotatedTileBox tileBox);

}