/**
 * Gera caminhos aleatórios para os inimigos seguirem
 * Cada vez que roda, cria um caminho diferente
 */
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGenerator {
    private Random random;
    
    public MapGenerator() {
        // Cria um novo Random sem seed fixa, então sempre será diferente
        this.random = new Random();
    }
    
    /**
     * Gera um caminho aleatório do canto superior esquerdo até o inferior direito
     * @param pathComplexity 1=simples, 2=médio, 3=complexo (mais curvas)
     */
    public List<Point> generateRandomPath(int rows, int cols, int pathComplexity) {
        List<Point> path = new ArrayList<>();
        
        // Sempre começa no canto superior esquerdo (0,0)
        int currentX = 0;
        int currentY = 0;
        path.add(new Point(currentX * 50 + 25, currentY * 50 + 25)); // +25 pra centralizar no tile
        
        // Destino: canto inferior direito
        int targetX = rows - 1;
        int targetY = cols - 1;
        
        // Gera caminho fazendo zigue-zague até chegar no destino
        // A cada passo, decide aleatoriamente se vai pra direita ou pra baixo
        while (currentX < targetX || currentY < targetY) {
            // Joga uma moeda: true = direita, false = baixo
            boolean moveHorizontal = random.nextBoolean();
            
            // Tenta mover na direção escolhida, se não conseguir vai na outra
            if (moveHorizontal && currentX < targetX) {
                currentX++; // Move pra direita
            } else if (currentY < targetY) {
                currentY++; // Move pra baixo
            } else if (currentX < targetX) {
                // Se não pode ir pra baixo, vai pra direita
                currentX++;
            }
            
            path.add(new Point(currentX * 50 + 25, currentY * 50 + 25));
            
            // Para complexidade maior, adiciona curvas extras aleatórias
            // Isso cria caminhos mais sinuosos e interessantes
            if (pathComplexity > 1 && random.nextDouble() < 0.3) {
                // 30% de chance de adicionar uma curva extra
                if (currentX < targetX && currentY < targetY) {
                    if (random.nextBoolean()) {
                        currentX++;
                    } else {
                        currentY++;
                    }
                    path.add(new Point(currentX * 50 + 25, currentY * 50 + 25));
                }
            }
        }
        
        return path;
    }
    
    // Gera coordenadas do caminho para o Map.setPath()
    public int[][] generatePathCoordinates(int rows, int cols, int pathComplexity) {
        List<Point> path = generateRandomPath(rows, cols, pathComplexity);
        int[][] coords = new int[path.size()][2];
        
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            coords[i][0] = (p.x - 25) / 50; // Converte de pixel para grid
            coords[i][1] = (p.y - 25) / 50;
        }
        
        return coords;
    }
    
    // Gera um caminho simples (linha reta com algumas curvas)
    public int[][] generateSimplePath(int rows, int cols) {
        return generatePathCoordinates(rows, cols, 1);
    }
    
    // Gera um caminho médio (mais curvas)
    public int[][] generateMediumPath(int rows, int cols) {
        return generatePathCoordinates(rows, cols, 2);
    }
    
    // Gera um caminho complexo (muitas curvas)
    public int[][] generateComplexPath(int rows, int cols) {
        return generatePathCoordinates(rows, cols, 3);
    }
}

