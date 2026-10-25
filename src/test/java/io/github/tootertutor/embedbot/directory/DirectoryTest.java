package io.github.tootertutor.embedbot.directory;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

class DirectoryTest {
    @TempDir Path temp;
    PluginCatalog.Plugin plugin(String id) {
        return new PluginCatalog.Plugin(id, "Plugin " + id, "Summary", "Description", "Paper",
                List.of(new PluginCatalog.Link("Source", "https://github.com/TooterTutor/ModularPacks")));
    }
    PluginCatalog catalog(int count) {
        var plugins = new ArrayList<PluginCatalog.Plugin>();
        for (int i = 0; i < count; i++) plugins.add(plugin("plugin-" + i));
        return new PluginCatalog(plugins);
    }
    @Test void bundledCatalogLoadsAndSerializes() throws Exception {
        var path = temp.resolve("catalog.json");
        try (var in = getClass().getResourceAsStream("/plugin-directory.example.json")) { Files.copy(in, path); }
        var catalog = PluginCatalog.load(path);
        assertEquals("modularpacks", catalog.plugins().get(0).id());
        assertTrue(DirectoryUi.validate(DirectoryUi.browse(catalog, 0, "modularpacks")).toString().contains("Downloads"));
        assertTrue(DirectoryUi.validate(DirectoryUi.landing(catalog)).toString().contains(DirectoryUi.PREFIX + "open"));
    }
    @Test void paginationRespectsTwentyFiveOptionsAndClampsStalePages() {
        var catalog = catalog(26);
        var first = DirectoryUi.validate(DirectoryUi.browse(catalog, 0, null)).toString();
        var last = DirectoryUi.validate(DirectoryUi.browse(catalog, 999, null)).toString();
        assertTrue(first.contains("plugin-24")); assertFalse(first.contains("plugin-25"));
        assertTrue(last.contains("plugin-25")); assertFalse(last.contains("plugin-24"));
        assertEquals(DirectoryUi.validate(DirectoryUi.browse(catalog, 0, null)).toString(), DirectoryUi.validate(DirectoryUi.browse(catalog, -8, null)).toString());
    }
    @Test void deletedPluginIsRecoverable() {
        assertTrue(DirectoryUi.validate(DirectoryUi.browse(catalog(1), 0, "removed")).toString().contains("no longer listed"));
    }
    @Test void validationRejectsInvalidCatalogs() {
        assertThrows(IllegalArgumentException.class, () -> new PluginCatalog(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PluginCatalog(List.of(plugin("same"), plugin("same"))));
        assertThrows(IllegalArgumentException.class, () -> new PluginCatalog(List.of(plugin("Bad ID"))));
        assertThrows(IllegalArgumentException.class, () -> catalog(251));
        for (String url : List.of("http://example.com", "javascript:alert(1)", "https://user:password@example.com"))
            assertThrows(IllegalArgumentException.class, () -> new PluginCatalog(List.of(new PluginCatalog.Plugin("test", "Test", "Summary", "Description", "Paper", List.of(new PluginCatalog.Link("Link", url))))));
    }
    @Test void snapshotsAreImmutable() {
        var plugins = new ArrayList<>(List.of(plugin("test")));
        var catalog = new PluginCatalog(plugins); plugins.clear();
        assertEquals(1, catalog.plugins().size());
        assertThrows(UnsupportedOperationException.class, () -> catalog.plugins().clear());
    }
    @Test void maximumMetadataStillSerializes() {
        var links = new ArrayList<PluginCatalog.Link>();
        for (int i = 0; i < 5; i++) links.add(new PluginCatalog.Link("L".repeat(80), "https://example.com/" + "a".repeat(490)));
        var p = new PluginCatalog.Plugin("test", "N".repeat(80), "S".repeat(100), "D".repeat(1800), "C".repeat(200), links);
        assertDoesNotThrow(() -> DirectoryUi.validate(DirectoryUi.browse(new PluginCatalog(List.of(p)), 0, "test")));
    }
    @Test void freshControllerHandlesPublishedButtonWithoutSession() throws Exception {
        Path path = temp.resolve("catalog.json");
        new DirectoryController(path);
        var restarted = new DirectoryController(path);
        var event = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
        when(event.getComponentId()).thenReturn(DirectoryUi.PREFIX + "open");
        restarted.onButtonInteraction(event);
        verify(event).replyComponents(any(net.dv8tion.jda.api.components.MessageTopLevelComponent.class));
    }
    @Test void unrelatedEventsAreIgnored() throws Exception {
        var controller = new DirectoryController(temp.resolve("catalog.json"));
        var button = mock(ButtonInteractionEvent.class); when(button.getComponentId()).thenReturn("eb:v2:test");
        controller.onButtonInteraction(button); verify(button, never()).getMessage();
        var select = mock(StringSelectInteractionEvent.class); when(select.getComponentId()).thenReturn("eb:v2:select");
        controller.onStringSelectInteraction(select); verify(select, never()).getValues();
    }
    @Test void nonAdminCannotPublishOrReload() throws Exception {
        var controller = new DirectoryController(temp.resolve("catalog.json"));
        for (String action : List.of("publish", "update", "reload")) {
            var event = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
            when(event.getName()).thenReturn("directory"); when(event.isFromGuild()).thenReturn(true);
            when(event.getSubcommandName()).thenReturn(action);
            controller.onSlashCommandInteraction(event);
            verify(event).reply("Only server administrators can publish, update, or reload the directory.");
            verify(event, never()).deferReply(true);
        }
    }
    @Test void reloadRetainsSnapshotOnFailureAndAcceptsRepairedMetadata() throws Exception {
        var path = temp.resolve("reload.json");
        var store = new CatalogStore(path);
        var initial = store.current();
        var good = Files.readString(path);
        Files.writeString(path, "{broken");
        assertThrows(RuntimeException.class, store::reload);
        assertSame(initial, store.current());
        Files.writeString(path, good.replace("ModularPacks", "UpdatedPacks"));
        store.reload();
        assertEquals("UpdatedPacks", store.current().plugins().get(0).name());
        assertEquals("ModularPacks", initial.plugins().get(0).name());
    }
    @Test void malformedDiskCatalogFailsWithoutOverwritingIt() throws Exception {
        var path = temp.resolve("catalog.json"); Files.writeString(path, "{broken");
        assertThrows(RuntimeException.class, () -> new DirectoryController(path));
        assertEquals("{broken", Files.readString(path));
    }
}
