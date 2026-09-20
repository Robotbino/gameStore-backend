package com.gameStore.Bino;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /orders/checkout, GET /orders/me, GET /orders/{id}, GET /rewards/me. */
class CheckoutIT extends AbstractIntegrationTest {

    @Test
    void checkout_noToken_returns401() throws Exception {
        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameIds\":[1],\"paymentMethod\":\"CARD\",\"redeemPoints\":false}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rewards/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_createsOrderWithSnapshots_andEarnsPoints() throws Exception {
        long a = seedGame("Hades", "19.99");
        long b = seedGame("Celeste", "30.01");
        String token = userToken("ana", "ana@example.com");

        checkout(token, List.of(a, b), "CARD", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal", closeTo(50.00, 0.001)))
                .andExpect(jsonPath("$.discount", closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.total", closeTo(50.00, 0.001)))
                .andExpect(jsonPath("$.pointsRedeemed", is(0)))
                .andExpect(jsonPath("$.pointsEarned", is(500)))
                .andExpect(jsonPath("$.pointsBalance", is(500)))
                .andExpect(jsonPath("$.paymentMethod", is("CARD")))
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.paymentReference", matchesPattern("DEMO-[A-HJ-NP-Z2-9]{6}")))
                .andExpect(jsonPath("$.items.length()", is(2)))
                .andExpect(jsonPath("$.items[0].title", is("Hades")))
                .andExpect(jsonPath("$.items[0].unitPrice", closeTo(19.99, 0.001)))
                .andExpect(jsonPath("$.alreadyOwned.length()", is(0)));

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.points", is(500)));
        mockMvc.perform(get("/purchases/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()", is(2)));
        mockMvc.perform(get("/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].items.length()", is(2)));
    }

    @Test
    void redeem_reducesTotal_andDeductsPoints() throws Exception {
        long a = seedGame("Elden Ring", "100.00");
        long b = seedGame("Tunic", "20.00");
        String token = userToken("ben", "ben@example.com");

        checkout(token, List.of(a), "STRIPE", false)
                .andExpect(jsonPath("$.pointsBalance", is(1000)));

        checkout(token, List.of(b), "PAYFLEX", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discount", closeTo(10.00, 0.001)))
                .andExpect(jsonPath("$.pointsRedeemed", is(1000)))
                .andExpect(jsonPath("$.total", closeTo(10.00, 0.001)))
                .andExpect(jsonPath("$.pointsEarned", is(100)))
                .andExpect(jsonPath("$.pointsBalance", is(100)));

        mockMvc.perform(get("/rewards/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(100)))
                .andExpect(jsonPath("$.worth", closeTo(1.00, 0.001)))
                .andExpect(jsonPath("$.transactions.length()", is(3)))
                .andExpect(jsonPath("$.transactions[0].reason", is("EARN")))
                .andExpect(jsonPath("$.transactions[0].delta", is(100)))
                .andExpect(jsonPath("$.transactions[1].reason", is("REDEEM")))
                .andExpect(jsonPath("$.transactions[1].delta", is(-1000)))
                .andExpect(jsonPath("$.transactions[2].reason", is("EARN")))
                .andExpect(jsonPath("$.transactions[2].delta", is(1000)));
    }

    @Test
    void redeem_capsAtSubtotal() throws Exception {
        long a = seedGame("Big", "100.00");
        long b = seedGame("Small", "5.00");
        String token = userToken("cy", "cy@example.com");
        checkout(token, List.of(a), "CARD", false);

        checkout(token, List.of(b), "CARD", true)
                .andExpect(jsonPath("$.discount", closeTo(5.00, 0.001)))
                .andExpect(jsonPath("$.pointsRedeemed", is(500)))
                .andExpect(jsonPath("$.total", closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.pointsEarned", is(0)))
                .andExpect(jsonPath("$.pointsBalance", is(500)));
    }

    @Test
    void redeem_withZeroBalance_isNoOp() throws Exception {
        long a = seedGame("Inside", "19.99");
        String token = userToken("dee", "dee@example.com");

        checkout(token, List.of(a), "CARD", true)
                .andExpect(jsonPath("$.discount", closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.pointsRedeemed", is(0)))
                .andExpect(jsonPath("$.pointsEarned", is(199)));

        mockMvc.perform(get("/rewards/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.transactions.length()", is(1)))
                .andExpect(jsonPath("$.transactions[0].reason", is("EARN")));
    }

    @Test
    void alreadyOwned_excludedFromTotals() throws Exception {
        long a = seedGame("Owned", "40.00");
        long b = seedGame("New", "25.00");
        String token = userToken("eli", "eli@example.com");
        checkout(token, List.of(a), "CARD", false);

        checkout(token, List.of(a, b, b), "CARD", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal", closeTo(25.00, 0.001)))
                .andExpect(jsonPath("$.items.length()", is(1)))
                .andExpect(jsonPath("$.items[0].title", is("New")))
                .andExpect(jsonPath("$.alreadyOwned[0]", is((int) a)));
    }

    @Test
    void allOwned_returns400_andMovesNoPoints() throws Exception {
        long a = seedGame("Owned", "40.00");
        String token = userToken("fay", "fay@example.com");
        checkout(token, List.of(a), "CARD", false);

        checkout(token, List.of(a), "CARD", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/rewards/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.balance", is(400)))
                .andExpect(jsonPath("$.transactions.length()", is(1)));
        mockMvc.perform(get("/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()", is(1)));
    }

    @Test
    void getOrder_isScopedToOwner() throws Exception {
        long a = seedGame("Mine", "10.00");
        String owner = userToken("gus", "gus@example.com");
        String other = userToken("hal", "hal@example.com");
        String body = checkout(owner, List.of(a), "CARD", false)
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) orderId)));
        mockMvc.perform(get("/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownGame_returns404() throws Exception {
        String token = userToken("ivy", "ivy@example.com");
        checkout(token, List.of(999999L), "CARD", false).andExpect(status().isNotFound());
    }

    @Test
    void badPaymentMethod_returns400() throws Exception {
        long a = seedGame("Any", "10.00");
        String token = userToken("jo", "jo@example.com");
        mockMvc.perform(post("/orders/checkout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameIds\":[" + a + "],\"paymentMethod\":\"BITCOIN\",\"redeemPoints\":false}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions checkout(String token, List<Long> ids, String method, boolean redeem) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "gameIds", ids,
                "paymentMethod", method,
                "redeemPoints", redeem));
        return mockMvc.perform(post("/orders/checkout")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // Registering the admin twice in one test would trip the duplicate-email rule.
    private String cachedAdminToken;

    private String seedToken() throws Exception {
        if (cachedAdminToken == null) cachedAdminToken = adminToken();
        return cachedAdminToken;
    }

    private long seedGame(String title, String price) throws Exception {
        String response = mockMvc.perform(post("/games/add")
                        .header(HttpHeaders.AUTHORIZATION, bearer(seedToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"price\":" + price + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }
}
