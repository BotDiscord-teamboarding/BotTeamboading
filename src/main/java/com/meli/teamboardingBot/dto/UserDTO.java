package com.meli.teamboardingBot.dto;
import java.util.List;
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private String picture;
    private Boolean pictureDeleted;
    private String preferredLanguageId;
    private LanguageDTO preferredLanguage;
    private Boolean isPepUser;
    private Boolean isObiUser;
    private List<ClientDTO> clients;
    private Object userProfileExtra;
    private List<RoleDTO> roles;
    private List<String> homeSections;
    private List<UserSquadDTO> userSquads;
    private String algorandWalletAddress;
    private Boolean firstLogin;
    private Boolean showFiltersHelp;
    private List<String> menuItems;
    private String privacyPolicyVersion;
    private String termsAndConditionsVersion;
    public UserDTO() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }
    public Boolean getPictureDeleted() { return pictureDeleted; }
    public void setPictureDeleted(Boolean pictureDeleted) { this.pictureDeleted = pictureDeleted; }
    public String getPreferredLanguageId() { return preferredLanguageId; }
    public void setPreferredLanguageId(String preferredLanguageId) { this.preferredLanguageId = preferredLanguageId; }
    public LanguageDTO getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(LanguageDTO preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public Boolean getIsPepUser() { return isPepUser; }
    public void setIsPepUser(Boolean isPepUser) { this.isPepUser = isPepUser; }
    public Boolean getIsObiUser() { return isObiUser; }
    public void setIsObiUser(Boolean isObiUser) { this.isObiUser = isObiUser; }
    public List<ClientDTO> getClients() { return clients; }
    public void setClients(List<ClientDTO> clients) { this.clients = clients; }
    public Object getUserProfileExtra() { return userProfileExtra; }
    public void setUserProfileExtra(Object userProfileExtra) { this.userProfileExtra = userProfileExtra; }
    public List<RoleDTO> getRoles() { return roles; }
    public void setRoles(List<RoleDTO> roles) { this.roles = roles; }
    public List<String> getHomeSections() { return homeSections; }
    public void setHomeSections(List<String> homeSections) { this.homeSections = homeSections; }
    public List<UserSquadDTO> getUserSquads() { return userSquads; }
    public void setUserSquads(List<UserSquadDTO> userSquads) { this.userSquads = userSquads; }
    public String getAlgorandWalletAddress() { return algorandWalletAddress; }
    public void setAlgorandWalletAddress(String algorandWalletAddress) { this.algorandWalletAddress = algorandWalletAddress; }
    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }
    public Boolean getShowFiltersHelp() { return showFiltersHelp; }
    public void setShowFiltersHelp(Boolean showFiltersHelp) { this.showFiltersHelp = showFiltersHelp; }
    public List<String> getMenuItems() { return menuItems; }
    public void setMenuItems(List<String> menuItems) { this.menuItems = menuItems; }
    public String getPrivacyPolicyVersion() { return privacyPolicyVersion; }
    public void setPrivacyPolicyVersion(String privacyPolicyVersion) { this.privacyPolicyVersion = privacyPolicyVersion; }
    public String getTermsAndConditionsVersion() { return termsAndConditionsVersion; }
    public void setTermsAndConditionsVersion(String termsAndConditionsVersion) { this.termsAndConditionsVersion = termsAndConditionsVersion; }
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", picture='" + picture + '\'' +
                ", pictureDeleted=" + pictureDeleted +
                ", preferredLanguageId='" + preferredLanguageId + '\'' +
                ", preferredLanguage=" + preferredLanguage +
                ", isPepUser=" + isPepUser +
                ", isObiUser=" + isObiUser +
                ", clients=" + clients +
                ", userProfileExtra=" + userProfileExtra +
                ", roles=" + roles +
                ", homeSections=" + homeSections +
                ", userSquads=" + userSquads +
                ", algorandWalletAddress='" + algorandWalletAddress + '\'' +
                ", firstLogin=" + firstLogin +
                ", showFiltersHelp=" + showFiltersHelp +
                ", menuItems=" + menuItems +
                ", privacyPolicyVersion='" + privacyPolicyVersion + '\'' +
                ", termsAndConditionsVersion='" + termsAndConditionsVersion + '\'' +
                '}';
    }
}
