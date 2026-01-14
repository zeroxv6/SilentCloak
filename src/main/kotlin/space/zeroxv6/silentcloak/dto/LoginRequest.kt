package space.zeroxv6.silentcloak.dto

data class LoginRequest(
    val username: String,
    val authHash: String
)

data class LoginResponse(
    val token: String,
    val encryptedPrivateKey: String,
    val encryptionSalt: String,
    val publicKey: String
)