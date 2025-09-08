package com.meli.teamboardingBot.factory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import java.util.List;
import java.util.Map;
public interface DiscordComponentFactory {
    EmbedBuilder createBasicEmbed(String title, String description, int color);
    EmbedBuilder createSuccessEmbed(String title, String description);
    EmbedBuilder createErrorEmbed(String title, String description);
    EmbedBuilder createInfoEmbed(String title, String description);
    EmbedBuilder createWarningEmbed(String title, String description);
    Button createPrimaryButton(String id, String label);
    Button createSecondaryButton(String id, String label);
    Button createSuccessButton(String id, String label);
    Button createDangerButton(String id, String label);
    StringSelectMenu.Builder createSelectMenu(String id, String placeholder);
    StringSelectMenu createSelectMenuWithOptions(String id, String placeholder, Map<String, String> options);
    Modal.Builder createModal(String id, String title);
    Modal createFormModal(String id, String title, List<String> fieldIds, List<String> fieldLabels, List<String> fieldPlaceholders);
}
