package com.meli.teamboardingBot.dto;
import java.time.LocalDate;
import java.util.List;
public class SquadDTO {
    private Long id;
    private String name;
    private String timezone;
    private String countryId;
    private Object country;
    private String languageId;
    private Object language;
    private Long trainingPlanId;
    private Object trainingPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate inOrientationModeUntil;
    private List<TechnologyDTO> technologies;
    private Object project;
    private List<SquadMemberDTO> userSquads;
    private Boolean isObiSquad;
    private Object group;
    private List<String> menuItems;
    private Boolean metricConnectionsEnabled;
    public SquadDTO() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getCountryId() { return countryId; }
    public void setCountryId(String countryId) { this.countryId = countryId; }
    public Object getCountry() { return country; }
    public void setCountry(Object country) { this.country = country; }
    public String getLanguageId() { return languageId; }
    public void setLanguageId(String languageId) { this.languageId = languageId; }
    public Object getLanguage() { return language; }
    public void setLanguage(Object language) { this.language = language; }
    public Long getTrainingPlanId() { return trainingPlanId; }
    public void setTrainingPlanId(Long trainingPlanId) { this.trainingPlanId = trainingPlanId; }
    public Object getTrainingPlan() { return trainingPlan; }
    public void setTrainingPlan(Object trainingPlan) { this.trainingPlan = trainingPlan; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getInOrientationModeUntil() { return inOrientationModeUntil; }
    public void setInOrientationModeUntil(LocalDate inOrientationModeUntil) { this.inOrientationModeUntil = inOrientationModeUntil; }
    public List<TechnologyDTO> getTechnologies() { return technologies; }
    public void setTechnologies(List<TechnologyDTO> technologies) { this.technologies = technologies; }
    public Object getProject() { return project; }
    public void setProject(Object project) { this.project = project; }
    public List<SquadMemberDTO> getUserSquads() { return userSquads; }
    public void setUserSquads(List<SquadMemberDTO> userSquads) { this.userSquads = userSquads; }
    public Boolean getIsObiSquad() { return isObiSquad; }
    public void setIsObiSquad(Boolean isObiSquad) { this.isObiSquad = isObiSquad; }
    public Object getGroup() { return group; }
    public void setGroup(Object group) { this.group = group; }
    public List<String> getMenuItems() { return menuItems; }
    public void setMenuItems(List<String> menuItems) { this.menuItems = menuItems; }
    public Boolean getMetricConnectionsEnabled() { return metricConnectionsEnabled; }
    public void setMetricConnectionsEnabled(Boolean metricConnectionsEnabled) { this.metricConnectionsEnabled = metricConnectionsEnabled; }
    @Override
    public String toString() {
        return "SquadDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", timezone='" + timezone + '\'' +
                ", countryId='" + countryId + '\'' +
                ", country=" + country +
                ", languageId='" + languageId + '\'' +
                ", language=" + language +
                ", trainingPlanId=" + trainingPlanId +
                ", trainingPlan=" + trainingPlan +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", inOrientationModeUntil=" + inOrientationModeUntil +
                ", technologies=" + technologies +
                ", project=" + project +
                ", userSquads=" + userSquads +
                ", isObiSquad=" + isObiSquad +
                ", group=" + group +
                ", menuItems=" + menuItems +
                ", metricConnectionsEnabled=" + metricConnectionsEnabled +
                '}';
    }
}
