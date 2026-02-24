package org.example.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.example.dtos.DeviceDTO;
import org.example.services.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/devices")
@Tag(
        name = "Device Controller",
        description = "Endpoints for managing devices, including creation, update, deletion, and retrieval by user or name"
)
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "Get all devices", description = "Retrieve a list of all devices from the system")
    @ApiResponse(responseCode = "200", description = "List of devices retrieved successfully")
    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }


    @Operation(summary = "Get device by ID", description = "Retrieve a specific device by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device found successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @Operation(summary = "Get devices by user ID", description = "Retrieve all devices assigned to a specific user ID")
    @ApiResponse(responseCode = "200", description = "Devices retrieved successfully")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDTO>> getDevicesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(deviceService.getDevicesByUserId(userId));
    }

    @Operation(summary = "Create new device", description = "Add a new device to the system")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<Void> createDevice(@RequestBody DeviceDTO dto) {
        Long id = deviceService.insertDevice(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @Operation(summary = "Update device by ID", description = "Update an existing device's details using its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device updated successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO dto) {
        deviceService.updateDevice(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete device by ID", description = "Remove a device from the system by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Internal delete devices by user ID", description = "Delete all devices associated with a user ID (internal use only)")
    @ApiResponse(responseCode = "204", description = "Devices deleted successfully")
    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteDevicesByUserIdInternal(@PathVariable Long userId) {
        deviceService.deleteDevicesByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete devices by user ID", description = "Delete all devices associated with a user ID")
    @ApiResponse(responseCode = "204", description = "Devices deleted successfully")
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteDevicesByUserId(@PathVariable Long userId) {
        deviceService.deleteDevicesByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get devices by username", description = "Retrieve all devices owned by a specific username")
    @ApiResponse(responseCode = "200", description = "Devices retrieved successfully")
    @GetMapping("/my/{username}")
    public ResponseEntity<List<DeviceDTO>> getMyDevices(@PathVariable String username) {
        // This endpoint is kept for backward compatibility but now returns empty list
        // Frontend should use /devices/user/{userId} instead
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Update device by name", description = "Update an existing device using its name")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device updated successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @PutMapping("/by-name/{name}")
    public ResponseEntity<Void> updateDeviceByName(@PathVariable String name, @RequestBody DeviceDTO dto) {
        Long id = deviceService.getDeviceIdByName(name);
        deviceService.updateDevice(id, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete device by name", description = "Delete a device from the system using its name")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @DeleteMapping("/by-name/{name}")
    public ResponseEntity<Void> deleteDeviceByName(@PathVariable String name) {
        Long id = deviceService.getDeviceIdByName(name);
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }


}
