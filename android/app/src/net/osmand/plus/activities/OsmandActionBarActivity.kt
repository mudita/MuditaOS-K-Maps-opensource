package net.osmand.plus.activities

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities

@SuppressLint("Registered")
open class OsmandActionBarActivity : OsmandBaseActivity() {
    private var haveHomeButton = true

    //should be called after set content view
    protected fun setupHomeButton() {
        supportActionBar?.let { supportActionBar ->
            val app: OsmandApplication = myApplication
            val nightMode: Boolean = app.settings?.isLightContent?.not() ?: false
            val iconId: Int = AndroidUtils.getNavigationIconResId(app)
            val colorId: Int = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode)
            supportActionBar.setHomeButtonEnabled(true)
            supportActionBar.setDisplayHomeAsUpEnabled(true)
            supportActionBar.setHomeAsUpIndicator(app.uIUtilities.getIcon(iconId, colorId))
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (haveHomeButton) {
            setupHomeButton()
        }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        if (haveHomeButton) {
            setupHomeButton()
        }
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        if (haveHomeButton) {
            setupHomeButton()
        }
    }
}