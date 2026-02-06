package com.gameStore.Bino.configuration;


import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter {

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
        final String jwt;
        final String userEmail;

        if(request.getServletPath().contains("api/v2/auth"))

    }

}
