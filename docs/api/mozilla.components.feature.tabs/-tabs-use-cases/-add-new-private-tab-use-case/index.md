[android-components](../../../index.md) / [mozilla.components.feature.tabs](../../index.md) / [TabsUseCases](../index.md) / [AddNewPrivateTabUseCase](./index.md)

# AddNewPrivateTabUseCase

`class AddNewPrivateTabUseCase : `[`LoadUrlUseCase`](../../../mozilla.components.feature.session/-session-use-cases/-load-url-use-case/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/tabs/src/main/java/mozilla/components/feature/tabs/TabsUseCases.kt#L82)

### Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | `fun invoke(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>`fun invoke(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, selectTab: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = true, startLoading: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = true, parent: `[`Session`](../../../mozilla.components.browser.session/-session/index.md)`? = null): `[`Session`](../../../mozilla.components.browser.session/-session/index.md)<br>Adds a new private tab and loads the provided URL. |
