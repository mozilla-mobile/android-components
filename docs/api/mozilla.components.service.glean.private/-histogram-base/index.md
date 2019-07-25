[android-components](../../index.md) / [mozilla.components.service.glean.private](../index.md) / [HistogramBase](./index.md)

# HistogramBase

`interface HistogramBase` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/glean/src/main/java/mozilla/components/service/glean/private/HistogramBase.kt#L11)

A common interface to be implemented by all the histogram-like metric types
supported by the Glean SDK.

### Functions

| Name | Summary |
|---|---|
| [accumulateSamples](accumulate-samples.md) | `abstract fun accumulateSamples(samples: `[`LongArray`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long-array/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Accumulates the provided samples in the metric. |

### Inheritors

| Name | Summary |
|---|---|
| [TimingDistributionMetricType](../-timing-distribution-metric-type/index.md) | `data class TimingDistributionMetricType : `[`CommonMetricData`](../-common-metric-data/index.md)`, `[`HistogramBase`](./index.md)<br>This implements the developer facing API for recording timing distribution metrics. |
