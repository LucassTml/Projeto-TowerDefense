// Enemy.java
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Point;
import java.util.List;

public class Enemy { // <--- Garanta que não seja 'abstract' se você não tiver subclasses que a implementem
    protected double x, y; // Tornar protected para subclasses acessarem diretamente
    protected double baseSpeed; // Velocidade base (sem modificadores)
    protected double speed; // Velocidade atual (com modificadores)
    protected int damage;
    protected int vidaAtual; // Renomeado para vidaAtual para clareza
    protected int vidaMaxima; // Adicionado vidaMaxima
    protected int currentWaypoint;
    protected List<Point> path;
    protected int killReward;
    
    // Sistema de status effects
    protected EnemyStatus status;
    
    // Resistências (0.0 = sem resistência, 1.0 = imune)
    protected double slowResistance = 0.0;   // Resistência a slow
    protected double burnResistance = 0.0;  // Resistência a burn
    protected double freezeResistance = 0.0; // Resistência a freeze

    public Enemy(List<Point> path, double speed, int vidaInicial, int damage, int killReward) {
        this.path = path;
        this.baseSpeed = speed;
        this.speed = speed;
        this.vidaAtual = vidaInicial;
        this.vidaMaxima = vidaInicial; // Vida máxima é a vida inicial
        this.damage = damage;
        this.killReward = killReward;
        this.currentWaypoint = 0;
        this.status = new EnemyStatus();

        Point start = path.get(0);
        this.x = start.x;
        this.y = start.y;
    }
    
    // Construtor com resistências
    public Enemy(List<Point> path, double speed, int vidaInicial, int damage, int killReward,
                 double slowResistance, double burnResistance, double freezeResistance) {
        this(path, speed, vidaInicial, damage, killReward);
        this.slowResistance = slowResistance;
        this.burnResistance = burnResistance;
        this.freezeResistance = freezeResistance;
    }

    /**
     * Atualiza o inimigo a cada frame
     * Aplica efeitos de status, atualiza velocidade, e move pelo caminho
     */
    public void update() {
        // Primeiro atualiza os efeitos de status (diminui duração, etc)
        status.update();
        
        // Aplica dano de queimadura se estiver queimando
        // A resistência reduz o dano (ex: 80% resistência = 20% do dano)
        if (status.getBurnDamage() > 0) {
            int burnDmg = (int)(status.getBurnDamage() * (1.0 - burnResistance));
            takeDamage(burnDmg);
        }
        
        // Calcula velocidade efetiva considerando efeitos de slow/freeze
        // Multiplicador: 1.0 = normal, 0.5 = metade, 0.2 = quase parado
        double effectiveSpeed = baseSpeed * status.getSlowMultiplier();
        this.speed = effectiveSpeed;
        
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
    
    // Aplica um status effect ao inimigo (considerando resistências)
    public void applyStatusEffect(StatusEffect effect, int duration) {
        boolean canApply = true;
        
        switch(effect) {
            case SLOW:
                canApply = (Math.random() > slowResistance);
                break;
            case BURN:
                canApply = (Math.random() > burnResistance);
                break;
            case FREEZE:
                canApply = (Math.random() > freezeResistance);
                break;
        }
        
        if (canApply) {
            status.applyEffect(effect, duration);
        }
    }
    
    public EnemyStatus getStatus() {
        return status;
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

        g2d.setColor(new Color(200, 50, 50)); // Vermelho padrão
        g2d.fillOval(drawX, drawY, enemySize, enemySize);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(drawX, drawY, enemySize, enemySize);
        
        // Desenha indicadores de status effects
        if (status.hasEffect(StatusEffect.SLOW) || status.hasEffect(StatusEffect.FREEZE)) {
            g2d.setColor(new Color(100, 150, 255, 150)); // Azul transparente para slow/freeze
            g2d.fillOval(drawX - 2, drawY - 2, enemySize + 4, enemySize + 4);
        }
        if (status.hasEffect(StatusEffect.BURN)) {
            g2d.setColor(new Color(255, 100, 0, 150)); // Laranja/vermelho transparente para burn
            g2d.fillOval(drawX - 2, drawY - 2, enemySize + 4, enemySize + 4);
        }
    }
}