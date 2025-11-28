package com.meli.teamboardingBot.adapters.out.rest;

import com.meli.teamboardingBot.adapters.config.http.HttpHeadersFactory;
import com.meli.teamboardingBot.core.ports.rest.RestPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateApi implements RestPort {

    private final HttpHeadersFactory headersFactory;

    private final RestTemplate restTemplate = new RestTemplate();

    public RestTemplateApi(HttpHeadersFactory headersFactory) {
        this.headersFactory = headersFactory;
    }

    @Override
    public String getExchangeWithStringType(String url, String token) {
        HttpEntity<Void> request = new HttpEntity<>(headersFactory.createAuthHeaders(token));
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    @Override
    public String postExchange(String url, String token, String payload) {
        try {
            HttpEntity<String> request = new HttpEntity<>(payload, headersFactory.createJsonHeaders(token));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("HTTP Error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }

    @Override
    public String putExchange(String url, String token, String payload) {
        try {
            HttpEntity<String> request = new HttpEntity<>(payload, headersFactory.createJsonHeaders(token));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, request, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("HTTP Error " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }
}
