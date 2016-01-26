package wsa.session;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.chart.PieChart;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaamda on 15/11/15.
 *
 * Thread di gestione dei dati a partire da una mappa {@link ObservableList}.
 * Elabora {@link Page} per ottenere i risultati e fornisce, tramite i suoi metodi i dati
 * di una visita. Questa classe non esegue le computazioni, ma prepara i dati cosicché possano
 * essere usati da ogni parte dell'applicazione.
 * Elabora i dati per i grafici a torta e per le tabelle con le informazioni degli entranti/uscenti (click sui grafici a torta quindi).
 */
public class GestoreDati extends Thread {

    private final ObservableMap<URI, Page> uri_pagina;                                                                                           //Mappa che associa ad ogni URI la sua pagina, osservabile
    private final ObservableMap<Integer, Set<URI>> entranti = FXCollections.observableMap(new ConcurrentHashMap<>());     //Mappa che associa <NUMERO DI ENTRANTI, URI CHE NE HANNO QUEL NUMERO>, osservabile
    private final ObservableMap<Integer, Set<URI>> uscenti = FXCollections.observableMap(new ConcurrentHashMap<>());          //Mappa che associa <NUMERO DI USCENTI, URI CHE NE HANNO QUEL NUMERO>, osservabile
    private final ObservableList<PieChart.Data> entrantiPieData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());     //Lista degli entranti nel dominio (per il grafico a torta), osservabile
    private final ObservableList<PieChart.Data> uscentiPieData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());       //Lista degli uscenti dal dominio (per il grafico a torta), osservabile
    private Integer rango = 5;                                                                                                                      //Rango predefinito per il partizionamento in base al numero degli entranti/uscenti

    /**
     * Ottiene il rango di una pagina, partendo dal numero dei suoi links.
     * (Dato il numero dei links, restituisce il Rango)
     * @param nl Il numero di links.
     * @return Un intero rappresentante il rango.
     */
    private Integer getRango(Integer nl){
        if (nl == null || nl == 0) return 0;
        if (nl < 1) nl = 1;
        return nl / this.rango;
    }

    /**
     * Ottiene la mappa dei risultati non modificabile di tipo {@link ObservableMap}
     * @return La mappa, non modificabile, dei risultati.
     */
    protected ObservableMap<URI, Page> getMap(){
        return FXCollections.unmodifiableObservableMap(this.uri_pagina);
    }

    /**
     * Setta il rango per i grafici di sessione ed esegue in modo asincrono l'aggiornamento della mappa
     * dei risultati.
     * In poche parole, settato il nuovo rango ripartiziona tutte le pagine scaricate nelle mappe (entranti,uscenti,ecc.) in base ad esso.
     * @param m Il nuovo rango.
     */
    protected void setRango(Integer m){
        if (m > 1 && !Objects.equals(m, this.rango)){
            this.rango = m;
            entranti.clear();
            uscenti.clear();
            uri_pagina.values().stream().forEach(this::elaboraEntrantiUscenti);
            updatePie(entrantiPieData, entranti);
            updatePie(uscentiPieData, uscenti);
        }
    }

    /**
     * Crea un gestore dei dati, che presa in consegna una mappa di uri, restituisce le possibili
     * strutture necessarie all'applicazione.
     * @param m La mappa da processare, esplicitamente osservabile.
     * @param mod Il modulo per i dati relativi ai grafici. Default = 5.
     */
    GestoreDati(ObservableMap<URI, Page> m, Integer mod){
        uri_pagina = m;
        if (mod > 0)
            rango = mod;
        else
            rango = 5;

        uri_pagina.addListener((InvalidationListener) observable -> {
            uri_pagina.values().forEach(this::elaboraEntrantiUscenti);
            Platform.runLater(() ->{
                updatePie(entrantiPieData, entranti);
                updatePie(uscentiPieData, uscenti);
            });
        });
    }

    /**
     * Elaborazione sequenziale dei dati dei grafici.
     * Riceve una pagina, aggiorna le mappe degli entranti e degli uscenti dal dominio usando i dati di quest'ultima.
     * @param p La pagina della quale eseguire l'elaborazione.
     */
    private void elaboraEntrantiUscenti(Page p){

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

       /* reinserisce gli uri uscenti per quel rango, e l'uri di p (il rango non è cambiato)
       ma è considerata la possibilità che quest'ultimo non fosse precedentemente contenuto */

        toadd = new HashSet<>();
        toadd.add(p.getURI());
        int uscKey = getRango(p.linksUscentiNumber());
        if (uscenti.containsKey(uscKey)){
            toadd.addAll(uscenti.get(uscKey));
        }
        uscenti.put(uscKey, toadd);
    }

    /**
     * Genera i dati relativi alle liste per i grafici di entranti e uscenti.
     * Aggiorna le mappe usate dai grafici a torta usando le mappe degli degli entranti e degli uscenti.
     * @param dataList La datalist di PieChart.Data da aggiornare.
     * @param URIMap La mappa di uri da cui prendere i dati. Questa mappa è costruita con un indice intero ed un
     *               set di URI come elemento. Il metodo creerà PieChart.Data(indice intero, size set).
     */
    private synchronized void updatePie(ObservableList<PieChart.Data> dataList, ObservableMap<Integer, Set<URI>> URIMap){
        dataList.clear();
        URIMap.forEach((k,v) -> dataList.add(new PieChart.Data(k*rango + " - " + ((k*rango) + (rango -1)), v.size())));
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
     * Ottiene le pagine entranti divise per rango, già pronte per grafici tipo torta.
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    ObservableList<PieChart.Data> getEntrantiPieData() {
        return entrantiPieData;
    }

    /**
     * Ottiene le pagine uscenti divise per rango, già pronte per grafici tipo torta.
     * @return La lista osservabile con tutto il necessario per un grafico.
     */
    ObservableList<PieChart.Data> getUscentiPieData() {
        return uscentiPieData;
    }

    /**
     * Ritorna i puntanti di uno specifico URI, se calcolati in questo momento.
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntanti o null.
     */
    Set<URI> getPuntanti(URI uri){
        if (uri_pagina.get(uri) != null){
            return uri_pagina.get(uri).getPtr();
        }
        return null;
    }

    /**
     * Ritorna i puntati di uno specifico URI, se calcolati in questo momento.
     * @param uri L'uri da cercare.
     * @return Il set degli uri puntati o null.
     */
    Set<URI> getPuntati(URI uri){
        if (uri_pagina.get(uri) != null){
            return uri_pagina.get(uri).getPtd();
        }
        return null;
    }
}
