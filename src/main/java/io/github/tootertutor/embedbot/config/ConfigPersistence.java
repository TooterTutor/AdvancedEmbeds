package io.github.tootertutor.embedbot.config;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tootertutor.embedbot.config.model.WebhookTarget;

/**
 * Persists guild configurations to .properties file.
 * <p>
 * Each guild's settings (builder channel, publish channel, webhooks)
 * are stored as properties with guild ID prefixes.
 * </p>
 */
public class ConfigPersistence {
    private static final Logger log = LoggerFactory.getLogger(ConfigPersistence.class);
    private static final String CONFIG_FILE = "embedbot-config.properties";
    private static final Path CONFIG_PATH = resolveConfigPath();
    private static final String CONFIG_COMMENT = "EmbedBot Guild Configuration";
    private static final Properties properties = new Properties();
    private static boolean isLoading = false;

    static {
        loadGuildConfig(null);
    }

    /**
     * Resolves the config path to live alongside the running JAR (or classpath root
     * in IDE).
     */
    private static Path resolveConfigPath() {
        try {
            var location = ConfigPersistence.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path base = Paths.get(location).toAbsolutePath();
            Path dir = Files.isRegularFile(base) ? base.getParent() : base;
            if (dir == null) {
                log.warn("Unable to determine config directory from code source, using working directory");
                return Paths.get(CONFIG_FILE).toAbsolutePath();
            }
            return dir.resolve(CONFIG_FILE);
        } catch (URISyntaxException e) {
            log.warn("Failed to resolve config path from code source, using working directory", e);
            return Paths.get(CONFIG_FILE).toAbsolutePath();
        }
    }

    /**
     * Saves guild configuration to properties file.
     */
    public static void saveGuildConfig(GuildConfig config) {
        // Skip saving during initial load to avoid cascading writes
        if (isLoading) {
            log.debug("Skipping save during load cycle");
            return;
        }

        try {
            properties.clear();

            // Collect all guild IDs that have settings
            var allGuildIds = config.getAllGuildIds();
            log.info("Saving {} guild(s) to {}", allGuildIds.size(), CONFIG_PATH.toAbsolutePath());

            for (Long guildId : allGuildIds) {
                String guildPrefix = "guild." + guildId + ".";

                Long builderChannel = config.builderChannel(guildId);
                if (builderChannel != null) {
                    properties.setProperty(guildPrefix + "builderChannel", builderChannel.toString());
                }

                Long publishChannel = config.publishChannel(guildId);
                if (publishChannel != null) {
                    properties.setProperty(guildPrefix + "publishChannel", publishChannel.toString());
                }

                var webhooks = config.webhooks(guildId);
                if (!webhooks.isEmpty()) {
                    for (int i = 0; i < webhooks.size(); i++) {
                        WebhookTarget webhook = webhooks.get(i);
                        properties.setProperty(guildPrefix + "webhook." + i + ".label", webhook.label());
                        properties.setProperty(guildPrefix + "webhook." + i + ".url", webhook.url());
                    }
                    properties.setProperty(guildPrefix + "webhook.count", String.valueOf(webhooks.size()));
                }
            }

            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }

            try (var out = Files.newOutputStream(CONFIG_PATH, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(out, CONFIG_COMMENT);
                log.info("Successfully saved guild configuration");
            }
        } catch (IOException e) {
            log.error("Failed to save guild configuration", e);
        }
    }

    /**
     * Loads guild configuration from properties file.
     */
    public static void loadGuildConfig(GuildConfig config) {
        isLoading = true;
        try {
            properties.clear();

            if (!Files.exists(CONFIG_PATH)) {
                log.info("No configuration file found at {}", CONFIG_PATH.toAbsolutePath());
                return;
            }

            try (var in = Files.newInputStream(CONFIG_PATH)) {
                properties.load(in);
                log.info("Loaded properties from {}", CONFIG_PATH.toAbsolutePath());
            }

            if (config == null) {
                return;
            }

            // Parse guild settings
            int guildCount = 0;
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith("guild.")) {
                    String[] parts = key.split("\\.");
                    if (parts.length >= 2) {
                        try {
                            long guildId = Long.parseLong(parts[1]);
                            String guildPrefix = "guild." + guildId + ".";

                            // Load builder channel
                            String builderChannelProp = guildPrefix + "builderChannel";
                            if (properties.containsKey(builderChannelProp)) {
                                long channelId = Long.parseLong(properties.getProperty(builderChannelProp));
                                config.setBuilderChannel(guildId, channelId);
                            }

                            // Load publish channel
                            String publishChannelProp = guildPrefix + "publishChannel";
                            if (properties.containsKey(publishChannelProp)) {
                                long channelId = Long.parseLong(properties.getProperty(publishChannelProp));
                                config.setPublishChannel(guildId, channelId);
                            }

                            // Load webhooks
                            String webhookCountProp = guildPrefix + "webhook.count";
                            if (properties.containsKey(webhookCountProp)) {
                                int webhookCount = Integer.parseInt(properties.getProperty(webhookCountProp));
                                for (int i = 0; i < webhookCount; i++) {
                                    String labelProp = guildPrefix + "webhook." + i + ".label";
                                    String urlProp = guildPrefix + "webhook." + i + ".url";
                                    if (properties.containsKey(labelProp) && properties.containsKey(urlProp)) {
                                        String label = properties.getProperty(labelProp);
                                        String url = properties.getProperty(urlProp);
                                        config.addWebhook(guildId, new WebhookTarget(label, url));
                                    }
                                }
                            }

                            guildCount++;
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse guild ID from property key: {}", key);
                        }
                    }
                }
            }
            log.info("Loaded configuration for {} guild(s)", guildCount);
        } catch (IOException e) {
            log.error("Failed to load guild configuration", e);
        } finally {
            isLoading = false;
        }
    }
}
