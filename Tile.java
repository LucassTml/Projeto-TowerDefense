public class Tile {
    private boolean isPath;
    private boolean canBuild;

    public Tile(boolean isPath, boolean canBuild) {
        this.isPath = isPath;
        this.canBuild = canBuild;
    }

    public boolean isPath() {
        return isPath;
    }

    public boolean canBuild() {
        return canBuild;
    }

    public void setCanBuild(boolean canBuild) {
        this.canBuild = canBuild;
    }
}