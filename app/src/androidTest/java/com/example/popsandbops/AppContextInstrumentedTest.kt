package com.example.popsandbops

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContextInstrumentedTest {
    @Test
    fun appPackageNameMatchesApplicationId() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.example.popsandbops", appContext.packageName)
    }
}
