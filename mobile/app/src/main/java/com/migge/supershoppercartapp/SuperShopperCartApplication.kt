package com.migge.supershoppercartapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SuperShopperCartApplication: Application() {

    override fun onCreate() {
        super.onCreate()
    }
}