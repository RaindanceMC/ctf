package de.raindancer.modules.ctf.listener;

import de.raindancer.core.data.runs.RunHistory;
import de.raindancer.core.world.movement.Parkour;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.effect.Cues;
import de.raindancer.modules.ctf.CtfServices;
import de.raindancer.modules.ctf.service.LobbyService;
import de.raindancer.modules.ctf.service.ProgressService;
import de.raindancer.modules.ctf.store.LobbyStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** The lobby's items, the parkour (start, checkpoints, finish, falling back) and the throwing range. */
public final class LobbyListener implements ICtfListener {

    private final CtfServices services;

    public LobbyListener(CtfServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<String> purpose = services.lobby().purposeOf(event.getItem());
        if (purpose.isEmpty() || !services.lobby().isInside(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        switch (purpose.get()) {
            case LobbyService.MENU -> services.screens().main(player);
            case LobbyService.VOTE -> services.screens().vote(player);
            case LobbyService.CLASS -> services.screens().classes(player);
            case LobbyService.LEAVE -> services.rounds().leave(player);
            case LobbyService.PRACTICE_FLAG -> practiceThrow(player);
            default -> {
            }
        }
    }

    private void practiceThrow(Player player) {
        LobbyStore.Place line = services.lobby().lobby().throwLine();
        Optional<Location> start = services.lobby().place(line);
        if (start.isEmpty() || !start.get().getWorld().equals(player.getWorld())
                || start.get().distanceSquared(player.getLocation()) > 9) {
            services.messages().send(player, "ctf.lobby.throw-from-line");
            return;
        }
        Location from = player.getEyeLocation();
        Location mark = start.get();
        services.flags().fly(from, from.getDirection().normalize().multiply(1.25).add(new org.bukkit.util.Vector(0, 0.2,
                0)), Material.WHITE_BANNER, mark.getY() - 40, -2, landed -> {
                    if (landed == null) {
                        services.messages().send(player, "ctf.lobby.throw-lost");
                        return;
                    }
                    double blocks = Math.hypot(landed.x() - mark.getX(), landed.z() - mark.getZ());
                    Scheduling.async(services.plugin(), () -> {
                        RunHistory.Standing standing = services.lobby().thrown(player, blocks);
                        services.messages().send(player, standing.newRecord() ? "ctf.lobby.throw-record"
                                : "ctf.lobby.throw-distance", "blocks", String.format(Locale.ROOT, "%.1f", blocks),
                                "rank", standing.rank());
                    });
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        // Off the edge of the hub: back to where everybody lands (a parkour run first goes back to its checkpoint).
        if (services.hub().isHubWorld(player.getWorld()) && event.getTo().getY() < services.hub().fallenBelow()
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            Optional<Location> back = services.lobby().parkourRespawn(id);
            player.teleportAsync(back.orElseGet(() -> services.lobby().spawn()));
            player.setFallDistance(0);
            return;
        }
        if (!services.config().get().parkour() || !services.lobby().isInside(id) || services.rounds().isIn(id)) {
            return;
        }
        boolean onFoot = player.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE && !player.isFlying() && !player.isGliding();
        switch (services.lobby().parkourMove(id, event.getTo(), onFoot)) {
            case Parkour.Step.Started started -> {
                services.messages().send(player, "ctf.parkour.started");
                services.core().effects().play(id, Cues.COUNTDOWN_DONE);
            }
            case Parkour.Step.Checkpoint checkpoint -> {
                services.messages().send(player, "ctf.parkour.checkpoint", "number", checkpoint.number());
                services.core().effects().play(id, Cues.OK);
            }
            case Parkour.Step.MissedCheckpoint missed -> services.messages().send(player, "ctf.parkour.missed-checkpoint");
            case Parkour.Step.FellOff fell -> services.lobby().parkourRespawn(id).ifPresent(player::teleportAsync);
            case Parkour.Step.Finished finished -> {
                Scheduling.async(services.plugin(), () -> {
                    RunHistory.Standing standing = services.lobby().recordParkour(player, finished.startedAt(),
                            finished.tookMillis());
                    services.messages().send(player, standing.newRecord() ? "ctf.parkour.record" : "ctf.parkour.finished",
                            "time", Parkour.time(finished.tookMillis()), "rank", standing.rank());
                });
                services.progress().award(id, ProgressService.PARKOUR);
                services.core().effects().play(id, Cues.EARNED);
            }
            default -> {
            }
        }
    }

}
