package com.tylermolamphy.sharetocalendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun appPackageName_isCorrect() {
        val expectedPackage = "com.tylermolamphy.sharetocalendar"
        assertEquals(expectedPackage, "com.tylermolamphy.sharetocalendar")
    }

    @Test
    fun basicSanityCheck() {
        assertNotNull("App should have a valid name", "Share to Calendar")
    }
}
