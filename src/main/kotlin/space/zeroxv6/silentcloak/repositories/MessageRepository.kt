package space.zeroxv6.silentcloak.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import space.zeroxv6.silentcloak.models.ChatGroup
import space.zeroxv6.silentcloak.models.Message
import space.zeroxv6.silentcloak.models.User
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {

    fun findByReceiverOrderByTimestampDesc(receiver: User) : List<Message>
    fun findByGroupOrderByTimestampDesc(group: ChatGroup): List<Message>
    
    @Query("SELECT m FROM Message m WHERE m.receiver = :receiver AND m.delivered = false ORDER BY m.timestamp ASC")
    fun findUndeliveredMessages(receiver: User): List<Message>
    
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :u1 AND m.receiver = :u2) OR " +
            "(m.sender = :u2 AND m.receiver = :u1) " +
            "ORDER BY m.timestamp ASC")
    fun findConversation(u1: User, u2: User): List<Message>
}