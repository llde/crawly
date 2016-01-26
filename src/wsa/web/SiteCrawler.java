package wsa.web;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

/** Un crawler specializzato per siti web. Se il SiteCrawler è stato creato con una
 * directory per l'archiviazione, allora ogni 30 secondi durante l'esplorazione
 * l'archivio deve essere aggiornato in modo tale che se per qualsiasi motivo
 * l'esplorazione si interrompe, l'esplorazione può essere ripresa senza perdere
 * troppo lavoro. Inoltre l'archivio deve essere aggiornato in caso di sospensione
 * (metodo {@link SiteCrawler#suspend()}) e quando l'esplorazione termina normalmente. */
public interface SiteCrawler {
    /** Controlla se l'URI specificato è un dominio. È un dominio se è un URI
     * assoluto gerarchico in cui la parte authority consiste solamente
     * nell'host (che può essere vuoto), ci può essere il path ma non ci
     * possono essere query e fragment.
     * @param dom  un URI
     * @return true se l'URI specificato è un dominio */
    static boolean checkDomain(URI dom) {
        if (dom.getAuthority() == null && dom.getHost() == null) {
            return (dom.isAbsolute() && dom.getFragment() == null && dom.getQuery() == null);
        }
        return dom.getAuthority() != null && dom.getHost() != null && (dom.isAbsolute() && dom.getAuthority().equals(dom.getHost()) && dom.getFragment() == null && dom.getQuery() == null);
    }

    /** Controlla se l'URI seed appartiene al dominio dom. Si assume che dom
     * sia un dominio valido. Quindi ritorna true se dom.equals(seed) or not
     * dom.relativize(seed).equals(seed).
     * @param dom  un dominio
     * @param seed  un URI
     * @return true se seed appartiene al dominio dom */
    static boolean checkSeed(URI dom, URI seed) {
        return (dom.equals(seed) || !dom.relativize(seed).equals(seed));
    }

    /** Aggiunge un seed URI. Se però è presente tra quelli già scaricati,
     * quelli ancora da scaricare o quelli che sono andati in errore,
     * l'aggiunta non ha nessun effetto. Se invece è un nuovo URI, è aggiunto
     * all'insieme di quelli da scaricare.
     * @throws IllegalArgumentException se uri non appartiene al dominio di
     * questo SuteCrawlerrawler
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @param uri  un URI */
    void addSeed(URI uri);

    /** Inizia l'esecuzione del SiteCrawler se non è già in esecuzione e ci sono
     * URI da scaricare, altrimenti l'invocazione è ignorata. Quando è in
     * esecuzione il metodo isRunning ritorna true.
     * @throws IllegalStateException se il SiteCrawler è cancellato */
    void start();

    /** Sospende l'esecuzione del SiteCrawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività dovrebbe essere ridotta al minimo possibile
     * (eventuali thread dovrebbero essere terminati). Se è stata specificata
     * una directory per l'archiviazione, lo stato del crawling è archiviato.
     * @throws IllegalStateException se il SiteCrawler è cancellato */
    void suspend();

    /** Cancella il SiteCrawler per sempre. Dopo questa invocazione il
     * SiteCrawler non può più essere usato. Tutte le risorse sono
     * rilasciate. */
    void cancel();

    /** Ritorna il risultato relativo al prossimo URI. Se il SiteCrawler non è
     * in esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return  il risultato relativo al prossimo URI scaricato */
    Optional<CrawlerResult> get();

    /** Ritorna il risultato del tentativo di scaricare la pagina che
     * corrisponde all'URI dato.
     * @param uri  un URI
     * @throws IllegalArgumentException se uri non è nell'insieme degli URI
     * scaricati né nell'insieme degli URI che hanno prodotto errori.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return il risultato del tentativo di scaricare la pagina */
    CrawlerResult get(URI uri);

    /** Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme di tutti gli URI scaricati (mai null) */
    Set<URI> getLoaded();

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme degli URI ancora da scaricare (mai null) */
    Set<URI> getToLoad();

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme degli URI che hanno prodotto errori (mai null) */
    Set<URI> getErrors();

    /** Ritorna true se il SiteCrawler è in esecuzione.
     * @return true se il SiteCrawler è in esecuzione */
    boolean isRunning();

    /** Ritorna true se il SiteCrawler è stato cancellato. In tal caso non può
     * più essere usato.
     * @return true se il SiteCrawler è stato cancellato */
    boolean isCancelled();


    /** Ritorna true se il SiteCralwer è sospeso
     *  @return true se il site crawler è sospeso.
     */
    boolean isSuspended();



    /** Ritorna true se il SiteCralwer è terminato
     *  @return true se il site crawler è terminato.
     */
    boolean isTerminated();

}
