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
        EmbedBuilder embed = new EmbedBuilder();
        
        String title = "📋 Preview do Squad Log";
        if (modifiedField != null) {
            title += " ✅";
        }
        
        embed.setTitle(title)
              .setColor(modifiedField != null ? Color.GREEN : Color.BLUE)
              .setDescription(String.format("**Log %d de %d**", currentIndex + 1, totalCount));

        embed.addField("🏢 Squad", formatFieldValue(entry.getSquadName(), "squad", modifiedField), false);
        embed.addField("👤 Pessoa", formatFieldValue(entry.getPersonName(), "person", modifiedField), false);
        embed.addField("📝 Tipo", formatFieldValue(entry.getLogType(), "type", modifiedField), false);
        embed.addField("🏷️ Categorias", formatFieldValue(String.join(", ", entry.getCategories()), "categories", modifiedField), false);
        embed.addField("📄 Descrição", formatFieldValue(entry.getDescription(), "description", modifiedField), false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Início", formatFieldValue(startDate, "dates", modifiedField), false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Fim", formatFieldValue(endDate, "dates", modifiedField), false);

        if (modifiedField != null) {
            embed.addField("", "✅ **Campo atualizado com sucesso!**\n✏️ Para editar novamente, clique nos botões abaixo", false);
        } else {
            embed.addField("", "✏️ **Para editar, clique nos botões abaixo**", false);
        }
        
        embed.setFooter(String.format("Linha %d do texto original", entry.getLineNumber()));
        
        return embed.build();
    }
    
    private String formatFieldValue(String value, String fieldName, String modifiedField) {
        if (modifiedField != null && fieldName.equals(modifiedField)) {
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
