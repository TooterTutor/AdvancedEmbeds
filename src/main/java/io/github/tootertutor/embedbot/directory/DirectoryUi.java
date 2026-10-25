package io.github.tootertutor.embedbot.directory;

import java.util.ArrayList;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;

public final class DirectoryUi {
    public static final String PREFIX = "tw:dir:v1:";
    public static final int PAGE_SIZE = 25;
    private DirectoryUi() {}

    public static net.dv8tion.jda.api.utils.data.DataObject validate(Container container) {
        try (var data = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .useComponentsV2().setComponents(container).build()) {
            return data.toData();
        }
    }

    public static Container landing(PluginCatalog catalog) {
        return Container.of(TextDisplay.of("# TooterWorks Plugin Directory\nExplore our Minecraft plugins, find downloads, and get support.\n\n**"
                + catalog.plugins().size() + " plugin(s)** available. Open your own browser below."),
                ActionRow.of(Button.primary(PREFIX + "open", "Browse plugins")));
    }

    public static Container browse(PluginCatalog catalog, int requestedPage, String selected) {
        int pages = (catalog.plugins().size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        var children = new ArrayList<ContainerChildComponent>();
        children.add(TextDisplay.of("# TooterWorks Plugins\nChoose a plugin to view details and useful links.\n-# Page " + (page + 1) + "/" + pages));
        var menu = StringSelectMenu.create(PREFIX + "select:" + page).setPlaceholder("Choose a plugin");
        var entries = catalog.plugins().subList(page * PAGE_SIZE, Math.min((page + 1) * PAGE_SIZE, catalog.plugins().size()));
        for (var p : entries) menu.addOption(p.name(), p.id(), p.summary());
        children.add(ActionRow.of(menu.build()));
        children.add(ActionRow.of(Button.secondary(PREFIX + "page:" + (page - 1), "Previous").withDisabled(page == 0),
                Button.secondary(PREFIX + "page:" + (page + 1), "Next").withDisabled(page == pages - 1),
                Button.secondary(PREFIX + "page:0", "Home")));
        if (selected != null) {
            var plugin = catalog.plugins().stream().filter(p -> p.id().equals(selected)).findFirst();
            if (plugin.isEmpty()) children.add(TextDisplay.of("That plugin is no longer listed. Please choose another."));
            else {
                var p = plugin.get();
                children.add(TextDisplay.of("## " + p.name() + "\n" + p.description() + "\n\n**Compatibility:** " + p.compatibility()));
                if (!p.links().isEmpty()) children.add(ActionRow.of(p.links().stream().map(l -> Button.link(l.url(), l.label())).toList()));
            }
        }
        return Container.of(children);
    }
}
