package org.example.UserMicroservice.dtos;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.example.UserMicroservice.validators.annotation.AgeLimit;

@Getter
@Setter
public class UserDTO {

    @NotBlank(message = "Username cannot be empty")
    private String username;
    @NotBlank(message = "Password cannot be empty")
    private String password;
    @AgeLimit(value = 18)
    private int age;

}
