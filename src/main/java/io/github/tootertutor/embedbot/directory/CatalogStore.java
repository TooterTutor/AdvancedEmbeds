package io.github.tootertutor.embedbot.directory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Owns disk loading and atomically publishes fully validated immutable snapshots. */
final class CatalogStore {
    private final Path path;
    private volatile PluginCatalog current;

    CatalogStore(Path path) throws IOException {
        this.path = path;
        if (Files.notExists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (var resource = CatalogStore.class.getResourceAsStream("/plugin-directory.example.json")) {
                if (resource == null) throw new IOException("Missing bundled directory catalog");
                Files.copy(resource, path);
            }
        }
        reload();
    }

    PluginCatalog current() { return current; }

    synchronized PluginCatalog reload() throws IOException {
        var next = PluginCatalog.load(path);
        DirectoryUi.validate(DirectoryUi.landing(next));
        for (int i = 0; i < next.plugins().size(); i++)
            DirectoryUi.validate(DirectoryUi.browse(next, i / DirectoryUi.PAGE_SIZE, next.plugins().get(i).id()));
        current = next;
        return next;
    }
}
