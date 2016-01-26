package wsa.gui;

import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import wsa.exceptions.EventFrame;

/**
 * Created by gaamda on 14/11/15.
 *
 * Crea una finestra di chiusura che termina l'applicazione.
 */
class CloseFrame extends EventFrame {

    /**
     * Un closeFrame è un eventFrame specializzato nell'uscita dell'applicazione.
     * Ci assicura inoltre che i vari thread attivi vengano cancellati in caso di chiusura.
     * @param e   L'evento da considerare.
     */
    public CloseFrame(Event e) {
        super(e, Alert.AlertType.CONFIRMATION, "Uscire?", ButtonType.CANCEL, ThreadUtilities::Dispose);
    }

}
