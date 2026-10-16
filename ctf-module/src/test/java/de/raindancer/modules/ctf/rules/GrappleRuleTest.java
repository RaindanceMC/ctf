package de.raindancer.modules.ctf.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the grappling hook is a jump, not wings")
class GrappleRuleTest {

    private final GrappleRule rule = new GrappleRule();

    @Test
    @DisplayName("one pull per jump: in the air again only after landing")
    void onePullPerJump() {
        assertThat(rule.mayPull(false, false, 10_000, 0)).as("the first pull may start in the air").isTrue();
        assertThat(rule.mayPull(false, true, 13_000, 0)).as("chained in the air").isFalse();
        assertThat(rule.mayPull(true, true, 13_000, 0)).as("landed").isTrue();
    }

    @Test
    @DisplayName("and a short wait between two pulls, even from the ground")
    void cooldown() {
        long ready = 10_000 + GrappleRule.COOLDOWN_MILLIS;
        assertThat(rule.mayPull(true, false, ready - 1, ready)).isFalse();
        assertThat(rule.mayPull(true, false, ready, ready)).isTrue();
    }

    @Test
    @DisplayName("only a hook stuck in a block or in somebody pulls — never one still flying")
    void onlyAStuckHook() {
        assertThat(GrappleRule.pulls(org.bukkit.event.player.PlayerFishEvent.State.IN_GROUND)).isTrue();
        assertThat(GrappleRule.pulls(org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_ENTITY)).isTrue();
        assertThat(GrappleRule.pulls(org.bukkit.event.player.PlayerFishEvent.State.REEL_IN)).isFalse();
        assertThat(GrappleRule.pulls(org.bukkit.event.player.PlayerFishEvent.State.FISHING)).isFalse();
    }
}
