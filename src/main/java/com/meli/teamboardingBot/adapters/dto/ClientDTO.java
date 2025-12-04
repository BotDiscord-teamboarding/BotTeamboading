package com.meli.teamboardingBot.adapters.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private Long id;
    private String name;
    private List<AreaDTO> areas;
    private List<Object> skillCategories;
    private List<Object> nomenclatures;
    private Boolean allowRookiesToSeeKnowledgeTree;
    private Boolean canAccessPublicContents;
}
