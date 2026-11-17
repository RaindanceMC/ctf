package de.raindancer.modules.ctf.service;

import de.raindancer.modules.ctf.CtfSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CtfLooksDoorTest {

    @Test
    @DisplayName("the looks are on /cosmetics only on a server of Capture the Flag's own, not on an SMP that also has it")
    void onlyOnItsOwnServer() {
        assertThat(CtfSettings.DEFAULTS.lobbyOnJoin()).as("the default is a server that also has it").isFalse();
        assertThat(CtfLooksDoor.onCosmeticsPage(CtfSettings.DEFAULTS)).isFalse();
        CtfSettings ownServer = org.mockito.Mockito.mock(CtfSettings.class);
        org.mockito.Mockito.when(ownServer.lobbyOnJoin()).thenReturn(true);
        assertThat(CtfLooksDoor.onCosmeticsPage(ownServer)).isTrue();
    }
}
