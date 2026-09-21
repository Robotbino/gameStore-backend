package com.gameStore.Bino;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Self-service coverage for /users/me/**.
 *
 * The reachability tests are the important ones. SecurityConfiguration's
 * self-service matcher used to be the exact string "/users/me", which meant
 * every sub-path fell through to the /users/** ADMIN rule — a plain USER got
 * 403 on their own profile. Anything that narrows that matcher again should
 * fail here first.
 */
class ProfileEndpointsIT extends AbstractIntegrationTest {

    // ── Reachability: a plain USER, not just an admin, must get through ──

    @Test
    void updateProfile_noToken_returns401() throws Exception {
        mockMvc.perform(put("/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAccount_noToken_returns401() throws Exception {
        mockMvc.perform(put("/users/me/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_noToken_returns401() throws Exception {
        mockMvc.perform(put("/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selfServicePaths_userToken_areNotAdminGated() throws Exception {
        String token = userToken("hank", "hank@example.com");

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "Hank"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "hank", "email", "hank@example.com"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "userpass123", "newPassword", "newpass1234"))))
                .andExpect(status().isNoContent());
    }

    // ── PUT /users/me/profile ──

    @Test
    void updateProfile_validBody_persistsFieldsAndNeverLeaksPassword() throws Exception {
        String token = userToken("ivy", "ivy@example.com");

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "displayName", "Ivy Q",
                                "avatarKey", "marquee-03",
                                "bio", "Mostly RPGs.",
                                "country", "ZA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName", is("Ivy Q")))
                .andExpect(jsonPath("$.avatarKey", is("marquee-03")))
                .andExpect(jsonPath("$.bio", is("Mostly RPGs.")))
                .andExpect(jsonPath("$.country", is("ZA")))
                // The handle is untouched by a profile edit — that is /me/account's job.
                .andExpect(jsonPath("$.userName", is("ivy")))
                .andExpect(jsonPath("$.password").doesNotExist());

        // And it survives a re-read rather than just being echoed back.
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.displayName", is("Ivy Q")))
                .andExpect(jsonPath("$.bio", is("Mostly RPGs.")));
    }

    @Test
    void updateProfile_blankFields_clearsThemInsteadOfKeepingOldValues() throws Exception {
        // Null-replaces-value is the whole reason this endpoint does not use
        // UpdateUserRequest's null-means-keep semantics: without it there is no
        // way to delete a bio you regret.
        String token = userToken("jack", "jack@example.com");

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bio", "Temporary.", "displayName", "Jack"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("Temporary.")));

        Map<String, Object> cleared = new HashMap<>();
        cleared.put("bio", "");
        cleared.put("displayName", null);

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cleared)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is(nullValue())))
                .andExpect(jsonPath("$.displayName", is(nullValue())));
    }

    @Test
    void updateProfile_bioOverLimit_returns400WithFieldError() throws Exception {
        String token = userToken("kim", "kim@example.com");

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bio", "x".repeat(281)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.bio").exists());
    }

    @Test
    void updateProfile_cannotPromoteSelfToAdmin() throws Exception {
        // role has no component on UpdateProfileRequest, so Jackson drops it.
        // This asserts the omission actually holds at the wire level.
        String token = userToken("liam", "liam@example.com");

        mockMvc.perform(put("/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "Liam", "role", "ADMIN", "points", 99999))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.points", is(0)));
    }

    // ── PUT /users/me/account ──

    @Test
    void updateAccount_unchangedEmail_returns200() throws Exception {
        // The uniqueness guard must exclude the caller's own row, or saving the
        // form without touching the email reports your own address as taken.
        String token = userToken("mia", "mia@example.com");

        mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "mia renamed", "email", "mia@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userName", is("mia renamed")))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void updateAccount_emailTakenByAnotherUser_returns400() throws Exception {
        userToken("nate", "nate@example.com");
        String token = userToken("olive", "olive@example.com");

        mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "olive", "email", "nate@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Email already in use")));
    }

    @Test
    void updateAccount_userNameTakenByAnotherUser_returns400() throws Exception {
        userToken("pat", "pat@example.com");
        String token = userToken("quinn", "quinn@example.com");

        mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "pat", "email", "quinn@example.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Username already in use")));
    }

    @Test
    void updateAccount_changedEmail_returnsUsableReplacementToken() throws Exception {
        // The JWT subject is the email, and JWTAuthenticationFilter resolves the
        // caller with findByEmail. Without a re-minted token the caller's very
        // next request is anonymous and the frontend logs them out mid-save.
        String oldToken = userToken("rosa", "rosa@example.com");

        String body = mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "rosa", "email", "rosa.new@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email", is("rosa.new@example.com")))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(body);
        String newToken = node.get("access_token").asText();

        // The replacement works...
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("rosa.new@example.com")));

        // ...and the old one is dead, which is exactly why the replacement is needed.
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(oldToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAccount_malformedEmail_returns400WithFieldError() throws Exception {
        String token = userToken("sam", "sam@example.com");

        mockMvc.perform(put("/users/me/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userName", "sam", "email", "not-an-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email", is("email must be a valid address")));
    }

    // ── PUT /users/me/password ──

    @Test
    void changePassword_wrongCurrentPassword_returns400NotUnauthorized() throws Exception {
        // 401 here would be semantically tidy and practically awful: the frontend's
        // axios interceptor treats any non-/auth/ 401 as a dead session, so a typo
        // would clear the token and bounce the user to /login mid-form.
        String token = userToken("tom", "tom@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "wrongpass123", "newPassword", "newpass1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }

    @Test
    void changePassword_correctCurrentPassword_returns204AndNewPasswordAuthenticates() throws Exception {
        String token = userToken("uma", "uma@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "userpass123", "newPassword", "brandnew123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v2/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "uma@example.com", "password", "brandnew123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    @Test
    void changePassword_shortNewPassword_returns400WithFieldError() throws Exception {
        String token = userToken("vic", "vic@example.com");

        mockMvc.perform(put("/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "userpass123", "newPassword", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword", is("newPassword must be at least 8 characters")));
    }

    /** Map -> JSON, allowing null values (Map.of does not). */
    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
