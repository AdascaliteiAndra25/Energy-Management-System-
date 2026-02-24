package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAuthDTO {
    private String username;
    private String password;
    private int age;
}
