package io.github.lucasstml.towerrogue.ui;

import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.meta.MetaProgression;
import io.github.lucasstml.towerrogue.meta.Profile;
import io.github.lucasstml.towerrogue.run.RunController;
import io.github.lucasstml.towerrogue.run.RunState;
import io.github.lucasstml.towerrogue.save.SaveManager;
import io.github.lucasstml.towerrogue.ui.screens.BattleScreen;
import io.github.lucasstml.towerrogue.ui.screens.EventScreen;
import io.github.lucasstml.towerrogue.ui.screens.HelpOverlay;
import io.github.lucasstml.towerrogue.ui.screens.MainMenuScreen;
import io.github.lucasstml.towerrogue.ui.screens.RestScreen;
import io.github.lucasstml.towerrogue.ui.screens.RewardScreen;
import io.github.lucasstml.towerrogue.ui.screens.RunEndScreen;
import io.github.lucasstml.towerrogue.ui.screens.RunMapScreen;
import io.github.lucasstml.towerrogue.ui.screens.SanctumScreen;
import io.github.lucasstml.towerrogue.ui.screens.ShopScreen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * Application context and navigation. Owns the long-lived services (content, saves,
 * profile) and the current run, and decides which screen shows each {@link
 * io.github.lucasstml.towerrogue.run.RunPhase}. Screens receive this object instead of
 * reaching for globals.
 */
public final class App {
    public final Content content;
    public final SaveManager saves;
    public final Input input;
    public Profile profile;
    public boolean damageNumbers = true;
    /** Set by screens with a focused text field so global hotkeys don't steal letters. */
    public boolean textInputActive;

    private final ScreenManager screens = new ScreenManager();
    private final Ui ui;
    private final HelpOverlay help = new HelpOverlay();
    private RunController run;
    private boolean runRecorded;
    private int lastEssence;
    private boolean quit;
    private double time;
    private String crash;

    App(Content content, Input input) {
        this.content = content;
        this.input = input;
        this.ui = new Ui(input);
        this.saves = new SaveManager(content);
        this.profile = saves.loadProfile();
        screens.set(new MainMenuScreen(this));
    }

    // ================================================================== navigation

    public void toMainMenu() {
        screens.set(new MainMenuScreen(this));
    }

    public void toSanctum() {
        screens.set(new SanctumScreen(this));
    }

    public void newRun(long seed, int ascension) {
        RunState state = MetaProgression.startRun(content, profile, seed, ascension);
        attach(state);
        showRunPhase();
    }

    public boolean continueRun() {
        RunState state = saves.loadRun();
        if (state == null) return false;
        attach(state);
        showRunPhase();
        return true;
    }

    private void attach(RunState state) {
        run = new RunController(content, state);
        runRecorded = false;
        run.setOnChange(this::onRunChanged);
        saves.saveRun(state);
    }

    /** Autosave after every run transition; record the result once the run ends. */
    private void onRunChanged() {
        RunState s = run.state();
        if (s.phase.finished()) {
            if (!runRecorded) {
                runRecorded = true;
                lastEssence = MetaProgression.recordRun(content, profile, s);
                saves.saveProfile(profile);
                saves.deleteRun();
            }
        } else {
            saves.saveRun(s);
        }
    }

    /** Shows the screen that matches the current run phase. */
    public void showRunPhase() {
        if (run == null) {
            toMainMenu();
            return;
        }
        Screen next = switch (run.state().phase) {
            case MAP -> new RunMapScreen(this);
            case BATTLE -> new BattleScreen(this);
            case REWARD -> new RewardScreen(this);
            case SHOP -> new ShopScreen(this);
            case EVENT -> new EventScreen(this);
            case REST -> new RestScreen(this);
            case VICTORY, DEFEAT -> new RunEndScreen(this, lastEssence);
        };
        screens.set(next);
    }

    public RunController run() {
        return run;
    }

    public void saveProfile() {
        saves.saveProfile(profile);
    }

    public void toggleHelp() {
        help.toggle();
    }

    public void quit() {
        quit = true;
    }

    boolean quitRequested() {
        return quit;
    }

    // ================================================================== loop hooks

    void update(double dt) {
        time += dt;
        if (crash == null && !help.isOpen()) screens.update(dt);
    }

    void renderFrame(Graphics2D g, double frameDt) {
        ui.begin(g, time);
        if (crash != null) {
            renderCrash(g);
            ui.end();
            return;
        }
        if (!textInputActive && input.keyPressed(KeyEvent.VK_H)) {
            help.toggle();
            input.clearKeys();
        }
        boolean helpOpen = help.isOpen();
        boolean escape = input.keyPressed(KeyEvent.VK_ESCAPE);
        if (helpOpen) input.clearKeys();
        ui.setBlocked(helpOpen);
        screens.render(g, ui, frameDt);
        ui.setBlocked(false);
        if (helpOpen) help.render(g, ui, escape);
        ui.end();
    }

    void crashed(Throwable t) {
        t.printStackTrace();
        crash = t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private void renderCrash(Graphics2D g) {
        g.setColor(new Color(0x10131C));
        g.fillRect(0, 0, GameApp.WIDTH, GameApp.HEIGHT);
        Draw.shadowText(g, "Something went wrong", 640, 280, Theme.title(36), Theme.HP);
        Draw.textCentered(g, Draw.ellipsize(g, crash, Theme.font(15), 1100), 640, 330, Theme.font(15), Theme.TEXT_DIM);
        Draw.textCentered(g, "Your run was autosaved before this screen. You can continue it from the menu.",
                640, 360, Theme.font(15), Theme.TEXT_DIM);
        if (ui.button("Return to menu", 540, 410, 200, 46, true, Ui.Style.PRIMARY)) {
            crash = null;
            toMainMenu();
        }
    }

    void shutdown() {
        saves.saveProfile(profile);
    }
}
