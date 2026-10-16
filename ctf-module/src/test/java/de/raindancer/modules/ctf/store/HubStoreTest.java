package de.raindancer.modules.ctf.store;

import de.raindancer.modules.ctf.model.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the hub's file")
class HubStoreTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("a hub is its schematic and its spawn, read from hub.yml")
    void reads() throws Exception {
        Files.writeString(folder.resolve("hub.yml"), String.join("\n",
                "file: castle.schem", "title: Castle Hub", "author: Somebody", "spawn: 40.5 12 33.5 180 0", ""));
        Files.writeString(folder.resolve("castle.schem"), "");
        HubStore.Hub hub = new HubStore(folder).load().orElseThrow();
        assertThat(hub.file()).isEqualTo("castle.schem");
        assertThat(hub.title()).isEqualTo("Castle Hub");
        assertThat(hub.spawn()).isEqualTo(new Point(40.5, 12, 33.5, 180f, 0f));
        assertThat(new HubStore(folder).schematic()).contains(folder.resolve("castle.schem"));
    }

    @Test
    @DisplayName("no hub.yml, no schematic or no spawn: no hub — the lobby stays where it was set")
    void absent() throws Exception {
        assertThat(new HubStore(folder).load()).isEmpty();
        Files.writeString(folder.resolve("hub.yml"), "file: gone.schem\nspawn: 1 2 3\n");
        assertThat(new HubStore(folder).load()).as("schematic missing").isEmpty();
        Files.writeString(folder.resolve("gone.schem"), "");
        Files.writeString(folder.resolve("hub.yml"), "file: gone.schem\n");
        assertThat(new HubStore(folder).load()).as("spawn missing").isEmpty();
    }

    @Test
    @DisplayName("the schematic named cannot lead out of the hub folder")
    void contained() throws Exception {
        Files.writeString(folder.resolve("hub.yml"), "file: ../secret.schem\nspawn: 1 2 3\n");
        assertThat(new HubStore(folder).load()).isEmpty();
    }
}
