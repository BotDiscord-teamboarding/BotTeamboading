package com.meli.teamboardingBot.dto;

import java.util.List;

public class ProjectDTO {
    
    private Long id;
    private String name;
    private String description;
    private List<SquadDTO> squads;
    private List<TechnologyDTO> technologies;
    private List<Object> referents;
    
    public ProjectDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<SquadDTO> getSquads() { return squads; }
    public void setSquads(List<SquadDTO> squads) { this.squads = squads; }
    
    public List<TechnologyDTO> getTechnologies() { return technologies; }
    public void setTechnologies(List<TechnologyDTO> technologies) { this.technologies = technologies; }
    
    public List<Object> getReferents() { return referents; }
    public void setReferents(List<Object> referents) { this.referents = referents; }
    
    @Override
    public String toString() {
        return "ProjectDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", squads=" + squads +
                ", technologies=" + technologies +
                ", referents=" + referents +
                '}';
    }

}
