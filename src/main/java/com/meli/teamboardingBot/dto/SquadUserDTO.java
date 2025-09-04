package com.meli.teamboardingBot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquadUserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private List<UserSquadDTO> userSquads;
    private String email;
    private String picture;
    private List<Object> creditUsages;
    private String preferredLanguageId;
    private LanguageDTO preferredLanguage;
    private List<TechnologyDTO> technologies;
    private Object userSkillTrees;
}
