package space.zeroxv6.silentcloak.controller

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.zeroxv6.silentcloak.dto.*
import space.zeroxv6.silentcloak.services.AuthService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/register")
    fun register(@RequestBody encryptedRequest: EncryptedRequest): ResponseEntity<EncryptedResponse> {
        logger.info("=== REGISTRATION REQUEST (ENCRYPTED) ===")
        
        return try {
            val cryptoService = authService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                RegisterRequest::class.java
            )
            val request = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            logger.info("Decrypted registration for username length: ${request.username.length}")
            
            val message = authService.registerUser(request)
            logger.info("Registration successful")
            
            val responseData = mapOf("message" to message)
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
            
            ResponseEntity.ok(EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            logger.error("Registration failed: ${e.message}", e)
            ResponseEntity.badRequest().body(
                EncryptedResponse(mapOf("error" to (e.message ?: "Registration failed")).toString())
            )
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody encryptedRequest: EncryptedRequest): ResponseEntity<EncryptedResponse> {
        logger.info("=== LOGIN REQUEST (ENCRYPTED) ===")
        
        return try {
            val cryptoService = authService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                LoginRequest::class.java
            )
            val request = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            logger.info("Decrypted login request")
            
            val response = authService.loginUser(request)
            logger.info("Login successful")
            
            val responseData = mapOf(
                "token" to response.token,
                "encryptedPrivateKey" to response.encryptedPrivateKey,
                "encryptionSalt" to response.encryptionSalt,
                "publicKey" to response.publicKey
            )
            val encryptedResponse = cryptoService.encryptResponseForClient(responseData, sessionKey)
            
            ResponseEntity.ok(EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            logger.error("Login failed: ${e.message}", e)
            ResponseEntity.status(401).body(
                EncryptedResponse(mapOf("error" to "Authentication failed").toString())
            )
        }
    }

    @PostMapping("/key-material")
    fun getKeyMaterial(@RequestBody encryptedRequest: EncryptedRequest): ResponseEntity<EncryptedResponse> {
        logger.info("=== KEY MATERIAL REQUEST (ENCRYPTED) ===")
        
        return try {
            val cryptoService = authService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                String::class.java
            )
            val username = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            logger.info("Decrypted username request")
            
            val keyMaterial = authService.getKeyMaterial(username)
            logger.info("Key material found")
            
            val encryptedResponse = cryptoService.encryptResponseForClient(keyMaterial, sessionKey)
            ResponseEntity.ok(EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            logger.error("Key material not found: ${e.message}", e)
            ResponseEntity.status(404).body(
                EncryptedResponse(mapOf("error" to (e.message ?: "User not found")).toString())
            )
        }
    }

    @PostMapping("/key")
    fun getPublicKey(@RequestBody encryptedRequest: EncryptedRequest): ResponseEntity<EncryptedResponse> {
        logger.info("=== PUBLIC KEY REQUEST (AUTH ENCRYPTED) ===")
        
        return try {
            val cryptoService = authService.getCryptoService()
            val decryptedContext = cryptoService.decryptAndValidate(
                encryptedRequest.encryptedData,
                String::class.java
            )
            val username = decryptedContext.data
            val sessionKey = decryptedContext.sessionKey
            
            logger.info("Decrypted username request")
            
            val publicKey = authService.getPublicKey(username)
            logger.info("Public key found")
            
            val encryptedResponse = cryptoService.encryptResponseForClient(publicKey, sessionKey)
            ResponseEntity.ok(EncryptedResponse(encryptedResponse))
        } catch (e: Exception) {
            logger.error("Public key not found: ${e.message}", e)
            ResponseEntity.status(404).body(
                EncryptedResponse(mapOf("error" to (e.message ?: "User not found")).toString())
            )
        }
    }
}