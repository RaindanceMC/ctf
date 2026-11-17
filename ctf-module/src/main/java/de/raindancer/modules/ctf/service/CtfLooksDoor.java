package de.raindancer.modules.ctf.service;

import de.raindancer.core.ui.cosmetic.CosmeticDoor;
import de.raindancer.core.ui.menu.Icons;
import de.raindancer.core.ui.menu.Menu;
import de.raindancer.modules.ctf.CtfServices;
import de.raindancer.modules.ctf.screen.LooksMenu;
import de.raindancer.modules.ctf.store.ProfileStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

/** The flag trail and capture fireworks as a door on the server's cosmetics page, beside everything else worn. */
public final class CtfLooksDoor implements CosmeticDoor {

    private final Supplier<CtfServices> services;

    public CtfLooksDoor(Supplier<CtfServices> services) {
        this.services = services;
    }

    @Override
    public String id() {
        return "ctf:looks";
    }

    @Override
    public ItemStack icon(Player viewer) {
        ProfileStore.Profile profile = services.get().progress().profile(viewer.getUniqueId());
        return Icons.of(Material.FIREWORK_ROCKET, "<light_purple>Capture the Flag looks",
                "<gray>Your flag trail and capture fireworks,", "<gray>unlocked by your Capture the Flag level.",
                "<gray>Yours: <white>" + profile.trail().title() + "<gray>, <white>" + profile.show().title(),
                "", "<dark_gray>Click to pick.");
    }

    @Override
    public Optional<String> locked(Player viewer) {
        return Optional.empty();
    }

    @Override
    public void open(Player viewer, Menu parent) {
        new LooksMenu(services.get(), viewer, parent).open();
    }

    @Override
    public boolean wearing(Player viewer) {
        return services.get().progress().profile(viewer.getUniqueId()).dressed();
    }

    @Override
    public void takeOff(Player viewer) {
        services.get().progress().pick(viewer.getUniqueId(), ProfileStore.Profile::plain);
    }
}
