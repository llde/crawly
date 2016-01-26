package wsa.web;

import wsa.web.html.Parsed;
import java.net.URL;

/** Il risultato del tentativo di scaricare una pagina web */
public class LoadResult {
    /** L'URL della pagina web */
    public final URL url;
    /** L'analisi sintattica della pagina scaricata o null se è accaduto un
     * errore */
    public final Parsed parsed;
    /** Se diverso da null, la pagina non è stata scaricata è la causa è
     * specificata dall'eccezione */
    public final Exception exc;

    public LoadResult(URL u, Parsed p, Exception e) {
        url = u;
        parsed = p;
        exc = e;
    }
}

