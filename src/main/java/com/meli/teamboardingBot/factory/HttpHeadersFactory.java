package com.meli.teamboardingBot.factory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

public interface HttpHeadersFactory {
    HttpHeaders createAuthHeaders(String token);
    HttpHeaders createJsonHeaders(String token);
}
