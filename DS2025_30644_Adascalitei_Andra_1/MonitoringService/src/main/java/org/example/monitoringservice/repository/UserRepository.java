package org.example.monitoringservice.repository;

import org.example.monitoringservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDeviceId(Long deviceId);
}
