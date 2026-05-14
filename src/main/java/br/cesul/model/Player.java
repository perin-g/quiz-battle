package br.cesul.model;

// Representa UM jogador do quiz
// Esta classe será um dado que não sera mudado
// depois de pronto: quando quisermos alterar
// (ex: somar pontos), eu crio um NOVO player
// em vez de mudar o antigo.

// Este padrão tem um nome: Imutabilidade
// Desta forma consigo manter um rastreamento de todos
// os estados que determinado player teve durante a
// execução

// Record: Gera todo boilerplate automaticamente
// pelo compilador

public record Player (
        String id, // Será o id gerado pelo MongoDB
        String nome, // Digitado pelo usuario na tela
        int pontuacaoTotal, // Pontos acumulados nas partidas
        int partidasJogadas) // QUantas partidas ele jogou
{
    // Método derivado (opcional em record)
    // Records podem ter métodos normais alem dos getters e
    // setters gerados automaticamente.

    // Calculamos a média de pontos por partida
    // Se partidasJogadas == 0, evitamos a divisão por 0,
    // retornando 0
    public double mediaPorPartida(){
//        if(partidasJogadas == 0){
//            return 0;
//        }else{
//            pontuacaoTotal / partidasJogadas;
//        }
        return partidasJogadas == 0 ? 0.0 : pontuacaoTotal / (double) partidasJogadas;
    }
}
