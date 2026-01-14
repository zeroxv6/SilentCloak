package space.zeroxv6.silentcloak.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.zeroxv6.silentcloak.dto.KeyResponse
import space.zeroxv6.silentcloak.repositories.UserRepository
import space.zeroxv6.silentcloak.services.CryptoService

@RestController
@RequestMapping("/api/keys")
class KeyController(
    private val userRepository: UserRepository,
    private val cryptoService: CryptoService
) {
    private val logger = LoggerFactory.getLogger(KeyController::class.java)

    @PostMapping
    fun getUserPublicKey(@RequestBody encryptedRequest: space.zeroxv6.silentcloak.dto.EncryptedRequest): ResponseEntity<space.zeroxv6.silentcloak.dto.EncryptedResponse> {
        logger.info("=== PUBLIC KEY REQUEST (ENCRYPTED) ===")
        
        return try {
            
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData, 
                String::class.java
            )
            val username = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            logger.info("Decrypted username request")
            
            val user = userRepository.findByUsername(username)
            
            if (user == null) {
                logger.warn("User not found")
                val errorResponse = cryptoService.encryptResponseForClient(
                    mapOf("error" to "User not found"),
                    sessionKey
                )
                return ResponseEntity.status(404).body(
                    space.zeroxv6.silentcloak.dto.EncryptedResponse(errorResponse)
                )
            }
            
            logger.info("User found, public key length: ${user.publicKey.length}")
            
            
            val responseData = mapOf(
                "username" to user.username,
                "publicKey" to user.publicKey
            )
            
            
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
            
            logger.info("Response encrypted successfully")
            
            
            ResponseEntity.ok(space.zeroxv6.silentcloak.dto.EncryptedResponse(encryptedResponse))

        } catch (e: Exception) {
            logger.error("Error fetching public key: ${e.message}", e)
            ResponseEntity.badRequest().body(
                space.zeroxv6.silentcloak.dto.EncryptedResponse(
                    mapOf("error" to (e.message ?: "Failed to fetch key")).toString()
                )
            )
        }
    }
}