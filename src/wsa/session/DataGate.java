package wsa.session;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.Synchronized;
import wsa.elaborazioni.Executor;
import wsa.elaborazioni.TaskDistanceUri;
import wsa.gui.ThreadUtilities;
import wsa.web.CrawlerResult;

import java.net.URI;
import java.util.*;
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
public final class DataGate implements Observable{

    private ExecutorService executor = ThreadUtilities.TrackExecutorService(
            Executors.newFixedThreadPool(30)
    );

    private List<InvalidationListener> listeners = new ArrayList<>(); /* Listeners */

    private final ObservableMap<URI, CrawlerResult> crawlerResultsTable = FXCollections.synchronizedObservableMap(
            FXCollections.observableHashMap()
    );

    //TODO è propro necessario tenersi sia le pagine sia i cralwer result?
    private final ObservableMap<URI, Page> downloadedPageTable = FXCollections.synchronizedObservableMap(
            FXCollections.observableHashMap()
    );

    private final ObservableList<Page> pageList = FXCollections.synchronizedObservableList(
            FXCollections.observableArrayList()
    );



    public DataGate(){
        crawlerResultsTable.addListener((MapChangeListener<URI, CrawlerResult>) change -> {
            executor.submit(() -> createPage(change.getValueAdded()));
        });
        downloadedPageTable.addListener((MapChangeListener<URI, Page>) change -> {
            executor.submit(() -> updateLinks(change.getValueAdded()));
        });
    }

    /**
     * Get the distance from Page p1 to Page p2. Is not blocking, return the reference to the TaskDistance uri.
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

    public ObservableList<Page> getDataList(){
        return this.pageList;
    }

    /**
     * Compute distance from given page to all the others.
     * Blocking, may require time.
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
     * @param cres The CrawlerResult to add.
     */
    @Synchronized
    public void add(CrawlerResult cres){
        if (cres == null || cres.uri == null) return;
        crawlerResultsTable.put(cres.uri, cres);
        notifyListeners();
    }

    @Synchronized
    private void add(Page pg){
        if (pg == null || pg.getURI() == null) return;
        downloadedPageTable.put(pg.getURI(), pg);
        pageList.add(pg);
        notifyListeners();
    }

    /**
     * Put the given CrawlerResult, if not null and if not present already, in the CrawlerResult table.
     * Notify all the listeners added if the add operation take place.
     * @param cres The CrawlerResult to add.
     */
    @Synchronized
    public void addIfNotPresent(CrawlerResult cres){
        if (cres == null || cres.uri == null) return;
        if (crawlerResultsTable.containsKey(cres.uri)) return;
        add(cres);
    }

    /**
     * Return true if a consistency check is passed.
     * It is intended like a rapid check, using internal data structure.
     * @return True id passed or false if not passed.
     */
    @Synchronized
    public boolean checkConsinstecy(){
        return crawlerResultsTable.keySet().equals(downloadedPageTable.keySet());
    }

    /**
     * Change in a whole the entire listeners list of this structure.
     * @param listenersList If null, reset the list. Else, the list of listeners.
     */
    @Synchronized
    protected void changeListenerList(List<InvalidationListener> listenersList){
        if (listeners == null) listeners = new ArrayList<>();
        else listeners = listenersList;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        if (listener != null && !listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        if (listener != null && listeners.contains(listener)){
            listeners.remove(listener);
        }
    }

    /**
     * Notify all the invalidationListeners added.
     */
    private void notifyListeners(){
        listeners.forEach(Object::notify);
    }

    /**
     * Create a page object from a crawlerResult.
     * @param cRes CrawlerResult needed for create a page.
     * @return Page or null.
     */
    private Page createPage(CrawlerResult cRes){
        Page pg;
        try {
            pg = new Page(cRes);
        } catch (Exception e) {
            return null;
        }
        add(pg);
        return pg;
    }

    private void updateLinks(Page pg){
        Set<URI> keys = pg.getPtd().stream().filter(downloadedPageTable.keySet()::contains)
                .collect(Collectors.toSet()); /* Collect a set of valid (Already downloaded) URIs */

        Collection<Page> pages = downloadedPageTable.values(); /* Collect all the pages. */
        pages.stream().filter(page -> keys.contains(page.getURI())).forEach(pageNeu -> pageNeu.update(pg)); /* Update */
    }

}
