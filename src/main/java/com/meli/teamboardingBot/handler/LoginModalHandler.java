package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.context.DiscordUserContext;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.config.MessageConfig;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.GoogleAuthIntegrationService;
import com.meli.teamboardingBot.service.SquadLogService;
import com.meli.teamboardingBot.config.MessageConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;


@Component
public class LoginModalHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LoginModalHandler.class);
    private final DiscordUserAuthenticationService authService;
    private final FormStateService formStateService;
    private final SquadLogService squadLogService;
    private final GoogleAuthIntegrationService googleAuthIntegration;
    private final com.meli.teamboardingBot.service.UserInteractionChannelService channelService;

    public LoginModalHandler(DiscordUserAuthenticationService authService,
                             FormStateService formStateService,
                             SquadLogService squadLogService,
                             GoogleAuthIntegrationService googleAuthIntegration,
                             com.meli.teamboardingBot.service.UserInteractionChannelService channelService) {
        this.authService = authService;
        this.formStateService = formStateService;
        this.squadLogService = squadLogService;
        this.googleAuthIntegration = googleAuthIntegration;
        this.channelService = channelService;
    }
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        try {
            if ("auth-manual".equals(buttonId)) {
                handleManualAuthButton(event);
                return;
            }

            if ("auth-google".equals(buttonId)) {
                handleGoogleAuthButton(event);
                return;
            }
            
            if ("btn-autenticar".equals(buttonId)) {
                handleAuthenticationMethodSelection(event);
                return;
            }

            if ("btn-auth-manual".equals(buttonId)) {
                handleManualAuthButton(event);
                return;
            }

            if ("btn-auth-google".equals(buttonId)) {
                handleGoogleAuthButton(event);
                return;
            }

            if ("btn-submit-google-code".equals(buttonId)) {
                handleGoogleCodeSubmission(event);
                return;
            }
        } catch (IllegalStateException e) {
            logger.warn("Interação já foi processada ou expirou para usuário {}: {}", 
                event.getUser().getId(), e.getMessage());
        }
    }

    private void handleAuthenticationMethodSelection(ButtonInteractionEvent event) {
        logger.info("Botão autenticar clicado pelo usuário: {}", event.getUser().getId());

        event.deferEdit().queue(hook -> {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🔐 " + messageSource.getMessage("txt_escolha_a_forma_de_autenticacao", null, formState.getLocale()) )
                    .setDescription(messageSource.getMessage("txt_selecione_como_dejea_fazer_login_no_sistema", null, formState.getLocale()) )
                    .addField("📝 "+ messageSource.getMessage("txt_manual", null, formState.getLocale()), messageSource.getMessage("txt_digite_suas_credenciais", null, formState.getLocale()) , false)
                    .addField("🌐 " + messageSource.getMessage("txt_google", null, formState.getLocale()) , messageSource.getMessage("txt_autentique_usando_sua_conta_coogle", null, formState.getLocale()) , false)
                    .setColor(0x5865F2);

            hook.editOriginalEmbeds(embed.build())
                    .setActionRow(
                            Button.primary("btn-auth-manual", "📝 " + messageSource.getMessage("txt_manual", null, formState.getLocale()) ),
                            Button.success("btn-auth-google", "🌐 " + messageSource.getMessage("txt_google", null, formState.getLocale()) ),
                            Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar", null, formState.getLocale()) )
                    )
                    .queue();
        });
    }


    private void handleManualAuthButton(ButtonInteractionEvent event) {
        logger.info("Autenticação manual selecionada pelo usuário: {}", event.getUser().getId());

        TextInput username = TextInput.create("username", messageSource.getMessage("txt_email", null, formState.getLocale()) , TextInputStyle.SHORT)
                .setPlaceholder(messageSource.getMessage("txt_digite_seu_email", null, formState.getLocale()) )
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(100)
                .build();

        TextInput password = TextInput.create("password", messageSource.getMessage("txt_senha", null, formState.getLocale()), TextInputStyle.SHORT)
                .setPlaceholder(messageSource.getMessage("txt_digite_sua_senha", null, formState.getLocale()) )
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("login-modal", "🔐 " + messageSource.getMessage("txt_login_squad_log", null, formState.getLocale()) )
                .addActionRow(username)
                .addActionRow(password)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleGoogleAuthButton(ButtonInteractionEvent event) {
        logger.info("Autenticação Google selecionada pelo usuário: {}", event.getUser().getId());

        String userId = event.getUser().getId();
        String channelId = event.getChannel().getId();
        String messageId = event.getMessageId();

        // Defer edit e aguardar callback antes de usar o hook
        event.deferEdit().queue(hook -> {
            try {
                // Registrar canal de interação para usar no callback
                channelService.registerUserChannel(userId, channelId, messageId);
                logger.info("📍 Canal registrado: userId={}, channelId={}, messageId={}", userId, channelId, messageId);
                
                String authUrl = googleAuthIntegration.getGoogleLoginConnectionUrl(userId);

                logger.info("URL de autenticação Google obtida da API: {}", authUrl);

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("🌐 " + messageSource.getMessage("txt_autenticar_com_google", null, formState.getLocale()) )
                            .setDescription("**" + messageSource.getMessage("txt_passo1", null, formState.getLocale()) + ":** " +
                                            messageSource.getMessage("txt_clique_no_link_abaixo_para_fazer_login_com_sua_conta_google", null, formState.getLocale()) +":\n\n" +

                                "🔗 [**" + messageSource.getMessage("txt_clique_aqui_para_autenticar", null, formState.getLocale()) + "**](" + authUrl + ")\n\n **" +
                                            messageSource.getMessage("txt_passo2", null, formState.getLocale()) +":** " + messageSource.getMessage("txt_apos_autenticar_vc_recebera_uma_confirmacao", null, formState.getLocale()) + ".\n\n" +
                                "⚠️ **" + messageSource.getMessage("txt_aguarde", null, formState.getLocale()) + "** " + messageSource.getMessage("txt_apos_fazer_login_no_google_a_resposta_aparecera", null, formState.getLocale()) + ".")
                        .setColor(0x4285F4)
                        .setFooter("Discord User ID: " + userId);

                hook.editOriginalEmbeds(embed.build())
                        .setActionRow(
                                Button.link(authUrl, "🌐 " + messageSource.getMessage("txt_autenticar_com_google", null, formState.getLocale()) ),
                                Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_cancelar", null, formState.getLocale()) )
                        )
                        .queue();

            } catch (Exception e) {
                logger.error("Erro ao obter URL de autenticação Google", e);

                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ " + messageSource.getMessage("txt_erro", null, formState.getLocale()))
                        .setDescription(messageSource.getMessage("txt_nao_foi_possivel_obter_a_url_de_autenticacao", null, formState.getLocale()) +". "+
                                messageSource.getMessage("txt_tente_novamente", null, formState.getLocale()) + ".")
                        .setColor(0xFF0000);

                hook.editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(Button.secondary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) ))
                        .queue();
            }
        });
    }

    private void handleGoogleCodeSubmission(ButtonInteractionEvent event) {
        logger.info("Botão inserir código Google clicado pelo usuário: {}", event.getUser().getId());

        TextInput codeInput = TextInput.create("google-code", messageSource.getMessage("txt_codigo_de_autorizacao", null, formState.getLocale()) , TextInputStyle.PARAGRAPH)
                .setPlaceholder(messageSource.getMessage("txt_cole_aqui_o_codigo_obtido_apos_autenticacao", null, formState.getLocale()) )
                .setMinLength(10)
                .setMaxLength(2000)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal-google-code", "🔑 " + messageSource.getMessage("txt_codigo_de_autorizacao_google", null, formState.getLocale()) )
                .addActionRow(codeInput)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if ("modal-google-code".equals(event.getModalId())) {
            handleGoogleCodeModal(event);
            return;
        }
        
        if (!event.getModalId().equals("login-modal")) {
            return;
        }

        String userId = event.getUser().getId();
        String username = event.getValue("username").getAsString();
        String password = event.getValue("password").getAsString();

        logger.info("Processando modal de login para usuário Discord: {}", userId);

        event.deferEdit().queue(hook -> {
            DiscordUserAuthenticationService.AuthResponse response =
                    authService.authenticateUser(userId, username, password);

            if (response.isSuccess()) {
                logger.info("Login bem-sucedido, carregando squads para usuário: {}", userId);

                FormState state = formStateService.getOrCreateState(Long.parseLong(userId));
                state.setCreating(true);
                state.setEditing(false);
                state.setStep(FormStep.SQUAD_SELECTION);
                formStateService.updateState(Long.parseLong(userId), state);

                try {
                    DiscordUserContext.setCurrentUserId(userId);

                    String squadsJson = squadLogService.getSquads();
                    JSONObject obj = new JSONObject(squadsJson);
                    JSONArray squadsArray = obj.optJSONArray("items");

                    if (squadsArray == null || squadsArray.length() == 0) {
                        EmbedBuilder errorEmbed = new EmbedBuilder()
                                .setTitle("❌ " + messageSource.getMessage("txt_nenhuma_squad_encontrada", null, formState.getLocale()) )
                                .setDescription(messageSource.getMessage("txt_nao_ha_squads_disponiveis_no_momento", null, formState.getLocale()) + "." )
                                .setColor(0xFF0000);
                        hook.editOriginalEmbeds(errorEmbed.build())
                                .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) ))
                                .queue();
                        return;
                    }

                    StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                            .setPlaceholder(messageSource.getMessage("txt_selecione_uma_squad", null, formState.getLocale()) );
                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        String squadName = squad.optString("name", "");
                        String squadId = String.valueOf(squad.get("id"));
                        if (!squadName.isEmpty()) {
                            squadMenuBuilder.addOption(squadName, squadId);
                        }
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("✅ " + messageSource.getMessage("txt_login_realizado_com_sucesso", null, formState.getLocale()) + "!")
                            .setDescription("🏢 " + messageSource.getMessage("txt_selecione_a_squad_para_o_seu_log", null, formState.getLocale()) + ":" )
                            .setColor(0x00FF00);

                    hook.editOriginalEmbeds(embed.build())
                            .setActionRow(squadMenuBuilder.build())
                            .queue();

                } catch (Exception e) {
                    logger.error("Erro ao carregar squads após login: {}", e.getMessage());
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ " + messageSource.getMessage("txt_erro_carregar_squads", null, formState.getLocale()) )
                            .setDescription(messageSource.getMessage("txt_login_realizado_mas_ocorreu_erro_ao_carregar_as_squads", null, formState.getLocale()) + ".\n\n" +
                                    messageSource.getMessage("txt_use_o_comando_squad_log_novamente", null, formState.getLocale()) + ".")
                            .setColor(0xFF0000);
                    hook.editOriginalEmbeds(errorEmbed.build())
                            .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) ))
                            .queue();
                } finally {
                    DiscordUserContext.clear();
                }
            } else {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ " + messageSource.getMessage("txt_falha_na_autenticacao", null, formState.getLocale()) )
                        .setDescription(response.getMessage() + "\n\n " + messageSource.getMessage("txt_tente_novamente", null, formState.getLocale()) +".")
                        .setColor(0xFF0000);
                hook.editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(
                                Button.success("btn-autenticar", "🔐 " + messageSource.getMessage("txt_tente_novamente", null, formState.getLocale()) ),
                                Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) )
                        )
                        .queue();
            }
        });
    }

    private void handleGoogleCodeModal(ModalInteractionEvent event) {
        String userId = event.getUser().getId();
        String code = event.getValue("google-code").getAsString().trim();

        logger.info("Processando código Google para usuário Discord: {}", userId);
        logger.info("Código recebido (primeiros 20 chars): {}...", code.substring(0, Math.min(20, code.length())));

        event.deferReply(true).queue(hook -> {
            try {
                // Trocar código por token
                logger.info("🔄 Trocando código por token...");
                String accessToken = googleAuthIntegration.exchangeCodeForToken(code, userId);
                logger.info("✅ Token obtido com sucesso!");

                // Autenticar usuário
                logger.info("🔐 Autenticando usuário...");
                authService.authenticateUserWithToken(userId, accessToken);
                logger.info("✅ Usuário {} autenticado via Google com sucesso!", userId);

                // Mostrar mensagem de sucesso PRIMEIRO
                EmbedBuilder successEmbed = new EmbedBuilder()
                        .setTitle("✅ " + messageSource.getMessage("txt_autenticado_com_sucesso", null, formState.getLocale()) + "!")
                        .setDescription(messageSource.getMessage("txt_sua_autenticacao_via_google_foi_realizada_com_sucesso", null, formState.getLocale()) +  "!\n\n" +
                                "🔄 " + messageSource.getMessage("txt_carregando_squads_disponiveis", null, formState.getLocale()) + "...")
                        .setColor(0x00FF00);
                
                hook.editOriginalEmbeds(successEmbed.build()).queue();
                logger.info("✅ Mensagem de sucesso enviada ao usuário");

                // Inicializar FormState
                FormState state = formStateService.getOrCreateState(Long.parseLong(userId));
                state.setCreating(true);
                state.setEditing(false);
                state.setStep(FormStep.SQUAD_SELECTION);
                formStateService.updateState(Long.parseLong(userId), state);
                logger.info("FormState inicializado para usuário {} no step SQUAD_SELECTION", userId);
                
                // Aguardar 1 segundo para o usuário ver a mensagem de sucesso
                Thread.sleep(1000);
                
                // Agora carregar squads
                try {
                    DiscordUserContext.setCurrentUserId(userId);
                    logger.info("✅ Contexto do usuário definido: {}", userId);
                    
                    boolean isAuthenticated = authService.isUserAuthenticated(userId);
                    logger.info("Usuário autenticado? {}", isAuthenticated);

                    logger.info("📞 Chamando squadLogService.getSquads()...");
                    String squadsJson = squadLogService.getSquads();
                    logger.info("📦 Resposta de getSquads() recebida: {} caracteres", squadsJson != null ? squadsJson.length() : "null");
                    
                    JSONObject obj = new JSONObject(squadsJson);
                    JSONArray squadsArray = obj.optJSONArray("items");
                    logger.info("📋 Squads array extraído: {} squads encontradas", squadsArray != null ? squadsArray.length() : "null");

                    if (squadsArray == null || squadsArray.length() == 0) {
                        EmbedBuilder errorEmbed = new EmbedBuilder()
                                .setTitle("❌ " + messageSource.getMessage("txt_nenhuma_squad_encontrada", null, formState.getLocale()) )
                                .setDescription(messageSource.getMessage("txt_nao_ha_squads_disponiveis_no_momento", null, formState.getLocale()) + ".")
                                .setColor(0xFF0000);
                        hook.editOriginalEmbeds(errorEmbed.build())
                                .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) ))
                                .queue();
                        return;
                    }

                    StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                            .setPlaceholder(messageSource.getMessage("txt_escolha_sua_squad", null, formState.getLocale()) );

                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        String squadName = squad.optString("name", "");
                        String squadId = String.valueOf(squad.get("id"));
                        if (!squadName.isEmpty()) {
                            squadMenuBuilder.addOption(squadName, squadId);
                        }
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("✅ " + messageSource.getMessage("txt_login_realizado_com_sucesso", null, formState.getLocale()) + "!")
                            .setDescription("🏢 " + messageSource.getMessage("txt_selecione_a_squad_para_o_seu_log", null, formState.getLocale()) + ":")
                            .setColor(0x00FF00);

                    logger.info("🎯 PRESTES A ENVIAR MENSAGEM COM MENU DE SQUADS");
                    logger.info("Número de opções no menu: {}", squadMenuBuilder.build().getOptions().size());
                    
                    hook.editOriginalEmbeds(embed.build())
                            .setActionRow(squadMenuBuilder.build())
                            .queue(
                                success -> logger.info("✅ MENSAGEM COM MENU DE SQUADS ENVIADA COM SUCESSO!"),
                                error -> logger.error("❌ ERRO AO ENVIAR MENSAGEM COM MENU DE SQUADS", error)
                            );

                } catch (Exception e) {
                    logger.error("❌ ERRO AO CARREGAR SQUADS após login Google", e);
                    logger.error("Tipo de erro: {}", e.getClass().getName());
                    logger.error("Mensagem: {}", e.getMessage());
                    logger.error("Stack trace:", e);
                    
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle( "✅ "+messageSource.getMessage("txt_autenticado", null, formState.getLocale()) +" | ❌ "
                                    + messageSource.getMessage("txt_erro_carregar_squads", null, formState.getLocale()))
                            .setDescription("**" + messageSource.getMessage("txt_sua_autenticacao_foi_bem_sucedida", null, formState.getLocale()) + "!**\n\n" +
                                    messageSource.getMessage("txt_porem_ocorreu_um_erro_ao_carregar_as_squads_disponiveis", null, formState.getLocale()) + ".\n\n**" +
                                    messageSource.getMessage("txt_detalhes_do_erro", null, formState.getLocale()) + ":**\n" +
                                    "```\n" + e.getMessage() + "\n```\n\n" +
                                    "💡 " + messageSource.getMessage("txt_use_o_comando_squad_log_novamente", null, formState.getLocale()) + ".")
                            .setColor(0xFFA500);
                    
                    hook.editOriginalEmbeds(errorEmbed.build())
                            .setActionRow(Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) ))
                            .queue();
                } finally {
                    DiscordUserContext.clear();
                    logger.info("🧹 Contexto do usuário limpo");
                }

            } catch (Exception e) {
                logger.error("❌ FALHA NA AUTENTICAÇÃO GOOGLE para usuário {}", userId, e);
                logger.error("Tipo de erro: {}", e.getClass().getName());
                logger.error("Mensagem: {}", e.getMessage());
                logger.error("Stack trace completo:", e);

                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ " + messageSource.getMessage("txt_falha_na_autenticacao", null, formState.getLocale()) )
                        .setDescription("**"+messageSource.getMessage("txt_nao_foi_possivel_autenticar_com_codigo_fornecido", null, formState.getLocale()) +
                                ".**\n\n **" + messageSource.getMessage("txt_possiveis_causas", null, formState.getLocale()) +":**\n•" +
                                messageSource.getMessage("txt_codigo_invalido_ou_expirado", null, formState.getLocale()) + "\n•" +
                                messageSource.getMessage("txt_codigo_ja_foi_usado_anteriormente", null, formState.getLocale()) +"\n•" +
                                messageSource.getMessage("txt_erro_de_comunicacao_com_api", null, formState.getLocale()) +"\n\n**" +
                                messageSource.getMessage("txt_detalhes_do_erro", null, formState.getLocale()) +":**\n" +
                                "```\n" + e.getMessage() + "\n```\n\n" +
                                "💡 **" + messageSource.getMessage("txt_tente_novamente", null, formState.getLocale()) + "** " +
                                messageSource.getMessage("txt_clicando_no_botao_abaixo", null, formState.getLocale())+ "." )
                        .setColor(0xFF0000);

                hook.editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(
                                Button.success("btn-autenticar", "🔐 " + messageSource.getMessage("txt_tente_novamente", null, formState.getLocale()) ),
                                Button.primary("voltar-inicio", "🏠 " + messageSource.getMessage("txt_voltar_inicio", null, formState.getLocale()) )
                        )
                        .queue();
            }
        });
    }
}