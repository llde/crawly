package wsa.gui;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Created by gaamda on 21/11/15.
 *
 * Classe che tiene frames ed utilità per la gui.
 */
public class OtherFrames {

    /**
     * Ottiene un file da un un DirectoryChooser.
     * @param s Lo stage della finestra che genera il dialogo.
     * @param t Il titolo da dare al dialogo.
     * @return Il file selezionato o null se nessun file è selezionato.
     */
    public static File openFolderFrame(Stage s, String t){
        DirectoryChooser dialogo = new DirectoryChooser();
        dialogo.setTitle(t);
        dialogo.setInitialDirectory(new File(System.getProperty("user.home")));
        return dialogo.showDialog(s);
    }

}

