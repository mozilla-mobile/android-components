/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts.login

import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.feature.prompts.R
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.support.test.fakes.fakeLoginEntry
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BasicLoginAdapterTest {
    val login = fakeLoginEntry(username = "username")
    val login2 = fakeLoginEntry(username = "username2")

    @Test
    fun `getItemCount should return the number of logins`() {
        var onLoginSelected: (LoginEntry) -> Unit = { }

        val adapter = BasicLoginAdapter(onLoginSelected)

        Assert.assertEquals(0, adapter.itemCount)

        adapter.submitList(
            listOf(login, login2)
        )
        Assert.assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `creates and binds login viewholder`() {
        var confirmedLogin: LoginEntry? = null
        var onLoginSelected: (LoginEntry) -> Unit = { confirmedLogin = it }

        val adapter = BasicLoginAdapter(onLoginSelected)

        adapter.submitList(listOf(login, login2))

        val holder = adapter.createViewHolder(FrameLayout(testContext), 0)
        adapter.bindViewHolder(holder, 0)

        Assert.assertEquals(login, holder.login)
        val userName = holder.itemView.findViewById<TextView>(R.id.username)
        Assert.assertEquals(login.username, userName.text)
        val password = holder.itemView.findViewById<TextView>(R.id.password)
        Assert.assertEquals(login.password.length, password.text.length)
        Assert.assertTrue(holder.itemView.isClickable)

        holder.itemView.performClick()

        Assert.assertEquals(confirmedLogin, login)
    }
}
