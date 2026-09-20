package com.gameStore.Bino;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PUT /users/me and PUT /users/me/password — the signed-in user's own account. */
class SelfServiceUserIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "userpass123";

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void updateMe_changesUserNameOnly() throws Exception {
        String token = userToken("hana", "hana@example.com");

        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "hana_renamed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName", is("hana_renamed")))
                .andExpect(jsonPath("$.email", is("hana@example.com")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void updateMe_takenUserName_returns400() throws Exception {
        userToken("ivan", "ivan@example.com");
        String token = userToken("judy", "judy@example.com");

        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "ivan"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Username already in use")));
    }

    @Test
    void updateMe_blankUserName_returns400WithFieldError() throws Exception {
        String token = userToken("kim", "kim@example.com");

        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userName").exists());
    }

    @Test
    void updateMe_noToken_returns401() throws Exception {
        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "nobody"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_correctCurrent_newPasswordWorksAndOldDoesNot() throws Exception {
        String token = userToken("leo", "leo@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "newpass456"))))
                .andExpect(status().isNoContent());

        authenticate("leo@example.com", "newpass456").andExpect(status().isOk());
        authenticate("leo@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_wrongCurrent_returns400NotLogout() throws Exception {
        String token = userToken("mia", "mia@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "wrongpass", "newPassword", "newpass456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }

    @Test
    void changePassword_shortNewPassword_returns400WithFieldError() throws Exception {
        String token = userToken("ned", "ned@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
    }

    @Test
    void changePassword_noToken_returns401() throws Exception {
        mockMvc.perform(put("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "newpass456"))))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions authenticate(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v2/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }
}
