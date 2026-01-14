package space.zeroxv6.silentcloak.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        
        logger.info("╔════════════════════════════════════════════════════════════")
        logger.info("║ INCOMING REQUEST")
        logger.info("║ Method: ${request.method}")
        logger.info("║ URI: ${request.requestURI}")
        logger.info("║ Query: ${request.queryString ?: "none"}")
        logger.info("║ Remote: ${request.remoteAddr}")
        logger.info("║ Headers:")
        request.headerNames.toList().forEach { headerName ->
            if (headerName.lowercase() != "authorization") {
                logger.info("║   $headerName: ${request.getHeader(headerName)}")
            } else {
                logger.info("║   $headerName: [REDACTED]")
            }
        }
        logger.info("╚════════════════════════════════════════════════════════════")

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logger.info("╔════════════════════════════════════════════════════════════")
            logger.info("║ RESPONSE")
            logger.info("║ Status: ${response.status}")
            logger.info("║ Duration: ${duration}ms")
            logger.info("╚════════════════════════════════════════════════════════════")
        }
    }
}
