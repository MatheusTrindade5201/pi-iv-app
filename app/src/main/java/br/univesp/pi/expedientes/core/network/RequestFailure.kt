package br.univesp.pi.expedientes.core.network

sealed interface RequestFailure {
    data class Api(
        val code: Int,
        val message: String,
    ) : RequestFailure

    data object Network : RequestFailure

    data object Unexpected : RequestFailure
}

fun ApiResult<*>.failureOrNull(): RequestFailure? =
    when (this) {
        is ApiResult.Success -> null
        is ApiResult.HttpError -> RequestFailure.Api(code, message)
        ApiResult.NetworkError -> RequestFailure.Network
        ApiResult.UnexpectedError -> RequestFailure.Unexpected
    }
