package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthDTO {
    private String username;
    private String password;
    private String role;
    private int age;
}