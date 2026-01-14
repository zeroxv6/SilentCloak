package space.zeroxv6.silentcloak.payloads

data class MessageRequest(
    val encryptedMetadata: String, 
    val encryptedContent: String
)

data class MessageMetadata(
    val receiverUsername: String? = null,
    val groupId: String? = null
)