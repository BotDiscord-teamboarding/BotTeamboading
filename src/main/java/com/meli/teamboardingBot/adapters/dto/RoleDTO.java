package com.meli.teamboardingBot.adapters.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private String id;
    private String name;
    private Boolean isSquadRole;
    private String scopesSet;
}
