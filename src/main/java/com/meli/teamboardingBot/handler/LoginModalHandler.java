package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.context.DiscordUserContext;
import com.meli.teamboardingBot.enums.FormStep;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.DiscordUserAuthenticationService;
import com.meli.teamboardingBot.service.FormStateService;
import com.meli.teamboardingBot.service.GoogleAuthIntegrationService;
import com.meli.teamboardingBot.service.SquadLogService;
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
import org.springframework.stereotype.Component;


@Component
public class LoginModalHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LoginModalHandler.class);
    private final DiscordUserAuthenticationService authService;
    private final FormStateService formStateService;
    private final SquadLogService squadLogService;
    private final GoogleAuthIntegrationService googleAuthIntegration;

    public LoginModalHandler(DiscordUserAuthenticationService authService,
                             FormStateService formStateService,
                             SquadLogService squadLogService,
                             GoogleAuthIntegrationService googleAuthIntegration) {
        this.authService = authService;
        this.formStateService = formStateService;
        this.squadLogService = squadLogService;
        this.googleAuthIntegration = googleAuthIntegration;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        try {
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

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔐 Escolha a forma de autenticação")
                .setDescription("Selecione como deseja fazer login no sistema:")
                .addField("📝 Manual", "Digite suas credenciais (e-mail e senha)", false)
                .addField("🌐 Google", "Autentique-se usando sua conta Google", false)
                .setColor(0x5865F2);

        event.editMessageEmbeds(embed.build())
                .setActionRow(
                        Button.primary("btn-auth-manual", "📝 Manual"),
                        Button.success("btn-auth-google", "🌐 Google"),
                        Button.secondary("voltar-inicio", "🏠 Voltar")
                )
                .queue();
    }


    private void handleManualAuthButton(ButtonInteractionEvent event) {
        logger.info("Autenticação manual selecionada pelo usuário: {}", event.getUser().getId());

        TextInput username = TextInput.create("username", "E-mail", TextInputStyle.SHORT)
                .setPlaceholder("Digite seu e-mail")
                .setRequired(true)
                .setMinLength(5)
                .setMaxLength(100)
                .build();

        TextInput password = TextInput.create("password", "Senha", TextInputStyle.SHORT)
                .setPlaceholder("Digite sua senha")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(100)
                .build();

        Modal modal = Modal.create("login-modal", "🔐 Login - Squad Log")
                .addActionRow(username)
                .addActionRow(password)
                .build();

        event.replyModal(modal).queue();
    }

    private void handleGoogleAuthButton(ButtonInteractionEvent event) {
        logger.info("Autenticação Google selecionada pelo usuário: {}", event.getUser().getId());

        String userId = event.getUser().getId();

        try {
            // Obter URL da API
            String authUrl = googleAuthIntegration.getGoogleLoginConnectionUrl();

            logger.info("URL de autenticação Google obtida da API: {}", authUrl);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🌐 Autenticação Google")
                    .setDescription("**Passo 1:** Clique no link abaixo para fazer login com sua conta Google:\n\n" +
                            "🔗 [**Clique aqui para autenticar**](" + authUrl + ")\n\n" +
                            "**Passo 2:** Após fazer login, você será redirecionado para uma página.\n" +
                            "Copie o **código de autorização** da URL e clique no botão abaixo para inseri-lo.\n\n" +
                            "⚠️ **Importante:**\n" +
                            "• O código está na URL após `?code=` ou `&code=`\n" +
                            "• Copie todo o código (pode ser longo)\n" +
                            "• Cole no formulário que aparecerá")
                    .setColor(0x4285F4)
                    .setFooter("Discord User ID: " + userId);

            event.editMessageEmbeds(embed.build())
                    .setActionRow(
                            Button.link(authUrl, "🌐 1️⃣ Autenticar com Google"),
                            Button.primary("btn-submit-google-code", "2️⃣ Inserir Código"),
                            Button.secondary("voltar-inicio", "🏠 Cancelar")
                    )
                    .queue();

        } catch (Exception e) {
            logger.error("Erro ao obter URL de autenticação Google", e);

            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setTitle("❌ Erro")
                    .setDescription("Não foi possível obter a URL de autenticação. Tente novamente.")
                    .setColor(0xFF0000);

            event.editMessageEmbeds(errorEmbed.build())
                    .setActionRow(Button.secondary("voltar-inicio", "🏠 Voltar ao Início"))
                    .queue();
        }
    }

    private void handleGoogleCodeSubmission(ButtonInteractionEvent event) {
        logger.info("Botão inserir código Google clicado pelo usuário: {}", event.getUser().getId());

        TextInput codeInput = TextInput.create("google-code", "Código de Autorização", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Cole aqui o código obtido após autenticação")
                .setMinLength(10)
                .setMaxLength(2000)
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal-google-code", "🔑 Código de Autorização Google")
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
                                .setTitle("❌ Nenhuma squad encontrada")
                                .setDescription("Não há squads disponíveis no momento.")
                                .setColor(0xFF0000);
                        hook.editOriginalEmbeds(errorEmbed.build())
                                .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                                .queue();
                        return;
                    }

                    StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-select")
                            .setPlaceholder("Selecione uma squad");
                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        String squadName = squad.optString("name", "");
                        String squadId = String.valueOf(squad.get("id"));
                        if (!squadName.isEmpty()) {
                            squadMenuBuilder.addOption(squadName, squadId);
                        }
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("✅ Login realizado com sucesso!")
                            .setDescription("🏢 Selecione a squad para o seu log:")
                            .setColor(0x00FF00);

                    hook.editOriginalEmbeds(embed.build())
                            .setActionRow(squadMenuBuilder.build())
                            .queue();

                } catch (Exception e) {
                    logger.error("Erro ao carregar squads após login: {}", e.getMessage());
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Erro ao carregar squads")
                            .setDescription("Login realizado, mas ocorreu um erro ao carregar as squads.\n\n" +
                                    "Use o comando `/squad-log` novamente.")
                            .setColor(0xFF0000);
                    hook.editOriginalEmbeds(errorEmbed.build())
                            .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                            .queue();
                } finally {
                    DiscordUserContext.clear();
                }
            } else {
                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Falha na Autenticação")
                        .setDescription(response.getMessage() + "\n\nTente novamente.")
                        .setColor(0xFF0000);
                hook.editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(
                                Button.success("btn-autenticar", "🔐 Tentar Novamente"),
                                Button.primary("voltar-inicio", "🏠 Voltar ao Início")
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
                // Trocar code por token
                String accessToken = googleAuthIntegration.exchangeCodeForToken(code);

                // Armazenar token no contexto do usuário
                authService.authenticateUserWithToken(userId, accessToken);

                logger.info("✅ Usuário {} autenticado via Google com sucesso!", userId);

                // Carregar squads
                try {
                    String squadsJson = squadLogService.getSquads();
                    JSONArray squadsArray = new JSONArray(squadsJson);

                    StringSelectMenu.Builder squadMenuBuilder = StringSelectMenu.create("squad-selection")
                            .setPlaceholder("Escolha sua squad");

                    for (int i = 0; i < squadsArray.length(); i++) {
                        JSONObject squad = squadsArray.getJSONObject(i);
                        String squadId = squad.getString("id");
                        String squadName = squad.getString("name");
                        squadMenuBuilder.addOption(squadName, squadId);
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("✅ Login realizado com sucesso!")
                            .setDescription("🏢 Selecione a squad para o seu log:")
                            .setColor(0x00FF00);

                    hook.editOriginalEmbeds(embed.build())
                            .setActionRow(squadMenuBuilder.build())
                            .queue();

                } catch (Exception e) {
                    logger.error("Erro ao carregar squads após login Google: {}", e.getMessage());
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Erro ao carregar squads")
                            .setDescription("Login realizado, mas ocorreu um erro ao carregar as squads.\n\n" +
                                    "Use o comando `/squad-log` novamente.")
                            .setColor(0xFF0000);
                    hook.editOriginalEmbeds(errorEmbed.build())
                            .setActionRow(Button.primary("voltar-inicio", "🏠 Voltar ao Início"))
                            .queue();
                }

            } catch (Exception e) {
                logger.error("❌ Erro ao processar código Google para usuário {}: {}", userId, e.getMessage());

                EmbedBuilder errorEmbed = new EmbedBuilder()
                        .setTitle("❌ Erro na autenticação")
                        .setDescription("Não foi possível autenticar com o código fornecido.\n\n" +
                                "**Possíveis causas:**\n" +
                                "• Código inválido ou expirado\n" +
                                "• Código já foi usado\n" +
                                "• Erro de comunicação com a API\n\n" +
                                "**Erro:** " + e.getMessage())
                        .setColor(0xFF0000);

                hook.editOriginalEmbeds(errorEmbed.build())
                        .setActionRow(
                                Button.success("btn-autenticar", "🔐 Tentar Novamente"),
                                Button.primary("voltar-inicio", "🏠 Voltar ao Início")
                        )
                        .queue();
            }
        });
    }
}