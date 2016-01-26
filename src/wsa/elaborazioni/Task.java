package wsa.elaborazioni;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Worker;

import javax.naming.OperationNotSupportedException;

/**
 * Created by gaamda on 13/11/15.
 *
 * Un'interfaccia per la creazione di un Task (unità di elaborazione) per l'esecutore {@link Executor}.
 * Di solito un'attività è formata da uno stato di contesto ed un metodo d'esecuzione.
 */
public interface Task<T> {

    /**
     * Esegue l'azione predisposta del task.
     * @param e L'esecutore {@link Executor}.
     */
    default void esegui(Executor e){}

    /**
     * Esegue con ritorno.
     * @param e L'esecutore {@link Executor}.
     * @return null o dato (Per Override).
     */
    default T  eseguiConRitorno(Executor e){return null;}

    /**
     * Ritorna il risultato della computazione, o null.
     * @return Il risultato o null.
     */
    T getData ();

    /**
     * Interrompe il task o lancia eccezione.
     * @throws OperationNotSupportedException se non implementato.
     */
    default void requestInterrupt() throws OperationNotSupportedException {
        throw new OperationNotSupportedException();
    }

    /**
     * Ottiene l'enum corrispondente allo stato attuale della computazione.
     * @return Lo stato della computazione al tempo t.
     */
    Worker.State getTaskState();

    /**
     * Ritorna l'oggetto osservabile relativo allo stato, sarà possibile in seguito accodare listners
     * a questo oggetto.
     * @return L'oggetto osservabile di stato o null.
     */
    ReadOnlyObjectProperty<Worker.State> getWorkerState();

}
