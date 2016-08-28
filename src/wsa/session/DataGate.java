package wsa.session;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.chart.PieChart;
import lombok.NonNull;
import lombok.Synchronized;
import wsa.elaborazioni.Executor;
import wsa.elaborazioni.TaskDistanceUri;
import wsa.gui.ThreadUtilities;
import wsa.web.CrawlerResult;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Represent a data structure for the session.
 * Is intended like a monitor, so basically allow multiple readings but just
 * one writing at time.
 * Assuming the use of pages objects.
 * The goal of this class is to achieve the 0% data fragmentation through the application.
 *
 * ALPHA CLASS <-- STILL NOT WORK
 *
 * @since 1.0 WIP
 * @author gaamda
 * @see Page
 * @see Observable
 */
public final class DataGate implements Observable {

    // Executor for multi-threading operations.
    //TODO possible bug here.
    private ExecutorService executor = ThreadUtilities.TrackExecutorService(
            Executors.newFixedThreadPool(30)
    );

    // Listeners of this data structure.
    private List<InvalidationListener> listeners = new ArrayList<>(); /* Listeners */

    // Page table. Actually contains downloaded pages.
    private final ObservableMap<URI, Page> downloadedPageTable = FXCollections.synchronizedObservableMap(
            FXCollections.observableHashMap()
    );

    // List of pages, reflect the table.
    private final ObservableList<Page> pageList = FXCollections.synchronizedObservableList(
            FXCollections.observableArrayList()
    );

    // Map of errors.
    private final ObservableMap<URI, List<String>> errorsLogs = FXCollections.synchronizedObservableMap(
            FXCollections.observableHashMap()
    );

    // Set the verbose bit in the data structure.
    private boolean verbose = false;

    //TODO better location for this data.
    private int rank = 5;
    private final ObservableList<PieChart.Data> entrantiPieData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());     //Lista degli entranti nel dominio (per il grafico a torta), osservabile
    private final ObservableList<PieChart.Data> uscentiPieData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());       //Lista degli uscenti dal dominio (per il grafico a torta), osservabile
    private final ObservableMap<Integer, Set<URI>> entranti = FXCollections.observableMap(new ConcurrentHashMap<>());     //Mappa che associa <NUMERO DI ENTRANTI, URI CHE NE HANNO QUEL NUMERO>, osservabile
    private final ObservableMap<Integer, Set<URI>> uscenti = FXCollections.observableMap(new ConcurrentHashMap<>());          //Mappa che associa <NUMERO DI USCENTI, URI CHE NE HANNO QUEL NUMERO>, osservabile


    /**
     * Constructor
     */
    public DataGate() {
        downloadedPageTable.addListener((MapChangeListener<URI, Page>) change -> {
            executor.submit(() -> updateLinks(change.getValueAdded()));
            executor.submit(() -> updatePieData(change.getValueAdded()));
        });
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get the distance from Page p1 to Page p2. Is not blocking, return the reference to the TaskDistance uri.
     *
     * @param p1 Page to search from.
     * @param p2 Page to search to.
     * @return Distance, if -1 return not valid.
     * @throws InterruptedException
     */
    public TaskDistanceUri getDistance(Page p1, Page p2) throws InterruptedException {
        TaskDistanceUri runner = new TaskDistanceUri(downloadedPageTable, p1, p2);
        Executor.perform(runner);
        return runner;
    }

    /**
     * Get the distance from Page p1 to Page p2. Is not blocking, return the reference to the TaskDistance uri.
     * It is blocking.
     *
     * @param p1 Page to search from.
     * @param p2 Page to search to.
     * @return Distance, if -1 return not valid.
     * @throws InterruptedException
     */
    public Integer getDistanceInteger(Page p1, Page p2) throws InterruptedException {
        TaskDistanceUri runner = new TaskDistanceUri(downloadedPageTable, p1, p2);
        runner.run();
        return runner.getData();
    }

    public ObservableList<Page> getDataList() {
        return this.pageList;
    }

    public ObservableMap<URI, List<String>> getErrorsLogs() {
        return errorsLogs;
    }

    /**
     * Compute distance from given page to all the others.
     * Blocking, may require time.
     *
     * @return Map of distances.
     * @throws InterruptedException
     */
    public Map<URI, Integer> getAllDistances(Page p) throws InterruptedException {
        Map<URI, Integer> results = new HashMap<>();
        for (Page page : downloadedPageTable.values()) {
            if (page.getPageLink() && page.getExc() == null) {
                /*
                Necessary condition for a URI to be valid: It MUST be in the domain
                and it MUST be downloaded without errors.
                 */
                results.put(page.getURI(), getDistanceInteger(p, page));
            }
        }
        return results;
    }

    /**
     * Put the given CrawlerResult, if not null, in the CrawlerResult table, overwriting the
     * existing record, if any. Notify all the listeners added.
     *
     * @param cres The CrawlerResult to add.
     */
    @Synchronized
    public void add(CrawlerResult cres) {
        if (cres == null || cres.uri == null) return;
        executor.submit(() -> {
            try {
                Page p = createPage(cres);
                downloadedPageTable.put(cres.uri, p); /* Using cres uri for warnings */
                notifyListeners();
            } catch (Exception ex) {
                error(cres.uri, "Page creation failed", verbose);
            }
        });
    }

    @Synchronized
    private void add(Page pg) {
        if (pg == null || pg.getURI() == null) return;
        downloadedPageTable.put(pg.getURI(), pg);
        pageList.add(pg);
        notifyListeners();
    }

    /**
     * Put the given CrawlerResult, if not null and if not present already, in the CrawlerResult table.
     * Notify all the listeners added if the add operation take place.
     *
     * @param cres The CrawlerResult to add.
     */
    @Synchronized
    public void addIfNotPresent(CrawlerResult cres) {
        if (cres == null || cres.uri == null) return;
        if (downloadedPageTable.containsKey(cres.uri)) return;
        add(cres);
    }

    /**
     * Return true if a consistency check is passed.
     * It is intended like a rapid check, using internal data structure.
     *
     * @return True id passed or false if not passed.
     */
    @Synchronized
    public boolean checkConsinstecy() {
        return downloadedPageTable.size() == pageList.size();
    }

    /**
     * Change in a whole the entire listeners list of this structure.
     *
     * @param listenersList If null, reset the list. Else, the list of listeners.
     */
    @Synchronized
    protected void changeListenerList(List<InvalidationListener> listenersList) {
        if (listeners == null) listeners = new ArrayList<>();
        else listeners = listenersList;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        if (listener != null && listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify all the invalidationListeners added.
     */
    private void notifyListeners() {
        listeners.forEach(Object::notify);
    }

    /**
     * Create a page object from a crawlerResult.
     *
     * @param cRes CrawlerResult needed for create a page.
     * @return Page or null.
     */
    private Page createPage(CrawlerResult cRes) {
        Page pg;
        try {
            pg = new Page(cRes);
        } catch (Exception e) {
            return null;
        }
        add(pg);
        return pg;
    }

    private void updateLinks(Page pg) {
        Set<URI> keys = pg.getPtd().stream().filter(downloadedPageTable.keySet()::contains)
                .collect(Collectors.toSet()); /* Collect a set of valid (Already downloaded) URIs */

        Collection<Page> pages = downloadedPageTable.values(); /* Collect all the pages. */
        pages.stream().filter(page -> keys.contains(page.getURI())).forEach(pageNeu -> pageNeu.update(pg)); /* Update */
    }

    private void error(URI cresURI, String cause, boolean notify) {
        List<String> errors = new ArrayList<>();
        errors.add(Date.from(Instant.now()).toString() + " @ " + cause);
        if (errorsLogs.keySet().contains(cresURI)) {
            errorsLogs.get(cresURI).addAll(errors);
        } else {
            errorsLogs.put(cresURI, errors);
        }
        if (notify) {
            System.out.println("[DG] Errors for uri: " + cresURI + " -- Log created.");
        }
    }


    /**
     * Ritorna i puntanti di uno specifico URI, se calcolati in questo momento.
     *
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntanti o null.
     */
    Set<URI> getPuntanti(URI uri) {
        if (downloadedPageTable.get(uri) != null) {
            return downloadedPageTable.get(uri).getPtr();
        }
        return null;
    }

    /**
     * Ritorna i puntati di uno specifico URI, se calcolati in questo momento.
     *
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntati o null.
     */
    Set<URI> getPuntati(URI uri) {
        if (downloadedPageTable.get(uri) != null) {
            return downloadedPageTable.get(uri).getPtd();
        }
        return null;
    }


//TODO found a better way.

    /**
     * Ottiene le pagine entranti divise per rango, già pronte per grafici tipo torta.
     *
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    public ObservableList<PieChart.Data> getEntrantiPieData() {
        return entrantiPieData;
    }

    /**
     * Ottiene le pagine uscenti divise per rango, già pronte per grafici tipo torta.
     *
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    public ObservableList<PieChart.Data> getUscentiPieData() {
        return uscentiPieData;
    }

    /**
     * Ottiene le pagine entranti classificate per il modulo dichiarato nel costruttore.
     * @return La mappa osservabile con tutto il necessario per un istogramma.
     */
    ObservableMap<Integer, Set<URI>> getClassifiedEntranti(){
        return this.entranti;
    }

    /**
     * Ottiene le pagine uscenti classificate per il modulo dichiarato nel costruttore.
     * @return La mappa osservabile con tutto il necessario per un istogramma.
     */
    ObservableMap<Integer, Set<URI>> getClassifiedUscenti(){
        return this.uscenti;
    }

    /**
     * Elaborazione sequenziale dei dati dei grafici.
     * Riceve una pagina, aggiorna le mappe degli entranti e degli uscenti dal dominio usando i dati di quest'ultima.
     * @param p La pagina della quale eseguire l'elaborazione.
     */
    private void updatePieData(Page p){

        if (p == null) return;
        if (p.getURI() == null) return;

        /*
        Si rende necessario eliminare la precedente referenza; giacchè gli entranti vengono modificati
        in real time l'uri di p potrebbe già essere presente nella mappa, ma con un numero di entranti non aggiornato.*/

        for (Map.Entry<Integer, Set<URI>> entry : entranti.entrySet())
            if (entry.getValue().contains(p.getURI()))
                entry.getValue().remove(p.getURI());

        /* reinserisce gli uri entranti per quel rango, e l'uri di p (rango aggiornato)*/

        Set<URI> toadd = new HashSet<>();
        toadd.add(p.getURI());
        int intKey = getRango(p.linksEntrantiNumber());
        if (entranti.containsKey(intKey)){
            toadd.addAll(entranti.get(intKey));
        }
        entranti.put(intKey, toadd);
        updatePie(entrantiPieData,entranti);
       /* reinserisce gli uri uscenti per quel rango, e l'uri di p (il rango non è cambiato)
       ma è considerata la possibilità che quest'ultimo non fosse precedentemente contenuto */

        toadd = new HashSet<>();
        toadd.add(p.getURI());
        int uscKey = getRango(p.linksUscentiNumber());
        if (uscenti.containsKey(uscKey)){
            toadd.addAll(uscenti.get(uscKey));
        }
        uscenti.put(uscKey, toadd);
        updatePie(uscentiPieData,uscenti);
    }


    /**
     * Ottiene il rango di una pagina, partendo dal numero dei suoi links.
     * (Dato il numero dei links, restituisce il Rango)
     * @param nl Il numero di links.
     * @return Un intero rappresentante il rango.
     */
    private Integer getRango(Integer nl){
        if (nl == null || nl == 0) return 0;
        if (nl < 1) nl = 1;
        return nl / this.rank;
    }

    /**
     * Setta il rango per i grafici di sessione ed esegue in modo asincrono l'aggiornamento della mappa
     * dei risultati.
     * In poche parole, settato il nuovo rango ripartiziona tutte le pagine scaricate nelle mappe (entranti,uscenti,ecc.) in base ad esso.
     * @param m Il nuovo rango.
     */
    protected void setRango(Integer m) {
        if (m > 1 && !Objects.equals(m, this.rank)) {
            this.rank = m;
            entranti.clear();
            uscenti.clear();
            executor.submit(() -> downloadedPageTable.values().stream().forEach(this::updatePieData));
        }
    }

    /**
     * Genera i dati relativi alle liste per i grafici di entranti e uscenti.
     * Aggiorna le mappe usate dai grafici a torta usando le mappe degli degli entranti e degli uscenti.
     * @param dataList La datalist di PieChart.Data da aggiornare.
     * @param URIMap La mappa di uri da cui prendere i dati. Questa mappa è costruita con un indice intero ed un
     *               set di URI come elemento. Il metodo creerà PieChart.Data(indice intero, size set).
     */
    private void updatePie(ObservableList<PieChart.Data> dataList, ObservableMap<Integer, Set<URI>> URIMap){
        Platform.runLater(()-> dataList.clear());
        URIMap.forEach((k,v) -> Platform.runLater(() -> dataList.add(new PieChart.Data(k*rank + " - " + ((k*rank) + (rank -1)), v.size()))));
    }

}