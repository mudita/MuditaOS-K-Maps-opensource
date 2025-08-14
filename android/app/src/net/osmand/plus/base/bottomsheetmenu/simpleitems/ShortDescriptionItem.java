package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import com.mudita.maps.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;

public class ShortDescriptionItem extends BottomSheetItemWithDescription {

	public ShortDescriptionItem(CharSequence description) {
		this.description = description;
		this.layoutId = R.layout.bottom_sheet_item_description;
	}
}
