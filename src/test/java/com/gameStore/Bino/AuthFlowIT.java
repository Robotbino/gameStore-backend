package com.gameStore.Bino;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Register / authenticate flows and their error mappings. */
class AuthFlowIT extends AbstractIntegrationTest {

    private String json(Object o) throws Exception {
        return new ObjectMapper().writeValueAsString(o);
    }

    @Test
    void register_returnsToken() throws Exception {
        mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "userName", "alice",
                                "email", "alice@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        registerAndGetToken("bob", "bob@example.com", "password123");

        mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "userName", "bob2",
                                "email", "bob@example.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    @Test
    void register_invalidEmailAndShortPassword_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "userName", "carol",
                                "email", "not-an-email",
                                "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void authenticate_wrongPassword_returns401WithVagueMessage() throws Exception {
        registerAndGetToken("dave", "dave@example.com", "password123");

        mockMvc.perform(post("/api/v2/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "dave@example.com",
                                "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void authenticate_correctPassword_returnsToken() throws Exception {
        registerAndGetToken("erin", "erin@example.com", "password123");

        mockMvc.perform(post("/api/v2/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "erin@example.com",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void authenticate_blankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v2/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "", "password", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")));
    }
}
