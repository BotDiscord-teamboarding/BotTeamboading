package com.meli.teamboardingBot.adapters.handler;

import com.meli.teamboardingBot.adapters.out.language.LanguageInterceptorService;
import com.meli.teamboardingBot.adapters.out.language.UserLanguageService;
import com.meli.teamboardingBot.adapters.out.session.ActiveFlowMessageService;
import com.meli.teamboardingBot.core.domain.FormState;
import com.meli.teamboardingBot.core.ports.auth.GetIsUserAuthenticatedPort;
import com.meli.teamboardingBot.core.ports.formstate.GetOrCreateFormStatePort;
import com.meli.teamboardingBot.core.usecase.auth.oath.UserTokenManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;

@Component
public class LanguageSelectionHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LanguageSelectionHandler.class);
    
    private final UserLanguageService languageService;
    private final MessageSource messageSource;
    private final GetOrCreateFormStatePort getOrCreateFormStatePort;
    private final LanguageInterceptorService languageInterceptor;
    private final GetIsUserAuthenticatedPort isUserAuthenticated;
    private final ActiveFlowMessageService activeFlowMessageService;

    @Autowired
    public LanguageSelectionHandler(UserLanguageService languageService, 
                                   MessageSource messageSource, 
                                   GetOrCreateFormStatePort getOrCreateFormStatePort,
                                   LanguageInterceptorService languageInterceptor,
                                   GetIsUserAuthenticatedPort isUserAuthenticated,
                                   ActiveFlowMessageService activeFlowMessageService) {
        this.languageService = languageService;
        this.messageSource = messageSource;
        this.getOrCreateFormStatePort = getOrCreateFormStatePort;
        this.languageInterceptor = languageInterceptor;
        this.isUserAuthenticated = isUserAuthenticated;
        this.activeFlowMessageService = activeFlowMessageService;
    }
    
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        
        if (buttonId.startsWith("confirm-language-")) {
            handleConfirmLanguage(event, buttonId);
        } else if (buttonId.startsWith("change-language-")) {
            handleChangeLanguage(event, buttonId);
        } else if (buttonId.equals("continue-to-auth")) {
            handleContinueToAuth(event);
        } else if (buttonId.equals("execute-pending-command")) {
            handleExecutePendingCommand(event);
        } else if (buttonId.startsWith("set-default-language-")) {
            handleSetDefaultLanguage(event, buttonId);
        } else if (buttonId.equals("skip-default-language")) {
            handleSkipDefaultLanguage(event);
        }
    }
    
    private void handleConfirmLanguage(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        String localeTag = buttonId.replace("confirm-language-", "");
        Locale selectedLocale = Locale.forLanguageTag(localeTag);
        
        logger.info("User {} confirmed language: {}", userId, selectedLocale);
        
        languageService.saveUserLanguagePreference(userId, selectedLocale);
        
        FormState formState = getOrCreateFormStatePort.getOrCreateState(event.getUser().getIdLong());
        formState.setLocale(selectedLocale);
        logger.info("FormState locale updated to: {}", selectedLocale);
        
        handleExecutePendingCommand(event);
    }
    
    private void handleChangeLanguage(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        String localeTag = buttonId.replace("change-language-", "");
        Locale newLocale = Locale.forLanguageTag(localeTag);
        
        logger.info("User {} changed language to: {}", userId, newLocale);
        
        languageService.saveUserLanguagePreference(userId, newLocale);
        
        FormState formState = getOrCreateFormStatePort.getOrCreateState(event.getUser().getIdLong());
        formState.setLocale(newLocale);
        logger.info("FormState locale updated to: {}", newLocale);
        
        LanguageInterceptorService.PendingCommand pendingCommand = languageInterceptor.getPendingCommand(userId);
        
        if (pendingCommand == null || !pendingCommand.commandName.equals("language")) {
            String languageName = languageService.getLanguageName(newLocale);
            String title = messageSource.getMessage("txt_idioma_alterado_titulo", null, newLocale);
            String description = MessageFormat.format(
                messageSource.getMessage("txt_idioma_alterado_descricao", null, newLocale),
                languageName
            );
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔄 " + title)
                .setDescription(description)
                .setColor(0x5865F2);
            
            String continueButtonText = messageSource.getMessage("txt_continuar", null, newLocale);
            
            event.editMessageEmbeds(embed.build())
                .setActionRow(
                    Button.primary("continue-to-auth", "▶️ " + continueButtonText)
                )
                .queue();
        } else {
            showDefaultLanguageConfirmation(event, newLocale);
        }
    }
    
    private void showDefaultLanguageConfirmation(ButtonInteractionEvent event, Locale newLocale) {
        String languageName = languageService.getLanguageName(newLocale);
        String title = messageSource.getMessage("txt_idioma_alterado_titulo", null, newLocale);
        String yesText = newLocale.equals(languageService.getPortugueseLocale()) ? "Sim, tornar padrão" : "Sí, hacer predeterminado";
        String noText = newLocale.equals(languageService.getPortugueseLocale()) ? "Não, apenas esta sessão" : "No, solo esta sesión";
        
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_alterado_descricao", null, newLocale),
            languageName
        ) + "\n\n" + (newLocale.equals(languageService.getPortugueseLocale()) 
            ? "Deseja tornar este idioma como padrão permanente?" 
            : "¿Deseas establecer este idioma como predeterminado permanente?");
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🔄 " + title)
            .setDescription(description)
            .setColor(0x5865F2);
        
        event.editMessageEmbeds(embed.build())
            .setActionRow(
                Button.success("set-default-language-" + newLocale.toLanguageTag(), "✅ " + yesText),
                Button.secondary("skip-default-language", "⏭️ " + noText)
            )
            .queue();
    }
    
    private void handleSetDefaultLanguage(ButtonInteractionEvent event, String buttonId) {
        String userId = event.getUser().getId();
        String localeTag = buttonId.replace("set-default-language-", "");
        Locale locale = Locale.forLanguageTag(localeTag);
        
        logger.info("User {} set default language to: {}", userId, locale);
        languageService.saveUserLanguagePreference(userId, locale);
        
        String languageName = languageService.getLanguageName(locale);
        String title = locale.equals(languageService.getPortugueseLocale()) 
            ? "Idioma Padrão Configurado" 
            : "Idioma Predeterminado Configurado";
        String description = MessageFormat.format(
            locale.equals(languageService.getPortugueseLocale())
                ? "O idioma **{0}** foi configurado como padrão!\n\nEste será seu idioma em todas as sessões."
                : "El idioma **{0}** fue configurado como predeterminado!\n\nEste será tu idioma en todas las sesiones.",
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("✅ " + title)
            .setDescription(description)
            .setColor(0x00FF00);
        
        event.editMessageEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    
    private void handleSkipDefaultLanguage(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        Locale userLocale = languageService.getUserLanguagePreference(userId);
        
        logger.info("User {} skipped setting default language", userId);
        
        String languageName = languageService.getLanguageName(userLocale);
        String title = messageSource.getMessage("txt_idioma_alterado_titulo", null, userLocale);
        String description = MessageFormat.format(
            messageSource.getMessage("txt_idioma_alterado_descricao", null, userLocale),
            languageName
        );
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🔄 " + title)
            .setDescription(description)
            .setColor(0x5865F2);
        
        event.editMessageEmbeds(embed.build())
            .setComponents()
            .queue();
    }
    
    private void handleContinueToAuth(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        Locale userLocale = languageService.getUserLanguagePreference(userId);
        
        if (userLocale == null) {
            logger.warn("User {} has no language preference, using Spanish as default", userId);
            userLocale = languageService.getSpanishLocale();
            languageService.saveUserLanguagePreference(userId, userLocale);
        }
        
        LanguageInterceptorService.PendingCommand pendingCommand = languageInterceptor.getPendingCommand(userId);
        logger.info("User {} completed language selection, pending command: {}", userId, 
            pendingCommand != null ? pendingCommand.commandName : "none");
        
        if (pendingCommand != null) {
            String languageName = languageService.getLanguageName(userLocale);
            String title = messageSource.getMessage("txt_idioma_confirmado_titulo", null, userLocale);
            
            String description = MessageFormat.format(
                messageSource.getMessage("txt_idioma_confirmado_descricao", null, userLocale),
                languageName
            );
            
            String continueButtonText = messageSource.getMessage("txt_continuar", null, userLocale);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + title)
                .setDescription(description)
                .setColor(0x00FF00);
            
            event.editMessageEmbeds(embed.build())
                .setComponents(
                    net.dv8tion.jda.api.interactions.components.ActionRow.of(
                        Button.success("execute-pending-command", "▶️ " + continueButtonText)
                    )
                )
                .queue();
        } else {
            String languageName = languageService.getLanguageName(userLocale);
            String title = messageSource.getMessage("txt_idioma_alterado_sucesso", null, userLocale);
            String description = MessageFormat.format(
                messageSource.getMessage("txt_idioma_alterado_sucesso_descricao", null, userLocale),
                languageName
            );
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ " + title)
                .setDescription(description)
                .setColor(0x00FF00);
            
            event.editMessageEmbeds(embed.build())
                .setComponents()
                .queue(success -> {
                    event.getHook().deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS);
                });
        }
    }
    
    private void handleExecutePendingCommand(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        Locale userLocale = languageService.getUserLanguagePreference(userId);
        
        if (userLocale == null) {
            logger.warn("User {} has no language preference during execute", userId);
            userLocale = languageService.getSpanishLocale();
        }
        
        final Locale finalUserLocale = userLocale;
        
        LanguageInterceptorService.PendingCommand pendingCommand = languageInterceptor.getPendingCommand(userId);
        
        if (pendingCommand == null) {
            logger.warn("No pending command found for user {}", userId);
            return;
        }
        
        final LanguageInterceptorService.PendingCommand finalPendingCommand = pendingCommand;
        
        boolean requiresAuth = finalPendingCommand.commandName.equals("squad-log") || 
                              finalPendingCommand.commandName.equals("squad-log-lote");
        
        if (requiresAuth && !isUserAuthenticated.isUserAuthenticated(userId)) {
            logger.warn("User {} not authenticated, showing login screen", userId);
            languageInterceptor.clearPendingCommand(userId);
            event.deferEdit().queue(hook -> showLoginRequiredScreen(hook, finalUserLocale));
            return;
        }
        
        languageInterceptor.clearPendingCommand(userId);
        
        logger.info("Executing pending command for user {}: {}", userId, pendingCommand.commandName);
        
        event.deferEdit().queue(hook -> {
            if (finalPendingCommand.commandName.equals("squad-log")) {
                executeSquadLogCommandWithHook(hook, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("squad-log-lote")) {
                executeSquadLogLoteCommandWithHook(hook, finalPendingCommand, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("start")) {
                executeStartCommandWithHook(hook, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("stop")) {
                executeStopCommandWithHook(hook, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("status")) {
                executeStatusCommandWithHook(hook, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("help")) {
                executeHelpCommandWithHook(hook, finalUserLocale);
            } else if (finalPendingCommand.commandName.equals("language")) {
                executeLanguageCommandWithHook(hook, finalUserLocale);
            } else {
                logger.warn("Unknown command to execute: {}", finalPendingCommand.commandName);
            }
        });
    }
    
    private void executeSquadLogCommandWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Locale userLocale) {
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("📋 Squad Log")
                .setDescription(messageSource.getMessage("txt_escolha_uma_opcao", null, userLocale))
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("criar", "✅ " + messageSource.getMessage("txt_criar", null, userLocale)),
            Button.secondary("atualizar", "📝 " + messageSource.getMessage("txt_atualizar", null, userLocale)),
            Button.danger("status-close", "❌ " + messageSource.getMessage("txt_cancelar", null, userLocale))
        ).queue();
    }
    
    private void executeSquadLogLoteCommandWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, LanguageInterceptorService.PendingCommand pendingCommand, Locale userLocale) {
        String title = userLocale.equals(languageService.getPortugueseLocale())
            ? "Criar Squad Logs em Lote"
            : "Crear Squad Logs en Lote";
        String description = userLocale.equals(languageService.getPortugueseLocale())
            ? "Crie vários logs de uma só vez através de escrita livre.\n\n" +
              "**Formato:** Squad - Pessoa - Tipo - Categorias - Data Início a Data Fim\n" +
              "**Exemplo:** Prueba Nati - Belen - Issue - Tecnologia - 10-12-2025 a 15-12-2025"
            : "Cree varios logs a la vez mediante escritura libre.\n\n" +
              "**Formato:** Squad - Persona - Tipo - Categorías - Fecha Inicio a Fecha Fin\n" +
              "**Ejemplo:** Prueba Nati - Belen - Issue - Tecnología - 10-12-2025 a 15-12-2025";
        String startButtonText = userLocale.equals(languageService.getPortugueseLocale())
            ? "Iniciar Logs em Lote"
            : "Iniciar Logs en Lote";
        String exitButtonText = messageSource.getMessage("txt_sair", null, userLocale);
            
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("📦 " + title)
                .setDescription(description)
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("open-batch-modal", "📝 " + startButtonText),
            Button.danger("status-close", "🚪 " + exitButtonText)
        ).queue();
    }
    
    private void executeStartCommandByEdit(ButtonInteractionEvent event, Locale userLocale) {
        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("🚀 " + messageSource.getMessage("txt_bem_vindo_ao_squad_log_bot", null, userLocale))
                .setDescription(messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, userLocale))
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, userLocale)),
            Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, userLocale)),
            Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, userLocale))
        ).queue();
    }
    
    private void executeStopCommandByEdit(ButtonInteractionEvent event, Locale userLocale) {
        String title = messageSource.getMessage("txt_nenhum_fluxo_ativo", null, userLocale);
        String description = messageSource.getMessage("txt_vc_n_esta_em_nenhum_processo_de_criacao_ou_edicao_no_momento", null, userLocale) + 
                           ".\n\n" + messageSource.getMessage("txt_use_squad_log_para_iniciar_um_novo_fluxo", null, userLocale);
        
        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .setColor(0x3498db)
                .build()
        ).setComponents().queue(hook -> {
            hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS);
        });
    }
    
    private void executeStatusCommandByEdit(ButtonInteractionEvent event, Locale userLocale) {
        String message = userLocale.equals(languageService.getPortugueseLocale())
            ? "Use o comando `/status` para verificar seu status de autenticação."
            : "Use el comando `/status` para verificar su estado de autenticación.";
            
        event.editMessage(message).setComponents().queue();
    }
    
    private void executeHelpCommandByEdit(ButtonInteractionEvent event, Locale userLocale) {
        String message = userLocale.equals(languageService.getPortugueseLocale())
            ? "Use o comando `/help` para ver a lista de comandos disponíveis."
            : "Use el comando `/help` para ver la lista de comandos disponibles.";
            
        event.editMessage(message).setComponents().queue();
    }
    
    private void executeLanguageCommandByEdit(ButtonInteractionEvent event, Locale userLocale) {
        String languageName = languageService.getLanguageName(userLocale);
        Locale alternativeLocale = languageService.getAlternativeLocale(userLocale);
        String alternativeLanguageName = languageService.getLanguageName(alternativeLocale);
        
        String title = messageSource.getMessage("txt_configuracao_idioma_titulo", null, userLocale);
        String description = java.text.MessageFormat.format(
            messageSource.getMessage("txt_configuracao_idioma_descricao", null, userLocale),
            languageName
        );
        
        String changeButtonText = java.text.MessageFormat.format(
            messageSource.getMessage("txt_mudar_para", null, userLocale),
            alternativeLanguageName
        );
        
        event.editMessageEmbeds(
            new EmbedBuilder()
                .setTitle("🌍 " + title)
                .setDescription(description)
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("change-language-" + alternativeLocale.toLanguageTag(), "🔄 " + changeButtonText)
        ).queue();
    }
    
    private void executeStartCommandWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Locale userLocale) {
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("🚀 " + messageSource.getMessage("txt_bem_vindo_ao_squad_log_bot", null, userLocale))
                .setDescription(messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, userLocale))
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, userLocale)),
            Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, userLocale)),
            Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, userLocale))
        ).queue();
    }
    
    private void executeStopCommandWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Locale userLocale) {
        String title = messageSource.getMessage("txt_nenhum_fluxo_ativo", null, userLocale);
        String description = messageSource.getMessage("txt_vc_n_esta_em_nenhum_processo_de_criacao_ou_edicao_no_momento", null, userLocale) + 
                           ".\n\n" + messageSource.getMessage("txt_use_squad_log_para_iniciar_um_novo_fluxo", null, userLocale);
        
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .setColor(0x3498db)
                .build()
        ).setComponents().queue(success -> {
            hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS);
        });
    }
    
    private void executeStatusCommandWithHook(InteractionHook hook, Locale userLocale) {
        Long userId = hook.getInteraction().getUser().getIdLong();
        String userIdStr = String.valueOf(userId);
        
        activeFlowMessageService.registerFlowHook(userId, hook);
        
        if (!isUserAuthenticated.isUserAuthenticated(userIdStr)) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageSource.getMessage("status.unauthenticated.title", null, userLocale))
                .setDescription(messageSource.getMessage("status.unauthenticated.description", null, userLocale))
                .setColor(0xFF0000);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(
                    Button.primary("start-auth", "🔐 " + messageSource.getMessage("status.button.login", null, userLocale)),
                    Button.secondary("status-close", "🚪 " + messageSource.getMessage("status.button.close", null, userLocale))
                )
                .queue();
        } else {
            String authMethod = UserTokenManager.getAuthMethod(userIdStr);
            String authMethodText = "manual".equals(authMethod) ?
                messageSource.getMessage("status.auth.method.manual", null, userLocale) :
                messageSource.getMessage("status.auth.method.google", null, userLocale);

            String userName = hook.getInteraction().getUser().getName();
            String description = messageSource.getMessage("status.authenticated.description",
                new Object[]{userName, authMethodText}, userLocale);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(messageSource.getMessage("status.authenticated.title", null, userLocale))
                .setDescription(description)
                .setColor(0x00FF00);

            hook.editOriginalEmbeds(embed.build())
                .setActionRow(
                    Button.secondary("status-close", "🚪 " + messageSource.getMessage("status.button.close", null, userLocale)),
                    Button.danger("status-logout", "🔓 " + messageSource.getMessage("status.button.logout", null, userLocale))
                )
                .queue();
        }
    }
    
    private void executeHelpCommandWithHook(InteractionHook hook, Locale userLocale) {
        Long userId = hook.getInteraction().getUser().getIdLong();
        activeFlowMessageService.registerFlowHook(userId, hook);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📚 " + messageSource.getMessage("txt_help_titulo", null, userLocale))
            .setDescription(messageSource.getMessage("txt_help_descricao", null, userLocale))
            .setColor(0x00AE86)
            .addField("📋 `/squad-log`", messageSource.getMessage("txt_help_squad_log_descricao", null, userLocale), false)
            .addField("📦 `/squad-log-lote`", messageSource.getMessage("txt_help_squad_log_lote_descricao", null, userLocale), false)
            .addField("🚀 `/start`", messageSource.getMessage("txt_help_start_descricao", null, userLocale), false)
            .addField("📊 `/status`", messageSource.getMessage("txt_help_status_descricao", null, userLocale), false)
            .addField("🛑 `/stop`", messageSource.getMessage("txt_help_stop_descricao", null, userLocale), false)
            .addField("🌐 `/language`", messageSource.getMessage("txt_help_language_descricao", null, userLocale), false)
            .addField("❓ `/help`", messageSource.getMessage("txt_help_help_descricao", null, userLocale), false)
            .setFooter(messageSource.getMessage("txt_help_footer", null, userLocale), null);

        hook.editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.danger("help-close", "🚪 " + messageSource.getMessage("txt_sair", null, userLocale))
            )
            .queue();
    }
    
    private void executeLanguageCommandWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook, Locale userLocale) {
        String languageName = languageService.getLanguageName(userLocale);
        Locale alternativeLocale = languageService.getAlternativeLocale(userLocale);
        String alternativeLanguageName = languageService.getLanguageName(alternativeLocale);
        
        String title = messageSource.getMessage("txt_configuracao_idioma_titulo", null, userLocale);
        String description = java.text.MessageFormat.format(
            messageSource.getMessage("txt_configuracao_idioma_descricao", null, userLocale),
            languageName
        );
        
        String changeButtonText = java.text.MessageFormat.format(
            messageSource.getMessage("txt_mudar_para", null, userLocale),
            alternativeLanguageName
        );
        
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("🌍 " + title)
                .setDescription(description)
                .setColor(0x5865F2)
                .build()
        ).setActionRow(
            Button.primary("change-language-" + alternativeLocale.toLanguageTag(), "🔄 " + changeButtonText)
        ).queue();
    }
    
    private void showLoginRequiredScreen(net.dv8tion.jda.api.interactions.InteractionHook hook, Locale userLocale) {
        String title = messageSource.getMessage("txt_autenticacao_necessaria", null, userLocale);
        String description = messageSource.getMessage("txt_faca_login_para_usar_os_comandos", null, userLocale) + 
                           "\n\n" + messageSource.getMessage("txt_escolha_o_metodo_de_autenticacao", null, userLocale);
        
        hook.editOriginalEmbeds(
            new EmbedBuilder()
                .setTitle("🔒 " + title)
                .setDescription(description)
                .setColor(0xFF6B6B)
                .build()
        ).setActionRow(
            Button.primary("auth-manual", "🔐 " + messageSource.getMessage("txt_manual", null, userLocale)),
            Button.success("auth-google", "🌐 " + messageSource.getMessage("txt_google", null, userLocale)),
            Button.danger("status-close", "🚪 " + messageSource.getMessage("txt_sair", null, userLocale))
        ).queue();
    }
}
