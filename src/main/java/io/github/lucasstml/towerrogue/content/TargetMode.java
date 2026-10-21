package io.github.lucasstml.towerrogue.content;

/** Target priority of a tower. FIRST means "closest to the base". */
public enum TargetMode {
    FIRST("First"),
    LAST("Last"),
    STRONGEST("Strongest"),
    WEAKEST("Weakest"),
    CLOSEST("Closest");

    public final String label;

    TargetMode(String label) {
        this.label = label;
    }

    public TargetMode next() {
        TargetMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
