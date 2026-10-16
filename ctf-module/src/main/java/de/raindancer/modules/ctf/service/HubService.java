package de.raindancer.modules.ctf.service;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.world.build.SchematicWorld;
import de.raindancer.modules.ctf.CtfSettings;
import de.raindancer.modules.ctf.store.HubStore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The hub players land in: a schematic built once into an empty world of its own ({@code hub-world}), locked so nobody
 * but staff in creative changes it — RainsCore's {@link SchematicWorld}. Built again only when its schematic changes;
 * {@code hub-built.yml} remembers which version stands.
 */
public final class HubService implements ICtfService {

    /** The height the hub's lowest layer is built at; below it lies the void. */
    public static final int HUB_Y = SchematicWorld.BASE_Y;
    /** How far below the hub's lowest layer somebody has fallen off. */
    public static final int FALL = 12;

    private final Plugin plugin;
    private final LogChannel log;
    private final HubStore store;
    private final YamlStore builtFile;
    private volatile CtfSettings settings;
    private volatile SchematicWorld world;

    public HubService(Plugin plugin, LogChannel log, HubStore store, Path builtFile, CtfSettings settings) {
        this.plugin = plugin;
        this.log = log;
        this.store = store;
        this.builtFile = new YamlStore(builtFile);
        this.settings = settings;
        this.world = world(settings.hubWorld());
    }

    private SchematicWorld world(String name) {
        return new SchematicWorld(plugin, log, name, builtFile, ArenaWorldService::isStaffBuilding,
                ArenaWorldService::calm);
    }

    @Override
    public void settings(CtfSettings settings) {
        this.settings = settings;
        if (!world.worldName().equals(settings.hubWorld())) {
            world.unlock();
            world = world(settings.hubWorld());
        }
    }

    /** Where players land, once the hub stands; empty without a hub. */
    public Optional<Location> spawn() {
        return world.spawn();
    }

    public boolean isHubWorld(World candidate) {
        return world.isWorld(candidate);
    }

    /** Below this height somebody in the hub has fallen off it. */
    public int fallenBelow() {
        return HUB_Y - FALL;
    }

    public boolean isBuilding() {
        return world.isBuilding();
    }

    /**
     * Makes sure the hub stands: built when its schematic is new or changed, else only loaded.
     *
     * @param force build again even if the same version stands
     * @param done  told on the global thread whether there is a hub now
     */
    public void ensure(boolean force, java.util.function.Consumer<Boolean> done) {
        Scheduling.async(plugin, () -> {
            Optional<HubStore.Hub> hub = store.load();
            if (hub.isEmpty()) {
                Scheduling.global(plugin, () -> done.accept(false));
                return;
            }
            var at = hub.get().spawn();
            world.ensure(store.folder().resolve(hub.get().file()),
                    new SchematicWorld.Landing(at.x(), at.y(), at.z(), at.yaw(), at.pitch()), force,
                    landing -> done.accept(landing.isPresent()));
        });
    }

    public void unlock() {
        world.unlock();
    }
}
