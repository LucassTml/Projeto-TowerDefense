public class Map {
    private Tile[][] grid;
    private int rows;
    private int cols;

    public Map(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new Tile[rows][cols];
        initializeGrid();
    }

    private void initializeGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Tile(false, true);
            }
        }
    }

    public void setPath(int[][] pathCoordinates) {
        for (int[] coord : pathCoordinates) {
            int row = coord[0];
            int col = coord[1];
            if (isValid(row, col)) {
                grid[row][col] = new Tile(true, false);
            }
        }
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public Tile getTile(int row, int col) {
        if (isValid(row, col)) {
            return grid[row][col];
        }
        return null;
    }
}
