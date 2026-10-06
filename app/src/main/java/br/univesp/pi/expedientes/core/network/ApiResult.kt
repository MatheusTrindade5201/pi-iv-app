package br.univesp.pi.expedientes.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

val ApiJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ApiResult<T>

    data class HttpError(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>

    data object NetworkError : ApiResult<Nothing>

    data object UnexpectedError : ApiResult<Nothing>
}

suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.HttpError(e.code(), errorMessage(e.response()?.errorBody()?.string(), e.code()))
    } catch (_: IOException) {
        ApiResult.NetworkError
    } catch (_: SerializationException) {
        ApiResult.UnexpectedError
    }

const val VALIDATION_MESSAGE = "Dados inválidos"

fun errorMessage(
    body: String?,
    code: Int,
): String {
    val detail =
        body
            ?.let { runCatching { ApiJson.parseToJsonElement(it) }.getOrNull() }
            ?.let { it as? JsonObject }
            ?.get("detail")
    return when (detail) {
        is JsonPrimitive -> detail.contentOrNull ?: fallbackMessage(code)
        is JsonArray -> validationMessage(detail)
        else -> fallbackMessage(code)
    }
}

private fun validationMessage(errors: JsonArray): String {
    val fields =
        errors
            .mapNotNull { error ->
                error.jsonObject["loc"]
                    ?.jsonArray
                    ?.lastOrNull()
                    ?.jsonPrimitive
                    ?.contentOrNull
            }.filter { it != "body" }
            .distinct()
    return if (fields.isEmpty()) VALIDATION_MESSAGE else "$VALIDATION_MESSAGE: ${fields.joinToString(", ")}"
}

private fun fallbackMessage(code: Int): String = "Erro inesperado ($code)"

fun ApiResult<*>.asFailure(): ApiResult<Nothing> =
    when (this) {
        is ApiResult.Success -> error("Resultado de sucesso não é uma falha")
        is ApiResult.HttpError -> ApiResult.HttpError(code, message)
        ApiResult.NetworkError -> ApiResult.NetworkError
        ApiResult.UnexpectedError -> ApiResult.UnexpectedError
    }
