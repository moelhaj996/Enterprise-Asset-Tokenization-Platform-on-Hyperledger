package com.enterprise.tokenization.exception;

/**
 * Exception thrown when a requested entity is not found in the database.
 *
 * This exception should be thrown when attempting to retrieve, update, or delete
 * an entity that does not exist in the system.
 *
 * @author Enterprise Asset Tokenization Platform
 * @version 1.0
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Constructs a new EntityNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining which entity was not found
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
}