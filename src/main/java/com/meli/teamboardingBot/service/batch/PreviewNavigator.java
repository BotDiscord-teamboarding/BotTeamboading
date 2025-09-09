package com.meli.teamboardingBot.service.batch;

import com.meli.teamboardingBot.model.batch.BatchLogEntry;
import net.dv8tion.jda.api.entities.MessageEmbed;
import java.util.List;

public interface PreviewNavigator {
    MessageEmbed createPreviewEmbed(BatchLogEntry entry, int currentIndex, int totalCount);
    boolean hasNext(int currentIndex, int totalCount);
    boolean hasPrevious(int currentIndex);
    int getNextIndex(int currentIndex, int totalCount);
    int getPreviousIndex(int currentIndex);
}
