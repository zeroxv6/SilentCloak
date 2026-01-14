package space.zeroxv6.silentcloak.config

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import space.zeroxv6.silentcloak.services.UserPresenceService

@Component
class WebSocketEventListener(
    private val userPresenceService: UserPresenceService
) {
    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val username = headerAccessor.user?.name
        
        if (username != null) {
            userPresenceService.markUserOnline(username, headerAccessor.sessionId!!)
            logger.info("User connected: $username with session: ${headerAccessor.sessionId}")
        }
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val username = headerAccessor.user?.name
        
        if (username != null) {
            userPresenceService.markUserOffline(username, headerAccessor.sessionId!!)
            logger.info("User disconnected: $username with session: ${headerAccessor.sessionId}")
        }
    }
}
