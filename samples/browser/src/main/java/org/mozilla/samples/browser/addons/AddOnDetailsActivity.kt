/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.addons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import mozilla.components.feature.addons.AddOn
import org.mozilla.samples.browser.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * An activity to show the details of an add-on.
 */
class AddOnDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_on_details)
        val addOn = requireNotNull(intent.getParcelableExtra<AddOn>("add_on"))
        bind(addOn)
    }

    private fun bind(addOn: AddOn) {

        title = addOn.translatableName.translate()

        bindDetails(addOn)

        bindAuthors(addOn)

        bindVersion(addOn)

        bindLastUpdated(addOn)

        bindWebsite(addOn)

        bindRating(addOn)
    }

    private fun bindRating(addOn: AddOn) {
        addOn.rating?.let {
            val ratingView = findViewById<RatingBar>(R.id.rating_view)
            val userCountView = findViewById<TextView>(R.id.users_count)

            val ratingContentDescription = getString(R.string.add_on_rating_content_description)
            ratingView.contentDescription = String.format(ratingContentDescription, it.average)
            ratingView.rating = it.average

            userCountView.text = getFormattedAmount(it.reviews)
        }
    }

    private fun bindWebsite(addOn: AddOn) {
        findViewById<View>(R.id.home_page_text).setOnClickListener {
            val intent =
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(addOn.siteUrl))
            startActivity(intent)
        }
    }

    private fun bindLastUpdated(addOn: AddOn) {
        val lastUpdatedView = findViewById<TextView>(R.id.last_updated_text)
        lastUpdatedView.text = formatDate(addOn.updatedAt)
    }

    private fun bindVersion(addOn: AddOn) {
        val versionView = findViewById<TextView>(R.id.version_text)
        versionView.text = addOn.version
    }

    private fun bindAuthors(addOn: AddOn) {
        val authorsView = findViewById<TextView>(R.id.author_text)

        val authorText = addOn.authors.joinToString { author ->
            author.name + " \n"
        }

        authorsView.text = authorText
    }

    private fun bindDetails(addOn: AddOn) {
        val detailsView = findViewById<TextView>(R.id.details)
        val detailsText = addOn.translatableDescription.translate()

        val parsedText = detailsText.replace("\n", "<br/>")
        val text = HtmlCompat.fromHtml(parsedText, HtmlCompat.FROM_HTML_MODE_COMPACT)

        detailsView.text = text
        detailsView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun formatDate(text: String): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return DateFormat.getDateInstance().format(formatter.parse(text)!!)
    }
}
