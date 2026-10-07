package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.core.session.SessionExpiry
import br.univesp.pi.expedientes.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

private val PUBLIC_PATHS = listOf("/auth/login", "/auth/register")

class AuthInterceptor
    @Inject
    constructor(
        private val sessionManager: SessionManager,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val token = sessionManager.currentToken
            val isPublic = PUBLIC_PATHS.any { request.url.encodedPath.endsWith(it) }
            if (token == null || isPublic) return chain.proceed(request)
            return chain.proceed(
                request
                    .newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build(),
            )
        }
    }

class UnauthorizedInterceptor
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val expiry: SessionExpiry,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code == 401 && request.header("Authorization") != null) {
                if (sessionManager.currentToken != null) expiry.expire()
                sessionManager.logout()
            }
            return response
        }
    }
