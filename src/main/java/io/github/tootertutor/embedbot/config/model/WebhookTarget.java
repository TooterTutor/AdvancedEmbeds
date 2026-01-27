package io.github.tootertutor.embedbot.config.model;

/**
 * Describes a saved webhook destination for publishing embeds.
 */
public record WebhookTarget(String label, String url) {
    public WebhookTarget {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Webhook label is required");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is required");
        }
    }
}
