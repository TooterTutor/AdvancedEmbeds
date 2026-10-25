package io.github.tootertutor.embedbot;

import java.util.EnumSet;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * Bootstraps the EmbedBot using modern JDA 6 and Components V2.
 */
public final class Bot extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static final String ENV_TOKEN = "DISCORD_TOKEN";
    private static final String ENV_GUILD_ID = "GUILD_ID"; // optional for faster command updates

    private static volatile JDA jda;

    private Bot() {
        // entrypoint only
    }

    public static void main(String[] args) throws Exception {
        start();
    }

    public static synchronized void start() throws Exception {
        if (jda != null) {
            log.info("Bot already running");
            return;
        }

        String token = resolveToken();
        JDABuilder builder = JDABuilder.createDefault(token)
                .enableIntents(EnumSet.of(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES))
                .disableCache(EnumSet.of(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS))
                .setActivity(Activity.playing("/embed to build"));

        // Wire up controllers
        builder.addEventListeners(new EmbedController(), new io.github.tootertutor.embedbot.directory.DirectoryController());

        jda = builder.build();
        registerSlashCommands();
        log.info("EmbedBot started");
    }

    public static synchronized void stop() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
            log.info("EmbedBot stopped");
        }
    }

    public static boolean isRunning() {
        return jda != null && jda.getStatus() != JDA.Status.SHUTDOWN;
    }

    private static String resolveToken() {
        // Check system property first (set by GUI)
        String prop = System.getProperty(ENV_TOKEN);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }

        String env = System.getenv(ENV_TOKEN);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        // Load .env from current working directory
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String fromDotenv = dotenv.get(ENV_TOKEN);
        if (fromDotenv != null && !fromDotenv.isBlank()) {
            log.info("Loaded DISCORD_TOKEN from .env file");
            return fromDotenv.trim();
        }
        throw new IllegalStateException("Missing DISCORD_TOKEN in environment or .env file");
    }

    private static void registerSlashCommands() throws InterruptedException {
        Objects.requireNonNull(jda, "jda");
        String guildId = System.getenv(ENV_GUILD_ID);

        var embedCommand = EmbedController.slashCommandDefinition();
        var directoryCommand = io.github.tootertutor.embedbot.directory.DirectoryController.command();

        if (guildId != null && !guildId.isBlank()) {
            var guild = jda.awaitReady().getGuildById(guildId);
            if (guild == null) {
                log.warn("GUILD_ID {} not found, falling back to global commands", guildId);
                jda.updateCommands().addCommands(embedCommand, directoryCommand).queue();
                return;
            }
            log.info("Registering guild commands for {} (fast updates)", guild.getName());
            guild.updateCommands().addCommands(embedCommand, directoryCommand).complete();
        } else {
            log.info("Registering global commands (may take up to an hour to propagate)");
            jda.updateCommands().addCommands(embedCommand, directoryCommand).queue();
        }
    }
}
