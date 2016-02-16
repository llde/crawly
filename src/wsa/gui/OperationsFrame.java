package wsa.gui;

import javafx.application.Platform;
import javafx.beans.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import wsa.API.Wrap;
import wsa.elaborazioni.*;
import wsa.exceptions.EventFrame;
import wsa.session.DataGate;
import wsa.session.GestoreDownload;
import wsa.session.Page;

import javax.naming.OperationNotSupportedException;
import java.net.URI;
import java.util.*;

/**
 * Created by gaamda on 13/12/15.
 *
 * Una finestra per azioni tra visite, gestiste tutte le richieste di elaborazioni.
 */
public class OperationsFrame {

    private List<Stage> childrens = new ArrayList<>();   //La lista degli stages delle operazioni
    private Stage primaryStage = new Stage();
    {
        primaryStage.setOnCloseRequest(event -> {
            childrens.forEach(Stage::close);
        });
    }
    private Scene rootScene = null;

    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./moreVisitsFrame.fxml"));
            loader.setController(this);
            rootScene = new Scene(loader.load());
            primaryStage.setScene(rootScene);
        }catch (Exception ex){
            new EventFrame(ex, null);
        }
    }

    /**
     * Crea una finestra per le operazioni tra visite.
     * Nota a margine: Sarebbe stato molto bello definire un'interfaccia dove, fornite delle standard basi (come la
     * lista degli uri, e così via), automaticamente si sarebbe definito un approccio a schede singole fornendo
     * un'interfaccia per la gestione di un calcolo computazionale su uri.
     * Personalmente avrei voluto fornire l'interfaccia per il comparto grafico, e l'interfaccia per i calcoli.
     * Se il task di computazione ha definita l'interfaccia, si inserisce come scheda, se no, come voce di calcolo
     * in un menù a tendina. Per scopo delle specifiche però, l'espandibilità in tal senso dell'applicazione
     * non era certamente contemplata, e l'approccio a classi concrete è decisamente più semplice.
     * (Riguardo allo scopo da voler raggiungere, ma più che altro per limiti di spazio).
     */
    public OperationsFrame(){

        Platform.runLater(()-> {
            comboDa.setItems(MainFrame.getMainFrame().getTabs());
            comboVerso.setItems(MainFrame.getMainFrame().getTabs());
            comboVisita.setItems(MainFrame.getMainFrame().getTabs());
            comboVisita.getSelectionModel().select(0);
        });

        primaryStage.setTitle("Operazioni");

        if (MainFrame.getMainFrame().getTabs().size() < 2){
            tabIdentificazione.setDisable(true);
        }else{
            tabIdentificazione.setDisable(false);
        }

        MainFrame.getMainFrame().getTabs().addListener((InvalidationListener) observable -> {
            if (MainFrame.getMainFrame().getTabs().size() < 2){
                tabIdentificazione.setDisable(true);
            }else{
                tabIdentificazione.setDisable(false);
            }
        });

    }

    /**
     * Prova a selezionare una tab di questo controllo senza però garantirne il risultato.
     * @param n il valore da selezionare.
     */
    public void select(int n){
        try{
            tabPaneDistanza.getSelectionModel().select(n);
        }catch (Exception ignored){} // Volutamente ignorata.
    }

    public void show(){
        primaryStage.setTitle("Azioni su visite.");
        primaryStage.show();
        primaryStage.requestFocus();
    }

    @FXML
    private Tab tabIdentificazione;
    @FXML
    private ComboBox<TabFrame> comboDa;
    @FXML
    private ComboBox<TabFrame> comboVerso;
    @FXML
    private ComboBox<Page> comboDistanzaDa;
    @FXML
    private ComboBox<Page> comboDistanzaVerso;
    @FXML
    private ComboBox<TabFrame> comboVisita;
    {
        comboVisita.setOnAction(event -> {
            comboDistanzaDa.setItems(comboVisita.getSelectionModel().getSelectedItem().getData());
            comboDistanzaVerso.setItems(comboVisita.getSelectionModel().getSelectedItem().getData());
        });
    }

    @FXML
    Button buttonCalcola;
    {
        buttonCalcola.setOnAction(event -> {
            if (comboDa.getSelectionModel().getSelectedItem() == null || comboVerso.getSelectionModel().getSelectedItem() == null){
                new EventFrame(event, Alert.AlertType.WARNING, "Scegliere delle visite valide", ButtonType.CANCEL, null);
                return;
            }

            if (!comboDa.getSelectionModel().getSelectedItem().isPaused() || !comboVerso.getSelectionModel().getSelectedItem().isPaused()){
                new EventFrame(event, Alert.AlertType.WARNING, "Terminare le visite interessate\n" +
                        "prima di procedere.", ButtonType.CANCEL, null);
                return;
            }

            DistanzaVisiteFrame dst = new DistanzaVisiteFrame(comboDa.getSelectionModel().getSelectedItem(), comboVerso.getSelectionModel().getSelectedItem());
            dst.show();
            dst.execute();

        });
    }
    @FXML
    Button buttonDistanzaMaxCalcola;
    {
        buttonDistanzaMaxCalcola.setOnAction(event -> {

            if (!comboVisita.getSelectionModel().getSelectedItem().isPaused()){
                new EventFrame(event, Alert.AlertType.WARNING, "Terminare le visite interessate\n" +
                        "prima di procedere.", ButtonType.CANCEL, null);
                return;
            }

            new EventFrame(event, Alert.AlertType.WARNING, "Questo calcolo richiede molto tempo,\n" +
                    "calcolerà le distanze tra tutte le coppie\n" +
                    "di pagine. Continuare?", ButtonType.CANCEL, ()->{
                DistanzaCoppieFrame dst = new DistanzaCoppieFrame(comboVisita.getSelectionModel().getSelectedItem().getWorker().getDataStruct());
                dst.show();
                dst.execute();
            });
        });
    }
    @FXML
    Button buttonDistanzaCalcola;
    {
        buttonDistanzaCalcola.setOnAction(event -> {
            if (comboDistanzaDa.getSelectionModel().getSelectedItem() == null || comboDistanzaVerso.getSelectionModel().getSelectedItem() == null){
                new EventFrame(event, Alert.AlertType.WARNING, "Scegliere delle visite valide", ButtonType.CANCEL, null);
                return;
            }
            if (!comboVisita.getSelectionModel().getSelectedItem().isPaused()){
                new EventFrame(event, Alert.AlertType.WARNING, "Terminare le visite interessate\n" +
                        "prima di procedere.", ButtonType.CANCEL, null);
                return;
            }

            DistanzaURIFrame dst = new DistanzaURIFrame(comboVisita.getSelectionModel().getSelectedItem().getDatamap(),
                    comboDistanzaDa.getSelectionModel().getSelectedItem(),
                    comboDistanzaVerso.getSelectionModel().getSelectedItem()
                    );
            dst.show();
            dst.execute();
        });

    }
    @FXML
    TabPane tabPaneDistanza;
    @FXML
    Label labelCalcolo;
    {
        labelCalcolo.setVisible(false);
    }

    /**
     * Finestra per il calcolo del numero dei links da una visita A ad una B.
     * Riferirsi alle specifiche per la richiesta, oppure al javadoc.
     */
    private class DistanzaVisiteFrame extends WaitingWindow{

        private TaskUriAB calcolo = null;

        /**
         * Genera una finestra, si occupa di dare gli argomenti per il Task
         */
        public DistanzaVisiteFrame(TabFrame A, TabFrame B){
            super("Verso " + B, "Calcolo della distanza in corso");
            calcolo = new TaskUriAB(A, B);
            childrens.add(this.getStage());
            this.setOnClose(event -> {
                try {
                    calcolo.requestInterrupt();
                } catch (OperationNotSupportedException e) {
                    e.printStackTrace();
                }finally {
                    childrens.remove(this.getStage());
                }
            });
            calcolo.getWorkerState().addListener((observable, oldValue, newValue) -> {    //finito il calcolo, mostra i risultati
                if (newValue == Worker.State.SUCCEEDED){
                    ListView<URI> lstURI = new ListView<>();
                    lstURI.setItems(calcolo.getData());
                    Platform.runLater(() -> {
                        this.getStage().setScene(new Scene(lstURI));
                    });
                }else{
                    this.getStage().close();
                    new EventFrame(null, Alert.AlertType.WARNING, "Impossibile eseguire il calcolo.", ButtonType.CANCEL, () -> primaryStage.close());
                }

            });
        }

        void execute(){
            Executor.perform(calcolo);
        }
    }

    /**
     * Finestra di calcolo della distanza tra due URI interni allo stesso dominio.
     */
    private class DistanzaURIFrame extends WaitingWindow{

        TaskDistanceUri calcolo = null;

        /**
         * Genera una finestra, si occupa di dare gli argomenti per il Task
         */
        public DistanzaURIFrame(Map<URI, Page> mapToWatch, Page start, Page ending){
            super("Distanza", "Calcolo distanza tra \n" +
                    "da: " + start + "\n" +
                    "verso: " + ending + "\n" +
                    "in corso...");
            calcolo = new TaskDistanceUri(mapToWatch, start, ending);
            childrens.add(this.getStage());
            this.setOnClose(event -> {
                calcolo.requestInterrupt();
                childrens.remove(this.getStage());
            });
            calcolo.getWorkerState().addListener((observable, oldValue, newValue) -> {    //finito il calcolo, mostra i risultati
                if (newValue == Worker.State.SUCCEEDED){
                    VBox vBox = new VBox();
                    Label lbl = new Label("Distanza tra \n" +
                            start + " e\n" +
                            ending + "\n" +
                            calcolo.getData());
                    vBox.getChildren().add(lbl);
                    VBox.setMargin(lbl, new Insets(30, 30, 30, 30));
                    Platform.runLater(() -> this.getStage().setScene(new Scene(vBox)));
                }else{
                    Platform.runLater(() -> new EventFrame(null, Alert.AlertType.WARNING, "Impossibile eseguire il calcolo. Bad return: -1.", ButtonType.CANCEL, () -> this.getStage().close()));
                }

            });
        }

        void execute(){
            Executor.perform(calcolo);
        }
    }

    /**
     * Finestra di calcolo della distanza tra tutte le coppie di URIs.
     */
    private class DistanzaCoppieFrame extends WaitingWindow{

        private TaskDistanceAllUrisPages calcolo = null;

        /**
         * Genera una finestra, si occupa di dare gli argomenti per il Task
         */
        public DistanzaCoppieFrame(DataGate gd){
            super("Distanza per coppie", "Calcolo distanza tra \n" +
                    "tutte le coppie di pagine...");
            this.setComputeLabel("Calcolo computo...");
            calcolo = new TaskDistanceAllUrisPages(gd);
            childrens.add(this.getStage());
            this.setOnClose(event -> {
                calcolo.requestInterrupt();
                childrens.remove(this.getStage());
            });
            this.setOnCancel(event -> {
                calcolo.requestInterrupt();
                onSuccess(true);
            });

            try {
                this.hideComputeLabel();
            }catch (Exception ex){
                new EventFrame(ex, ()->{
                    calcolo.requestInterrupt();
                    childrens.remove(this.getStage());
                    this.getStage().close();
                });
            }

            calcolo.getWorkerState().addListener((observable, oldValue, newValue) -> { //finito il calcolo, costituisce una tabella per mostrare i risultati
                if (newValue == Worker.State.SUCCEEDED){
                    onSuccess(false);
                }else{
                    onSuccess(true);
                }
            });
        }

        private void onSuccess(boolean partial){
            VBox vBox = new VBox();
            TableView<Wrap<Integer, URI[]>> visualTable = new TableView<>();

            TableColumn<Wrap<Integer, URI[]>, Integer> distanceNumber = new TableColumn<>();
            TableColumn<Wrap<Integer, URI[]>, URI> uriA = new TableColumn<>();
            TableColumn<Wrap<Integer, URI[]>, URI> uriB = new TableColumn<>();

            distanceNumber.setCellValueFactory(param -> param.getValue().key);
            uriA.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().val.get()[0]));
            uriB.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().val.get()[1]));

            visualTable.getColumns().add(distanceNumber);
            visualTable.getColumns().add(uriA);
            visualTable.getColumns().add(uriB);

            calcolo.getData().forEach((k,v) -> {     //Aggiunge alla visualTable i valori elaborati dal Task, separando le coppie di uri che possono aver avuto la stessa distanza (erano in un Set).
                v.forEach(setWrap -> {
                    visualTable.getItems().add(new Wrap<>(k, new URI[]{setWrap.key.get(), setWrap.val.get()}));
                });
            });

            Label lbl;
            if (!partial) {
                lbl = new Label("La Distanza massima è: " + calcolo.getData().keySet().stream().max(Comparator.naturalOrder()).get() +
                        "\nAnalizzate " + visualTable.getItems().size() + " coppie.");
            }else{
                lbl = new Label("Dati incompleti, l'algoritmo\n" +
                        "non ha completato.");
            }

            vBox.getChildren().addAll(lbl, visualTable);
            VBox.setMargin(lbl, new Insets(5, 5, 5, 5));
            Platform.runLater(() -> {
                this.getStage().setScene(new Scene(vBox));
            });

        }

        void execute(){
            Executor.perform(calcolo);
        }
    }

}


