/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.error

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.glean.error.ErrorRecording.ErrorType.InvalidLabel
import mozilla.components.service.glean.error.ErrorRecording.ErrorType.InvalidValue
import mozilla.components.service.glean.private.Lifetime
import mozilla.components.service.glean.private.StringMetricType
import mozilla.components.service.glean.resetGlean
import mozilla.components.service.glean.storages.CountersStorageEngine
import mozilla.components.support.base.log.logger.Logger
import mozilla.ext.combineWith
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorRecordingTest {

    @Before
    fun setup() {
        resetGlean()
    }

    @Test
    fun `test recording of all error types`() {
        CountersStorageEngine.clearAllStores()
        val logger = Logger("glean/ErrorRecordingTest")

        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_metric",
            sendInPings = listOf("store1", "store2")
        )

        ErrorRecording.recordError(
            stringMetric,
            InvalidValue,
            "Invalid value",
            logger
        )

        ErrorRecording.recordError(
            stringMetric,
            InvalidLabel,
            "Invalid label",
            logger
        )

        listOf("store1", "store2", "metrics")
            .combineWith(listOf(InvalidValue, InvalidLabel))
            .forEach { (storeName, errorType) ->
                assertEquals(
                    1,
                    ErrorRecording.testGetNumRecordedErrors(stringMetric, errorType, storeName)
                )
            }
    }
}
