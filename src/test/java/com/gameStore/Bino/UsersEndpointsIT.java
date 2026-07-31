package com.gameStore.Bino;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RBAC and DTO-contract coverage for /users/**. */
class UsersEndpointsIT extends AbstractIntegrationTest {

    @Test
    void usersAll_noToken_returns401() throws Exception {
        mockMvc.perform(get("/users/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersAll_userToken_returns403() throws Exception {
        String token = userToken("frank", "frank@example.com");
        mockMvc.perform(get("/users/all").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersAll_adminToken_returns200AndNeverLeaksPassword() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/users/all").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                // The DTO has no password component — the hash can't leak even by accident.
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void usersMe_noToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersMe_userToken_returnsOwnRecordWithCorrectUserNameAndEmail() throws Exception {
        // Regression test for the UserResponse.from bug: userName must be the username,
        // NOT the email (getUsername() is the UserDetails override that returns email).
        String token = userToken("grace", "grace@example.com");
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName", is("grace")))
                .andExpect(jsonPath("$.email", is("grace@example.com")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void usersMe_adminToken_reportsAdminRole() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName", is("admin")))
                .andExpect(jsonPath("$.email", is(ADMIN_EMAIL)))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }
}
