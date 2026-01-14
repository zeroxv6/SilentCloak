package space.zeroxv6.silentcloak.dto

data class EncryptedRequest(
    val encryptedData: String
)

data class EncryptedResponse(
    val encryptedResponseData: String
)


data class SecureLoginRequest(
    val username: String,
    val password: String,
    val publicKeyIdentity: String? = null,
    val publicKeyPre: String? = null,
    val timestamp: Long
)

data class SecureRegisterRequest(
    val username: String,
    val password: String,
    val publicKeyIdentity: String,
    val publicKeyPre: String,
    val timestamp: Long
)