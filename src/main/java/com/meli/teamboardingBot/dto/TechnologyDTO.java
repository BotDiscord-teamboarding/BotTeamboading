package com.meli.teamboardingBot.dto;

public class TechnologyDTO {
    
    private Long id;
    private String name;
    private String technologyTypeId;
    
    public TechnologyDTO() {}
    
    public TechnologyDTO(Long id, String name, String technologyTypeId) {
        this.id = id;
        this.name = name;
        this.technologyTypeId = technologyTypeId;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTechnologyTypeId() {
        return technologyTypeId;
    }
    
    public void setTechnologyTypeId(String technologyTypeId) {
        this.technologyTypeId = technologyTypeId;
    }
    
    @Override
    public String toString() {
        return "TechnologyDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", technologyTypeId='" + technologyTypeId + '\'' +
                '}';
    }
}
