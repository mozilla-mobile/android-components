/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.content.Context
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.os.Build.VERSION_CODES.LOLLIPOP

import android.view.ContextThemeWrapper
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import mozilla.components.feature.prompts.TimePickerDialogFragment.Companion.SELECTION_TYPE_DATE_AND_TIME
import mozilla.components.feature.prompts.TimePickerDialogFragment.Companion.SELECTION_TYPE_TIME
import mozilla.components.support.ktx.kotlin.toDate
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class TimePickerDialogFragmentTest {
    private val context: Context
        get() = ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_AppCompat
        )

    @Test
    fun `build dialog`() {
        val initialDate = "2019-11-29".toDate("yyyy-MM-dd")
        val minDate = "2019-11-28".toDate("yyyy-MM-dd")
        val maxDate = "2019-11-30".toDate("yyyy-MM-dd")
        val fragment = spy(
                TimePickerDialogFragment.newInstance("sessionId", "title", initialDate, minDate, maxDate)
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val titleTextView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        val datePicker = dialog.findViewById<DatePicker>(mozilla.components.feature.prompts.R.id.date_picker)

        assertEquals(titleTextView.text, "title")
        assertEquals(2019, datePicker.year)
        assertEquals(11, datePicker.month + 1)
        assertEquals(29, datePicker.dayOfMonth)
        assertEquals(minDate, Date(datePicker.minDate))
        assertEquals(maxDate, Date(datePicker.maxDate))

        datePicker.updateDate(2101, 10, 28)

        val selectedDate = fragment.selectedDate
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        assertEquals(2101, calendar.year)
        assertEquals(10, calendar.month)
        assertEquals(28, calendar.day)
    }

    @Test
    fun `Clicking on positive, neutral and negative button notifies the feature`() {
        val mockFeature: PromptFeature = mock()
        val initialDate = "2019-11-29".toDate("yyyy-MM-dd")
        val fragment = spy(
                TimePickerDialogFragment.newInstance("sessionId", "", initialDate, null, null)
        )
        fragment.feature = mockFeature

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
        positiveButton.performClick()
        verify(mockFeature).onConfirm("sessionId", initialDate)

        val neutralButton = dialog.getButton(BUTTON_NEUTRAL)
        neutralButton.performClick()
        verify(mockFeature).onClear("sessionId")

        val negativeButton = dialog.getButton(BUTTON_NEGATIVE)
        negativeButton.performClick()
        verify(mockFeature).onCancel("sessionId")
    }

    @Test
    fun `touching outside of the dialog must notify the feature onCancel`() {
        val mockFeature: PromptFeature = mock()
        val fragment = spy(
                TimePickerDialogFragment.newInstance("sessionId", "", Date(), null, null)
        )
        fragment.feature = mockFeature
        doReturn(context).`when`(fragment).requireContext()
        fragment.onCancel(null)
        verify(mockFeature).onCancel("sessionId")
    }

    @Test
    fun `onTimeChanged must update the selectedDate`() {

        val dialogPicker = TimePickerDialogFragment.newInstance("sessionId", "", Date(), null, null)

        dialogPicker.onTimeChanged(mock(), 1, 12)

        val calendar = dialogPicker.selectedDate.toCalendar()

        assertEquals(calendar.hour, 1)
        assertEquals(calendar.minutes, 12)
    }

    @Test
    fun `building a date and time picker`() {
        val initialDate = "2018-06-12T19:30".toDate("yyyy-MM-dd'T'HH:mm")
        val minDate = "2018-06-07T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val maxDate = "2018-06-14T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val fragment = spy(
            TimePickerDialogFragment.newInstance(
                "sessionId",
                "title",
                initialDate,
                minDate,
                maxDate,
                SELECTION_TYPE_DATE_AND_TIME
            )
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val datePicker = dialog.findViewById<DatePicker>(mozilla.components.feature.prompts.R.id.date_picker)

        assertEquals(2018, datePicker.year)
        assertEquals(6, datePicker.month + 1)
        assertEquals(12, datePicker.dayOfMonth)

        assertEquals(minDate, Date(datePicker.minDate))
        assertEquals(maxDate, Date(datePicker.maxDate))

        val timePicker = dialog.findViewById<TimePicker>(mozilla.components.feature.prompts.R.id.datetime_picker)

        assertEquals(19, timePicker.hour)
        assertEquals(30, timePicker.minute)
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun `building a time picker`() {
        val initialDate = "2018-06-12T19:30".toDate("yyyy-MM-dd'T'HH:mm")
        val minDate = "2018-06-07T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val maxDate = "2018-06-14T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val fragment = spy(
            TimePickerDialogFragment.newInstance(
                "sessionId",
                "title",
                initialDate,
                minDate,
                maxDate,
                SELECTION_TYPE_TIME
            )
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val timePicker = dialog.findViewById<TimePicker>(mozilla.components.feature.prompts.R.id.time_picker)

        assertEquals(19, timePicker.currentHour)
        assertEquals(30, timePicker.currentMinute)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `creating a TimePickerDialogFragment with an invalid type selection will throw an exception`() {
        val initialDate = "2018-06-12T19:30".toDate("yyyy-MM-dd'T'HH:mm")
        val minDate = "2018-06-07T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val maxDate = "2018-06-14T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val fragment = spy(
            TimePickerDialogFragment.newInstance(
                "sessionId",
                "title",
                initialDate,
                minDate,
                maxDate,
                -223
            )
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `creating a TimePickerDialogFragment with empty title and an invalid type selection will throw an exception `() {
        val initialDate = "2018-06-12T19:30".toDate("yyyy-MM-dd'T'HH:mm")
        val minDate = "2018-06-07T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val maxDate = "2018-06-14T00:00".toDate("yyyy-MM-dd'T'HH:mm")
        val fragment = spy(
            TimePickerDialogFragment.newInstance(
                "sessionId",
                "",
                initialDate,
                minDate,
                maxDate,
                -223
            )
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()
    }

    private fun Date.toCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return calendar
    }

    private val Calendar.minutes: Int
        get() = get(Calendar.MINUTE)
    private val Calendar.hour: Int
        get() = get(Calendar.HOUR_OF_DAY)
}