package io.github.tootertutor.embedbot.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import io.github.tootertutor.embedbot.config.GuildConfig;
import io.github.tootertutor.embedbot.model.MenuMode;
import io.github.tootertutor.embedbot.session.EditingSession;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public class EmbedUiService {
    private final String prefix;
    private final GuildConfig guildConfig;

    public EmbedUiService(String prefix, GuildConfig guildConfig) {
        this.prefix = prefix;
        this.guildConfig = guildConfig;
    }

    public void sendInterface(SlashCommandInteractionEvent event, EditingSession session, String ackMessage) {
        var channel = resolveBuilderChannel(event);
        if (channel == null) {
            event.reply("Builder channel not found; set one with /embed set-builder-channel.")
                    .setEphemeral(true).queue();
            return;
        }
        session.touch();

        channel.sendMessage("📝 **DRAFT** - Will not be posted until you click Publish")
                .addEmbeds(session.current().build())
                .queue(previewMsg -> {
                    session.previewMessageId(previewMsg.getId());
                    var components = buildComponentsV2(session);
                    channel.sendMessage("")
                            .useComponentsV2()
                            .setComponents(components)
                            .queue(controlMsg -> {
                                session.controlMessageId(controlMsg.getId());
                                event.reply(ackMessage + " Builder UI is in " + channel.getAsMention())
                                        .setEphemeral(true).queue();
                            });
                });
    }

    public void updateInterface(ButtonInteractionEvent event, EditingSession session) {
        if (session.previewMessageId() != null && session.controlMessageId() != null) {
            event.getChannel().editMessageEmbedsById(session.previewMessageId(), session.current().build()).queue();
            event.deferEdit().queue();
            event.getChannel().editMessageComponentsById(session.controlMessageId())
                    .useComponentsV2()
                    .setComponents(buildComponentsV2(session))
                    .queue();
        }
    }

    public void updateInterface(StringSelectInteractionEvent event, EditingSession session) {
        if (session.previewMessageId() != null && session.controlMessageId() != null) {
            event.getChannel().editMessageEmbedsById(session.previewMessageId(), session.current().build()).queue();
            event.deferEdit().queue();
            event.getChannel().editMessageComponentsById(session.controlMessageId())
                    .useComponentsV2()
                    .setComponents(buildComponentsV2(session))
                    .queue();
        }
    }

    public void updateInterface(ModalInteractionEvent event, EditingSession session) {
        if (session.previewMessageId() != null && session.controlMessageId() != null) {
            event.getChannel().editMessageEmbedsById(session.previewMessageId(), session.current().build()).queue();
            event.deferEdit().queue();
            event.getChannel().editMessageComponentsById(session.controlMessageId())
                    .useComponentsV2()
                    .setComponents(buildComponentsV2(session))
                    .queue();
        }
    }

    public TextChannel resolveBuilderChannel(SlashCommandInteractionEvent event) {
        Long builder = guildConfig.builderChannel(event.getGuild().getIdLong());
        if (builder != null) {
            return event.getGuild().getTextChannelById(builder);
        }
        return event.getChannel().getType() == ChannelType.TEXT
                ? event.getChannel().asTextChannel()
                : null;
    }

    public List<MessageTopLevelComponent> buildComponentsV2(EditingSession session) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        List<ContainerChildComponent> containerChildren = new ArrayList<>();

        String headerText = "## 🎨 Embed Builder #%d (%d embed%s)"
                .formatted(session.selectedIndex() + 1, session.size(), session.size() != 1 ? "s" : "");
        containerChildren.add(TextDisplay.of(headerText));
        containerChildren.add(Separator.createDivider(Separator.Spacing.SMALL));

        String modeDesc = switch (session.menuMode()) {
            case MAIN -> "*Edit title, description, color, and images*";
            case FIELDS -> "*Manage embed fields and layout*";
            case REORDER -> "*Add, remove, or reorder embeds*";
            case MISC -> "*Additional options and settings*";
        };
        containerChildren.add(TextDisplay.of(modeDesc));
        containerChildren.add(Separator.createDivider(Separator.Spacing.SMALL));

        if (session.size() > 1) {
            StringSelectMenu.Builder select = StringSelectMenu.create(prefix + "select");
            for (int i = 0; i < session.size(); i++) {
                select.addOption("Embed " + (i + 1), Integer.toString(i));
            }
            select.setDefaultValues(Integer.toString(session.selectedIndex()));
            select.setPlaceholder("Switch to another embed");
            containerChildren.add(ActionRow.of(select.build()));
        }

        StringSelectMenu menu = StringSelectMenu.create(prefix + "menu")
                .addOption("✏️ Main", MenuMode.MAIN.name(), "Edit titles, descriptions, colors")
                .addOption("📋 Fields", MenuMode.FIELDS.name(), "Manage embed fields")
                .addOption("🔀 Reorder", MenuMode.REORDER.name(), "Move and manage embeds")
                .addOption("⚙️ Misc", MenuMode.MISC.name(), "Additional options")
                .setDefaultValues(session.menuMode().name())
                .setPlaceholder("Choose editing mode")
                .build();
        containerChildren.add(ActionRow.of(menu));

        containerChildren.add(Separator.createDivider(Separator.Spacing.SMALL));
        addActionButtons(containerChildren, session);

        containerChildren.add(Separator.createDivider(Separator.Spacing.SMALL));
        containerChildren.add(ActionRow.of(
                Button.success(prefix + "publish", "Publish").withEmoji(Emoji.fromUnicode("📤")),
                Button.danger(prefix + "cancel", "Cancel").withEmoji(Emoji.fromUnicode("❌"))));

        components.add(Container.of(containerChildren).withAccentColor(Color.decode("#5865F2")));
        return components;
    }

    private void addActionButtons(List<ContainerChildComponent> children, EditingSession session) {
        switch (session.menuMode()) {
            case MAIN -> {
                children.add(ActionRow.of(
                        Button.primary(prefix + "title", "Title").withEmoji(Emoji.fromUnicode("📌")),
                        Button.primary(prefix + "desc", "Description").withEmoji(Emoji.fromUnicode("📄")),
                        Button.primary(prefix + "color", "Color").withEmoji(Emoji.fromUnicode("🎨"))));
                children.add(ActionRow.of(
                        Button.secondary(prefix + "images", "Images").withEmoji(Emoji.fromUnicode("🖼️")),
                        Button.secondary(prefix + "author", "Author").withEmoji(Emoji.fromUnicode("👤")),
                        Button.secondary(prefix + "footer", "Footer").withEmoji(Emoji.fromUnicode("🔖")),
                        Button.secondary(prefix + "timestamp", "Timestamp").withEmoji(Emoji.fromUnicode("⏱️"))));
            }
            case REORDER -> {
                boolean canMoveLeft = session.selectedIndex() > 0;
                boolean canMoveRight = session.selectedIndex() < session.size() - 1;
                boolean canDelete = session.size() > 1;

                children.add(ActionRow.of(
                        Button.secondary(prefix + "left", "Move Left")
                                .withEmoji(Emoji.fromUnicode("⬅️"))
                                .withDisabled(!canMoveLeft),
                        Button.secondary(prefix + "right", "Move Right")
                                .withEmoji(Emoji.fromUnicode("➡️"))
                                .withDisabled(!canMoveRight)));
                children.add(ActionRow.of(
                        Button.success(prefix + "add", "Add Embed").withEmoji(Emoji.fromUnicode("➕")),
                        Button.danger(prefix + "del", "Delete")
                                .withEmoji(Emoji.fromUnicode("🗑️"))
                                .withDisabled(!canDelete)));
            }
            case FIELDS -> {
                int fieldCount = session.current().build().getFields().size();
                children.add(TextDisplay.of("Fields: %d/25".formatted(fieldCount)));
                children.add(ActionRow.of(
                        Button.success(prefix + "field:add", "Add").withEmoji(Emoji.fromUnicode("➕")),
                        Button.primary(prefix + "field:edit", "Edit")
                                .withEmoji(Emoji.fromUnicode("✏️"))
                                .withDisabled(fieldCount == 0),
                        Button.danger(prefix + "field:delete", "Delete")
                                .withEmoji(Emoji.fromUnicode("🗑️"))
                                .withDisabled(fieldCount == 0)));
            }
            case MISC -> {
                children.add(ActionRow.of(
                        Button.primary(prefix + "paste_json", "Paste JSON").withEmoji(Emoji.fromUnicode("📋"))));
            }
        }
    }
}
