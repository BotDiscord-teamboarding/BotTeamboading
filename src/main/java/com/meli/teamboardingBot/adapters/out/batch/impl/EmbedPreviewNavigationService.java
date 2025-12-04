package com.meli.teamboardingBot.adapters.out.batch.impl;

import com.meli.teamboardingBot.adapters.out.batch.PreviewNavigator;
import com.meli.teamboardingBot.core.domain.batch.BatchLogEntry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Service
public class EmbedPreviewNavigationService implements PreviewNavigator {
    
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public MessageEmbed createPreviewEmbed(BatchLogEntry entry, int currentIndex, int totalCount) {
        return createPreviewEmbed(entry, currentIndex, totalCount, null);
    }
    
    public MessageEmbed createPreviewEmbed(BatchLogEntry entry, int currentIndex, int totalCount, String modifiedField) {
        if (modifiedField != null) {
            entry.addModifiedField(modifiedField);
        }
        
        EmbedBuilder embed = new EmbedBuilder();
        
        boolean hasModifications = entry.hasModifications();
        String title = "📋 Preview do Squad Log";
        if (hasModifications) {
            title += " ✅";
        }
        
        embed.setTitle(title)
              .setColor(hasModifications ? Color.GREEN : Color.BLUE)
              .setDescription(String.format("**Log %d de %d**", currentIndex + 1, totalCount));

        embed.addField("🏢 Squad", formatFieldValueFromEntry(entry.getSquadName(), "squad", entry), false);
        embed.addField("👤 Pessoa", formatFieldValueFromEntry(entry.getPersonName(), "person", entry), false);
        embed.addField("📝 Tipo", formatFieldValueFromEntry(entry.getLogType(), "type", entry), false);
        embed.addField("🏷️ Categorias", formatFieldValueFromEntry(String.join(", ", entry.getCategories()), "categories", entry), false);
        embed.addField("📄 Descrição", formatFieldValueFromEntry(entry.getDescription(), "description", entry), false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Início", formatFieldValueFromEntry(startDate, "dates", entry), false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Fim", formatFieldValueFromEntry(endDate, "dates", entry), false);

        if (hasModifications) {
            embed.addField("", "✅ **Este log foi modificado**\n✏️ Para editar novamente, clique nos botões abaixo", false);
        } else {
            embed.addField("", "✏️ **Para editar, clique nos botões abaixo**", false);
        }
        
        embed.setFooter(String.format("Linha %d do texto original", entry.getLineNumber()));
        
        return embed.build();
    }
    
    private String formatFieldValueFromEntry(String value, String fieldName, BatchLogEntry entry) {
        if (entry.isFieldModified(fieldName)) {
            return "✅ **" + value + "** *(modificado)*";
        }
        return value;
    }

    @Override
    public boolean hasNext(int currentIndex, int totalCount) {
        return currentIndex < totalCount - 1;
    }

    @Override
    public boolean hasPrevious(int currentIndex) {
        return currentIndex > 0;
    }

    @Override
    public int getNextIndex(int currentIndex, int totalCount) {
        return hasNext(currentIndex, totalCount) ? currentIndex + 1 : currentIndex;
    }

    @Override
    public int getPreviousIndex(int currentIndex) {
        return hasPrevious(currentIndex) ? currentIndex - 1 : currentIndex;
    }
}
