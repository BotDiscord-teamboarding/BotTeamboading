package com.meli.teamboardingBot.handler;

import com.meli.teamboardingBot.model.FormState;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface InteractionHandler {
    
    boolean canHandle(String componentId);
    
    void handleButton(ButtonInteractionEvent event, FormState state);
    
    void handleStringSelect(StringSelectInteractionEvent event, FormState state);
    
    void handleModal(ModalInteractionEvent event, FormState state);
    
    int getPriority();
}
