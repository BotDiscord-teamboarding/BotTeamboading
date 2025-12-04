package com.meli.teamboardingBot.core.ports.defaultclient;

public interface GetDefaultClientPort {
    String get(String endpoint);
    String get(String endpoint, String queryParams);
    String post(String endpoint, String payload);
    String put(String endpoint, String payload);
}
