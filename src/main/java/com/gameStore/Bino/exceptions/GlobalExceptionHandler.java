package com.gameStore.Bino.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException exception) {
        return new ResponseEntity<>(Map.of("message", exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleBadCredentials(RuntimeException exception) {
        return new ResponseEntity<>(Map.of("message", "Invalid email or password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException exception) {
        return new ResponseEntity<>(Map.of("message", exception.getMessage()), HttpStatus.BAD_REQUEST);
    }
}
