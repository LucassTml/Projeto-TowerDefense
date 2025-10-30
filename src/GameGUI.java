import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List; // Importe o List do java.util

public class GameGUI extends JPanel {
    private Map map;
    private Base base;
    private WaveManager waveManager;

    private List<Tower> towers = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private int playerMoney = 100; // Economia inicial

    // NOVOS: Estado do Jogo e Referências de UI
    private enum GameState { BUILDING_PHASE, WAVE_IN_PROGRESS }
    private GameState currentState;
    private int selectedTowerType = 0; // 0 = Nada, 1 = Basic, 2 = Sniper

    private JButton startWaveButton;
    private JRadioButton basicTowerButton;
    private JRadioButton sniperTowerButton;

    // CONSTRUTOR MODIFICADO: Recebe os botões da UI
    public GameGUI(JButton startWaveButton, JRadioButton basicTowerButton, JRadioButton sniperTowerButton) {
        map = new Map(10, 10);
        base = new Base(100);

        int[][] pathCoords = {{0,0},{1,0},{2,0},{3,0},{4,0}};
        map.setPath(pathCoords);

        java.util.List<Point> path = new ArrayList<>();
        for (int[] p : pathCoords) {
            path.add(new Point(p[0] * 50, p[1] * 50));
        }

        waveManager = new WaveManager(path);

        // Atribui as referências de UI
        this.startWaveButton = startWaveButton;
        this.basicTowerButton = basicTowerButton;
        this.sniperTowerButton = sniperTowerButton;

        // Define o estado inicial
        this.currentState = GameState.BUILDING_PHASE;
        updateUIElements(); // Atualiza o texto/estado inicial dos botões

        // Adiciona o listener de clique (sem mudança)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                placeTower(e.getX(), e.getY());
            }
        });

        Timer timer = new Timer(50, e -> gameLoop());
        timer.start();

        // REMOVEMOS O waveManager.startNextWave() DAQUI
        // Agora o usuário inicia a primeira onda
    }

    public void startWave() {
        if (currentState == GameState.BUILDING_PHASE) {
            waveManager.startNextWave();
            currentState = GameState.WAVE_IN_PROGRESS;
        }
    }

    public void setSelectedTowerType(int type) {
        this.selectedTowerType = type;
        System.out.println("Tipo de torre selecionado: " + type);
    }

    private void placeTower(int x, int y) {
        int row = x / 50;
        int col = y / 50;

        Tile tile = map.getTile(row, col);

        if (tile != null && tile.canBuild()) {

            // Lógica de construção atualizada
            if (selectedTowerType == 1 && playerMoney >= BasicTower.COST) {
                playerMoney -= BasicTower.COST;
                towers.add(new BasicTower(row * 50 + 25, col * 50 + 25));
                tile.setCanBuild(false);
                System.out.println("Basic Tower construída! Dinheiro: " + playerMoney);

            } else if (selectedTowerType == 2 && playerMoney >= SniperTower.COST) {
                playerMoney -= SniperTower.COST;
                towers.add(new SniperTower(row * 50 + 25, col * 50 + 25));
                tile.setCanBuild(false);
                System.out.println("Sniper Tower construída! Dinheiro: " + playerMoney);

            } else if (selectedTowerType == 0) {
                System.out.println("Não pode construir: Selecione um tipo de torre primeiro.");
            } else {
                // Se o tipo estiver selecionado mas chegou aqui, é falta de dinheiro
                System.out.println("Não pode construir: Dinheiro insuficiente.");
            }
        } else {
            System.out.println("Não pode construir: Local inválido (caminho ou já ocupado).");
        }
    }

    // GAMELOOP MODIFICADO: Agora usa o GameState
    private void gameLoop() {

        // Só atualiza inimigos, torres e projéteis se a onda estiver em progresso
        if (currentState == GameState.WAVE_IN_PROGRESS) {

            // 1. Atualiza WaveManager (spawns, move inimigos, remove mortos)
            int moneyFromKills = waveManager.update();
            playerMoney += moneyFromKills;

            // 2. Verifica inimigos que chegaram ao fim
            List<Enemy> enemiesReachedEnd = new ArrayList<>();
            for (Enemy enemy : waveManager.getEnemies()) {
                if (enemy.reachedEnd()) {
                    base.receberDano(enemy.getDamage());
                    enemiesReachedEnd.add(enemy);
                }
            }
            waveManager.getEnemies().removeAll(enemiesReachedEnd); // Remove-os

            // 3. Atualiza Torres (encontram alvos, disparam projéteis)
            for (Tower tower : towers) {
                Projectile p = tower.update(waveManager.getEnemies());
                if (p != null) {
                    projectiles.add(p);
                }
            }

            // 4. Atualiza Projéteis (movem, dão dano, são removidos)
            List<Projectile> projectilesToRemove = new ArrayList<>();
            for (Projectile p : projectiles) {
                p.update();
                if (!p.isActive()) {
                    projectilesToRemove.add(p);
                }
            }
            projectiles.removeAll(projectilesToRemove);

            // 5. Verifica se a onda terminou
            if (waveManager.isWaveComplete() && waveManager.getCurrentWave() > 0) {
                currentState = GameState.BUILDING_PHASE;
                System.out.println("Onda " + waveManager.getCurrentWave() + " completada!");
            }
        }

        // 6. Atualiza o estado dos botões (sempre)
        updateUIElements();

        // 7. Redesenha a tela (sempre)
        repaint();
    }

    private void updateUIElements() {
        // Atualiza o botão de iniciar onda
        if (currentState == GameState.BUILDING_PHASE) {
            startWaveButton.setEnabled(true);
            startWaveButton.setText("Iniciar Onda " + (waveManager.getCurrentWave() + 1));
        } else {
            startWaveButton.setEnabled(false);
            startWaveButton.setText("Onda " + waveManager.getCurrentWave() + " em progresso...");
        }

        // Atualiza botões de torre (pode comprar?)
        basicTowerButton.setEnabled(playerMoney >= BasicTower.COST);
        sniperTowerButton.setEnabled(playerMoney >= SniperTower.COST);

        // Atualiza texto dos botões (para mostrar o custo)
        basicTowerButton.setText("Torre Básica ($" + BasicTower.COST + ")");
        sniperTowerButton.setText("Torre Sniper ($" + SniperTower.COST + ")");
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. desenha grid (sem mudanças)
        for (int i = 0; i < map.getRows(); i++) {
            for (int j = 0; j < map.getCols(); j++) {
                Tile t = map.getTile(i, j);
                if (t.isPath()) {
                    g.setColor(Color.LIGHT_GRAY);
                } else {
                    g.setColor(Color.GREEN);
                }
                g.fillRect(i * 50, j * 50, 50, 50);
                g.setColor(Color.BLACK);
                g.drawRect(i * 50, j * 50, 50, 50);
            }
        }

        // 2. desenha inimigos (sem mudanças)
        for (Enemy e : waveManager.getEnemies()) {
            int ex = (int)e.getX();
            int ey = (int)e.getY();
            int size = 40;

            g.setColor(Color.RED);
            g.fillOval(ex, ey, size, size);
            g.setColor(Color.BLACK);
            g.drawOval(ex, ey, size, size);
        }

        // 3. Desenha Torres (sem mudanças)
        for (Tower t : towers) {
            t.draw(g2d);
        }

        // 4. Desenha Projéteis (sem mudanças)
        for (Projectile p : projectiles) {
            p.draw(g2d);
        }

        // 5. HUD (sem mudanças, apenas no 'main' que aumentamos o frame)
        Font hudFont = new Font("Verdana", Font.BOLD, 16);
        g.setFont(hudFont);
        g.setColor(Color.BLACK);

        String waveText = "Onda: " + waveManager.getCurrentWave();
        String healthText = "Base: " + base.getVidaAtual() + "/" + base.getVidaMaxima();
        String moneyText = "Dinheiro: $" + playerMoney;

        g.drawString(waveText, 10, 390);
        g.drawString(healthText, 10, 420);
        g.drawString(moneyText, 10, 450);

        int barWidth = 150;
        int barHeight = 20;
        int barX = 10;
        int barY = 460;

        double healthPercentage = (double) base.getVidaAtual() / base.getVidaMaxima();
        int greenWidth = (int) (barWidth * healthPercentage);

        g.setColor(new Color(100, 0, 0));
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, greenWidth, barHeight);
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tower Defense Projeto Alpha");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout()); // Usa BorderLayout

        // 1. Criar os botões de controle
        JButton startWaveButton = new JButton("Carregando...");

        JRadioButton basicTowerButton = new JRadioButton("Torre Básica ($25)");
        JRadioButton sniperTowerButton = new JRadioButton("Torre Sniper ($75)");

        // Agrupar os RadioButtons (para que só um seja selecionado)
        ButtonGroup towerGroup = new ButtonGroup();
        towerGroup.add(basicTowerButton);
        towerGroup.add(sniperTowerButton);

        // 2. Criar o painel do jogo (GameGUI)
        // Passamos os botões para o construtor do GameGUI
        GameGUI gamePanel = new GameGUI(startWaveButton, basicTowerButton, sniperTowerButton);

        // 3. Criar o painel de controle e adicionar os botões
        JPanel controlPanel = new JPanel(); // Painel para os botões
        controlPanel.add(startWaveButton);
        controlPanel.add(basicTowerButton);
        controlPanel.add(sniperTowerButton);

        // 4. Adicionar ActionListeners aos botões
        // Eles chamam os métodos públicos que criamos no gamePanel
        startWaveButton.addActionListener(e -> gamePanel.startWave());

        basicTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(1));
        sniperTowerButton.addActionListener(e -> gamePanel.setSelectedTowerType(2));

        // 5. Adicionar os painéis ao Frame
        frame.add(gamePanel, BorderLayout.CENTER); // Jogo no centro
        frame.add(controlPanel, BorderLayout.SOUTH); // Controles embaixo

        // Aumentar a altura para caber os botões
        frame.setSize(600, 700);
        frame.setVisible(true);
    }
}