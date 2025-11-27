package com.meli.teamboardingBot.adapters.in.controller;

import com.meli.teamboardingBot.core.ports.discorduserauthentication.DiscordUserAuthenticationPort;
import com.meli.teamboardingBot.adapters.out.oath.googleauth.ports.ExchangeCodeForTokenPort;
import com.meli.teamboardingBot.adapters.out.language.UserInteractionChannelService;
import com.meli.teamboardingBot.adapters.out.language.UserLanguageService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class GoogleOAuthCallbackController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthCallbackController.class);
    
    private final ExchangeCodeForTokenPort googleAuthIntegration;
    private final DiscordUserAuthenticationPort authService;
    private final JDA jda;
    private final UserInteractionChannelService channelService;
    private final UserLanguageService languageService;

    public GoogleOAuthCallbackController(
            ExchangeCodeForTokenPort googleAuthIntegration,
            DiscordUserAuthenticationPort authService,
            JDA jda,
            UserInteractionChannelService channelService,
            UserLanguageService languageService) {
        this.googleAuthIntegration = googleAuthIntegration;
        this.authService = authService;
        this.jda = jda;
        this.channelService = channelService;
        this.languageService = languageService;
    }

    @GetMapping("/login/oauth2/code/google")
    public RedirectView handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "authuser", required = false) String authuser,
            @RequestParam(value = "hd", required = false) String hd,
            @RequestParam(value = "prompt", required = false) String prompt,
            @RequestParam(value = "error", required = false) String error) {
        
        logger.info("Callback recebido do Google para usuário: {}", state);

        if (error != null) {
            logger.error("❌ Erro retornado pelo Google: {}", error);
            return new RedirectView("/auth-error.html");
        }

        if (code == null || code.trim().isEmpty()) {
            logger.error("❌ Code não fornecido no callback");
            return new RedirectView("/auth-error.html");
        }

        try {
            String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);
            logger.info("Code (decoded): {}", decodedCode);
            
            String accessToken = googleAuthIntegration.exchangeCodeForToken(decodedCode, state);
            
            if (accessToken != null) {
                logger.info("✅ Token obtido com sucesso para o usuário Discord: {}", state);
                
                authService.authenticateUserWithToken(state, accessToken);
                logger.info("✅ Token salvo no DiscordUserAuthenticationService para usuário: {}", state);
                
                logger.info("🔄 Enviando confirmação de autenticação...");
                try {
                    sendAuthenticationSuccessMessage(state);
                    logger.info("✅ Mensagem de sucesso enviada");
                } catch (Exception e) {
                    logger.error("❌ ERRO ao enviar mensagem de sucesso: {}", e.getMessage(), e);
                }
                
                logger.info("=".repeat(80));
                return new RedirectView("/auth-success.html");
            } else {
                logger.warn("⚠️ Falha ao obter token");
                notifyUserAboutError(state, "Falha ao obter token", 
                    "Não foi possível trocar o código de autorização por um token de acesso.");
                return new RedirectView("/auth-error.html");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("❌ Erro HTTP ao processar callback: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            
            String errorDetail = extractErrorDetail(e.getResponseBodyAsString());
            notifyUserAboutError(state, 
                String.format("Erro %s ao autenticar", e.getStatusCode().value()),
                String.format("**Status:** %s %s\n**Detalhes:** %s", 
                    e.getStatusCode().value(), e.getStatusText(), errorDetail));
            return new RedirectView("/auth-error.html");
        } catch (Exception e) {
            logger.error("❌ Erro ao processar callback do Google: {}", e.getMessage(), e);
            notifyUserAboutError(state, "Erro na autenticação", 
                String.format("**Tipo:** %s\n**Mensagem:** %s", 
                    e.getClass().getSimpleName(), e.getMessage()));
            return new RedirectView("/auth-error.html");
        }
    }
    

    private void sendAuthenticationSuccessMessage(String discordUserId) {
        try {
            logger.info("📨 Enviando confirmação de autenticação para usuário: {}", discordUserId);
            
            String channelId = channelService.getUserChannelId(discordUserId);
            String messageId = channelService.getUserMessageId(discordUserId);
            
            if (channelId == null) {
                logger.error("❌ Canal não encontrado para usuário: {}", discordUserId);
                return;
            }
            
            var channel = jda.getTextChannelById(channelId);
            
            if (channel == null) {
                logger.error("❌ Canal {} não encontrado no Discord", channelId);
                return;
            }
            
            java.util.Locale userLocale = languageService.getUserLanguagePreference(discordUserId);
            boolean isSpanish = userLocale != null && "es".equals(userLocale.getLanguage());
            
            String title = isSpanish 
                ? "✅ Autenticación Google completada!"
                : "✅ Autenticação Google concluída!";
            
            String successMsg = isSpanish
                ? "Has sido autenticado con éxito!"
                : "Você foi autenticado com sucesso!";
            
            String commandsTitle = isSpanish
                ? "📋 Comandos Disponibles:"
                : "📋 Comandos Disponíveis:";
            
            String squadLogDesc = isSpanish
                ? "Crear o actualizar squad log"
                : "Criar ou atualizar squad log";
            
            String squadLogLoteDesc = isSpanish
                ? "Crear múltiples logs a la vez"
                : "Criar múltiplos logs de uma vez";
            
            String statusDesc = isSpanish
                ? "Verificar tu estado de autenticación"
                : "Verificar seu status de autenticação";
            
            String helpDesc = isSpanish
                ? "Ver todos los comandos disponibles"
                : "Ver todos os comandos disponíveis";
            
            String footerMsg = isSpanish
                ? "💡 Usa los comandos anteriores para comenzar!"
                : "💡 Use os comandos acima para começar!";
            
            String description = successMsg + "\n\n" + commandsTitle + "\n" +
                "• `/squad-log` - " + squadLogDesc + "\n" +
                "• `/squad-log-lote` - " + squadLogLoteDesc + "\n" +
                "• `/status` - " + statusDesc + "\n" +
                "• `/help` - " + helpDesc + "\n\n" +
                footerMsg;
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(0x00FF00);
            
            if (messageId != null) {
                channel.retrieveMessageById(messageId).queue(
                    message -> {
                        message.editMessageEmbeds(embed.build())
                                .setComponents()
                                .queue(
                                    success -> {
                                        logger.info("✅ Mensagem de sucesso editada para usuário: {}", discordUserId);
                                        channelService.clearUserChannel(discordUserId);
                                    },
                                    error -> logger.error("❌ Erro ao editar mensagem: {}", error.getMessage())
                                );
                    },
                    error -> {
                        logger.error("❌ Erro ao recuperar mensagem {}: {}", messageId, error.getMessage());
                        channelService.clearUserChannel(discordUserId);
                    }
                );
            } else {
                logger.warn("⚠️ MessageId não encontrado.");
                channelService.clearUserChannel(discordUserId);
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar mensagem de sucesso para usuário {}: {}", discordUserId, e.getMessage(), e);
        }
    }



    private void notifyUserAboutError(String discordUserId, String errorTitle, String errorDescription) {
        try {
            logger.info("📨 Notificando usuário {} sobre erro: {}", discordUserId, errorTitle);
            
            String channelId = channelService.getUserChannelId(discordUserId);
            
            if (channelId != null) {
                var channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    java.util.Locale userLocale = languageService.getUserLanguagePreference(discordUserId);
                    boolean isSpanish = userLocale != null && "es".equals(userLocale.getLanguage());
                    
                    String whatToDo = isSpanish ? "Qué hacer" : "O que fazer";
                    String tryAgain = isSpanish 
                        ? "Intenta cerrar sesión y volver a iniciar sesión"
                        : "Tente fazer logout e login novamente";
                    String useStart = isSpanish
                        ? "Usa el comando `/start` para intentar de nuevo"
                        : "Use o comando `/start` para tentar novamente";
                    String contactAdmin = isSpanish
                        ? "Si el error persiste, contacta al administrador"
                        : "Se o erro persistir, contate o administrador";
                    
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ " + errorTitle)
                            .setDescription(errorDescription + "\n\n💡 **" + whatToDo + ":**\n" +
                                    "• " + tryAgain + "\n" +
                                    "• " + useStart)
                            .setColor(0xFF0000)
                            .setFooter(contactAdmin);
                    
                    channel.sendMessageEmbeds(errorEmbed.build()).queue();
                    channelService.clearUserChannel(discordUserId);
                } else {
                    logger.error("❌ Canal {} não encontrado no Discord", channelId);
                }
            } else {
                logger.warn("⚠️ Canal não registrado para usuário {}, não foi possível notificar erro", discordUserId);
            }
        } catch (Exception e) {
            logger.error("❌ Erro ao tentar notificar usuário sobre erro: {}", e.getMessage());
        }
    }


    private String extractErrorDetail(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "Sem detalhes adicionais / No additional details";
        }
        return responseBody;
    }
}
