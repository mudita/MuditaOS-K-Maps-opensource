package net.osmand.plus.mapcontextmenu.builders.cards;

import com.mudita.maps.R;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class ProgressCard extends AbstractCard {

	public ProgressCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_progress;
	}

	@Override
	public void update() {
		OsmandApplication app = getMyApplication();
		AndroidUtils.setBackgroundColor(app, view, ColorUtilities.getListBgColorId(false));
	}
}
