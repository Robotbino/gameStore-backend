package com.gameStore.Bino;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Public browsing, ADMIN-gated writes, validation, and the RAWG genre shape. */
class GamesEndpointsIT extends AbstractIntegrationTest {

    @Test
    void gamesAll_anonymous_returns200() throws Exception {
        mockMvc.perform(get("/games/all"))
                .andExpect(status().isOk());
    }

    @Test
    void addGame_userToken_returns403() throws Exception {
        String token = userToken("henry", "henry@example.com");
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doom\",\"price\":19.99}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addGame_adminToken_returns201() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doom\",\"price\":19.99,\"rating\":4.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Doom")));
    }

    @Test
    void addGame_blankTitleAndNegativePrice_returns400() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"price\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void addGame_ratingAboveScale_returns400() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doom\",\"price\":19.99,\"rating\":9.9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());
    }

    @Test
    void addGame_genreAsArray_isStoredAsCommaSeparated() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Halo\",\"price\":29.99,\"genre\":[\"Action\",\"RPG\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.genre", is("Action,RPG")));
    }

    @Test
    void addGame_genreAsString_isStoredAsIs() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Halo\",\"price\":29.99,\"genre\":\"Action,RPG\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.genre", is("Action,RPG")));
    }
}
