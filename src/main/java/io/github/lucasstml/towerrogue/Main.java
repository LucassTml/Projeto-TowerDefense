package io.github.lucasstml.towerrogue;

import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.content.ContentLoader;
import io.github.lucasstml.towerrogue.relic.RelicEffects;
import io.github.lucasstml.towerrogue.tools.Headless;
import io.github.lucasstml.towerrogue.ui.GameApp;

/**
 * Entry point.
 * <pre>
 *   java -jar TowerRogue.jar                          # play
 *   java -jar TowerRogue.jar --verify-determinism     # headless replay check
 *   java -jar TowerRogue.jar --balance-report 20      # headless bot balance stats
 * </pre>
 */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Content content = ContentLoader.loadDefault();
        RelicEffects.validate(content);
        if (args.length > 0 && args[0].equals("--verify-determinism")) {
            System.exit(Headless.verifyDeterminism(content, args.length > 1 ? args[1] : null));
        }
        if (args.length > 0 && args[0].equals("--balance-report")) {
            System.exit(Headless.balanceReport(content, args.length > 1 ? Integer.parseInt(args[1]) : 10));
        }
        GameApp.launch(content);
    }
}
