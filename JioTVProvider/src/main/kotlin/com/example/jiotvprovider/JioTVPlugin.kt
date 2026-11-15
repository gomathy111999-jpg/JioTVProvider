package com.example.jiotvprovider

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class JioTVPlugin : Plugin() {
    override fun load(context: Context) {
        val provider = JioTVProvider(context.applicationContext)
        registerMainAPI(provider)
        openSettings = { settingsContext -> JioTVSettings(provider).show(settingsContext) }
    }
}
