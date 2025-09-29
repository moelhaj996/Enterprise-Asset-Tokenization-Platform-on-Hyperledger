package com.enterprise.tokenization.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 *
 * This exception should be thrown when a unique constraint violation occurs,
 * such as attempting to create a user with an email that already exists, or
 * creating an asset with a duplicate identifier.
 *
 * @author Enterprise Asset Tokenization Platform
 * @version 1.0
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified detail message.
     *
     * @param message the detail message explaining which resource is duplicated
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}