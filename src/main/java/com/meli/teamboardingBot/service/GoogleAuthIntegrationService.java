package com.meli.teamboardingBot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.Map;

@Service
public class GoogleAuthIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthIntegrationService.class);

    @Value("${api.auth.google.connection.url:https://api.prod.tq.teamcubation.com/auth/get_google_login_connection_url}")
    private String googleConnectionUrl;

    @Value("${api.auth.google.login.url:https://api.prod.tq.teamcubation.com/auth/google_login}")
    private String googleLoginUrl;

    private final RestTemplate restTemplate;

    public GoogleAuthIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public String getGoogleLoginConnectionUrl(String discordUserId) {
        try {
            logger.info("=".repeat(80));
            logger.info("OBTENDO URL DE AUTENTICAÇÃO GOOGLE");
            logger.info("=".repeat(80));
            logger.info("Discord User ID: {}", discordUserId);
            
            String urlWithParams = googleConnectionUrl + "?state=" + discordUserId + "&from_discord=true";
            logger.info("Endpoint da API: {}", urlWithParams);

            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(headers);

            String response = restTemplate.exchange(
                    urlWithParams,
                    HttpMethod.GET,
                    request,
                    String.class
            ).getBody();
            logger.info("Response from Google API: {}", response);



            if (response != null && response.startsWith("\"") && response.endsWith("\"")) {
                response = response.substring(1, response.length() - 1);
            }

            logger.info("-".repeat(80));
            logger.info("URL COMPLETA DO GOOGLE:");
            logger.info("{}", response);
            logger.info("-".repeat(80));
            
            if (response != null && response.contains("?")) {
                String[] parts = response.split("\\?");
                logger.info("Base URL: {}", parts[0]);
                
                if (parts.length > 1) {
                    logger.info("PARÂMETROS:");
                    String[] params = parts[1].split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = keyValue[0];
                            String value = URLDecoder.decode(keyValue[1], "UTF-8");
                            logger.info("  {} = {}", key, value);
                        }
                    }
                }
            }
            
            logger.info("=".repeat(80));
            return response;

        } catch (Exception e) {
            logger.error("Erro ao obter URL de conexão Google", e);
            throw new RuntimeException("Falha ao obter URL de conexão: " + e.getMessage(), e);
        }
    }

    public String exchangeCodeForToken(String code, String discordUserId) {
        try {
            logger.info("=".repeat(80));
            logger.info("TROCANDO CODE POR TOKEN");
            logger.info("=".repeat(80));
            logger.info("Discord User ID: {}", discordUserId);
            logger.info("Code recebido (já decodificado): {}", code);
            
            String encodedCode = java.net.URLEncoder.encode(code, "UTF-8");
            String url = googleLoginUrl + "?code=" + code + "&from_discord=true";
            
            logger.info("Chamando: POST {}", googleLoginUrl);
            logger.info("Query params: code={}, from_discord=true", encodedCode);
            logger.info("URL completa: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>("", headers);
            
            logger.info("Headers: {}", headers);
            logger.info("Body: (vazio)");
            
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            logger.info("-".repeat(80));
            logger.info("RESPOSTA DA API:");
            if (response != null) {
                response.forEach((key, value) -> {
                    if ("access_token".equals(key)) {
                        String token = (String) value;
                        logger.info("  {} = {}...", key, token.substring(0, Math.min(20, token.length())));
                    } else {
                        logger.info("  {} = {}", key, value);
                    }
                });
            }
            logger.info("-".repeat(80));
            
            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                logger.info("✅ Token obtido com sucesso para Discord User ID: {}", discordUserId);
                logger.info("=".repeat(80));
                return accessToken;
            } else {
                logger.error("❌ Resposta não contém access_token: {}", response);
                throw new RuntimeException("Token não encontrado na resposta");
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("❌ Erro HTTP ao trocar code por token");
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Response Body: {}", e.getResponseBodyAsString());
            logger.error("Headers: {}", e.getResponseHeaders());
            
            String errorMessage = String.format("Erro %s: %s", 
                e.getStatusCode(), 
                e.getResponseBodyAsString().isEmpty() ? "Sem detalhes" : e.getResponseBodyAsString()
            );
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            logger.error("❌ Erro ao trocar code por token", e);
            throw new RuntimeException("Falha ao obter token: " + e.getMessage(), e);
        }
    }
}