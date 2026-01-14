package space.zeroxv6.silentcloak.repositories

import org.springframework.data.jpa.repository.JpaRepository
import space.zeroxv6.silentcloak.models.ChatGroup
import space.zeroxv6.silentcloak.models.User
import java.util.UUID

interface GroupRepository : JpaRepository<ChatGroup, UUID> {
    fun findByMembersContaining(user: User): List<ChatGroup>
}