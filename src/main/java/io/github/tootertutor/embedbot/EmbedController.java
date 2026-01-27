package io.github.tootertutor.embedbot;

import java.time.Duration;

import com.google.gson.Gson;

import io.github.tootertutor.embedbot.api.WebhookClient;
import io.github.tootertutor.embedbot.config.ConfigPersistence;
import io.github.tootertutor.embedbot.config.GuildConfig;
import io.github.tootertutor.embedbot.interaction.EmbedInteractionHandler;
import io.github.tootertutor.embedbot.session.SessionStore;
import io.github.tootertutor.embedbot.ui.EmbedUiService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import okhttp3.OkHttpClient;

/**
 * Component V2 controller: delegates interaction handling to
 * {@link EmbedInteractionHandler}.
 */
public class EmbedController extends ListenerAdapter {
    private static final String COMMAND = "embed";
    private static final String PREFIX = "eb:v2:";

    private final SessionStore sessions;
    private final GuildConfig guildConfig;
    private final WebhookClient webhookClient;
    private final EmbedUiService uiService;
    private final EmbedInteractionHandler handler;

    public EmbedController() {
        sessions = new SessionStore(Duration.ofMinutes(30));
        guildConfig = new GuildConfig();
        webhookClient = new WebhookClient(new OkHttpClient(), new Gson());
        uiService = new EmbedUiService(PREFIX, guildConfig);
        handler = new EmbedInteractionHandler(COMMAND, PREFIX, sessions, guildConfig, webhookClient, uiService);
        ConfigPersistence.loadGuildConfig(guildConfig);
    }

    public static CommandData slashCommandDefinition() {
        return EmbedInteractionHandler.slashCommandDefinition(COMMAND);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        handler.handleSlashCommand(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        handler.handleButton(event);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        handler.handleStringSelect(event);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        handler.handleModal(event);
    }
}
