package com.enterprise.tokenization.exception;

/**
 * Exception thrown when a blockchain operation fails.
 *
 * This exception should be thrown when there are errors interacting with
 * the Hyperledger Fabric network, such as chaincode invocation failures,
 * network connectivity issues, or transaction submission errors.
 *
 * @author Enterprise Asset Tokenization Platform
 * @version 1.0
 */
public class BlockchainException extends RuntimeException {

    /**
     * Constructs a new BlockchainException with the specified detail message.
     *
     * @param message the detail message explaining the blockchain operation failure
     */
    public BlockchainException(String message) {
        super(message);
    }

    /**
     * Constructs a new BlockchainException with the specified detail message and cause.
     *
     * @param message the detail message explaining the blockchain operation failure
     * @param cause the underlying cause of the blockchain failure
     */
    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }
}