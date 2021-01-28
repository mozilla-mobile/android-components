/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.ktor.fetch

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * A [HttpClientEngine] implementation that uses the given [Client] implementing `concept-fetch`.
 */
class FetchEngine(
    private val client: Client,
    override val config: FetchEngineConfig
) : HttpClientEngineBase("ktor-fetch-${client.javaClass.simpleName}") {
    @OptIn(KtorExperimentalAPI::class) // config.threadsCount is experimental
    override val dispatcher: CoroutineDispatcher by lazy {
        Executors.newFixedThreadPool(
            config.threadsCount
        ).asCoroutineDispatcher()
    }

    @InternalAPI
    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val request = data.toFetchRequest()
        val response = client.fetch(request)
        return response.toKtorResponse()
    }
}

private fun HttpRequestData.toFetchRequest(): Request {
    return Request(
        url.toString(),
        method = method.toFetchMethod(),
        headers = toFetchHeaders(),
        body = body.toFetchRequestBody()
    )
}

private fun HttpMethod.toFetchMethod(): Request.Method {
    return when (this) {
        HttpMethod.Get -> Request.Method.GET
        HttpMethod.Delete -> Request.Method.DELETE
        HttpMethod.Head -> Request.Method.HEAD
        HttpMethod.Post -> Request.Method.POST
        HttpMethod.Put -> Request.Method.PUT
        HttpMethod.Options -> Request.Method.OPTIONS
        else -> throw NotImplementedError("Unsupported method: ${this.value}")
    }
}

private fun OutgoingContent.toFetchRequestBody(): Request.Body? {
    return when (this) {
        is OutgoingContent.NoContent -> null
        is OutgoingContent.ByteArrayContent -> Request.Body.fromByteArray(bytes())
        is OutgoingContent.ReadChannelContent -> Request.Body(readFrom().toInputStream())
        is OutgoingContent.WriteChannelContent -> throw NotImplementedError()
        is OutgoingContent.ProtocolUpgrade -> throw NotImplementedError()
    }
}

private fun HttpRequestData.toFetchHeaders(): MutableHeaders {
    val fetchHeaders = MutableHeaders()

    headers.forEach { name, values ->
        values.forEach { value ->
            fetchHeaders.append(name, value)
        }
    }

    return fetchHeaders
}

private suspend fun Response.toKtorResponse(): HttpResponseData {
    return HttpResponseData(
        statusCode = HttpStatusCode.fromValue(status),
        requestTime = GMTDate(),
        headers = toKtorHeaders(),
        version = toHttpProtocolVersion(),
        body = body.toKtorBody(),
        callContext = getCallContext()
    )
}

private fun Response.Body.toKtorBody(): Any {
    return string()
}

private fun Response.toKtorHeaders(): Headers {
    val builder = HeadersBuilder()

    headers.forEach { header ->
        builder.append(header.name, header.value)
    }

    return builder.build()
}

private fun Response.toHttpProtocolVersion(): HttpProtocolVersion {
    return HttpProtocolVersion.HTTP_1_1
}

@OptIn(InternalAPI::class)
private suspend fun getCallContext(): CoroutineContext {
    return callContext()
}
