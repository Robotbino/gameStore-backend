package com.gameStore.Bino.service;

import com.gameStore.Bino.authentication.AuthenticationRequest;
import com.gameStore.Bino.authentication.AuthenticationResponse;
import com.gameStore.Bino.authentication.RegisterRequest;
import com.gameStore.Bino.models.Role;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.email}")
    private String adminEmail;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request){

        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        //Compares inserted email against the adminEmail
        Role role = request.getEmail().equalsIgnoreCase(adminEmail)
                ? Role.ADMIN
                : Role.USER;

        //Using the builder pattern to create a new user when registering
        var newUser = Users.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        var savedUser = repository.save(newUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());

        var jwtToken = jwtService.generateToken(claims, newUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(()-> new UsernameNotFoundException("Unknown user"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());

        var jwtToken = jwtService.generateToken(claims, user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

}
