package com.enterprise.tokenization.security;

import com.enterprise.tokenization.model.User;
import com.enterprise.tokenization.model.UserRole;
import com.enterprise.tokenization.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * Loads user-specific data from the database for authentication and authorization.
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads user details by username for authentication.
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        if (!user.getEnabled()) {
            log.warn("User account is disabled: {}", username);
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        log.debug("Successfully loaded user: {} with role: {}", username, user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.getEnabled(),
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                mapRoleToAuthorities(user.getRole())
        );
    }

    /**
     * Maps a UserRole enum to Spring Security GrantedAuthority collection.
     * Converts the user's role into a format that Spring Security can use for authorization.
     *
     * @param role the user's role
     * @return collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> mapRoleToAuthorities(UserRole role) {
        String authority = "ROLE_" + role.name();
        log.debug("Mapping role {} to authority: {}", role, authority);
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}