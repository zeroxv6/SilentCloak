package space.zeroxv6.silentcloak.models

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "friendships", uniqueConstraints = [
    UniqueConstraint(columnNames = ["user1_id", "user2_id"])
])
class Friendship(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    val user1: User,

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    val user2: User,

    @Column(nullable = false)
    var accepted: Boolean = false 
) {
    constructor() : this(null, User(), User(), false)
}