package wsa.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wsa.exceptions.DominioException;
import wsa.exceptions.EventFrame;
import wsa.session.Dominio;
import wsa.session.Seed;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by gaamda on 19/11/15.
 *
 * Genera una finestra in grado di validare dei Seeds, il dominio, e di fornire, in tutti i casi, i dati
 * per iniziare una nuova visita. Questa finestra si avvale di facilities per il parsing delle stringhe.
 * Prova ad aiutare l'utente nel completamento dell'inserimento degli url ma non sempre ci riesce, pertanto
 * si richiede all'utente di verificare i dati inseriti.
 */
public class VisitFrame {

    private enum pass {pass, validation}

    private Scene rootScene;
    private ObservableList<Label> obsSeeds = FXCollections.observableArrayList();
    private ObjectProperty<pass> validityPass = new SimpleObjectProperty<>(pass.pass);
    {
        this.validityPass.addListener((observable, oldValue, newValue) -> {  // Static code injection.
            if (newValue == pass.pass) {
                this.hboxValidazione.setVisible(false);
                return;
            }
            this.hboxValidazione.setVisible(true);
        });
    }

    private Dominio dom;
    private File f;
    private Alert al = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
    private Stage primaryStage = new Stage();
    private final List<Seed> seeds = new ArrayList<>();
    private final String textGreen = "-fx-text-fill: green;";
    private final String textRed = "-fx-text-fill: red;";
    private final String textNoSelectedFolder = "Nessuna cartella selezionata...";
    {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./visitFrame.fxml"));
            loader.setController(this);
            rootScene = new Scene(loader.load());
        }catch (Exception ex){
            new EventFrame(ex, null);   // Lancio un'eccezione visiva. L'applicazione continua.
        }
    }

    /**
     * Costruisce un oggetto di tipo VisitFrame. Può essere usato come contenuto di una tab {@link TabPane},
     * può essere usato sia come finestra per generare una nuova visita, sia - dato il suo help per l'utente -
     * come una finestra per aggiungere i seeds durante una visita. Questa scelta viene fatta in automatico
     * passando al costruttore un tabFrame, se esso è null, la finestra si comporterà in modo da creare una
     * nuova visita, altrimenti, avendo un tabFrame associato, si occuperà di aggiungere eventuali nuovi seeds
     * al tabFrame associato.
     * @param tb Il tabFrame associato.
     */
    public VisitFrame(TabFrame tb){
        primaryStage.setScene(rootScene);
        primaryStage.setTitle("Nuova visita");
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        if (tb != null) toAddSeedWindow(tb);
    }

    /**
     * Mostra questa finestra.
     */
    public void show(){
        primaryStage.show();
    }

    /**
     * Crea un dominio partendo dalla stringa immessa dall'utente e se possibile lo valida.
     */
    private boolean createDom() {
        if (textDominio.getText().isEmpty()) return false;
        Dominio dom;
        try {
            dom = new Dominio(textDominio.getText());
        } catch (DominioException e) {
            return false;
        }
        if (dom.validateDom()){
            textDominio.setStyle(textGreen);
            this.dom = dom;
            List<Seed> bads = validateAllSeeds();
            if (!bads.isEmpty()){

                new EventFrame(null, Alert.AlertType.WARNING, "Eliminare i seeds non validi col nuovo dominio?", ButtonType.NO,
                        () -> {
                            for (int i = 0; i < bads.size(); i++){
                                removeSeed(i);
                            }
                        });
            }
            return true;
        }else{
            textDominio.setStyle(textRed);
            return false;
        }
    }

    /**
     * Crea un nuovo seed dalla stringa immessa dall'utente e se possibile lo valida.
     * @throws MalformedURLException per problemi con la creazione.
     * @throws DominioException per problemi con la creazione.
     * @return True se generato, false altrimenti.
     * @see Seed
     */
    private boolean createSeed() throws MalformedURLException, DominioException {
        if (textSeed.getText().isEmpty()) return false; // Non creo un seed.

        Seed seed = new Seed(textSeed.getText());

        for (Seed sd : seeds){
            if (sd.getURI().equals(seed.getURI())) return false;
        }
        if (dom == null){
            addSeed(seed, textRed);
            validityPass.setValue(pass.validation);
            return true;
        }
        if (seed.validateSeed(dom)){
            seed.setChecked(true);
            addSeed(seed, textGreen);
            return true;
        }else{
            throw new DominioException("Non creato", "Impossibile creare il seed valido");
        }
    }

    /**
     * Aggiunge un seed s alla lista, applicando lo stile desiderato all'elemento label che lo rappresenta.
     * @param s Il seeds.
     * @param style Lo stile.
     */
    private void addSeed(Seed s, String style){
        this.seeds.add(s);
        Label l = new Label(s.getURI().toString());
        l.setStyle(style);
        obsSeeds.add(l);
        System.out.println("Seed aggiunto: " + s);

    }

    /**
     * Se presente, rimuove il seed i-esimo dai seed accumulati.
     * @param i l'indice da rimuovere.
     */
    private void removeSeed(int i){
        seeds.remove(i);
        obsSeeds.remove(i);
        if (obsSeeds.isEmpty()) validityPass.setValue(pass.pass);
        System.out.println("Remove request");
    }

    /**
     * La validazione di un seed consiste nel constatare se il seed in questione sia appartenente al dominio di referenza,
     * e nel "marcarlo" di conseguenza. Questo metodo esegue un ciclo for e valida tutti i seeds non ancora validati;
     * ritorna una lista contenente quei seed che non hanno superato la validazione con esito positivo (quelli non appartenenti al dominio).
     * @return La lista dei "cattivi ragazzi".
     * @see Seed
     */
    private List<Seed> validateAllSeeds(){
        if (seeds.isEmpty()) return new ArrayList<>();

        if (dom == null) createDom();
        List<Seed> bad = seeds.stream().filter(sw -> !validateSeed(sw)).collect(Collectors.toList());

        if (bad.isEmpty()) {
            validityPass.setValue(pass.pass);
            return bad;
        }
        validityPass.setValue(pass.validation);
        return bad;
    }

    /**
     * Prova a validare un singolo seed e lo colora in base all'esito.
     * @param sw Il seeds da validare.
     * @return True se valida, false altrimenti.
     */
    private boolean validateSeed(Seed sw){
        if (sw.validateSeed(this.dom)) {
            sw.setChecked(true);
            obsSeeds.get(seeds.indexOf(sw)).setStyle(textGreen);
            System.out.println("Checked seed " + sw);
            return true;
        }else{
            sw.setChecked(false);
            obsSeeds.get(seeds.indexOf(sw)).setStyle(textRed);
            System.out.println("UnChecked seed " + sw);
            return false;
        }
    }

    /**
     * Crea un oggetto Seed (non validato) partendo dal contenuto del TextBox, e lo aggiunge alla label dei seed.
     */
    private void AddFromTextSeed(){
        if (textSeed.getText().isEmpty()) return;
        al.setContentText("Impossibile validare il seeds: " + textSeed.getText());
        try {
            createSeed();
            textSeed.clear();
        } catch (Exception ex) {
            al.showAndWait();
        }
    }

    /**
     * Usato solo a visita già in corso, mostra un VisitFrame ridotto per la sola aggiunta dei seeds.
     */
    private void toAddSeedWindow(TabFrame tb){
        dom = tb.getDom();
        borderBase.setTop(null);
        vboxCenter.getChildren().removeAll(hboxCartella, hboxValidazione);
        textSeed.setText(dom.toString() + "/");
        borderBase.setPrefSize(borderBase.getPrefWidth(), BorderPane.USE_PREF_SIZE);
        ((Stage) rootScene.getWindow()).setTitle("Aggiungi seeds");

        buttonAvvia.setOnAction(e -> {
            List<Seed> s = validateAllSeeds();
            if (s.isEmpty()) {
                seeds.forEach(tb::addSeed);
                primaryStage.close();
            } else {
                al.setContentText("Impossibile aggiungere questi seeds.");
                al.showAndWait();
            }
        });
    }

    // FXML declare

    private @FXML   BorderPane                              borderBase;
    private @FXML   TextField                               textDominio;
    {
        textDominio.setOnAction(e -> createDom());
    }
    private @FXML   TextField                               textSeed;
    {
        textSeed.setOnAction(e -> AddFromTextSeed());
    }
    private @FXML   Button                                  buttonAggiungiSeed;
    {
        buttonAggiungiSeed.setOnAction(e -> AddFromTextSeed());
    }
    private @FXML   Label                                   labelCartella;
    {
        labelCartella.setText(textNoSelectedFolder);
    }
    private @FXML   Label                                   labelDownload;
    private @FXML   Button                                  buttonSfoglia;
    {
        buttonSfoglia.setOnAction(e -> {
            f = OtherFrames.openFolderFrame((Stage) rootScene.getWindow(), "Scegli dove salvare.");
            System.out.println(f);
            if (f != null){
                labelCartella.setText(f.getPath());
            }else{
                labelCartella.setText(textNoSelectedFolder);
            }
        });
    }
    private @FXML   Button                                  buttonCancella;
    {
        buttonCancella.setOnAction(e -> ((Stage) rootScene.getWindow()).close());
    }
    private @FXML   Button                                  buttonAvvia;
    {
        buttonAvvia.setOnAction(e -> {

            if (dom == null && seeds.isEmpty() && f == null){
                al.setContentText("Nessun dato valido inserito.");   // Validazione obsSeeds, per scrupolo.
                al.showAndWait();
                return;
            }else if (!seeds.isEmpty() && f == null){
                if (!createDom()){
                    al.setContentText("Impossibile determinare il dominio.\n" +
                            "Se si sta avviando una visita pregressa\n" +
                            "eliminare i seeds ed aggiungerli una volta caricata.");   // Validazione obsSeeds, per scrupolo.
                    al.showAndWait();
                    return;
                }
            }else if (dom != null && seeds.isEmpty()){
                new EventFrame(e, Alert.AlertType.CONFIRMATION, "Nessun seed inserito.\n" +
                        "Se si sta avviando una visita pregressa\n" +
                        "il dominio verrà automaticamente caricato,\n" +
                        "eliminare il dominio correntemente memorizzato?.", ButtonType.CANCEL, () -> {
                    this.dom = null;
                    this.textDominio.clear();
                    this.textDominio.setStyle(null);
                });
                return;
            }

            if (!validateAllSeeds().isEmpty()){
                al.setContentText("Ricontrollare i dati inseriti.");   // Validazione obsSeeds, per scrupolo.
                al.showAndWait();
                return;
            }

            if (f == null){
                new EventFrame(e, Alert.AlertType.CONFIRMATION,
                        "Continuare senza salvare?", ButtonType.CANCEL,
                        ()-> {
                            MainFrame.getMainFrame().addVisit(dom,
                                    new ArrayList<>(seeds.stream().collect(Collectors.toList())),
                                    null, 5, true);
                            primaryStage.close();
                        });
            }else {
                MainFrame.getMainFrame().addVisit(dom,
                        new ArrayList<>(seeds.stream().collect(Collectors.toList())),
                        f.toPath(), 5, true);
                primaryStage.close();

            }
        });
    }
    private @FXML   Button                                  buttonValida;
    {
        buttonValida.setOnAction(e -> validateAllSeeds());
    }
    private @FXML   HBox                                    hboxValidazione;
    {
        hboxValidazione.setVisible(false);
        hboxValidazione.setStyle("-fx-background-color: lightYellow;");
    }
    private @FXML   HBox                                    hboxCartella;
    private @FXML   VBox                                    vboxDownload;
    private @FXML   VBox                                    vboxCenter;
    private @FXML   Label                                   labelValidazione;
    {
        labelValidazione.setText("Alcuni seeds devono essere\n" +
                "ancora convalidati.");
    }
    private @FXML   ListView<Label>                         listSeed;
    {
        listSeed.setItems(obsSeeds);
        listSeed.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.DELETE) && !obsSeeds.isEmpty()){
                removeSeed(listSeed.getSelectionModel().getSelectedIndex());
                validateAllSeeds();
            }
        });
    }
}

