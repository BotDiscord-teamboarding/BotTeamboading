package com.meli.teamboardingBot.controller;

import com.meli.teamboardingBot.context.DiscordUserContext;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.GoogleAuthIntegrationService;
import com.meli.teamboardingBot.service.SquadLogService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private final GoogleAuthIntegrationService googleAuthIntegration;
    private final DiscordUserAuthenticationService authService;
    private final FormStateService formStateService;
    private final SquadLogService squadLogService;
    private final JDA jda;
    private final com.meli.teamboardingBot.service.UserInteractionChannelService channelService;

    public GoogleOAuthCallbackController(
            GoogleAuthIntegrationService googleAuthIntegration,
            DiscordUserAuthenticationService authService,
            FormStateService formStateService,
            SquadLogService squadLogService,
            JDA jda,
            com.meli.teamboardingBot.service.UserInteractionChannelService channelService) {
        this.googleAuthIntegration = googleAuthIntegration;
        this.authService = authService;
        this.formStateService = formStateService;
        this.squadLogService = squadLogService;
        this.jda = jda;
        this.channelService = channelService;
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
        
        logger.info("=".repeat(80));
        logger.info("CALLBACK DO GOOGLE OAUTH RECEBIDO");
        logger.info("=".repeat(80));
        logger.info("State (Discord User ID): {}", state);
        logger.info("Code (encoded): {}", code);
        logger.info("Scope: {}", scope);
        logger.info("Auth User: {}", authuser);
        logger.info("HD: {}", hd);
        logger.info("Prompt: {}", prompt);
        logger.info("Error: {}", error);
        logger.info("-".repeat(80));

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
            
            // 1. Trocar code por token
            String accessToken = googleAuthIntegration.exchangeCodeForToken(decodedCode, state);
            
            if (accessToken != null) {
                logger.info("✅ Token obtido com sucesso para o usuário Discord: {}", state);
                
                // 2. Salvar token no serviço de autenticação
                authService.authenticateUserWithToken(state, accessToken);
                logger.info("✅ Token salvo no DiscordUserAuthenticationService para usuário: {}", state);
                
                // 3. Enviar mensagem privada ao usuário no Discord com menu de squads
                logger.info("🔄 Iniciando envio de menu de squads...");
                try {
                    sendSquadMenuToUser(state);
                    logger.info("✅ Método sendSquadMenuToUser executado sem exceções");
                } catch (Exception e) {
                    logger.error("❌ ERRO ao executar sendSquadMenuToUser: {}", e.getMessage(), e);
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

    /**
     * Envia menu de squads no canal onde o usuário iniciou a interação
     */
    private void sendSquadMenuToUser(String discordUserId) {
        try {
            logger.info("📨 [STEP 1/6] Enviando menu de squads para usuário Discord: {}", discordUserId);
            
            // Buscar canal de interação registrado
            logger.info("📨 [STEP 2/6] Buscando canal registrado...");
            String channelId = channelService.getUserChannelId(discordUserId);
            String messageId = channelService.getUserMessageId(discordUserId);
            logger.info("📨 [STEP 2/6] Canal obtido: channelId={}, messageId={}", channelId, messageId);
            
            if (channelId == null) {
                logger.error("❌ Canal não encontrado para usuário: {}", discordUserId);
                return;
            }
            
            logger.info("📍 [STEP 3/6] Usando canal registrado: channelId={}, messageId={}", channelId, messageId);
            
            // Inicializar FormState
            logger.info("📨 [STEP 4/6] Inicializando FormState...");
            FormState state = formStateService.getOrCreateState(Long.parseLong(discordUserId));
            state.setCreating(true);
            state.setEditing(false);
            state.setStep(FormStep.SQUAD_SELECTION);
            formStateService.updateState(Long.parseLong(discordUserId), state);
            logger.info("✅ [STEP 4/6] FormState inicializado para usuário {} no step SQUAD_SELECTION", discordUserId);
            
            // Buscar canal e enviar menu
            logger.info("📨 [STEP 5/6] Buscando canal no Discord...");
            var channel = jda.getTextChannelById(channelId);
            
            if (channel == null) {
                logger.error("❌ Canal {} não encontrado no Discord", channelId);
                return;
            }
            
            logger.info("✅ [STEP 5/6] Canal encontrado, iniciando busca de squads...");
            
            try {
                DiscordUserContext.setCurrentUserId(discordUserId);
                
                String squadsJson = squadLogService.getSquads();
                JSONObject obj = new JSONObject(squadsJson);
                JSONArray squadsArray = obj.optJSONArray("items");
                
                if (squadsArray == null || squadsArray.length() == 0) {
                    logger.warn("⚠️ Nenhuma squad encontrada para usuário: {}", discordUserId);
                    sendErrorToChannel(channel, 
                        "Nenhuma squad encontrada", 
                        "A API não retornou nenhuma squad disponível para seu usuário.");
                    return;
                }
                
                // Construir menu de squads
                StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                        .setPlaceholder("Escolha sua squad");
                
                for (int i = 0; i < squadsArray.length(); i++) {
                    JSONObject squad = squadsArray.getJSONObject(i);
                    String squadName = squad.optString("name", "");
                    String squadId = String.valueOf(squad.get("id"));
                    if (!squadName.isEmpty()) {
                        squadMenuBuilder.addOption(squadName, squadId);
                    }
                }
                
                // EDITAR MENSAGEM ORIGINAL (mantém ephemeral)
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("✅ Autenticação Google concluída!")
                        .setDescription("🏢 Selecione a squad para o seu log:")
                        .setColor(0x00FF00);
                
                logger.info("📨 [STEP 6/6] Editando mensagem original com menu de squads...");
                
                if (messageId != null) {
                    // Editar a mensagem original da interação
                    channel.retrieveMessageById(messageId).queue(
                        message -> {
                            message.editMessageEmbeds(embed.build())
                                    .setActionRow(squadMenuBuilder.build())
                                    .queue(
                                        success -> {
                                            logger.info("✅ [STEP 6/6] Mensagem editada com sucesso para usuário: {}", discordUserId);
                                            channelService.clearUserChannel(discordUserId);
                                        },
                                        error -> logger.error("❌ [STEP 6/6] Erro ao editar mensagem: {}", error.getMessage())
                                    );
                        },
                        error -> {
                            logger.error("❌ Erro ao recuperar mensagem {}: {}", messageId, error.getMessage());
                            logger.warn("⚠️ Não foi possível editar a mensagem original. Usuário precisa usar /squad-log novamente.");
                            channelService.clearUserChannel(discordUserId);
                        }
                    );
                } else {
                    logger.warn("⚠️ MessageId não encontrado. Não é possível enviar menu de squads.");
                    logger.info("💡 Usuário {} precisa usar /squad-log para continuar.", discordUserId);
                    channelService.clearUserChannel(discordUserId);
                }
                
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                logger.error("❌ Erro HTTP ao buscar squads para usuário {}: {} - {}", 
                    discordUserId, e.getStatusCode(), e.getResponseBodyAsString());
                
                String errorDetail = extractErrorDetail(e.getResponseBodyAsString());
                sendErrorToChannel(channel, 
                    "Erro ao carregar squads", 
                    String.format("**Status HTTP:** %s %s\n**Detalhes:** %s", 
                        e.getStatusCode().value(), 
                        e.getStatusText(),
                        errorDetail));
            } catch (Exception e) {
                logger.error("❌ Erro ao buscar squads para usuário {}: {}", discordUserId, e.getMessage(), e);
                sendErrorToChannel(channel, 
                    "Erro ao carregar squads", 
                    String.format("**Tipo:** %s\n**Mensagem:** %s", 
                        e.getClass().getSimpleName(), 
                        e.getMessage()));
            } finally {
                DiscordUserContext.clear();
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar menu de squads para usuário {}: {}", discordUserId, e.getMessage(), e);
        }
    }


    private void sendErrorToChannel(TextChannel channel,
                                    String errorTitle,
                                    String errorDescription) {
        EmbedBuilder errorEmbed = new EmbedBuilder()
                .setTitle("❌ " + errorTitle)
                .setDescription(errorDescription + "\n\n💡 **O que fazer:**\n" +
                        "• Verifique se você tem permissões adequadas\n" +
                        "• Tente fazer logout e login novamente\n" +
                        "• Use o comando `/squad-log` para tentar novamente")
                .setColor(0xFF0000)
                .setFooter("Se o erro persistir, contate o administrador do sistema");
        
        // Nota: Este método é chamado em contexto de erro onde não temos messageId
        // A mensagem será pública no canal, mas é um caso de erro raro
        logger.warn("⚠️ Enviando mensagem de erro pública no canal (contexto de erro)");
        channel.sendMessageEmbeds(errorEmbed.build())
                .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                .queue(
                    success -> logger.info("✅ Mensagem de erro enviada ao usuário"),
                    error -> logger.error("❌ Falha ao enviar mensagem de erro: {}", error.getMessage())
                );
    }



    private void notifyUserAboutError(String discordUserId, String errorTitle, String errorDescription) {
        try {
            logger.info("📨 Notificando usuário {} sobre erro: {}", discordUserId, errorTitle);
            
            String channelId = channelService.getUserChannelId(discordUserId);
            
            if (channelId != null) {
                var channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    sendErrorToChannel(channel, errorTitle, errorDescription);
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
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return "Sem detalhes adicionais";
            }
            
            JSONObject errorJson = new JSONObject(responseBody);
            
            // Tentar extrair campo "detail"
            if (errorJson.has("detail")) {
                return errorJson.getString("detail");
            }
            
            // Tentar extrair campo "message"
            if (errorJson.has("message")) {
                return errorJson.getString("message");
            }
            
            // Tentar extrair campo "error"
            if (errorJson.has("error")) {
                return errorJson.getString("error");
            }
            
            // Retornar o JSON completo se não encontrar campos conhecidos
            return responseBody;
            
        } catch (Exception e) {
            logger.warn("Não foi possível parsear corpo do erro: {}", responseBody);
            return responseBody != null ? responseBody : "Erro desconhecido";
        }
    }
}
