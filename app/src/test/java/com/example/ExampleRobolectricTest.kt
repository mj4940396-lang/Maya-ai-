package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LanguageMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val welcomeMessage = context.getString(R.string.welcome_message)
        assertEquals("Maya AI", appName)
        assertEquals("Assalam-o-Alaikum! I am Maya AI. How can I help you today?", welcomeMessage)
    }

    @Test
    fun `verify language modes configured`() {
        assertNotNull(LanguageMode.URDU)
        assertNotNull(LanguageMode.ROMAN_URDU)
        assertNotNull(LanguageMode.ENGLISH)
        assertNotNull(LanguageMode.AUTO)
    }
}
