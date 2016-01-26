package wsa.elaborazioni;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import wsa.API.Wrap;
import wsa.session.GestoreDownload;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaamda on 14/12/15.
 *
 * Task per la distanza tra tutte le coppie di URI in una determinata mappa.
 * Usa Task forniti da {@link TaskDistanceUri} per il calcolo che viene eseguito in modo del tutto
 * concorrente. L'operazione potrebbe prendere del tempo.
 * Estende Runnable per consentirne il lancio in un thread.
 * Il risultato sarà una mappa con chiave intera (Rappresentante la distanza) e valori oggetti Wrap indicanti
 * gli URI relativi a quella distanza. Notare che per ogni distanza più Wrap sono possibili, e sono pertanto
 * contenuti in Set.
 */
public class TaskDistanceAllUrisPages implements Task<Map<Integer,Set<Wrap<URI,URI>>>>, Runnable {

    private ObjectProperty<Worker.State> stato = new SimpleObjectProperty<>(Worker.State.SCHEDULED);
    private final Map<Integer, Set<Wrap<URI, URI>>> resultsURI = new ConcurrentHashMap<>(); /* Mappa dei risultati */
    private final GestoreDownload gd;
    private boolean interrupt = false;

    /**
     * Crea un Task che elabora tutte le distanza di URI di pagine interne al dominio
     * e correttamente scaricate dall'applicazione.
     * @param gd Un Gestore download.
     */
    public TaskDistanceAllUrisPages(GestoreDownload gd){
        this.gd = gd;
    }

    @Override
    public void requestInterrupt(){
        interrupt = true;
        this.stato.setValue(Worker.State.CANCELLED);
    }

    @Override
    public void esegui(Executor e) {
        algoritmo();
    }

    @Override
    public void run(){
        algoritmo();
    }

    /*
    L'algoritmo di ricerca distanze usa un metodo dell'oggetto Page, per ottenere le distanze di
    ogni pagina con tutte le altre. In base a questi risultati riempie, classificando per distanza,
    la mappa dei risultati.
    */
    private void algoritmo(){
        if (gd == null){
            stato.setValue(Worker.State.FAILED);
            return;
        }

        /*
        Per ogni pagina appartenente al dominio (pagelink true ed exc null), ne ottiene la distanza con tutte le
        altre pagine, e per ogni entry della mappa che ne risulta elabora la posizione per la mappa dei risultati.
        Questo algoritmo esegue prima un for, e poi un secondo for annidato. Supponendo n pagine, e quindi n entry,
        la complessità si aggira intorno ad O(n^2).
         */
        gd.getDataList().forEach(page -> {
            if (page.getPageLink() && page.getExc() == null) {
                try {
                    Map<URI, Integer> elab = page.getAllDistances(); /* Ottiene le distanze della pagina */
                    elab.forEach((k, v) -> {
                        if (interrupt){ /* Verifica per l'interruzione */
                            return;
                        }
                        Set<Wrap<URI, URI>> toadd = new HashSet<>();
                        toadd.add(new Wrap<>(page.getURI(), k)); /* Crea un Wrap con l'URI della pagina e la chiave dell'entry */
                        resultsURI.merge(v, toadd, (wraps, wraps2) -> { /* Aggiunge ai risultati discriminando per il valore */
                            wraps.addAll(wraps2);
                            return wraps;
                        });
                    });
                } catch (InterruptedException e) { /* Ogni errore generato causa il fallimento dell'algoritmo */
                    stato.setValue(Worker.State.FAILED);
                }
            }
        });
        stato.setValue(Worker.State.SUCCEEDED); /* Terminato */
    }

    @Override
    public Map<Integer, Set<Wrap<URI, URI>>> getData() {
        return resultsURI;
    }

    /**
     * Se la computazione è terminata correttamente lo stato sarà SUCCEDED.
     * In questo specifico Task, però, il FAILED non si riferisce ad un fallimento totale nella computazione,
     * ma accade nel momento in cui almeno una coppia è stata (per un errore) esclusa dalla computazione, ergo,
     * il risultato non è garantito accurato al 100%. Spetta a chi usa l'algoritmo scegliere cosa mostrare.
     * La computazione entra in FAILED non appena la coppia è esclusa.
     * @return Lo stato della computazione come enum di tipo Worker.
     */
    @Override
    public Worker.State getTaskState() {
        return stato.getValue();
    }

    @Override
    public ReadOnlyObjectProperty<Worker.State> getWorkerState() {
        return stato;
    }
}
