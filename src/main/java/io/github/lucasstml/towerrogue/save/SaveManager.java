package io.github.lucasstml.towerrogue.save;

import io.github.lucasstml.towerrogue.content.Content;
import io.github.lucasstml.towerrogue.json.JsonObject;
import io.github.lucasstml.towerrogue.json.JsonParser;
import io.github.lucasstml.towerrogue.json.JsonWriter;
import io.github.lucasstml.towerrogue.meta.Profile;
import io.github.lucasstml.towerrogue.run.RunState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes {@code profile.json} and {@code run.json} under {@code ~/.towerrogue}
 * (override with {@code -Dtowerrogue.home=...}).
 *
 * <p>Writes are atomic: data goes to a temporary file that then replaces the real one, so a
 * crash mid-save can never leave a half-written, corrupted save. A save that fails to
 * parse (e.g. from an incompatible version) is moved aside instead of crashing the game.
 */
public final class SaveManager {
    private final Path dir;
    private final Content content;

    public SaveManager(Content content) {
        this(content, Path.of(System.getProperty("towerrogue.home",
                Path.of(System.getProperty("user.home"), ".towerrogue").toString())));
    }

    public SaveManager(Content content, Path dir) {
        this.content = content;
        this.dir = dir;
    }

    public Path directory() {
        return dir;
    }

    public Profile loadProfile() {
        JsonObject o = read("profile.json");
        if (o == null) return new Profile();
        try {
            return Serializers.readProfile(o);
        } catch (RuntimeException e) {
            quarantine("profile.json", e);
            return new Profile();
        }
    }

    public void saveProfile(Profile p) {
        write("profile.json", Serializers.writeProfile(p));
    }

    public boolean hasRun() {
        return Files.exists(dir.resolve("run.json"));
    }

    public RunState loadRun() {
        JsonObject o = read("run.json");
        if (o == null) return null;
        try {
            return Serializers.readRun(o, content);
        } catch (RuntimeException e) {
            quarantine("run.json", e);
            return null;
        }
    }

    public void saveRun(RunState run) {
        write("run.json", Serializers.writeRun(run));
    }

    public void deleteRun() {
        try {
            Files.deleteIfExists(dir.resolve("run.json"));
        } catch (IOException e) {
            System.err.println("Could not delete run save: " + e.getMessage());
        }
    }

    private JsonObject read(String name) {
        Path p = dir.resolve(name);
        if (!Files.exists(p)) return null;
        try {
            Object v = JsonParser.parse(Files.readString(p, StandardCharsets.UTF_8));
            return v instanceof JsonObject o ? o : null;
        } catch (IOException | RuntimeException e) {
            quarantine(name, e);
            return null;
        }
    }

    private void write(String name, JsonObject data) {
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Path tmp = dir.resolve(name + ".tmp");
            Files.writeString(tmp, JsonWriter.write(data, true), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not save " + name + ": " + e.getMessage());
        }
    }

    private void quarantine(String name, Exception cause) {
        System.err.println("Ignoring unreadable save " + name + ": " + cause.getMessage());
        try {
            Path p = dir.resolve(name);
            if (Files.exists(p)) Files.move(p, dir.resolve(name + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // nothing else we can do; the game continues with fresh data
        }
    }
}
