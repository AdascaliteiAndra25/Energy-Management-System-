package org.example.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dtos.AuthDTO;
import org.example.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(
        name = "Auth Controller",
        description = "Endpoints responsible for user registration, login, and internal account management"
)

public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Register new user", description = "Register a new user in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
           })
    @PostMapping("/register")
    public String register(@RequestBody AuthDTO dto) {
        return service.register(dto);
    }

    @Operation(summary = "Login user", description = "Authenticate a user and return a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody AuthDTO dto) {
        return service.login(dto);
    }

    @Operation(summary = "Delete user (internal)", description = "Delete a user by username (internal use only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/internal/delete/{username}")
    public ResponseEntity<Void> deleteAuthUserInternal(@PathVariable String username) {
        service.deleteAuthUserByUsername(username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user (internal)", description = "Update username, password, or age by username (internal use only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data provided"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/internal/update/{username}")
    public ResponseEntity<Void> updateAuthUserInternal(
            @PathVariable String username,
            @RequestBody Map<String, Object> body
    ) {
        String newUsername = (String) body.get("username");
        String password = (String) body.get("password");
        Integer age = (Integer) body.get("age");
        service.updateAuthUserByUsername(username, newUsername, password, age);
        return ResponseEntity.noContent().build();
    }




}
