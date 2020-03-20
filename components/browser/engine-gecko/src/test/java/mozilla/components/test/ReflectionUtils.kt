/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

package mozilla.components.test

import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ReflectionUtils {
    fun <T : Any> setField(instance: T, fieldName: String, value: Any?) {
        val originField = instance.javaClass.getField(fieldName)

        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(originField, originField.modifiers and Modifier.FINAL.inv())

        originField.set(instance, value)
    }
}
