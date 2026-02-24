package org.example.UserMicroservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncDTO {
    private String eventType;
    private Long userId;
    private String username;
    private Long deviceId; // Pentru user-device mapping
}
