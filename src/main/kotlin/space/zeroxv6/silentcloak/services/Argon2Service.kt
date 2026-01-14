package space.zeroxv6.silentcloak.services

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class Argon2Service {
    
    companion object {
        private const val HASH_LENGTH = 32
        private const val MEMORY_COST = 65536
        private const val ITERATIONS = 3
        private const val PARALLELISM = 1
    }

    fun verifyAuthHash(providedHash: String, storedHash: String): Boolean {
        return providedHash == storedHash
    }

    fun hashToBase64(input: ByteArray, salt: ByteArray): String {
        val hash = generateArgon2Hash(input, salt)
        return Base64.getEncoder().encodeToString(hash)
    }

    private fun generateArgon2Hash(input: ByteArray, salt: ByteArray): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withMemoryAsKB(MEMORY_COST)
            .withIterations(ITERATIONS)
            .withParallelism(PARALLELISM)

        val generator = Argon2BytesGenerator()
        generator.init(builder.build())

        val hash = ByteArray(HASH_LENGTH)
        generator.generateBytes(input, hash)

        return hash
    }
}
