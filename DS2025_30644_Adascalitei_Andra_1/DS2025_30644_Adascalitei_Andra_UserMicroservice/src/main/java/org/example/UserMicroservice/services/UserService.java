package org.example.UserMicroservice.services;


import org.example.UserMicroservice.clients.AuthRestClient;
import org.example.UserMicroservice.dtos.UserDTO;
import org.example.UserMicroservice.dtos.builders.UserBuilder;
import org.example.UserMicroservice.entities.User;
import org.example.UserMicroservice.handlers.exceptions.model.ResourceNotFoundException;
import org.example.UserMicroservice.repositories.UserRepository;


import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final AuthRestClient authRestClient;
    private final SyncPublisherService syncPublisherService;

    public UserService(UserRepository userRepository, AuthRestClient authRestClient, 
                      SyncPublisherService syncPublisherService) {
        this.userRepository = userRepository;
        this.authRestClient = authRestClient;
        this.syncPublisherService = syncPublisherService;
    }


    public List<UserDTO> getAllUsers() {
        List<User> userList = userRepository.findAll();
        return userList.stream()
                .map(UserBuilder::toUserDTO)
                .collect(Collectors.toList());
    }


    public UserDTO getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id: " + id);
        }

        return UserBuilder.toUserDTO(userOptional.get());
    }


    public Long insertUser(UserDTO userDTO) {
        User user = UserBuilder.toEntity(userDTO);
        user = userRepository.save(user);
        LOGGER.debug("User with id {} was inserted in db", user.getId());
        

        syncPublisherService.publishUserCreated(user.getId(), user.getUsername());
        
        return user.getId();
    }


    public void deleteUser(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            LOGGER.error("User with id {} was not found in db", id);
            throw new ResourceNotFoundException("User with id: " + id);
        }

        User user = userOptional.get();

        syncPublisherService.publishUserDeleted(id);

        try {
            authRestClient.deleteAuthUserByUsername(user.getUsername());
            LOGGER.info("Auth user {} was deleted from auth database.", user.getUsername());
        } catch (Exception e) {
            LOGGER.warn("Could not delete auth user {}: {}", user.getUsername(), e.getMessage());
        }

        userRepository.delete(userOptional.get());
        LOGGER.debug("User with id {} was deleted from db", id);
    }

    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    public void updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + id));

        String oldUsername = user.getUsername();

        user.setUsername(userDTO.getUsername());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(userDTO.getPassword());
        }
        user.setAge(userDTO.getAge());

        userRepository.save(user);
        LOGGER.debug("User with id {} was updated in user DB", id);

        syncPublisherService.publishUserUpdated(user.getId(), user.getUsername());

        //  Update in AuthMicroservice
        try {
            authRestClient.updateAuthUser(
                    oldUsername,
                    userDTO.getUsername(),
                    userDTO.getPassword(),
                    userDTO.getAge()
            );
            LOGGER.debug("User {} updated in Auth Microservice", oldUsername);
        } catch (Exception e) {
            LOGGER.warn("Could not update auth user {}: {}", oldUsername, e.getMessage());
        }
    }





}
