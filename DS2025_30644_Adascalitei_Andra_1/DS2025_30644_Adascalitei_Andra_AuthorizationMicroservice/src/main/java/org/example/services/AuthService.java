package org.example.services;

import org.example.dtos.AuthDTO;
import org.example.dtos.UserAuthDTO;
import org.example.entities.AuthUser;
import org.example.repositories.AuthRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Service
public class AuthService {
    private final AuthRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;


    public AuthService(AuthRepository repository, PasswordEncoder encoder, JwtService jwtService, RestTemplate restTemplate) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.restTemplate = restTemplate;
    }

    public String register(AuthDTO dto) {
        if (repository.findByUsername(dto.getUsername()).isPresent()) {
            return "Username already exists!";
        }

        AuthUser user = AuthUser.builder()
                .username(dto.getUsername())
                .password(encoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .age(dto.getAge())
                .build();

        repository.save(user);

        UserAuthDTO userDto = new UserAuthDTO();
        userDto.setUsername(dto.getUsername());
        userDto.setPassword(dto.getPassword());
        userDto.setAge(dto.getAge());

        restTemplate.postForEntity("http://user-microservice:8081/users/register", userDto, Void.class);

        return "User registered successfully!";
    }

    public Map<String, String> login(AuthDTO dto) {
        return repository.findByUsername(dto.getUsername())
                .map(user -> {
                    if (encoder.matches(dto.getPassword(), user.getPassword())) {
                        String token = jwtService.generateToken(user);
                        return Map.of("token", token);
                    } else {
                        throw new RuntimeException("Invalid password!");
                    }
                })
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public void deleteAuthUserByUsername(String username) {
        repository.findByUsername(username).ifPresentOrElse(
                repository::delete,
                () -> {
                    throw new RuntimeException("Auth user not found with username: " + username);
                }
        );
    }

    public void updateAuthUserByUsername(String oldUsername, String newUsername, String password, int age) {
        AuthUser user = repository.findByUsername(oldUsername)
                .orElseThrow(() -> new RuntimeException("Auth user not found with username: " + oldUsername));

        user.setUsername(newUsername);
        if (password != null && !password.isEmpty()) {
            user.setPassword(encoder.encode(password));
        }
        user.setAge(age);

        repository.save(user);
    }




}
