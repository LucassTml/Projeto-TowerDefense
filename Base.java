// Base.java
public class Base {
    private int vidaAtual;
    private int vidaMaxima;

    public Base(int vidaMaxima) {
        this.vidaMaxima = vidaMaxima;
        this.vidaAtual = vidaMaxima;
    }

    public void receberDano(int dano) {
        this.vidaAtual -= dano;
        // Garante que a vida não fique negativa
        if (this.vidaAtual < 0) {
            this.vidaAtual = 0;
        }
    }

    // Método para verificar se a base foi destruída
    // Este é o método que estava causando o erro de "cannot find symbol"
    public boolean estaDestruida() {
        return vidaAtual <= 0;
    }

    // Getters para a vida atual e máxima da base
    public int getVidaAtual() {
        return vidaAtual;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }
}
