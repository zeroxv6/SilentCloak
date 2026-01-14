package space.zeroxv6.silentcloak.dto

import java.util.UUID

data class FriendRequest(
    val requestId: UUID,
    val senderUsername: String
)

data class FriendResponseRequest(
    val requestId: UUID,
    val accept: Boolean
)

data class AddFriendRequest(
    val username: String
)