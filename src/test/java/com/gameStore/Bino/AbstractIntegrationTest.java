package com.gameStore.Bino;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared plumbing for the endpoint integration tests. Runs the full application
 * context against the in-memory H2 database configured in
 * src/test/resources/application.properties — no MySQL, no real secrets.
 *
 * @Transactional rolls back every test method, so each test starts from an empty
 * schema and tests can reuse emails freely across classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class AbstractIntegrationTest {

    // The test config sets admin.email to this value, so registering AS this email
    // yields an ADMIN token with no extra wiring.
    protected static final String ADMIN_EMAIL = "admin@gamestore.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** Registers a user through the real endpoint and returns their signed JWT. */
    protected String registerAndGetToken(String userName, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "userName", userName,
                "email", email,
                "password", password
        ));
        String response = mockMvc.perform(post("/api/v2/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("access_token").asText();
    }

    /** A fresh ADMIN token (registered as the configured admin.email). */
    protected String adminToken() throws Exception {
        return registerAndGetToken("admin", ADMIN_EMAIL, "adminpass123");
    }

    /** A fresh USER token for the given email. */
    protected String userToken(String userName, String email) throws Exception {
        return registerAndGetToken(userName, email, "userpass123");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
