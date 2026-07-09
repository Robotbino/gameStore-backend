package com.gameStore.Bino.configuration;


import com.gameStore.Bino.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    @Override
    public void doFilterInternal(
            @NonNull
            HttpServletRequest request,
            @NonNull
            HttpServletResponse response,
            @NonNull
            FilterChain filterChain
    )throws ServletException, IOException{
        final String authHeader = request.getHeader("Authorization");

        //Check that the Servlet path for authentication is correct
        if(request.getServletPath().contains("api/v2/auth")){
            filterChain.doFilter(request,response);
            return;
        }

        //Checks that authHeader isn't empty
        if(authHeader != null && authHeader.startsWith("Bearer ")){
            try{
                //Reads substring after the JSON objects property which would be in reference to the Bearer section
                final String jwt = authHeader.substring(7);
                final String userEmail = jwtService.extractUsername(jwt);
                //Verify that the email used in the  logins is no null and  check the auth status from the SecurityContext holder.
                if(userEmail !=null && SecurityContextHolder.getContext().getAuthentication() == null){

                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                    if(jwtService.isTokenValid(jwt,userDetails)){

                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }catch(JwtException | UsernameNotFoundException e){
                //Invalid or expired token: continue unauthenticated so protected routes return 401
            }
        }
        filterChain.doFilter(request,response);
    }

}
