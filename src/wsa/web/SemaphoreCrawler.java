package wsa.web;

import wsa.API.ConditonSemaphore;
import wsa.Constants;
import wsa.gui.ThreadUtilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Created by Lorenzo on 08/12/2015.
 *
 * Un' implementazione del Crawler che usa il Pattern Produttore/consumatore
 * ed i semafori per gestire lo stato

 */
public class SemaphoreCrawler implements Crawler {

    private enum crawlerState{INIT, RUNNING, CANCELLED, SUSPENDED, TERMINATED_DOWNLOAD , TERMINATED}

    private final AtomicReference<crawlerState> stato = new AtomicReference<>(crawlerState.INIT);
    private Set<URI> Scaricare = Collections.synchronizedSet(new ConcurrentSkipListSet<>());
    private Set<URI> Scaricati = Collections.synchronizedSet(new ConcurrentSkipListSet<>());
    private Set<URI> Errori = Collections.synchronizedSet(new ConcurrentSkipListSet<>());
    /* Un buffer per domarli, un buffer per trovarli
        un buffer per ghermirli e nel buio incatenarli
     */
    private final AtomicReference<Queue<CrawlerResult>> progress = new AtomicReference<>(new ConcurrentLinkedQueue<>());  //La coda dei CrawlerResult da Gettare
    private Queue<Future<LoadResult>>  buffer = new ConcurrentLinkedQueue<>();  //La coda dei future dei LoadResult sotoomessi
    private Set<URI> holder = new ConcurrentSkipListSet<>(); //Necessario nel caso in cui il produttore inizi un secondo giro, ed abbia tra gli Scaricare un URI già sottomesso ma non ancora processato dal produttore e quindi non ancora aggiunto a Scaricati, evita che venga sottomesso nuovamente.
    private Predicate<URI> pageLink;  //Il predicato per l'approvazione degli uri
    private final Semaphore maxresinqueue = new Semaphore(50);    //numero massimo di sottomissioni al consumatore in coda
    private final ConditonSemaphore<AtomicReference<crawlerState>> runpause = new ConditonSemaphore<>((pauss) -> pauss.get().equals(crawlerState.SUSPENDED), stato, 2, false);   //Per la gestione della pausa, si assicurerà che produttore e consumatore lavorino solo quando i Crawler è in funzione.
    private final AtomicReference<AsyncLoader> async = new AtomicReference<>();    //AsyncLoader atomico per poter essere gestito dai thread concorrenti

    private Runnable prod = () ->{
        while(true){
            if(Thread.currentThread().isInterrupted()) return;
            if(Scaricare.isEmpty() && holder.isEmpty() && buffer.isEmpty()){
                System.out.println("Ho finito di Scaricareeeee!!!!");
                stato.set(crawlerState.TERMINATED_DOWNLOAD);
                runpause.Disable();
                return;
            }
            if(Scaricare.isEmpty()) continue;
            Scaricare.stream().forEach((ur) ->{
                if(!holder.contains(ur)) {
                    try {
                        maxresinqueue.acquire();
                    } catch (InterruptedException ignored) {}
                    try {
                        runpause.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        buffer.add(async.get().submitLoad(ur.toURL()));
                        holder.add(ur);
                    } catch (MalformedURLException e) {
                        Errori.add(ur);
                        Scaricare.remove(ur);
                    }
                    catch (NullPointerException | RejectedExecutionException | IllegalStateException ignored){ return;} //Si verificano quando spengo l'AsyncLoader
                }
            });
        }
    };

    private Runnable consumer = () ->{
        while(true){
            if(Thread.currentThread().isInterrupted()) return;
            if(stato.get() == crawlerState.TERMINATED_DOWNLOAD) return;
            if(buffer.isEmpty()) continue;
            for(Future<LoadResult>  fut : buffer){
                try {
                    runpause.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(Thread.currentThread().isInterrupted()) break;
                if(fut.isDone()){
                    try {
                        LoadResult res = fut.get();
                        URI mainuri = null;
                        try {
                            mainuri = res.url.toURI();
                            CrawlerResult cres;
                            if(res.exc == null) {
                                if (pageLink.test(mainuri)) {
                                    List<String> links = res.parsed.getLinks();
                                    Map.Entry<List<URI>, List<String>> entri = ResolveLinks(mainuri, links);
                                    entri.getKey().forEach((ur) -> add(ur));
                                    cres = new CrawlerResult(mainuri, true, entri.getKey(), entri.getValue(), null);
                                } else {
                                    cres = new CrawlerResult(mainuri, false, null, null, null);
                                }
                                Scaricati.add(mainuri);
                            }
                            else{
                                cres = new CrawlerResult(mainuri, pageLink.test(mainuri), null,null,res.exc);
                                Errori.add(mainuri);
                            }
                            progress.get().add(cres);
                            holder.remove(mainuri);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                        finally {
                            Scaricare.remove(mainuri);
                            buffer.remove(fut);
                            maxresinqueue.release();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private Thread prodt = null;
    private Thread tcons = null;

    public SemaphoreCrawler(Collection<URI>  loaded, Collection<URI> toLoad, Collection<URI> errors, Predicate<URI> pagelink){
        async.set(WebFactory.getAsyncLoader());
        if(toLoad != null) Scaricare.addAll(toLoad);
        if(loaded != null) Scaricati.addAll(loaded);
        if(errors != null) Errori.addAll(errors);
        if(pagelink != null) pageLink = pagelink;
        else pageLink = (ur) -> true;
    }



    /**
     * Aggiunge un URI all'insieme degli URI da scaricare. Se però è presente
     * tra quelli già scaricati, quelli ancora da scaricare o quelli che sono
     * andati in errore, l'aggiunta non ha nessun effetto. Se invece è un nuovo
     * URI, è aggiunto all'insieme di quelli da scaricare.
     *
     * @param uri un URI che si vuole scaricare
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void add(URI uri) {
        throwIfCancelled();

        for (String ext : Constants.illegalExtension){
            if (uri.toString().endsWith(ext)) return;
        }
        for (String prot : Constants.illegalProtocols){
            if (uri.toString().startsWith(prot)) return;
        }

        if(Scaricare.contains(uri) || Scaricati.contains(uri) || Errori.contains(uri)) return;
        Scaricare.add(uri);
        if(stato.get() == crawlerState.TERMINATED_DOWNLOAD || stato.get() == crawlerState.TERMINATED) start();
    }

    /**
     * Inizia l'esecuzione del Crawler se non è già in esecuzione e ci sono URI
     * da scaricare, altrimenti l'invocazione è ignorata. Quando è in esecuzione
     * il metodo isRunning ritorna true.
     *
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void start() {
        throwIfCancelled();
        if(stato.get() == crawlerState.INIT || stato.get() == crawlerState.TERMINATED_DOWNLOAD || stato.get() == crawlerState.TERMINATED) {

            if (stato.get() == crawlerState.TERMINATED_DOWNLOAD  || stato.get() == crawlerState.TERMINATED) runpause.Enable(); // Il runner mette il semaforo in disable, riabilito.

            System.out.println("START CRAWLER - From Init, terminated, or terminated_download");
            prodt = ThreadUtilities.CreateThread(prod);
            tcons = ThreadUtilities.CreateThread(consumer);
            stato.set(crawlerState.RUNNING);
            prodt.start();
            tcons.start();
        }
        else if(stato.get() == crawlerState.SUSPENDED) {
            System.out.println("START CRAWLER - From suspended");
            stato.set(crawlerState.RUNNING);
            runpause.release();
        }
        System.out.println("Stato: " + stato.get());
    }

    /**
     * Sospende l'esecuzione del Crawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività del Crawler dovrebbe essere ridotta al minimo
     * possibile (eventuali thread dovrebbero essere terminati).
     *
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void suspend() {
        throwIfCancelled();
        if(stato.get() == crawlerState.RUNNING) stato.set(crawlerState.SUSPENDED);
    }

    /**
     * Cancella il Crawler per sempre. Dopo questa invocazione il Crawler non
     * può più essere usato. Tutte le risorse devono essere rilasciate.
     */
    @Override
    public void cancel() {
        prodt.interrupt();
        tcons.interrupt();
        stato.set(crawlerState.CANCELLED);
        async.get().shutdown();
    }

    /**
     * Ritorna il risultato relativo al prossimo URI. Se il Crawler non è in
     * esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     *
     * @return il risultato relativo al prossimo URI scaricato
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Optional<CrawlerResult> get() {
        throwIfCancelled();
        if(isRunning()) {
            CrawlerResult cre = progress.get().poll();
            if (cre != null) return Optional.of(cre);
            if(progress.get().isEmpty() && stato.get() == crawlerState.TERMINATED_DOWNLOAD) {
                stato.set(crawlerState.TERMINATED);
                return Optional.empty();
            }
            return Optional.of(new CrawlerResult(null, false, null, null, null));
        }
        else return Optional.empty();
    }

    /**
     * Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     *
     * @return l'insieme di tutti gli URI scaricati (mai null)
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Set<URI> getLoaded() {
        throwIfCancelled();
        return Scaricati;
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     *
     * @return l'insieme degli URI ancora da scaricare (mai null)
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Set<URI> getToLoad() {
        throwIfCancelled();
        return Scaricare;
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     *
     * @return l'insieme degli URI che hanno prodotto errori (mai null)
     * @throws IllegalStateException se il crawler è cancellato
     */
    @Override
    public Set<URI> getErrors() {
        throwIfCancelled();
        return Errori;
    }

    /**
     * Ritorna true se il Crawler è in esecuzione.
     *
     * @return true se il Crawler è in esecuzione
     */
    @Override
    public boolean isRunning() {
        return stato.get() == crawlerState.RUNNING || stato.get() == crawlerState.TERMINATED_DOWNLOAD;
    }

    /**
     * Ritorna true se il Crawler è stato cancellato. In tal caso non può più
     * essere usato.
     *
     * @return true se il Crawler è stato cancellato
     */
    @Override
    public boolean isCancelled() {
        return stato.get() == crawlerState.CANCELLED;
    }

    @Override
    public boolean isSuspended() {
        return stato.get() == crawlerState.SUSPENDED;
    }

    @Override
    public boolean isTerminated() {
        return stato.get() == crawlerState.TERMINATED;
    }

    /**
     * Risolve i links della pagina con l'uri della pagina stessa
     * Crea di link assoluti
     * @param thisuri  l'uri da usare per risovlere i link per ottenere un uri assoluto
     * @param links   la lista dei link
     * @return Una {@links Map.Entry} con Chiave una lista di URI Assoluti, come Valore una lista di stringhe impossibili da risolvere
     */
    private Map.Entry<List<URI>, List<String>> ResolveLinks(URI thisuri , List<String>  links){
        List<URI> goodlinks = new ArrayList<>();
        List<String>  badlinks = new ArrayList<>();
        links.stream().forEach((wannabeuri)-> {
            try {
                URI notabs = new URI(wannabeuri);
                URI absoluted = thisuri.resolve(notabs);
                goodlinks.add(absoluted);
                this.add(absoluted);
            } catch (URISyntaxException e) {
                badlinks.add(wannabeuri);
            }
        });
        return new AbstractMap.SimpleImmutableEntry<>(goodlinks, badlinks);
    }

    /**
     * Controlla se il SiteCralwer è cancellato, in caso lancia eccezione
     * @throws IllegalArgumentException
     */
    private void throwIfCancelled(){
        if(stato.get() == crawlerState.CANCELLED) throw new IllegalStateException("Questo cralwer è chiuso e non può più essere usato");
    }
}
