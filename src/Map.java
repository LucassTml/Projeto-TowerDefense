public class Map {
    // grid é uma matriz (um array de duas dimensões)
    private Tile[][] grid;

    private int rows;
    private int cols;

    // Construtor
    public Map(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        // Cria o grid com o tamanho definido
        this.grid = new Tile[rows][cols];

        // preencher o grid com os ladrilhos iniciais. Olhar metodo abaixo
        initializeGrid();
    }

    // Preencher o mapa com ladrilhos
    private void initializeGrid() {
        // Double for para cada linha (i) e coluna (j) do grid
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Em cada posição [i][j], criamos um novo Tile.
                // Por padrao, ele n é um caminho (isPath = false) e pode ser construido (canBuild = true).
                grid[i][j] = new Tile(false, true);
            }
        }
    }

    // Quais ladrilhos sao caminho dos enemys.

    public void setPath(int[][] pathCoordinates) {
        // Iterar por cada par de coordenadas
        for (int[] coord : pathCoordinates) {
            int row = coord[0]; // 1pos é a linha.
            int col = coord[1]; // 2pos é a coluna.

            // Verificar se coord eh valida
            if (isValid(row, col)) {
                // Sim -> ent Tile antigo vira um novo.
                // Este novo Tile agora eh um caminho (isPath = true) e n se pode construir nele (canBuild = false).
                grid[row][col] = new Tile(true, false);
            }
        }
    }

    private boolean isValid(int row, int col) {
        // True if row and col forem >=0 e menores que o tamanho total.
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    // Get tile para outras classes
    public Tile getTile(int row, int col) {
        if (isValid(row, col)) {
            return grid[row][col];
        }
        return null;
    }

// outros gets
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}