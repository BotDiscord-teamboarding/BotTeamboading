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
public class UserDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("enabled")
    private Boolean enabled;
    
    @JsonProperty("picture")
    private String picture;
    
    @JsonProperty("picture_deleted")
    private Boolean pictureDeleted;
    
    @JsonProperty("preferred_language_id")
    private String preferredLanguageId;
    
    @JsonProperty("preferred_language")
    private LanguageDTO preferredLanguage;
    
    @JsonProperty("is_pep_user")
    private Boolean isPepUser;
    
    @JsonProperty("is_obi_user")
    private Boolean isObiUser;
    
    @JsonProperty("clients")
    private List<ClientDTO> clients;
    
    @JsonProperty("user_profile_extra")
    private Object userProfileExtra;
    
    @JsonProperty("roles")
    private List<RoleDTO> roles;
    
    @JsonProperty("home_sections")
    private List<String> homeSections;
    
    @JsonProperty("user_squads")
    private List<UserSquadDTO> userSquads;
    
    @JsonProperty("algorand_wallet_address")
    private String algorandWalletAddress;
    
    @JsonProperty("first_login")
    private Boolean firstLogin;
    
    @JsonProperty("show_filters_help")
    private Boolean showFiltersHelp;
    
    @JsonProperty("menu_items")
    private List<String> menuItems;
    
    @JsonProperty("privacy_policy_version")
    private String privacyPolicyVersion;
    
    @JsonProperty("terms_and_conditions_version")
    private String termsAndConditionsVersion;
}
