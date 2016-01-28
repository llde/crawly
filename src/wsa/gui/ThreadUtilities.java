package wsa.gui;

import javafx.application.Platform;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by gaamda on 04/11/2015.
 *
 *Classe usata per creare thread e che tiene traccia dei thread creati, essenziale per avere il controllo
 *nella terminazione dei thread generati.
 *
 */

public class ThreadUtilities {

    //Questa lista contiene tutti i thread creati con createThread() NON garbage collected.
    private static List<WeakReference<Thread>> threadlist = new ArrayList<>();
    private static ExecutorService globalExecutor = null;
    /**
     * Questo metodo statico serve per creare un thread dato uno specifico Runnable
     * e di tracciarlo per la chiusura. Non fa partire il thread.
     * Può essere usato in una ThreadFactory
     * @param run Runnable per creare il thread
     * @return il thread creato
     */
    public synchronized static Thread CreateThread(Runnable run){
        Thread thread = new Thread(run);
        threadlist.add(new WeakReference<>(thread));
        return thread;
    }
    /**
     * Questo metodo statico serve per tracciare un ExecutorService  per la chiusura
     * @param exs ExecutorService da tracciare
     * @return l'ExecutorService tracciato
     */
    public synchronized static ExecutorService TrackExecutorService(ExecutorService exs){
        globalExecutor = exs;
        return exs;
    }

    /**
     *  Questo metodo deve essere chiamato SOLTANTO durante la chiusura dell'applicazione
     *  Questo metodo statico serve a chiudere tutti i thread tracciati, creati attraverso CreateThread(),
     *  a chiudere i gestoriDownload nelle Tab per propagarne la chiusura attraverso la propagazione di cancel()
     *  per la catena degli oggetti in wsa.web
     *  Chiude anche l'esecutore globale del programma.
     *  Il metodo non garantisce la chiusura in caso di errori negli oggetti di wsa.web, che impediscono la corretta chiusura
     *  di essi.
     */

    @SuppressWarnings("ConstantConditions")
    synchronized static void  Dispose(){
        StackTraceElement[] stc = Thread.currentThread().getStackTrace();

        System.out.println(stc[2].getClassName());
        if (MainFrame.getMainFrame().getTabs().size() > 0) {
            try {
                MainFrame.getMainFrame().getTabs().forEach((tabl -> tabl.dispose()));
            } catch (Exception ignored){}
        }
        for(WeakReference<Thread> t : threadlist){
            if(t.get() == null) continue;
            if(t.get().isDaemon() || !t.get().isAlive()) continue;
            t.get().interrupt();
        }
        if(globalExecutor != null)globalExecutor.shutdownNow();
        Platform.exit();
       // System.exit(0);
    }

}
