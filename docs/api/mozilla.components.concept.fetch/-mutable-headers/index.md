[android-components](../../index.md) / [mozilla.components.concept.fetch](../index.md) / [MutableHeaders](./index.md)

# MutableHeaders

`class MutableHeaders : `[`Headers`](../-headers/index.md)`, `[`MutableIterable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterable/index.html)`<`[`Header`](../-header/index.md)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/fetch/src/main/java/mozilla/components/concept/fetch/Headers.kt#L74)

A collection of HTTP [Headers](../-headers/index.md) (mutable) of a [Request](../-request/index.md) or [Response](../-response/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MutableHeaders(vararg pairs: `[`Pair`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>)`<br>A collection of HTTP [Headers](../-headers/index.md) (mutable) of a [Request](../-request/index.md) or [Response](../-response/index.md). |

### Properties

| Name | Summary |
|---|---|
| [size](size.md) | `val size: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Returns the number of headers (key / value combinations). |

### Functions

| Name | Summary |
|---|---|
| [append](append.md) | `fun append(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, value: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`MutableHeaders`](./index.md)<br>Append a header without removing the headers already present. |
| [contains](contains.md) | `operator fun contains(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if a [Header](../-header/index.md) with the given [name](contains.md#mozilla.components.concept.fetch.MutableHeaders$contains(kotlin.String)/name) exists. |
| [get](get.md) | `fun get(index: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`Header`](../-header/index.md)<br>Gets the [Header](../-header/index.md) at the specified [index](get.md#mozilla.components.concept.fetch.MutableHeaders$get(kotlin.Int)/index).`fun get(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>Returns the last value corresponding to the specified header field name. Or null if the header does not exist. |
| [getAll](get-all.md) | `fun getAll(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>`<br>Returns the list of values corresponding to the specified header field name. |
| [iterator](iterator.md) | `fun iterator(): `[`MutableIterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-iterator/index.html)`<`[`Header`](../-header/index.md)`>`<br>Returns an iterator over the headers that supports removing elements during iteration. |
| [set](set.md) | `fun set(index: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, header: `[`Header`](../-header/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Sets the [Header](../-header/index.md) at the specified [index](set.md#mozilla.components.concept.fetch.MutableHeaders$set(kotlin.Int, mozilla.components.concept.fetch.Header)/index).`fun set(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, value: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`MutableHeaders`](./index.md)<br>Set the only occurrence of the header; potentially overriding an already existing header. |
