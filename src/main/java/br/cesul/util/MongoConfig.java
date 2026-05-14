package br.cesul.util;

// Configuração mínima para conectar no mongodb local
// e expor as coleções que o projeto precisa

// Zero regra de negócio
// O objetivo desta classe é: estabelecer a conexapo
// com o banco e expor ao meu código o acesso
// ao banco de dados/coleção que usaremos

// Tudo aqui dentro é STATIC:
// - Existe só UMA conexão aberta durante a excução
// - Qualquer DAO pode chamar MongoConfi.players() ou
// - .questions() sem instanciar esta classe
// - Não há estado mutável.

import br.cesul.model.Dificuldade;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

public class MongoConfig {
    // Conexão única (Singleton implicito)
    // Como o create é um metodo pesado (abre socket...)
    // criamos apenas UMA instancia e reaproveitamos sempre
    private static final MongoClient CLIENT =
            MongoClients.create("mongodb://localhost:27017");

    // MongoDB funciona assim:
    // 1 - Estabelece a conexão com o servidor
    // 2 - Acesso determinado banco de dados
    // 3 - Acesso determinada coleção dentro do banco

    private static final String DB_NAME = "quiz_battle";

    // Helpers que retornam cada coleção específica.
    // Assim, os DAO's não precisam repetir o nome do banco

    private static MongoDatabase db(){
        return CLIENT.getDatabase(DB_NAME);
    }

    public static MongoCollection<Document> players(){
        return db().getCollection("players");
    }
    public static MongoCollection<Document> questions(){
        return db().getCollection("questions");
    }

    // Seed de perguntas (popula o banco na primeira execução)
    // Se a coleção questions estiver vazia, inserimos um
    // lote de perguntas prontas

    private static Document novaQuestao(String enunciado,
                                        List<String> alts,
                                        int correta,
                                        String categoria,
                                        String dificuldade){
        return new Document()
                .append("enunciado", enunciado)
                .append("alternativas", alts)
                .append("indiceCorreto", correta)
                .append("categoria", categoria)
                .append("dificuldade", dificuldade);
    }

    public static void seedQuestionsIfEmpty() {
        // countDocuments() devolve quantos documentos a coleção tem.
        // Se for > 0, significa que alguém (ex: execução anterior)
        // já populou — não fazemos nada.
        if (questions().countDocuments() > 0) return;

        // Cada Document representa UMA pergunta. Os campos batem
        // com os atributos do record Question.
        questions().insertMany(Arrays.asList(
                novaQuestao("Qual é a capital do Brasil?",
                        Arrays.asList("São Paulo", "Rio de Janeiro", "Brasília", "Salvador"),
                        2, "GERAL", "FACIL"),

                novaQuestao("Em que ano o Brasil foi descoberto?",
                        Arrays.asList("1492", "1500", "1502", "1498"),
                        1, "HISTORIA", "FACIL"),

                novaQuestao("Quem pintou a Mona Lisa?",
                        Arrays.asList("Van Gogh", "Picasso", "Da Vinci", "Michelangelo"),
                        2, "GERAL", "MEDIO"),

                novaQuestao("Qual o maior planeta do sistema solar?",
                        Arrays.asList("Terra", "Saturno", "Júpiter", "Netuno"),
                        2, "CIENCIA", "FACIL"),

                novaQuestao("Quantos ossos tem o corpo humano adulto?",
                        Arrays.asList("186", "206", "226", "196"),
                        1, "CIENCIA", "MEDIO"),

                novaQuestao("Quem foi o primeiro presidente do Brasil?",
                        Arrays.asList("Getúlio Vargas", "JK", "Deodoro da Fonseca", "Floriano Peixoto"),
                        2, "HISTORIA", "MEDIO"),

                novaQuestao("Qual seleção ganhou a Copa do Mundo de 2022?",
                        Arrays.asList("Brasil", "França", "Argentina", "Alemanha"),
                        2, "ESPORTES", "FACIL"),

                novaQuestao("Quantos jogadores compõem um time de vôlei em quadra?",
                        Arrays.asList("5", "6", "7", "8"),
                        1, "ESPORTES", "FACIL"),

                novaQuestao("Qual o elemento químico de símbolo 'Au'?",
                        Arrays.asList("Prata", "Ouro", "Alumínio", "Argônio"),
                        1, "CIENCIA", "MEDIO"),

                novaQuestao("Quem escreveu 'Dom Casmurro'?",
                        Arrays.asList("José de Alencar", "Machado de Assis", "Clarice Lispector", "Graciliano Ramos"),
                        1, "GERAL", "MEDIO"),

                novaQuestao("Qual o rio mais longo do mundo?",
                        Arrays.asList("Nilo", "Amazonas", "Yangtzé", "Mississipi"),
                        1, "GERAL", "DIFICIL"),

                novaQuestao("Em que ano caiu o Muro de Berlim?",
                        Arrays.asList("1987", "1989", "1991", "1985"),
                        1, "HISTORIA", "DIFICIL"),

                novaQuestao("Qual foi a primeira mulher a ganhar um Nobel?",
                        Arrays.asList("Marie Curie", "Rosalind Franklin", "Ada Lovelace", "Hedy Lamarr"),
                        0, "CIENCIA", "DIFICIL"),

                novaQuestao("Qual o velocista recordista mundial dos 100m rasos?",
                        Arrays.asList("Asafa Powell", "Tyson Gay", "Usain Bolt", "Justin Gatlin"),
                        2, "ESPORTES", "MEDIO"),

                novaQuestao("Em que ano o homem pisou na Lua pela primeira vez?",
                        Arrays.asList("1965", "1969", "1972", "1961"),
                        1, "HISTORIA", "MEDIO")
        ));
    }


    // Construtor privado
    // Impede que alguem faça um "new MongoConfig()"
    // por engano
    private MongoConfig(){}
}
