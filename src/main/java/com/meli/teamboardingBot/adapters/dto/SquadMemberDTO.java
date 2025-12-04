package com.meli.teamboardingBot.adapters.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquadMemberDTO {
    private SquadUserDTO user;
    private String roleId;
    private RoleDTO role;
    private Long buddyUserId;
    private LocalDateTime newHireInOrientationModeUntil;
    private String certificateAssetId;
    private String certificateCid;
    private String certificateState;
}
