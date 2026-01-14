package space.zeroxv6.silentcloak.config

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import space.zeroxv6.silentcloak.services.ApplicationUserDetailService
import space.zeroxv6.silentcloak.utils.JwtUtil

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketSecurityConfig(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: ApplicationUserDetailService
) : WebSocketMessageBrokerConfigurer {

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(object : ChannelInterceptor {
            override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {

                val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

                if (StompCommand.CONNECT == accessor?.command) {

                    val authHeader = accessor.getFirstNativeHeader("Authorization")

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        val token = authHeader.substring(7)

                        try {
                            if (jwtUtil.validateToken(token)) {
                                val username = jwtUtil.getUsernameFromToken(token)
                                val userDetails = userDetailsService.loadUserByUsername(username)
                                val auth = UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.authorities
                                )
                                accessor.user = auth
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                return message
            }
        })
    }
}