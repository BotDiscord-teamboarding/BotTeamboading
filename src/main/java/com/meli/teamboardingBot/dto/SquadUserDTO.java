package com.meli.teamboardingBot.dto;

import java.util.List;

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
    
    public SquadUserDTO() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public List<UserSquadDTO> getUserSquads() { return userSquads; }
    public void setUserSquads(List<UserSquadDTO> userSquads) { this.userSquads = userSquads; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }
    
    public List<Object> getCreditUsages() { return creditUsages; }
    public void setCreditUsages(List<Object> creditUsages) { this.creditUsages = creditUsages; }
    
    public String getPreferredLanguageId() { return preferredLanguageId; }
    public void setPreferredLanguageId(String preferredLanguageId) { this.preferredLanguageId = preferredLanguageId; }
    
    public LanguageDTO getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(LanguageDTO preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    
    public List<TechnologyDTO> getTechnologies() { return technologies; }
    public void setTechnologies(List<TechnologyDTO> technologies) { this.technologies = technologies; }
    
    public Object getUserSkillTrees() { return userSkillTrees; }
    public void setUserSkillTrees(Object userSkillTrees) { this.userSkillTrees = userSkillTrees; }
    
    @Override
    public String toString() {
        return "SquadUserDTO{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userSquads=" + userSquads +
                ", email='" + email + '\'' +
                ", picture='" + picture + '\'' +
                ", creditUsages=" + creditUsages +
                ", preferredLanguageId='" + preferredLanguageId + '\'' +
                ", preferredLanguage=" + preferredLanguage +
                ", technologies=" + technologies +
                ", userSkillTrees=" + userSkillTrees +
                '}';
    }
}
