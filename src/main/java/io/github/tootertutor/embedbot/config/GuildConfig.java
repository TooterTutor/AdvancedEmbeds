package io.github.tootertutor.embedbot.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.github.tootertutor.embedbot.config.model.WebhookTarget;

/**
 * In-memory per-guild configuration (builder/publish channels and webhook
 * targets).
 * Auto-saves to embedbot-config.json via ConfigPersistence.
 */
public class GuildConfig {
    private final ConcurrentHashMap<Long, Long> builderChannels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> publishChannels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<WebhookTarget>> webhookTargets = new ConcurrentHashMap<>();

    public void setBuilderChannel(long guildId, long channelId) {
        builderChannels.put(guildId, channelId);
        ConfigPersistence.saveGuildConfig(this);
    }

    public void setPublishChannel(long guildId, long channelId) {
        publishChannels.put(guildId, channelId);
        ConfigPersistence.saveGuildConfig(this);
    }

    public Long builderChannel(long guildId) {
        return builderChannels.get(guildId);
    }

    public Long publishChannel(long guildId) {
        return publishChannels.get(guildId);
    }

    public List<WebhookTarget> webhooks(long guildId) {
        return webhookTargets.getOrDefault(guildId, List.of());
    }

    public void addWebhook(long guildId, WebhookTarget target) {
        webhookTargets.compute(guildId, (id, list) -> {
            List<WebhookTarget> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.removeIf(existing -> existing.label().equalsIgnoreCase(target.label()));
            next.add(target);
            return next;
        });
        ConfigPersistence.saveGuildConfig(this);
    }

    public boolean removeWebhook(long guildId, String label) {
        boolean removed = webhookTargets.computeIfPresent(guildId, (id, list) -> {
            List<WebhookTarget> next = new ArrayList<>(list);
            next.removeIf(existing -> existing.label().equalsIgnoreCase(label));
            return next.isEmpty() ? null : next;
        }) != null;
        if (removed) {
            ConfigPersistence.saveGuildConfig(this);
        }
        return removed;
    }

    public List<WebhookTarget> immutableWebhooks(long guildId) {
        return Collections.unmodifiableList(webhooks(guildId));
    }

    /**
     * Returns all guild IDs that have any configuration.
     */
    public java.util.Set<Long> getAllGuildIds() {
        java.util.Set<Long> guildIds = new java.util.HashSet<>();
        guildIds.addAll(builderChannels.keySet());
        guildIds.addAll(publishChannels.keySet());
        guildIds.addAll(webhookTargets.keySet());
        return guildIds;
    }
}
