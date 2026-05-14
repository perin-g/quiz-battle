package br.cesul.dao;

import br.cesul.model.Categoria;
import br.cesul.model.Dificuldade;
import br.cesul.model.Question;
import br.cesul.util.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

// Objeto de acesso de dados (Data Access Object) para a coleção 'questions'
// Tarefa principal: entregar perguntas prontas para a main usar na partida
public class QuestionDao {
    private final MongoCollection<Document> col = MongoConfig.questions();

    private Question toQuestion(Document d){
        List<String> alts = d.getList("alternativas", String.class); // String.class garante que todos os itens da lista vão ser objetos de String
        return new Question(
                d.getObjectId("_id").toHexString(),
                d.getString("enunciado"),
                alts,
                d.getInteger("indiceCorreto", 0),
                Categoria.valueOf(d.getString("categoria")),
                Dificuldade.valueOf(d.getString("dificuldade"))
        );
    }

    // Faça um método que retorne uma lista de questions
    // chamado findAll()

    public List<Question> findAll(){
        List <Question> list = new ArrayList<>();
        for(Document d : col.find()){
            list.add(toQuestion(d));
        }
        return list;
    }

    // Listagem por categoria
    // Faça um método que receba uma categoria da main,
    // retorne uma lista de questions de apenas as perguntas do banco que sejam dessa categoria
    public List<Question> findByCategoria(Categoria categoria){
        // 1 - Criar uma lista de questions vazia
        List<Question> list = new ArrayList<>();
        // 2 - Iterar num for o retorno do col.find
        // 2.1 - Adicionar como parâmetro do find o filtro eq da categoria
        for(Document d : col.find(eq("categoria", categoria.name()))){
            // 3 Dentro do for, adicionar na lista
            list.add(toQuestion(d));
        }
        // 4 - retorno a lista
        return list;
    }

    // Método para sortear X perguntas aleatórias
    // Estratégia
    // - Carregar TODAS as perguntas de uma categoria
    // - Devolver só as X primeiras
    public List<Question> sortearPartida(Categoria categoria, int quantas){
        // EXPRESSÃO ? SE TRUE : SE FALSE
        List<Question> pool = categoria == null ? findAll() : findByCategoria(categoria);

        // Sheffle é um método estático que randomiza a lista e grava nela mesma a nova configuração
        Collections.shuffle(pool);

        // Se pedirem mais perguntas do que existem, devolvemos só o que temos
        // min (outro método estático) retorna o menor valor entre os parametros
        int limite = Math.min(quantas, pool.size());

        return new ArrayList<>(pool.subList(0, limite));
    }
}
