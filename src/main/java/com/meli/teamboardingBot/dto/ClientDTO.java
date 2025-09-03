package com.meli.teamboardingBot.dto;
import java.util.List;
public class ClientDTO {
    private Long id;
    private String name;
    private List<AreaDTO> areas;
    private List<Object> skillCategories;
    private List<Object> nomenclatures;
    private Boolean allowRookiesToSeeKnowledgeTree;
    private Boolean canAccessPublicContents;
    public ClientDTO() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AreaDTO> getAreas() { return areas; }
    public void setAreas(List<AreaDTO> areas) { this.areas = areas; }
    public List<Object> getSkillCategories() { return skillCategories; }
    public void setSkillCategories(List<Object> skillCategories) { this.skillCategories = skillCategories; }
    public List<Object> getNomenclatures() { return nomenclatures; }
    public void setNomenclatures(List<Object> nomenclatures) { this.nomenclatures = nomenclatures; }
    public Boolean getAllowRookiesToSeeKnowledgeTree() { return allowRookiesToSeeKnowledgeTree; }
    public void setAllowRookiesToSeeKnowledgeTree(Boolean allowRookiesToSeeKnowledgeTree) { this.allowRookiesToSeeKnowledgeTree = allowRookiesToSeeKnowledgeTree; }
    public Boolean getCanAccessPublicContents() { return canAccessPublicContents; }
    public void setCanAccessPublicContents(Boolean canAccessPublicContents) { this.canAccessPublicContents = canAccessPublicContents; }
    @Override
    public String toString() {
        return "ClientDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", areas=" + areas +
                ", skillCategories=" + skillCategories +
                ", nomenclatures=" + nomenclatures +
                ", allowRookiesToSeeKnowledgeTree=" + allowRookiesToSeeKnowledgeTree +
                ", canAccessPublicContents=" + canAccessPublicContents +
                '}';
    }
}
