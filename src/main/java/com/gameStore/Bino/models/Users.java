package com.gameStore.Bino.models;

import jakarta.persistence.*;

@Table(name="user")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String userName;
    private String email;
    @Column(nullable = false)
    private String password;
    private Double points;
    @Enumerated(EnumType.STRING)
    private Role role;
}
