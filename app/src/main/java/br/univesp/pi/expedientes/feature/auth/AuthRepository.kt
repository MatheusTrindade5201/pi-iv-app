package br.univesp.pi.expedientes.feature.auth

import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.AuthApi
import br.univesp.pi.expedientes.core.network.api.LoginRequest
import br.univesp.pi.expedientes.core.network.api.RegisterRequest
import br.univesp.pi.expedientes.core.network.apiCall
import br.univesp.pi.expedientes.core.session.SessionManager
import javax.inject.Inject

sealed interface AuthFailure {
    data class Api(
        val message: String,
        val field: AuthField? = null,
    ) : AuthFailure

    data object Network : AuthFailure

    data object Unexpected : AuthFailure
}

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(
        val failure: AuthFailure,
    ) : AuthResult
}

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): AuthResult

    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): AuthResult
}

class DefaultAuthRepository
    @Inject
    constructor(
        private val api: AuthApi,
        private val sessionManager: SessionManager,
    ) : AuthRepository {
        override suspend fun login(
            email: String,
            password: String,
        ): AuthResult =
            when (val result = apiCall { api.login(LoginRequest(email, password)) }) {
                is ApiResult.Success -> {
                    sessionManager.startSession(result.value.accessToken)
                    AuthResult.Success
                }
                else -> AuthResult.Failure(result.toFailure())
            }

        override suspend fun register(
            name: String,
            email: String,
            password: String,
        ): AuthResult =
            when (val result = apiCall { api.register(RegisterRequest(name, email, password, password)) }) {
                is ApiResult.Success -> login(email, password)
                else -> AuthResult.Failure(result.toFailure(conflictField = AuthField.Email))
            }

        private fun ApiResult<*>.toFailure(conflictField: AuthField? = null): AuthFailure =
            when (this) {
                is ApiResult.HttpError -> AuthFailure.Api(message, field = if (code == 409) conflictField else null)
                ApiResult.NetworkError -> AuthFailure.Network
                else -> AuthFailure.Unexpected
            }
    }
