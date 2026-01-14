package space.zeroxv6.silentcloak.services

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import space.zeroxv6.silentcloak.repositories.UserRepository

@Service
class ApplicationUserDetailService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val myDbUser = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Error: User not found")

        return org.springframework.security.core.userdetails.User.builder()
            .username(myDbUser.username)
            .password(myDbUser.authHash)
            .roles("USER")
            .build()
    }

}