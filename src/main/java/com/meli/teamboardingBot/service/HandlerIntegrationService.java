package com.meli.teamboardingBot.service;

import com.meli.teamboardingBot.handler.LogSelectionHandler;
import com.meli.teamboardingBot.handler.SummaryHandler;
import com.meli.teamboardingBot.model.FormState;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HandlerIntegrationService {
    
    @Autowired
    private LogSelectionHandler logSelectionHandler;
    
    @Autowired
    private SummaryHandler summaryHandler;
    
    public void showLogSelection(ButtonInteractionEvent event) {
        logSelectionHandler.showLogSelection(event);
    }
    
    public void showLogSelectionWithHook(net.dv8tion.jda.api.interactions.InteractionHook hook) {
        logSelectionHandler.showLogSelectionWithHook(hook);
    }
    
    public void showCreateSummary(ButtonInteractionEvent event, FormState state) {
        summaryHandler.showCreateSummary(event, state);
    }
    
    public void showUpdateSummary(ButtonInteractionEvent event, FormState state) {
        summaryHandler.showUpdateSummary(event, state);
    }
    
    public void showCreateSummary(ModalInteractionEvent event, FormState state) {
        summaryHandler.showCreateSummary(event, state);
    }
    
    public void showUpdateSummary(StringSelectInteractionEvent event, FormState state) {
        summaryHandler.showUpdateSummary(event, state);
    }
    
    public void showSummary(ModalInteractionEvent event, FormState state) {
        summaryHandler.showSummary(event, state);
    }
    
    public void showSummary(StringSelectInteractionEvent event) {
        summaryHandler.showSummary(event);
    }
}
