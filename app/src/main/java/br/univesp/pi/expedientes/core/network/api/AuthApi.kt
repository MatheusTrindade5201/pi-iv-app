package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirm") val passwordConfirm: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("created_at") val createdAt: Timestamp,
)

@Serializable
data class UserUpdateRequest(
    val name: String,
)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest,
    ): UserResponse

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest,
    ): TokenResponse
}

interface UserApi {
    @GET("users/me")
    suspend fun me(): UserResponse

    @PATCH("users/me")
    suspend fun updateMe(
        @Body body: UserUpdateRequest,
    ): UserResponse
}
