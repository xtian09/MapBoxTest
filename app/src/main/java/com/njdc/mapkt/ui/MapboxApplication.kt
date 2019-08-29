package com.njdc.mapkt.ui

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import com.njdc.mapkt.R

class MapboxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
    }
}