package com.meli.teamboardingBot.service.command;

import com.meli.teamboardingBot.service.FormStateService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StopCommand implements SlashCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StopCommand.class);
    private final FormStateService formStateService;

    public StopCommand(FormStateService formStateService) {
        this.formStateService = formStateService;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        
        logger.info("🛑 Comando /stop executado por usuário: {}", userId);
        
        var state = formStateService.getState(userId);
        
        if (state == null || (!state.isCreating() && !state.isEditing())) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("ℹ️ Nenhum fluxo ativo")
                .setDescription("Você não está em nenhum processo de criação ou edição no momento.\n\n" +
                              "Use `/squad-log` para iniciar um novo fluxo.")
                .setColor(0x3498db);
            
            event.replyEmbeds(embed.build())
                .setEphemeral(true)
                .queue();
            
            logger.info("ℹ️ Usuário {} não tinha fluxo ativo", userId);
            return;
        }
        
        String fluxoTipo = state.isCreating() ? "criação" : "edição";
        formStateService.removeState(userId);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("🛑 Fluxo de " + fluxoTipo + " encerrado")
            .setDescription("O processo foi cancelado com sucesso.\n\n" +
                          "Todos os dados não salvos foram descartados.\n\n" +
                          "Use `/squad-log` quando quiser começar novamente.")
            .setColor(0xe74c3c)
            .setFooter("Processo cancelado pelo usuário");
        
        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .queue();
        
        logger.info("✅ Fluxo de {} encerrado para usuário {}", fluxoTipo, userId);
    }
}
