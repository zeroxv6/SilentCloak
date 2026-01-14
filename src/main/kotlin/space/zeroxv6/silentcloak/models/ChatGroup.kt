package space.zeroxv6.silentcloak.models

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "chat_groups")
class ChatGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    var createdBy: User,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_members",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var members: MutableSet<User> = mutableSetOf()
) {
    constructor() : this(null, "", User())
}