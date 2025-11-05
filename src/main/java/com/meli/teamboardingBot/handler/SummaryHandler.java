package com.meli.teamboardingBot.handler;
import com.meli.teamboardingBot.model.FormState;
import com.meli.teamboardingBot.service.FormStateService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@Order(9)
public class SummaryHandler extends AbstractInteractionHandler {
    
    public SummaryHandler(FormStateService formStateService) {
        super(formStateService);
    }
    @Override
    public boolean canHandle(String componentId) {
        return false;
    }
    public void showCreateSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação via modal");
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do Squad Log", "Confirme os dados antes de criar:");
        Button createButton = Button.success("confirmar-criacao", "✅ Criar");
        Button editButton = Button.secondary("editar-log", "✏️ Editar");
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(createButton, editButton))
            .queue();
    }
    public void showCreateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de criação");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do que foi preenchido", "Verifique todos os dados antes de criar o log:");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "✅ Criar"),
                Button.secondary("editar-log", "✏️ Editar")
            )
            .queue();
    }
    public void showUpdateSummary(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do Questionário Selecionado", "Dados atuais do questionário:");
        event.getHook().editOriginalEmbeds(embed.build())
            .setActionRow(
                Button.success("criar-log", "💾 Salvar"),
                Button.secondary("editar-log", "✏️ Alterar"),
                Button.primary("voltar-logs", "↩️ Voltar")
            )
            .queue();
    }
    public void showUpdateSummary(StringSelectInteractionEvent event, FormState state) {
        log.info("Mostrando resumo de atualização via select");
        event.deferEdit().queue();
        EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo do Questionário Selecionado", "Dados atuais do questionário:");
        event.getHook().editOriginalEmbeds(embed.build())
            .setComponents(ActionRow.of(
                Button.success("criar-log", "💾 Salvar"),
                Button.secondary("editar-log", "✏️ Alterar"),
                Button.primary("voltar-logs", "↩️ Voltar")
            ))
            .queue();
    }
    public void showSummary(ModalInteractionEvent event, FormState state) {
        log.info("Mostrando resumo após modal");
        if (state.isCreating()) {
            showCreateSummary(event, state);
        } else {
            EmbedBuilder embed = buildSummaryEmbed(state, "📋 Resumo Atualizado", "Dados atualizados:");
            event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(ActionRow.of(
                    Button.success("criar-log", "💾 Salvar"),
                    Button.secondary("editar-log", "✏️ Alterar"),
                    Button.primary("voltar-logs", "↩️ Voltar")
                ))
                .queue();
        }
    }
    public void showSummary(StringSelectInteractionEvent event) {
        log.warn("Método showSummary(StringSelectInteractionEvent) chamado mas não implementado");
    }
    private EmbedBuilder buildSummaryEmbed(FormState state, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(0x0099FF);
        String squadName = state.getSquadName() != null ? state.getSquadName() : "Não informado";
        String userName = state.getUserName() != null ? state.getUserName() : "Não informado";
        log.info("Construindo resumo - squadName: '{}', userName: '{}', userId: '{}'", 
                   squadName, userName, state.getUserId());
        String typeName = state.getTypeName() != null ? state.getTypeName() : "Não informado";
        String categoryNames = (!state.getCategoryNames().isEmpty()) ? 
            String.join(", ", state.getCategoryNames()) : "Não informado";
        String description2 = state.getDescription() != null ? state.getDescription() : "Não informado";
        String startDateText = state.getStartDate() != null ? 
            formatToBrazilianDate(state.getStartDate()) : "Não informado";
        String endDateText = state.getEndDate() != null ? 
            formatToBrazilianDate(state.getEndDate()) : "Não informada";
        embed.addField("🏢 Squad", squadName, false);
        embed.addField("👤 Pessoa", userName, false);
        embed.addField("📝 Tipo", typeName, false);
        embed.addField("🏷️ Categorias", categoryNames, false);
        embed.addField("📄 Descrição", description2, false);
        embed.addField("📅 Data de Início", startDateText, false);
        embed.addField("📅 Data de Fim", endDateText, false);
        return embed;
    }
    @Override
    public int getPriority() {
        return 9;
    }
}
