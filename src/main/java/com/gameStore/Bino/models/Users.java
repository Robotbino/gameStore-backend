package com.gameStore.Bino.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="users")
public class Users implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String userName;
    private String email;
    private Double points;

    @Column(nullable = false)
    private String password;


    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;
    @Column(name = "is_enabled")
    private boolean enabled = true;  // Rename to 'enabled'

    // ---------------------------------------------------------------
    // Relationship: One User -> Many Purchases
    // mappedBy = "user" means the Purchase entity owns the FK column.
    // CascadeType.ALL: if you delete a user, their purchases go too.
    // ---------------------------------------------------------------
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Purchases> purchases = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        // Spring Security uses this for authentication lookups.
        // We authenticate by email, so return email here.
        return email;
    }
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

