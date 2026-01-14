package space.zeroxv6.silentcloak.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import space.zeroxv6.silentcloak.services.FriendService

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val friendService: FriendService,
    private val cryptoService: space.zeroxv6.silentcloak.services.CryptoService
) {

    @PostMapping("/add")
    fun addFriend(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: java.security.Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                space.zeroxv6.silentcloak.dto.AddFriendRequest::class.java
            )
            val username = decryptedContext.data.username.trim()
            val sessionKey = decryptedContext.sessionKey

            println("DEBUG: Attempting to add friend. Requester: '${principal.name}', Target: '$username'")

            val result = friendService.addFriend(principal.name, username)
            
            val responseData = mapOf("message" to result)
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)

            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            println("DEBUG: Error adding friend: ${e.message}")
            e.printStackTrace()
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Failed to add friend")).toString()
                )
            )
        }
    }

    @PostMapping("/list")
    fun getFriends(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: java.security.Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                Map::class.java
            )
            val sessionKey = decryptedContext.sessionKey

            val friends = friendService.getFriends(principal.name)
            
            val responseData = mapOf("friends" to friends)
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)

            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Failed to get friends")).toString()
                )
            )
        }
    }


    @PostMapping("/requests")
    fun getPendingRequests(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: java.security.Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                Map::class.java
            )
            val sessionKey = decryptedContext.sessionKey

            val requests = friendService.getPendingRequests(principal.name)
            
            val responseData = mapOf("requests" to requests)
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)

            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Failed to get requests")).toString()
                )
            )
        }
    }

    @PostMapping("/respond")
    fun respondToFriendRequest(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: java.security.Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                space.zeroxv6.silentcloak.dto.FriendResponseRequest::class.java
            )
            val request = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey

            val result = friendService.respondToRequest(request.requestId, principal.name, request.accept)
            
            val responseData = mapOf("message" to result)
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)

            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Failed to respond to request")).toString()
                )
            )
        }
    }
}