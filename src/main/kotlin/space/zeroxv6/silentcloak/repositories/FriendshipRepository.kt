package space.zeroxv6.silentcloak.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import space.zeroxv6.silentcloak.models.Friendship
import space.zeroxv6.silentcloak.models.User
import java.util.UUID

@Repository
interface FriendshipRepository : JpaRepository<Friendship, UUID> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE ((f.user1 = :u1 AND f.user2 = :u2) OR (f.user1 = :u2 AND f.user2 = :u1)) " +
            "AND f.accepted = true")
    fun areFriends(u1: User, u2: User): Boolean

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.user1 = :user OR f.user2 = :user) " +
            "AND f.accepted = true")
    fun findAllFriendsForUser(user: User): List<Friendship>

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE (f.user1 = :u1 AND f.user2 = :u2) OR (f.user1 = :u2 AND f.user2 = :u1)")
    fun relationshipExists(u1: User, u2: User): Boolean

    @Query("SELECT f FROM Friendship f WHERE f.user2 = :user AND f.accepted = false")
    fun findPendingRequests(user: User): List<Friendship>
}