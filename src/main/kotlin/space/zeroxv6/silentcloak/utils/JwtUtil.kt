package space.zeroxv6.silentcloak.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date
import java.util.HashMap
import javax.crypto.SecretKey

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400000}")
    private var jwtExpiration: Long = 86400000

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = parseToken(token)
            !claims.payload.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    fun getUsernameFromToken(token: String): String {
        return parseToken(token).payload.subject
    }

    private fun parseToken(token: String) = Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
}