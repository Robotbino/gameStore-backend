package com.gameStore.Bino.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What PUT /users/me/account returns: the updated record AND a fresh token.
 *
 * The token is not a convenience. A JWT's subject is the user's EMAIL, and
 * JWTAuthenticationFilter resolves the caller by calling findByEmail on that
 * subject. The moment someone changes their email, every token they hold
 * names an address that no longer exists — the filter finds nobody, the
 * request arrives unauthenticated, and the frontend's 401 interceptor throws
 * them back to the login page mid-save. Handing back a re-minted token lets
 * the client swap it in and stay signed in.
 *
 * The JSON key matches AuthenticationResponse's access_token, so the frontend
 * reads a token the same way wherever one arrives.
 */
public record AccountUpdateResponse(
        UserResponse user,

        @JsonProperty("access_token")
        String accessToken
) {
}
