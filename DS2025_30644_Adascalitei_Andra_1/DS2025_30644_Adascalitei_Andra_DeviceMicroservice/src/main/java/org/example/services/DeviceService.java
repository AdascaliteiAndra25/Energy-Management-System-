package org.example.services;


import org.example.dtos.DeviceDTO;
import org.example.dtos.builders.DeviceBuilder;
import org.example.entities.Device;
import org.example.repositories.DeviceRepository;
import org.example.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final SyncPublisherService syncPublisherService;
    private final UserRepository userRepository;

    public DeviceService(DeviceRepository deviceRepository, SyncPublisherService syncPublisherService,
                        UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.syncPublisherService = syncPublisherService;
        this.userRepository = userRepository;
    }

    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDTO getDeviceById(Long id) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (!deviceOptional.isPresent()) {
            throw new RuntimeException("Device with id " + id + " not found.");
        }
        return DeviceBuilder.toDeviceDTO(deviceOptional.get());
    }

    public Long insertDevice(DeviceDTO dto) {

        if (!userRepository.existsById(dto.getUserId())) {
            throw new RuntimeException("User with id " + dto.getUserId() + " does not exist.");
        }
        
        Device device = DeviceBuilder.toEntity(dto);
        device = deviceRepository.save(device);
        

        syncPublisherService.publishDeviceCreated(device.getId(), device.getName(), device.getUserId());
        
        return device.getId();
    }

    public void updateDevice(Long id, DeviceDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device with id " + id + " not found."));
        
        // Validate that user exists in local database
        if (!userRepository.existsById(dto.getUserId())) {
            throw new RuntimeException("User with id " + dto.getUserId() + " does not exist.");
        }
        
        device.setName(dto.getName());
        device.setType(dto.getType());
        device.setMaxConsumption(dto.getMaxConsumption());
        device.setUserId(dto.getUserId());
        deviceRepository.save(device);
        

        syncPublisherService.publishDeviceUpdated(device.getId(), device.getName(), device.getUserId());
    }

    public void deleteDevice(Long id) {

        syncPublisherService.publishDeviceDeleted(id);
        
        deviceRepository.deleteById(id);
    }

    public List<DeviceDTO> getDevicesByUserId(Long userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public void deleteDevicesByUserId(Long userId) {
        List<Device> devices = deviceRepository.findByUserId(userId);
        deviceRepository.deleteAll(devices);
    }

    public Long getDeviceIdByName(String name) {
        Device device = deviceRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return device.getId();
    }






}
