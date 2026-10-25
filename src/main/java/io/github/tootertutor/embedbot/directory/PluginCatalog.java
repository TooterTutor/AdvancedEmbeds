package io.github.tootertutor.embedbot.directory;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

/** Validated immutable catalog. A failed reload never replaces the live snapshot. */
public record PluginCatalog(List<Plugin> plugins) {
    public record Link(String label, String url) {}
    public record Plugin(String id, String name, String summary, String description,
                         String compatibility, List<Link> links) {}

    public PluginCatalog {
        if (plugins == null || plugins.isEmpty() || plugins.size() > 250)
            throw new IllegalArgumentException("Catalog must contain 1–250 plugins");
        var ids = new HashSet<String>();
        for (var p : plugins) {
            if (p == null || p.id() == null || !p.id().matches("[a-z0-9-]{1,40}") || !ids.add(p.id()))
                throw new IllegalArgumentException("Plugin IDs must be unique lowercase slugs (1–40 characters)");
            text(p.name(), 80, "name"); text(p.summary(), 100, "summary");
            text(p.description(), 1800, "description"); text(p.compatibility(), 200, "compatibility");
            if (p.links() == null || p.links().size() > 5) throw new IllegalArgumentException("Use 0–5 links per plugin");
            for (var link : p.links()) {
                if (link == null) throw new IllegalArgumentException("Null link");
                text(link.label(), 80, "link label"); text(link.url(), 512, "link URL");
                var uri = URI.create(link.url());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException("Links must be absolute HTTPS URLs without credentials");
            }
        }
        plugins = plugins.stream().map(p -> new Plugin(p.id(), p.name(), p.summary(), p.description(),
                p.compatibility(), List.copyOf(p.links()))).toList();
    }

    private static void text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException(field + " must contain 1–" + max + " characters");
    }

    public static PluginCatalog load(Path path) throws IOException {
        // Deserialize to a DTO: Gson does not consistently invoke record constructors on older runtimes.
        try (var reader = Files.newBufferedReader(path)) {
            var dto = new Gson().fromJson(reader, CatalogData.class);
            if (dto == null) throw new IllegalArgumentException("Empty catalog");
            return new PluginCatalog(dto.plugins);
        }
    }
    private static class CatalogData { List<Plugin> plugins; }
}
