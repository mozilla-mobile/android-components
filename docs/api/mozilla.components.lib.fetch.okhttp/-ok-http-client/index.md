[android-components](../../index.md) / [mozilla.components.lib.fetch.okhttp](../index.md) / [OkHttpClient](./index.md)

# OkHttpClient

`class OkHttpClient : `[`Client`](../../mozilla.components.concept.fetch/-client/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/fetch-okhttp/src/main/java/mozilla/components/lib/fetch/okhttp/OkHttpClient.kt#L28)

[Client](../../mozilla.components.concept.fetch/-client/index.md) implementation using OkHttp.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `OkHttpClient(client: OkHttpClient = OkHttpClient(), context: <ERROR CLASS>? = null)`<br>[Client](../../mozilla.components.concept.fetch/-client/index.md) implementation using OkHttp. |

### Inherited Properties

| Name | Summary |
|---|---|
| [defaultHeaders](../../mozilla.components.concept.fetch/-client/default-headers.md) | `val defaultHeaders: `[`Headers`](../../mozilla.components.concept.fetch/-headers/index.md)<br>List of default headers that should be added to every request unless overridden by the headers in the request. |

### Functions

| Name | Summary |
|---|---|
| [fetch](fetch.md) | `fun fetch(request: `[`Request`](../../mozilla.components.concept.fetch/-request/index.md)`): `[`Response`](../../mozilla.components.concept.fetch/-response/index.md)<br>Starts the process of fetching a resource from the network as described by the [Request](../../mozilla.components.concept.fetch/-request/index.md) object. This call is synchronous. |

### Companion Object Functions

| Name | Summary |
|---|---|
| [getOrCreateCookieManager](get-or-create-cookie-manager.md) | `fun getOrCreateCookieManager(): `[`CookieManager`](https://developer.android.com/reference/java/net/CookieManager.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [withInterceptors](../../mozilla.components.concept.fetch.interceptor/with-interceptors.md) | `fun `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)`.withInterceptors(vararg interceptors: `[`Interceptor`](../../mozilla.components.concept.fetch.interceptor/-interceptor/index.md)`): `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)<br>Creates a new [Client](../../mozilla.components.concept.fetch/-client/index.md) instance that will use the provided list of [Interceptor](../../mozilla.components.concept.fetch.interceptor/-interceptor/index.md) instances. |
