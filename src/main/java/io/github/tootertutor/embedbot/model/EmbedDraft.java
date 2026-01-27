package io.github.tootertutor.embedbot.model;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * Mutable embed draft wrapper around JDA's {@link EmbedBuilder} with a
 * convenience converter to MessageEmbed.
 */
public class EmbedDraft {
    private final EmbedBuilder builder;

    public EmbedDraft() {
        this.builder = new EmbedBuilder();
    }

    public EmbedDraft(MessageEmbed source) {
        this();
        copyFrom(source);
    }

    public EmbedBuilder builder() {
        return builder;
    }

    public MessageEmbed build() {
        return builder.build();
    }

    public void copyFrom(MessageEmbed me) {
        builder.clear();
        if (me.getTitle() != null) {
            builder.setTitle(me.getTitle(), me.getUrl());
        }
        if (me.getDescription() != null) {
            builder.setDescription(me.getDescription());
        }
        if (me.getColor() != null) {
            builder.setColor(me.getColor());
        }
        if (me.getThumbnail() != null) {
            builder.setThumbnail(me.getThumbnail().getUrl());
        }
        if (me.getImage() != null) {
            builder.setImage(me.getImage().getUrl());
        }
        if (me.getAuthor() != null) {
            builder.setAuthor(me.getAuthor().getName(), me.getAuthor().getUrl(), me.getAuthor().getIconUrl());
        }
        if (me.getFooter() != null) {
            builder.setFooter(me.getFooter().getText(), me.getFooter().getIconUrl());
        }
        if (me.getTimestamp() != null) {
            builder.setTimestamp(me.getTimestamp());
        }
        List<MessageEmbed.Field> fields = new ArrayList<>(me.getFields());
        fields.forEach(f -> builder.addField(f.getName(), f.getValue(), f.isInline()));
    }

    public void setDefaults() {
        if (builder.isEmpty()) {
            builder.setDescription("Your embed content here...");
        }
    }

    public void toggleTimestamp() {
        if (builder.isEmpty()) {
            builder.setTimestamp(Instant.now());
            return;
        }
        MessageEmbed built = builder.build();
        if (built.getTimestamp() != null) {
            builder.setTimestamp(null);
        } else {
            builder.setTimestamp(Instant.now());
        }
    }

    public void setColorHex(Color color) {
        builder.setColor(color);
    }
}
