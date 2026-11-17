package de.raindancer.modules.ctf.store;

import de.raindancer.modules.ctf.model.CaptureShow;
import de.raindancer.modules.ctf.model.Role;
import de.raindancer.modules.ctf.model.Trail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileLooksTest {

    private static Trail anotherTrail() {
        return Arrays.stream(Trail.values()).filter(trail -> trail != Trail.TEAM).findFirst().orElseThrow();
    }

    private static CaptureShow anotherShow() {
        return Arrays.stream(CaptureShow.values()).filter(show -> show != CaptureShow.BURST).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("the defaults are not dressed up; a picked trail or firework is")
    void dressed() {
        assertThat(ProfileStore.Profile.defaults().dressed()).isFalse();
        assertThat(new ProfileStore.Profile(anotherTrail(), CaptureShow.BURST, null).dressed()).isTrue();
        assertThat(new ProfileStore.Profile(Trail.TEAM, anotherShow(), null).dressed()).isTrue();
    }

    @Test
    @DisplayName("taking the looks off goes back to the defaults and keeps the class they last played")
    void plain() {
        Role role = Role.values()[0];
        ProfileStore.Profile plain = new ProfileStore.Profile(anotherTrail(), anotherShow(), role).plain();
        assertThat(plain.dressed()).isFalse();
        assertThat(plain.lastClass()).isEqualTo(role);
    }
}
