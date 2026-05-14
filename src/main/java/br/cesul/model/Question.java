package br.cesul.model;

import java.util.List;

// Representa UMA pergunta do quiz
// É tambem um recor porque uma vez carregada do banco
// não muda durante o jogo
public record Question (
        String id,
        String enunciado,
        List<String> alternativas,
        int indiceCorreto, // posição da alternativa na lista
        Categoria categoria,
        Dificuldade dificuldade
    )
{
    public boolean acertou(int indiceEscolhido){
        return indiceEscolhido == indiceCorreto;
    }

    public String textoCorreto(){
        return alternativas.get(indiceCorreto);
    }
}
