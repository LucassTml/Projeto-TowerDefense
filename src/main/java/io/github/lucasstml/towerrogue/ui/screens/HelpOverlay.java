package io.github.lucasstml.towerrogue.ui.screens;

import io.github.lucasstml.towerrogue.ui.Draw;
import io.github.lucasstml.towerrogue.ui.Theme;
import io.github.lucasstml.towerrogue.ui.Ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** "How to play" overlay, toggled with H from anywhere. */
public final class HelpOverlay {
    private boolean open;

    private static final String[][] SECTIONS = {
            {"The run", "Cross 3 acts on a branching map. Each node is a battle, an elite fight, a merchant, a campfire, "
                    + "a treasure or a random event. Beat the act boss to move on. Lose all HP and the run ends; "
                    + "Essence earned unlocks permanent upgrades in the Sanctum."},
            {"Battles", "Each battle starts fresh with mana. Build towers from your blueprints, upgrade them to level 3, "
                    + "then pick one of two specializations. Kills and cleared waves give mana. Calling a wave early gives bonus mana."},
            {"Growing stronger", "After battles, draft 1 of 3 cards: a new blueprint or a permanent blueprint upgrade (Mk II, Mk III). "
                    + "Elites and bosses also drop relics: passive items that bend the rules."},
            {"Synergies", "Towers share tags (physical, frost, fire...). Having enough towers of a tag on the field activates bonuses. "
                    + "One tower of each element unlocks Elemental Harmony."},
            {"Damage", "Armor reduces each physical hit (at least 25% always goes through). Poison ignores armor and stops regeneration. "
                    + "Physical hits on frozen enemies SHATTER for double damage. Flying enemies ignore the road: only some towers hit them."},
            {"Controls", "1-9 pick blueprint · Click build · Shift+click keep building · Right-click cancel · Space start/call wave · "
                    + "P pause · F speed · U upgrade · S sell · T target · D damage numbers · Esc menu · F11 fullscreen · H help"},
    };

    public void toggle() {
        open = !open;
    }

    public boolean isOpen() {
        return open;
    }

    public void render(Graphics2D g, Ui ui, boolean closeKey) {
        g.setColor(new Color(5, 6, 10, 200));
        g.fillRect(0, 0, 1280, 720);
        double x = 170, y = 50, w = 940, h = 620;
        Draw.panel(g, x, y, w, h);
        Draw.shadowText(g, "How to play", 640, y + 38, Theme.title(30), Theme.GOLD);
        double ty = y + 80;
        for (String[] s : SECTIONS) {
            Draw.text(g, s[0], x + 36, ty, Theme.bold(16), Theme.TEXT);
            ty += 22;
            ty += Draw.paragraph(g, s[1], x + 36, ty, w - 72, Theme.font(14), Theme.TEXT_DIM, 19) + 12;
        }
        if (ui.button("Close  [H]", 640 - 90, y + h - 58, 180, 42, true, Ui.Style.PRIMARY) || closeKey) {
            open = false;
        }
    }
}
