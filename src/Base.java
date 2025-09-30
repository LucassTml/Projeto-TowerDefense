public class Base {

    private int vidaAtual;
    private final int vidaMaxima;

    public Base(int vidaMaxima) {
        this.vidaMaxima = vidaMaxima;
        this.vidaAtual = vidaMaxima;
    }

    public void receberDano(int dano) {
        if (dano > 0) {
            this.vidaAtual -= dano;
            if (this.vidaAtual < 0) {   //caso passe da vida max so torna igual a zero
                this.vidaAtual = 0;
            }
        }
    }

    public void curar(int quantidade) { //futuro maybe fazer algo pra curar a base, item ou poder talvez. ou nao tbm
        if (quantidade > 0) {
            this.vidaAtual += quantidade;
            if (this.vidaAtual > this.vidaMaxima) {
                this.vidaAtual = this.vidaMaxima;
            }
        }
    }

    public int getVidaAtual() {
        return vidaAtual;
    }

    public int getVidaMaxima() {
        return vidaMaxima;
    }

    public boolean Destruida() {
        return this.vidaAtual <= 0;
    }
}
