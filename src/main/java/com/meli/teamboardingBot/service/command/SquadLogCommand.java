package com.meli.teamboardingBot.service.command;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.ui.Ui;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
@Component
public class SquadLogCommand implements SlashCommandHandler {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private FormState formState;
    @Override
    public String getName() {
        return "squad-log";
    }
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook ->
                hook.editOriginalEmbeds(
                        Ui.info(messageSource.getMessage("txt_escolha_uma_opcao", null, formState.getLocale())).build()
                ).setActionRow(
                        Button.success("criar",  messageSource.getMessage("txt_criar", null, formState.getLocale())),
                        Button.secondary("atualizar", messageSource.getMessage("txt_atualizar", null, formState.getLocale()))
                ).queue()
        );
    }
}