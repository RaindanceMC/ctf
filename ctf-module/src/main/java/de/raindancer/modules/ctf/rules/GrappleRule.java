package de.raindancer.modules.ctf.rules;

import org.bukkit.event.player.PlayerFishEvent;

/**
 * When the grappling hook pulls: only a hook stuck in a block or in somebody (never one still in the air), once per
 * jump — in the air again only after landing — and not twice within {@link #COOLDOWN_MILLIS}. Chained pulls were
 * flying.
 */
public final class GrappleRule implements ICtfRule {

    public static final long COOLDOWN_MILLIS = 2_000;

    public static boolean pulls(PlayerFishEvent.State state) {
        return state == PlayerFishEvent.State.IN_GROUND || state == PlayerFishEvent.State.CAUGHT_ENTITY;
    }

    /**
     * @param pulledSinceLanding a pull already happened since this player last stood on the ground
     * @param readyAt            when the last pull's wait is over
     */
    public boolean mayPull(boolean onGround, boolean pulledSinceLanding, long now, long readyAt) {
        return now >= readyAt && (onGround || !pulledSinceLanding);
    }
}
