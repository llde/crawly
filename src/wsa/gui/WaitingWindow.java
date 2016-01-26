package wsa.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Created by lorenzo on 13/01/16.
 */
class WaitingWindow {

    private Stage stg = new Stage();
    //private ProgressIndicator pi = new ProgressIndicator(0);
    private ProgressBar pi = new ProgressBar();
    private Label msgLbl = new Label();
    private Label compute = new Label();
    {
        compute.setVisible(false);
    }
    private Button cancelButton = new Button("Cancella");
    {
        cancelButton.setVisible(false);
    }
    private VBox vv = new VBox(pi, msgLbl, compute, cancelButton);
    {
        stg.setTitle("Calcolo in corso...");
        msgLbl.setText("Calcolo in corso...");
        pi.setMaxHeight(100);
        pi.setMaxWidth(100);
        vv.setAlignment(Pos.CENTER);
        VBox.setMargin(msgLbl, new Insets(30, 30, 30, 30));
        VBox.setMargin(pi, new Insets(30, 30, 0, 30));
        stg.setScene(new Scene(vv));
    }

    /**
     * Genera una finestra d'attesa con il messaggio di default.
     */
    public WaitingWindow(){}

    /**
     * Genera una finestra di attesa con dei parametri definiti.
     * @param title Il titolo da inserire nello Stage.
     * @param message Il messaggio per l'utente da
     */
    public WaitingWindow(String title, String message){
        stg.setTitle(title);
        msgLbl.setText(message);
    }

    public WaitingWindow(String title, String message, EventHandler<WindowEvent> setOnCloseRequest){
        this(title, message);
        if (setOnCloseRequest != null) {
            stg.setOnCloseRequest(setOnCloseRequest);
        }
    }

    public WaitingWindow(String title, String message, EventHandler<WindowEvent> setOnCloseRequest, EventHandler<ActionEvent> cancel){
        this(title, message, setOnCloseRequest);
        if (cancel != null){
            cancelButton.setVisible(true);
            cancelButton.setOnAction(cancel);
        }
    }

    public void hideComputeLabel(){
        this.compute.setVisible(false);
    }

    public void showComputeLabel(){
        this.compute.setVisible(true);
    }

    public void setComputeLabel(String text){
        if (!compute.isVisible()){
            compute.setVisible(true);
        }
        Platform.runLater(() -> compute.setText(text));
    }

    public void setOnClose(EventHandler<WindowEvent> setOnCloseRequest){
        if (setOnCloseRequest != null) {
            stg.setOnCloseRequest(setOnCloseRequest);
        }
    }

    public void setOnCancel(EventHandler<ActionEvent> cancel){
        if (cancel != null){
            cancelButton.setVisible(true);
            cancelButton.setOnAction(cancel);
        }
    }

    /**
     * Ritorna lo stage di questa finestra.
     * @return Lo stage.
     */
    public Stage getStage(){
        return this.stg;
    }

    /**
     * Mostra questa finestra.
     */
    public void show(){
        this.stg.show();
    }

}
