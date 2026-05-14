package br.cesul.dao;

// DAO -> Data Access Object para a coleção de players
// Esta camada é a conversa com o banco
// A regra é: A UI jamais acessa o Mongo diretamente
// Ela chama o DAO, e o DAO converte
// de Player <-> Document

import br.cesul.model.Player;
import br.cesul.util.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.util.ArrayList;
import java.util.List;

// Esse é um import para auxiliar na COMPARAÇÃO de uma variável que veio do Java
// com alguma variável de algum documento do MongDB. Util em Updates
import static com.mongodb.client.model.Filters.eq;
// Serve para auxiliar a incrementação de valores salvos em documentos do MongoDB
import static com.mongodb.client.model.Updates.inc;
// Serve para auxiliar a alteração de diferentes campos de documentos do MongoDB
import static com.mongodb.client.model.Updates.combine;

// Porque? Porque se amanha trocarmos o Mongo
// pelo PostreSQL, só este arquivo muda, e a UI fica
// igual
public class PlayerDao {
    // Referência à COLEÇÃO
    private final MongoCollection<Document> col =
            MongoConfig.players();

    // Serialização
    private Player toPlayer(Document d){
        return new Player(
                d.getObjectId("_id").toHexString(),
                d.getString("nome"),
                d.getInteger("pontuacaoTotal", 0),
                d.getInteger("partidasJogadas", 0)
        );
    }

    // Insert - Inserir um novo Jogador
    // Recebe apenas o nome, os demais campos começam em 0
    // Se vier nulo ou vazio, ignora.
    public void insert(String nome){
        if (nome == null || nome.isBlank()) return;

        col.insertOne(new Document()
                .append("nome", nome)
                .append("pontuacaoTotal", 0)
                .append("partidasJogadas", 0)
        );
    }

    // Read(get, fetch) - Lista todos os jogadores
    // ordenados por pontuação desc
    public List<Player> findAllOrderedByScore(){
        // percorrer todos os doc da coleção
        // para cada doc chama toPlayer(doc)
        // ordenar
        List<Player> list = new ArrayList<>();
        for(Document d : col.find().sort(Sorts.descending("pontuacaoTotal"))){
            list.add(toPlayer(d));
        }
        return list;
    }

    // UPDATE - Somar pontos à pontuação e incrementar as partidas
    // é uma operação 'fim de partida'. O MainApp vai chama-lo quando o jogador terminar o quiz
    // passando a pontuação que ele fez
    public void registrarPartida(String id, int pontosGanhos){
        ObjectId oid = new ObjectId(id);

        col.updateOne(
                // 1 - Qual é o ID que estou procurando?
                eq("_id", oid),
                // 2 - O que quero atualizar?
                combine(
                        inc("pontuacaoTotal", pontosGanhos),
                        inc("partidasJogadas", 1)
                )

        );
    }
}
