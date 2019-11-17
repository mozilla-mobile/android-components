[android-components](../../../index.md) / [mozilla.components.concept.engine](../../index.md) / [EngineSession](../index.md) / [SafeBrowsingPolicy](./index.md)

# SafeBrowsingPolicy

`enum class SafeBrowsingPolicy` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/EngineSession.kt#L119)

Represents a safe browsing policy, which is indicates with type of site should be alerted
to user as possible harmful.

### Enum Values

| Name | Summary |
|---|---|
| [NONE](-n-o-n-e.md) |  |
| [MALWARE](-m-a-l-w-a-r-e.md) | Blocks malware sites. |
| [UNWANTED](-u-n-w-a-n-t-e-d.md) | Blocks unwanted sites. |
| [HARMFUL](-h-a-r-m-f-u-l.md) | Blocks harmful sites. |
| [PHISHING](-p-h-i-s-h-i-n-g.md) | Blocks phishing sites. |
| [RECOMMENDED](-r-e-c-o-m-m-e-n-d-e-d.md) | Blocks all unsafe sites. |

### Properties

| Name | Summary |
|---|---|
| [id](id.md) | `val id: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
