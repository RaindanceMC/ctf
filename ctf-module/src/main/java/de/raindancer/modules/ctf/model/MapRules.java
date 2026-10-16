package de.raindancer.modules.ctf.model;

import java.util.Locale;
import java.util.Optional;

/**
 * What a map itself asks for, over the server's settings: a building rule (a map like The Wall cannot be crossed
 * without building) and how many blocks every player gets while building is on there.
 */
public record MapRules(Optional<Building> building, int blocks) {

    public static final MapRules NONE = new MapRules(Optional.empty(), 0);

    public MapRules {
        building = building == null ? Optional.empty() : building;
        blocks = Math.max(0, Math.min(blocks, 256));
    }

    /** The map's rule if it has one, else the server's. */
    public Building building(Building server) {
        return building.orElse(server);
    }

    public static Optional<Building> read(String written) {
        if (written == null || written.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Building.valueOf(written.strip().toUpperCase(Locale.ROOT).replace('-', '_')));
        } catch (IllegalArgumentException unknown) {
            return Optional.empty();
        }
    }
}
