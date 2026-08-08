package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.CreateUserRequest;
import com.gameStore.Bino.dto.PagedResponse;
import com.gameStore.Bino.dto.UpdateUserRequest;
import com.gameStore.Bino.dto.UserResponse;
import com.gameStore.Bino.models.Role;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// @RestController = @Controller + @ResponseBody, so returns go through Jackson.
// QUIZ Q1: under a plain @Controller (no @ResponseBody), a handler returning
// List<Users> would have its return value treated as a VIEW NAME and handed to
// the view resolver — no view "[Users{...}]" exists, so you'd get a 500, not JSON.
// This class only ever returns ResponseEntity, which Spring serializes either way,
// which is why it "worked" as @Controller before.
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor // DIP (your A9): constructor injection on a final field — same style as your services
public class UsersController {

    private final UsersService usersService;

    // GET /users/me — the caller's own record, identified by the token, never a param.
    // The principal IS a Users: ApplicationConfig.userDetailsService() returns the entity
    // from findByEmail, and JWTAuthenticationFilter places it in the SecurityContext.
    // @AuthenticationPrincipal injects it directly (no getPrincipal() cast needed).
    //
    // QUIZ Q2: identity must come from the signed token, not a ?userId= param, because
    // the token is the one part of the request the client can't forge. Reading the id
    // from a query string is a textbook IDOR — anyone could read another user by changing
    // the number.
    //
    // Note: this principal is a DETACHED entity (loaded outside a transaction), so
    // UserResponse.from reads only scalar columns — touching user.getPurchases() here
    // would throw LazyInitializationException.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // Map entities -> DTOs at the controller edge. That single change stops the hash
    // leak AND defuses the Jackson entity-cycle. Path stays /all — the frontend calls it.
    //
    // Now paginated: the previous unbounded findAllUsers() would happily serialise
    // the entire users table on every admin dashboard load. Optional ?q= filters
    // by username substring; ?page=/?size=/?sort= are the standard Spring Data
    // params; default sort is id ASC so pagination is deterministic across pages.
    // Response wrapped in PagedResponse (our own record) rather than Page<T>
    // because Spring Boot 3.3 warns that PageImpl's JSON shape is not a contract.
    @GetMapping("/all")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<Users> page = usersService.search(q, pageable);
        return ResponseEntity.ok(PagedResponse.from(page, UserResponse::from));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Integer id) {
        usersService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add")
    public ResponseEntity<UserResponse> addUser(@Valid @RequestBody CreateUserRequest request) {
        Users toCreate = Users.builder()
                .userName(request.userName())
                .email(request.email())
                .password(request.password())
                .points(request.points() != null ? request.points() : 0)
                .role(request.role() != null ? request.role() : Role.USER)
                .build();
        Users created = usersService.addUser(toCreate);
        return new ResponseEntity<>(UserResponse.from(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateUserRequest request) {
        Users changes = Users.builder()
                .userName(request.userName())
                .email(request.email())
                .password(request.password()) // blank/null -> service keeps the existing hash
                .points(request.points())
                .role(request.role())
                .build();
        Users updated = usersService.updateUser(id, changes);
        return new ResponseEntity<>(UserResponse.from(updated), HttpStatus.OK);
    }

}
