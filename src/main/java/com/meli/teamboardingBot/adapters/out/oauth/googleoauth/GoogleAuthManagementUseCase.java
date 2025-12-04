package com.meli.teamboardingBot.adapters.out.oauth.googleoauth;

import com.meli.teamboardingBot.adapters.out.oauth.googleoauth.ports.ExchangeCodeForTokenPort;
import com.meli.teamboardingBot.adapters.out.oauth.googleoauth.ports.GetGoogleLoginUrlPort;
import com.meli.teamboardingBot.core.ports.logger.LoggerApiPort;
import com.meli.teamboardingBot.core.ports.rest.RestPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.util.Map;

public class GoogleAuthManagementUseCase extends GoogleAuthAbstract 
        implements GetGoogleLoginUrlPort, ExchangeCodeForTokenPort {

    private final RestTemplate restTemplate;

    public GoogleAuthManagementUseCase(LoggerApiPort loggerApiPort, RestPort restPort,
                                      RestTemplate restTemplate,
                                      String googleConnectionUrl, String googleLoginUrl) {
        super(loggerApiPort, restPort, googleConnectionUrl, googleLoginUrl);
        this.restTemplate = restTemplate;
    }

    @Override
    public String getGoogleLoginConnectionUrl(String discordUserId) {
        try {
            loggerApiPort.info("=".repeat(80));
            loggerApiPort.info("OBTENDO URL DE AUTENTICAÇÃO GOOGLE");
            loggerApiPort.info("=".repeat(80));
            loggerApiPort.info("Discord User ID: {}", discordUserId);
            
            String urlWithParams = googleConnectionUrl + "?state=" + discordUserId + "&from_discord=true";
            loggerApiPort.info("Endpoint da API: {}", urlWithParams);

            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(headers);

            String response = restTemplate.exchange(
                    urlWithParams,
                    HttpMethod.GET,
                    request,
                    String.class
            ).getBody();
            loggerApiPort.info("Response from Google API: {}", response);

            if (response != null && response.startsWith("\"") && response.endsWith("\"")) {
                response = response.substring(1, response.length() - 1);
            }

            loggerApiPort.info("-".repeat(80));
            loggerApiPort.info("URL COMPLETA DO GOOGLE:");
            loggerApiPort.info("{}", response);
            loggerApiPort.info("-".repeat(80));
            
            if (response != null && response.contains("?")) {
                String[] parts = response.split("\\?");
                loggerApiPort.info("Base URL: {}", parts[0]);
                
                if (parts.length > 1) {
                    loggerApiPort.info("PARÂMETROS:");
                    String[] params = parts[1].split("&");
                    for (String param : params) {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2) {
                            String key = keyValue[0];
                            String value = URLDecoder.decode(keyValue[1], "UTF-8");
                            loggerApiPort.info("  {} = {}", key, value);
                        }
                    }
                }
            }
            
            loggerApiPort.info("=".repeat(80));
            return response;

        } catch (Exception e) {
            loggerApiPort.error("Erro ao obter URL de conexão Google: {}", e.getMessage());
            throw new RuntimeException("Falha ao obter URL de conexão: " + e.getMessage(), e);
        }
    }

    @Override
    public String exchangeCodeForToken(String code, String discordUserId) {
        try {
            if (processedCodes.containsKey(code)) {
                loggerApiPort.warn("⚠️ Code OAuth já foi usado anteriormente. Ignorando requisição duplicada.");
                loggerApiPort.warn("Discord User ID: {}", discordUserId);
                loggerApiPort.warn("Code: {}...", code.substring(0, Math.min(20, code.length())));
                throw new RuntimeException("Code OAuth já foi utilizado. Por favor, faça login novamente.");
            }
            
            processedCodes.put(code, discordUserId);
            codeTimestamps.put(code, System.currentTimeMillis());
            
            cleanupOldCodes();
            
            loggerApiPort.info("=".repeat(80));
            loggerApiPort.info("TROCANDO CODE POR TOKEN");
            loggerApiPort.info("=".repeat(80));
            loggerApiPort.info("Discord User ID: {}", discordUserId);
            loggerApiPort.info("Code recebido (já decodificado): {}", code);
            
            String encodedCode = java.net.URLEncoder.encode(code, "UTF-8");
            String url = googleLoginUrl + "?code=" + code + "&from_discord=true";
            
            loggerApiPort.info("Chamando: POST {}", googleLoginUrl);
            loggerApiPort.info("Query params: code={}, from_discord=true", encodedCode);
            loggerApiPort.info("URL completa: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json");
            
            HttpEntity<String> request = new HttpEntity<>("", headers);
            
            loggerApiPort.info("Headers: {}", headers);
            loggerApiPort.info("Body: (vazio)");
            
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            loggerApiPort.info("-".repeat(80));
            loggerApiPort.info("RESPOSTA DA API:");
            if (response != null) {
                response.forEach((key, value) -> {
                    if ("access_token".equals(key)) {
                        String token = (String) value;
                        loggerApiPort.info("  {} = {}...", key, token.substring(0, Math.min(20, token.length())));
                    } else {
                        loggerApiPort.info("  {} = {}", key, value);
                    }
                });
            }
            loggerApiPort.info("-".repeat(80));
            
            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                loggerApiPort.info("✅ Token obtido com sucesso para Discord User ID: {}", discordUserId);
                loggerApiPort.info("=".repeat(80));
                return accessToken;
            } else {
                loggerApiPort.error("❌ Resposta não contém access_token: {}", response);
                throw new RuntimeException("Token não encontrado na resposta");
            }
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            loggerApiPort.error("❌ Erro HTTP ao trocar code por token");
            loggerApiPort.error("Status Code: {}", e.getStatusCode());
            loggerApiPort.error("Response Body: {}", e.getResponseBodyAsString());
            loggerApiPort.error("Headers: {}", e.getResponseHeaders());
            
            String errorMessage = String.format("Erro %s: %s", 
                e.getStatusCode(), 
                e.getResponseBodyAsString().isEmpty() ? "Sem detalhes" : e.getResponseBodyAsString()
            );
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            loggerApiPort.error("❌ Erro ao trocar code por token: {}", e.getMessage());
            throw new RuntimeException("Falha ao obter token: " + e.getMessage(), e);
        }
    }
}
