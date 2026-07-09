package com.gameStore.Bino.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="users")
public class Users implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String userName;

    @Column(unique = true, nullable = false)
    private String email;

    @Builder.Default
    private Integer points = 0;

    //Accepted on incoming requests but never serialized into responses
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;


    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_enabled")
    @Builder.Default
    private boolean enabled = true;

    // ---------------------------------------------------------------
    // Relationship: One User -> Many Purchases
    // mappedBy = "user" means the Purchase entity owns the FK column.
    // CascadeType.ALL: if you delete a user, their purchases go too.
    // ---------------------------------------------------------------
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<Purchases> purchases = new ArrayList<>();

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    //Lombok skips this getter because getUsername() below matches case-insensitively,
    //so declare it explicitly for the userName field (also puts userName in JSON responses)
    public String getUserName() {
        return userName;
    }

    @Override
    @JsonIgnore
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
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", points=" + points +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return id != null && Objects.equals(id, users.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
