package com.meli.teamboardingBot.service.batch.impl;

import com.meli.teamboardingBot.model.batch.BatchLogEntry;
import com.meli.teamboardingBot.service.batch.PreviewNavigator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmbedPreviewNavigationService implements PreviewNavigator {
    
    private static final DateTimeFormatter BRAZILIAN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public MessageEmbed createPreviewEmbed(BatchLogEntry entry, int currentIndex, int totalCount) {
        EmbedBuilder embed = new EmbedBuilder();
        
        embed.setTitle("📋 Preview do Squad Log")
              .setColor(Color.BLUE)
              .setDescription(String.format("**Log %d de %d**", currentIndex + 1, totalCount));

        embed.addField("🏢 Squad", entry.getSquadName(), false);
        embed.addField("👤 Pessoa", entry.getPersonName(), false);
        embed.addField("📝 Tipo", entry.getLogType(), false);
        embed.addField("🏷️ Categorias", String.join(", ", entry.getCategories()), false);
        embed.addField("📄 Descrição", entry.getDescription(), false);
        
        String startDate = entry.getStartDate() != null ? 
            entry.getStartDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Início", startDate, false);
        
        String endDate = entry.getEndDate() != null ? 
            entry.getEndDate().format(BRAZILIAN_DATE_FORMAT) : "Não informado";
        embed.addField("📅 Data de Fim", endDate, false);

        embed.setFooter(String.format("Linha %d do texto original", entry.getLineNumber()));
        
        return embed.build();
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
