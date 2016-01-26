package wsa.web;

/** Una factory per Loader */
public interface LoaderFactory {
    /** Ritorna una nuova istanza di un Loader
     * @return una nuova istanza di un Loader */
    Loader newInstance();
}
