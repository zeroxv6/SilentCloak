package space.zeroxv6.silentcloak.models

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
class User (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    var id: UUID? = null,

    @Column(name = "username", nullable = false, unique = true)
    var username: String,

    @Column(name = "auth_hash", nullable = false)
    var authHash: String,

    @Column(name = "auth_salt", nullable = false)
    var authSalt: String,

    @Column(name = "public_key", nullable = false, length = 2048)
    var publicKey: String,

    @Column(name = "encrypted_private_key", nullable = false, length = 4096)
    var encryptedPrivateKey: String,

    @Column(name = "encryption_salt", nullable = false)
    var encryptionSalt: String
){
    constructor(): this(null, "", "", "", "", "", "")
}