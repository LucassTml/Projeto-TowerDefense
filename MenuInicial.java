/**
 * Menu inicial do jogo
 * Deixa o jogador escolher tamanho do mapa e complexidade do caminho
 * antes de começar a jogar
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuInicial extends JFrame {
    // Componentes da interface
    private JComboBox<String> mapSizeCombo;        // Tamanho do mapa
    private JComboBox<String> pathComplexityCombo;  // Complexidade do caminho
    private JButton startButton;
    
    // Controle de estado
    private boolean gameStarted = false;
    private GameConfig config; // Configuração escolhida pelo jogador
    
    public MenuInicial() {
        setTitle("Tower Defense - Menu Inicial");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Título
        JLabel titleLabel = new JLabel("TOWER DEFENSE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        mainPanel.add(Box.createVerticalStrut(30));
        
        // Tamanho do mapa
        JLabel sizeLabel = new JLabel("Tamanho do Mapa:");
        sizeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(sizeLabel);
        
        mapSizeCombo = new JComboBox<>(new String[]{"Pequeno (8x8)", "Médio (10x10)", "Grande (12x12)"});
        mapSizeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        mapSizeCombo.setMaximumSize(new Dimension(200, 30));
        mainPanel.add(mapSizeCombo);
        
        mainPanel.add(Box.createVerticalStrut(20));
        
        // Complexidade do caminho
        JLabel complexityLabel = new JLabel("Complexidade do Caminho:");
        complexityLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        complexityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(complexityLabel);
        
        pathComplexityCombo = new JComboBox<>(new String[]{"Simples", "Médio", "Complexo"});
        pathComplexityCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pathComplexityCombo.setMaximumSize(new Dimension(200, 30));
        mainPanel.add(pathComplexityCombo);
        
        mainPanel.add(Box.createVerticalStrut(30));
        
        // Botão iniciar
        startButton = new JButton("INICIAR JOGO");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setPreferredSize(new Dimension(200, 40));
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
        mainPanel.add(startButton);
        
        add(mainPanel);
    }
    
    private void startGame() {
        int mapSizeIndex = mapSizeCombo.getSelectedIndex();
        int complexityIndex = pathComplexityCombo.getSelectedIndex();
        
        int rows, cols;
        switch(mapSizeIndex) {
            case 0: rows = 8; cols = 8; break;
            case 1: rows = 10; cols = 10; break;
            case 2: rows = 12; cols = 12; break;
            default: rows = 10; cols = 10; break;
        }
        
        int pathComplexity;
        switch(complexityIndex) {
            case 0: pathComplexity = 1; break;
            case 1: pathComplexity = 2; break;
            case 2: pathComplexity = 3; break;
            default: pathComplexity = 2; break;
        }
        
        config = new GameConfig(rows, cols, pathComplexity);
        gameStarted = true;
        dispose();
    }
    
    public boolean isGameStarted() {
        return gameStarted;
    }
    
    public GameConfig getConfig() {
        return config;
    }
    
}

// Classe para armazenar configuração do jogo
class GameConfig {
    public final int rows;
    public final int cols;
    public final int pathComplexity;
    
    public GameConfig(int rows, int cols, int pathComplexity) {
        this.rows = rows;
        this.cols = cols;
        this.pathComplexity = pathComplexity;
    }
}

