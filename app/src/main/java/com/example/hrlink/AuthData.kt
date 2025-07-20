package com.example.hrlink

import com.google.gson.annotations.SerializedName

data class MainResponse(
    @SerializedName("JWT") val jwt: String
)

data class JwtPayload(
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)