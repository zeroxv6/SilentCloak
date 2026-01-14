package space.zeroxv6.silentcloak.services

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserPresenceService {
    private val onlineUsers = ConcurrentHashMap<String, MutableSet<String>>()

    fun markUserOnline(username: String, sessionId: String) {
        onlineUsers.computeIfAbsent(username) { ConcurrentHashMap.newKeySet() }.add(sessionId)
    }

    fun markUserOffline(username: String, sessionId: String) {
        onlineUsers[username]?.remove(sessionId)
        if (onlineUsers[username]?.isEmpty() == true) {
            onlineUsers.remove(username)
        }
    }

    fun isUserOnline(username: String): Boolean {
        return onlineUsers.containsKey(username) && onlineUsers[username]?.isNotEmpty() == true
    }

    fun getOnlineUsers(): Set<String> {
        return onlineUsers.keys.toSet()
    }
}
