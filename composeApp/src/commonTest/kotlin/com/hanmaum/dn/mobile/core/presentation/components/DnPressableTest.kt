package com.hanmaum.dn.mobile.core.presentation.components

import com.hanmaum.dn.mobile.core.presentation.theme.AppMotion
import kotlin.test.Test
import kotlin.test.assertEquals

class DnPressableTest {
    @Test
    fun pressedAndEnabledScalesDown() {
        assertEquals(AppMotion.PRESS_SCALE, pressScale(pressed = true, enabled = true))
    }

    @Test
    fun releasedStaysAtRest() {
        assertEquals(1f, pressScale(pressed = false, enabled = true))
    }

    @Test
    fun disabledNeverScales() {
        assertEquals(1f, pressScale(pressed = true, enabled = false))
    }
}
