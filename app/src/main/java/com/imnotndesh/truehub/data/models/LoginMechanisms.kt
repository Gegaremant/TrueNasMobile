package com.imnotndesh.truehub.data.models

sealed class LoginMechanisms {
    @Suppress("PropertyName")
    data class AuthApiKeyPlain(
        val mechanism : String = "API_KEY_PLAIN",
        val username : String,
        val api_key : String,
        val login_options : LoginOptions
    ): LoginMechanisms()
    @Suppress("PropertyName")
    data class LoginOptions(
        val user_info : Boolean ?= true
    ): LoginMechanisms()
    @Suppress("PropertyName")
    data class AuthPasswordPlain(
        val mechanism: String = "PASSWORD_PLAIN",
        val username : String,
        val password :String,
        val login_options: LoginOptions
    ): LoginMechanisms()
    @Suppress("PropertyName")
    data class AuthTokenPlain(
        val mechanism: String = "TOKEN_PLAIN",
        val token : String,
        val login_options: LoginOptions
    ): LoginMechanisms()
    @Suppress("PropertyName")
    data class AuthOTPToken(
        val mechanism: String = "OTP_TOKEN",
        val otp_token :String,
        val login_options: LoginOptions
    ): LoginMechanisms()
}
sealed class LoginExResult{
    enum class AuthenticatorLevel{
        LEVEL_1,
        LEVEL_2
    }
    @Suppress("PropertyName")
    data class AuthRespSuccess(
        val response_type : String = "SUCCESS",
        val user_info : Auth.AuthResponse?= null,
        val authenticator : AuthenticatorLevel
    ): LoginExResult()
    @Suppress("PropertyName")
    data class AuthRespAuthErr(
        val response_type: String = "AUTH_ERR"
    ): LoginExResult()
    @Suppress("PropertyName")
    data class AuthRespAuthExpired(
            val response_type: String = "EXPIRED"
    ): LoginExResult()
    @Suppress("PropertyName")
    data class AuthRespOTPRequired(
        val response_type: String = "OTP_REQUIRED",
        val username : String
    ): LoginExResult()
    @Suppress("PropertyName")
    data class AuthRespAuthRedirect(
        val response_type: String = "REDIRECT",
        val urls :  List<String>
    ): LoginExResult()
}

