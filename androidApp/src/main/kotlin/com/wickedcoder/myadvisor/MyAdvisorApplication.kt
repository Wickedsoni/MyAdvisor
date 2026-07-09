package com.wickedcoder.myadvisor

import android.app.Application
import com.wickedcoder.myadvisor.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyAdvisorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyAdvisorApplication)
        }
    }
}
