package wsa.session;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import wsa.exceptions.DominioException;
import wsa.web.CrawlerResult;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Rappresenta una pagina, scaricata o tentata di scaricare.
 * Tiene traccia di tutti gli insiemi di links necessari per il buon funzionamento
 * dell'architettura. Completamente osservabile per compatibilità nella gui in JavaFX.
 * Una pagina tiene traccia delle altre pagine che la puntano, e che punta.
 */
public class Page extends Observable{

    private final URI uri;                                                                    //L'uri corrispondente al CrawlerResult che ha dato origine alla pagina
    private final BooleanProperty follow = new SimpleBooleanProperty(false);                   //Indica se la pagina sia interna al dominio

    private final Exception exc;
    private final AtomicReference<ObservableSet<URI>> ptd = new AtomicReference<>(FXCollections.observableSet(new HashSet<>()));                     //links a cui punta la pagina
    private final AtomicReference<ObservableSet<URI>> ptr = new AtomicReference<>(FXCollections.observableSet(new HashSet<>()));                    //links che puntano a questa pagina
    private final AtomicReference<ObservableSet<URI>> linksEntranti = new AtomicReference<>(FXCollections.observableSet(new HashSet<>()));        //links entranti
    private final AtomicReference<ObservableSet<URI>> linksUscenti = new AtomicReference<>(FXCollections.observableSet(new HashSet<>()));        //links uscenti
    private final List<URI> links = new ArrayList<>();                                           //tutti i links dal CrawlerResult
    private final List<String> errRawLinks = new ArrayList<>();                                //tutti il links che hanno dato errore nel CrawlerResult

    private final ObservableMap<URI, Page> ptrPAGES = FXCollections.observableHashMap();                                                  //Mappa che assegna ad ogni URI le pagine che lo puntano
    private final ObservableMap<URI, Page> ptdPAGES = FXCollections.observableHashMap();                                                 //Mappa che assegna ad ogni URI le pagine che punta

    /**
     * Genera una pagina a partire da un crawlerResult.
     * Una pagina è un oggetto che tiene traccia dei collegamenti tra se stessa e gli altri oggetti dello stesso tipo,
     * data una mappa di risultati infatti, riesce a cambiare il suo stato, passando da oggetto a se stante ad oggetto
     * con collegamenti verso altri oggetti.
     * Presuppone di conoscere la mappa dove le altre pagine sono caricate.
     * Le eccezioni potrebbero essere evitate, ma devono far prendere atto allo sviluppatore di cosa sta trattando,
     * distruggere questo oggetto è distruggere una parte del cuore di crawly. Si potrebbe pensare in uno sviluppo futuro
     * un'interfaccia, probabilmente si farà poi.
     *
     * @param cr Il risultato di uno scaricamento del crawler.
     * @throws IllegalArgumentException Se cr.uri == null.
     * @throws DominioException Se l'aggiunta dell'uri non va a buon fine.
     * @throws MalformedURLException Se l'aggiunta dell'uri non va a buon fine.
     * @see Page
     * @see Seed
     * @see Dominio
     * @see DominioException
     * @see UUID
     * @see javafx.scene.web.WebEngine
     */
    public Page(CrawlerResult cr) {
        if (cr.uri == null) throw new IllegalArgumentException("URI == null");

        uri = cr.uri;
        follow.setValue(cr.linkPage);
        exc = cr.exc;

        if (cr.links != null) { /* Se i links sono diversi da null */
            links.addAll(cr.links);
            ptd.get().addAll(cr.links.stream().collect(Collectors.toSet()));
            ptd.get().stream().filter(link -> !link.equals(this.uri)).forEach(linksUscenti.get()::add);
        }

        if (cr.errRawLinks != null) {
            errRawLinks.addAll(cr.errRawLinks);
        }
    }

    /**
     * Forza l'aggiornamento rispetto ad una pagina Page.
     * Aggiorna la pagina passata aggiungendo ai puntanti di essa questa pagina, la mappa di questa pagina che contiene tutte le pagine che punta,
     * e se la pagina passata punta anch'essa questa pagina, aggiorna i puntanti a questa pagina.
     * @param page La pagina verso la quale aggiornare.
     */
    protected void update(Page page){
        if (ptd.get() != null && ptd.get().contains(page.getURI())){
            page.addPtr(this);
            ptdPAGES.put(page.getURI(), page);
        }
        if (page.getPtd() != null && page.getPtd().contains(this.getURI())){
            addPtr(page);
        }
    }

    /**
     * Ottiene un oggetto di tipo {@link CrawlerResult} partendo dalla pagina. La pagina non verrà cancellata,
     * l'oggetto non è presente nella memoria della pagina. Verrà creato ex-novo.
     * @return Il {@link CrawlerResult} per questa pagina.
     */
    CrawlerResult toCrawlerResult(){
        return new CrawlerResult(uri, this.follow.get(), links, errRawLinks, exc);
    }


    /**
     * Ottiene l'uri relativo a questa pagina.
     * @return L'uri.
     */
    public URI getURI(){
        return this.uri;
    }

    /**
     * Ottiene l'eccezione di questa pagina. Da specifiche ritorna null.
     * @return L'eccezione o null.
     */
    public Exception getExc() {
        return exc;
    }

    /**
     * Ritorna un pageLink true/false. Indica se la pagina viene seguita, cioè, se interna al dominio.
     * @return True se interna, false altrimenti.
     */
    public boolean getPageLink(){
        return follow.get();
    }

    /**
     * Ritorna il numero dei links.
     * @return Il numero dei links.
     */
    public Integer linksNumber(){
        return links.size();
    }

    /**
     * Ritorna il numero dei che hanno generato errore.
     * @return Il numero dei links errati.
     */
    public Integer errsNumber(){
        return errRawLinks.size();
    }

    /**
     * Ritorna il numero dei links entranti.
     * @return Il numero dei links entranti.
     */
    public Integer linksEntrantiNumber(){
        return linksEntranti.get().size();
    }

    /**
     * Ritorna il numero dei links uscenti.
     * @return Il numero dei links uscenti.
     */
    public Integer linksUscentiNumber(){return linksUscenti.get().size();}

    /**
     * Ritorna il numero dei links puntati.
     * @return Il numero dei links puntati.
     */
    public Integer ptdNumbers(){
        return ptd.get().size();
    }

    /**
     * Ritorna il numero dei links che puntano la pagina.
     * @return Il numero dei links puntanti.
     */
    public Integer ptrNumbers(){
        return ptr.get().size();
    }

    /**
     * Ottiene i links puntati.
     * Si assume null il valore non presente.
     * @return Un set di links.
     */
    public ObservableSet<URI> getPtd(){
        return this.ptd.get();
    }

    /**
     * Ottiene i links che puntano la pagina.
     * @return Un set di links.
     */
    public ObservableSet<URI> getPtr(){
        return this.ptr.get();
    }

    /**
     * Ottiene la mappa dei links puntati.
     * @return La mappa uri-pagina.
     */
    public ObservableMap<URI, Page> getPtdMap(){
        return this.ptdPAGES;
    }

    /**
     * Ottiene la mappa dei links che puntano la pagina.
     * Si assume null il valore non presente.
     * @return La mappa uri-pagina.
     */

    public ObservableMap<URI, Page> getPtrMap(){
        return this.ptrPAGES;
    }
    /**
     * Aggiunge un uri all'insieme dei puntanti per questa pagina.
     * @param ptr l'uri da aggiungere a questa pagina.
     */
    public void addPtr(Page ptr){
        this.ptr.get().add(ptr.getURI());
        this.ptrPAGES.put(ptr.getURI(), ptr);
    }

    /**
     * Ottiene i links entranti.
     * @return Un set di links.
     */
    public ObservableSet<URI> getEntranti(){return this.linksEntranti.get();}

    /**
     * Ottiene i links uscenti.
     * @return Un set di links.
     */
    public ObservableSet<URI> getUscenti(){return this.linksUscenti.get();}

    @Override
    public boolean equals(Object other){
        if (other != null && getClass() == other.getClass()){
            Page o = (Page)other;
            return uri.equals(o.uri) && links.size() == o.links.size()
                    && errRawLinks.size() == o.errRawLinks.size();
        }
        return false;
    }

    /**
     * Per sapere come si usa l'hashcode nelle collections, riferirsi alla documentazione.
     * Questa pagina può tornare utile: https://en.wikipedia.org/wiki/Java_hashCode%28%29;
     * @return L'hashcode per questo oggetto.
     */
    @Override
    public int hashCode(){
        return this.uri.hashCode();
    }

    @Override
    public String toString(){
        return this.uri.toString();
    }
}
