package space.zeroxv6.silentcloak.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import space.zeroxv6.silentcloak.models.GroupMessageDelivery
import space.zeroxv6.silentcloak.models.Message
import space.zeroxv6.silentcloak.models.User
import java.util.UUID

interface GroupMessageDeliveryRepository : JpaRepository<GroupMessageDelivery, UUID> {
    
    @Query("SELECT gmd FROM GroupMessageDelivery gmd WHERE gmd.user = :user AND gmd.delivered = false ORDER BY gmd.message.timestamp ASC")
    fun findUndeliveredGroupMessages(user: User): List<GroupMessageDelivery>
    
    fun findByMessageAndUser(message: Message, user: User): GroupMessageDelivery?
    
    @Query("SELECT gmd FROM GroupMessageDelivery gmd WHERE gmd.message.group.id = :groupId AND gmd.user = :user ORDER BY gmd.message.timestamp ASC")
    fun findGroupMessagesForUser(groupId: UUID, user: User): List<GroupMessageDelivery>
}
