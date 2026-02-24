package org.example.monitoringservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_consumption")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, name = "energy_consumption")
    private Double energyConsumption;
}
