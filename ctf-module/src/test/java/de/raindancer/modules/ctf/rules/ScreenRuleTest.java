package de.raindancer.modules.ctf.rules;

import de.raindancer.core.world.time.Times;
import de.raindancer.modules.ctf.model.GameType;
import de.raindancer.modules.ctf.model.Phase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("What the Capture the Flag screens say")
class ScreenRuleTest {

    private final ScreenRule rule = new ScreenRule();

    @Test
    @DisplayName("the board lists the most captures coloured by place, then the week's best")
    void board() {
        String text = rule.board(List.of(new ScreenRule.Entry("Ada", 12), new ScreenRule.Entry("Bo", 7),
                new ScreenRule.Entry("Cy", 3), new ScreenRule.Entry("Di", 2)), List.of(new ScreenRule.Entry("Bo", 4)));
        assertThat(text).startsWith("<red><bold>⚑ CAPTURE THE FLAG ⚑</bold></red>\n<gray>Most captures\n");
        assertThat(text).contains("<gold>① <white>Ada <gray>— <yellow>12")
                .contains("<gray>② <white>Bo <gray>— <yellow>7")
                .contains("<#cd7f32>③ <white>Cy <gray>— <yellow>3")
                .contains("<white>④ <white>Di <gray>— <yellow>2")
                .contains("<aqua>This week\n<gold>① <white>Bo <gray>— <yellow>4");
        assertThat(text).doesNotEndWith("\n");
    }

    @Test
    @DisplayName("an empty board invites somebody to be first, and a name cannot smuggle in markup")
    void emptyAndEscaped() {
        assertThat(rule.board(List.of(), List.of())).contains("Nobody yet").doesNotContain("This week");
        assertThat(rule.board(List.of(new ScreenRule.Entry("<click:run_command:/op me>", 1)), List.of()))
                .contains("\\<click:run_command:/op me>");
    }

    @Test
    @DisplayName("somebody who never played is pointed to /ctf")
    void neverPlayed() {
        assertThat(rule.you(ScreenRule.Own.NONE)).contains("Your CTF record").contains("/ctf");
    }

    @Test
    @DisplayName("a record shows level, wins, captures, the kill ratio and the day's challenges")
    void record() {
        ScreenRule.Own own = new ScreenRule.Own(true, 10, 6, 14, 30, 12, 2, 4, 150, true, 150, 1, 3);
        String text = rule.you(own);
        assertThat(text).contains("<gray>Level <white>4").contains("150 xp to go")
                .contains("<gray>Wins <white>6<dark_gray>/<white>10")
                .contains("<gray>Captures <white>14")
                .contains("<gray>Kills <white>30 <dark_gray>· <gray>K/D <white>2.50")
                .contains("<gray>MVPs <white>2")
                .contains("<gray>Challenges today <white>1<dark_gray>/<white>3");
    }

    @Test
    @DisplayName("no level line while levels are off, no K/D without deaths, no challenge line without challenges")
    void leftOut() {
        String text = rule.you(new ScreenRule.Own(true, 1, 0, 0, 5, 0, 0, 0, 0, false, 0, 0, 0));
        assertThat(text).doesNotContain("Level").doesNotContain("Challenges").contains("K/D <white>5.00");
    }

    @Test
    @DisplayName("while no round runs, the lobby is described: who is waiting, the countdown, the build")
    void lobby() {
        assertThat(rule.round(ScreenRule.Glance.lobby(Phase.WAITING, 1, 4, 0)))
                .contains("<gold><bold>Capture the Flag</bold></gold>").contains("Waiting for players")
                .contains("<white>1</white><dark_gray>/<white>4");
        assertThat(rule.round(ScreenRule.Glance.lobby(Phase.COUNTDOWN, 5, 4, 23))).contains("Votes open")
                .contains("starts in <white>23 s");
        assertThat(rule.round(ScreenRule.Glance.lobby(Phase.PREPARING, 5, 4, 0))).contains("Building the map");
    }

    @Test
    @DisplayName("a running round shows the map, the type, the score of each side, the time left and the players")
    void running() {
        ScreenRule.Glance glance = new ScreenRule.Glance(Phase.RUNNING, "Canyon", GameType.CLASSIC.title(),
                List.of("<red>Red</red>", "<blue>Blue</blue>"), new int[] {2, 1}, 125_000, 8, 0, 0, 0);
        assertThat(rule.round(glance)).contains("<gray>Map <white>Canyon <dark_gray>· <white>Classic")
                .contains("<red>Red</red> <white>2 <dark_gray>: <white>1 <blue>Blue</blue>")
                .contains("<gray>Time left <white>" + Times.brief(Duration.ofMillis(125_000))).contains("<gray>Players <white>8");
    }

    @Test
    @DisplayName("overtime says so instead of a clock, a finished round says it is over")
    void overtimeAndOver() {
        ScreenRule.Glance overtime = new ScreenRule.Glance(Phase.OVERTIME, "Canyon", "Classic",
                List.of("Red", "Blue"), new int[] {1, 1}, 0, 6, 0, 0, 0);
        assertThat(rule.round(overtime)).contains("Overtime").doesNotContain("Time left");
        ScreenRule.Glance over = new ScreenRule.Glance(Phase.OVER, "Canyon", "Classic",
                List.of("Red", "Blue"), new int[] {3, 1}, 0, 6, 0, 0, 0);
        assertThat(rule.round(over)).contains("Round over");
    }
}
