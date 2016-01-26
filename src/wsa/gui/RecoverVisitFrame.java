package wsa.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import wsa.exceptions.EventFrame;

import java.io.File;

/**
 * Created by Lorenzo on 01/12/2015.
 */
public class RecoverVisitFrame {
    boolean found = false;
    boolean loop = true;
    File f = null;
    Runnable openvisit = ()->{
        f = OtherFrames.openFolderFrame(null, "Seleziona la cartella da cui caricare una visita");
        found = false;
        for(File file : f.listFiles()){
            if(file.getName().equalsIgnoreCase("DOM.crawly")){
                found = true;
                break;
            }
        }
    };

    public RecoverVisitFrame() {
        //inserire la scena parente.
        openvisit.run();
        while(!found  && loop)
            new EventFrame(null, Alert.AlertType.INFORMATION, "La cartella selezionata non include una visita salvata valida. Riprovare?", ButtonType.CLOSE, openvisit, ()-> loop = false);
        if(found){
            MainFrame.getMainFrame().addVisit(null, null, f.toPath(), 5, true);  //Cambiare il modulo
        }
    }
}
