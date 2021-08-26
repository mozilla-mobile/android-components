/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts.dialog

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputEditText
import junit.framework.TestCase
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.concept.storage.LoginsStorage
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.prompts.R
import mozilla.components.support.test.any
import mozilla.components.support.test.ext.appCompatContext
import mozilla.components.support.test.fakes.fakeLogin
import mozilla.components.support.test.fakes.fakeEncryptedLogin
import mozilla.components.support.test.fakes.fakeLoginEntry
import mozilla.components.support.test.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.Shadows

private data class ViewState(
    var headline: String? = null,
    var negativeText: String? = null,
    var confirmText: String? = null,
    var confirmButtonEnabled: Boolean? = null,
    var passwordErrorText: String? = null
)

@kotlinx.coroutines.ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveLoginDialogFragmentTest : TestCase() {
    // Login entry data that the user is asking to save.  usernameField and passwordField are blank, because we don't yet support them.
    val initialEntry = fakeLoginEntry(usernameField="", passwordField="")
    val testLoginsStorage: LoginsStorage = mock(LoginsStorage::class.java)
    val testFeature = spy(PromptFeature(
        activity = mock(),
        store = mock(),
        fragmentManager = mock(),
        onNeedToRequestPermissions = mock(),
        loginsStorage = testLoginsStorage,
    ))
    val loginsForOrigin = listOf(fakeLogin(username="user1"), fakeLogin(username="user2"), fakeLogin(username=""))
    // SaveLoginDialogFragment offloads some of it's work into coroutines (for
    // example `LoginsStorage.getByBaseDomain()`).  Use TestCoroutineDispatcher
    // to ensure that we get consistent behavior in the tests.
    val testDispatcher = TestCoroutineDispatcher()
    val sessionId = "sessionId"
    val requestUID = "uid"
    val hint = 42
    val shouldDismissOnLoad = true
    var findLoginToUpdateResult: Login? = null
    private var lastSetViewState = ViewState()

    init {
        runBlocking {
            `when`(testLoginsStorage.findLoginToUpdate(any(), any())).thenAnswer {
                findLoginToUpdateResult
            }
            `when`(testLoginsStorage.decryptLogins(any())).thenReturn(loginsForOrigin)
            // we only care about what comes out of decryptLogins, so just return an empty list for getByBaseDomain
            `when`(testLoginsStorage.getByBaseDomain(any())).thenReturn(listOf())
        }
        doNothing().`when`(testFeature).onConfirm(any(), any(), any())
    }

    internal fun createFragment(
        icon: Bitmap? = mock(),
    ): SaveLoginDialogFragment {
        val fragment = spy(
            SaveLoginDialogFragment.newInstance(
                sessionId, requestUID, shouldDismissOnLoad, hint,
                initialEntry, icon,
                dispatcher = testDispatcher,
            )
        )
        fragment.feature = testFeature
        return fragment
    }

    internal fun createView(fragment: SaveLoginDialogFragment): View {
        doReturn(appCompatContext).`when`(fragment).getContext()
        doNothing().`when`(fragment).dismiss()

        doAnswer {
            FrameLayout(appCompatContext).apply {
                addView(AppCompatTextView(appCompatContext).apply { id = R.id.host_name })
                addView(AppCompatTextView(appCompatContext).apply { id = R.id.save_message })
                addView(TextInputEditText(appCompatContext).apply { id = R.id.username_field })
                addView(TextInputEditText(appCompatContext).apply { id = R.id.password_field })
                addView(ImageView(appCompatContext).apply { id = R.id.host_icon })
                addView(Button(appCompatContext).apply { id = R.id.save_cancel })
                addView(Button(appCompatContext).apply { id = R.id.save_confirm })
            }
        }.`when`(fragment).inflateRootView(any())

        doAnswer { invocation ->
            lastSetViewState.headline = invocation.getArgument(0) ?: lastSetViewState.headline
            lastSetViewState.negativeText = invocation.getArgument(1) ?: lastSetViewState.negativeText
            lastSetViewState.confirmText = invocation.getArgument(2) ?: lastSetViewState.confirmText
            lastSetViewState.confirmButtonEnabled = invocation.getArgument(3) ?: lastSetViewState.confirmButtonEnabled
            lastSetViewState.passwordErrorText = invocation.getArgument(4) ?: lastSetViewState.passwordErrorText
            Unit
        }.`when`(fragment).setViewState(any(), any(), any(), any(), any())

        return fragment.onCreateView(mock(), mock(), mock()).also {
            fragment.onViewCreated(it, null)
        }
    }

    @Test
    fun `dialog should setup the root view`() {
        val fragment = createFragment()
        createView(fragment)
        verify(fragment).inflateRootView(any())
        verify(fragment).setupRootView(any())
    }
    @Test
    fun `dialog should initialize the username and password fields`() {
        val view = createView(createFragment())
        assertTrue(initialEntry.username == view.findViewById<TextInputEditText>(R.id.username_field).text.toString())
        assertTrue(initialEntry.password == view.findViewById<TextInputEditText>(R.id.password_field).text.toString())
    }

    @Test
    fun `dialog should always set the website icon if it is available`() {
        val icon: Bitmap = mock()
        val fragment = createFragment(icon=icon)
        val view = createView(fragment)
        // The image tint shouldn't be set in this case
        verify(fragment, times(0)).setImageViewTint(any())
        // Actually verifying that the provided image is used
        assertSame(icon, (view.findViewById<ImageView>(R.id.host_icon).drawable as BitmapDrawable).bitmap)
    }

    @Test
    fun `dialog should use a default tinted icon if favicon is not available`() {
        val fragment = createFragment(icon=null)
        val view = createView(fragment)
        val iconView = view.findViewById<ImageView>(R.id.host_icon)
        verify(fragment).setImageViewTint(iconView)
        assertNotNull(iconView.imageTintList)
    }

    @Test
    fun `the confirm button should be disabled if the password field is empty`() {
        val fragment = createFragment()
        val view = createView(fragment)
        view.findViewById<TextInputEditText>(R.id.password_field).setText("")
        assert(lastSetViewState.confirmButtonEnabled == false)

        view.findViewById<TextInputEditText>(R.id.password_field).setText("some-value")
        assert(lastSetViewState.confirmButtonEnabled == true)
    }

    @Test
    fun `the dialog should update on username changes`() {
        val fragment = createFragment()
        val view = createView(fragment)
        // Changing the username field should result in the fragment matching
        // the new entry against the logins for the site (what was returned by
        // decrypt/getByBaseDomain)
        view.findViewById<TextInputEditText>(R.id.username_field).setText("new-username")
        verify(testLoginsStorage).findLoginToUpdate(
            entry = initialEntry.copy(username="new-username"),
            logins = loginsForOrigin)
    }

    @Test
    fun `the dialog should inform users when they are adding a login`() {
        val fragment = createFragment()
        val view = createView(fragment)

        findLoginToUpdateResult = null;
        view.findViewById<TextInputEditText>(R.id.username_field).setText("new-username")

        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_login_save_headline),
            lastSetViewState.headline,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_never_save),
            lastSetViewState.negativeText,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_save_confirmation),
            lastSetViewState.confirmText,
        )
    }

    @Test
    fun `the dialog should inform users when they are updating a login`() {
        val fragment = createFragment()
        val view = createView(fragment)

        findLoginToUpdateResult = fakeLogin(username="user2")
        view.findViewById<TextInputEditText>(R.id.username_field).setText("user2")

        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_login_update_headline),
            lastSetViewState.headline,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_dont_update),
            lastSetViewState.negativeText,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_update_confirmation),
            lastSetViewState.confirmText,
        )
    }

    @Test
    fun `the dialog should inform users when they are adding a username a login without one`() {
        val fragment = createFragment()
        val view = createView(fragment)

        findLoginToUpdateResult = fakeLogin(username="")
        view.findViewById<TextInputEditText>(R.id.username_field).setText("some-username")

        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_login_add_username_headline),
            lastSetViewState.headline,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_dont_update),
            lastSetViewState.negativeText,
        )
        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_update_confirmation),
            lastSetViewState.confirmText,
        )
    }

    @Test
    fun `the dialog should give preference to the update header the add username`() {
        // Edge case: The username field is blank and there is a saved login
        // with a blank username.  In this scenario we're updating a login AND
        // the login has a blank username.  We should display the update
        // headline since "Add username to password" doesn't make sense in this
        // case.

        val fragment = createFragment()
        val view = createView(fragment)

        findLoginToUpdateResult = fakeLogin(username="")
        view.findViewById<TextInputEditText>(R.id.username_field).setText("")

        assertEquals(
            appCompatContext.getString(R.string.mozac_feature_prompt_login_update_headline),
            lastSetViewState.headline,
        )
    }

    @Test
    fun `dialog should call onConfirm to save logins`() {
        val fragment = createFragment()
        val view = createView(fragment)

        view.findViewById<TextInputEditText>(R.id.username_field).setText("new-user")
        view.findViewById<Button>(R.id.save_confirm).performClick()

        verify(testFeature).onConfirm(
            sessionId, requestUID, initialEntry.copy(username="new-user")
        )
    }
}
