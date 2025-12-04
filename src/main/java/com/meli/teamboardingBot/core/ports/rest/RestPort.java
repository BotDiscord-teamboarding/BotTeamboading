package com.meli.teamboardingBot.core.ports.rest;

public interface RestPort {

    String getExchangeWithStringType (String url, String token);

    String postExchange (String url, String token, String payload);

    String putExchange (String url, String token, String payload);

}
