package com.meli.teamboardingBot.dto;

import java.util.List;

public class AreaDTO {
    
    private Long id;
    private String name;
    private List<TechnologyDTO> technologies;
    private List<ProjectDTO> projects;
    
    public AreaDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<TechnologyDTO> getTechnologies() { return technologies; }
    public void setTechnologies(List<TechnologyDTO> technologies) { this.technologies = technologies; }
    
    public List<ProjectDTO> getProjects() { return projects; }
    public void setProjects(List<ProjectDTO> projects) { this.projects = projects; }
    
    @Override
    public String toString() {
        return "AreaDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", technologies=" + technologies +
                ", projects=" + projects +
                '}';
    }

}
