package com.meli.teamboardingBot.dto;

public class AuthTokenResponseDTO {
    
    private String accessToken;
    private String tokenType;
    private UserDTO user;
    
    public AuthTokenResponseDTO() {}
    
    public AuthTokenResponseDTO(String accessToken, String tokenType, UserDTO user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.user = user;
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
    
    @Override
    public String toString() {
        return "AuthTokenResponseDTO{" +
                "accessToken='" + accessToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", user=" + user +
                '}';
    }
}
