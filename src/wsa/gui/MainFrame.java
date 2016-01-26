package wsa.gui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import wsa.Constants;
import wsa.Settings;
import wsa.exceptions.EventFrame;
import wsa.session.Dominio;
import wsa.session.Seed;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by gaamda on 14/11/15.
 *
 * MainFrame per la finestra principale.
 * Questa è la finestra principale del programma, accedibile in ogni parte dello stesso. È un singleton, in quanto si
 * ammette una sola istanza di questa finestra per processo. Essendo essa locata nel package {@link wsa.gui}, contiene
 * metodi protetti ed indefiniti atti al corretto funzionamento della finestra.
 */
public class MainFrame {

    private static MainFrame mf;
    private Stage primaryStage = new Stage();
    private ObservableList<TabFrame> ret = FXCollections.observableArrayList();

    private Scene rootScene;
    {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./mainFrame.fxml")); /* Carico risorsa */
            loader.setController(this);
            rootScene = new Scene(loader.load());
        }catch (Exception ex){
            new EventFrame(ex, Platform::exit);
        }

        primaryStage.setMaximized(true);
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(600);
    }

    /*
    Costruttore privato.
     */
    private MainFrame(){
        primaryStage.setScene(rootScene);
        primaryStage.setTitle(Constants.APPLICATION_NAME);
        primaryStage.setOnCloseRequest(a -> {
            if (tabBase.getTabs().isEmpty()){
                ThreadUtilities.Dispose();
            }else {
                new CloseFrame(a).start();
            }
        });
    }

    /**
     * Aggiunge una visita alla finestra.
     * @param d Il dominio.
     * @param s Una lista di seeds.
     * @param p Il percorso.
     * @param m Il modulo dei grafici.
     * @param r Se true avvia la visita.
     */
    public void addVisit(Dominio d, List<Seed> s, Path p, Integer m, boolean r){
        TabFrame tb = new TabFrame(d, s, p, m, r);
        tabBase.getTabs().add(tb);
        ret.add(tb);
    }

    /**
     * Mostra questa finestra, se non già mostrata.
     */
    public void show(){
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
    }

    /**
     * Ottiene il MainFrame per questa sessione, un solo MainFrame è definito per processo.
     * Usare il metodo start per lanciare la finestra.
     * @return Il MainFrame per il programma.
     */
    public static MainFrame getMainFrame(){
        if (mf == null) mf = new MainFrame();
        return mf;
    }

    /**
     * Ritorna lo stage di questa finestra come oggetto Stage.
     * @return Lo Stage di questa finestra.
     */
    public Stage getStage(){return this.primaryStage;}

    /**
     * Ottiene le tab di visita di questa finestra, cioè, del programma.
     * @return Una lista di TabFrames.
     * @see TabFrame
     */
    public ObservableList<TabFrame> getTabs(){
        return ret;
    }

    // FXML declare

    private @FXML   TabPane     tabBase;
    private @FXML   Menu        menuFile;
    private @FXML   Menu        menuStrumenti;
    private @FXML   MenuItem    menuAzioniVisite;
    {
        menuAzioniVisite.setOnAction(event -> new OperationsFrame().show());
        menuAzioniVisite.setVisible(false);
        tabBase.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (tabBase.getTabs().size() > 1){
                menuAzioniVisite.setVisible(true);
            }else{
                menuAzioniVisite.setVisible(false);
            }
        });
    }
    private @FXML   MenuItem    menuImpostazioni;
    {
        menuImpostazioni.setOnAction(e -> {
            System.out.println("Mostrato");
            new SetFrame().show();
        });
    }
    private @FXML   Menu        aiuto;
    private @FXML   MenuItem    menuNuovaVisita;
    {
        menuNuovaVisita.setOnAction(a -> new VisitFrame(null).show());
    }
    private @FXML   MenuItem    menuApriVisita;
    {
        menuApriVisita.setOnAction(k -> new RecoverVisitFrame());
    }
    private @FXML   MenuItem    menuChiudi;
    {
        menuChiudi.setOnAction(a -> new CloseFrame(a).start());
    }
    private @FXML   MenuItem    menuInformazioni;
    {
        menuInformazioni.setOnAction(event -> {
            /*
            Genera la finestra di informazioni, con animazione per la scritta e
            numero di versione ove applicabile.
             */
            Label subtitle = new Label("\"" + Constants.getRandomString() + "\"\n\n");
            Timeline pulse = new Timeline();
            pulse.setAutoReverse(true);
            pulse.getKeyFrames().add(new KeyFrame(Duration.ZERO,
                    new KeyValue(subtitle.scaleXProperty(), 1),
                    new KeyValue(subtitle.scaleYProperty(), 1)
                    ));
            pulse.getKeyFrames().add(new KeyFrame(new Duration(1000),
                    new KeyValue(subtitle.scaleXProperty(), 1.3),
                    new KeyValue(subtitle.scaleYProperty(), 1.3)
            ));
            pulse.getKeyFrames().add(new KeyFrame(new Duration(1000),
                    new KeyValue(subtitle.scaleXProperty(), 1),
                    new KeyValue(subtitle.scaleYProperty(), 1)
            ));
            Stage stg = new Stage(StageStyle.UTILITY);
            stg.setMinHeight(250);
            stg.setMinWidth(300);
            stg.setMaxHeight(250);
            stg.setMaxWidth(300);
            Label name = new Label(Constants.APPLICATION_NAME + "  "  + Constants.APPLICATION_VERSION);

            ImageView imgv = new ImageView(new Image(this.getClass().getResourceAsStream("./logo.png")));

            name.setFont(new Font(30));
            Label copyright = new Label("Nato in seno al corso di\n" +
                    "Metodologie di programmazione.\n" +
                    "A.A. 2014/2015");
            copyright.setAlignment(Pos.CENTER);
            VBox box = new VBox((Settings.RUN_WITH_LOGO ? imgv : name), subtitle, copyright);

            box.setAlignment(Pos.CENTER);
            stg.setScene(new Scene(box));
            stg.initModality(Modality.APPLICATION_MODAL);
            stg.show();
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        });
    }
}
