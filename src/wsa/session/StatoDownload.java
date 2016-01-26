package wsa.session;

import javafx.concurrent.Worker;
import wsa.web.SiteCrawler;

/**
 * Created by gaamda on 13/11/15.
 * Prende un Sitecrawler e restituisce lo stato dell'elaborazione come enumerazione.
 */
class StatoDownload {

    private SiteCrawler siteCrawler;

    /**
     * Crea uno stato dei download
     * @param sc Il siteCrawler da associare.
     */
    public StatoDownload(SiteCrawler sc){
        siteCrawler = sc;
    }

    /**
     * Ottiene lo stato del sitecrawler corrente.
     * Assume che RUNNING stia per "crawling", CANCELLED per "cancellato", SCHEDULED per "pausa"
     * e SUCCEDED per visita completa.
     * @return Lo stato attuale del worker interno.
     */
    public Worker.State getStato(){
        if(siteCrawler.isCancelled()) return Worker.State.CANCELLED;
        if(siteCrawler.isRunning()) return Worker.State.RUNNING;
        if(!siteCrawler.isRunning() && siteCrawler.getToLoad().isEmpty()) return Worker.State.SUCCEEDED;
        else return Worker.State.SCHEDULED;
    }

    public synchronized SiteCrawler getWorker(){
        return this.siteCrawler;
    }

}
