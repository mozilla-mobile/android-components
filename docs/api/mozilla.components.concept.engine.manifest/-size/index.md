[android-components](../../index.md) / [mozilla.components.concept.engine.manifest](../index.md) / [Size](./index.md)

# Size

`data class Size` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/manifest/Size.kt#L14)

Represents dimensions for an image.
Corresponds to values of the "sizes" HTML attribute.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Size(width: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, height: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`)`<br>Represents dimensions for an image. Corresponds to values of the "sizes" HTML attribute. |

### Properties

| Name | Summary |
|---|---|
| [height](height.md) | `val height: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Height of the image. |
| [width](width.md) | `val width: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Width of the image. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [ANY](-a-n-y.md) | `val ANY: `[`Size`](./index.md)<br>Represents the "any" size. |

### Companion Object Functions

| Name | Summary |
|---|---|
| [parse](parse.md) | `fun parse(raw: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Size`](./index.md)`?`<br>Parse a value from an HTML sizes attribute (512x512, 16x16, etc). Returns null if the value was invalid. |
