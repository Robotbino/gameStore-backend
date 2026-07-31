package com.gameStore.Bino.configuration;

import com.gameStore.Bino.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

 private final JWTAuthenticationFilter jwtAuthFilter;
 private final AuthenticationProvider authenticationProvider;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                // Rules are evaluated IN ORDER, first match wins. That ordering is load-
                // bearing twice here: GET /games/** must precede the /games/** ADMIN rule,
                // and /users/me must precede the /users/** ADMIN rule — otherwise the broad
                // rule would swallow the specific one and a logged-in USER could never read
                // their own record.
                //
                // QUIZ Q4: hasRole("ADMIN") checks for authority ROLE_ADMIN — it prepends
                // "ROLE_" to the string you pass. That meets the string built on the other
                // side by Users.getAuthorities(): "ROLE_" + role.name(). Use
                // hasAuthority("ROLE_ADMIN") if you'd rather write the prefix yourself.
                .authorizeHttpRequests(auth -> auth
                        //Everything that I have in here are authentication related methods
                        .requestMatchers("/api/v2/auth/**"
                        ).permitAll()
                        //Boot forwards error responses to /error; securing it rewrites 403s into 401s
                        .requestMatchers("/error").permitAll()
                        //Browsing the catalog is public; managing it is admin-only
                        .requestMatchers(HttpMethod.GET, "/games/**").permitAll()
                        .requestMatchers("/games/**").hasRole("ADMIN")
                        //Self-service: any authenticated user reads their own record.
                        //MUST come before the /users/** ADMIN rule (first match wins).
                        .requestMatchers("/users/me").authenticated()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .anyRequest()
                        .authenticated()
                )
                //Return 401 (not 403) for unauthenticated requests so the frontend redirects to login
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}


