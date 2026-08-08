package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31])
class ThemeTest {

    @Test
    fun `static dark scheme uses navy background`() {
        assertEquals(NavyBackground, DarkColors.background)
        assertEquals(NavyBackground, DarkColors.surfaceDim)
    }

    @Test
    fun `static light scheme is not overridden with navy`() {
        assertTrue(LightColors.background != NavyBackground)
    }

    @Test
    fun `static dark scheme onBackground has sufficient contrast against navy`() {
        val contrast = ColorUtils.calculateContrast(
            DarkColors.onBackground.toArgb(),
            NavyBackground.toArgb(),
        )
        assertTrue("contrast was $contrast, expected >= 4.5", contrast >= 4.5)
    }

}
