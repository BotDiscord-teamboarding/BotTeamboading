package com.meli.teamboardingBot.filter;

import com.meli.teamboardingBot.shared.context.DiscordUserContext;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class UserContextFilter extends ListenerAdapter {

    @Override
    public void onGenericInteractionCreate(@NotNull GenericInteractionCreateEvent event) {
        try {
            String userId = event.getUser().getId();
            DiscordUserContext.setCurrentUserId(userId);
        } catch (Exception e) {
            DiscordUserContext.clear();
            throw e;
        }
    }
}
