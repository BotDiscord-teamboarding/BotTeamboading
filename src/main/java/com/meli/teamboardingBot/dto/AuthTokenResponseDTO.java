package com.meli.teamboardingBot.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
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
}
