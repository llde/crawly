package wsa.elaborazioni;

import wsa.gui.ThreadUtilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by gaamda on 13/11/15.
 *
 * Un esecutore specializzato per eseguire solo compiti di tipo Task<T>.
 * @see Task
 */
public class Executor {

    private Executor(){}

    private final static ExecutorService exec = ThreadUtilities.TrackExecutorService(Executors.newFixedThreadPool(100, ThreadUtilities::CreateThread));

    /**
     * Esegue in maniera asincrona un {@link Task}, non ritornando nessun risultato.
     * Esegue il metodo Esegui del Task.
     * @param t Il task da elaborare.
     */
    public synchronized static void perform(Task t){
        exec.submit(() -> t.esegui(new Executor()));
    }

    /**
     * Esegue in maniera asincrona un {@link Task}, ritornando un future.
     * Esegue il metodo eseguiConRitorno del Task.
     * @param t Il task da elaborare.
     */
    public synchronized static <T> Future<T> performFuture(Task<T> t){
        return exec.submit(() -> t.eseguiConRitorno(new Executor()));
    }

}


