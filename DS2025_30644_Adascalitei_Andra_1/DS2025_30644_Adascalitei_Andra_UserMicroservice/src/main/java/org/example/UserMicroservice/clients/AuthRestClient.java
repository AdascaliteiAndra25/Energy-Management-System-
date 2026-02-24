package org.example.UserMicroservice.clients;

import org.example.UserMicroservice.dtos.UserDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AuthRestClient {

    private final RestTemplate restTemplate;
    //private final String authServiceUrl = "http://localhost:8083/auth/internal";
    private final String authServiceUrl = "http://auth-microservice:8083/auth/internal";

    public AuthRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void deleteAuthUserByUsername(String username) {
        String url = authServiceUrl + "/delete/" + username;
        restTemplate.delete(url);
    }

    public void updateAuthUser(String oldUsername, String newUsername, String password, int age) {
        String url = authServiceUrl + "/update/" + oldUsername;

        Map<String, Object> body = Map.of(
                "username", newUsername,
                "password", password,
                "age", age
        );

        restTemplate.put(url, body);
    }


}