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
    void gamesAll_anonymous_returns200AndPagedEnvelope() throws Exception {
        // Response is a PagedResponse — content array plus paging metadata.
        // Anonymous is fine because /games/** GET is open per SecurityConfiguration.
        mockMvc.perform(get("/games/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    void gamesAll_withQuery_filtersByTitleCaseInsensitive() throws Exception {
        // Seed two games as admin, then filter as anonymous — proves the server-side
        // keyword filter works and that ?q= is case-insensitive.
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doom Eternal\",\"price\":29.99}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Halo Infinite\",\"price\":49.99}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/games/all").param("q", "doom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Doom Eternal")));
    }

    @Test
    void gamesAll_withGenre_filtersExactMatch() throws Exception {
        String token = adminToken();
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doom\",\"price\":19.99,\"genre\":\"Action\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Skyrim\",\"price\":29.99,\"genre\":\"RPG\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/games/all").param("genre", "RPG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Skyrim")));
    }

    @Test
    void gamesAll_pagination_respectsPageAndSize() throws Exception {
        // Seed three games; ?size=2 splits into two pages.
        String token = adminToken();
        for (String title : new String[]{"A", "B", "C"}) {
            mockMvc.perform(post("/games/add")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + title + "\",\"price\":9.99}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/games/all").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)));

        mockMvc.perform(get("/games/all").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)));
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
