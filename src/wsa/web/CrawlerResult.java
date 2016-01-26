package wsa.web;

import java.net.URI;
import java.util.List;

/** Il risultato del tentativo di scaricare una pagina tramite un Crawler */
public class CrawlerResult {
    /** L'URI della pagina o null. Se null, significa che la prossima pagina
     * non è ancora pronta. */
    public final URI uri;
    /** true se l'URI è di una pagina i cui link sono seguiti. Se false i campi
     * links e errRawLinks sono null. */
    public final boolean linkPage;
    /** La lista degli URI assoluti dei link della pagina o null */
    public final List<URI> links;
    /** La lista dei link che non è stato possibile trasformare in URI assoluti
     * o null */
    public final List<String> errRawLinks;
    /** Se è null, la pagina è stata scaricata altrimenti non è stato possibile
     * scaricarla e l'eccezione ne dà la causa */
    public final Exception exc;

    public CrawlerResult(URI u, boolean lp, List<URI> ll, List<String> erl,
                         Exception e) {
        uri = u;
        linkPage = lp;
        links = ll;
        errRawLinks = erl;
        exc = e;
    }
}

