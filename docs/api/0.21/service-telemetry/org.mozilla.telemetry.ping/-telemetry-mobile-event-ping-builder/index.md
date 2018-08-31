---
title: TelemetryMobileEventPingBuilder - 
---

[org.mozilla.telemetry.ping](../index.html) / [TelemetryMobileEventPingBuilder](./index.html)

# TelemetryMobileEventPingBuilder

`open class TelemetryMobileEventPingBuilder : `[`TelemetryPingBuilder`](../-telemetry-ping-builder/index.html)

A telemetry ping builder for events of type "mobile-event". See the schema for more details: https://github.com/mozilla-services/mozilla-pipeline-schemas/blob/master/schemas/telemetry/mobile-event/mobile-event.1.schema.json

### Constructors

| [&lt;init&gt;](-init-.html) | `TelemetryMobileEventPingBuilder(configuration: `[`TelemetryConfiguration`](../../org.mozilla.telemetry.config/-telemetry-configuration/index.html)`)` |

### Properties

| [TYPE](-t-y-p-e.html) | `static val TYPE: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Functions

| [canBuild](can-build.html) | `open fun canBuild(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [getEventsMeasurement](get-events-measurement.html) | `open fun getEventsMeasurement(): `[`EventsMeasurement`](../../org.mozilla.telemetry.measurement/-events-measurement/index.html) |

### Inherited Functions

| [build](../-telemetry-ping-builder/build.html) | `open fun build(): `[`TelemetryPing`](../-telemetry-ping/index.html) |
| [generateDocumentId](../-telemetry-ping-builder/generate-document-id.html) | `open fun generateDocumentId(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getConfiguration](../-telemetry-ping-builder/get-configuration.html) | `open fun getConfiguration(): `[`TelemetryConfiguration`](../../org.mozilla.telemetry.config/-telemetry-configuration/index.html) |
| [getType](../-telemetry-ping-builder/get-type.html) | `open fun getType(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

