package io.github.lucasstml.towerrogue.ui;

import java.awt.Graphics2D;

/**
 * One screen of the game (State pattern). {@link #update} runs at the fixed simulation
 * rate; {@link #render} runs once per displayed frame and also handles UI interaction
 * (immediate mode).
 */
public interface Screen {

    default void enter() {
    }

    /** Fixed-step logic, {@code dt} is always 1/60 s. */
    default void update(double dt) {
    }

    void render(Graphics2D g, Ui ui, double frameDt);
}
