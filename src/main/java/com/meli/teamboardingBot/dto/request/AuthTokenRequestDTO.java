package com.meli.teamboardingBot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuthTokenRequestDTO {
    
    @JsonProperty("grant_type")
    private String grantType;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
}
