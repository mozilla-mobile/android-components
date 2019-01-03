/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.test.espresso

import android.view.View
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matcher
import android.support.test.espresso.matcher.ViewMatchers.isChecked as espressoIsChecked
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed as espressoIsDisplayed
import android.support.test.espresso.matcher.ViewMatchers.isEnabled as espressoIsEnabled
import android.support.test.espresso.matcher.ViewMatchers.isSelected as espressoIsSelected

/**
 * The [espressoIsChecked] function that can also handle unchecked state through the boolean argument.
 */
fun isChecked(isChecked: Boolean): Matcher<View> = maybeInvertMatcher(espressoIsChecked(), isChecked)

/**
 * The [espressoIsDisplayed] function that can also handle not selected state through the boolean argument.
 */
fun isDisplayed(isDisplayed: Boolean): Matcher<View> = maybeInvertMatcher(espressoIsDisplayed(), isDisplayed)

/**
 * The [espressoIsEnabled] function that can also handle disabled state through the boolean argument.
 */
fun isEnabled(isEnabled: Boolean): Matcher<View> = maybeInvertMatcher(espressoIsEnabled(), isEnabled)

/**
 * The [espressoIsSelected] function that can also handle not selected state through the boolean argument.
 */
fun isSelected(isSelected: Boolean): Matcher<View> = maybeInvertMatcher(espressoIsSelected(), isSelected)

private fun maybeInvertMatcher(matcher: Matcher<View>, useUnmodifiedMatcher: Boolean): Matcher<View> = when {
    useUnmodifiedMatcher -> matcher
    else -> not(matcher)
}
