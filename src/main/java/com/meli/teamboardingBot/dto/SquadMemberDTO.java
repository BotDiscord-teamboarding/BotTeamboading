package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SquadMemberDTO {
    
    @JsonProperty("user")
    private SquadUserDTO user;
    
    @JsonProperty("role_id")
    private String roleId;
    
    @JsonProperty("role")
    private RoleDTO role;
    
    @JsonProperty("buddy_user_id")
    private Long buddyUserId;
    
    @JsonProperty("new_hire_in_orientation_mode_until")
    private LocalDateTime newHireInOrientationModeUntil;
    
    @JsonProperty("certificate_asset_id")
    private String certificateAssetId;
    
    @JsonProperty("certificate_cid")
    private String certificateCid;
    
    @JsonProperty("certificate_state")
    private String certificateState;
}
