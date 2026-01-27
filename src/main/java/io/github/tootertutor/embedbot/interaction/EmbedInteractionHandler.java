package io.github.tootertutor.embedbot.interaction;

import static io.github.tootertutor.embedbot.Util.*;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.github.tootertutor.embedbot.api.WebhookClient;
import io.github.tootertutor.embedbot.config.ConfigPersistence;
import io.github.tootertutor.embedbot.config.GuildConfig;
import io.github.tootertutor.embedbot.config.model.WebhookTarget;
import io.github.tootertutor.embedbot.model.EmbedDraft;
import io.github.tootertutor.embedbot.model.MenuMode;
import io.github.tootertutor.embedbot.session.EditingSession;
import io.github.tootertutor.embedbot.session.SessionStore;
import io.github.tootertutor.embedbot.ui.EmbedUiService;
import io.github.tootertutor.embedbot.util.EmbedMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.OkHttpClient;

/**
 * Central interaction handler for slash commands, buttons, select menus, and
 * modals.
 * Keeps EmbedController thin while preserving existing behavior.
 */
public class EmbedInteractionHandler {
    private static final Logger log = LoggerFactory.getLogger(EmbedInteractionHandler.class);

    private final String command;
    private final String prefix;
    private final SessionStore sessions;
    private final GuildConfig guildConfig;
    private final WebhookClient webhookClient;
    private final EmbedUiService uiService;

    public EmbedInteractionHandler(String command, String prefix, SessionStore sessions, GuildConfig guildConfig,
            WebhookClient webhookClient, EmbedUiService uiService) {
        this.command = command;
        this.prefix = prefix;
        this.sessions = sessions;
        this.guildConfig = guildConfig;
        this.webhookClient = webhookClient;
        this.uiService = uiService;
    }

    // ---------- Slash commands ----------

    public void handleSlashCommand(SlashCommandInteractionEvent event) {
        if (!command.equals(event.getName())) {
            return;
        }
        if (event.getGuild() == null) {
            event.reply("This command must be used in a server.").setEphemeral(true).queue();
            return;
        }

        String sub = event.getSubcommandName();
        if (sub == null) {
            event.reply("Try /embed help to get started.").setEphemeral(true).queue();
            return;
        }

        try {
            switch (sub) {
                case "new" -> handleNew(event);
                case "edit" -> handleEdit(event);
                case "copy" -> handleCopy(event);
                case "cancel" -> handleCancel(event);
                case "publish" -> handlePublish(event);
                case "set-builder-channel" -> handleSetBuilderChannel(event);
                case "set-publish-channel" -> handleSetPublishChannel(event);
                case "webhook-add" -> handleWebhookAdd(event);
                case "webhook-remove" -> handleWebhookRemove(event);
                case "webhook-list" -> handleWebhookList(event);
                case "help" -> handleHelp(event);
                default -> event.reply("Unknown subcommand").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("Unexpected error handling subcommand {}", sub, e);
            event.reply("Something went wrong. Please try again.").setEphemeral(true).queue();
        }
    }

    public static CommandData slashCommandDefinition(String command) {
        return Commands.slash(command, "Interactive embed builder")
                .addSubcommands(
                        new SubcommandData("new", "Start a new embed session"),
                        new SubcommandData("edit", "Edit embeds of an existing message")
                                .addOption(OptionType.STRING, "message_id", "ID of a message in this channel", true),
                        new SubcommandData("copy", "Copy embeds from a message URL")
                                .addOption(OptionType.STRING, "message_url", "Full message URL to copy embeds from",
                                        true),
                        new SubcommandData("cancel", "Cancel your current session"),
                        new SubcommandData("publish", "Publish current draft"),
                        new SubcommandData("set-builder-channel", "Set channel for builder UI")
                                .addOption(OptionType.CHANNEL, "channel", "Text channel to host the builder", true),
                        new SubcommandData("set-publish-channel", "Set default publish channel")
                                .addOption(OptionType.CHANNEL, "channel", "Text channel for publishing embeds", true),
                        new SubcommandData("webhook-add", "Save a webhook destination")
                                .addOption(OptionType.STRING, "label", "Display name for webhook", true)
                                .addOption(OptionType.STRING, "url", "Webhook URL", true),
                        new SubcommandData("webhook-remove", "Remove a saved webhook")
                                .addOption(OptionType.STRING, "label", "Display name to remove", true),
                        new SubcommandData("webhook-list", "List saved webhooks"),
                        new SubcommandData("help", "How to use the embed builder"));
    }

    private void handleNew(SlashCommandInteractionEvent event) {
        EditingSession session = sessions.getOrCreate(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                event.getChannel().getIdLong());
        session.menuMode(MenuMode.MAIN);
        session.touch();
        uiService.sendInterface(event, session, "Starting a fresh embed builder session…");
    }

    private void handleEdit(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("message_id").getAsString();
        event.getChannel().retrieveMessageById(messageId).queue(msg -> {
            if (msg.getEmbeds().isEmpty()) {
                event.reply("That message has no embeds to edit.").setEphemeral(true).queue();
                return;
            }
            EditingSession session = sessions.getOrCreate(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                    event.getChannel().getIdLong());
            session.drafts().clear();
            msg.getEmbeds().forEach(embed -> session.drafts().add(new EmbedDraft(embed)));
            session.select(0);
            uiService.sendInterface(event, session,
                    "Loaded %d embed(s) from the message.".formatted(msg.getEmbeds().size()));
        }, err -> event.reply("Couldn't find that message in this channel.").setEphemeral(true).queue());
    }

    private void handleCopy(SlashCommandInteractionEvent event) {
        event.reply("Copy flow not yet implemented. Paste message ID in this channel as a workaround.")
                .setEphemeral(true).queue();
    }

    private void handleCancel(SlashCommandInteractionEvent event) {
        sessions.remove(event.getGuild().getIdLong(), event.getUser().getIdLong(), event.getChannel().getIdLong());
        event.reply("Session cancelled.").setEphemeral(true).queue();
    }

    private void handlePublish(SlashCommandInteractionEvent event) {
        Optional<EditingSession> sessionOpt = sessions.get(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                event.getChannel().getIdLong());
        if (sessionOpt.isEmpty()) {
            event.reply("No active session. Use /embed new.").setEphemeral(true).queue();
            return;
        }
        EditingSession session = sessionOpt.get();
        List<MessageEmbed> toSend = session.drafts().stream().map(EmbedDraft::build).toList();
        Long publishChannelId = guildConfig.publishChannel(event.getGuild().getIdLong());
        var targetChannel = publishChannelId != null ? event.getGuild().getTextChannelById(publishChannelId)
                : event.getChannel();
        if (targetChannel == null) {
            event.reply("Publish channel not set or not found.").setEphemeral(true).queue();
            return;
        }
        targetChannel.sendMessageEmbeds(toSend).queue(msg -> {
            event.reply("Published %d embed(s) to %s".formatted(toSend.size(), targetChannel.getAsMention()))
                    .setEphemeral(true).queue();
            sessions.remove(event.getGuild().getIdLong(), event.getUser().getIdLong(), event.getChannel().getIdLong());
        }, err -> event.reply("Failed to publish: %s".formatted(err.getMessage())).setEphemeral(true).queue());
    }

    private void handleSetBuilderChannel(SlashCommandInteractionEvent event) {
        var channel = event.getOption("channel").getAsChannel();
        if (channel.getType() != ChannelType.TEXT) {
            event.reply("Please pick a text channel.").setEphemeral(true).queue();
            return;
        }
        guildConfig.setBuilderChannel(event.getGuild().getIdLong(), channel.getIdLong());
        event.reply("Builder channel set to %s".formatted(channel.getAsMention())).setEphemeral(true).queue();
    }

    private void handleSetPublishChannel(SlashCommandInteractionEvent event) {
        var channel = event.getOption("channel").getAsChannel();
        if (channel.getType() != ChannelType.TEXT) {
            event.reply("Please pick a text channel.").setEphemeral(true).queue();
            return;
        }
        guildConfig.setPublishChannel(event.getGuild().getIdLong(), channel.getIdLong());
        event.reply("Publish channel set to %s".formatted(channel.getAsMention())).setEphemeral(true).queue();
    }

    private void handleWebhookAdd(SlashCommandInteractionEvent event) {
        String label = nullIfBlank(event.getOption("label").getAsString());
        String url = nullIfBlank(event.getOption("url").getAsString());
        if (label == null || url == null) {
            event.reply("Label and URL are required.").setEphemeral(true).queue();
            return;
        }
        guildConfig.addWebhook(event.getGuild().getIdLong(), new WebhookTarget(label, url));
        event.reply("Saved webhook '%s'.".formatted(label)).setEphemeral(true).queue();
    }

    private void handleWebhookRemove(SlashCommandInteractionEvent event) {
        String label = event.getOption("label").getAsString();
        boolean removed = guildConfig.removeWebhook(event.getGuild().getIdLong(), label);
        if (removed) {
            event.reply("Removed webhook '%s'.".formatted(label)).setEphemeral(true).queue();
        } else {
            event.reply("No webhook named '%s'.".formatted(label)).setEphemeral(true).queue();
        }
    }

    private void handleWebhookList(SlashCommandInteractionEvent event) {
        List<WebhookTarget> hooks = guildConfig.webhooks(event.getGuild().getIdLong());
        if (hooks.isEmpty()) {
            event.reply("No saved webhooks.").setEphemeral(true).queue();
            return;
        }
        String body = hooks.stream().map(h -> "• %s → %s".formatted(h.label(), h.url()))
                .reduce((a, b) -> a + "\n" + b).orElse("No webhooks");
        event.reply("Saved webhooks:\n" + body).setEphemeral(true).queue();
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        event.reply("Use /embed new to start building. Buttons and modals drive the UI.")
                .setEphemeral(true)
                .queue();
    }

    // ---------- Buttons ----------

    public void handleButton(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith(prefix)) {
            return;
        }
        var sessionOpt = sessions.get(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                event.getChannel().getIdLong());
        if (sessionOpt.isEmpty()) {
            event.reply("No active session. Use /embed new.").setEphemeral(true).queue();
            return;
        }
        EditingSession session = sessionOpt.get();
        String id = event.getComponentId();
        try {
            if (id.equals(prefix + "add")) {
                session.addNew();
            } else if (id.equals(prefix + "del")) {
                session.deleteSelected();
            } else if (id.equals(prefix + "left")) {
                session.moveSelectedLeft();
            } else if (id.equals(prefix + "right")) {
                session.moveSelectedRight();
            } else if (id.equals(prefix + "publish")) {
                handlePublishOptions(event, session);
                return;
            } else if (id.equals(prefix + "cancel")) {
                event.reply("Session cancelled.").setEphemeral(true).queue();
                sessions.remove(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                        event.getChannel().getIdLong());
                return;
            } else if (id.equals(prefix + "title")) {
                showTitleModal(event, session);
                return;
            } else if (id.equals(prefix + "desc")) {
                showDescModal(event, session);
                return;
            } else if (id.equals(prefix + "color")) {
                showColorModal(event, session);
                return;
            } else if (id.equals(prefix + "author")) {
                showAuthorModal(event, session);
                return;
            } else if (id.equals(prefix + "footer")) {
                showFooterModal(event, session);
                return;
            } else if (id.equals(prefix + "images")) {
                showImagesModal(event, session);
                return;
            } else if (id.equals(prefix + "timestamp")) {
                session.current().toggleTimestamp();
            } else if (id.equals(prefix + "field:add")) {
                showAddFieldModal(event, session);
                return;
            } else if (id.equals(prefix + "field:edit")) {
                showEditFieldPicker(event, session);
                return;
            } else if (id.equals(prefix + "field:delete")) {
                showDeleteFieldPicker(event, session);
                return;
            } else if (id.equals(prefix + "paste_json")) {
                showJsonModal(event, session);
                return;
            } else {
                event.reply("Unknown action").setEphemeral(true).queue();
                return;
            }
            uiService.updateInterface(event, session);
        } catch (IllegalArgumentException iae) {
            event.reply("Validation error: %s".formatted(iae.getMessage())).setEphemeral(true).queue();
        }
    }

    // ---------- Select menus ----------

    public void handleStringSelect(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith(prefix)) {
            return;
        }
        var sessionOpt = sessions.get(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                event.getChannel().getIdLong());
        if (sessionOpt.isEmpty()) {
            event.reply("No active session. Use /embed new.").setEphemeral(true).queue();
            return;
        }
        EditingSession session = sessionOpt.get();
        String id = event.getComponentId();
        if (id.equals(prefix + "select")) {
            int idx = Integer.parseInt(event.getValues().get(0));
            session.select(idx);
            uiService.updateInterface(event, session);
        } else if (id.equals(prefix + "menu")) {
            MenuMode mode = MenuMode.valueOf(event.getValues().get(0));
            session.menuMode(mode);
            uiService.updateInterface(event, session);
        } else if (id.equals(prefix + "pub:dest")) {
            handlePublishDestinationSelect(event, session);
        } else if (id.equals(prefix + "field:edit:pick")) {
            int idx = Integer.parseInt(event.getValues().get(0));
            showEditFieldModal(event, session, idx);
        } else if (id.equals(prefix + "field:delete:pick")) {
            int idx = Integer.parseInt(event.getValues().get(0));
            handleDeleteField(event, session, idx);
        }
    }

    // ---------- Modals ----------

    public void handleModal(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(prefix)) {
            return;
        }
        var sessionOpt = sessions.get(event.getGuild().getIdLong(), event.getUser().getIdLong(),
                event.getChannel().getIdLong());
        if (sessionOpt.isEmpty()) {
            event.reply("No active session. Use /embed new.").setEphemeral(true).queue();
            return;
        }
        EditingSession session = sessionOpt.get();
        String id = event.getModalId();
        if (id.startsWith(prefix + "m:field:edit:")) {
            if (!applyFieldEdit(event, session, id)) {
                return;
            }
        } else {
            if (id.equals(prefix + "m:title")) {
                String title = nullIfBlank(event.getValue("title").getAsString());
                String url = nullIfBlank(event.getValue("url").getAsString());
                if (url != null) {
                    session.current().builder().setTitle(title, url);
                } else {
                    session.current().builder().setTitle(title);
                }
            } else if (id.equals(prefix + "m:desc")) {
                String desc = nullIfBlank(event.getValue("desc").getAsString());
                session.current().builder().setDescription(desc);
            } else if (id.equals(prefix + "m:color")) {
                Color c = parseColor(nullIfBlank(event.getValue("color").getAsString()));
                session.current().builder().setColor(c);
            } else if (id.equals(prefix + "m:author")) {
                String name = nullIfBlank(event.getValue("name").getAsString());
                String url = nullIfBlank(event.getValue("url").getAsString());
                String icon = nullIfBlank(event.getValue("icon").getAsString());
                if (name == null && url == null && icon == null) {
                    session.current().builder().setAuthor(null, null, null);
                } else {
                    session.current().builder().setAuthor(name, url, icon);
                }
            } else if (id.equals(prefix + "m:footer")) {
                String text = nullIfBlank(event.getValue("text").getAsString());
                String icon = nullIfBlank(event.getValue("icon").getAsString());
                session.current().builder().setFooter(text, icon);
            } else if (id.equals(prefix + "m:images")) {
                String thumb = nullIfBlank(event.getValue("thumb").getAsString());
                String image = nullIfBlank(event.getValue("image").getAsString());
                session.current().builder().setThumbnail(thumb);
                session.current().builder().setImage(image);
            } else if (id.equals(prefix + "m:json")) {
                String jsonStr = nullIfBlank(event.getValue("json").getAsString());
                if (jsonStr == null) {
                    event.reply("❌ No JSON provided.").setEphemeral(true).queue();
                    return;
                }
                try {
                    DataObject jsonData = DataObject.fromJson(jsonStr);
                    EmbedMapper.applyJsonToEmbed(session.current().builder(), jsonData);
                } catch (Exception e) {
                    log.error("Error parsing JSON", e);
                    event.reply("❌ Invalid JSON: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
            } else if (id.equals(prefix + "m:field:new")) {
                if (!applyFieldAdd(event, session)) {
                    return;
                }
            } else {
                event.reply("Unknown modal").setEphemeral(true).queue();
                return;
            }
        }
        session.touch();
        uiService.updateInterface(event, session);
    }

    // ---------- UI helpers (modals and pickers) ----------

    private void showTitleModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        TextInput.Builder title = TextInput
                .create("title", TextInputStyle.SHORT)
                .setPlaceholder("Title (blank to clear)")
                .setRequired(false)
                .setMaxLength(256);
        TextInput.Builder url = TextInput.create("url", TextInputStyle.SHORT)
                .setPlaceholder("URL (optional)")
                .setRequired(false);

        if (embed.getTitle() != null) {
            title.setValue(embed.getTitle());
        }
        if (embed.getUrl() != null) {
            url.setValue(embed.getUrl());
        }

        Modal modal = Modal.create(prefix + "m:title", "Set Title / URL")
                .addComponents(
                        Label.of("Title", title.build()),
                        Label.of("URL", url.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showDescModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        var desc = TextInput.create("desc",
                TextInputStyle.PARAGRAPH)
                .setPlaceholder("Description (blank to clear)")
                .setRequired(false)
                .setMaxLength(4000);
        if (embed.getDescription() != null) {
            desc.setValue(embed.getDescription());
        }

        Modal modal = Modal.create(prefix + "m:desc", "Set Description")
                .addComponents(Label.of("Description", desc.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showColorModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        var color = TextInput.create("color",
                TextInputStyle.SHORT)
                .setPlaceholder("Hex Color (#RRGGBB)")
                .setRequired(false);
        if (embed.getColor() != null) {
            color.setValue(EmbedMapper.toHex(embed.getColor()));
        }

        Modal modal = Modal.create(prefix + "m:color", "Set Color")
                .addComponents(Label.of("Color", color.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showAuthorModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        var name = TextInput.create("name",
                TextInputStyle.SHORT)
                .setPlaceholder("Author name")
                .setRequired(false);
        var url = TextInput.create("url",
                TextInputStyle.SHORT)
                .setPlaceholder("Author URL")
                .setRequired(false);
        var icon = TextInput.create("icon",
                TextInputStyle.SHORT)
                .setPlaceholder("Author icon URL")
                .setRequired(false);

        if (embed.getAuthor() != null) {
            if (embed.getAuthor().getName() != null) {
                name.setValue(embed.getAuthor().getName());
            }
            if (embed.getAuthor().getUrl() != null) {
                url.setValue(embed.getAuthor().getUrl());
            }
            if (embed.getAuthor().getIconUrl() != null) {
                icon.setValue(embed.getAuthor().getIconUrl());
            }
        }

        Modal modal = Modal.create(prefix + "m:author", "Set Author")
                .addComponents(
                        Label.of("Name", name.build()),
                        Label.of("URL", url.build()),
                        Label.of("Icon", icon.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showFooterModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        var text = TextInput.create("text",
                TextInputStyle.SHORT)
                .setPlaceholder("Footer text")
                .setRequired(false);
        var icon = TextInput.create("icon",
                TextInputStyle.SHORT)
                .setPlaceholder("Footer icon URL")
                .setRequired(false);

        if (embed.getFooter() != null) {
            if (embed.getFooter().getText() != null) {
                text.setValue(embed.getFooter().getText());
            }
            if (embed.getFooter().getIconUrl() != null) {
                icon.setValue(embed.getFooter().getIconUrl());
            }
        }

        Modal modal = Modal.create(prefix + "m:footer", "Set Footer")
                .addComponents(
                        Label.of("Text", text.build()),
                        Label.of("Icon", icon.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showImagesModal(ButtonInteractionEvent event, EditingSession session) {
        var embed = session.current().build();
        var thumb = TextInput.create("thumb",
                TextInputStyle.SHORT)
                .setPlaceholder("Thumbnail URL")
                .setRequired(false);
        var image = TextInput.create("image",
                TextInputStyle.SHORT)
                .setPlaceholder("Image URL")
                .setRequired(false);

        if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null) {
            thumb.setValue(embed.getThumbnail().getUrl());
        }
        if (embed.getImage() != null && embed.getImage().getUrl() != null) {
            image.setValue(embed.getImage().getUrl());
        }

        Modal modal = Modal.create(prefix + "m:images", "Set Images")
                .addComponents(
                        Label.of("Thumbnail", thumb.build()),
                        Label.of("Image", image.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showAddFieldModal(ButtonInteractionEvent event, EditingSession session) {
        var fields = session.current().build().getFields();

        var position = TextInput.create("position",
                TextInputStyle.SHORT)
                .setPlaceholder("0 to " + fields.size())
                .setRequired(false)
                .setValue(Integer.toString(fields.size()));

        var name = TextInput.create("name",
                TextInputStyle.SHORT)
                .setPlaceholder("Field name")
                .setRequired(true)
                .setMaxLength(256);
        var value = TextInput.create("value",
                TextInputStyle.PARAGRAPH)
                .setPlaceholder("Field value")
                .setRequired(true)
                .setMaxLength(1024);
        var inline = TextInput.create("inline",
                TextInputStyle.SHORT)
                .setPlaceholder("true or false")
                .setRequired(false)
                .setValue("false");

        Modal modal = Modal.create(prefix + "m:field:new", "Add Field")
                .addComponents(
                        Label.of("Position", position.build()),
                        Label.of("Name", name.build()),
                        Label.of("Value", value.build()),
                        Label.of("Inline?", inline.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void showEditFieldPicker(ButtonInteractionEvent event, EditingSession session) {
        List<MessageEmbed.Field> fields = session.current().build().getFields();
        if (fields.isEmpty()) {
            event.reply("No fields to edit.").setEphemeral(true).queue();
            return;
        }
        StringSelectMenu.Builder menu = StringSelectMenu.create(prefix + "field:edit:pick")
                .setPlaceholder("Choose a field to edit");
        for (int i = 0; i < fields.size(); i++) {
            MessageEmbed.Field f = fields.get(i);
            menu.addOption(truncate(f.getName(), 80), Integer.toString(i), truncate(f.getValue(), 80));
        }
        event.reply("Select a field to edit")
                .setComponents(ActionRow.of(menu.build()))
                .setEphemeral(true)
                .queue();
    }

    private void showDeleteFieldPicker(ButtonInteractionEvent event, EditingSession session) {
        List<MessageEmbed.Field> fields = session.current().build().getFields();
        if (fields.isEmpty()) {
            event.reply("No fields to delete.").setEphemeral(true).queue();
            return;
        }
        StringSelectMenu.Builder menu = StringSelectMenu.create(prefix + "field:delete:pick")
                .setPlaceholder("Choose a field to delete");
        for (int i = 0; i < fields.size(); i++) {
            MessageEmbed.Field f = fields.get(i);
            menu.addOption(truncate(f.getName(), 80), Integer.toString(i), truncate(f.getValue(), 80));
        }
        event.reply("Select a field to delete")
                .setComponents(ActionRow.of(menu.build()))
                .setEphemeral(true)
                .queue();
    }

    private void showEditFieldModal(StringSelectInteractionEvent event, EditingSession session, int index) {
        List<MessageEmbed.Field> fields = session.current().build().getFields();
        if (index < 0 || index >= fields.size()) {
            event.reply("Invalid field index.").setEphemeral(true).queue();
            return;
        }
        MessageEmbed.Field field = fields.get(index);

        var position = TextInput.create("position",
                TextInputStyle.SHORT)
                .setPlaceholder("0 to " + fields.size())
                .setRequired(false)
                .setValue(Integer.toString(index));
        var name = TextInput.create("name",
                TextInputStyle.SHORT)
                .setRequired(true)
                .setMaxLength(256)
                .setValue(field.getName());
        var value = TextInput.create("value",
                TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .setMaxLength(1024)
                .setValue(field.getValue());
        var inline = TextInput.create("inline",
                TextInputStyle.SHORT)
                .setPlaceholder("true or false")
                .setRequired(false)
                .setValue(Boolean.toString(field.isInline()));

        Modal modal = Modal.create(prefix + "m:field:edit:" + index, "Edit Field")
                .addComponents(
                        Label.of("Position", position.build()),
                        Label.of("Name", name.build()),
                        Label.of("Value", value.build()),
                        Label.of("Inline?", inline.build()))
                .build();
        event.replyModal(modal).queue();
    }

    private void handleDeleteField(StringSelectInteractionEvent event, EditingSession session, int index) {
        EmbedBuilder builder = session.current().builder();
        List<MessageEmbed.Field> fields = new ArrayList<>(builder.build().getFields());
        if (index < 0 || index >= fields.size()) {
            event.reply("Invalid field index.").setEphemeral(true).queue();
            return;
        }
        fields.remove(index);
        EmbedMapper.replaceFields(builder, fields);
        session.touch();
        uiService.updateInterface(event, session);
    }

    private boolean applyFieldAdd(ModalInteractionEvent event, EditingSession session) {
        EmbedBuilder builder = session.current().builder();
        List<MessageEmbed.Field> fields = new ArrayList<>(builder.build().getFields());
        if (fields.size() >= 25) {
            event.reply("❌ Embeds can have at most 25 fields.").setEphemeral(true).queue();
            return false;
        }

        String name = nullIfBlank(event.getValue("name").getAsString());
        String value = nullIfBlank(event.getValue("value").getAsString());
        String inlineRaw = event.getValue("inline") != null ? nullIfBlank(event.getValue("inline").getAsString())
                : null;
        String positionRaw = event.getValue("position") != null
                ? nullIfBlank(event.getValue("position").getAsString())
                : null;

        if (name == null || value == null) {
            event.reply("❌ Field name and value are required.").setEphemeral(true).queue();
            return false;
        }

        int position = fields.size();
        if (positionRaw != null) {
            try {
                position = Integer.parseInt(positionRaw);
            } catch (NumberFormatException e) {
                event.reply("❌ Position must be a number.").setEphemeral(true).queue();
                return false;
            }
        }
        if (position < 0 || position > fields.size()) {
            event.reply("❌ Position must be between 0 and %d.".formatted(fields.size())).setEphemeral(true)
                    .queue();
            return false;
        }

        boolean inline = inlineRaw != null && Boolean.parseBoolean(inlineRaw);
        fields.add(position, new MessageEmbed.Field(name, value, inline));
        EmbedMapper.replaceFields(builder, fields);
        return true;
    }

    private boolean applyFieldEdit(ModalInteractionEvent event, EditingSession session, String modalId) {
        EmbedBuilder builder = session.current().builder();
        List<MessageEmbed.Field> fields = new ArrayList<>(builder.build().getFields());
        int index;
        try {
            index = Integer.parseInt(modalId.substring((prefix + "m:field:edit:").length()));
        } catch (NumberFormatException e) {
            event.reply("❌ Invalid field index.").setEphemeral(true).queue();
            return false;
        }

        if (index < 0 || index >= fields.size()) {
            event.reply("❌ Invalid field index.").setEphemeral(true).queue();
            return false;
        }

        String name = nullIfBlank(event.getValue("name").getAsString());
        String value = nullIfBlank(event.getValue("value").getAsString());
        String inlineRaw = event.getValue("inline") != null ? nullIfBlank(event.getValue("inline").getAsString())
                : null;
        String positionRaw = event.getValue("position") != null
                ? nullIfBlank(event.getValue("position").getAsString())
                : null;

        if (name == null || value == null) {
            event.reply("❌ Field name and value are required.").setEphemeral(true).queue();
            return false;
        }

        boolean inline = inlineRaw != null && Boolean.parseBoolean(inlineRaw);

        fields.remove(index);
        int maxPos = fields.size();
        int position = Math.min(index, maxPos);
        if (positionRaw != null) {
            try {
                position = Integer.parseInt(positionRaw);
            } catch (NumberFormatException e) {
                event.reply("❌ Position must be a number.").setEphemeral(true).queue();
                return false;
            }
        }
        if (position < 0 || position > maxPos) {
            event.reply("❌ Position must be between 0 and %d.".formatted(maxPos)).setEphemeral(true).queue();
            return false;
        }

        fields.add(position, new MessageEmbed.Field(name, value, inline));
        EmbedMapper.replaceFields(builder, fields);
        return true;
    }

    private void showJsonModal(ButtonInteractionEvent event, EditingSession session) {
        var json = TextInput.create("json",
                TextInputStyle.PARAGRAPH)
                .setPlaceholder("Paste embed JSON here...")
                .setRequired(true)
                .setMinLength(2)
                .setMaxLength(4000);

        Modal modal = Modal.create(prefix + "m:json", "Paste Embed JSON")
                .addComponents(Label.of("JSON Data", json.build()))
                .build();
        event.replyModal(modal).queue();
    }

    // ---------- Publishing ----------

    private void handlePublishOptions(ButtonInteractionEvent event, EditingSession session) {
        StringSelectMenu.Builder destinationMenu = StringSelectMenu.create(prefix + "pub:dest");

        Long publishChannelId = guildConfig.publishChannel(event.getGuild().getIdLong());
        if (publishChannelId != null) {
            var channel = event.getGuild().getTextChannelById(publishChannelId);
            if (channel != null) {
                destinationMenu.addOption("📢 Publish Channel", "publish_channel",
                        "Post to " + channel.getName());
            }
        }

        var webhooks = guildConfig.webhooks(event.getGuild().getIdLong());
        if (!webhooks.isEmpty()) {
            for (int i = 0; i < webhooks.size(); i++) {
                var hook = webhooks.get(i);
                destinationMenu.addOption("🪝 " + hook.label(), "webhook:" + i,
                        hook.url().length() > 60 ? hook.url().substring(0, 60) + "…" : hook.url());
            }
            destinationMenu.addOption("🪝 All Webhooks (" + webhooks.size() + ")", "webhooks_all",
                    "Post to every saved webhook");
        }

        destinationMenu.addOption("💬 This Channel", "current_channel",
                "Post to " + event.getChannel().getAsMention());

        destinationMenu.addOption("❌ Cancel", "cancel", "Don't post");

        event.reply("📤 **Where do you want to publish?**")
                .addComponents(ActionRow.of(destinationMenu.setPlaceholder("Select destination").build()))
                .setEphemeral(true)
                .queue();
    }

    private void handlePublishDestinationSelect(StringSelectInteractionEvent event, EditingSession session) {
        String destination = event.getValues().get(0);
        var embeds = session.drafts().stream().map(EmbedDraft::build).toList();

        if (destination.equals("cancel")) {
            event.reply("Publishing cancelled.").setEphemeral(true).queue();
            return;
        }

        try {
            if (destination.equals("current_channel")) {
                event.getChannel().asTextChannel().sendMessageEmbeds(embeds).queue(
                        success -> event.reply("✅ Published to this channel!").setEphemeral(true).queue(),
                        error -> event.reply("❌ Failed to publish: " + error.getMessage()).setEphemeral(true).queue());
            } else if (destination.equals("publish_channel")) {
                Long channelId = guildConfig.publishChannel(event.getGuild().getIdLong());
                if (channelId == null) {
                    event.reply("No publish channel configured.").setEphemeral(true).queue();
                    return;
                }
                var channel = event.getGuild().getTextChannelById(channelId);
                if (channel == null) {
                    event.reply("Publish channel not found.").setEphemeral(true).queue();
                    return;
                }
                channel.sendMessageEmbeds(embeds).queue(
                        success -> event.reply("✅ Published to " + channel.getAsMention()).setEphemeral(true).queue(),
                        error -> event.reply("❌ Failed to publish: " + error.getMessage()).setEphemeral(true).queue());
            } else if (destination.startsWith("webhook:")) {
                int index = Integer.parseInt(destination.substring("webhook:".length()));
                var webhooks = guildConfig.webhooks(event.getGuild().getIdLong());
                if (index >= webhooks.size()) {
                    event.reply("Webhook not found.").setEphemeral(true).queue();
                    return;
                }
                WebhookTarget target = webhooks.get(index);
                sendToWebhook(target, embeds, event);
            } else if (destination.equals("webhooks_all")) {
                var webhooks = guildConfig.webhooks(event.getGuild().getIdLong());
                if (webhooks.isEmpty()) {
                    event.reply("No webhooks configured.").setEphemeral(true).queue();
                    return;
                }
                event.deferReply(true).queue();
                int successCount = 0;
                int failCount = 0;
                for (WebhookTarget target : webhooks) {
                    try {
                        sendToWebhookSync(target, embeds);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to send to webhook {}", target.label(), e);
                        failCount++;
                    }
                }
                event.getHook().editOriginal("✅ Published to " + successCount + " webhook(s)" +
                        (failCount > 0 ? " (" + failCount + " failed)" : "")).queue();
            } else {
                event.reply("Unknown destination.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("Error publishing embeds", e);
            event.reply("❌ Error: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void sendToWebhook(WebhookTarget target, List<MessageEmbed> embeds,
            StringSelectInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            try {
                WebhookClient.WebhookResult result = webhookClient.send(target, embeds);
                if (result.success()) {
                    hook.editOriginal("✅ Published to webhook: " + target.label()).queue();
                } else {
                    log.error("Webhook request failed: {} - {}", result.code(), result.body());
                    hook.editOriginal("❌ Failed: HTTP " + result.code()).queue();
                }
            } catch (Exception e) {
                log.error("Error preparing webhook request for {}", target.label(), e);
                hook.editOriginal("❌ Error: " + e.getMessage()).queue();
            }
        });
    }

    private void sendToWebhookSync(WebhookTarget target, List<MessageEmbed> embeds) throws Exception {
        WebhookClient.WebhookResult result = webhookClient.send(target, embeds);
        if (!result.success()) {
            throw new Exception("HTTP " + result.code() + ": " + result.body());
        }
    }

    // ---------- Factory helper ----------

    public static EmbedInteractionHandler createDefault() {
        SessionStore sessions = new SessionStore(Duration.ofMinutes(30));
        GuildConfig guildConfig = new GuildConfig();
        WebhookClient webhookClient = new WebhookClient(new OkHttpClient(), new Gson());
        EmbedUiService uiService = new EmbedUiService("eb:v2:", guildConfig);
        ConfigPersistence.loadGuildConfig(guildConfig);
        return new EmbedInteractionHandler("embed", "eb:v2:", sessions, guildConfig, webhookClient, uiService);
    }
}
