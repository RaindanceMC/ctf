package de.raindancer.modules.ctf.store;

import de.raindancer.core.platform.log.Log;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.modules.ctf.model.Point;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The hub players land in: {@code hub/hub.yml} beside its schematic, the spawn relative to the schematic's lowest
 * corner. A plain file, like a map's — something copied in and edited by hand.
 */
public final class HubStore {

    private static final LogChannel log = Log.of("ctf");

    /** The hub as its file describes it. */
    public record Hub(String file, String title, String author, Point spawn) {
    }

    private final Path folder;

    public HubStore(Path folder) {
        this.folder = folder;
    }

    public Path folder() {
        return folder;
    }

    /** The hub, when its file names a schematic that is there and a spawn. */
    public Optional<Hub> load() {
        Path file = folder.resolve("hub.yml");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file));
        } catch (IOException | InvalidConfigurationException unreadable) {
            log.warn("hub/hub.yml cannot be read: {}", unreadable.getMessage());
            return Optional.empty();
        }
        String schematic = yaml.getString("file", "");
        Optional<Point> spawn = Point.parse(yaml.getString("spawn"));
        Path path = folder.resolve(schematic).normalize();
        if (schematic.isBlank() || !path.startsWith(folder.normalize()) || !Files.isRegularFile(path) || spawn.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Hub(schematic, yaml.getString("title", "Hub"), yaml.getString("author", ""),
                spawn.get()));
    }

    public Optional<Path> schematic() {
        return load().map(hub -> folder.resolve(hub.file()));
    }
}
