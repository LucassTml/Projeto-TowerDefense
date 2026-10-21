package io.github.lucasstml.towerrogue.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** Holds the active screen and cross-fades between screens. */
final class ScreenManager {
    private static final double FADE_SECONDS = 0.16;

    private Screen current;
    private Screen next;
    private double fade;
    private int direction;

    void set(Screen screen) {
        if (current == null) {
            current = screen;
            screen.enter();
        } else {
            next = screen;
            direction = 1;
        }
    }

    Screen current() {
        return current;
    }

    void update(double dt) {
        if (current != null && next == null) current.update(dt);
    }

    void render(Graphics2D g, Ui ui, double frameDt) {
        if (direction == 1) {
            fade += frameDt / FADE_SECONDS;
            if (fade >= 1) {
                fade = 1;
                current = next;
                next = null;
                current.enter();
                direction = -1;
            }
        } else if (direction == -1) {
            fade -= frameDt / FADE_SECONDS;
            if (fade <= 0) {
                fade = 0;
                direction = 0;
            }
        }
        ui.setBlocked(direction != 0);
        if (current != null) current.render(g, ui, frameDt);
        ui.setBlocked(false);
        if (fade > 0) {
            g.setColor(new Color(0, 0, 0, (int) (fade * 255)));
            g.fillRect(0, 0, GameApp.WIDTH, GameApp.HEIGHT);
        }
    }
}
