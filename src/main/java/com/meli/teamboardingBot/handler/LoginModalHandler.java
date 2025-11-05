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
    private final GoogleAuthIntegrationService googleAuthService;

    public LoginModalHandler(DiscordUserAuthenticationService authService,
                             FormStateService formStateService,
                             SquadLogService squadLogService,
                             GoogleAuthIntegrationService googleAuthService) {
        this.authService = authService;
        this.formStateService = formStateService;
        this.squadLogService = squadLogService;
        this.googleAuthService = googleAuthService;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

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

        String googleAuthUrl = googleAuthService.getGoogleAuthUrl();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌐 Autenticação Google")
                .setDescription("Clique no botão abaixo para fazer login com sua conta Google:\n\n" +
                        "⚠️ **Importante:**\n" +
                        "• O link abrirá em seu navegador\n" +
                        "• Faça login com sua conta Google\n" +
                        "• Após o login, você será redirecionado automaticamente\n" +
                        "• Aguarde a confirmação no Discord")
                .setColor(0x4285F4);

        event.editMessageEmbeds(embed.build())
                .setActionRow(
                        Button.link(googleAuthUrl, "🌐 Autenticar com Google"),
                        Button.secondary("voltar-inicio", "🏠 Cancelar")
                )
                .queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
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
}