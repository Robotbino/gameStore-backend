package com.gameStore.Bino.exceptions;

/**
 * Thrown when a create/update would violate a uniqueness rule (duplicate email or
 * username). Distinct from a bare RuntimeException so the global handler can map it
 * to a deliberate 400 without also catching genuine server bugs.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
