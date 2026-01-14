package space.zeroxv6.silentcloak

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SilentCloakApplication

fun main(args: Array<String>) {
    runApplication<SilentCloakApplication>(*args)
}
