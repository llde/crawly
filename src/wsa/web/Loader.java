package wsa.web;

import java.net.URL;

/** Un Loader permette di scaricare una pagina alla volta */
public interface Loader {
    /** Ritorna il risultato del tentativo di scaricare la pagina specificata. È
     * bloccante, finchè l'operazione non è conclusa non ritorna.
     * @param url  l'URL di una pagina web
     * @return il risultato del tentativo di scaricare la pagina */
    LoadResult load(URL url);

    /** Ritorna null se l'URL è scaricabile senza errori, altrimenti ritorna
     * un'eccezione che riporta l'errore.
     * @param url  un URL
     * @return null se l'URL è scaricabile senza errori, altrimenti
     * l'eccezione */
    Exception check(URL url);
}
