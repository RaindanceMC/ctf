package de.raindancer.modules.ctf.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("where a map is built in the arena world")
class SlotTest {

    @Test
    @DisplayName("the first slot nobody stands in and nobody stood in: a map built again never lands on its old blocks")
    void freshSlot() {
        assertThat(ArenaWorldService.freeSlot(List.of(), Set.of())).isZero();
        assertThat(ArenaWorldService.freeSlot(List.of(0, 1), Set.of())).isEqualTo(2);
        assertThat(ArenaWorldService.freeSlot(List.of(1), Set.of(0))).isEqualTo(2);
        assertThat(ArenaWorldService.freeSlot(List.of(), Set.of(0, 2))).isEqualTo(1);
    }

    @Test
    @DisplayName("an edited map keeps a copy of the file it was before")
    void backupName() {
        assertThat(ArenaWorldService.backupName("empirewar-a.schem", 1_792_190_000_000L))
                .isEqualTo("empirewar-a.schem.before-edit-1792190000000");
    }
}
