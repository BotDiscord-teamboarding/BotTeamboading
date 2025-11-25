package com.meli.teamboardingBot.adapters.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSquadDTO {
    private Long userId;
    private Long squadId;
    private String roleId;
    private LocalDateTime addedOn;
    private Long buddyUserId;
    private String certificateAssetId;
    private String certificateCid;
    private String certificateState;
    private LocalDateTime newHireInOrientationModeUntil;
}
