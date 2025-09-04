package com.meli.teamboardingBot.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenRequestDTO {
    private String grantType;
    private String username;
    private String password;
    private String scope;
    private String clientId;
    private String clientSecret;
}
