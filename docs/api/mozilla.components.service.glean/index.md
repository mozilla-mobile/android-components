[android-components](../index.md) / [mozilla.components.service.glean](./index.md)

## Package mozilla.components.service.glean

### Types

| Name | Summary |
|---|---|
| [BooleanMetricType](-boolean-metric-type/index.md) | `data class BooleanMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording boolean metrics. |
| [CommonMetricData](-common-metric-data/index.md) | `interface CommonMetricData`<br>This defines the common set of data shared across all the different metric types. |
| [CounterMetricType](-counter-metric-type/index.md) | `data class CounterMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording counter metrics. |
| [DatetimeMetricType](-datetime-metric-type/index.md) | `data class DatetimeMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording datetime metrics. |
| [EventMetricType](-event-metric-type/index.md) | `data class EventMetricType<ExtraKeysEnum : `[`Enum`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)`<`[`ExtraKeysEnum`](-event-metric-type/index.md#ExtraKeysEnum)`>> : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording events. |
| [Glean](-glean.md) | `object Glean : `[`GleanInternalAPI`](-glean-internal-a-p-i/index.md) |
| [GleanInternalAPI](-glean-internal-a-p-i/index.md) | `open class GleanInternalAPI` |
| [HistogramType](-histogram-type/index.md) | `enum class HistogramType`<br>Enumeration of the different kinds of histograms supported by metrics based on histograms. |
| [LabeledMetricType](-labeled-metric-type/index.md) | `data class LabeledMetricType<T> : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for labeled metrics. |
| [Lifetime](-lifetime/index.md) | `enum class Lifetime`<br>Enumeration of different metric lifetimes. |
| [NoExtraKeys](-no-extra-keys/index.md) | `enum class NoExtraKeys`<br>An enum with no values for convenient use as the default set of extra keys that an [EventMetricType](-event-metric-type/index.md) can accept. |
| [StringListMetricType](-string-list-metric-type/index.md) | `data class StringListMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording string list metrics. |
| [StringMetricType](-string-metric-type/index.md) | `data class StringMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording string metrics. |
| [TimeUnit](-time-unit/index.md) | `enum class TimeUnit`<br>Enumeration of different resolutions supported by the Timespan and TimingDistribution metric types. |
| [TimespanMetricType](-timespan-metric-type/index.md) | `data class TimespanMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording timespans. |
| [TimingDistributionMetricType](-timing-distribution-metric-type/index.md) | `data class TimingDistributionMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording timing distribution metrics. |
| [UuidMetricType](-uuid-metric-type/index.md) | `data class UuidMetricType : `[`CommonMetricData`](-common-metric-data/index.md)<br>This implements the developer facing API for recording uuids. |
