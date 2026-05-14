package br.cesul.model;

// Categoria de uma pergunta no sistema
// Este arquivo representa a ENTIDADE
// do objeto que estamos trabalhos

// O model representa a entidade do banco
// de dados

// Usamos um ENUM (ao inves de um simples string) porque::
// - O conjunto de valores é FINITO e FIXO (só tem 4)
// - Usando enum, o compilador nos protege contra
//  digitação errada ("ciencia" vs "Ciência")
// - É mais facil de usar switch-case de forma limpa
// - A "entidade" que estou trabalho SÓ tem um texto.
public enum Categoria {
    // Voce pode colocar num enum constantes
    // em cada linha declara uma instância UNICA,
    // é convenção usar MAIUSCULO
    HISTORIA,
    CIENCIA,
    ESPORTES,
    GERAL;

    // Enums podem ter métodos
    public String rotulo(){
        return switch(this){
            case HISTORIA -> "História";
            case CIENCIA -> "Ciência";
            case ESPORTES -> "Esportes";
            case GERAL -> "Geral";
        };
    }
}
