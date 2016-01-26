package wsa.session;

import wsa.exceptions.DominioException;
import wsa.web.SiteCrawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by gaamda on 03/11/2015.
 *
 * Rappresenta un dominio.
 * Per definizione, non fornisce URL.
 */
public class Dominio {

    private URI uri = null;
    private URL url = null;

    /**
     * Crea un nuovo dominio con l'uri associato.
     * @param uri L'uri da usare come dominio.
     */
    public Dominio(String uri) throws DominioException {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new DominioException(e.getInput(), e.getReason());
        }
    }

    /**
     * Valida questo dominio secondo le specifiche dell'interfaccia {@link SiteCrawler}.
     * @return True se il dominio è valido, false altrimenti.
     */
    public boolean validateDom(){
        return SiteCrawler.checkDomain(this.uri);
    }

    void setURL(URL url){
        this.url = url;
    }

    public URI getURI() {
        return this.uri;
    }

    public URL getURL() {
        return this.url;
    }

    /**
     * Overriding di equals per dominio.
     * Un dominio è uguale all'altro se e solo se gli uri che contengono sono uguali.
     * @param other Il dominio da controllare.
     * @return True se uguali o false altrimenti.
     */
    @Override
    public boolean equals(Object other){
        if (other != null && getClass() == other.getClass()){
            Dominio o = (Dominio)other;
            return this.uri.equals(o.uri);
        } else return false;
    }

    public int hashCode(){
        return this.uri.hashCode();
    }

    @Override
    public String toString(){
        return uri.toString();
    }
}
