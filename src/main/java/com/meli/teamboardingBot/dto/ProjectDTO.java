package com.meli.teamboardingBot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ProjectDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("squads")
    private List<SquadDTO> squads;
    
    @JsonProperty("technologies")
    private List<TechnologyDTO> technologies;
    
    @JsonProperty("referents")
    private List<Object> referents;

}
