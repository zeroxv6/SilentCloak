package space.zeroxv6.silentcloak.controllers

import org.springframework.boot.context.properties.bind.Bindable.mapOf
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*
import space.zeroxv6.silentcloak.payloads.MessageRequest
import space.zeroxv6.silentcloak.services.MessageService
import java.security.Principal
import java.util.UUID
import kotlin.collections.mapOf

@RestController
@RequestMapping("/api/messages")
class MessageController(
    private val messageService: MessageService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val messageRepository: space.zeroxv6.silentcloak.repositories.MessageRepository,
    private val groupMessageDeliveryRepository: space.zeroxv6.silentcloak.repositories.GroupMessageDeliveryRepository,
    private val userRepository: space.zeroxv6.silentcloak.repositories.UserRepository
) {

    @MessageMapping("/chat")
    fun processMessage(@Payload encryptedPayload: Map<String, String>, principal: Principal) {
        try {
            val cryptoService = messageService.getCryptoService()
            
            
            val encryptedData = encryptedPayload["encryptedData"] 
                ?: throw RuntimeException("Missing encryptedData")
            
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedData,
                MessageRequest::class.java
            )
            val request = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            val savedMsg = messageService.sendMessage(principal.name, request)

            if (savedMsg.receiver != null) {
                
                if (messageService.isUserOnline(savedMsg.receiver!!.username)) {
                    try {
                        val responseData = mapOf(
                            "id" to savedMsg.id.toString(),
                            "sender" to savedMsg.sender.username,
                            "content" to savedMsg.encryptedContent,
                            "timestamp" to savedMsg.timestamp.toString()
                        )
                        
                        
                        val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
                        
                        messagingTemplate.convertAndSendToUser(
                            savedMsg.receiver!!.username,
                            "/queue/messages",
                            mapOf("encryptedData" to encryptedResponse)
                        )
                        
                        
                        savedMsg.delivered = true
                        savedMsg.deliveredAt = java.time.LocalDateTime.now()
                        messageRepository.save(savedMsg)
                    } catch (e: Exception) {
                        
                    }
                }
                
            }

            else if (savedMsg.group != null) {
                val groupMembers = savedMsg.group!!.members

                groupMembers.forEach { member ->
                    if (member.username != principal.name) {
                        
                        if (messageService.isUserOnline(member.username)) {
                            try {
                                val responseData = mapOf(
                                    "id" to savedMsg.id.toString(),
                                    "sender" to savedMsg.sender.username,
                                    "groupId" to savedMsg.group!!.id.toString(),
                                    "content" to savedMsg.encryptedContent,
                                    "timestamp" to savedMsg.timestamp.toString()
                                )
                                
                                
                                val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
                                
                                messagingTemplate.convertAndSendToUser(
                                    member.username,
                                    "/queue/messages",
                                    mapOf("encryptedData" to encryptedResponse)
                                )
                                
                                
                                val delivery = groupMessageDeliveryRepository.findByMessageAndUser(savedMsg, member)
                                if (delivery != null) {
                                    delivery.delivered = true
                                    delivery.deliveredAt = java.time.LocalDateTime.now()
                                    groupMessageDeliveryRepository.save(delivery)
                                }
                            } catch (e: Exception) {
                                
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            
            println("WebSocket message processing error: ${e.message}")
        }
    }

    @PostMapping("/send")
    fun sendMessage(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            
            
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                MessageRequest::class.java
            )
            val request = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            
            val savedMessage = messageService.sendMessage(principal.name, request)
            
            
            val responseData = mapOf(
                "id" to savedMessage.id.toString(),
                "timestamp" to savedMessage.timestamp.toString(),
                "status" to "sent"
            )
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
            
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }

    @PostMapping("/inbox")
    fun getInbox(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                Map::class.java
            )
            val sessionKey = decryptedContext.sessionKey
            
            val messages = messageService.getMessagesForUser(principal.name)

            val response = messages.map { msg ->
                mapOf(
                    "id" to msg.id.toString(),
                    "sender" to msg.sender.username,
                    "content" to msg.encryptedContent,
                    "timestamp" to msg.timestamp.toString(),
                    "delivered" to msg.delivered
                )
            }

            val encryptedResponse = cryptoService.encryptResponseForClient(
                mapOf("messages" to response),
                sessionKey
            )
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }

    @PostMapping("/undelivered")
    fun getUndeliveredMessages(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                Map::class.java
            )
            val sessionKey = decryptedContext.sessionKey
            
            val messages = messageService.getUndeliveredMessages(principal.name)

            val response = messages.map { msg ->
                val baseMap = mutableMapOf(
                    "id" to msg.id.toString(),
                    "sender" to msg.sender.username,
                    "content" to msg.encryptedContent,
                    "timestamp" to msg.timestamp.toString()
                )
                
                
                if (msg.group != null) {
                    baseMap["groupId"] = msg.group!!.id.toString()
                    baseMap["groupName"] = msg.group!!.name
                }
                
                baseMap.toMap()
            }

            val encryptedResponse = cryptoService.encryptResponseForClient(
                mapOf("messages" to response),
                sessionKey
            )
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }

    @PostMapping("/group")
    fun getGroupMessages(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                Map::class.java
            )
            val groupIdStr = (decryptedContext.data as Map<*, *>)["groupId"] as String
            val groupId = UUID.fromString(groupIdStr)
            val sessionKey = decryptedContext.sessionKey
            
            val messages = messageService.getGroupMessages(groupId, principal.name)

            val response = messages.map { msg ->
                mapOf(
                    "id" to msg.id.toString(),
                    "sender" to msg.sender.username,
                    "content" to msg.encryptedContent,
                    "timestamp" to msg.timestamp.toString()
                )
            }
            
            val encryptedResponse = cryptoService.encryptResponseForClient(
                mapOf("messages" to response),
                sessionKey
            )
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }
    
    @PostMapping("/conversation")
    fun getConversation(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                String::class.java
            )
            val partnerUsername = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            val messages = messageService.getConversation(principal.name, partnerUsername)

            val response = messages.map { msg ->
                mapOf(
                    "id" to msg.id.toString(),
                    "sender" to msg.sender.username,
                    "content" to msg.encryptedContent,
                    "timestamp" to msg.timestamp.toString()
                )
            }
            
            val encryptedResponse = cryptoService.encryptResponseForClient(
                mapOf("messages" to response),
                sessionKey
            )
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }
    
    @PostMapping("/user/online")
    fun checkUserOnlineStatus(
        @RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest,
        principal: Principal
    ): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        return try {
            val cryptoService = messageService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                String::class.java
            )
            val username = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            val isOnline = messageService.isUserOnline(username)
            
            val encryptedResponse = cryptoService.encryptResponseForClient(
                mapOf("online" to isOnline),
                sessionKey
            )
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Unknown error")).toString()
                )
            )
        }
    }
    
    @PostMapping("/mark-delivered/{messageId}")
    fun markMessageAsDelivered(
        @PathVariable messageId: UUID,
        principal: Principal
    ): ResponseEntity<Map<String, String>> {
        return try {
            val message = messageRepository.findById(messageId)
                .orElseThrow { RuntimeException("Message not found") }
            
            
            if (message.receiver?.username != principal.name) {
                return ResponseEntity.status(403).body(mapOf("error" to "Unauthorized"))
            }
            
            message.delivered = true
            message.deliveredAt = java.time.LocalDateTime.now()
            messageRepository.save(message)
            
            ResponseEntity.ok(mapOf("status" to "delivered"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }
}