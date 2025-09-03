package com.meli.teamboardingBot.service.command;
import com.meli.teamboardingBot.ui.Ui;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.stereotype.Component;
@Component
public class SquadLogCommand implements SlashCommandHandler {
    @Override
    public String getName() {
        return "squad-log";
    }
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook ->
                hook.editOriginalEmbeds(
                        Ui.info("Escolha uma opção").build()
                ).setActionRow(
                        Button.success("criar", "Criar"),
                        Button.secondary("atualizar", "Atualizar")
                ).queue()
        );
    }
}