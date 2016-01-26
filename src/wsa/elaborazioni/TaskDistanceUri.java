package wsa.elaborazioni;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import wsa.session.Page;

import java.net.URI;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gaamda on 14/12/15.
 *
 * Un Task {@link wsa.elaborazioni.Task} per computare la distanza tra uri, presa una pagina A ed una pagina B.
 * Se non si potrà calcolare la distanza, probabilmente per un problema di conformità dei dati, il valore sarà -1.
 */

public class TaskDistanceUri implements Task, Runnable{

    private ObjectProperty<Worker.State> stato = new SimpleObjectProperty<>(Worker.State.SCHEDULED);
    private Page start = null;
    private Page ending = null;
    private Map<URI, Page> mapToWatch;
    private Integer distance = 0;
    private boolean interrupt = false;

    /**
     * Crea un Task per la distanza.
     * @param mapToWatch La mappa da dove prendere le pagine computate.
     * @param start La pagina di partenza.
     * @param ending La pagina di destinazione.
     */
    public TaskDistanceUri(Map<URI, Page> mapToWatch, Page start, Page ending){
        this.start = start;
        this.ending = ending;
        this.mapToWatch = mapToWatch;
    }

    @Override
    public void requestInterrupt(){
        interrupt = true;
    }

    @Override
    public void esegui(Executor e) {
        algoritmo();
    }

    @Override
    public void run(){
        algoritmo();
    }

    private void algoritmo(){
        if (start == null || ending == null){
            distance = -1;
            stato.setValue(Worker.State.FAILED);
            return;
        }
        distance = 0;
        Queue<Page> codaURI = new LinkedBlockingQueue<>();
        codaURI.add(start);
        while (!codaURI.isEmpty()){
            if (interrupt) {
                stato.setValue(Worker.State.CANCELLED);
                return;
            }
            /*
            Prendo l'elemento da elaborare.
            NB. Nella prima esecuzione questo elemento è start.
             */
            Page toWatch = codaURI.poll();
            if (toWatch == ending){
                stato.setValue(Worker.State.SUCCEEDED);
                return;
            }
            distance ++;
            if (toWatch.ptdNumbers() > 0){
                /*
                Per ognuno dei puntati, la routine si preoccupa di prelevarlo dalla mappa
                e di verificarne la correttezza di diversità rispetto a null. Verifica se sia "arrivato all'altro capo".
                 */
                for (URI uri : toWatch.getPtd()){
                    Page getted = mapToWatch.get(uri);
                    if (getted != null){
                        if (getted == ending){
                            stato.setValue(Worker.State.SUCCEEDED);
                            return;
                        }
                        codaURI.add(getted);
                    }
                }
            }else{
                distance = -1;
                stato.setValue(Worker.State.FAILED);
                return;
            }
        }
        /*
        Se sono arrivato qui significa che l'algoritmo non ha trovato la pagina cercata.
        Setto lo stato failed con valore -1.
         */
        distance = -1;
        stato.setValue(Worker.State.FAILED);
    }

    @Override
    public Integer getData() {
        return distance;
    }

    @Override
    public Worker.State getTaskState() {
        return this.stato.getValue();
    }

    @Override
    public ReadOnlyObjectProperty<Worker.State> getWorkerState() {
        return this.stato;
    }
}
