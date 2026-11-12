package com.didichuxing.doraemonkit.kit.core

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi

/**
 * Starting with Android 15, apps targeting API 35+ always draw edge-to-edge.
 * DoKit screens therefore have to inset their own content; older releases keep
 * the system-managed behaviour.
 */
internal object WindowInsetsUtils {

    fun applySystemBarInsets(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            applyInsets(view)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun applyInsets(view: View) {
        view.setOnApplyWindowInsetsListener { target, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            target.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        view.requestApplyInsets()
    }
}
