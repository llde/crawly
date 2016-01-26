package wsa.exceptions;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import wsa.Constants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Random;

/**
 * Created by gaamda on 14/11/15.
 *
 * Frame per un dialogo di conferma/errore/pericolo con messaggio personalizzato.
 * È Un frame che prende in consegna un buttonType, e fornisce già il pulsante di OK.
 * Col pulsante dato genererà un event.consume(), con l'OK, eseguirà l'evento.
 */
public class EventFrame extends Thread{

    /**
     * Un eventFrame è una finestra di dialogo che gestisce un messaggio personalizzato con
     * un'opzione di scelta binaria. Fornisce già il pulsante di OK, ma non il secondario.
     * @param e L'evento da considerare.
     * @param t Il tipo di Alert da mostrare.
     * @param m Il messaggio da visualizzare.
     * @param b Il bottone secondario. NB: La scelta "OK" verrà dirottata su "CANCEL".
     * @param eOK La runnable da eseguire in caso di scelta "OK".
     */
    public EventFrame(Event e, Alert.AlertType t, String m, ButtonType b, Runnable eOK){
        this(e,t,m,b,eOK, null);
    }

    /**
     * Un eventFrame è una finestra di dialogo che gestisce un messaggio personalizzato con
     * un'opzione di scelta binaria. Fornisce già il pulsante di OK, ma non il secondario.
     * @param e L'evento da considerare.
     * @param t Il tipo di Alert da mostrare.
     * @param m Il messaggio da visualizzare.
     * @param b Il bottone secondario. NB: La scelta "OK" verrà dirottata su "CANCEL".
     * @param eOK La runnable da eseguire in caso di scelta "OK".
     * @param bButton La runnable da eseguire in caso di scelta del secondo bottone.
     */
    public EventFrame(Event e, Alert.AlertType t, String m, ButtonType b, Runnable eOK, Runnable bButton){

        if (b == ButtonType.OK) b = ButtonType.CANCEL; /* Non possono esistere due buttons OK */

        Alert al = new Alert(t, m , ButtonType.OK, b);
        Optional<ButtonType> op = al.showAndWait();

        if(op.get().equals(b)){ /* Se il bottone premuto è quello in parametro consuma l'evento,
                                   se c'è, o esegui la runnable, se c'è. */
            if (e != null){
                e.consume();
            }
            if(bButton != null){
                bButton.run();
            }
        }
        else if(op.get().equals(ButtonType.OK)) {
            if (e != null) System.out.println("Event: " + e.toString());
            if (eOK != null) {
                eOK.run();
            } else {
                if(e != null) e.consume(); /* Consuma l'evento */
            }
        }
    }

    /**
     * Un event frame specializzato nella creazione di un alert per visualizzare stacktraces.
     * @param ex L'eccezione da stampare.
     * @param eOK Runnable da eseguire alla pressione del tasto OK.
     */
    public EventFrame(Exception ex, Runnable eOK){
        if (ex == null) return;
        Alert al = new Alert(Alert.AlertType.ERROR, ex.toString(), ButtonType.OK);
        al.setTitle("Errore in " + Constants.APPLICATION_NAME);
        al.setHeaderText(ex.toString());

        Label l = new Label("Stacktrace dell'errore:");
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        TextArea tx = new TextArea(sw.toString());
        tx.setEditable(false);
        tx.setWrapText(true);
        tx.maxHeight(600);
        tx.maxWidth(800);
        GridPane.setVgrow(tx, Priority.ALWAYS);
        GridPane.setHgrow(tx, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(l, 0, 0);
        expContent.add(tx, 0, 1);

        al.getDialogPane().setContent(expContent);

        Optional<ButtonType> op = al.showAndWait();
        if(op.get().equals(ButtonType.OK)){
            if (eOK != null) {
                eOK.run();
            }
        }
    }
}
