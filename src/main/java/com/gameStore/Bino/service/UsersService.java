package com.gameStore.Bino.service;

import com.gameStore.Bino.exceptions.DuplicateResourceException;
import com.gameStore.Bino.exceptions.InvalidPasswordException;
import com.gameStore.Bino.exceptions.ResourceNotFoundException;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UsersService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Paginated user listing with optional username keyword. Replaces the
     * unbounded findAllUsers() and the never-called searchUsers() with a
     * single method that covers /users/all and future admin search UIs.
     */
    public Page<Users> search(String q, Pageable pageable) {
        return userRepository.search(q, pageable);
    }

    // id params are Integer now — must match JpaRepository<Users, Integer>
    public Users findUserByID(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    //Add a User
    public Users addUser(Users user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already in use");
        }
        if (userRepository.existsByUserName(user.getUserName())) {
            throw new DuplicateResourceException("Username already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    //Delete User record
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // =================================================================
    // Self-service. Everything below acts on the CALLER, whose id comes
    // from the @AuthenticationPrincipal, never from the request body.
    //
    // The principal itself is a DETACHED entity — JWTAuthenticationFilter
    // loaded it outside any transaction — so it is re-read by id here
    // (same as updateProfile/changePassword below) rather than mutated
    // and saved directly.
    // =================================================================

    /**
     * Replace the caller's presentation fields. Nulls REPLACE rather than
     * being skipped — that's what makes "clear my bio" possible, and it's
     * safe here because the client already holds the whole record.
     */
    public Users updateOwnProfile(Integer id, String displayName, String avatarKey, String bio, String country) {
        Users existing = findUserByID(id);

        existing.setDisplayName(blankToNull(displayName));
        existing.setAvatarKey(blankToNull(avatarKey));
        existing.setBio(blankToNull(bio));
        existing.setCountry(blankToNull(country));

        return userRepository.save(existing);
    }

    /**
     * Change the caller's own userName and/or email.
     *
     * Both columns are unique, so availability is checked EXCLUDING the
     * caller's own row: without that, saving the form without touching the
     * email would report your own address as already in use.
     */
    public Users updateOwnAccount(Integer id, String newUserName, String newEmail) {
        Users existing = findUserByID(id);

        requireAvailable(existing, newUserName, newEmail);
        existing.setUserName(newUserName);
        existing.setEmail(newEmail);

        return userRepository.save(existing);
    }

    /**
     * Uniqueness guard for an EDIT. exists* would report the row being edited
     * as a conflict with itself, so only check a value that actually changed.
     */
    private void requireAvailable(Users existing, String newUserName, String newEmail) {
        boolean emailChanged = newEmail != null && !newEmail.equalsIgnoreCase(existing.getEmail());
        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("Email already in use");
        }

        boolean userNameChanged = newUserName != null && !newUserName.equals(existing.getUserName());
        if (userNameChanged && userRepository.existsByUserName(newUserName)) {
            throw new DuplicateResourceException("Username already in use");
        }
    }

    /** An empty text input arrives as "", which should clear the column, not store a blank. */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public Users updateUser(Integer id, Users updatedUser) {
        Users existing = findUserByID(id);  // throws if not found

        // Same guard the self-service path uses. Without it a duplicate email
        // surfaced as a DB constraint violation — a 500 blaming the server for
        // what is plainly a client mistake.
        requireAvailable(existing, updatedUser.getUserName(), updatedUser.getEmail());

        existing.setUserName(updatedUser.getUserName());
        existing.setEmail(updatedUser.getEmail());
        // points/role are optional on an edit — only overwrite when supplied, so an
        // update that omits them doesn't null out the stored values.
        if (updatedUser.getPoints() != null) {
            existing.setPoints(updatedUser.getPoints());
        }
        if (updatedUser.getRole() != null) {
            existing.setRole(updatedUser.getRole());
        }

        // The frontend omits the password on edits; only replace it when a new one
        // is provided, otherwise the stored hash would be wiped on a normal edit.
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existing);
    }

    public Users updateProfile(Integer id, String newUserName) {
        Users existing = findUserByID(id);
        boolean nameChanged = !existing.getUserName().equals(newUserName);
        if (nameChanged && userRepository.existsByUserName(newUserName)) {
            throw new DuplicateResourceException("Username already in use");
        }
        existing.setUserName(newUserName);
        return userRepository.save(existing);
    }

    public void changePassword(Integer id, String currentPassword, String newPassword) {
        Users existing = findUserByID(id);
        if (!passwordEncoder.matches(currentPassword, existing.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        existing.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(existing);
    }
}
