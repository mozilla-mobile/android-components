/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.toolbar

import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * A customizable action menu for browser toolbar.
 *
 * @param items The list of action menu items
 * @param menuExpanded If the menu is expended then true, else false
 */
@Composable
fun BrowserToolbarActionMenu(
    items: List<BrowserToolbarActionMenuItem>,
    menuExpanded: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    IconButton(onClick = { menuExpanded.value = true }) {
        Icon(Icons.Default.MoreVert, "Menu")
    }

    DropdownMenu(
        expanded = menuExpanded.value,
        onDismissRequest = { menuExpanded.value = false }
    ) {
        for (item in items) {
            DropdownMenuItem(onClick = item.onClick) {
                Text(item.name)
            }
        }
    }
}

/**
 * A menu item in action menu.
 */
data class BrowserToolbarActionMenuItem(
    val name: String,
    val onClick: () -> Unit
)
