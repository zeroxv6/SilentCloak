package space.zeroxv6.silentcloak.services

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DecryptedContext<T>(val data: T, val sessionKey: SecretKey)

@Service
class CryptoService(
    private val objectMapper: ObjectMapper,
    @Value("\${app.security.keystore-password:ChangeThisToSomethingStrong}")
    private val keystorePassword: String
) {

    private val keystoreFile = File("identity.p12")
    private val keyAlias = "server-identity"
    private lateinit var keyPair: KeyPair

    private val rsaTransformation = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private val aesTransformation = "AES/GCM/NoPadding"

    @PostConstruct
    fun init() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }

        if (keystoreFile.exists() && keystoreFile.length() > 0) {
            try {
                loadKeystore()
                println("Loaded existing keystore from ${keystoreFile.absolutePath}")
            } catch (e: Exception) {
                println("Failed to load keystore, regenerating: ${e.message}")
                keystoreFile.delete()
                generateAndSaveKeystore()
            }
        } else {
            println("Keystore not found, generating new one at ${keystoreFile.absolutePath}")
            generateAndSaveKeystore()
        }
    }

    fun <T> decryptAndValidate(encryptedBase64: String, clazz: Class<T>): DecryptedContext<T> {
        try {
            val fullBytes = Base64.getDecoder().decode(encryptedBase64)
            val encryptedKeyLen = 256
            val ivLen = 12

            if (fullBytes.size < encryptedKeyLen + ivLen) throw RuntimeException("Invalid payload size")

            val encryptedAesKey = fullBytes.copyOfRange(0, encryptedKeyLen)
            val iv = fullBytes.copyOfRange(encryptedKeyLen, encryptedKeyLen + ivLen)
            val encryptedPayload = fullBytes.copyOfRange(encryptedKeyLen + ivLen, fullBytes.size)

            val rsaCipher = Cipher.getInstance(rsaTransformation, "BC")
            rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
            val aesKey = SecretKeySpec(aesKeyBytes, "AES")

            val aesCipher = Cipher.getInstance(aesTransformation, "BC")
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            val jsonBytes = aesCipher.doFinal(encryptedPayload)

            val payload = if (clazz == String::class.java) {
                
                String(jsonBytes, Charsets.UTF_8) as T
            } else {
                
                objectMapper.readValue(jsonBytes, clazz)
            }

            return DecryptedContext(payload, aesKey)

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Decryption Failed: ${e.message}")
        }
    }

    fun encryptResponseForClient(responseObj: Any, sessionKey: SecretKey): String {
        try {
            val jsonBytes = objectMapper.writeValueAsBytes(responseObj)

            val aesCipher = Cipher.getInstance(aesTransformation, "BC")
            aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey)
            val iv = aesCipher.iv
            val encryptedData = aesCipher.doFinal(jsonBytes)

            val output = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, output, 0, iv.size)
            System.arraycopy(encryptedData, 0, output, iv.size, encryptedData.size)

            return Base64.getEncoder().encodeToString(output)

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Encryption of response failed")
        }
    }

    fun getPublicKeyString(): String {
        return Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }

    private fun loadKeystore() {
        val ks = KeyStore.getInstance("PKCS12")
        FileInputStream(keystoreFile).use { ks.load(it, keystorePassword.toCharArray()) }
        val privateKey = ks.getKey(keyAlias, keystorePassword.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(keyAlias)
        keyPair = KeyPair(cert.publicKey, privateKey)
    }

    private fun generateAndSaveKeystore() {
        val kpg = KeyPairGenerator.getInstance("RSA", "BC")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()
        this.keyPair = kp

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(kp.private)
        val now = System.currentTimeMillis()
        val owner = X500Name("CN=SilentCloak Server")
        val certBuilder = JcaX509v3CertificateBuilder(
            owner, BigInteger.valueOf(now), Date(now), Date(now + 315360000000L), owner, kp.public
        )
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer))

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)
        ks.setKeyEntry(keyAlias, kp.private, keystorePassword.toCharArray(), arrayOf(cert))
        FileOutputStream(keystoreFile).use { ks.store(it, keystorePassword.toCharArray()) }
    }
}