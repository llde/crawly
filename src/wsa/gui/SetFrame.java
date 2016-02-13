package wsa.gui;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wsa.Constants;
import wsa.Settings;
import wsa.exceptions.EventFrame;

import static wsa.Settings.*;

/**
 * Created by gaamda on 29/11/15.
 * Frame per le impostazioni modificabili dall'utente; usa le impostazioni di {@link wsa.Settings}.
 */
public class SetFrame {

    private Scene rootScene;
    private Stage primaryStage = new Stage();

    {
        primaryStage.setTitle("Impostazioni");
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("./setFrame.fxml"));
            loader.setController(this);
            rootScene = new Scene(loader.load());
            primaryStage.setScene(rootScene);
        }catch (Exception ex){
            ex.printStackTrace();
            new EventFrame(ex, Platform::exit);
        }
    }

    /**
     * Mostra questa finestra.
     */
    public void show(){
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.show();
    }

    private @FXML   ColorPicker     colorPtd;
    {
        colorPtd.setValue(config().CR_PTR.getValue());
        colorPtd.setOnAction(e -> {
            config().CR_PTR.setValue(colorPtd.getValue());
            System.out.println("Ricalcolo");
            ObjectProperty<Stop[]> stops = new SimpleObjectProperty<>(new Stop[]{
                    new Stop(0, config().CR_PTR.get()),
                    new Stop(1, config().CR_PTD.get())
            });
            config().CR_PTDandPTR = new SimpleObjectProperty<>(
                    new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops.get())
            );
        });
    }
    private @FXML   ColorPicker     colorPtr;
    {
        colorPtr.setValue(config().CR_PTD.getValue());
        colorPtr.setOnAction(e -> {
            config().CR_PTD.setValue(colorPtr.getValue());
            ObjectProperty<Stop[]> stops = new SimpleObjectProperty<>(new Stop[]{
                    new Stop(0, config().CR_PTR.get()),
                    new Stop(1, config().CR_PTD.get())
            });
            config().CR_PTDandPTR = new SimpleObjectProperty<>(
                    new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops.get())
            );
        });
    }
    private @FXML   CheckBox        checkFollow;
    {
        checkFollow.setSelected(config().CR_FOLLOW.getValue());
        checkFollow.selectedProperty().addListener((observable, oldValue, newValue) -> {
            checkFollow.setSelected(newValue);
            config().CR_FOLLOW.setValue(newValue);
        });
    }
    private @FXML   ComboBox<Constants.grabberMillis>        comboGrabber;
    {
        comboGrabber.getItems().addAll(Constants.grabberMillis.values());
        switch (config().RES_GRABBER_MILLIS){
            case 1000:
                comboGrabber.getSelectionModel().select(Constants.grabberMillis.veloce);
                break;
            case 2000:
                comboGrabber.getSelectionModel().select(Constants.grabberMillis.medio);
                break;
            case 3000:
                comboGrabber.getSelectionModel().select(Constants.grabberMillis.lento);
                break;
        }
        comboGrabber.setOnAction(e -> {
            switch (comboGrabber.getSelectionModel().getSelectedItem()){
                case veloce:
                    config().RES_GRABBER_MILLIS = 1000;
                    break;
                case medio:
                    config().RES_GRABBER_MILLIS = 2000;
                    break;
                case lento:
                    config().RES_GRABBER_MILLIS = 3000;
                    break;
            }
        });
    }

}
