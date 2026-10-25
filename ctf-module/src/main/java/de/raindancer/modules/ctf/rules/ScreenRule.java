package de.raindancer.modules.ctf.rules;

import de.raindancer.core.world.time.Times;
import de.raindancer.modules.ctf.model.Phase;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** What the Capture the Flag screens say: the board, somebody's own record, where the round is. */
public final class ScreenRule implements ICtfRule {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String[] PLACES = {"<gold>①", "<gray>②", "<#cd7f32>③", "<white>④", "<white>⑤"};

    /** One line of a board. */
    public record Entry(String name, int value) {
    }

    /**
     * Somebody's record.
     *
     * @param levels  whether levels are on at all
     * @param toNext  experience to the next level
     * @param of      how many challenges there are today; none means no line about them
     */
    public record Own(boolean played, int rounds, int wins, int captures, int kills, int deaths, int mvps, int level,
                      long xp, boolean levels, long toNext, int challengesDone, int of) {

        public static final Own NONE = new Own(false, 0, 0, 0, 0, 0, 0, 1, 0, false, 0, 0, 0);
    }

    /**
     * Where Capture the Flag is: in the lobby ({@link #lobby}) or in a round.
     *
     * @param sides  each side's name, already as MiniMessage
     * @param scores each side's points, in the same order
     */
    public record Glance(Phase phase, String map, String type, List<String> sides, int[] scores, long leftMillis,
                         int players, int lobby, int enough, int countdown) {

        public static Glance lobby(Phase phase, int waiting, int enough, int countdown) {
            return new Glance(phase, "", "", List.of(), new int[0], 0, 0, waiting, enough, countdown);
        }
    }

    /** The most captures, and below them the week's best. Names are the players' own, so they are escaped. */
    public String board(List<Entry> best, List<Entry> week) {
        StringBuilder out = new StringBuilder("<red><bold>⚑ CAPTURE THE FLAG ⚑</bold></red>\n<gray>Most captures\n");
        if (best.isEmpty()) {
            out.append("<gray>Nobody yet — be the first!\n");
        }
        list(out, best);
        if (!week.isEmpty()) {
            out.append("\n<aqua>This week\n");
            list(out, week);
        }
        return out.toString().stripTrailing();
    }

    private static void list(StringBuilder out, List<Entry> entries) {
        for (int index = 0; index < entries.size() && index < PLACES.length; index++) {
            out.append(PLACES[index]).append(" <white>").append(MINI.escapeTags(entries.get(index).name()))
                    .append(" <gray>— <yellow>").append(entries.get(index).value()).append("\n");
        }
    }

    public String you(Own own) {
        StringBuilder out = new StringBuilder("<gold><bold>Your CTF record</bold></gold>");
        if (!own.played() && own.of() == 0) {
            return out.append("\n<gray>No round played yet — <white>/ctf").toString();
        }
        if (own.levels()) {
            out.append("\n<gray>Level <white>").append(own.level()).append(" <dark_gray>(").append(own.toNext())
                    .append(" xp to go)");
        }
        if (own.played()) {
            out.append("\n<gray>Wins <white>").append(own.wins()).append("<dark_gray>/<white>").append(own.rounds());
            out.append("\n<gray>Captures <white>").append(own.captures());
            out.append("\n<gray>Kills <white>").append(own.kills()).append(" <dark_gray>· <gray>K/D <white>")
                    .append(String.format(Locale.ROOT, "%.2f", (double) own.kills() / Math.max(1, own.deaths())));
            if (own.mvps() > 0) {
                out.append("\n<gray>MVPs <white>").append(own.mvps());
            }
        } else {
            out.append("\n<gray>No round played yet — <white>/ctf");
        }
        if (own.of() > 0) {
            out.append("\n<gray>Challenges today <white>").append(own.challengesDone()).append("<dark_gray>/<white>")
                    .append(own.of());
        }
        return out.toString();
    }

    public String round(Glance glance) {
        StringBuilder out = new StringBuilder("<gold><bold>Capture the Flag</bold></gold>");
        switch (glance.phase()) {
            case WAITING -> out.append("\n<gray>Waiting for players\n<gray>In the lobby <white>").append(glance.lobby())
                    .append("</white><dark_gray>/<white>").append(glance.enough());
            case COUNTDOWN -> out.append("\n<gray>Votes open — starts in <white>").append(glance.countdown())
                    .append(" s\n<gray>In the lobby <white>").append(glance.lobby());
            case PREPARING -> out.append("\n<gray>Building the map…");
            default -> {
                out.append("\n<gray>Map <white>").append(MINI.escapeTags(glance.map())).append(" <dark_gray>· <white>")
                        .append(MINI.escapeTags(glance.type()));
                if (glance.sides().size() == 2 && glance.scores().length == 2) {
                    out.append("\n").append(glance.sides().get(0)).append(" <white>").append(glance.scores()[0])
                            .append(" <dark_gray>: <white>").append(glance.scores()[1]).append(" ")
                            .append(glance.sides().get(1));
                } else {
                    for (int side = 0; side < glance.sides().size() && side < glance.scores().length; side++) {
                        out.append("\n").append(glance.sides().get(side)).append(" <white>").append(glance.scores()[side]);
                    }
                }
                switch (glance.phase()) {
                    case OVERTIME -> out.append("\n<red>Overtime — the next point wins");
                    case OVER -> out.append("\n<gray>Round over");
                    default -> out.append("\n<gray>Time left <white>")
                            .append(Times.brief(Duration.ofMillis(Math.max(0, glance.leftMillis()))));
                }
                out.append("\n<gray>Players <white>").append(glance.players());
            }
        }
        return out.toString();
    }

    @Override
    public String describe() {
        return "what the Capture the Flag screens say";
    }
}
