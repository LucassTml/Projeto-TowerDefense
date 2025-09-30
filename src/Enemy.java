import java.awt.Point;
import java.util.List;

// Primeiro tipo de inimigo, o mais basico
// Fazer depois, mais rapido, um com mais vida e maior, um com dupla camada, um com camada secreta/algo do tipo

public class Enemy {
    private double x, y;
    private double speed;
    private int damage;
    private int vida;
    private int currentWaypoint;
    private List<Point> path;

    public Enemy(List<Point> path, double speed, int vida, int damage) {
        this.path = path;
        this.speed = speed;
        this.vida = vida;
        this.damage = damage;
        this.currentWaypoint = 0;

        // comecar no centro no primeiro tile
        Point start = path.get(0);
        this.x = start.x + 5; // Centraliza em X (tile 50px, inimigo 40px)
        this.y = start.y + 5; // Centraliza em Y
    }

    public void update() {
        //último ponto, não se move mais
        if (currentWaypoint >= path.size()) {
            return;
        }

        // Define o centro do tile alvo
        Point targetPoint = path.get(currentWaypoint);
        double targetX = targetPoint.x + 5;
        double targetY = targetPoint.y + 5;

        // Distancia ate o alvo
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy); // pitagoras

        // Se a distancia < velocidade da tp para o alvo (oq nao deve acontecer idalmente ne)
        // para evitar que ele passe do ponto, e avanca para o proximo alvo.
        if (distance < speed) {
            x = targetX;
            y = targetY;
            currentWaypoint++;

        } else {
            // Se estiver longe, vai normal
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
    }

    public boolean reachedEnd() {
        return currentWaypoint >= path.size();  // retorna true se chegou
    }


    // Gets e tal
    public int getDamage() {
        return damage;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public void takeDamage(int amount) {
        vida -= amount;
    }

    public boolean isDead() {
        return vida <= 0;
    }
}