package com.sonbum.diacalendar2.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    val email: String,
    val password: String
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    val user: AuthUser?
)

data class AuthUser(
    val id: String,
    val email: String?
)

data class ProfileDto(
    val id: String,
    val nickname: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class ProfileUpsertDto(
    val id: String,
    val nickname: String
)

data class PasswordRecoverRequest(
    val email: String
)

data class PasswordUpdateRequest(
    val password: String
)
