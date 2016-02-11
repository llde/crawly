package wsa.gui;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wsa.API.Wrap;
import wsa.Settings;
import wsa.exceptions.DominioException;
import wsa.exceptions.EventFrame;
import wsa.session.Dominio;
import wsa.session.GestoreDownload;
import wsa.session.Page;
import wsa.session.Seed;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gaamda on 22/11/15.
 *
 * Una tab estesa per accomodare la visita di crawling. Fornisce tutto il necessario e genera, passati
 * gli argomenti, una nuova visita. Estende tab per compatibilità in JavaFX. Gestisce due grafici di sessione.
 * @see Tab
 */
public class TabFrame extends Tab {

    // Il dominio, il percorso di visita, il gestore download, la scena di root.
    private Dominio dom;
    private Path path;
    private GestoreDownload gd;
    private Scene rootScene;


    //Variabili di utilità
    private boolean pausedByUser = false;
    private boolean paused = false;
    /*
    Le dichiarazioni a seguire servono per le tabelle dei grafici, definite programmaticamente.
     */
    private TableView<Wrap<String, Integer>> entersTable = new TableView<>();
    private TableView<Wrap<String, Integer>> exitTable = new TableView<>();
    private TableColumn<Wrap<String, Integer>, String> classEnters = new TableColumn<>("n° links");
    private TableColumn<Wrap<String, Integer>, Integer> valueEnters = new TableColumn<>("pagine");
    private TableColumn<Wrap<String, Integer>, String> classExit = new TableColumn<>("n° links");
    private TableColumn<Wrap<String, Integer>, Integer> valueExit = new TableColumn<>("pagine");
    Stage entST = new Stage();
    Stage exitST = new Stage();

    {
        entST.setTitle("Links entranti");
        exitST.setTitle("Links uscenti");
        entersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        exitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        classEnters.setCellValueFactory(param -> param.getValue().key);
        valueEnters.setCellValueFactory(param -> param.getValue().val);
        classExit.setCellValueFactory(param -> param.getValue().key);
        valueExit.setCellValueFactory(param -> param.getValue().val);
        entersTable.getColumns().add(classEnters);
        entersTable.getColumns().add(valueEnters);
        exitTable.getColumns().add(classExit);
        exitTable.getColumns().add(valueExit);
        entST.setScene(new Scene(entersTable));
        exitST.setScene(new Scene(exitTable));
    }
    private ObservableList<Wrap<String, Integer>> entersWrapList = FXCollections.observableArrayList();
    private ObservableList<Wrap<String, Integer>> exitWrapList = FXCollections.observableArrayList();

    /*
    Il menù contestuale si adatterà una volta che l'utente lo userà cliccando col
    tasto destro del mouse su una riga della tabella principale.
     */
    private ContextMenu contestuale = new ContextMenu();

    /*
    Loading della GUI.
     */
    {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./tabFrame.fxml"));
            loader.setController(this);
            rootScene = new Scene(loader.load());
            this.setContent(rootScene.getRoot());
        }catch (Exception ex){
            ex.printStackTrace();
            new EventFrame(ex, Platform::exit); // Inutile continuare se non si carica questa GUI.
        }
    }

    /**
     * Un tabFrame rappresenta il contenuto di una tab.
     * Accomoda una visita nelle sue componenti principali.
     * @param dominio Il dominio di visita.
     * @param s La lista di seeds.
     * @param p Il percorso di salvataggio/recupero visita.
     * @param m Il modulo per i grafici.
     * @param run flag di run. Se true avvia direttamente l'esplorazione.
     */
    public TabFrame(Dominio dominio, List<Seed> s, Path p, Integer m, boolean run){

        this.dom = dominio;   // Il dominio di visita
        this.path = p;  // Il path di visita
        this.setText("Preparazione visita in corso");
        Thread t = new Thread(() -> {
            try {
                this.gd = new GestoreDownload(dominio, s, path, m, this);  // Inizializza gestore.
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(()-> this.setText("Preparazione visita fallita"));
                //TODO notify the users about IOExceptions.
            }
            if(dominio == null) this.dom = Dominio.getDomainSneaky(gd.getDomain().toString());
            Platform.runLater(() -> this.setText(dom != null ? dom.toString() : "Unknow dom"));
            this.tableData.setItems(gd.getDataList());  // Inizializza items tabella.

        /*
        Inizializza le label per le statistiche, inoltre aggiunge un invalidation listner per
        aggiornarle.
        */

            tableData.getItems().addListener((InvalidationListener) observable -> {
                long visitati = tableData.getItems().stream().filter(page -> page.getExc() == null).count();
                long errori = tableData.getItems().stream().filter(page -> page.getExc() != null).count();
                long indom = tableData.getItems().stream().filter(Page::getPageLink).count();
                Platform.runLater(() -> {
                    labelVisitati.setText(String.valueOf(visitati));
                    labelErrori.setText(String.valueOf(errori));
                    labelDominio.setText(String.valueOf(indom));
                });
                if (gd.getPageWithMaxPointers().get() != null && dominio == null) {                                           //in caso di recupero visita
                    Platform.runLater(() -> {
                        labelMaxPointers.setText(gd.getPageWithMaxPointers().getValue().ptrNumbers().toString());
                        Tooltip tt = new Tooltip(gd.getPageWithMaxPointers().getValue().getURI().toString());
                        labelMaxPointers.setTooltip(tt);
                    });
                }
            });

        /*
        Aggiunge un listner per il cambiamento dei massimi nelle strutture dati.
        Questi calcoli vengono eseguiti a livello di Gestore Download, qui sono solo riportati.
        Ci sono anche dei Tooltip per mostrare quali pagine abbiano più puntati/putannti.
         */

            gd.getPageWithMaxLinks().addListener(observable -> {
                labelMaxLinks.setText(gd.getPageWithMaxLinks().getValue().linksNumber().toString());
                Tooltip tt = new Tooltip(gd.getPageWithMaxLinks().getValue().getURI().toString());
                labelMaxLinks.setTooltip(tt);
            });

            gd.getPageWithMaxPointers().addListener(observable -> {
                labelMaxPointers.setText(gd.getPageWithMaxPointers().getValue().ptrNumbers().toString());
                Tooltip tt = new Tooltip(gd.getPageWithMaxPointers().getValue().getURI().toString());
                labelMaxPointers.setTooltip(tt);
            });

        /*
        Setta i grafici di sessione.
        Setta anche dei changeListner per aggiornarli, usa dei Wrapper.
         */
            this.pieEntranti.setData(gd.getEntrantiPieData());
            this.pieUscenti.setData(gd.getUscentiPieData());
            this.entersTable.setItems(entersWrapList);
            this.exitTable.setItems(exitWrapList);

            gd.getEntrantiPieData().addListener((InvalidationListener) observable -> {
                entersWrapList.clear();
                if (gd != null)
                    gd.getEntrantiPieData().forEach(data -> entersWrapList.add(new Wrap<>(data.getName(), (int) data.getPieValue())));
            });
            gd.getUscentiPieData().addListener((InvalidationListener) observable -> {
                exitWrapList.clear();
                if (gd != null)
                    gd.getUscentiPieData().forEach(data -> exitWrapList.add(new Wrap<>(data.getName(), (int) data.getPieValue())));
            });
            if (run) this.run(); /* Avvia la visita se true */
        });
        t.start();
        /*
        In chiusura, per una felice chiusura, rimuove se stessa dalla lista delle tabs principale.
         */
        this.setOnCloseRequest(e -> {
            MainFrame.getMainFrame().getTabs().remove(this);
            new EventFrame(e, Alert.AlertType.WARNING,
                    "Chiudere la visita definitivamente?\n" +
                            "Tutti i dati non salvati saranno perduti.",
                    ButtonType.CANCEL,
                    this::dispose, ()-> MainFrame.getMainFrame().getTabs().add(this));
        });
    }

    /**
     * Ritorna il gestoreDownload associato a questa Tab.
     * @return Il gestore download, o null.
     */
    GestoreDownload getWorker(){return this.gd;}

    /**
     * Ritorna lo stato di visita come enumerazione.
     * Lo stato della visita è lo stato del gestore download contenuto.
     * @return Lo stato come enumerazione di Worker.
     * @see javafx.concurrent.Worker.State
     * @see wsa.session.StatoDownload
     */
    public  Worker.State getStato(){
        return gd.getStato();
    }

    /**
     * Cancella il gestore download, non sarà più utilizzabile.
     * Attenzione nell'uso.
     */
    private void cancel(){
        if (gd == null) return;
        if (gd.getStato() != null && gd.getStato() == Worker.State.CANCELLED) return;
        gd.cancel();
        Platform.runLater(() ->{
            pausa.setDisable(true);
            cancella.setDisable(true);
        });
    }

    /**
     * Indica se la tab è in pausa.
     * @return True se pausa o false altrimenti.
     */
    public boolean isPaused(){
        if (!paused && !pausedByUser) return false;
        return true;
    }


    /**
     * Mette in pausa il gestore dei download.
     */
    public void pause(){
        if (gd == null) return;
        if (gd.getStato() != null && gd.getStato() == Worker.State.CANCELLED) return;
        gd.pause();
        paused = true;
        Platform.runLater(()-> {
            pausa.setText("Avvia");
            if(gd.getStato() == Worker.State.SUCCEEDED && !pausedByUser){
                pausa.setDisable(true);}
        });
    }

    /**
     * force start o resume per il gestore dei download.
     */
    public void start() {
        if (gd == null || gd.getStato() == Worker.State.CANCELLED) return;
        if(gd.getStato() == Worker.State.SUCCEEDED && !pausedByUser)  return;
        gd.start();
        paused = false;
        pausedByUser = false;
        pausa.setText("Pausa");
        pausa.setDisable(false);
    }


    public void enablebuttons(){
        if(gd == null || gd.getStato() == Worker.State.CANCELLED) return;
        System.out.println("heyyyyyyy");
        gd.start();
        pausa.setText("PAusa");
        pausa.setDisable(true);

    }

    /**
     * Ritorna il dominio di visita.
     * @return Il dominio come oggetto Dominio.
     * @see Dominio
     */
    public Dominio getDom(){
        return this.dom;
    }

    /**
     * Routine di uscita dalla tab che, nel suo piccolo, tenta di rendere tutto gc compatibile.
     */
    void dispose(){
        try {
            gd.cancel();
        }
        catch (Exception e){return;}
        dom = null;
        gd = null;
    }

    /**
     * Avvia il gestore dei downloads di questa se non già avviato.
     * Non avvierà nulla se il gestore download è stato cancellato.
     */
    private void run(){
        System.out.println(gd.getStato());
        if (gd.getStato() != Worker.State.READY) return;
        //TODO unire il metodo con start()
        Thread laucher = ThreadUtilities.CreateThread(() -> this.gd.start());
        laucher.setDaemon(true);
        laucher.setName("Thread di visita");
        laucher.start();
        System.out.println("Lanciato demone");
    }

    /**
     * Aggiunge un seed alla visita.
     * @param seed Il seed da aggiungere.
     */
    public void addSeed(Seed seed){
        this.gd.addSeed(seed);
    }

    /**
     * Ottiene i dati finora scaricati in lista osservabile ed immodificabile.
     * @return La lista dei risultati non editabile.
     * @see javafx.collections.FXCollections.UnmodifiableObservableListImpl
     */
    public ObservableList<Page> getData(){
        return FXCollections.unmodifiableObservableList(gd.getDataList());
    }

    /**
     * Ottiene i dati finora modificati in mappa osservabile ed immodificabile.
     * @return La mappa dei risultati non editabile.
     */
    public ObservableMap<URI, Page> getDatamap(){
        return FXCollections.unmodifiableObservableMap(gd.getResults());
    }

    /**
     * Carica la pagina in una webview.
     * @param url L'url da caricare.
     */
    private void loadInNewWindow(String url){
        WebView ww = new WebView();
        Stage stg = new Stage();
        stg.setTitle(url);
        stg.setScene(new Scene(ww));
        ww.getEngine().load(url);
        stg.show();
    }

    private enum links {puntati, puntanti}  /* Enumerazione privata per tipologia links */

    /**
     * Mostra i puntanti/puntanti in una nuova finestra, su tabella, così come da specifiche.
     * La finestra si aggiorna in tempo reale ricreando da zero il grafico.
     * @param tipo Il tipo di links da mostrare.
     * @see links
     */
    private void showSelectedInNewWindow(links tipo){

        Page selectedElement = tableData.getSelectionModel().getSelectedItem(); // Pagina selezionata.
        TableView<URI> tabella = new TableView<>();
        tabella.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<URI, String> uriCol = new TableColumn<>("Uri");
        uriCol.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().toString()));
        TableColumn<URI, Boolean> uriStat = new TableColumn<>("Scaricata");
        tabella.getColumns().add(uriCol);
        tabella.getColumns().add(uriStat);

        ObservableList<URI> data = FXCollections.observableArrayList();

        /*
        Se si sceglie puntanti si deve aggiornare la tabella ricaricando la lista, difatti essi vengono
        computati durante la visita.
        Se altrsì si sceglie puntati, bisogna solo fare un refresh della tabella con un piccolo work-around
        di visualizzazione. Questo è dovuto all'implementazione di javafx che aggiorna la grafica della tabella
        se cambia la lista (Aggiunto o rimosso uno o più elementi) ma non nel cambiamento di stato di ogni elemento.
        Mettere degli elementi osservabili sembrava troppo.
        I calcoli sono eseguiti dall'oggetto Page.
         */
        if (tipo == links.puntanti) {
            data.addAll(selectedElement.getPtr());
            uriStat.setCellValueFactory(param ->
                            new SimpleObjectProperty<>(selectedElement.getPtrMap().containsKey(param.getValue()))
            );
            selectedElement.getPtrMap().addListener((InvalidationListener) observable -> {
                data.clear();
                data.addAll(selectedElement.getPtr());
            });
        }else{
            data.addAll(selectedElement.getPtd());
            uriStat.setCellValueFactory(param ->
                            new SimpleObjectProperty<>(selectedElement.getPtdMap().containsKey(param.getValue()))
            );
            selectedElement.getPtdMap().addListener((InvalidationListener) observable -> {
                uriCol.setVisible(false);
                uriCol.setVisible(true);
            });
        }

        // Setta la tabella
        tabella.setItems(data);
        Stage st = new Stage();
        st.setScene(new Scene(tabella));
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle((tipo == links.puntanti? "Mi puntanto in " : "Punto a ") + tabella.getItems().size());
        st.show();
    }

    @Override
    public String toString(){
        return this.dom.getURI().toString();
    }

    ////////////////////////////////////////////////////////////////////////////////////// FXML declarations

    private @FXML   PieChart        pieEntranti;
    {
        pieEntranti.setLegendVisible(false);
        pieEntranti.setLabelsVisible(false);
        pieEntranti.setAnimated(false);
        pieEntranti.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2){
                entST.show();
            }
        });
    }
    private @FXML   PieChart        pieUscenti;
    {
        pieUscenti.setLegendVisible(false);
        pieUscenti.setLabelsVisible(false);
        pieUscenti.setAnimated(false);
        pieUscenti.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2){
                exitST.show();
            }
        });
    }
    private @FXML   ToolBar         toolbarTab;
    private @FXML   TableView<Page> tableData;
    private @FXML TableColumn<Page, URI> domColumn;
    private @FXML TableColumn<Page, Boolean> followColumn;
    private @FXML TableColumn<Page, String> statusColumn;
    private @FXML TableColumn<Page, Integer> linksNumColumn;
    {
        domColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getURI()));
        followColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getPageLink()));
        statusColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getExc()== null ? "" : param.getValue().getExc().getMessage()));
        linksNumColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().linksNumber()));

        // Menu contestuale.
        MenuItem info = new MenuItem("Informazioni");
        info.setOnAction(e -> {
            if (tableData.getSelectionModel().getSelectedItem() != null)
                new InfoFrame(tableData.getSelectionModel().getSelectedItem()).show();
        });
        MenuItem getPuntanti = new MenuItem("Puntanti a questa pagina.");
        getPuntanti.setOnAction(event -> showSelectedInNewWindow(links.puntanti));
        MenuItem getPuntati = new MenuItem("Puntati da questa pagina.");
        getPuntati.setOnAction(event -> showSelectedInNewWindow(links.puntati));
        contestuale.getItems().addAll(info, getPuntanti, getPuntati);

        tableData.setContextMenu(contestuale);
        tableData.setOnMouseClicked(e ->{
            Page pagina = tableData.getSelectionModel().getSelectedItem();
            /*
             Aprire una webView, se e solo se, per coerenza:
             - Ho una pagina da aprire (Selezione diversa da null)
             - L'eccezione è null (Non ho avuto errori)
             - La pagina ha almeno un link (Spesso i files hanno 0 links).
             - Se la pagina ha 0 links E non appartiene al dominio.
             */
            if (e.getClickCount() == 2 && pagina != null){
                if (pagina.getExc() != null){
                    new EventFrame(e,
                            Alert.AlertType.WARNING,
                            "La pagina è stata scaricata con errori.\n" +
                                    "Aprire ugualmente?",
                            ButtonType.CANCEL,
                            () -> loadInNewWindow(tableData.getSelectionModel().getSelectedItem().getURI().toString())
                    );
                }else if (pagina.linksNumber() == 0 && pagina.getPageLink()){
                    new EventFrame(e,
                            Alert.AlertType.WARNING,
                            "La pagina ha 0 links.\n" +
                                    "Di solito questo significa che è un file \n" +
                                    "multimediale.\n" +
                                    "Aprire ugualmente?",
                            ButtonType.CANCEL,
                            () -> loadInNewWindow(tableData.getSelectionModel().getSelectedItem().getURI().toString())
                    );
                }else{
                    loadInNewWindow(tableData.getSelectionModel().getSelectedItem().getURI().toString());
                }
            }
        });
    }
    private @FXML   Label           labelVisitati;
    private @FXML   Label           labelErrori;
    private @FXML   Label           labelDominio;
    private @FXML   Label           labelMaxLinks;
    private @FXML   Label           labelMaxPointers;

    private MenuItem pausa;
    private MenuItem cancella;

    private @FXML   MenuButton      menuButtonAzioni;
    {
        MenuItem aggiungiSeed = new MenuItem("Aggiungi Seeds...");
        aggiungiSeed.setOnAction(e -> new VisitFrame(this).show());
        pausa = new MenuItem("Pausa");
        pausa.setOnAction(e -> {
            if (gd.getStato() == Worker.State.CANCELLED) return;
            if (!paused){
                System.out.println("Pausa");
                pausedByUser = true;
                pause();
            }else{
                System.out.println("Resume");
                start();
            }
        });
        cancella = new MenuItem("Cancella crawling");
        cancella.setOnAction(e -> {
            new EventFrame(e, Alert.AlertType.WARNING, "Cancellando la visita\n" +
                    "non sarà più possibile continuare\n" +
                    "a scaricare le pagine. Cancellare?", ButtonType.CANCEL, this::cancel);
        });
        MenuItem operazioni = new MenuItem("Operazioni");
        operazioni.setOnAction(event -> {
            OperationsFrame of = new OperationsFrame();
            of.show();
            of.select(1);
        });
        menuButtonAzioni.getItems().addAll(aggiungiSeed, pausa, operazioni, new SeparatorMenuItem(), cancella);
    }

    {
        tableData.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            domColumn.setVisible(false);
            domColumn.setVisible(true);
        });
    }

    /*//////////////////////////////////////////////////////////////////////////////////////

    Queste chiamate, per logica, sono separate dai blocchi precedenti. Ovviamente avrei potuto usare una ed una
    sola coppia di parentesi graffe, ma la logica deve prevalere sempre sulla pigrizia in programmazione.
    Una nota sulla colorazione:
    I colori sono locati in Settings, probabilmente si potrebbe (e sarebbe logico) provvedere alla modifica da parte
    dell'utente, tuttavia, per ora, ho mantenuto i nostri soliti VIOLA/GIALLO.
    Non è una richiesta delle specifiche, si intende, ma era una feature carina da mantenere.
    */

    /**
     * Annulla i cambiamenti di stato nella cella di una tabella.
     * @param cell La cella dove annullare lo stato.
     */
    private void updateNull(TableCell cell){
        cell.setText(null);
        cell.setGraphic(null);
        cell.setBackground(null);
    }

    public void forceTableRefresh(){
        domColumn.setVisible(false);
        domColumn.setVisible(true);
    }

    {
        domColumn.setCellFactory(param ->
                new TableCell<Page, URI>(){
                    @Override
                    public void updateItem(URI item, boolean empty){
                        super.updateItem(item, empty);

                        /*
                        Se l'item è null, allora necessariamente
                        deve pulire la cella da precedenti grafiche
                        o residue sporcizie visive.
                        */
                        if (item == null){
                            updateNull(this);
                            return;
                        }

                        /*
                        Setta il testo all'item consegnato
                        ed il background a null.
                        */
                        setText(item.toString());
                        setBackground(null);

                        if (param.getTableView().getSelectionModel().getSelectedItem() == null) return;

                        Page data = param.getTableView().getSelectionModel().getSelectedItem(); // prendo la pagina per comodità.

                        /*
                        Il primo caso è per i puntanti:
                        Il secondo per i puntati.
                        Il terzo per entrambi i caso.
                        Da notare che, rispetto alla precedente versione, non colora indiscriminatamente e nel caso fa il revert, anzi.
                        */
                        if (data.getPtr().contains(item) && !data.getPtd().contains(item)){
                            setBackground(new Background(new BackgroundFill(Settings.CR_PTD.get(), new CornerRadii(0), Insets.EMPTY)));
                        }else if(!data.getPtr().contains(item) && data.getPtd().contains(item)){
                            setBackground(new Background(new BackgroundFill(Settings.CR_PTR.get(), new CornerRadii(0), Insets.EMPTY)));
                        }else if(data.getPtr().contains(item) && data.getPtd().contains(item)){
                            this.setBackground(new Background(new BackgroundFill(Settings.CR_PTDandPTR.get(), new CornerRadii(0), Insets.EMPTY)));
                        }
                    }
                });

        statusColumn.setCellFactory(param ->
                        new TableCell<Page,String>(){
                            @Override
                            public void updateItem(String item, boolean emtpy) {
                                super.updateItem(item,emtpy);
                                if(item == null){
                                    this.setText(null);
                                    this.setBackground(null);
                                }
                                else{
                                    if(item.equalsIgnoreCase("")){
                                        this.setText("Scaricata");
                                        this.setTextFill(Color.GREEN);
                                        this.setTextAlignment(TextAlignment.CENTER);
                                    }
                                    else{
                                        this.setText(item);
                                        this.setTextFill(Color.RED);
                                        this.setTextAlignment(TextAlignment.CENTER);
                                    }
                                }
                            }
                        }
        );

        followColumn.setCellFactory(param ->
                new TableCell<Page, Boolean>(){
                    @Override
                    public void updateItem(Boolean item, boolean empty){
                        super.updateItem(item, empty);

                        /*
                        Se l'item è null, allora necessariamente
                        deve pulire la cella da precedenti grafiche
                        o residue sporcizie visive.
                        */
                        if (item == null){
                            updateNull(this);
                            return;
                        }

                        /*
                        Setta il testo all'item consegnato
                        ed il background a null.
                        */
                        setText(item ? "Si" : "No");
                        setBackground(null);

                        if (item && Settings.CR_FOLLOW.getValue()){
                            setTextFill(Color.GREEN);
                        }else if (!item && Settings.CR_FOLLOW.getValue()){
                            setTextFill(Color.RED);
                        }
                    }
                });
    }
}

