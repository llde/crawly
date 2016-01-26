package wsa.gui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wsa.elaborazioni.Executor;
import wsa.elaborazioni.TaskTagGetter;
import wsa.exceptions.EventFrame;
import wsa.session.Page;

/**
 * Created by gaamda on 29/11/15.
 *
 * Finestra che gestisce le informazioni relative alla pagina scaricata, è visibile dall'utente nel programma.
 * Molta attenzione viene data allo scaricamento delle immagini.
 */
class InfoFrame {

    private Page pagina = null;
    private Scene rootScene;
    private Stage primaryStage = new Stage();

    {
        primaryStage.setTitle("Informazioni");
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./infoFrame.fxml"));
            loader.setController(this);
            rootScene = new Scene(loader.load());
            primaryStage.setScene(rootScene);
        }catch (Exception ex){
            ex.printStackTrace();
            new EventFrame(ex, Platform::exit);
        }
    }

    /**
     * Crea un infoFrame che gestisce le informazioni lato utente.
     * @param pagina La pagina della quale ottenere informazioni.
     */
    InfoFrame(Page pagina){
        this.pagina = pagina;
        labelURI.setText(this.pagina.getURI().toString());
        labelSeguita.setText(this.pagina.getPageLink() ? "Sì" : "No");
        labelTotali.setText(this.pagina.linksNumber().toString());
        labelErrati.setText(this.pagina.errsNumber().toString());
        labelEntranti.setText(this.pagina.linksEntrantiNumber().toString());
        labelUscenti.setText(this.pagina.linksUscentiNumber().toString());
        labelPuntanti.setText(this.pagina.ptrNumbers().toString());
        labelPuntati.setText(this.pagina.ptdNumbers().toString());
        labelExc.setText(this.pagina.getExc() != null ? "Sì" : "No");
        labelImmagini.setText("Caricamento...");
        labelNodi.setText("Caricamento...");
        TaskTagGetter tg = new TaskTagGetter(pagina.getURI().toString(), "img");
        tg.getWorkerState().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.FAILED){
                labelImmagini.setText("Impossibile scaricare.");
            }else if (newValue == Worker.State.SUCCEEDED){
                labelImmagini.setText(String.valueOf(tg.getData().size()));
            }
            labelNodi.setText(String.valueOf(tg.getNodes()));
        });
        Executor.perform(tg);
    }

    /**
     * Mostra questa finestra.
     */
    public void show(){
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.show();
    }

    private @FXML   Label   labelURI;
    private @FXML   Label   labelSeguita;
    private @FXML   Label   labelTotali;
    private @FXML   Label   labelErrati;
    private @FXML   Label   labelEntranti;
    private @FXML   Label   labelUscenti;
    private @FXML   Label   labelPuntanti;
    private @FXML   Label   labelPuntati;
    private @FXML   Label   labelImmagini;
    private @FXML   Label   labelNodi;
    private @FXML   Label   labelExc;

}
