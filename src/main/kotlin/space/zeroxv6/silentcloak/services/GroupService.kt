package space.zeroxv6.silentcloak.services

import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import space.zeroxv6.silentcloak.models.ChatGroup
import space.zeroxv6.silentcloak.repositories.GroupRepository
import space.zeroxv6.silentcloak.repositories.UserRepository
import java.util.UUID

@Service
class GroupService(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository
) {

    fun createGroup(groupName: String, creatorUsername: String) : ChatGroup {
        val creator = userRepository.findByUsername(creatorUsername)
            ?: throw UsernameNotFoundException("Error: Username not found")

        val group = ChatGroup(name = groupName, createdBy = creator)
        group.members.add(creator)

        return groupRepository.save(group)
    }

    fun addMember(groupId: UUID, usernameToAdd: String) {
        val group = groupRepository.findById(groupId)
            .orElseThrow { RuntimeException("Group not found") }

        val user = userRepository.findByUsername(usernameToAdd)
            ?: throw UsernameNotFoundException("Error: Username not found")

        group.members.add(user)
        groupRepository.save(group)
    }

    fun getUserGroups(username: String) : List<ChatGroup> {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Error: Username not found")

        return groupRepository.findByMembersContaining(user)

    }
}