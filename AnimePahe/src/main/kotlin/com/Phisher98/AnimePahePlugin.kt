package com.phisher98

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.phisher98.settings.SettingsFragment

@CloudstreamPlugin
class AnimePaheProviderPlugin: Plugin() {
    override fun load(context: Context) {
        val sharedPref = context.getSharedPreferences("AnimePahe", Context.MODE_PRIVATE)
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(AnimePahe(sharedPref))
        registerExtractorAPI(Kwik())
        registerExtractorAPI(Pahe())
        val activity = context as AppCompatActivity
        openSettings = {
            val frag = SettingsFragment(this, sharedPref)
            frag.show(activity.supportFragmentManager, "Frag")
        }
    }
}
