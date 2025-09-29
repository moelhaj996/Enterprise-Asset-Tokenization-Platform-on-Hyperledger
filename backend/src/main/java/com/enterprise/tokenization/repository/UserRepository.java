package com.enterprise.tokenization.repository;

import com.enterprise.tokenization.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity operations.
 * Provides CRUD operations and custom query methods for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by Ethereum address.
     *
     * @param address the Ethereum address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEthereumAddress(String address);

    /**
     * Check if a user exists with the given username.
     *
     * @param username the username to check
     * @return true if a user exists with the username, false otherwise
     */
    boolean existsByUsername(String username);
}