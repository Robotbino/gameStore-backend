package com.gameStore.Bino;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /wishlist/me, POST /wishlist/{gameId}, DELETE /wishlist/{gameId}. */
class WishlistIT extends AbstractIntegrationTest {

    @Test
    void wishlist_noToken_returns401() throws Exception {
        mockMvc.perform(get("/wishlist/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/wishlist/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/wishlist/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void add_thenList_containsTheGame() throws Exception {
        long gameId = seedGame("Hades");
        String token = userToken("olga", "olga@example.com");

        mockMvc.perform(post("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.game.title", is("Hades")))
                .andExpect(jsonPath("$.addedAt").exists());

        mockMvc.perform(get("/wishlist/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].game.id", is((int) gameId)));
    }

    @Test
    void add_twice_keepsOneEntry() throws Exception {
        long gameId = seedGame("Celeste");
        String token = userToken("pat", "pat@example.com");

        mockMvc.perform(post("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/wishlist/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void add_unknownGame_returns404() throws Exception {
        String token = userToken("quinn", "quinn@example.com");

        mockMvc.perform(post("/wishlist/999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void remove_thenList_isEmpty_andRemovingAgainIsStill204() throws Exception {
        long gameId = seedGame("Inside");
        String token = userToken("rae", "rae@example.com");
        mockMvc.perform(post("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)));

        mockMvc.perform(delete("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/wishlist/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void list_isScopedToTheCaller() throws Exception {
        long gameId = seedGame("Tunic");
        String alice = userToken("sam", "sam@example.com");
        String bob = userToken("tess", "tess@example.com");
        mockMvc.perform(post("/wishlist/" + gameId).header(HttpHeaders.AUTHORIZATION, bearer(alice)));

        mockMvc.perform(get("/wishlist/me").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(jsonPath("$.length()", is(0)));
    }

    private long seedGame(String title) throws Exception {
        String response = mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"price\":19.99}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }
}
