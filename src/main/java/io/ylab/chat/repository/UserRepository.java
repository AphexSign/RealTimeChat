package io.ylab.chat.repository;

import io.ylab.chat.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for managing {@link UserEntity} persistence operations.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Finds a user entity by its username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the found {@link UserEntity}, or empty if no user is
     * found
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Checks whether a user with the specified username exists in the repository.
     *
     * @param username the username to check for existence
     * @return {@code true} if a user with the given username exists, {@code false} otherwise
     */
    boolean existsByUsername(String username);
}