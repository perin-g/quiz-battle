package br.cesul.model;

// Representar o nivel de dificuldade de uma pergunta
// Cada constante de dif. carrega UM VALOR (pontuacao)

public enum Dificuldade {
    // Constantes com parametros:
    // Os argumentos de cada const. vão para
    // o construtor do ENUM
    // Assim como classes normais, quando rodar o
    // sistema, cada constante já tera o seu atributo
    // pontos preenchidos.
    FACIL(5),
    MEDIO(10),
    DIFICIL(20);

    // Cada constante guarda seu próprio "pontos"
    // É final porque nao faz sentido mudar
    // depois de criado
    private final int pontos;

    // Em enums o contrutor SEMPRE é privado
    // (implicitamente). Só a JVM chama este const.
    // uma vez para cada constante
    // Porque o construtor do enum é private?
    // Porque os unicos objetos desta classe
    // são os que eu declarei la em cima, não faz
    // sentido deixar publico e alguem poder criar
    // uma 4º entidade.
    Dificuldade(int pontos){
        this.pontos = pontos;
    }

    public int getPontos() {
        return pontos;
    }

    public String rotulo() {
        return switch (this) {
            case FACIL -> "Fácil";
            case MEDIO -> "Médio";
            case DIFICIL -> "Difícil";
        };
    }
}
