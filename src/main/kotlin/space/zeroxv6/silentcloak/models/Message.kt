package space.zeroxv6.silentcloak.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "messages")
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = true)
    var receiver: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = true)
    var group: ChatGroup? = null,

    @Column(name = "encrypted_content", nullable = false, columnDefinition = "TEXT")
    var encryptedContent: String,

    @Column(name = "timestamp", nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now(),

    @Column(name = "delivered", nullable = false)
    var delivered: Boolean = false,

    @Column(name = "delivered_at", nullable = true)
    var deliveredAt: LocalDateTime? = null
) {
    constructor() : this(null, User(), null, null, "", LocalDateTime.now(), false, null)
}