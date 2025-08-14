package net.osmand.plus.mapcontextmenu.builders.cards;

import android.widget.ImageView;
import android.widget.TextView;

import com.mudita.maps.R;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class NoImagesCard extends AbstractCard {

	public NoImagesCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.context_menu_card_no_images;
	}

	@Override
	public void update() {
		if (view != null) {
			UiUtilities ic = getMyApplication().getUIUtilities();
			MapActivity ctx = getMapActivity();
			AndroidUtils.setBackgroundColor(ctx, view, ColorUtilities.getListBgColorId(false));
			((ImageView) view.findViewById(R.id.icon_sadface)).setImageDrawable(ic.getIcon(R.drawable.ic_action_sadface,
					false ? R.color.color_white : R.color.icon_color_default_light));
			AndroidUtils.setTextPrimaryColor(ctx, view.findViewById(R.id.title), false);
			AndroidUtils.setBackgroundColor(ctx, view.findViewById(R.id.button_background), false,
					R.color.inactive_buttons_and_links_bg_light, R.color.inactive_buttons_and_links_bg_dark);
			((ImageView) view.findViewById(R.id.icon_add_photos)).setImageDrawable(
					ic.getIcon(R.drawable.ic_action_add_photos, ColorUtilities.getActiveColorId(false)));
			((TextView) view.findViewById(R.id.app_photos_text_view))
					.setTextColor(ColorUtilities.getActiveColor(ctx, false));
			AndroidUtils.setBackground(ctx, view.findViewById(R.id.card_background), false,
					R.drawable.context_menu_card_light, R.drawable.context_menu_card_dark);
		}
	}
}
