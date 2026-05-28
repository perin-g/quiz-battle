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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private javafx.scene.control.Label lblEnunciado;
    private javafx.scene.control.Label lblFeedback;
    private javafx.scene.control.Label lblProgresso;
    private javafx.scene.control.Button[] btnsAlternativas;
    private javafx.scene.control.Button btnProxima;
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
                criarAbaJogar()
//                criarAbaRankind(),
//                criarAbaNovoJogador()
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

        // "Caixa" horizontal que armazena componentes lado a lado na linha
        HBox topo = new HBox(
                // Primeiro parâmetro é o espaçamento entre itens da HBox
                8,
                new javafx.scene.control.Label("Jogador: "), cboPlayer,
                new javafx.scene.control.Label("Categoria: "), cboCategoria,
                btnIniciar
        );

        topo.setAlignment(Pos.CENTER_LEFT);
        topo.setPadding(new Insets(10));

        // Área da pergunta
        lblProgresso = new javafx.scene.control.Label("-");
        lblProgresso.setStyle("-fx-text-fill:#666");

        lblEnunciado = new Label("Clique em 'Inicia partida' para começar");
        lblEnunciado.setWrapText(true);
        lblEnunciado.setStyle("-fx-font-size:16; -fx-font-weight:bold;");

        // Criando 4 instâncias de botão
        btnsAlternativas = new Button[4];
        VBox boxAlternativas = new VBox(6);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            Button b = new Button();
            // Define que o botão ocupa toda a largura disponível na tela
            b.setMaxWidth(Double.MAX_VALUE);
            b.setDisable(true);
            b.setOnAction(e -> responder(idx));
            btnsAlternativas[i] = b;
            boxAlternativas.getChildren().add(b);
        }

        lblFeedback = new Label();
        lblFeedback.setStyle("-fx-font-wight:bold;");

        // Criar uma nova instância de Button
        // Setar que está desabilitado
        // Setar o onAction para um metodo chamado avancarPergunta()
        btnProxima = new Button("Próxima");
        btnProxima.setDisable(true);
        btnProxima.setOnAction(e -> avancarPergunta());

        HBox boxFeedback = new HBox(
                10,
                lblFeedback,
                btnProxima
        );

        boxFeedback.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(
                12,
                topo,
                lblProgresso,
                lblEnunciado,
                boxAlternativas,
                boxFeedback
        );

        conteudo.setPadding(new Insets(10));

        Tab tab = new Tab("Jogar", conteudo);
        return tab;
    }

    // Será chamado por qualquer botão de alternativa que o usuário clicar
    // Marcar certo/errado, atualizar os pontos e liberar o botão "prox"
    private void responder(int indiceEscolhido) {
        //Desabilitar todos os botões para impedir re-clique
        for (Button b : btnsAlternativas) b.setDisable(true);

        Question q = perguntasPartida.get(indicePergunta);
        if (q.acertou(indiceEscolhido)){
            pontosPartida += q.dificuldade().getPontos();
            lblFeedback.setStyle("-fx-text-fill:#00FF00; -fx-fon-wight:bold;");
            lblFeedback.setText("V Acertou! + "+ q.dificuldade().getPontos() + " pts");
            btnsAlternativas[indiceEscolhido].setStyle("-fx-background-color:#ACD8A7;");
        } else {
            lblFeedback.setStyle("-fx-text-fill:#FF0000; -fx-fon-wight:bold;");
            lblFeedback.setText("X Errou!. Resposta:  "+ q.textoCorreto());
            btnsAlternativas[indiceEscolhido].setStyle("-fx-background-color:#D8A7A7;");
            btnsAlternativas[q.indiceCorreto()].setStyle("-fx-background-color:#ACD8A7;");
        }
        btnProxima.setDisable(false);
    }

    // Se o jogo tiver terminado, ou seja, o perguntasPartida.size() for <= indicePergunta
    // finalize a partida (chama o método)
    // Se ainda tiverem perguntas para serem mostradas, came o método mostrarPerguntaAtual()
    // incrementando o indicePergunta
    private void avancarPergunta() {
        indicePergunta++;
        if (perguntasPartida.size() <= indicePergunta) {
            finalizarPartida();
        } else {
            mostrarPerguntaAtual();
        }

        lblFeedback.setStyle("");
    }

    // 1 - Salvar os pontos no MongoDB;
    // 2 - Mostrar quantos pontos fez na tela;
    // 3 - Limpar a tela para a próxima partida;
    private void finalizarPartida(){
        playerDao.registrarPartida(jogadorAtual.id(), pontosPartida);

        lblProgresso.setText("Partida encerrada");
        lblEnunciado.setText(jogadorAtual.nome() + " você fez + " + pontosPartida + " Pontos!");

        for(Button b : btnsAlternativas) {
            b.setText("");
            b.setDisable(true);
            b.setStyle("");
        }

        lblFeedback.setText("Vá para a aba ranking para ver sua posição");
        lblFeedback.setStyle("-fx-text-fill:#333");

        // Atualiza listas locais
        recarregarComboJogadores();
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
                + "(" + q.dificuldade().getPontos() + " pts)"
        );

        lblEnunciado.setText((q.enunciado()));

        for (int i = 0; i < btnsAlternativas.length; i++) {
            javafx.scene.control.Button b = btnsAlternativas[i];
            if (i < q.alternativas().size()) {
                b.setText((char)('A' + i) + ") " + q.alternativas().get(i));
            } else {
                b.setText("");
                b.setDisable(true);
                b.setStyle(""); // Limpar a cor anterior
            }
        }
        btnProxima.setDisable(true);
    }

    public static void main(String[] args ) {
        launch(args);
    }
}
