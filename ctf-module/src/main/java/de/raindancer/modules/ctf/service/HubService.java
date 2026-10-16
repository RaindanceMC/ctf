package de.raindancer.modules.ctf.service;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.world.build.PasteJob;
import de.raindancer.core.world.build.Schematic;
import de.raindancer.core.world.manage.VoidGenerator;
import de.raindancer.core.world.protection.LockedWorlds;
import de.raindancer.core.world.safety.Spot;
import de.raindancer.modules.ctf.CtfSettings;
import de.raindancer.modules.ctf.store.HubStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The hub players land in: a schematic built once into an empty world of its own ({@code hub-world}), locked so nobody
 * but staff in creative changes it. Built again only when its schematic changes; {@code hub-built.yml} remembers which
 * version stands.
 */
public final class HubService implements ICtfService {

    /** The height the hub's lowest layer is built at; below it lies the void. */
    public static final int HUB_Y = 64;
    /** How far below the hub's lowest layer somebody has fallen off. */
    public static final int FALL = 12;

    private final Plugin plugin;
    private final LogChannel log;
    private final HubStore store;
    private final YamlStore builtFile;
    private volatile CtfSettings settings;
    private volatile Location spawn;
    private volatile boolean building;

    public HubService(Plugin plugin, LogChannel log, HubStore store, Path builtFile, CtfSettings settings) {
        this.plugin = plugin;
        this.log = log;
        this.store = store;
        this.builtFile = new YamlStore(builtFile);
        this.settings = settings;
    }

    @Override
    public void settings(CtfSettings settings) {
        this.settings = settings;
    }

    /** Where players land, once the hub stands; empty without a hub. */
    public Optional<Location> spawn() {
        return Optional.ofNullable(spawn);
    }

    public boolean isHubWorld(World world) {
        return world != null && world.getName().equals(settings.hubWorld());
    }

    /** Below this height somebody in the hub has fallen off it. */
    public int fallenBelow() {
        return HUB_Y - FALL;
    }

    public boolean isBuilding() {
        return building;
    }

    /**
     * Makes sure the hub stands: built when its schematic is new or changed, else only loaded. Reads the file and the
     * schematic off the server thread, builds on the regions.
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
            Path file = store.folder().resolve(hub.get().file());
            long version = version(file);
            boolean standing = !force && builtFile.read().getLong("version", -1) == version;
            Schematic schematic;
            try {
                schematic = Schematic.read(file);
            } catch (IOException unreadable) {
                log.error("The hub {} cannot be read: {}", hub.get().file(), unreadable.getMessage());
                Scheduling.global(plugin, () -> done.accept(false));
                return;
            }
            Scheduling.global(plugin, () -> {
                World world = world();
                if (world == null) {
                    done.accept(false);
                    return;
                }
                int originX = -schematic.width() / 2;
                int originZ = -schematic.length() / 2;
                var at = hub.get().spawn();
                Location landing = new Location(world, originX + at.x(), HUB_Y + at.y(), originZ + at.z(), at.yaw(),
                        at.pitch());
                if (standing) {
                    spawn = landing;
                    world.setSpawnLocation(landing);
                    done.accept(true);
                    return;
                }
                building = true;
                PasteJob job = new PasteJob(plugin, world,
                        schematic.placements(new Spot(world.getName(), originX, HUB_Y, originZ)));
                log.info("Building the hub {} into '{}': {} chunk(s).", hub.get().file(), world.getName(), job.chunks());
                job.start(blocks -> {
                    building = false;
                    spawn = landing;
                    world.setSpawnLocation(landing);
                    Scheduling.async(plugin, () -> builtFile.write(yaml -> yaml.set("version", version)));
                    log.info("The hub is built: {} block(s).", blocks);
                    done.accept(true);
                });
            });
        });
    }

    private World world() {
        String name = settings.hubWorld();
        World world = Bukkit.getWorld(name);
        if (world == null) {
            try {
                world = new WorldCreator(name).generator(VoidGenerator.empty()).type(WorldType.FLAT)
                        .generateStructures(false).environment(World.Environment.NORMAL).createWorld();
            } catch (RuntimeException refused) {
                log.error(refused, "The server refused to make the hub world '{}'.", name);
                return null;
            }
        }
        if (world != null) {
            if (!LockedWorlds.isLocked(name)) {
                LockedWorlds.lock(plugin, name, ArenaWorldService::isStaffBuilding);
            }
            ArenaWorldService.calm(world);
        }
        return world;
    }

    private static long version(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis() ^ Files.size(file);
        } catch (IOException missing) {
            return -2;
        }
    }

    public void unlock() {
        LockedWorlds.unlock(settings.hubWorld());
    }
}
