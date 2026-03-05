package me.marie.pronouns.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import me.marie.pronouns.PronounDbIntegration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object RequestUtil {
    private const val USER_AGENT = "PronounDBIntegrationMod/0.0.1 (GITHUB_REPO)"

    val gson: Gson = GsonBuilder().create()
    private val client = HttpClient.newBuilder().build()

    suspend inline fun <reified T> fetchJson(url: String, json: Gson = gson): Result<T> = runCatching {
        json.fromJson<T>(getString(url).getOrElse { return Result.failure(it) }, T::class.java)
    }

    suspend fun getString(url: String): Result<String> =
        execRequest(createGetRequest(url))
            .mapCatching { response -> response.body() }
            .onFailure { PronounDbIntegration.logger.warn("Failed to fetch from $url: ${it.message}") }

    fun createGetRequest(url: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .GET()
            .build()

    private suspend fun execRequest(request: HttpRequest): Result<HttpResponse<String>> =
        suspendCancellableCoroutine { continuation ->
            PronounDbIntegration.logger.info("Executing request to ${request.uri()}")

            val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())

            continuation.invokeOnCancellation {
                PronounDbIntegration.logger.info("Cancelling HTTP request to ${request.uri()}")
                future.cancel(true)
            }

            future.whenComplete { response, error ->
                if (error != null) {
                    if (continuation.isActive) {
                        PronounDbIntegration.logger.warn("Request failed for ${request.uri()}: ${error.message}")
                        continuation.resume(Result.failure(error)) { _, _, _ -> }
                    }
                } else {
                    if (!continuation.isActive) return@whenComplete

                    if (response.statusCode() in 200..299) {
                        continuation.resume(Result.success(response)) { _, _, _ -> }
                    } else {
                        continuation.resume(
                            Result.failure(
                                InputStreamException(
                                    response.statusCode(),
                                    request.uri().toString()
                                )
                            )
                        ) { _, _, _ -> }
                    }
                }
            }
        }

    class InputStreamException(code: Int, url: String) : Exception("Failed to get input stream from $url: HTTP $code")

}