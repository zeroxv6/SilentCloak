package space.zeroxv6.silentcloak.models

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "group_message_delivery")
class GroupMessageDelivery(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    var message: Message,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "delivered", nullable = false)
    var delivered: Boolean = false,

    @Column(name = "delivered_at", nullable = true)
    var deliveredAt: LocalDateTime? = null
) {
    constructor() : this(null, Message(), User(), false, null)
}
