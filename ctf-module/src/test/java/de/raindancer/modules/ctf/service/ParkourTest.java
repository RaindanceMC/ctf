package de.raindancer.modules.ctf.service;

import de.raindancer.core.world.movement.Parkour;
import de.raindancer.modules.ctf.model.Point;
import de.raindancer.modules.ctf.store.LobbyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("the lobby's parkour is run on RainsCore's Parkour")
class ParkourTest {

    @Test
    @DisplayName("start, checkpoints and finish become the course")
    void course() {
        LobbyStore.Place start = new LobbyStore.Place("ctf_hub", Point.of(1.5, 84, 2.5));
        LobbyStore.Place check = new LobbyStore.Place("ctf_hub", Point.of(5.5, 90, 2.5));
        LobbyStore.Place finish = new LobbyStore.Place("ctf_hub", Point.of(9.5, 100, 2.5));
        Parkour.Course course = LobbyService.course(new LobbyStore.Lobby(null, start, List.of(check), finish, null));
        assertThat(course.start()).isEqualTo(new Parkour.Spot("ctf_hub", 1.5, 84, 2.5));
        assertThat(course.checkpoints()).containsExactly(new Parkour.Spot("ctf_hub", 5.5, 90, 2.5));
        assertThat(course.finish()).isEqualTo(new Parkour.Spot("ctf_hub", 9.5, 100, 2.5));
    }

    @Test
    @DisplayName("without a start or a finish there is nothing to run")
    void incomplete() {
        assertThat(LobbyService.course(LobbyStore.Lobby.empty()).isComplete()).isFalse();
    }
}
