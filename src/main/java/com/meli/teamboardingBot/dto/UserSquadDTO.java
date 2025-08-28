package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserSquadDTO {
    
    @JsonProperty("user_id")
    private Long userId;
    
    @JsonProperty("squad_id")
    private Long squadId;
    
    @JsonProperty("role_id")
    private String roleId;
    
    @JsonProperty("added_on")
    private LocalDateTime addedOn;
    
    @JsonProperty("buddy_user_id")
    private Long buddyUserId;
    
    @JsonProperty("certificate_asset_id")
    private String certificateAssetId;
    
    @JsonProperty("certificate_cid")
    private String certificateCid;
    
    @JsonProperty("certificate_state")
    private String certificateState;
    
    @JsonProperty("new_hire_in_orientation_mode_until")
    private LocalDateTime newHireInOrientationModeUntil;

}
