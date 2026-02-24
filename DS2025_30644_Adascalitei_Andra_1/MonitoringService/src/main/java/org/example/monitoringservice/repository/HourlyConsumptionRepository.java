package org.example.monitoringservice.repository;

import org.example.monitoringservice.entity.HourlyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, Long> {
    
    List<HourlyConsumption> findByDeviceIdAndTimestampBetween(
        Long deviceId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    @Query("SELECT h FROM HourlyConsumption h WHERE h.deviceId = :deviceId ORDER BY h.timestamp DESC")
    List<HourlyConsumption> findByDeviceIdOrderByTimestampDesc(@Param("deviceId") Long deviceId);
}
