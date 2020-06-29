[android-components](../index.md) / [mozilla.components.concept.menu.candidate](index.md) / [MenuIconEffect](./-menu-icon-effect.md)

# MenuIconEffect

`sealed class MenuIconEffect : `[`MenuEffect`](-menu-effect.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/menu/src/main/java/mozilla/components/concept/menu/candidate/MenuEffect.kt#L25)

Describes an effect for a menu icon.
Effects can also alter the button that opens the menu.

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |

### Inheritors

| Name | Summary |
|---|---|
| [LowPriorityHighlightEffect](-low-priority-highlight-effect/index.md) | `data class LowPriorityHighlightEffect : `[`MenuIconEffect`](./-menu-icon-effect.md)<br>Displays a notification dot. Used for highlighting new features to the user, such as what's new or a recommended feature. |
