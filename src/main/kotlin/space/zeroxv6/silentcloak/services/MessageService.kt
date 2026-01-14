package space.zeroxv6.silentcloak.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import space.zeroxv6.silentcloak.models.Message
import space.zeroxv6.silentcloak.payloads.MessageRequest
import space.zeroxv6.silentcloak.repositories.FriendshipRepository
import space.zeroxv6.silentcloak.repositories.GroupRepository
import space.zeroxv6.silentcloak.repositories.MessageRepository
import space.zeroxv6.silentcloak.repositories.UserRepository
import java.util.UUID

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val friendshipRepository: FriendshipRepository,
    private val cryptoService: CryptoService,
    private val userPresenceService: UserPresenceService,
    private val groupMessageDeliveryRepository: space.zeroxv6.silentcloak.repositories.GroupMessageDeliveryRepository
) {
    
    fun getCryptoService(): CryptoService = cryptoService
    
    fun isUserOnline(username: String): Boolean {
        return userPresenceService.isUserOnline(username)
    }

    @Transactional
    fun sendMessage(senderUsername: String, request: MessageRequest): Message {
        val sender = userRepository.findByUsername(senderUsername)
            ?: throw RuntimeException("Error: Sender not found")

        
        val decryptedContext = cryptoService.decryptAndValidate(
            request.encryptedMetadata,
            space.zeroxv6.silentcloak.payloads.MessageMetadata::class.java
        )
        val metadata = decryptedContext.data

        if (metadata.groupId != null) {
            val group = groupRepository.findById(UUID.fromString(metadata.groupId))
                .orElseThrow { RuntimeException("Group not found") }

            if (!group.members.contains(sender)) {
                throw RuntimeException("You are not a member of this group")
            }

            val message = Message(
                sender = sender,
                group = group,
                encryptedContent = request.encryptedContent
            )

            val savedMsg = messageRepository.save(message)

            
            group.members.forEach { member ->
                if (member.id != sender.id) {
                    val delivery = space.zeroxv6.silentcloak.models.GroupMessageDelivery(
                        message = savedMsg,
                        user = member,
                        delivered = false
                    )
                    groupMessageDeliveryRepository.save(delivery)
                }
            }

            return savedMsg
        }

        else if (metadata.receiverUsername != null) {
            val receiver = userRepository.findByUsername(metadata.receiverUsername)
                ?: throw RuntimeException("Receiver not found")

            val areFriends = friendshipRepository.areFriends(sender, receiver)

            if (!areFriends) {
                throw RuntimeException("Access Denied: You can only send messages to your friends.")
            }

            val message = Message(
                sender = sender,
                receiver = receiver,
                encryptedContent = request.encryptedContent
            )
            return messageRepository.save(message)
        }

        throw RuntimeException("Must provide either receiverUsername or groupId")

    }

    fun getMessagesForUser(username: String): List<Message> {
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("User not found")

        return messageRepository.findByReceiverOrderByTimestampDesc(user)
    }

    @Transactional
    fun getUndeliveredMessages(username: String): List<Message> {
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("User not found")

        
        val undeliveredDirectMessages = messageRepository.findUndeliveredMessages(user)
        
        
        undeliveredDirectMessages.forEach { message ->
            message.delivered = true
            message.deliveredAt = java.time.LocalDateTime.now()
        }
        
        messageRepository.saveAll(undeliveredDirectMessages)
        
        
        val undeliveredGroupDeliveries = groupMessageDeliveryRepository.findUndeliveredGroupMessages(user)
        
        
        undeliveredGroupDeliveries.forEach { delivery ->
            delivery.delivered = true
            delivery.deliveredAt = java.time.LocalDateTime.now()
        }
        
        groupMessageDeliveryRepository.saveAll(undeliveredGroupDeliveries)
        
        
        val allUndeliveredMessages = undeliveredDirectMessages.toMutableList()
        allUndeliveredMessages.addAll(undeliveredGroupDeliveries.map { it.message })
        
        
        return allUndeliveredMessages.sortedBy { it.timestamp }
    }
    fun getGroupMessages(groupId: UUID, username: String): List<Message> {
        val group = groupRepository.findById(groupId)
            .orElseThrow { RuntimeException("Group not found") }

        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("User not found")

        if (!group.members.contains(user)) {
            throw RuntimeException("You are not a member of this group")
        }

        return messageRepository.findByGroupOrderByTimestampDesc(group)
    }
    fun getConversation(username1: String, username2: String): List<Message> {
        val user1 = userRepository.findByUsername(username1)
            ?: throw RuntimeException("User not found")
        val user2 = userRepository.findByUsername(username2)
            ?: throw RuntimeException("User not found")

        return messageRepository.findConversation(user1, user2)
    }
}