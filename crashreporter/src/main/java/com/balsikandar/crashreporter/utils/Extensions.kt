package com.balsikandar.crashreporter.utils

import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/***
 * Enable edge-to-edge mode for the activity.
 */
fun ComponentActivity.enableEdgeToEdgeModeCompat() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        enableEdgeToEdge()
    }
}

fun View.applyWindowInsetsCompat(type: Int = WindowInsetsCompat.Type.statusBars()) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return
    }

    ViewCompat.setOnApplyWindowInsetsListener(
        this,
        OnApplyWindowInsetsListener { v: View, insets: WindowInsetsCompat ->
            val outInsets = insets.getInsets(type)
            v.setPadding(outInsets.left, outInsets.top, outInsets.right, outInsets.bottom)
            insets
        }
    )
}