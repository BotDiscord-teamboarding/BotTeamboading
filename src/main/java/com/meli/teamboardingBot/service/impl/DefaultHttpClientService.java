package com.meli.teamboardingBot.service.impl;
import com.meli.teamboardingBot.context.DiscordUserContext;
import com.meli.teamboardingBot.dto.AuthTokenResponseDTO;
import com.meli.teamboardingBot.factory.HttpHeadersFactory;
import com.meli.teamboardingBot.service.AuthenticationService;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.HttpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service
public class DefaultHttpClientService implements HttpClientService {
    private final Logger logger = LoggerFactory.getLogger(DefaultHttpClientService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final AuthenticationService authService;
    private final DiscordUserAuthenticationService discordAuthService;
    private final HttpHeadersFactory headersFactory;
    private final String apiUrl;
    @Autowired
    public DefaultHttpClientService(AuthenticationService authService,
                                  DiscordUserAuthenticationService discordAuthService,
                                  HttpHeadersFactory headersFactory,
                                  @Value("${api.url}") String apiUrl) {
        this.authService = authService;
        this.discordAuthService = discordAuthService;
        this.headersFactory = headersFactory;
        this.apiUrl = apiUrl;
    }
    

    private String getAuthToken() {
        String discordUserId = DiscordUserContext.getCurrentUserId();
        if (discordUserId != null) {
            AuthTokenResponseDTO userToken = discordAuthService.getUserToken(discordUserId);
            if (userToken != null) {
                logger.debug("Usando token do usuário Discord: {}", discordUserId);
                return userToken.getAccessToken();
            }
        }
        logger.debug("Usando token padrão (credenciais do application.properties)");
        return authService.getAuthToken().getAccessToken();
    }
    @Override
    public String get(String endpoint) {
        String token = getAuthToken();
        HttpEntity<Void> request = new HttpEntity<>(headersFactory.createAuthHeaders(token));
        String fullUrl = apiUrl + endpoint;
        logger.info("GET request to: {}", fullUrl);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.GET, request, String.class);
            logger.info("GET request successful");
            return response.getBody();
        } catch (Exception e) {
            logger.error("GET request failed: {}", e.getMessage());
            throw e;
        }
    }
    @Override
    public String get(String endpoint, String queryParams) {
        String fullEndpoint = endpoint + "?" + queryParams;
        return get(fullEndpoint);
    }
    @Override
    public ResponseEntity<String> post(String endpoint, String payload) {
        String token = getAuthToken();
        HttpEntity<String> request = new HttpEntity<>(payload, headersFactory.createJsonHeaders(token));
        String fullUrl = apiUrl + endpoint;
        logger.info("POST request to: {}", fullUrl);
        logger.info("Payload: {}", payload);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.POST, request, String.class);
            logger.info("POST request successful");
            return response;
        } catch (Exception e) {
            logger.error("POST request failed: {}", e.getMessage());
            throw e;
        }
    }
    @Override
    public ResponseEntity<String> put(String endpoint, String payload) {
        String token = getAuthToken();
        HttpEntity<String> request = new HttpEntity<>(payload, headersFactory.createJsonHeaders(token));
        String fullUrl = apiUrl + endpoint;
        logger.info("PUT request to: {}", fullUrl);
        logger.info("Payload: {}", payload);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                fullUrl, HttpMethod.PUT, request, String.class);
            logger.info("PUT request successful");
            return response;
        } catch (Exception e) {
            logger.error("PUT request failed: {}", e.getMessage());
            throw e;
        }
    }
}
