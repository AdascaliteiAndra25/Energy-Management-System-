package org.example.UserMicroservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.UserMicroservice.dtos.UserDTO;
import org.example.UserMicroservice.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
@Validated
@Tag(
        name = "User Controller",
        description = "Endpoints for managing users — including registration, update, and deletion by ID or username"
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @Operation(summary = "Get all users", description = "Retrieve a list of all registered users")
    @ApiResponse(responseCode = "200", description = "List of users retrieved successfully")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @Operation(summary = "Get user by ID", description = "Retrieve user details using their ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(summary = "Internal check if user exists by ID", description = "Check whether a user exists based on their ID (internal use only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User exists"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/internal/{id}")
    public ResponseEntity<Void> checkUserExistsInternal(@PathVariable Long id) {
        try {
            userService.getUserById(id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @Operation(summary = "Create new user", description = "Create a new user in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserDTO userDTO) {
        Long id = userService.insertUser(userDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Register user", description = "Register a new user with basic information (for external registration endpoint)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> addRegisterUser(@RequestBody UserDTO userDto) {
        userService.insertUser(userDto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user by ID", description = "Update user details using their unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> replaceUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        userService.updateUser(id, userDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user by username", description = "Update user details based on their username")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/by-username/{username}")
    public ResponseEntity<Void> replaceUserByUsername(@PathVariable String username, @Valid @RequestBody UserDTO userDTO) {
        System.out.println("🔹 Trying to update user with username = " + username);
        Long id = userService.getUserIdByUsername(username);
        System.out.println("🔹 Found id: " + id);


        userService.updateUser(id, userDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user by username", description = "Remove a user from the system using their username")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/by-username/{username}")
    public ResponseEntity<Void> deleteUserByUsername(@PathVariable String username) {
        Long id = userService.getUserIdByUsername(username);
        System.out.println("🔹 Found id for delete: " + id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete user by ID", description = "Delete a user using their unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Internal get user ID by username", description = "Retrieve the ID of a user based on their username (internal use only)")
    @ApiResponse(responseCode = "200", description = "User ID retrieved successfully")
    @GetMapping("/internal/id/{username}")
    public ResponseEntity<Long> getUserIdByUsernameInternal(@PathVariable String username) {
        Long id = userService.getUserIdByUsername(username);
        return ResponseEntity.ok(id);
    }



}