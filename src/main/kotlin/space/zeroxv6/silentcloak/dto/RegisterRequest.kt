package space.zeroxv6.silentcloak.dto

data class RegisterRequest(
    val username: String,
    val authHash: String,
    val authSalt: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val encryptionSalt: String
)