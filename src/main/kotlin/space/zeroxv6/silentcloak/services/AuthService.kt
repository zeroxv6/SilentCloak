package space.zeroxv6.silentcloak.services

import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import space.zeroxv6.silentcloak.dto.LoginRequest
import space.zeroxv6.silentcloak.dto.LoginResponse
import space.zeroxv6.silentcloak.models.User
import space.zeroxv6.silentcloak.dto.RegisterRequest
import space.zeroxv6.silentcloak.repositories.UserRepository
import space.zeroxv6.silentcloak.utils.JwtUtil

@Service
class AuthService (
    private val userRepository: UserRepository,
    private val argon2Service: Argon2Service,
    private val jwtUtil: JwtUtil,
    private val cryptoService: CryptoService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    
    fun getCryptoService(): CryptoService = cryptoService
    
    fun registerUser(request: RegisterRequest): String {
        logger.info("AuthService: Registering user ${request.username}")
        
        if (userRepository.existsByUsername(request.username)) {
            logger.warn("AuthService: Username already exists: ${request.username}")
            throw RuntimeException("Error: Username already taken")
        }

        val newUser = User(
            username = request.username,
            authHash = request.authHash,
            authSalt = request.authSalt,
            publicKey = request.publicKey,
            encryptedPrivateKey = request.encryptedPrivateKey,
            encryptionSalt = request.encryptionSalt
        )

        userRepository.save(newUser)
        logger.info("AuthService: User saved successfully: ${request.username}")

        return "User Registered Successfully"
    }

    fun loginUser(request: LoginRequest): LoginResponse {
        logger.info("AuthService: Login attempt for ${request.username}")
        
        val user = userRepository.findByUsername(request.username)
        if (user == null) {
            logger.warn("AuthService: User not found: ${request.username}")
            throw UsernameNotFoundException("Error: User Not Found")
        }
        
        logger.info("AuthService: User found, verifying auth hash")
        logger.info("AuthService: Stored authHash length: ${user.authHash.length}")
        logger.info("AuthService: Provided authHash length: ${request.authHash.length}")

        if (!argon2Service.verifyAuthHash(request.authHash, user.authHash)) {
            logger.warn("AuthService: Invalid credentials for ${request.username}")
            throw RuntimeException("Error: Invalid credentials")
        }

        logger.info("AuthService: Auth hash verified successfully")
        val token = jwtUtil.generateToken(user.username)
        logger.info("AuthService: JWT token generated")

        return LoginResponse(
            token = token,
            encryptedPrivateKey = user.encryptedPrivateKey,
            encryptionSalt = user.encryptionSalt,
            publicKey = user.publicKey
        )
    }

    fun getKeyMaterial(username: String): Map<String, String> {
        logger.info("AuthService: Fetching key material for $username")
        
        val user = userRepository.findByUsername(username)
        if (user == null) {
            logger.warn("AuthService: User not found for key material: $username")
            throw UsernameNotFoundException("User not found")
        }
        
        logger.info("AuthService: Key material found for $username")
        return mapOf(
            "encryptedPrivateKey" to user.encryptedPrivateKey,
            "encryptionSalt" to user.encryptionSalt,
            "authSalt" to user.authSalt
        )
    }

    fun getPublicKey(username: String): Map<String, String> {
        logger.info("AuthService: Fetching public key for $username")
        
        val user = userRepository.findByUsername(username)
        if (user == null) {
            logger.warn("AuthService: User not found for public key: $username")
            throw UsernameNotFoundException("User not found")
        }
        
        logger.info("AuthService: Public key found for $username (length: ${user.publicKey.length})")
        return mapOf(
            "publicKey" to user.publicKey
        )
    }
}