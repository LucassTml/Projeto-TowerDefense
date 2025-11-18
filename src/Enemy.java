// Enemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class Enemy { // <--- Garanta que não seja 'abstract' se você não tiver subclasses que a implementem
    protected double x, y; // Tornar protected para subclasses acessarem diretamente
    protected double speed;
    protected int damage;
    protected int vidaAtual; // Renomeado para vidaAtual para clareza
    protected int vidaMaxima; // Adicionado vidaMaxima
    protected int currentWaypoint;
    protected List<Point> path;
    protected int killReward;

    public Enemy(List<Point> path, double speed, int vidaInicial, int damage, int killReward) {
        this.path = path;
        this.speed = speed;
        this.vidaAtual = vidaInicial;
        this.vidaMaxima = vidaInicial; // Vida máxima é a vida inicial
        this.damage = damage;
        this.killReward = killReward;
        this.currentWaypoint = 0;

        Point start = path.get(0);
        this.x = start.x;
        this.y = start.y;
    }

    public void update() {
        if (currentWaypoint >= path.size()) {
            return;
        }

        Point targetPoint = path.get(currentWaypoint);
        double targetX = targetPoint.x;
        double targetY = targetPoint.y;

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < speed) {
            x = targetX;
            y = targetY;
            currentWaypoint++;
        } else {
            x += (dx / distance) * speed;
            y += (dy / distance) * speed;
        }
    }

    public boolean reachedEnd() {
        return currentWaypoint >= path.size();
    }

    public int getDamage() {
        return damage;
    }

    public double getX() { return x; } // PUBLICO
    public double getY() { return y; } // PUBLICO

    public int getVidaAtual() {
        return vidaAtual;
    }

    public void takeDamage(int amount) {
        vidaAtual -= amount;
        if (vidaAtual < 0) {
            vidaAtual = 0;
        }
    }

    public boolean isAlive() { // O método que Tower.java está procurando
        return vidaAtual > 0;
    }

    public int getKillReward() {
        return killReward;
    }

    public int getVidaMaxima() { // Adicionei este getter também
        return vidaMaxima;
    }

    public void draw(Graphics2D g2d) {
        int enemySize = 40;
        int drawX = (int)x - enemySize / 2;
        int drawY = (int)y - enemySize / 2;

        g2d.setColor(Color.RED);
        g2d.fillOval(drawX, drawY, enemySize, enemySize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(drawX, drawY, enemySize, enemySize);
    }
}