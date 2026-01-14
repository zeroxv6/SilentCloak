package space.zeroxv6.silentcloak.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.zeroxv6.silentcloak.services.GroupService
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/groups")
class GroupController(
    private val groupService: GroupService
) {

    @PostMapping("/create")
    fun createGroup(@RequestParam name: String, principal: Principal): ResponseEntity<Any> {
        return try {
            val group = groupService.createGroup(name, principal.name)
            ResponseEntity.ok(mapOf("id" to group.id, "name" to group.name))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/{groupId}/add")
    fun addMember(
        @PathVariable groupId: UUID,
        @RequestParam username: String
    ): ResponseEntity<String> {
        return try {
            groupService.addMember(groupId, username)
            ResponseEntity.ok("User added to group")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @GetMapping("/my")
    fun getMyGroups(principal: Principal): ResponseEntity<Any> {
        return try {
            val groups = groupService.getUserGroups(principal.name)
            val response = groups.map { mapOf("id" to it.id, "name" to it.name) }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}