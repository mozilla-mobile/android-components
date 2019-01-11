/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.prompt

import android.content.Context
import android.net.Uri

/**
 * Value type that represents a request for showing a native dialog for prompt web content.
 *
 */
sealed class PromptRequest {
    /**
     * Value type that represents a request for a single choice prompt.
     * @property choices All the possible options.
     * @property onSelect A callback indicating which option was selected.
     */
    data class SingleChoice(val choices: Array<Choice>, val onSelect: (Choice) -> Unit) : PromptRequest()

    /**
     * Value type that represents a request for a multiple choice prompt.
     * @property choices All the possible options.
     * @property onSelect A callback indicating witch options has been selected.
     */
    data class MultipleChoice(val choices: Array<Choice>, val onSelect: (Array<Choice>) -> Unit) : PromptRequest()

    /**
     * Value type that represents a request for a menu choice prompt.
     * @property choices All the possible options.
     * @property onSelect A callback indicating which option was selected.
     */
    data class MenuChoice(val choices: Array<Choice>, val onSelect: (Choice) -> Unit) : PromptRequest()

    /**
     * Value type that represents a request for an alert prompt.
     * @property title of the dialog.
     * @property message the body of the dialog.
     * @property hasShownManyDialogs tells if this page has shown multiple prompts within a short period of time.
     * @property onDismiss callback to let the page know the user dismissed the dialog.
     * @property onShouldShowNoMoreDialogs tells the web page if it should continue showing alerts or not.
     */
    data class Alert(
        val title: String,
        val message: String,
        val hasShownManyDialogs: Boolean = false,
        val onDismiss: () -> Unit,
        val onShouldShowNoMoreDialogs: (Boolean) -> Unit
    ) : PromptRequest()

    /**
     * Value type that represents a request for a date prompt for picking a year, month, and day.
     * @property title of the dialog.
     * @property initialDate date that dialog should be set by default.
     * @property minimumDate date allow to be selected.
     * @property maximumDate date allow to be selected.
     * @property onSelect callback that is called when the date is selected.
     * @property onClear callback that is called when the user requests the picker to be clear up.
     */
    data class Date(
        val title: String,
        val initialDate: java.util.Date,
        val minimumDate: java.util.Date?,
        val maximumDate: java.util.Date?,
        val onSelect: (java.util.Date) -> Unit,
        val onClear: () -> Unit
    ) : PromptRequest()

    /**
     * Value type that represents a request for a selecting one or multiple files.
     * @property mimeTypes a set of allowed mime types. Only these file types can be selected.
     * @property isMultipleFilesSelection true if the user can select more that one file false otherwise.
     * @property onSingleFileSelected callback to notify that the user has selected a single file.
     * @property onMultipleFilesSelected callback to notify that the user has selected multiple files.
     * @property onDismiss callback to notify that the user has canceled the file selection.
     */
    data class File(
        val mimeTypes: Array<out String>,
        val isMultipleFilesSelection: Boolean,
        val onSingleFileSelected: (Context, Uri) -> Unit,
        val onMultipleFilesSelected: (Context, Array<Uri>) -> Unit,
        val onDismiss: () -> Unit
    ) : PromptRequest()

    /**
     * Value type that represents a request for an authentication prompt.
     * For more related info take a look at
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication>MDN docs</a>
     * @property title of the dialog.
     * @property message the body of the dialog.
     * @property userName default value provide for this session.
     * @property password default value provide for this session.
     * @property method type of authentication,  valid values [Method.HOST] and [Method.PROXY].
     * @property level indicates the level of security of the authentication like [Level.NONE],
     * [Level.SECURED] and [Level.PASSWORD_ENCRYPTED].
     * @property onlyShowPassword indicates if the dialog should only include a password field.
     * @property previousFailed indicates if this request is the result of a previous failed attempt to login.
     * @property isCrossOrigin indicates if this request is from a cross-origin sub-resource.
     * @property onConfirm callback to indicate the user want to start the authentication flow.
     * @property onDismiss callback to indicate the user dismissed this request.
     */
    data class Authentication(
        val title: String,
        val message: String,
        val userName: String,
        val password: String,
        val method: Method,
        val level: Level,
        val onlyShowPassword: Boolean = false,
        val previousFailed: Boolean = false,
        val isCrossOrigin: Boolean = false,
        val onConfirm: (String, String) -> Unit,
        val onDismiss: () -> Unit
    ) : PromptRequest() {

        enum class Level {
            NONE, PASSWORD_ENCRYPTED, SECURED
        }

        enum class Method {
            HOST, PROXY
        }
    }
}
