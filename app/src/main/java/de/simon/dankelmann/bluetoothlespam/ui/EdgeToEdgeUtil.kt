package de.simon.dankelmann.bluetoothlespam.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding


fun setupEdgeToEdge(view: View, top: Boolean = true, bottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        val i = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()
        )
        v.updatePadding(
            left = i.left,
            right = i.right,
        )
        if (top) {
            v.updatePadding(top = i.top)
        }
        if (bottom) {
            v.updatePadding(bottom = i.bottom)
        }
        WindowInsetsCompat.CONSUMED
    }
}
