package com.meli.teamboardingBot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
