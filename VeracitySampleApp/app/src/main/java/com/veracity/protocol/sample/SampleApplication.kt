package com.veracity.protocol.sample

import android.app.Application
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump

class SampleApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        // inject custom font library
        ViewPump.init(
            ViewPump.builder()
            .addInterceptor(
                CalligraphyInterceptor(
                CalligraphyConfig.Builder()
                    .setDefaultFontPath("fonts/Regular.otf")
                    .setFontAttrId(R.attr.fontPath)
                    .build())
            )
            .build())
    }
}