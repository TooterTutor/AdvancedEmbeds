package io.github.tootertutor.embedbot.directory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.LoggerFactory;

/** Stateless routing: public controls survive restarts without an editing session. */
public final class DirectoryController extends ListenerAdapter {
    private final CatalogStore store;

    public DirectoryController() throws IOException {
        this(Path.of(System.getProperty("tooterworks.catalog", "plugin-directory.json")));
    }

    DirectoryController(Path path) throws IOException {
        store = new CatalogStore(path);
    }

    public static CommandData command() {
        return Commands.slash("directory", "Browse and manage the TooterWorks plugin directory").setContexts(net.dv8tion.jda.api.interactions.InteractionContextType.GUILD)
                .addSubcommands(new SubcommandData("browse", "Browse plugins privately"),
                        new SubcommandData("reload", "Admin: validate and reload the catalog from disk"),
                        new SubcommandData("publish", "Admin: post a new directory landing message")
                                .addOptions(channelOption()),
                        new SubcommandData("update", "Admin: refresh an existing directory landing message")
                                .addOptions(channelOption(), new OptionData(OptionType.STRING, "message", "Directory message ID", true)));
    }

    private static OptionData channelOption() {
        return new OptionData(OptionType.CHANNEL, "channel", "Directory text channel", true).setChannelTypes(ChannelType.TEXT);
    }

    @Override public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("directory")) return;
        if (!event.isFromGuild()) { event.reply("Use this command in a server.").setEphemeral(true).queue(); return; }
        if ("browse".equals(event.getSubcommandName())) {
            event.replyComponents(DirectoryUi.browse(store.current(), 0, null)).useComponentsV2().setEphemeral(true)
                    .setAllowedMentions(List.of()).queue();
            return;
        }
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Only server administrators can publish, update, or reload the directory.").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue(hook -> {
            try {
                switch (event.getSubcommandName()) {
                    case "reload" -> {
                        var next = store.reload();
                        hook.editOriginal("Loaded " + next.plugins().size() + " plugins. Published browse buttons now use this catalog; use /directory update to refresh landing counts.").queue();
                    }
                    case "publish", "update" -> {
                        var channel = event.getOption("channel").getAsChannel().asTextChannel();
                        if (!event.getMember().hasPermission(channel, Permission.VIEW_CHANNEL)
                                || !event.getGuild().getSelfMember().hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)) {
                            hook.editOriginal("You and the bot need access to the destination channel; the bot also needs Send Messages.").queue();
                            return;
                        }
                        if ("publish".equals(event.getSubcommandName())) {
                            channel.sendMessageComponents(DirectoryUi.landing(store.current())).useComponentsV2().setAllowedMentions(List.of())
                                    .queue(message -> hook.editOriginal("Directory published: " + message.getJumpUrl()).queue(),
                                            error -> failure(event, error));
                        } else {
                            String id = event.getOption("message").getAsString();
                            if (!id.matches("[0-9]{1,20}")) { hook.editOriginal("Enter a numeric Discord message ID.").queue(); return; }
                            channel.retrieveMessageById(id).queue(message -> {
                                boolean directory = message.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())
                                        && message.getComponentTree().findAll(net.dv8tion.jda.api.components.buttons.Button.class).stream().anyMatch(b -> (DirectoryUi.PREFIX + "open").equals(b.getCustomId()));
                                if (!directory) { hook.editOriginal("That is not a directory landing message from this bot.").queue(); return; }
                                message.editMessageComponents(DirectoryUi.landing(store.current())).useComponentsV2().setAllowedMentions(List.of())
                                        .queue(updated -> hook.editOriginal("Directory updated: " + updated.getJumpUrl()).queue(), error -> failure(event, error));
                            }, error -> failure(event, error));
                        }
                    }
                    default -> hook.editOriginal("Unknown directory command.").queue();
                }
            } catch (Exception error) { failure(event, error); }
        });
    }

    private void failure(SlashCommandInteractionEvent event, Throwable error) {
        LoggerFactory.getLogger(DirectoryController.class).warn("Directory operation failed", error);
        event.getHook().editOriginal("Directory operation failed. Check the catalog and bot logs, message ID, and channel permissions. The active catalog is kept on a failed reload.").queue();
    }

    @Override public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(DirectoryUi.PREFIX)) return;
        try {
            String action = id.substring(DirectoryUi.PREFIX.length());
            if (!action.equals("open") && !action.matches("page:-?[0-9]{1,6}")) throw new IllegalArgumentException("Unknown control");
            int page = action.equals("open") ? 0 : Integer.parseInt(action.substring("page:".length()));
            var ui = DirectoryUi.browse(store.current(), page, null);
            if (action.equals("open") || !event.getMessage().isEphemeral())
                event.replyComponents(ui).useComponentsV2().setEphemeral(true).setAllowedMentions(List.of()).queue();
            else event.editComponents(ui).useComponentsV2().setAllowedMentions(List.of()).queue();
        } catch (RuntimeException error) { event.reply("This control is outdated. Use /directory browse.").setEphemeral(true).queue(); }
    }

    @Override public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith(DirectoryUi.PREFIX + "select:")) return;
        try {
            int page = Integer.parseInt(event.getComponentId().substring((DirectoryUi.PREFIX + "select:").length()));
            var ui = DirectoryUi.browse(store.current(), page, event.getValues().get(0));
            if (event.getMessage().isEphemeral()) event.editComponents(ui).useComponentsV2().setAllowedMentions(List.of()).queue();
            else event.replyComponents(ui).useComponentsV2().setEphemeral(true).setAllowedMentions(List.of()).queue();
        } catch (RuntimeException error) { event.reply("This control is outdated. Use /directory browse.").setEphemeral(true).queue(); }
    }
}
