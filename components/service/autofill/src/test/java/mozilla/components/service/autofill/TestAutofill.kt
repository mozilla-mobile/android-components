/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.autofill


import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test
import org.mozilla.appservices.autofill.Store
import org.mozilla.appservices.autofill.NewCreditCardFields
import java.io.File
import mozilla.appservices.Megazord

@RunWith(AndroidJUnit4::class)
class AutofillTest {
    @Test
    fun `check we can create and use the low level component`() {
        // Set the name of the native library so that we use
        // the appservices megazord for compiled code.
        Megazord.init()
        System.setProperty(
            "uniffi.component.autofill.libraryOverride",
            System.getProperty("mozilla.appservices.megazord.library", "megazord")
        )

        val context: Context = ApplicationProvider.getApplicationContext()
        val dataDir = File(context.applicationInfo.dataDir, "autofill")
        val store = Store(dataDir.path)

        // Add a credit-card.
        val cc = NewCreditCardFields(
            ccName = "My name",
            ccNumber = "1234-5678-9012-3456",
            ccExpMonth = 12,
            ccExpYear = 2020,
            ccType = "fake"
        )
        val added = store.addCreditCard(cc)
        val fetched = store.getCreditCard(added.guid)
        assertEquals(added, fetched)
        assertEquals(fetched.ccType, "fake")
    }
}

