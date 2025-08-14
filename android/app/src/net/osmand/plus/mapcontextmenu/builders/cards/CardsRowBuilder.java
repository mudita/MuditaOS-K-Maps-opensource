package net.osmand.plus.mapcontextmenu.builders.cards;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.mudita.maps.R;

import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CardsRowBuilder {

	private final OsmandApplication app;
	private final MapActivity mapActivity;

	private final MenuBuilder menuBuilder;
	private final List<AbstractCard> cards = new ArrayList<>();

	private final View view;
	private LockableViewPager viewPager;
	private ViewsPagerAdapter pagerAdapter;

	private final boolean addToLayout;
	private final int dp10;

	public CardsRowBuilder(MenuBuilder menuBuilder, View view, boolean addToLayout) {
		this.menuBuilder = menuBuilder;
		this.view = view;
		this.addToLayout = addToLayout;
		this.mapActivity = menuBuilder.getMapActivity();
		this.app = menuBuilder.getApplication();
		this.dp10 = AndroidUtils.dpToPx(app, 10f);
	}

	public MenuBuilder getMenuBuilder() {
		return menuBuilder;
	}

	public View getContentView() {
		return viewPager;
	}

	public void setCards(AbstractCard... cards) {
		setCards(Arrays.asList(cards));
	}

	public void setCards(Collection<? extends AbstractCard> cards) {
		this.cards.clear();
		if (cards != null) {
			this.cards.addAll(cards);
		}
		if (!menuBuilder.isHidden()) {
			viewPager.setSwipeLocked(itemsCount() < 2);
			pagerAdapter.notifyDataSetChanged();
		}
	}

	public void setProgressCard() {
		setCards(new ProgressCard(mapActivity));
	}

	public void build() {
		viewPager = new LockableViewPager(view.getContext());
		ViewPager.LayoutParams params = new ViewPager.LayoutParams();
		params.width = ViewGroup.LayoutParams.MATCH_PARENT;
		params.height = (int) app.getResources().getDimension(R.dimen.context_img_card_height) + dp10 + dp10;
		viewPager.setLayoutParams(params);
		viewPager.setPageMargin(dp10);
		viewPager.setPadding(dp10, dp10, dp10, dp10);
		viewPager.setClipToPadding(false);
		pagerAdapter = new ViewsPagerAdapter();
		viewPager.setAdapter(pagerAdapter);
		viewPager.setSwipeLocked(itemsCount() < 2);
		if (addToLayout) {
			((LinearLayout) view).addView(viewPager);
		}
	}

	private int itemsCount() {
		return cards.size();
	}

	private View createPageView(int position) {
		return cards.get(position).build(view.getContext());
	}

	private class ViewsPagerAdapter extends PagerAdapter {

		@Override
		public float getPageWidth(int position) {
			return 0.8f;
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			return POSITION_NONE;
		}

		@Override
		public int getCount() {
			return itemsCount();
		}

		@NonNull
		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			View view = createPageView(position);
			container.addView(view, 0);

			return view;
		}

		@Override
		public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
			return view == object;
		}
	}
}
