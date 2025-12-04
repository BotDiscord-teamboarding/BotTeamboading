package com.meli.teamboardingBot.core.ports.defaultclient;

import org.springframework.http.ResponseEntity;

public interface PostDefaultClientPort {
    String post(String endpoint, String payload);
}
