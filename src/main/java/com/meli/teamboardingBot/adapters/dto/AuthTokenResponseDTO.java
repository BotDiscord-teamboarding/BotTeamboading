package com.meli.teamboardingBot.adapters.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthTokenResponseDTO {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    private UserDTO user;
    public AuthTokenResponseDTO(String accessToken, String tokenType, UserDTO user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.user = user;
    }
    @Override
    public String toString() {
        return "AuthTokenResponseDTO{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", user=" + user +
                '}';
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}
