package space.zeroxv6.silentcloak.services

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import space.zeroxv6.silentcloak.models.Friendship
import space.zeroxv6.silentcloak.repositories.FriendshipRepository
import space.zeroxv6.silentcloak.repositories.UserRepository

@Service
class FriendService (
    private val friendshipRepository: FriendshipRepository,
    private val userRepository: UserRepository,
    private val messagingTemplate: SimpMessagingTemplate
) {
    fun addFriend(requesterUsername: String, targetUsername: String) : String {
        println("DEBUG FriendService: requesterUsername='$requesterUsername' (length=${requesterUsername.length})")
        println("DEBUG FriendService: targetUsername='$targetUsername' (length=${targetUsername.length})")
        
        if (requesterUsername == targetUsername) {
            throw RuntimeException("You cannot add yourself as friend")
        }

        val requester = userRepository.findByUsername(requesterUsername)
            ?: throw RuntimeException("Requester not found")
        
        println("DEBUG FriendService: Looking up target user: '$targetUsername'")
        val target = userRepository.findByUsername(targetUsername)
            ?: throw RuntimeException("User to add not found")

        if (friendshipRepository.relationshipExists(requester, target)) {
            throw RuntimeException("Friend request already sent or you are already friends")
        }

        val friendship = Friendship(
            user1 = requester,
            user2 = target,
            accepted = false
        )
        friendshipRepository.save(friendship)

        
        messagingTemplate.convertAndSendToUser(
            targetUsername,
            "/queue/friend-requests",
            "New friend request from $requesterUsername"
        )

        return "Friend added successfully"
    }

    fun getFriends(username: String) : List<String> {
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("User not found")

        val friendships = friendshipRepository.findAllFriendsForUser(user)

        return friendships.map { f->
            if (f.user1.username == username) f.user2.username else f.user1.username
        }
    }

    fun getPendingRequests(username: String): List<space.zeroxv6.silentcloak.dto.FriendRequest> {
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("User not found")

        val pendingFriendships = friendshipRepository.findPendingRequests(user)

        return pendingFriendships.map { friendship ->
            space.zeroxv6.silentcloak.dto.FriendRequest(
                requestId = friendship.id!!,
                senderUsername = friendship.user1.username
            )
        }
    }

    fun respondToRequest(requestId: java.util.UUID, username: String, accept: Boolean): String {
        val request = friendshipRepository.findById(requestId)
            .orElseThrow { RuntimeException("Friend request not found") }

        if (request.user2.username != username) {
            throw RuntimeException("You are not authorized to respond to this request")
        }

        if (accept) {
            request.accepted = true
            friendshipRepository.save(request)
            
            
            messagingTemplate.convertAndSendToUser(
                request.user1.username,
                "/queue/friend-accepted",
                "Friend request accepted by $username"
            )
            
            
            messagingTemplate.convertAndSendToUser(
                username,
                "/queue/friend-requests",
                "Friend added"
            )
            
            return "Friend request accepted"
        } else {
            friendshipRepository.delete(request)
            return "Friend request rejected"
        }
    }
}