package de.raindancer.modules.ctf.service;

import de.raindancer.core.ui.hologram.ScreenKind;
import de.raindancer.core.ui.hologram.Screens;
import de.raindancer.modules.ctf.model.Challenge;
import de.raindancer.modules.ctf.model.Stats;
import de.raindancer.modules.ctf.rules.LevelRule;
import de.raindancer.modules.ctf.rules.ScreenRule;
import de.raindancer.modules.ctf.store.StatsStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Capture the Flag's screens for Core's {@code /screen}: everybody's own record, the board, and where the round is.
 * The records come from the database, so each is kept a little while instead of read every two seconds.
 */
public final class CtfScreens {

    public static final List<String> KINDS = List.of("ctf-you", "ctf-top", "ctf-round");
    /** The board's text is the one of the floating boards, so both always agree. */
    public static final int BOARD_SHOWN = 5;
    private static final long KEPT_MILLIS = 20_000;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private record Kept(long at, ScreenRule.Own own) {
    }

    private final StatsStore stats;
    private final ProgressService progress;
    private final RoundService rounds;
    private final LongSupplier clock;
    private final ScreenRule rule = new ScreenRule();
    private final LevelRule levels = new LevelRule();
    private final Map<UUID, Kept> kept = new ConcurrentHashMap<>();

    public CtfScreens(StatsStore stats, ProgressService progress, RoundService rounds, LongSupplier clock) {
        this.stats = stats;
        this.progress = progress;
        this.rounds = rounds;
        this.clock = clock;
    }

    public void register(Screens screens, Plugin owner) {
        screens.register(owner, ScreenKind.personal("ctf-you", "Your CTF record",
                "Everybody's own level, wins, captures, kills and challenges of the day.", Material.RED_BANNER,
                viewer -> MINI.deserialize(rule.you(own(viewer.id())))));
        screens.register(owner, ScreenKind.everyone("ctf-top", "CTF leaderboard",
                "The most captures, and the best of this week.", Material.GOLDEN_HELMET, () -> board()));
        screens.register(owner, ScreenKind.everyone("ctf-round", "CTF round",
                "The round that is running — map, score, time left — or the lobby.", Material.WHITE_BANNER,
                () -> MINI.deserialize(rule.round(rounds.glance()))));
    }

    /** The text of the leaderboard, for the screen and for the floating boards alike. */
    public Component board() {
        List<ScreenRule.Entry> best = stats.top(StatsStore.Board.CAPTURES, BOARD_SHOWN).stream()
                .map(each -> new ScreenRule.Entry(each.name(), each.captures())).toList();
        List<ScreenRule.Entry> week = stats.week(LocalDate.now().toEpochDay() / 7, 3).stream()
                .map(each -> new ScreenRule.Entry(each.name(), each.captures())).toList();
        return MINI.deserialize(rule.board(best, week));
    }

    private ScreenRule.Own own(UUID player) {
        long now = clock.getAsLong();
        Kept held = kept.get(player);
        if (held != null && now - held.at() < KEPT_MILLIS) {
            return held.own();
        }
        kept.values().removeIf(old -> now - old.at() >= KEPT_MILLIS);
        ScreenRule.Own read = read(player);
        kept.put(player, new Kept(now, read));
        return read;
    }

    private ScreenRule.Own read(UUID player) {
        Stats record = stats.of(player).orElse(null);
        long xp = Math.max(record == null ? 0 : record.xp(), progress.xp(player));
        int level = levels.level(xp);
        List<Challenge> today = progress.todays();
        Map<Challenge, Integer> done = today.isEmpty() ? Map.of() : progress.challengesOf(player);
        int finished = (int) today.stream().filter(each -> done.getOrDefault(each, 0) >= each.goal()).count();
        boolean levelsOn = progress.levelsOn();
        long toNext = Math.max(0, levels.xpFor(level + 1) - xp);
        if (record == null) {
            return new ScreenRule.Own(false, 0, 0, 0, 0, 0, 0, level, xp, levelsOn, toNext, finished, today.size());
        }
        return new ScreenRule.Own(true, record.rounds(), record.wins(), record.captures(), record.kills(),
                record.deaths(), record.mvps(), level, xp, levelsOn, toNext, finished, today.size());
    }
}
