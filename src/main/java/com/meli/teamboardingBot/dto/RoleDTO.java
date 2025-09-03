package com.meli.teamboardingBot.dto;
public class RoleDTO {
    private String id;
    private String name;
    private Boolean isSquadRole;
    private String scopesSet;
    public RoleDTO() {}
    public RoleDTO(String id, String name, Boolean isSquadRole, String scopesSet) {
        this.id = id;
        this.name = name;
        this.isSquadRole = isSquadRole;
        this.scopesSet = scopesSet;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Boolean getIsSquadRole() {
        return isSquadRole;
    }
    public void setIsSquadRole(Boolean isSquadRole) {
        this.isSquadRole = isSquadRole;
    }
    public String getScopesSet() {
        return scopesSet;
    }
    public void setScopesSet(String scopesSet) {
        this.scopesSet = scopesSet;
    }
    @Override
    public String toString() {
        return "RoleDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", isSquadRole=" + isSquadRole +
                ", scopesSet='" + scopesSet + '\'' +
                '}';
    }
}
