package com.meli.teamboardingBot.adapters.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {
    private Long id;
    private String name;
    private List<TechnologyDTO> technologies;
    private List<ProjectDTO> projects;
}
