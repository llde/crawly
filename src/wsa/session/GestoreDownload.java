package wsa.session;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Worker;
import javafx.scene.chart.PieChart;
import wsa.Settings;
import wsa.exceptions.VisitException;
import wsa.gui.TabFrame;
import wsa.gui.ThreadUtilities;
import wsa.web.CrawlerResult;
import wsa.web.WebFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by gaamda on 13/11/15.
 *
 * Rappresenta una esplorazione. Fa uso di classi quali Dominio,Seed,StatoDownload e GestoreDati.
 * Verrà fatto girare come runnable in un'esecutore.
 *
 * Un gestore dei download che, preso un dominio, una lista dei seeds ed un path, aggiunge uno stato, e, di conseguenza
 * può essere lanciato, usando solo siteCrawler forniti da WebFactory. Il gestore assume che tutto sia formattato
 * correttamente, quindi ogni dato è inviato al gestore in maniera corretta.
 *
 * */
public class GestoreDownload {

    private TabFrame mytab;                                        //La TabFrame di riferimento
    private final Dominio dom;                                     //Il Dominio dell'esplorazione
    private List<Seed> seeds;                                      //I Seeds dell'esplorazione
    private final Path path;                                       //Percorso di recupero esplorazione, quando necessario
    private StatoDownload stato;                                   //L'oggetto StatoDownload contenente il SiteCrawler che GestoreDownload vuole gestire

    private final SimpleObjectProperty<Page> pageWithMaxLinks = new SimpleObjectProperty<>(null);          //La pagina con il maggior numero di links, osservabile
    private final SimpleObjectProperty<Page> pageWithMaxPointers = new SimpleObjectProperty<>(null);       //La pagina che punta più pagine, osservabile

    private final ObservableMap<URI, Page> resultMap = FXCollections.synchronizedObservableMap(            //Mappa che lega ad ogni URI scaricato la sua pagina, osservabile
            FXCollections.observableHashMap()
    );
    private final ObservableList<Page> resultList = FXCollections.synchronizedObservableList(              //Lista dei risultati dell'esplorazione, osservabile
            FXCollections.observableArrayList()
    );
    {
        resultMap.addListener((MapChangeListener<URI, Page>) change -> {
            if (change.wasAdded()){
                if (!resultList.contains(change.getValueAdded()))
                    resultList.add(change.getValueAdded());
            }
        });
    }

    private GestoreDati dataset = null;          // TEST
    Thread toLaunch = null;                      // Thread che verrà usato per gestire il download delle pagine attraverso il Runnable runtoLaunch

    /**
     * Crea un gestore dei download.
     * @param d Un dominio dom.
     * @param s Una lista di seeds.
     * @param p Un percorso d'archiviazione.
     * @param m Modulo non nullo per le tabelle.
     */
    public GestoreDownload(Dominio d, List<Seed> s, Path p, Integer m, TabFrame tb) throws IOException, VisitException {
        this.seeds = (s == null? new ArrayList<>() : s);
        this.path = p;
        this.dataset = new GestoreDati(resultMap, m);
        this.mytab = tb;
        stato = new StatoDownload(WebFactory.getSiteCrawler(d == null ? null : d.getURI(), path));
        this.dom = Dominio.getDomainSneaky(stato.getWorker().getDomain().toString());
        if(seeds != null) seeds.forEach(seed -> stato.getWorker().addSeed(seed.getURI()));
        if(d == null){
            stato.getWorker().getLoaded().stream().forEach(this::recoverPageInfo);
            stato.getWorker().getErrors().stream().forEach(this::recoverPageInfo);
        }
    }

    public ObjectProperty<Page> getPageWithMaxLinks(){
        return this.pageWithMaxLinks;
    }

    public ObjectProperty<Page> getPageWithMaxPointers(){
        return this.pageWithMaxPointers;
    }

    /**
     * Cancella il gestore download. Non sarà più possibile eseguire nessun download con
     * questo gestore, attenzione all'uso. Questo comando è come DD, una "Dangerous Decision".
     */
    public void cancel(){
        if (stato.getStato() != Worker.State.CANCELLED) {
            stato.getWorker().cancel();
        }
    }

    /**
     * Sospende il gestore download.
     */
    public void pause(){
        toLaunch.interrupt();
        stato.getWorker().suspend();
    }

    /**
     * Avvia il gestore download.
     */
    public void start(){
        stato.getWorker().start();
        toLaunch = ThreadUtilities.CreateThread(runtoLaunch);
        toLaunch.start();
    }

    /**
     * Modifica il rango inserito precedentemente.
     * @param m Il rango da modificare.
     */
    public void setRango(Integer m){
        this.dataset.setRango(m);
    }

    /**
     * Ritorna i puntanti di uno specifico URI, se calcolati in questo momento.
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntanti.
     */
    public Set<URI> getPuntanti(URI uri){
        return this.dataset.getPuntanti(uri);
    }

    /**
     * Ritorna i puntati di uno specifico URI, se calcolati in questo momento.
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntati.
     */
    public Set<URI> getPuntati(URI uri){
        return this.dataset.getPuntati(uri);
    }

    /**
     * Ottiene le pagine entranti classificate per il modulo dichiarato nel costruttore.
     * @return La mappa osservabile con tutto il necessario per un istogramma.
     */
    public ObservableMap<Integer, Set<URI>> getClassifiedEntranti(){
        return this.dataset.getClassifiedEntranti();
    }

    /**
     * Ottiene le pagine uscenti classificate per il modulo dichiarato nel costruttore.
     * @return La mappa osservabile con tutto il necessario per un istogramma.
     */
    public ObservableMap<Integer, Set<URI>> getClassifiedUscenti(){
        return this.dataset.getClassifiedUscenti();
    }

    /**
     * Ottiene le pagine entranti divise per rango, già pronte per grafici tipo torta.
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    public ObservableList<PieChart.Data> getEntrantiPieData() {
        return this.dataset.getEntrantiPieData();
    }

    /**
     * Ottiene le pagine uscenti divise per rango, già pronte per grafici tipo torta.
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    public ObservableList<PieChart.Data> getUscentiPieData() {
        return this.dataset.getUscentiPieData();
    }

    /**
     * Ottiene i risultati ottenuti finora dal gestore dei download sottoforma di mappa.
     * Questa struttura dati è semplice e gestibile.
     * @return Una mappa di URI e FXcr con i risultati.
     */
    public ObservableMap<URI, Page> getResults(){
        return this.resultMap;
    }

    /**
     * Aggiunge un seed al worker di questo gestoreDownload, se lo fa assume che il seed sia nuovo, o comunque
     * questo metodo non effettua nessun controllo, nemmeno sul valore null.
     * @param s Il seed da aggiungere.
     */
    public void addSeed(Seed s){
        Worker.State st = this.getStato();
        if (!seeds.contains(s)) {
            seeds.add(s);
            this.stato.getWorker().addSeed(s.getURI());
        }
        if(st == Worker.State.SUCCEEDED){
            if(mytab != null) mytab.enablebuttons();
            else this.start();
        }
        toLaunch = ThreadUtilities.CreateThread(runtoLaunch);
    }

    /**
     * Ritorna la lista dei seeds aggiunti fino a questo momento.
     * @return La lista immodificabile dei seeds.
     */
    public List<Seed> getAddedSeeds(){
        return Collections.unmodifiableList(this.seeds);
    }

    /**
     * Ritorna la lista di pagine ottenute dalla mappa.
     * @return La lista di pagine.
     */
    public ObservableList<Page> getDataList(){
        return FXCollections.unmodifiableObservableList(this.resultList);
    }

    /**
     * Lo stato del gestore dei download, che corrisponde allo stato del worker interno.
     * Se lo stato interno non è definito, per definizione,
     * @return Lo stato di questo componente.
     */
    public synchronized Worker.State getStato(){
        if (this.stato == null) return null;
        return this.stato.getStato();
    }

    /*finchè gira, questo Runnable getta incessantemente dal SiteCrawler */
    //(quando ne ha, trasforma i risultati in oggetti Page e riassegna le informazioni)

    private Runnable runtoLaunch = () -> {
        while(stato.getStato() == Worker.State.RUNNING) {
            if (Thread.interrupted()) break;
            Optional<CrawlerResult> cr = stato.getWorker().get();
            if(cr.isPresent()){
                if(cr.get().uri != null){
                    recoverPageInfo(cr.get().uri);
                }
                else{
                    try {
                        Thread.sleep(Settings.config().RES_GRABBER_MILLIS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
        if(stato.getStato() == Worker.State.SUCCEEDED && mytab != null) mytab.pause();
    };

    /**
     * Ricevuto un URI, crea una pagina corrispondente ad esso e l'aggiunge alla mappa <URI,PAGINA> dei risultati.
     * Prova anche ad aggiornare la statistica sul massimo numero di links
     * Se il CralwerResult corrispondente all'uri non viene trovato prova a risottomettere la pagina al SiteCralwer.
     * Non è detto che l'implementazione lo consenta
     * @param ur l'URI da recuperare
     */
    private void recoverPageInfo(URI ur) {
        try {
            Page pg = new Page(stato.getWorker().get(ur));
            resultMap.putIfAbsent(ur, pg);
            updateMaxLinks(pg);
        } catch (Exception e) {
            try {
                if (stato.getWorker().getLoaded().contains(ur)) stato.getWorker().getLoaded().remove(ur);
                else if (stato.getWorker().getErrors().contains(ur)) stato.getWorker().getErrors().remove(ur);
                stato.getWorker().addSeed(ur);
                System.out.println("Provo a risottomettere l'uri: " + ur);
            }
            catch (Exception ew) {
                System.err.println("Non posso ottenere questo URI: "  + ur);
            }
        }
    }


    /**
     * Aggiorna il label dei Max Pointers se la pagina pg ha più puntanti del numero già segnalato come massimo
     * @param pg la pagina
     */
    protected void updateMaxPointers (Page pg){
        Platform.runLater(()-> {
            if (pageWithMaxPointers.getValue() == null || pageWithMaxPointers.getValue().ptrNumbers() < pg.ptrNumbers()) {
                pageWithMaxPointers.setValue(pg);
            }
        });
    }

    /**
     * Aggiorna il label dei Max Links se la pagina pg ha più links del numero già segnalato come massimo
     * @param pg la pagina
     */
    private void updateMaxLinks (Page pg){
        Platform.runLater(() -> {
            if (pageWithMaxLinks.getValue() == null) {
                pageWithMaxLinks.setValue(pg);
            } else if (pageWithMaxLinks.getValue().linksNumber() < pg.linksNumber()) {
                pageWithMaxLinks.setValue(pg);
            }
        });
    }



    public URI getDomain(){
        return stato.getWorker().getDomain();
    }
}
