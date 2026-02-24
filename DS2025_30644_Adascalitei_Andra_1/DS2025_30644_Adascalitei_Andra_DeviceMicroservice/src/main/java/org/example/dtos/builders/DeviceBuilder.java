package org.example.dtos.builders;

import org.example.dtos.DeviceDTO;
import org.example.entities.Device;

public class DeviceBuilder {
    private DeviceBuilder() {}

    public static DeviceDTO toDeviceDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        dto.setMaxConsumption(device.getMaxConsumption());
        dto.setUserId(device.getUserId());
        return dto;
    }

    public static Device toEntity(DeviceDTO dto) {
        Device device = Device.builder()
                .name(dto.getName())
                .type(dto.getType())
                .userId(dto.getUserId())
                .maxConsumption(dto.getMaxConsumption())
                .build();
        return device;
    }
}
