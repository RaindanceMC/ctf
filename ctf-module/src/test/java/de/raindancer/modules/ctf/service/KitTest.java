package de.raindancer.modules.ctf.service;

import de.raindancer.core.social.team.TeamColour;
import de.raindancer.modules.ctf.model.Role;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("what the classes carry and get")
class KitTest {

    @Test
    @DisplayName("the archer's longbow: Power I, no flame, 24 arrows — strong at range, not a one-shot turret")
    void archer() {
        assertThat(RoleService.ARCHER_POWER).isEqualTo(1);
        assertThat(RoleService.ARCHER_FLAME).isFalse();
        assertThat(RoleService.ARCHER_ARROWS).isEqualTo(24);
    }

    @Test
    @DisplayName("blocks for building are the side's wool")
    void blocks() {
        assertThat(RoleService.blocksOf(TeamColour.RED)).isEqualTo(Material.RED_WOOL);
        assertThat(RoleService.blocksOf(TeamColour.BLUE)).isEqualTo(Material.BLUE_WOOL);
    }

    @Test
    @DisplayName("speed from a power-up: Speed IV for a scout, Speed II for everybody else")
    void speed() {
        assertThat(PowerUpService.speedAmplifier(Role.SCOUT)).isEqualTo(3);
        assertThat(PowerUpService.speedAmplifier(Role.SOLDIER)).isEqualTo(1);
        assertThat(PowerUpService.speedAmplifier(null)).isEqualTo(1);
    }
}
