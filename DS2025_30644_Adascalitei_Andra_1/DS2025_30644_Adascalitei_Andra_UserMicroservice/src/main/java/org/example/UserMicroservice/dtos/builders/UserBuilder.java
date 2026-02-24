package org.example.UserMicroservice.dtos.builders;

import org.example.UserMicroservice.dtos.UserDTO;
import org.example.UserMicroservice.entities.User;


public class UserBuilder {
    private UserBuilder() {}

    public static UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setAge(user.getAge());
        return dto;
    }

    public static User toEntity(UserDTO userDTO) {
        User user = User.builder()
                .username(userDTO.getUsername())
                .password(userDTO.getPassword())
                .age(userDTO.getAge())
                .build();


        return user;

    }
}
