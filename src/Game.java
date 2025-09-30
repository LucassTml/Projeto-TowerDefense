import java.awt.Point;
import java.util.ArrayList;


// Tava usando essa classe antes pra testar funcionamento, mas agr com a GameGUI essa aq eh so meio q pra testes para printar coisas no terminal e tal
// A principal de vdd eh a GameGUI, ela veio daqui

 class Game {
    private Map map;
    private Base base;
    private WaveManager waveManager;

// guarda para depois das waves
    private int ultimaVida;
    private int ultimaOnda;

    public Game() {

        // daria pra fazer algo no terminal com ascii, mas acho melhor so ir pra gui direto. talvez dps fazer uma versao em terminal
        map = new Map(10, 10);
        base = new Base(100);

        // coordenadas
        int[][] pathCoords = {{0,0},{1,0},{2,0},{3,0},{4,0}};
        map.setPath(pathCoords);

        java.util.List<Point> path = new ArrayList<>();
        for (int[] p : pathCoords) {
            path.add(new Point(p[0], p[1]));
        }

        waveManager = new WaveManager(path);

        ultimaVida = base.getVidaAtual();
        ultimaOnda = 0;
    }

    public void start() {
        waveManager.startNextWave();

        while (!base.Destruida()) {
            System.out.println("Game Over!");
        }
    }

    public void update() {
        waveManager.update();

        for (Enemy e : new ArrayList<>(waveManager.getEnemies())) {
            if (e.reachedEnd()) {
                base.receberDano(e.getDamage());
                waveManager.getEnemies().remove(e);
            }
        }

        if (base.getVidaAtual() != ultimaVida || waveManager.getCurrentWave() != ultimaOnda) {
            render();
            ultimaVida = base.getVidaAtual();
            ultimaOnda = waveManager.getCurrentWave();
        }
    }

    public void render() {
        System.out.println("Base: " + base.getVidaAtual() + "/" + base.getVidaMaxima() +
                " | Onda: " + waveManager.getCurrentWave());
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
