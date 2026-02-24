package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceDTO {

        private Long id;
        private String name;
        private String type;
        private Double maxConsumption;
        private Long userId;
}
