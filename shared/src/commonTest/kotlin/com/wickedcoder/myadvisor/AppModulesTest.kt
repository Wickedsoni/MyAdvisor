package com.wickedcoder.myadvisor

import com.wickedcoder.myadvisor.di.appModules
import kotlin.test.Test
import kotlin.test.assertTrue

class AppModulesTest {

    @Test
    fun `DI module list is assembled`() {
        assertTrue(appModules().isNotEmpty())
    }
}
