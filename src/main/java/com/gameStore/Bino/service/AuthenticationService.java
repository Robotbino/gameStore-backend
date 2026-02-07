package com.gameStore.Bino.service;

import com.gameStore.Bino.authentication.AuthenticationResponse;
import com.gameStore.Bino.authentication.RegisterRequest;
import com.gameStore.Bino.models.AuthUsers;
import com.gameStore.Bino.repositories.AuthRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthRepo repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    }