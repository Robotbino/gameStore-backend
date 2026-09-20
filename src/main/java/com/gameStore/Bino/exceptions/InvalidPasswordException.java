package com.gameStore.Bino.exceptions;

/**
 * Thrown when a signed-in user supplies the wrong current password while changing it.
 * Mapped to 400 rather than 401: the session itself is still valid, and the frontend
 * treats any 401 outside /auth as an expired session and redirects to login.
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
