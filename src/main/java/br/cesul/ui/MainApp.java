package br.cesul.ui;

// A MainApp é o ponto de entrada e interface do projeto
// Só será executado ao dar run, o que estiver dentro do método 'main'


// Arquitetura/fluxo lógica:
// UI (MainApp) -> chama -> PlayerDao/QuestionDao -> usar o MongoConfig -> Conecta e manipula o banco de dados

// Modularidade. A UI NUNCA acessa o MongoDB, nem define campos de entidades

import br.cesul.dao.PlayerDao;
import br.cesul.dao.QuestionDao;
import br.cesul.model.Categoria;
import br.cesul.model.Player;
import br.cesul.model.Question;
import br.cesul.util.MongoConfig;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainApp extends Application {
    // Criação de objetos locais dos DAO's e regras de negócio
    private final PlayerDao playerDao = new PlayerDao();
    private final QuestionDao questionDao = new QuestionDao();
    private static final int QTD_PERGUNTAS = 5;

    // Criação de variáveis para controle do estado atual da partida
    private Player jogadorAtual; // Quem está jogando agora
    private List<Question> perguntasPartida;
    private int indicePergunta; //0..3, para saber qual é a resposta certa
    private int pontosPartida; // Quanto o jogador fez nesta rodada

    // Variáveis de controle de UI, aqui colocamos variáveis para todos os campos visuais
    // que terão alguma modificação no decorrer do runtime
    // Label = componente gráfico para mostrar text oem tela
    private Label lblEnunciado;
    private Label lblFeedback;
    private Label lblProgresso;
    private Button[] btnsAlternativas;
    private Button btnProxima;
    private ComboBox<Player> cboPlayer;
    private ComboBox<Categoria> cboCategoria;
    // A table view fornece a estrutura da tabela.
    // Nós precisamos forneceder uma lista com os dados para serem mostrados na tabela
    // Se eu criar uma List<>, preciso atualizar o componente da tela
    // para que os novos valores da lista apareçam
    private TableView<Player> tabelaRanking;
    // Se usarmos a ObservableList para popular a tabela, toda vez que a lista
    // for alterada localmente, a tela refletira a mudança (auto refresh)
    private ObservableList<Player> dadosRanking;

    // 1° coisa: Método start() que é o ponto de entrada do JFX
    // O objetivo do Stage é realizar a montagem do esqueleto da tela
    // juntamente com os componentes que queremos e ao final dela
    // pode incluir isso ao STAGE e dar stage.show();
    // A partir desse momento, nada mais acontece a não ser que
    // o usuário realize alguma ação.
    @Override
    public void start(Stage stage) throws Exception {
        // Realizar operações que devem ser aplicadas toda vez que o APP rodar (regras de negócio)
        MongoConfig.seedQuestionsIfEmpty();

        // Montagem das abas
        // Como utilizaremos 3 abas, faz sentido implementar o componente
        // TabPane:. Cada aba, ao ser clicada, alterará o conteúdo da tela de acordo com a sua tab
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                criarAbaJogar(),
                criarAbaRankind(),
                criarAbaNovoJogador()
        );

        // Configurado política para que não seja possível fechar uma tela
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene cena = new Scene(tabs, 800,600);
        stage.setScene(cena);
        stage.setTitle("Quiz Battle");

        stage.show();
    }

    // Método para criar a aba JOGAR
    // Fluxo> Escolher jogador + categoria -> clicar em iniciar -> responder as perguntas -> ver pontuação final
    private Tab criarAbaJogar() {
        // Primeiro declaramos os atributos que terão alteração, ou que precisamos acompanhar estado.

        // Combo de jogadores existentes
        cboPlayer = new ComboBox<>();
        cboPlayer.setPromptText("Selecione o jogador");
        recarregarComboJogadores();

        // Combo de Categorias (enum.values() te retorna todos)
        cboCategoria = new ComboBox<>(FXCollections.observableArrayList(Categoria.values()));
        cboCategoria.setPromptText("Categoria");
        cboCategoria.getSelectionModel().selectFirst(); // Seleciona o primeiro item da ComboBox

        // Botão iniciar partida
        javafx.scene.control.Button btnIniciar = new javafx.scene.control.Button("Iniciar partida");
        btnIniciar.setOnAction(e -> iniciarPartida());
    }

    private void recarregarComboJogadores() {
        Player antes = cboPlayer == null ? null : cboPlayer.getValue();
        List<Player> todos = playerDao.findAllOrderedByScore();

        if (cboPlayer != null) {
            cboPlayer.setItems(FXCollections.observableArrayList(todos)); // Cast de List para ObservableList

            if (antes != null) {
                // Tentar re-selecionar o mesmo jogador (Caso não tenha sido apagadao)
                // A stream possibilita que façamos operações com for's aninhados a partir de uma lista inicial
                // Por que precisamos disso?
                // Porque logo a seguir, eu preciso aplicar um filtro em todos os items da lista
                // Depois do filter, só se passam para a próxima linha os items que dada regra do filter, retornem true
                // Depois, com o findFirst, separamos apenas o primeiro item da lista resultante do filtro
                // E por último se o dado que chegou até aqui não for null, aplicamos um método em um componente
                // passando esse valor de forma implícita.
                todos.stream().filter(player -> player.id().equals(antes.id())).findFirst().ifPresent(cboPlayer::setValue);
            }
        }

    }

    private void iniciarPartida() {
        jogadorAtual = cboPlayer.getValue();
        if (jogadorAtual == null) {
            lblEnunciado.setText("Selecione um jogador antes de começar!");
            return;
        }
        Categoria cat = cboCategoria.getValue();
        perguntasPartida = questionDao.sortearPartida(cat, QTD_PERGUNTAS);
        if (perguntasPartida.isEmpty()) {
            lblEnunciado.setText("Sem perguntas disponíveis para esta categoria");
            return;
        }

        indicePergunta = 0;
        pontosPartida = 0;
        lblFeedback.setText("");
        mostrarPerguntaAtual();
    }

    private void mostrarPerguntaAtual() {
        Question q = perguntasPartida.get(indicePergunta);
        lblProgresso.setText(
                "Pergunta " + (indicePergunta + 1) + "/" + (perguntasPartida.size() + 1)
                + "  |  " +  q.categoria().rotulo()
                + "  *  " + q.dificuldade().rotulo()
                + "(" + q.dificuldade()
        );
    }

    public static void main(String[] args ) {
        launch(args);
    }
}
