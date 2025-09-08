package com.meli.teamboardingBot.factory;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.Map;

@Component
public class DefaultDiscordComponentFactory implements DiscordComponentFactory {
    
    private static final int SUCCESS_COLOR = 0x00FF00;
    private static final int ERROR_COLOR = 0xFF0000;
    private static final int INFO_COLOR = 0x0099FF;
    private static final int WARNING_COLOR = 0xFFAA00;
    
    @Override
    public EmbedBuilder createBasicEmbed(String title, String description, int color) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color);
    }
    
    @Override
    public EmbedBuilder createSuccessEmbed(String title, String description) {
        return createBasicEmbed(title, description, SUCCESS_COLOR);
    }
    
    @Override
    public EmbedBuilder createErrorEmbed(String title, String description) {
        return createBasicEmbed(title, description, ERROR_COLOR);
    }
    
    @Override
    public EmbedBuilder createInfoEmbed(String title, String description) {
        return createBasicEmbed(title, description, INFO_COLOR);
    }
    
    @Override
    public EmbedBuilder createWarningEmbed(String title, String description) {
        return createBasicEmbed(title, description, WARNING_COLOR);
    }
    
    @Override
    public Button createPrimaryButton(String id, String label) {
        return Button.primary(id, label);
    }
    
    @Override
    public Button createSecondaryButton(String id, String label) {
        return Button.secondary(id, label);
    }
    
    @Override
    public Button createSuccessButton(String id, String label) {
        return Button.success(id, label);
    }
    
    @Override
    public Button createDangerButton(String id, String label) {
        return Button.danger(id, label);
    }
    
    @Override
    public StringSelectMenu.Builder createSelectMenu(String id, String placeholder) {
        return StringSelectMenu.create(id)
            .setPlaceholder(placeholder)
            .setRequiredRange(1, 1);
    }
    
    @Override
    public StringSelectMenu createSelectMenuWithOptions(String id, String placeholder, Map<String, String> options) {
        StringSelectMenu.Builder builder = createSelectMenu(id, placeholder);
        
        for (Map.Entry<String, String> option : options.entrySet()) {
            builder.addOption(option.getValue(), option.getKey());
        }
        
        return builder.build();
    }
    
    @Override
    public Modal.Builder createModal(String id, String title) {
        return Modal.create(id, title);
    }
    
    @Override
    public Modal createFormModal(String id, String title, List<String> fieldIds, List<String> fieldLabels, List<String> fieldPlaceholders) {
        Modal.Builder builder = createModal(id, title);
        
        for (int i = 0; i < fieldIds.size() && i < fieldLabels.size(); i++) {
            String fieldId = fieldIds.get(i);
            String fieldLabel = fieldLabels.get(i);
            String placeholder = (i < fieldPlaceholders.size()) ? fieldPlaceholders.get(i) : "";
            
            TextInput textInput = TextInput.create(fieldId, fieldLabel, TextInputStyle.SHORT)
                .setPlaceholder(placeholder)
                .setRequired(true)
                .build();
            
            builder.addActionRow(textInput);
        }
        
        return builder.build();
    }
}
