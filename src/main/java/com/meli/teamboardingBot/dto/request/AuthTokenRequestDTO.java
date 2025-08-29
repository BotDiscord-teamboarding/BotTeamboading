package com.meli.teamboardingBot.dto.request;

public class AuthTokenRequestDTO {
    
    private String grantType;
    private String username;
    private String password;
    private String scope;
    private String clientId;
    private String clientSecret;
    
    public AuthTokenRequestDTO() {}
    
    public AuthTokenRequestDTO(String grantType, String username, String password, String scope, String clientId, String clientSecret) {
        this.grantType = grantType;
        this.username = username;
        this.password = password;
        this.scope = scope;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    public String getGrantType() {
        return grantType;
    }
    
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    @Override
    public String toString() {
        return "AuthTokenRequestDTO{" +
                "grantType='" + grantType + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", scope='" + scope + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                '}';
    }
}
