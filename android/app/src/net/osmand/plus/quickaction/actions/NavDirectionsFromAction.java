package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mudita.maps.R;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavDirectionsFromAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(19,
			"nav.directions", NavDirectionsFromAction.class).
			nameRes(R.string.context_menu_item_directions_from).iconRes(R.drawable.ic_action_route_direction_from_here).nonEditable().
			category(QuickActionType.NAVIGATION);

	public NavDirectionsFromAction() {
		super(TYPE);
	}

	public NavDirectionsFromAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_directions_from_desc);

		parent.addView(view);
	}
}
