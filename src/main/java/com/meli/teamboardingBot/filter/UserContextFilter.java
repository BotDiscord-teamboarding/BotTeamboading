package com.meli.teamboardingBot.filter;

import com.meli.teamboardingBot.context.DiscordUserContext;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Filtro para gerenciar o contexto do usuário durante as interações do Discord.
 * Garante que o ID do usuário esteja disponível durante todo o processamento da requisição.
 */
@Component
@Order(1) // Alta prioridade para garantir que seja executado primeiro
public class UserContextFilter extends ListenerAdapter {

    @Override
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
        try {
            // Define o ID do usuário atual no contexto
            String userId = event.getUser().getId();
            DiscordUserContext.setCurrentUserId(userId);
            
            // A execução continua para os próximos listeners
        } catch (Exception e) {
            // Em caso de erro, limpa o contexto para evitar vazamento
            DiscordUserContext.clear();
            throw e;
        }
    }
}
